package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.Cotizacion;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EvaluacionPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.WatchlistItemResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.CotizacionClientPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultarWatchlistUseCaseTest {

    private static final long USUARIO = 99101L;
    private static final Instant INICIO = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant FIN = Instant.parse("2027-11-22T00:00:00Z");

    @Mock
    private WatchlistRepositoryPort repositorio;

    @Mock
    private EvaluadorPremium evaluador;

    @Mock
    private CotizacionClientPort cotizaciones;

    private ConsultarWatchlistUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new ConsultarWatchlistUseCase(repositorio, evaluador, cotizaciones,
                Clock.fixed(INICIO, ZoneOffset.UTC));
    }

    @Test
    void sinPremiumResponde403() {
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(false, null));

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO))
                .isInstanceOf(PremiumRequeridoException.class);
        verifyNoInteractions(repositorio, cotizaciones);
    }

    @Test
    void sinItemsNoLlamaAM4() {
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(true, FIN));
        when(repositorio.findByUserId(USUARIO)).thenReturn(List.of());

        assertThat(casoDeUso.ejecutar(USUARIO)).isEmpty();
        verifyNoInteractions(cotizaciones);
    }

    @Test
    void devuelveLosItemsConElUltimoPrecio() {
        WatchlistItem item = WatchlistItem.crear(USUARIO, Simbolo.desde("aapl"), INICIO);
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(true, FIN));
        when(repositorio.findByUserId(USUARIO)).thenReturn(List.of(item));
        when(cotizaciones.obtenerCotizaciones(Set.of("AAPL"))).thenReturn(Map.of("AAPL", cotizacion()));

        List<WatchlistItemResponse> respuesta = casoDeUso.ejecutar(USUARIO);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.getFirst().symbol()).isEqualTo("AAPL");
        assertThat(respuesta.getFirst().price()).isEqualByComparingTo("200.00");
        assertThat(respuesta.getFirst().priceTimestamp()).isEqualTo(INICIO);
        assertThat(respuesta.getFirst().stale()).isFalse();
    }

    @Test
    void siM4NoRespondeLosPreciosSonNulosYStaleEsVerdadero() {
        WatchlistItem item = WatchlistItem.crear(USUARIO, Simbolo.desde("AAPL"), INICIO);
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(true, FIN));
        when(repositorio.findByUserId(USUARIO)).thenReturn(List.of(item));
        when(cotizaciones.obtenerCotizaciones(any()))
                .thenThrow(new DatosMercadoNoDisponibleException(new RuntimeException("caida")));

        List<WatchlistItemResponse> respuesta = casoDeUso.ejecutar(USUARIO);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.getFirst().price()).isNull();
        assertThat(respuesta.getFirst().stale()).isTrue();
    }

    @Test
    void consultaElPremiumConElRelojInyectado() {
        when(evaluador.evaluar(eq(USUARIO), any())).thenReturn(new EvaluacionPremium(false, null));

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO))
                .isInstanceOf(PremiumRequeridoException.class);
    }

    private static Cotizacion cotizacion() {
        return new Cotizacion("AAPL", new BigDecimal("200.00"),
                new BigDecimal("1.00"), new BigDecimal("0.50"), INICIO);
    }
}
