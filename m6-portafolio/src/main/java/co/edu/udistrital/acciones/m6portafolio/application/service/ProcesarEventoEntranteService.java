package co.edu.udistrital.acciones.m6portafolio.application.service;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m6portafolio.application.dto.FillPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.OrdenCanceladaPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.OrdenEjecutadaPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.OrdenRechazadaPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.exception.EventoInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.InboxEventoPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.AplicarEjecucionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.Cantidades;
import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.Montos;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.NegocioException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ProcesarEventoEntranteService {

    private static final Logger log = LoggerFactory.getLogger(ProcesarEventoEntranteService.class);
    private static final String MOTIVO_CANCELADA = "OrdenCancelada";
    private static final String MOTIVO_RECHAZADA = "OrdenRechazada";
    private static final String LADO_VENTA = "SELL";

    private final TransactionTemplate transacciones;
    private final InboxEventoPort inboxEventos;
    private final ReservaPosicionRepositoryPort reservas;
    private final PosicionRepositoryPort posiciones;
    private final AplicarEjecucionUseCase aplicarEjecucion;
    private final LiberarReservaUseCase liberarReserva;
    private final ObjectMapper json;
    private final Clock clock;

    public ProcesarEventoEntranteService(PlatformTransactionManager gestorTransacciones,
                                          InboxEventoPort inboxEventos,
                                          ReservaPosicionRepositoryPort reservas,
                                          PosicionRepositoryPort posiciones,
                                          AplicarEjecucionUseCase aplicarEjecucion,
                                          LiberarReservaUseCase liberarReserva,
                                          ObjectMapper json,
                                          Clock clock) {
        this.transacciones = new TransactionTemplate(gestorTransacciones);
        this.inboxEventos = inboxEventos;
        this.reservas = reservas;
        this.posiciones = posiciones;
        this.aplicarEjecucion = aplicarEjecucion;
        this.liberarReserva = liberarReserva;
        this.json = json;
        this.clock = clock;
    }

    public void procesar(EventoEntrante evento) {
        try {
            transacciones.executeWithoutResult(estado -> {
                if (!inboxEventos.registrarSiNuevo(evento.eventId(), evento.eventType(),
                        evento.aggregateId(), evento.correlationId(), evento.payload(),
                        Instant.now(clock))) {
                    log.info("El evento {} de tipo {} ya fue procesado; se ignora",
                            evento.eventId(), evento.eventType());
                    return;
                }
                despachar(evento);
            });
        } catch (NegocioException ex) {
            log.warn("El evento {} de tipo {} fue descartado por una regla de negocio: {}",
                    evento.eventId(), evento.eventType(), ex.getMessage());
            transacciones.executeWithoutResult(estado -> inboxEventos.registrarSiNuevo(
                    evento.eventId(), evento.eventType(),
                    evento.aggregateId(), evento.correlationId(), evento.payload(),
                    Instant.now(clock)));
        }
    }

    private void despachar(EventoEntrante evento) {
        switch (evento.eventType()) {
            case RoutingKeys.M5_ORDEN_EJECUTADA -> procesarEjecutada(evento);
            case RoutingKeys.M5_ORDEN_CANCELADA -> liberarCancelada(evento);
            case RoutingKeys.M5_ORDEN_RECHAZADA -> liberarRechazada(evento);
            default -> log.warn("Tipo de evento desconocido '{}' en el evento {}; se descarta",
                    evento.eventType(), evento.eventId());
        }
    }

    private void procesarEjecutada(EventoEntrante evento) {
        OrdenEjecutadaPayload payload = obligatorio(convertir(evento.payload(),
                OrdenEjecutadaPayload.class), evento);
        String orderId = exigirTexto(payload.orderId(), "orderId", evento);
        if (payload.traeDatosDePosicion()) {
            aplicar(evento, payload, orderId, payload.userId(), payload.symbol(), payload.side());
            return;
        }
        Optional<ReservaPosicion> reserva = reservas.findByOrderId(orderId);
        if (reserva.isEmpty() || reserva.get().getEstado() != EstadoReservaPosicion.ACTIVA) {
            log.warn("El evento {} no trae userId, symbol y side y no hay reserva ACTIVA para la orden {};"
                    + " se descarta", evento.eventId(), orderId);
            return;
        }
        Optional<Posicion> posicion = posiciones.findById(reserva.get().getPosicionId());
        if (posicion.isEmpty()) {
            log.warn("La reserva de la orden {} apunta a una posicion inexistente; se descarta el evento {}",
                    orderId, evento.eventId());
            return;
        }
        aplicar(evento, payload, orderId, posicion.get().getUserId(), posicion.get().getSymbol(), LADO_VENTA);
    }

    private void liberarCancelada(EventoEntrante evento) {
        OrdenCanceladaPayload payload = convertir(evento.payload(), OrdenCanceladaPayload.class);
        liberar(evento, payload == null ? null : payload.orderId(),
                motivo(payload == null ? null : payload.reason(), MOTIVO_CANCELADA));
    }

    private void liberarRechazada(EventoEntrante evento) {
        OrdenRechazadaPayload payload = convertir(evento.payload(), OrdenRechazadaPayload.class);
        liberar(evento, payload == null ? null : payload.orderId(),
                motivo(payload == null ? null : payload.reason(), MOTIVO_RECHAZADA));
    }

    private void liberar(EventoEntrante evento, String orderId, String motivo) {
        String orden = exigirTexto(orderId, "orderId", evento);
        Optional<ReservaPosicion> reserva = reservas.findByOrderId(orden);
        if (reserva.isEmpty()) {
            log.warn("No existe reserva para la orden {} del evento {}; se descarta", orden, evento.eventId());
            return;
        }
        if (reserva.get().getEstado() == EstadoReservaPosicion.CONSUMIDA) {
            log.info("La reserva de la orden {} ya fue consumida; no se libera por el evento {}",
                    orden, evento.eventId());
            return;
        }
        liberarReserva.ejecutar(reserva.get().getId(), motivo, Actor.interno());
    }

    private void aplicar(EventoEntrante evento, OrdenEjecutadaPayload payload, String orderId, Long userId,
                         String symbol, String side) {
        String executionId = exigirTexto(payload.executionId(), "executionId", evento);
        List<FillPayload> fills = payload.fills();
        if (fills == null || fills.isEmpty()) {
            throw new EventoInvalidoException("El evento " + evento.eventId()
                    + " no trae fills para aplicar la ejecucion " + executionId);
        }
        BigDecimal cantidad = Cantidades.cero();
        BigDecimal importe = BigDecimal.ZERO;
        for (FillPayload fill : fills) {
            if (fill == null) {
                throw new EventoInvalidoException("El evento " + evento.eventId() + " trae un fill vacio");
            }
            BigDecimal precio = Montos.validarPositivo(fill.price());
            BigDecimal unidades = Cantidades.validarPositiva(fill.qty());
            cantidad = cantidad.add(unidades);
            importe = importe.add(precio.multiply(unidades));
        }
        BigDecimal promedio = importe.divide(cantidad, Montos.ESCALA, Montos.REDONDEO);
        LadoEjecucion lado = lado(side, evento);
        aplicarEjecucion.ejecutar(new EjecucionRequest(orderId, executionId, userId,
                Simbolos.normalizar(symbol), lado, cantidad, promedio, evento.occurredAt(), evento.eventId()), Actor.interno());
    }

    private LadoEjecucion lado(String side, EventoEntrante evento) {
        try {
            return LadoEjecucion.valueOf(side.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new EventoInvalidoException("El evento " + evento.eventId()
                    + " trae un side invalido: " + side, ex);
        }
    }

    private static String motivo(String reason, String porDefecto) {
        return reason == null || reason.isBlank() ? porDefecto : reason;
    }

    private static String exigirTexto(String valor, String campo, EventoEntrante evento) {
        if (valor == null || valor.isBlank()) {
            throw new EventoInvalidoException("El evento " + evento.eventId() + " no trae " + campo);
        }
        return valor;
    }

    private static <T> T obligatorio(T payload, EventoEntrante evento) {
        if (payload == null) {
            throw new EventoInvalidoException("El evento " + evento.eventId() + " no trae payload");
        }
        return payload;
    }

    private <T> T convertir(JsonNode payload, Class<T> tipo) {
        if (payload == null || payload.isNull()) {
            return null;
        }
        try {
            return json.treeToValue(payload, tipo);
        } catch (com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new EventoInvalidoException("El payload del evento no tiene el formato esperado", ex);
        }
    }
}