package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort.AlpacaOrderRequest;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort.AlpacaOrderResponse;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OutboxEventoRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.HistorialEstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.OutboxEvento;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CrearOrdenUseCaseImpl implements CrearOrdenUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final ConfiguracionComisionRepositoryPort comisionRepository;
    private final HistorialEstadoOrdenRepositoryPort historialRepository;
    private final OutboxEventoRepositoryPort outboxRepository;
    private final FondosClientPort fondosClient;
    private final PortafolioClientPort portafolioClient;
    private final DatosMercadoClientPort datosMercadoClient;
    private final AlpacaOrderPort alpacaOrderPort;
    private final PublicadorOrdenesPort publicador;
    private final ObjectMapper objectMapper;

    public CrearOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                  ConfiguracionComisionRepositoryPort comisionRepository,
                                  HistorialEstadoOrdenRepositoryPort historialRepository,
                                  OutboxEventoRepositoryPort outboxRepository,
                                  FondosClientPort fondosClient,
                                  PortafolioClientPort portafolioClient,
                                  DatosMercadoClientPort datosMercadoClient,
                                  AlpacaOrderPort alpacaOrderPort,
                                  PublicadorOrdenesPort publicador,
                                  ObjectMapper objectMapper) {
        this.ordenRepository = ordenRepository;
        this.comisionRepository = comisionRepository;
        this.historialRepository = historialRepository;
        this.outboxRepository = outboxRepository;
        this.fondosClient = fondosClient;
        this.portafolioClient = portafolioClient;
        this.datosMercadoClient = datosMercadoClient;
        this.alpacaOrderPort = alpacaOrderPort;
        this.publicador = publicador;
        this.objectMapper = objectMapper;
    }

    @Override
    public Orden ejecutar(CrearOrdenCommand comando) {
        // 1. Idempotencia: si clientOrderId ya existe, retornar orden existente
        UUID clientOrderId = comando.clientOrderId() != null ? comando.clientOrderId() : UUID.randomUUID();
        Optional<Orden> existente = ordenRepository.findByClientOrderId(clientOrderId);
        if (existente.isPresent()) {
            return existente.get();
        }

        // 2. Validaciones de negocio según tipo de orden (SAD CK_ORDEN_PRECIOS_SEGUN_TIPO)
        validarPreciosSegunTipo(comando.tipoOrden(), comando.precioLimite(), comando.precioActivacion());

        // 3. Obtener configuración de comisión vigente (mercado por defecto NYSE o derivado)
        String mercado = "NYSE";
        ConfiguracionComision comisionConfig = comisionRepository.findVigentePorMercado(mercado, Instant.now())
                .orElseGet(() -> comisionRepository.findVigentes(Instant.now()).stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("No hay configuración de comisión activa para el mercado: " + mercado)));

        // 4. Calcular precio estimado y comisión
        BigDecimal precioEstimado = comando.precioEstimado();
        if (precioEstimado == null || precioEstimado.compareTo(BigDecimal.ZERO) <= 0) {
            precioEstimado = comando.precioLimite() != null ? comando.precioLimite() : datosMercadoClient.obtenerPrecioEstimado(comando.ticker());
        }
        if (precioEstimado == null || precioEstimado.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El precio estimado debe ser mayor a cero");
        }

        BigDecimal montoBruto = precioEstimado.multiply(BigDecimal.valueOf(comando.cantidad()));
        BigDecimal comision = montoBruto.multiply(comisionConfig.getPorcentaje()).setScale(2, RoundingMode.HALF_UP);

        // 5. Verificar horario de mercado (SAD RF-13)
        boolean mercadoAbierto = datosMercadoClient.esMercadoAbierto(mercado);

        Orden orden = new Orden();
        orden.setUsuarioId(comando.usuarioId());
        orden.setAccionId(comando.accionId() != null ? comando.accionId() : 1L);
        orden.setTicker(comando.ticker().toUpperCase());
        orden.setTipoOrden(comando.tipoOrden());
        orden.setLadoOrden(comando.ladoOrden());
        orden.setVigenciaOrden(comando.vigenciaOrden());
        orden.setConfiguracionComisionId(comisionConfig.getId());
        orden.setCantidad(comando.cantidad());
        orden.setPrecioLimite(comando.precioLimite());
        orden.setPrecioActivacion(comando.precioActivacion());
        orden.setPrecioEstimado(precioEstimado);
        orden.setClientOrderId(clientOrderId);
        orden.setCreadaEn(Instant.now());
        orden.setActualizadaEn(Instant.now());

        if (!mercadoAbierto) {
            // Mercado cerrado: requiere encolamiento
            orden.setEstadoOrden(EstadoOrden.PENDIENTE);
            orden.setRequiereEncolamiento(true);
            orden = ordenRepository.save(orden);

            historialRepository.save(HistorialEstadoOrden.registrar(
                    orden.getId(), EstadoOrden.PENDIENTE, TipoActor.SISTEMA, null,
                    "Mercado cerrado. Orden creada requiriendo confirmación de encolamiento"));

            registrarOutbox(orden, "OrdenCreada");
            return orden;
        }

        // Mercado abierto: procesar reservas y enviar al broker
        orden.setRequiereEncolamiento(false);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden = ordenRepository.save(orden);

        historialRepository.save(HistorialEstadoOrden.registrar(
                orden.getId(), EstadoOrden.PENDIENTE, TipoActor.USUARIO, comando.usuarioId(),
                "Orden creada por usuario"));

        try {
            if (comando.ladoOrden() == LadoOrden.COMPRA) {
                BigDecimal montoReserva = montoBruto.add(comision);
                Long reservaFondosId = fondosClient.reservarFondos(
                        String.valueOf(orden.getId()),
                        comando.usuarioId(),
                        montoReserva,
                        "RES:" + clientOrderId
                );
                orden.setReservaFondosId(reservaFondosId);
            } else {
                Long reservaPosicionId = portafolioClient.reservarPosicion(
                        String.valueOf(orden.getId()),
                        comando.usuarioId(),
                        orden.getTicker(),
                        orden.getCantidad(),
                        "RES:" + clientOrderId
                );
                orden.setReservaPosicionId(reservaPosicionId);
            }

            // Enviar orden a Alpaca
            AlpacaOrderRequest alpacaReq = new AlpacaOrderRequest(
                    orden.getTicker(),
                    orden.getCantidad(),
                    orden.getLadoOrden(),
                    orden.getTipoOrden(),
                    orden.getVigenciaOrden(),
                    orden.getPrecioLimite(),
                    orden.getPrecioActivacion(),
                    orden.getClientOrderId()
            );

            AlpacaOrderResponse alpacaResp = alpacaOrderPort.enviarOrden(alpacaReq);
            orden.setBrokerOrderId(alpacaResp.id());
            orden.setEstadoOrden(EstadoOrden.ENVIADA);
            orden.setActualizadaEn(Instant.now());

            historialRepository.save(HistorialEstadoOrden.registrar(
                    orden.getId(), EstadoOrden.ENVIADA, TipoActor.SISTEMA, null,
                    "Reserva completada y enviada a Alpaca Paper Trading con brokerOrderId: " + alpacaResp.id()));

            registrarOutbox(orden, "OrdenCreada");

            publicador.publicarOrdenCreada(
                    orden.getId(),
                    orden.getUsuarioId(),
                    orden.getTicker(),
                    orden.getLadoOrden().name(),
                    orden.getCantidad(),
                    orden.getTipoOrden().name()
            );

        } catch (Exception ex) {
            orden.setEstadoOrden(EstadoOrden.RECHAZADA);
            orden.setActualizadaEn(Instant.now());
            historialRepository.save(HistorialEstadoOrden.registrar(
                    orden.getId(), EstadoOrden.RECHAZADA, TipoActor.SISTEMA, null,
                    "Rechazo en procesamiento: " + ex.getMessage()));

            publicador.publicarOrdenRechazada(orden.getId(), "Error al reservar recursos o enviar al broker", ex.getMessage());
        }

        return ordenRepository.save(orden);
    }

    private void validarPreciosSegunTipo(TipoOrden tipo, BigDecimal precioLimite, BigDecimal precioActivacion) {
        if (tipo == TipoOrden.MARKET) {
            if (precioLimite != null || precioActivacion != null) {
                throw new IllegalArgumentException("Orden MARKET no debe incluir precioLimite ni precioActivacion");
            }
        } else if (tipo == TipoOrden.LIMIT) {
            if (precioLimite == null || precioLimite.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Orden LIMIT requiere precioLimite mayor a cero");
            }
            if (precioActivacion != null) {
                throw new IllegalArgumentException("Orden LIMIT no debe incluir precioActivacion");
            }
        } else if (tipo == TipoOrden.STOP_LOSS || tipo == TipoOrden.TAKE_PROFIT) {
            if (precioActivacion == null || precioActivacion.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Orden " + tipo + " requiere precioActivacion mayor a cero");
            }
        }
    }

    private void registrarOutbox(Orden orden, String tipoEvento) {
        try {
            Map<String, Object> carga = Map.of(
                    "eventId", UUID.randomUUID().toString(),
                    "orderId", orden.getId(),
                    "userId", orden.getUsuarioId(),
                    "symbol", orden.getTicker(),
                    "side", orden.getLadoOrden().name(),
                    "qty", orden.getCantidad(),
                    "type", orden.getTipoOrden().name(),
                    "status", orden.getEstadoOrden().name()
            );
            String json = objectMapper.writeValueAsString(carga);
            outboxRepository.save(OutboxEvento.crear("ORDEN", String.valueOf(orden.getId()), tipoEvento, json));
        } catch (Exception ignored) {
        }
    }
}
