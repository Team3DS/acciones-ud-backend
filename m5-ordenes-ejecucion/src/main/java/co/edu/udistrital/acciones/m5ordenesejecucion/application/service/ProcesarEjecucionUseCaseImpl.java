package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class ProcesarEjecucionUseCaseImpl implements ProcesarEjecucionUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final ConfiguracionComisionRepositoryPort comisionRepository;
    private final EjecucionOrdenRepositoryPort ejecucionRepository;
    private final ConfirmacionEjecucionRepositoryPort confirmacionRepository;
    private final HistorialEstadoOrdenRepositoryPort historialRepository;
    private final OutboxEventoRepositoryPort outboxRepository;
    private final FondosClientPort fondosClient;
    private final PortafolioClientPort portafolioClient;
    private final PublicadorOrdenesPort publicador;
    private final ObjectMapper objectMapper;

    public ProcesarEjecucionUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                        ConfiguracionComisionRepositoryPort comisionRepository,
                                        EjecucionOrdenRepositoryPort ejecucionRepository,
                                        ConfirmacionEjecucionRepositoryPort confirmacionRepository,
                                        HistorialEstadoOrdenRepositoryPort historialRepository,
                                        OutboxEventoRepositoryPort outboxRepository,
                                        FondosClientPort fondosClient,
                                        PortafolioClientPort portafolioClient,
                                        PublicadorOrdenesPort publicador,
                                        ObjectMapper objectMapper) {
        this.ordenRepository = ordenRepository;
        this.comisionRepository = comisionRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.confirmacionRepository = confirmacionRepository;
        this.historialRepository = historialRepository;
        this.outboxRepository = outboxRepository;
        this.fondosClient = fondosClient;
        this.portafolioClient = portafolioClient;
        this.publicador = publicador;
        this.objectMapper = objectMapper;
    }

    @Override
    public EjecucionOrden ejecutar(ProcesarEjecucionCommand comando) {
        // 1. Idempotencia: UQ_EJECUCION_ORDEN_BROKER_EXECUTION
        Optional<EjecucionOrden> existente = ejecucionRepository.findByBrokerExecutionId(comando.brokerExecutionId());
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Buscar la orden asociada
        Orden orden = null;
        if (comando.ordenId() != null) {
            orden = ordenRepository.findById(comando.ordenId()).orElse(null);
        }
        if (orden == null && comando.brokerOrderId() != null) {
            orden = ordenRepository.findByBrokerOrderId(comando.brokerOrderId()).orElse(null);
        }
        if (orden == null) {
            throw new IllegalArgumentException("No se encontró orden para ejecución: " + comando.brokerExecutionId());
        }

        // 3. Determinar comisión
        BigDecimal comision = comando.comision();
        if (comision == null) {
            BigDecimal porcentaje = comisionRepository.findById(orden.getConfiguracionComisionId())
                    .map(c -> c.getPorcentaje())
                    .orElse(new BigDecimal("0.0020"));
            BigDecimal importe = comando.precioEjecucion().multiply(BigDecimal.valueOf(comando.cantidad()));
            comision = importe.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
        }

        Instant ejecutadaEn = comando.ejecutadaEn() != null ? comando.ejecutadaEn() : Instant.now();

        EjecucionOrden ejecucion = new EjecucionOrden(
                null,
                orden.getId(),
                comando.brokerExecutionId(),
                comando.cantidad(),
                comando.precioEjecucion(),
                comision,
                ejecutadaEn,
                Instant.now()
        );
        ejecucion = ejecucionRepository.save(ejecucion);

        // 4. Actualizar estado de la orden
        List<EjecucionOrden> previas = ejecucionRepository.findByOrdenId(orden.getId());
        int ejecutadoTotal = previas.stream().mapToInt(EjecucionOrden::getCantidad).sum();

        EstadoOrden nuevoEstado = (ejecutadoTotal >= orden.getCantidad())
                ? EstadoOrden.EJECUTADA
                : EstadoOrden.PARCIALMENTE_EJECUTADA;
        boolean finalExecution = nuevoEstado == EstadoOrden.EJECUTADA;
        Integer expectedExecutionCount = finalExecution ? previas.size() : null;

        orden.setEstadoOrden(nuevoEstado);
        orden.setActualizadaEn(Instant.now());
        ordenRepository.save(orden);

        historialRepository.save(HistorialEstadoOrden.registrar(
                orden.getId(), nuevoEstado, TipoActor.BROKER, null,
                "Ejecución registrada (Fill: " + comando.brokerExecutionId() + ", Qty: " + comando.cantidad() + ")"));

        // 5. Coordinar con M2 y M6
        UUID eventId = UUID.randomUUID();
        try {
            if (orden.getLadoOrden() == LadoOrden.COMPRA) {
                // Confirmar reserva en M2
                if (orden.getReservaFondosId() != null) {
                    BigDecimal montoTotalEjecucion = comando.precioEjecucion()
                            .multiply(BigDecimal.valueOf(comando.cantidad()))
                            .add(comision);
                    fondosClient.confirmarReserva(orden.getReservaFondosId(), orden.getUsuarioId(), comando.brokerExecutionId(),
                            montoTotalEjecucion, finalExecution, expectedExecutionCount);
                    confirmacionRepository.save(new ConfirmacionEjecucion(
                            ejecucion.getId(), ServicioParticipante.FONDOS, null, Instant.now()));
                }

                // Actualizar posición en M6
                BigDecimal costoPromedio = portafolioClient.actualizarPosicion(
                        String.valueOf(orden.getId()),
                        comando.brokerExecutionId(),
                        orden.getUsuarioId(),
                        orden.getTicker(),
                        LadoOrden.COMPRA,
                        comando.cantidad(),
                        comando.precioEjecucion(),
                        ejecutadaEn,
                        eventId.toString()
                );

                if (costoPromedio != null) {
                    confirmacionRepository.save(new ConfirmacionEjecucion(
                            ejecucion.getId(), ServicioParticipante.PORTAFOLIO, costoPromedio, Instant.now()));
                }
            } else {
                // Venta: actualizar posición en M6 (descarga acciones y devuelve averageCost)
                BigDecimal costoPromedio = portafolioClient.actualizarPosicion(
                        String.valueOf(orden.getId()),
                        comando.brokerExecutionId(),
                        orden.getUsuarioId(),
                        orden.getTicker(),
                        LadoOrden.VENTA,
                        comando.cantidad(),
                        comando.precioEjecucion(),
                        ejecutadaEn,
                        eventId.toString()
                );

                if (costoPromedio != null) {
                    confirmacionRepository.save(new ConfirmacionEjecucion(
                            ejecucion.getId(), ServicioParticipante.PORTAFOLIO, costoPromedio, Instant.now()));
                }

                // Confirmación fondos
                confirmacionRepository.save(new ConfirmacionEjecucion(
                        ejecucion.getId(), ServicioParticipante.FONDOS, null, Instant.now()));
            }
        } catch (Exception ex) {
            // Se registrará conciliación pendiente
        }

        // 6. Registrar en Outbox y publicar evento
        registrarOutboxEjecutada(eventId, comando.brokerExecutionId(), ejecucion, orden, finalExecution, expectedExecutionCount);
        publicador.publicarOrdenEjecutada(
                eventId,
                comando.brokerExecutionId(),
                orden.getId(),
                orden.getUsuarioId(),
                orden.getTicker(),
                orden.getLadoOrden().name(),
                ejecucion.getCantidad(),
                ejecucion.getPrecioEjecucion(),
                ejecucion.getComision(),
                ejecutadaEn,
                finalExecution,
                expectedExecutionCount
        );

        return ejecucion;
    }

    /** Guarda en el outbox el evento OrdenEjecutada con el mismo sobre y eventId que el publicador directo. */
    private void registrarOutboxEjecutada(UUID eventId, String executionId, EjecucionOrden ejecucion, Orden orden,
                                         boolean finalExecution, Integer expectedExecutionCount) {
        try {
            Map<String, Object> carga = EventosOrden.sobre(eventId, EventosOrden.ORDEN_EJECUTADA, orden.getId(),
                    EventosOrden.ejecutada(executionId, orden.getId(), orden.getUsuarioId(), orden.getTicker(),
                            orden.getLadoOrden().name(), ejecucion.getCantidad(), ejecucion.getPrecioEjecucion(),
                            ejecucion.getComision(), ejecucion.getEjecutadaEn(), finalExecution, expectedExecutionCount));
            String json = objectMapper.writeValueAsString(carga);
            outboxRepository.save(OutboxEvento.crear("EJECUCION_ORDEN", String.valueOf(ejecucion.getId()), "OrdenEjecutada", json));
        } catch (Exception ignored) {
        }
    }
}
