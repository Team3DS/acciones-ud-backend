package co.edu.udistrital.acciones.m2cuentasfondos.application.service;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ConfirmarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.LiberarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenCanceladaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenEjecutadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenRechazadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SuscripcionActivadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.EventoProcesadoPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.AcreditarVentaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConfirmarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.NegocioException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class ProcesarEventoEntranteService {

    private static final Logger log = LoggerFactory.getLogger(ProcesarEventoEntranteService.class);
    private static final String MOTIVO_CANCELADA = "OrdenCancelada";
    private static final String MOTIVO_RECHAZADA = "OrdenRechazada";

    private final TransactionTemplate transactionTemplate;
    private final EventoProcesadoPort eventosProcesados;
    private final ReservaRepositoryPort reservas;
    private final ConfirmarReservaUseCase confirmarReserva;
    private final LiberarReservaUseCase liberarReserva;
    private final AcreditarVentaUseCase acreditarVenta;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ProcesarEventoEntranteService(TransactionTemplate transactionTemplate,
                                         EventoProcesadoPort eventosProcesados,
                                         ReservaRepositoryPort reservas,
                                         ConfirmarReservaUseCase confirmarReserva,
                                         LiberarReservaUseCase liberarReserva,
                                         AcreditarVentaUseCase acreditarVenta,
                                         ObjectMapper objectMapper,
                                         Clock clock) {
        this.transactionTemplate = transactionTemplate;
        this.eventosProcesados = eventosProcesados;
        this.reservas = reservas;
        this.confirmarReserva = confirmarReserva;
        this.liberarReserva = liberarReserva;
        this.acreditarVenta = acreditarVenta;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    public void procesar(EventoEntrante evento) {
        try {
            transactionTemplate.executeWithoutResult(estado -> {
                boolean nuevo = eventosProcesados.registrarSiNuevo(evento.eventId(), evento.eventType(),
                        Instant.now(clock));
                if (!nuevo) {
                    log.info("El evento {} de tipo {} ya fue procesado; se ignora",
                            evento.eventId(), evento.eventType());
                    return;
                }
                despachar(evento);
            });
        } catch (NegocioException ex) {
            log.warn("El evento {} de tipo {} fue descartado por una regla de negocio: {}",
                    evento.eventId(), evento.eventType(), ex.getMessage());
            transactionTemplate.executeWithoutResult(estado ->
                    eventosProcesados.registrarSiNuevo(evento.eventId(), evento.eventType(), Instant.now(clock)));
        }
    }

    private void despachar(EventoEntrante evento) {
        switch (evento.eventType()) {
            case RoutingKeys.M5_ORDEN_EJECUTADA -> procesarEjecutada(evento);
            case RoutingKeys.M5_ORDEN_CANCELADA -> {
                OrdenCanceladaPayload payload = convertir(evento.payload(), OrdenCanceladaPayload.class);
                liberarOrden(evento, payload == null ? null : payload.orderId(),
                        motivo(payload == null ? null : payload.reason(), MOTIVO_CANCELADA));
            }
            case RoutingKeys.M5_ORDEN_RECHAZADA -> {
                OrdenRechazadaPayload payload = convertir(evento.payload(), OrdenRechazadaPayload.class);
                liberarOrden(evento, payload == null ? null : payload.orderId(),
                        motivo(payload == null ? null : payload.reason(), MOTIVO_RECHAZADA));
            }
            case RoutingKeys.M10_SUSCRIPCION_ACTIVADA -> registrarSuscripcion(evento);
            default -> log.warn("Tipo de evento desconocido '{}' en el evento {}; se descarta",
                    evento.eventType(), evento.eventId());
        }
    }

    private void procesarEjecutada(EventoEntrante evento) {
        OrdenEjecutadaPayload payload = obligatorio(convertir(evento.payload(), OrdenEjecutadaPayload.class), evento);
        if (payload.esVentaDelUsuario()) {
            acreditarVenta.ejecutar(payload);
            return;
        }
        confirmarOrden(evento, payload);
    }

    private void confirmarOrden(EventoEntrante evento, OrdenEjecutadaPayload payload) {
        String orderId = orderIdObligatorio(payload.orderId(), evento);
        Optional<Reserva> reserva = reservas.findByOrderId(orderId);
        if (reserva.isEmpty()) {
            log.warn("No existe reserva para la orden {} del evento {}; se descarta", orderId, evento.eventId());
            return;
        }
        if (payload.executionId() == null || payload.executionId().isBlank() || payload.totalAmount() == null) {
            throw new IllegalArgumentException("El evento " + evento.eventId()
                    + " no trae executionId o totalAmount para confirmar la orden " + orderId);
        }
        confirmarReserva.ejecutar(reserva.get().getId(),
                new ConfirmarReservaRequest(payload.executionId(), payload.totalAmount()), Actor.sistema());
    }

    private void liberarOrden(EventoEntrante evento, String orderId, String motivo) {
        String orden = orderIdObligatorio(orderId, evento);
        Optional<Reserva> reserva = reservas.findByOrderId(orden);
        if (reserva.isEmpty()) {
            log.warn("No existe reserva para la orden {} del evento {}; se descarta", orden, evento.eventId());
            return;
        }
        liberarReserva.ejecutar(reserva.get().getId(), new LiberarReservaRequest(motivo), Actor.sistema());
    }

    private String motivo(String reason, String porDefecto) {
        return reason == null || reason.isBlank() ? porDefecto : reason;
    }

    private void registrarSuscripcion(EventoEntrante evento) {
        SuscripcionActivadaPayload payload = convertir(evento.payload(), SuscripcionActivadaPayload.class);
        if (payload == null) {
            log.info("Suscripción activada en el evento {} sin payload; no se mueven fondos", evento.eventId());
            return;
        }
        log.info("Suscripción activada para el usuario {} (plan {}, suscripción {}) en el evento {}; "
                        + "no se mueven fondos", payload.userId(), payload.planId(), payload.stripeSubscriptionId(),
                evento.eventId());
    }

    private String orderIdObligatorio(String orderId, EventoEntrante evento) {
        if (orderId == null || orderId.isBlank()) {
            throw new IllegalArgumentException("El evento " + evento.eventId() + " no trae orderId");
        }
        return orderId;
    }

    private <T> T obligatorio(T payload, EventoEntrante evento) {
        if (payload == null) {
            throw new IllegalArgumentException("El evento " + evento.eventId() + " no trae payload");
        }
        return payload;
    }

    private <T> T convertir(JsonNode payload, Class<T> tipo) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        try {
            return objectMapper.treeToValue(payload, tipo);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("El payload del evento no tiene el formato esperado", ex);
        }
    }
}
