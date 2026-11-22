package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CancelarOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
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

    public CancelarOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                    HistorialEstadoOrdenRepositoryPort historialRepository,
                                    OutboxEventoRepositoryPort outboxRepository,
                                    FondosClientPort fondosClient,
                                    PortafolioClientPort portafolioClient,
                                    AlpacaOrderPort alpacaOrderPort,
                                    PublicadorOrdenesPort publicador,
                                    ObjectMapper objectMapper) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.outboxRepository = outboxRepository;
        this.fondosClient = fondosClient;
        this.portafolioClient = portafolioClient;
        this.alpacaOrderPort = alpacaOrderPort;
        this.publicador = publicador;
        this.objectMapper = objectMapper;
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
            try {
                alpacaOrderPort.cancelarOrden(orden.getBrokerOrderId());
            } catch (Exception ex) {
                // Registrar intento
            }
        }

        // Liberar reservas locales si existen (SAD RF-34)
        if (orden.getReservaFondosId() != null) {
            try {
                fondosClient.liberarReserva(orden.getReservaFondosId(), motivoReal);
            } catch (Exception ignored) {}
        }
        if (orden.getReservaPosicionId() != null) {
            try {
                portafolioClient.liberarReserva(orden.getReservaPosicionId(), motivoReal);
            } catch (Exception ignored) {}
        }

        orden.setEstadoOrden(EstadoOrden.CANCELADA);
        orden.setActualizadaEn(Instant.now());
        ordenRepository.save(orden);

        historialRepository.save(HistorialEstadoOrden.registrar(
                orden.getId(), EstadoOrden.CANCELADA, TipoActor.USUARIO, usuarioId, motivoReal));

        registrarOutbox(orden, motivoReal);
        publicador.publicarOrdenCancelada(orden.getId(), motivoReal);
    }

    private void registrarOutbox(Orden orden, String motivo) {
        try {
            Map<String, Object> carga = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "orderId", orden.getId(),
                    "userId", orden.getUsuarioId(),
                    "symbol", orden.getTicker(),
                    "reason", motivo
            );
            String json = objectMapper.writeValueAsString(carga);
            outboxRepository.save(OutboxEvento.crear("ORDEN", String.valueOf(orden.getId()), "OrdenCancelada", json));
        } catch (Exception ignored) {
        }
    }
}
