package co.edu.udistrital.acciones.m8notificaciones.application.service;

import co.edu.udistrital.acciones.m8notificaciones.application.port.out.AvisosRepositoryPort;
import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;
import co.edu.udistrital.acciones.m8notificaciones.domain.DireccionCorreo;
import co.edu.udistrital.acciones.m8notificaciones.domain.EventoAviso;
import co.edu.udistrital.acciones.m8notificaciones.domain.TipoAviso;

import com.fasterxml.jackson.databind.JsonNode;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class EventosService {
    private final AvisosRepositoryPort repository;
    private final NotificacionesService notifications;
    private final Clock clock;

    public EventosService(
            AvisosRepositoryPort repository, NotificacionesService notifications, Clock clock) {
        this.repository = repository;
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional
    public void procesar(EventoAviso event) {
        if (event.eventId() == null
                || event.occurredAt() == null
                || event.payload() == null
                || !event.payload().isObject()) {
            throw invalid();
        }
        String type = event.eventType();
        String source = origen(type);
        if (!source.equals(event.source())) {
            throw invalid();
        }
        TipoAviso category = categoria(type);
        if (!repository.registrarEvento(event)) {
            return;
        }
        if (source.equals("M1")) {
            autenticacion(event);
        } else if (category == TipoAviso.APERTURA_MERCADO
                || category == TipoAviso.CIERRE_MERCADO
                || category == TipoAviso.CAMBIO_HORARIO_MERCADO) {
            mercado(event, category);
        } else {
            long user = usuario(event.payload());
            if (type.equals("m10.suscripcion.cancelada")
                    || type.equals("m10.suscripcion.vencida")) {
                repository.desactivarAlertas(user);
            }
            notificar(event, user, category, null);
        }
        repository.finalizarEvento(event.eventId());
    }

    private void autenticacion(EventoAviso event) {
        long user = usuario(event.payload());
        String email = texto(event.payload(), "email");
        if (!DireccionCorreo.valida(email)) {
            throw invalid();
        }
        String title;
        String message;
        Instant expiry = clock.instant().plus(Duration.ofHours(23));
        if (event.eventType().equals("m1.usuario.registrado")) {
            title = "Bienvenido a Acciones UD";
            message = "Tu registro en Acciones UD fue recibido.";
        } else {
            String code = texto(event.payload(), "code");
            if (!code.matches("[0-9]{6}")) {
                throw invalid();
            }
            try {
                expiry = Instant.parse(texto(event.payload(), "expiresAt"));
            } catch (java.time.format.DateTimeParseException exception) {
                throw invalid();
            }
            if (expiry.isAfter(event.occurredAt().plus(Duration.ofMinutes(15)))) {
                throw invalid();
            }
            title =
                    switch (event.eventType()) {
                        case "m1.mfa.solicitado" -> "Código de acceso a Acciones UD";
                        case "m1.verificacion.solicitada" -> "Verifica tu correo en Acciones UD";
                        default -> "Recuperación de acceso a Acciones UD";
                    };
            message =
                    "Tu código es "
                            + code
                            + ". Caduca en "
                            + expiry
                            + ". Si no lo solicitaste, ignora este correo.";
        }
        if (!expiry.isAfter(clock.instant())) {
            return;
        }
        notifications.encolar(
                NotificacionesService.entregaId(event.eventId(), user),
                new Correo(user, email, title, "<p>" + HtmlUtils.htmlEscape(message) + "</p>"),
                null,
                null,
                expiry);
    }

    private void mercado(EventoAviso event, TipoAviso category) {
        String market = texto(event.payload(), "market");
        if (!List.of("NYSE", "NASDAQ").contains(market)) {
            throw invalid();
        }
        long after = 0;
        while (true) {
            List<Long> users = repository.suscriptores(market, after);
            if (users.isEmpty()) {
                return;
            }
            for (long user : users) {
                notificar(event, user, category, market);
            }
            after = users.getLast();
        }
    }

    private void notificar(EventoAviso event, long user, TipoAviso category, String market) {
        JsonNode orderNode = event.payload().get("orderId");
        Long order = orderNode == null || orderNode.isNull() ? null : positivo(orderNode);
        String title =
                switch (category) {
                    case CAMBIO_ESTADO_ORDEN -> "Cambio de estado de orden";
                    case EJECUCION_ORDEN -> "Ejecución de orden";
                    case ACTUALIZACION_SALDO -> "Actualización de saldo";
                    case LIBERACION_FONDOS -> "Liberación de fondos";
                    case APERTURA_MERCADO -> "Apertura de mercado";
                    case CIERRE_MERCADO -> "Cierre de mercado";
                    case CAMBIO_HORARIO_MERCADO -> "Cambio de horario de mercado";
                    default -> "Actualización de suscripción";
                };
        String message =
                title
                        + (order == null ? "" : " de la orden " + order)
                        + (market == null ? "" : " " + market)
                        + ". Consulta el detalle en Acciones UD.";
        notifications.crear(
                event.eventId(), user, category, title, message, order, market, null, null);
    }

    public static TipoAviso categoria(String type) {
        return switch (type) {
            case "m1.usuario.registrado",
                            "m1.mfa.solicitado",
                            "m1.verificacion.solicitada",
                            "m1.recuperacion.solicitada" ->
                    null;
            case "m2.movimiento.creado", "m2.fondos.confirmados", "m2.fondos.reservados" ->
                    TipoAviso.ACTUALIZACION_SALDO;
            case "m2.fondos.liberados" -> TipoAviso.LIBERACION_FONDOS;
            case "m4.mercado.abierto" -> TipoAviso.APERTURA_MERCADO;
            case "m4.mercado.cerrado" -> TipoAviso.CIERRE_MERCADO;
            case "m4.mercado.horario.cambiado" -> TipoAviso.CAMBIO_HORARIO_MERCADO;
            case "m5.orden.ejecutada" -> TipoAviso.EJECUCION_ORDEN;
            case "m5.orden.creada",
                            "m5.orden.cancelada",
                            "m5.orden.rechazada",
                            "m5.orden.actualizada" ->
                    TipoAviso.CAMBIO_ESTADO_ORDEN;
            case "m10.suscripcion.activada",
                            "m10.suscripcion.cancelada",
                            "m10.suscripcion.vencida",
                            "m10.pago.fallido" ->
                    TipoAviso.SUSCRIPCION;
            default -> throw invalid();
        };
    }

    public static String origen(String type) {
        categoria(type);
        return type.substring(0, type.indexOf('.')).toUpperCase(java.util.Locale.ROOT);
    }

    private static long usuario(JsonNode payload) {
        JsonNode value = payload.has("userId") ? payload.get("userId") : payload.get("usuarioId");
        return positivo(value);
    }

    private static long positivo(JsonNode node) {
        try {
            if (node == null || !node.asText().matches("[1-9][0-9]{0,18}")) {
                throw invalid();
            }
            long value = Long.parseLong(node.asText());
            if (value <= 0) {
                throw invalid();
            }
            return value;
        } catch (NumberFormatException exception) {
            throw invalid();
        }
    }

    private static String texto(JsonNode payload, String field) {
        JsonNode node = payload.get(field);
        if (node == null || !node.isTextual() || node.asText().isBlank()) {
            throw invalid();
        }
        return node.asText();
    }

    private static AvisoException invalid() {
        return new AvisoException(400, "Evento inválido o no compatible con el contrato M8.");
    }
}
