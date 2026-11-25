package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionRequest;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EjecucionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionesActualizadasPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.EjecucionAplicadaPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.MovimientoPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.ReservaPosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.domain.EjecucionAplicada;
import co.edu.udistrital.acciones.m6portafolio.domain.EstadoReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.LadoEjecucion;
import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
public class AplicarEjecucionUseCase {

    private final PosicionRepositoryPort posiciones;
    private final ReservaPosicionRepositoryPort reservas;
    private final MovimientoPosicionRepositoryPort movimientos;
    private final EjecucionAplicadaPort ejecucionesAplicadas;
    private final ValoracionService valoracion;
    private final OutboxPort outbox;
    private final Clock clock;

    public AplicarEjecucionUseCase(
            PosicionRepositoryPort posiciones,
            ReservaPosicionRepositoryPort reservas,
            MovimientoPosicionRepositoryPort movimientos,
            EjecucionAplicadaPort ejecucionesAplicadas,
            ValoracionService valoracion,
            OutboxPort outbox,
            Clock clock) {
        this.posiciones = posiciones;
        this.reservas = reservas;
        this.movimientos = movimientos;
        this.ejecucionesAplicadas = ejecucionesAplicadas;
        this.valoracion = valoracion;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public EjecucionResponse ejecutar(EjecucionRequest peticion, Actor actor) {
        actor.verificarOperacion(peticion.userId());
        String symbol = Simbolos.normalizar(peticion.symbol());
        UUID claveIdempotencia = claveIdempotencia(peticion);
        if (ejecucionesAplicadas.existePorClaveIdempotencia(claveIdempotencia)) {
            return repetida(peticion, symbol, claveIdempotencia, null);
        }
        if (ejecucionesAplicadas.existePorExecutionId(peticion.executionId())) {
            return repetida(peticion, symbol, claveIdempotencia, null);
        }
        Instant ahora = Instant.now(clock);
        Instant ocurrido = peticion.executedAt() == null ? ahora : peticion.executedAt();
        Posicion posicion = obtenerPosicionBloqueada(peticion, symbol, ahora);
        if (ejecucionesAplicadas.existePorClaveIdempotencia(claveIdempotencia)) {
            return repetida(peticion, symbol, claveIdempotencia, posicion);
        }
        if (ejecucionesAplicadas.existePorExecutionId(peticion.executionId())) {
            return repetida(peticion, symbol, claveIdempotencia, posicion);
        }
        BigDecimal costoAnterior = posicion.getCostoPromedio();
        aplicarMovimiento(peticion, posicion, ocurrido);
        BigDecimal costoEjecucion =
                peticion.side() == LadoEjecucion.SELL ? costoAnterior : posicion.getCostoPromedio();
        posiciones.save(posicion);
        movimientos.save(crearMovimiento(peticion, posicion, claveMovimiento(peticion), ocurrido));
        EjecucionAplicada aplicada =
                new EjecucionAplicada(
                        posicion.getId(),
                        peticion.executionId(),
                        peticion.side(),
                        peticion.quantity(),
                        peticion.price(),
                        costoEjecucion,
                        claveIdempotencia,
                        Instant.now(clock));
        ejecucionesAplicadas.save(aplicada);
        outbox.registrar(
                RoutingKeys.TIPO_AGREGADO_POSICION,
                String.valueOf(posicion.getId()),
                RoutingKeys.POSICIONES_ACTUALIZADAS,
                new PosicionesActualizadasPayload(
                        peticion.orderId(),
                        peticion.executionId(),
                        peticion.userId(),
                        symbol,
                        peticion.side().name(),
                        peticion.quantity(),
                        peticion.price(),
                        posicion.getCantidad(),
                        costoEjecucion,
                        posicion.getPnlRealizado()));
        return new EjecucionResponse(true, valoracion.valorar(posicion), costoEjecucion);
    }

    private UUID claveIdempotencia(EjecucionRequest peticion) {
        if (peticion.eventId() != null && !peticion.eventId().isBlank()) {
            try {
                return UUID.fromString(peticion.eventId());
            } catch (IllegalArgumentException ignored) {
                return UUID.nameUUIDFromBytes(
                        ("EVENTO:" + peticion.eventId()).getBytes(StandardCharsets.UTF_8));
            }
        }
        return UUID.nameUUIDFromBytes(
                ("EXE:" + peticion.executionId()).getBytes(StandardCharsets.UTF_8));
    }

    private String claveMovimiento(EjecucionRequest peticion) {
        return "EXE:" + peticion.executionId();
    }

    private Posicion obtenerPosicionBloqueada(
            EjecucionRequest peticion, String symbol, Instant ahora) {
        if (peticion.side() == LadoEjecucion.BUY) {
            posiciones.crearSiNoExiste(peticion.userId(), symbol, ahora);
        }
        return posiciones
                .findByUserIdAndSymbolForUpdate(peticion.userId(), symbol)
                .orElseThrow(() -> posicionNoDisponible(peticion, symbol));
    }

    private RuntimeException posicionNoDisponible(EjecucionRequest peticion, String symbol) {
        String mensaje = "No existe la posicion " + symbol + " del usuario " + peticion.userId();
        if (peticion.side() == LadoEjecucion.SELL) {
            return new VentaNoDisponibleException(mensaje);
        }
        return new RecursoNoEncontradoException(mensaje);
    }

    private void aplicarMovimiento(EjecucionRequest peticion, Posicion posicion, Instant ocurrido) {
        if (peticion.side() == LadoEjecucion.BUY) {
            posicion.comprar(peticion.quantity(), peticion.price(), ocurrido);
            return;
        }
        ReservaPosicion reserva = reservas.findByOrderIdForUpdate(peticion.orderId()).orElse(null);
        if (reserva != null && reserva.getEstado() == EstadoReservaPosicion.ACTIVA) {
            if (!reserva.getPosicionId().equals(posicion.getId())) {
                throw new VentaNoDisponibleException(
                        "La reserva no corresponde a la posicion de la venta");
            }
            reserva.consumir(peticion.quantity().intValue());
            posicion.venderReservado(peticion.quantity(), peticion.price(), ocurrido);
            reservas.save(reserva);
            return;
        }
        posicion.venderDisponible(peticion.quantity(), peticion.price(), ocurrido);
    }

    private MovimientoPosicion crearMovimiento(
            EjecucionRequest peticion, Posicion posicion, String clave, Instant ocurrido) {
        if (peticion.side() == LadoEjecucion.BUY) {
            return MovimientoPosicion.compra(
                    posicion.getId(),
                    peticion.quantity(),
                    peticion.price(),
                    peticion.executionId(),
                    peticion.orderId(),
                    clave,
                    ocurrido);
        }
        return MovimientoPosicion.venta(
                posicion.getId(),
                peticion.quantity(),
                peticion.price(),
                peticion.executionId(),
                peticion.orderId(),
                clave,
                ocurrido);
    }

    private EjecucionResponse repetida(
            EjecucionRequest peticion, String symbol, UUID clave, Posicion bloqueada) {
        EjecucionAplicada aplicada =
                ejecucionesAplicadas
                        .findByClaveIdempotencia(clave)
                        .or(() -> ejecucionesAplicadas.findByExecutionId(peticion.executionId()))
                        .orElseThrow(
                                () ->
                                        new ConflictoIdempotenciaException(
                                                "La ejecucion ya fue aplicada y su costo no esta"
                                                    + " disponible"));
        Posicion posicion =
                bloqueada != null
                        ? bloqueada
                        : posiciones
                                .findByUserIdAndSymbol(peticion.userId(), symbol)
                                .orElseThrow(
                                        () ->
                                                new ConflictoIdempotenciaException(
                                                        "La ejecucion ya fue aplicada y la posicion"
                                                            + " no esta disponible"));
        if (!aplicada.getPosicionId().equals(posicion.getId())) {
            throw new ConflictoIdempotenciaException("La ejecucion corresponde a otra posicion");
        }
        return new EjecucionResponse(
                false, valoracion.valorar(posicion), aplicada.getPrecioPromedioCosto());
    }
}
