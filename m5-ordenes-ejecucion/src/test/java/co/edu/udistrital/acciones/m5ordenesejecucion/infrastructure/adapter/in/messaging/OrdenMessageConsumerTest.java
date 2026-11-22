package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.in.messaging;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.FondosClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.HistorialEstadoOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PortafolioClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config.RabbitConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrdenMessageConsumerTest {

    @Mock
    OrdenRepositoryPort ordenRepository;

    @Mock
    HistorialEstadoOrdenRepositoryPort historialRepository;

    @Mock
    ConfirmacionEjecucionRepositoryPort confirmacionRepository;

    @Mock
    FondosClientPort fondosClient;

    @Mock
    PortafolioClientPort portafolioClient;

    @Mock
    AlpacaOrderPort alpacaOrderPort;

    @Mock
    PublicadorOrdenesPort publicador;

    @Mock
    ObjectMapper objectMapper;

    private OrdenMessageConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new OrdenMessageConsumer(
                ordenRepository, historialRepository, confirmacionRepository,
                fondosClient, portafolioClient, alpacaOrderPort, publicador, objectMapper
        );
    }

    @Test
    void recibirMensajeRoutingKeyMercadoAbierto() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M4_MERCADO_ABIERTO;
        String cuerpo = "{}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);
        when(ordenRepository.findEncoladasListasParaEnvio()).thenReturn(List.of());

        Message mensaje = crearMensaje(routingKey, cuerpo);

        consumer.recibirMensaje(mensaje);

        verify(ordenRepository).findEncoladasListasParaEnvio();
    }

    @Test
    void recibirMensajeRoutingKeyFondosConfirmados() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS;
        String cuerpo = "{\"executionId\": 42}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);
        when(jsonNode.has("executionId")).thenReturn(true);
        when(jsonNode.get("executionId")).thenReturn(mock(JsonNode.class));
        when(jsonNode.get("executionId").asLong()).thenReturn(42L);

        Message mensaje = crearMensaje(routingKey, cuerpo);

        consumer.recibirMensaje(mensaje);

        ArgumentCaptor<ConfirmacionEjecucion> captor = ArgumentCaptor.forClass(ConfirmacionEjecucion.class);
        verify(confirmacionRepository).save(captor.capture());
        assertEquals(42L, captor.getValue().getEjecucionOrdenId());
        assertEquals(ServicioParticipante.FONDOS, captor.getValue().getServicioParticipante());
        assertNull(captor.getValue().getPrecioPromedioCosto());
    }

    @Test
    void recibirMensajeRoutingKeyPortafolio() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M6_POSICIONES_ACTUALIZADAS;
        String cuerpo = "{\"executionId\": 99, \"averageCost\": \"150.25\"}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);
        when(jsonNode.has("executionId")).thenReturn(true);
        when(jsonNode.has("averageCost")).thenReturn(true);
        when(jsonNode.get("executionId")).thenReturn(mock(JsonNode.class));
        when(jsonNode.get("executionId").asLong()).thenReturn(99L);
        when(jsonNode.get("averageCost")).thenReturn(mock(JsonNode.class));
        when(jsonNode.get("averageCost").asText()).thenReturn("150.25");

        Message mensaje = crearMensaje(routingKey, cuerpo);

        consumer.recibirMensaje(mensaje);

        ArgumentCaptor<ConfirmacionEjecucion> captor = ArgumentCaptor.forClass(ConfirmacionEjecucion.class);
        verify(confirmacionRepository).save(captor.capture());
        assertEquals(99L, captor.getValue().getEjecucionOrdenId());
        assertEquals(ServicioParticipante.PORTAFOLIO, captor.getValue().getServicioParticipante());
        assertEquals(new BigDecimal("150.25"), captor.getValue().getPrecioPromedioCosto());
    }

    @Test
    void recibirMensajeExcepcionRoutingKeyDesconocida() throws Exception {
        String routingKey = "routing.desconocida";
        String cuerpo = "{}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);

        Message mensaje = crearMensaje(routingKey, cuerpo);

        assertDoesNotThrow(() -> consumer.recibirMensaje(mensaje));
        verifyNoInteractions(ordenRepository, confirmacionRepository, fondosClient, portafolioClient, alpacaOrderPort, publicador);
    }

    @Test
    void recibirMensajeExcepcionJsonInvalido() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M4_MERCADO_ABIERTO;
        String cuerpo = "{json invalido";
        when(objectMapper.readTree(cuerpo)).thenThrow(new RuntimeException("JSON inválido"));

        Message mensaje = crearMensaje(routingKey, cuerpo);

        assertDoesNotThrow(() -> consumer.recibirMensaje(mensaje));
    }

    @Test
    void procesarAperturaMercadoCompra() throws Exception {
        Orden orden = crearOrdenCompra();
        when(ordenRepository.findEncoladasListasParaEnvio()).thenReturn(List.of(orden));
        when(fondosClient.reservarFondos(anyString(), anyLong(), any(), anyString())).thenReturn(100L);
        when(alpacaOrderPort.enviarOrden(any())).thenReturn(new AlpacaOrderPort.AlpacaOrderResponse("broker-123", null, null, null, null, null, null));

        consumer.recibirMensaje(crearMensaje(RabbitConfig.ROUTING_M4_MERCADO_ABIERTO, "{}"));

        verify(fondosClient).reservarFondos(eq("1"), eq(1L), any(), anyString());
        assertEquals(100L, orden.getReservaFondosId());
        verify(alpacaOrderPort).enviarOrden(any());
        verify(ordenRepository).save(orden);
        verify(publicador).publicarOrdenCreada(eq(1L), eq(1L), eq("AAPL"), eq("COMPRA"), eq(10), eq("MARKET"));
    }

    @Test
    void procesarAperturaMercadoVenta() throws Exception {
        Orden orden = crearOrdenVenta();
        when(ordenRepository.findEncoladasListasParaEnvio()).thenReturn(List.of(orden));
        when(portafolioClient.reservarPosicion(anyString(), anyLong(), anyString(), anyInt(), anyString())).thenReturn(200L);
        when(alpacaOrderPort.enviarOrden(any())).thenReturn(new AlpacaOrderPort.AlpacaOrderResponse("broker-456", null, null, null, null, null, null));

        consumer.recibirMensaje(crearMensaje(RabbitConfig.ROUTING_M4_MERCADO_ABIERTO, "{}"));

        verify(portafolioClient).reservarPosicion(eq("2"), eq(2L), eq("TSLA"), eq(5), anyString());
        assertEquals(200L, orden.getReservaPosicionId());
        verify(alpacaOrderPort).enviarOrden(any());
        verify(ordenRepository).save(orden);
        verify(publicador).publicarOrdenCreada(eq(2L), eq(2L), eq("TSLA"), eq("VENTA"), eq(5), eq("LIMIT"));
    }

    private Message crearMensaje(String routingKey, String cuerpo) {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(routingKey);
        return new Message(cuerpo.getBytes(), props);
    }

    private Orden crearOrdenCompra() {
        Orden orden = new Orden();
        orden.setId(1L);
        orden.setUsuarioId(1L);
        orden.setAccionId(1L);
        orden.setTicker("AAPL");
        orden.setTipoOrden(TipoOrden.MARKET);
        orden.setLadoOrden(LadoOrden.COMPRA);
        orden.setVigenciaOrden(VigenciaOrden.DIA);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setConfiguracionComisionId(1L);
        orden.setCantidad(10);
        orden.setPrecioEstimado(new BigDecimal("150.00"));
        orden.setClientOrderId(UUID.randomUUID());
        return orden;
    }

    private Orden crearOrdenVenta() {
        Orden orden = new Orden();
        orden.setId(2L);
        orden.setUsuarioId(2L);
        orden.setAccionId(2L);
        orden.setTicker("TSLA");
        orden.setTipoOrden(TipoOrden.LIMIT);
        orden.setLadoOrden(LadoOrden.VENTA);
        orden.setVigenciaOrden(VigenciaOrden.DIA);
        orden.setEstadoOrden(EstadoOrden.PENDIENTE);
        orden.setConfiguracionComisionId(1L);
        orden.setCantidad(5);
        orden.setPrecioEstimado(new BigDecimal("250.00"));
        orden.setClientOrderId(UUID.randomUUID());
        return orden;
    }

    @Test
    void procesarConfirmacionFondosJsonInvalidoCatchIgnorado() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS;
        String cuerpo = "{\"executionId\": \"no-es-numero\"}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);
        when(jsonNode.has("executionId")).thenReturn(true);
        JsonNode executionIdNode = mock(JsonNode.class);
        when(jsonNode.get("executionId")).thenReturn(executionIdNode);
        when(executionIdNode.asLong()).thenThrow(new NumberFormatException());

        Message mensaje = crearMensaje(routingKey, cuerpo);

        assertDoesNotThrow(() -> consumer.recibirMensaje(mensaje));
        verify(confirmacionRepository, never()).save(any());
    }

    @Test
    void procesarConfirmacionFondosSinExecutionIdNoGuarda() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS;
        String cuerpo = "{}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);
        when(jsonNode.has("executionId")).thenReturn(false);

        Message mensaje = crearMensaje(routingKey, cuerpo);

        consumer.recibirMensaje(mensaje);

        verify(confirmacionRepository, never()).save(any());
    }

    @Test
    void procesarConfirmacionPortafolioSinAverageCostNoGuarda() throws Exception {
        String routingKey = RabbitConfig.ROUTING_M6_POSICIONES_ACTUALIZADAS;
        String cuerpo = "{\"executionId\": 99}";
        JsonNode jsonNode = mock(JsonNode.class);
        when(objectMapper.readTree(cuerpo)).thenReturn(jsonNode);
        when(jsonNode.has("executionId")).thenReturn(true);
        when(jsonNode.has("averageCost")).thenReturn(false);

        Message mensaje = crearMensaje(routingKey, cuerpo);

        consumer.recibirMensaje(mensaje);

        verify(confirmacionRepository, never()).save(any());
    }

    @Test
    void procesarAperturaMercadoExcepcionEnUnaOrdenContinuaConSiguiente() throws Exception {
        Orden ordenOk = crearOrdenCompra();
        ordenOk.setId(1L);
        ordenOk.setClientOrderId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

        Orden ordenFalla = crearOrdenCompra();
        ordenFalla.setId(2L);
        ordenFalla.setClientOrderId(UUID.fromString("22222222-2222-2222-2222-222222222222"));

        when(ordenRepository.findEncoladasListasParaEnvio()).thenReturn(List.of(ordenOk, ordenFalla));
        when(fondosClient.reservarFondos(eq("1"), eq(1L), any(), anyString())).thenReturn(100L);
        when(fondosClient.reservarFondos(eq("2"), eq(1L), any(), anyString())).thenThrow(new RuntimeException("Error fondos"));
        when(alpacaOrderPort.enviarOrden(any())).thenReturn(new AlpacaOrderPort.AlpacaOrderResponse("broker-123", null, null, null, null, null, null));

        consumer.recibirMensaje(crearMensaje(RabbitConfig.ROUTING_M4_MERCADO_ABIERTO, "{}"));

        verify(fondosClient).reservarFondos(eq("1"), eq(1L), any(), anyString());
        verify(fondosClient).reservarFondos(eq("2"), eq(1L), any(), anyString());
        verify(alpacaOrderPort).enviarOrden(any());
        verify(ordenRepository).save(ordenOk);
        verify(ordenRepository, never()).save(ordenFalla);
        verify(publicador).publicarOrdenCreada(eq(1L), eq(1L), eq("AAPL"), eq("COMPRA"), eq(10), eq("MARKET"));
        verify(publicador, never()).publicarOrdenCreada(eq(2L), anyLong(), anyString(), anyString(), anyInt(), anyString());
    }

    @Test
    void procesarAperturaMercadoCompraConReservaFondosIdYaSeteada() throws Exception {
        Orden orden = crearOrdenCompra();
        orden.setReservaFondosId(999L);
        when(ordenRepository.findEncoladasListasParaEnvio()).thenReturn(List.of(orden));
        when(fondosClient.reservarFondos(anyString(), anyLong(), any(), anyString())).thenReturn(100L);
        when(alpacaOrderPort.enviarOrden(any())).thenReturn(new AlpacaOrderPort.AlpacaOrderResponse("broker-123", null, null, null, null, null, null));

        consumer.recibirMensaje(crearMensaje(RabbitConfig.ROUTING_M4_MERCADO_ABIERTO, "{}"));

        verify(fondosClient).reservarFondos(eq("1"), eq(1L), any(), anyString());
        assertEquals(100L, orden.getReservaFondosId());
        verify(alpacaOrderPort).enviarOrden(any());
        verify(ordenRepository).save(orden);
    }
}