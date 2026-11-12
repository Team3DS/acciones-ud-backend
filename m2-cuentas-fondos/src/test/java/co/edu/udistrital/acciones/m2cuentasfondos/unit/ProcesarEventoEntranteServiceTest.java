package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.ConfirmarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.EventoEntrante;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.LiberarReservaRequest;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenCanceladaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenEjecutadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.OrdenRechazadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.SuscripcionActivadaPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.EventoInvalidoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.EventoProcesadoPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.ProcesarEventoEntranteService;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.AcreditarVentaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.ConfirmarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.application.usecase.LiberarReservaUseCase;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.TransicionReservaInvalidaException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcesarEventoEntranteServiceTest {

    private static final Instant AHORA = Instant.parse("2026-11-11T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(AHORA, ZoneOffset.UTC);
    private static final String EVENTO_ID = "evento-1";
    private static final String ORDER_ID = "orden-1";

    @Mock
    private EventoProcesadoPort eventosProcesados;

    @Mock
    private ReservaRepositoryPort reservas;

    @Mock
    private ConfirmarReservaUseCase confirmarReserva;

    @Mock
    private LiberarReservaUseCase liberarReserva;

    @Mock
    private AcreditarVentaUseCase acreditarVenta;

    @Mock
    private PlatformTransactionManager transactionManager;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private ProcesarEventoEntranteService servicio;

    @BeforeEach
    void preparar() {
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        servicio = new ProcesarEventoEntranteService(new TransactionTemplate(transactionManager), eventosProcesados,
                reservas, confirmarReserva, liberarReserva, acreditarVenta, objectMapper, CLOCK);
    }

    @Test
    void ignoraUnEventoYaProcesado() {
        cuandoEsNuevo(false);

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_CANCELADA,
                new OrdenCanceladaPayload(ORDER_ID, "OrdenCancelada")));

        verifyNoInteractions(reservas, confirmarReserva, liberarReserva, acreditarVenta);
        verify(transactionManager).commit(any());
    }

    @Test
    void unaCompraEjecutadaConfirmaLaReserva() {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(reserva(77L)));

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_EJECUTADA, new OrdenEjecutadaPayload(ORDER_ID,
                "ejecucion-1", List.of(), new BigDecimal("290.50"), "BUY", 5L)));

        ArgumentCaptor<ConfirmarReservaRequest> captor = ArgumentCaptor.forClass(ConfirmarReservaRequest.class);
        verify(confirmarReserva).ejecutar(eq(77L), captor.capture(), eq(Actor.sistema()));
        assertThat(captor.getValue().executionId()).isEqualTo("ejecucion-1");
        assertThat(captor.getValue().amount()).isEqualByComparingTo("290.50");
        verifyNoInteractions(liberarReserva, acreditarVenta);
        verify(transactionManager).commit(any());
    }

    @Test
    void unaVentaEjecutadaConUserIdAcreditaElCredito() {
        cuandoEsNuevo(true);

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_EJECUTADA, new OrdenEjecutadaPayload(ORDER_ID,
                "ejecucion-1", List.of(), new BigDecimal("180.00"), "SELL", 5L)));

        ArgumentCaptor<OrdenEjecutadaPayload> captor = ArgumentCaptor.forClass(OrdenEjecutadaPayload.class);
        verify(acreditarVenta).ejecutar(captor.capture());
        assertThat(captor.getValue().orderId()).isEqualTo(ORDER_ID);
        assertThat(captor.getValue().totalAmount()).isEqualByComparingTo("180.00");
        verifyNoInteractions(reservas, confirmarReserva, liberarReserva);
    }

    @Test
    void unaVentaSinUserIdConfirmaLaReserva() {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(reserva(77L)));

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_EJECUTADA, new OrdenEjecutadaPayload(ORDER_ID,
                "ejecucion-1", List.of(), new BigDecimal("180.00"), "SELL", null)));

        verify(confirmarReserva).ejecutar(eq(77L), any(ConfirmarReservaRequest.class), eq(Actor.sistema()));
        verifyNoInteractions(acreditarVenta);
    }

    @Test
    void unaOrdenCanceladaLiberaLaReservaConElMotivo() {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(reserva(77L)));

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_CANCELADA,
                new OrdenCanceladaPayload(ORDER_ID, "Cancelada por el usuario")));

        ArgumentCaptor<LiberarReservaRequest> captor = ArgumentCaptor.forClass(LiberarReservaRequest.class);
        verify(liberarReserva).ejecutar(eq(77L), captor.capture(), eq(Actor.sistema()));
        assertThat(captor.getValue().reason()).isEqualTo("Cancelada por el usuario");
        verifyNoInteractions(confirmarReserva, acreditarVenta);
    }

    @Test
    void unaOrdenRechazadaSinMotivoUsaLiberacionPorDefecto() {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(reserva(77L)));

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_RECHAZADA,
                new OrdenRechazadaPayload(ORDER_ID, null, "broker caído")));

        ArgumentCaptor<LiberarReservaRequest> captor = ArgumentCaptor.forClass(LiberarReservaRequest.class);
        verify(liberarReserva).ejecutar(eq(77L), captor.capture(), eq(Actor.sistema()));
        assertThat(captor.getValue().reason()).isEqualTo("OrdenRechazada");
    }

    @Test
    void sinReservaSeDescartaElEvento() {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.empty());

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_CANCELADA,
                new OrdenCanceladaPayload(ORDER_ID, "OrdenCancelada")));

        verifyNoInteractions(confirmarReserva, liberarReserva, acreditarVenta);
        verify(transactionManager).commit(any());
    }

    @Test
    void unTipoDesconocidoSeDescarta() {
        cuandoEsNuevo(true);

        servicio.procesar(evento("m5.evento.desconocido", new OrdenCanceladaPayload(ORDER_ID, "x")));

        verifyNoInteractions(reservas, confirmarReserva, liberarReserva, acreditarVenta);
        verify(transactionManager).commit(any());
    }

    @Test
    void ignoraCamposDesconocidosEnElPayload() throws Exception {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(reserva(77L)));

        servicio.procesar(new EventoEntrante(EVENTO_ID, RoutingKeys.M5_ORDEN_EJECUTADA, AHORA, "agregado-1",
                "trace-1", objectMapper.readTree("""
                        {"orderId":"orden-1","executionId":"ejecucion-1","totalAmount":290.50,
                         "side":"BUY","futuro":"ignorado","fills":[{"precio":10.00}]}
                        """)));

        verify(confirmarReserva).ejecutar(eq(77L), any(ConfirmarReservaRequest.class), eq(Actor.sistema()));
        verifyNoInteractions(acreditarVenta);
    }

    @Test
    void unaSuscripcionSoloSeRegistraComoProcesada() {
        cuandoEsNuevo(true);

        servicio.procesar(evento(RoutingKeys.M10_SUSCRIPCION_ACTIVADA,
                new SuscripcionActivadaPayload(5L, "PREMIUM", "sub_123")));

        verifyNoInteractions(reservas, confirmarReserva, liberarReserva, acreditarVenta);
        verify(transactionManager).commit(any());
    }

    @Test
    void unaExcepcionDeNegocioHaceRollbackYRegistraElEvento() {
        cuandoEsNuevo(true);
        when(reservas.findByOrderId(ORDER_ID)).thenReturn(Optional.of(reserva(77L)));
        doThrow(new TransicionReservaInvalidaException("estado inválido"))
                .when(confirmarReserva).ejecutar(any(), any(), any());

        servicio.procesar(evento(RoutingKeys.M5_ORDEN_EJECUTADA, new OrdenEjecutadaPayload(ORDER_ID,
                "ejecucion-1", List.of(), new BigDecimal("290.50"), "BUY", 5L)));

        verify(transactionManager).rollback(any());
        verify(transactionManager).commit(any());
        verify(eventosProcesados, times(2)).registrarSiNuevo(eq(EVENTO_ID),
                eq(RoutingKeys.M5_ORDEN_EJECUTADA), any(Instant.class));
    }

    @Test
    void unErrorTecnicoRelanzaLaExcepcion() {
        cuandoEsNuevo(true);

        doThrow(new IllegalStateException("fallo técnico")).when(acreditarVenta).ejecutar(any());

        assertThatThrownBy(() -> servicio.procesar(evento(RoutingKeys.M5_ORDEN_EJECUTADA,
                new OrdenEjecutadaPayload(ORDER_ID, "ejecucion-1", List.of(), new BigDecimal("180.00"),
                        "SELL", 5L))))
                .isInstanceOf(IllegalStateException.class);

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verify(eventosProcesados, times(1)).registrarSiNuevo(eq(EVENTO_ID),
                eq(RoutingKeys.M5_ORDEN_EJECUTADA), any(Instant.class));
    }

    @Test
    void unEventoEjecutadoSinPayloadRelanzaUnEventoInvalido() {
        cuandoEsNuevo(true);

        assertThatThrownBy(() -> servicio.procesar(evento(RoutingKeys.M5_ORDEN_EJECUTADA, null)))
                .isInstanceOf(EventoInvalidoException.class)
                .hasMessageContaining("no trae payload");

        verify(transactionManager).rollback(any());
        verify(transactionManager, never()).commit(any());
        verifyNoInteractions(reservas, confirmarReserva, liberarReserva, acreditarVenta);
    }

    private void cuandoEsNuevo(boolean nuevo) {
        when(eventosProcesados.registrarSiNuevo(eq(EVENTO_ID), any(String.class), any(Instant.class)))
                .thenReturn(nuevo);
    }

    private EventoEntrante evento(String tipo, Object payload) {
        return new EventoEntrante(EVENTO_ID, tipo, AHORA, "agregado-1", "trace-1",
                objectMapper.valueToTree(payload));
    }

    private Reserva reserva(Long id) {
        Reserva reserva = Reserva.crear(1L, ORDER_ID, new BigDecimal("300.00"), "RES:key", AHORA);
        ReflectionTestUtils.setField(reserva, "id", id);
        return reserva;
    }
}
