package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort.AlpacaOrderRequest;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort.AlpacaOrderResponse;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class OrdenMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrdenMessageConsumer.class);

    private final OrdenRepositoryPort ordenRepository;
    private final HistorialEstadoOrdenRepositoryPort historialRepository;
    private final ConfirmacionEjecucionRepositoryPort confirmacionRepository;
    private final FondosClientPort fondosClient;
    private final PortafolioClientPort portafolioClient;
    private final AlpacaOrderPort alpacaOrderPort;
    private final PublicadorOrdenesPort publicador;
    private final ObjectMapper objectMapper;
    private final EjecucionOrdenRepositoryPort ejecucionRepository;

    public OrdenMessageConsumer(OrdenRepositoryPort ordenRepository,
                                HistorialEstadoOrdenRepositoryPort historialRepository,
                                ConfirmacionEjecucionRepositoryPort confirmacionRepository,
                                FondosClientPort fondosClient,
                                PortafolioClientPort portafolioClient,
                                AlpacaOrderPort alpacaOrderPort,
                                PublicadorOrdenesPort publicador,
                                ObjectMapper objectMapper,
                                EjecucionOrdenRepositoryPort ejecucionRepository) {
        this.ordenRepository = ordenRepository;
        this.historialRepository = historialRepository;
        this.confirmacionRepository = confirmacionRepository;
        this.fondosClient = fondosClient;
        this.portafolioClient = portafolioClient;
        this.alpacaOrderPort = alpacaOrderPort;
        this.publicador = publicador;
        this.objectMapper = objectMapper;
        this.ejecucionRepository = ejecucionRepository;
    }

    @RabbitListener(queues = RabbitConfig.COLA_ORDENES, containerFactory = "m5ListenerContainerFactory")
    @Transactional
    public void recibirMensaje(Message mensaje) {
        String routingKey = mensaje.getMessageProperties().getReceivedRoutingKey();
        String cuerpo = new String(mensaje.getBody(), StandardCharsets.UTF_8);

        log.debug("Mensaje recibido en M5 con routingKey {}: {}", routingKey, cuerpo);

        try {
            JsonNode json = objectMapper.readTree(cuerpo);

            if (RabbitConfig.ROUTING_M4_MERCADO_ABIERTO.equals(routingKey)) {
                procesarAperturaMercado();
            } else if (RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS.equals(routingKey)) {
                procesarConfirmacionFondos(json);
            } else if (RabbitConfig.ROUTING_M6_POSICIONES_ACTUALIZADAS.equals(routingKey)) {
                procesarConfirmacionPortafolio(json);
            }
        } catch (Exception ex) {
            log.error("Error al procesar mensaje con routingKey {}: {}", routingKey, ex.getMessage(), ex);
            if (RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS.equals(routingKey)
                    || RabbitConfig.ROUTING_M6_POSICIONES_ACTUALIZADAS.equals(routingKey)) {
                // El acuse puede adelantar el commit de la ejecución de M5. Usar el reintento configurado y la DLQ.
                throw new IllegalStateException("No se pudo registrar la confirmación de ejecución", ex);
            }
        }
    }

    private void procesarAperturaMercado() {
        // SAD RF-13: procesar ordenes encoladas y confirmadas en la siguiente apertura
        List<Orden> encoladas = ordenRepository.findEncoladasListasParaEnvio();
        log.info("Procesando apertura de mercado. {} órdenes encoladas encontradas.", encoladas.size());

        for (Orden orden : encoladas) {
            try {
                if (orden.getLadoOrden() == LadoOrden.COMPRA) {
                    BigDecimal montoReserva = orden.getPrecioEstimado().multiply(BigDecimal.valueOf(orden.getCantidad()));
                    Long reservaFondosId = fondosClient.reservarFondos(
                            String.valueOf(orden.getId()),
                            orden.getUsuarioId(),
                            montoReserva,
                            "RES:" + orden.getClientOrderId()
                    );
                    orden.setReservaFondosId(reservaFondosId);
                } else {
                    Long reservaPosicionId = portafolioClient.reservarPosicion(
                            String.valueOf(orden.getId()),
                            orden.getUsuarioId(),
                            orden.getTicker(),
                            orden.getCantidad(),
                            "RES:" + orden.getClientOrderId()
                    );
                    orden.setReservaPosicionId(reservaPosicionId);
                }

                AlpacaOrderRequest req = new AlpacaOrderRequest(
                        orden.getTicker(),
                        orden.getCantidad(),
                        orden.getLadoOrden(),
                        orden.getTipoOrden(),
                        orden.getVigenciaOrden(),
                        orden.getPrecioLimite(),
                        orden.getPrecioActivacion(),
                        orden.getClientOrderId()
                );

                AlpacaOrderResponse resp = alpacaOrderPort.enviarOrden(req);
                orden.setBrokerOrderId(resp.id());
                orden.setEstadoOrden(EstadoOrden.ENVIADA);
                orden.setActualizadaEn(Instant.now());
                ordenRepository.save(orden);

                historialRepository.save(HistorialEstadoOrden.registrar(
                        orden.getId(), EstadoOrden.ENVIADA, TipoActor.SISTEMA, null,
                        resp.id().startsWith(AlpacaOrderPort.PREFIJO_PENDIENTE)
                                ? "Envío de la orden encolada pendiente de confirmación de Alpaca; se mantienen las reservas"
                                : "Apertura de mercado: orden encolada enviada al broker con brokerOrderId: " + resp.id()));

                publicador.publicarOrdenCreada(
                        UUID.randomUUID(),
                        orden.getId(),
                        orden.getUsuarioId(),
                        orden.getTicker(),
                        orden.getLadoOrden().name(),
                        orden.getCantidad(),
                        orden.getTipoOrden().name()
                );
            } catch (Exception ex) {
                log.error("Error al procesar orden encolada {}: {}", orden.getId(), ex.getMessage());
            }
        }
    }

    private void procesarConfirmacionFondos(JsonNode json) {
        registrarConfirmacion(payload(json), ServicioParticipante.FONDOS, null);
    }

    private void procesarConfirmacionPortafolio(JsonNode json) {
        JsonNode datos = payload(json);
        if (datos.has("executionId") && datos.has("averageCost")) {
            registrarConfirmacion(datos, ServicioParticipante.PORTAFOLIO,
                    new BigDecimal(datos.get("averageCost").asText()));
        }
    }

    private static JsonNode payload(JsonNode json) {
        return json.has("payload") ? json.get("payload") : json;
    }

    private void registrarConfirmacion(JsonNode datos, ServicioParticipante servicio, BigDecimal costoPromedio) {
        if (!datos.has("executionId")) {
            return;
        }
        String brokerExecutionId = datos.get("executionId").asText();
        var ejecucion = ejecucionRepository.findByBrokerExecutionId(brokerExecutionId)
                .orElseThrow(() -> new IllegalStateException("La ejecución " + brokerExecutionId + " aún no está registrada en M5"));
        if (confirmacionRepository.findByEjecucionIdAndServicio(ejecucion.getId(), servicio).isPresent()) {
            return;
        }
        confirmacionRepository.save(new ConfirmacionEjecucion(ejecucion.getId(), servicio, costoPromedio, Instant.now()));
    }
}
