package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EvaluacionPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoAgregado;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SolicitudWatchlist;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SimboloNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.CatalogoClientPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.AgregarAWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.SimboloInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgregarAWatchlistUseCaseTest {

    private static final long USUARIO = 99102L;
    private static final Instant INICIO = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant FIN = Instant.parse("2027-11-22T00:00:00Z");

    @Mock
    private WatchlistRepositoryPort repositorio;

    @Mock
    private EvaluadorPremium evaluador;

    @Mock
    private CatalogoClientPort catalogo;

    private AgregarAWatchlistUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new AgregarAWatchlistUseCase(repositorio, evaluador, catalogo,
                Clock.fixed(INICIO, ZoneOffset.UTC));
    }

    @Test
    void sinPremiumResponde403() {
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(false, null));

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO, new SolicitudWatchlist("AAPL")))
                .isInstanceOf(PremiumRequeridoException.class);
        verifyNoInteractions(catalogo);
    }

    @Test
    void agregaUnSimboloNormalizadoDelCatalogo() {
        premium();
        when(repositorio.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.empty());
        when(catalogo.esValido(Simbolo.desde("AAPL"))).thenReturn(true);
        when(repositorio.save(any(WatchlistItem.class)))
                .thenAnswer(invocacion -> invocacion.getArgument(0));

        ResultadoAgregado resultado = casoDeUso.ejecutar(USUARIO, new SolicitudWatchlist(" aapl "));

        assertThat(resultado.creado()).isTrue();
        assertThat(resultado.item().getSymbol()).isEqualTo("AAPL");
        assertThat(resultado.item().getAgregadoEn()).isEqualTo(INICIO);
    }

    @Test
    void repetirUnSimboloDevuelve200SinLlamarAlCatalogo() {
        WatchlistItem existente = WatchlistItem.crear(USUARIO, Simbolo.desde("AAPL"), INICIO);
        premium();
        when(repositorio.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.of(existente));

        ResultadoAgregado resultado = casoDeUso.ejecutar(USUARIO, new SolicitudWatchlist("aapl"));

        assertThat(resultado.creado()).isFalse();
        assertThat(resultado.item()).isSameAs(existente);
        verifyNoInteractions(catalogo);
        verify(repositorio, never()).save(any());
    }

    @Test
    void unSimboloFueraDelCatalogoResponde422() {
        premium();
        when(repositorio.findByUserIdAndSymbol(USUARIO, "ZZZZ")).thenReturn(Optional.empty());
        when(catalogo.esValido(Simbolo.desde("ZZZZ"))).thenReturn(false);

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO, new SolicitudWatchlist("ZZZZ")))
                .isInstanceOf(SimboloNoDisponibleException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void siM3NoRespondeNoSeGuardaNada() {
        premium();
        when(repositorio.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.empty());
        when(catalogo.esValido(Simbolo.desde("AAPL")))
                .thenThrow(new CatalogoNoDisponibleException(new RuntimeException("caida")));

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO, new SolicitudWatchlist("AAPL")))
                .isInstanceOf(CatalogoNoDisponibleException.class);
        verify(repositorio, never()).save(any());
    }

    @Test
    void unSimboloConFormatoInvalidoSeRechazaAntesDeLlamarAlCatalogo() {
        premium();

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO, new SolicitudWatchlist("@@")))
                .isInstanceOf(SimboloInvalidoException.class);
        verifyNoInteractions(catalogo);
    }

    private void premium() {
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(true, FIN));
    }
}
