package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class CancelarOrdenUseCaseImpl implements CancelarOrdenUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final HistorialEstadoOrdenRepositoryPort historialRepository;
    private final OutboxEventoRepositoryPort outboxRepository;
    private final FondosClientPort fondosClient;
    private final PortafolioClientPort portafolioClient;
    private final AlpacaOrderPort alpacaOrderPort;
    private final PublicadorOrdenesPort publicador;
    private final ObjectMapper objectMapper;
    private final EjecucionOrdenRepositoryPort ejecucionRepository;
    private final ConfirmacionEjecucionRepositoryPort confirmacionRepository;

    public CancelarOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                    HistorialEstadoOrdenRepositoryPort historialRepository,
                                    OutboxEventoRepositoryPort outboxRepository,
                                    FondosClientPort fondosClient,
                                    PortafolioClientPort portafolioClient,
                                    AlpacaOrderPort alpacaOrderPort,
                                    PublicadorOrdenesPort publicador,
                                    ObjectMapper objectMapper,
                                    EjecucionOrdenRepositoryPort ejecucionRepository,
                                    ConfirmacionEjecucionRepositoryPort confirmacionRepository) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.outboxRepository = outboxRepository;
        this.fondosClient = fondosClient;
        this.portafolioClient = portafolioClient;
        this.alpacaOrderPort = alpacaOrderPort;
        this.publicador = publicador;
        this.objectMapper = objectMapper;
        this.ejecucionRepository = ejecucionRepository;
        this.confirmacionRepository = confirmacionRepository;
    }

    @Override
    public void ejecutar(Long ordenId, Long usuarioId, String motivo) {
        Orden orden = ordenRepository.findById(ordenId)
                .orElseThrow(() -> new IllegalArgumentException("Orden no encontrada: " + ordenId));

        if (!orden.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("No tiene permisos para cancelar esta orden");
        }

        if (orden.getEstadoOrden() == EstadoOrden.CANCELADA) {
            return; // Idempotente
        }

        if (orden.getEstadoOrden() == EstadoOrden.EJECUTADA) {
            throw new IllegalStateException("La orden ya fue ejecutada y no se puede cancelar");
        }

        String motivoReal = (motivo != null && !motivo.isBlank()) ? motivo : "Cancelada por usuario";

        // Si la orden ya está en el broker
        if (orden.getBrokerOrderId() != null && !orden.getBrokerOrderId().isBlank()) {
            // Si el broker rechaza o no responde, conservar estado y reservas para no dejar una orden activa sin respaldo.
            AlpacaOrderPort.AlpacaOrderResponse confirmada = alpacaOrderPort.cancelarOrden(orden.getBrokerOrderId());
            verificarEjecucionesConciliadas(orden, confirmada);
        }

        // Liberar reservas locales si existen (SAD RF-34)
        if (orden.getReservaFondosId() != null) {
            try {
                fondosClient.liberarReserva(orden.getReservaFondosId(), orden.getUsuarioId(), motivoReal);
            } catch (Exception ignored) {}
        }
        if (orden.getReservaPosicionId() != null) {
            try {
                portafolioClient.liberarReserva(orden.getReservaPosicionId(), orden.getUsuarioId(), motivoReal);
            } catch (Exception ignored) {}
        }

        orden.setEstadoOrden(EstadoOrden.CANCELADA);
        orden.setActualizadaEn(Instant.now());
        ordenRepository.save(orden);

        historialRepository.save(HistorialEstadoOrden.registrar(
                orden.getId(), EstadoOrden.CANCELADA, TipoActor.USUARIO, usuarioId, motivoReal));

        UUID eventoCancelada = UUID.randomUUID();
        registrarOutbox(eventoCancelada, orden, motivoReal);
        publicador.publicarOrdenCancelada(eventoCancelada, orden.getId(), orden.getUsuarioId(), motivoReal);
    }

    private void verificarEjecucionesConciliadas(Orden orden, AlpacaOrderPort.AlpacaOrderResponse confirmada) {
        if (confirmada == null || confirmada.id() == null || confirmada.status() == null
                || !java.util.Set.of("canceled", "expired", "rejected").contains(confirmada.status())) {
            throw new IllegalStateException("El broker todavía no confirmó la cancelación; se mantienen las reservas");
        }
        var ejecuciones = ejecucionRepository.findByOrdenId(orden.getId());
        int cantidadLocal = ejecuciones.stream().mapToInt(EjecucionOrden::getCantidad).sum();
        if (!orden.getBrokerOrderId().startsWith(AlpacaOrderPort.PREFIJO_SIMULADO)
                && (confirmada.filledQty() == null || confirmada.filledQty() != cantidadLocal)) {
            throw new IllegalStateException("Hay ejecuciones del broker pendientes de conciliar; se mantienen las reservas");
        }
        for (EjecucionOrden ejecucion : ejecuciones) {
            boolean portafolioConfirmado = confirmacionRepository.findByEjecucionIdAndServicio(
                    ejecucion.getId(), ServicioParticipante.PORTAFOLIO).isPresent();
            boolean fondosConfirmados = orden.getLadoOrden() != LadoOrden.COMPRA
                    || confirmacionRepository.findByEjecucionIdAndServicio(ejecucion.getId(), ServicioParticipante.FONDOS).isPresent();
            if (!portafolioConfirmado || !fondosConfirmados) {
                throw new IllegalStateException("La aplicación de las ejecuciones aún está pendiente en fondos o portafolio; se mantienen las reservas");
            }
        }
        orden.setBrokerOrderId(confirmada.id());
    }

    /** Guarda en el outbox el evento OrdenCancelada con el mismo sobre y eventId que el publicador directo. */
    private void registrarOutbox(UUID eventId, Orden orden, String motivo) {
        try {
            Map<String, Object> carga = EventosOrden.sobre(eventId, EventosOrden.ORDEN_CANCELADA, orden.getId(),
                    EventosOrden.cancelada(orden.getId(), orden.getUsuarioId(), motivo));
            String json = objectMapper.writeValueAsString(carga);
            outboxRepository.save(OutboxEvento.crear("ORDEN", String.valueOf(orden.getId()), "OrdenCancelada", json));
        } catch (Exception ignored) {
        }
    }
}
