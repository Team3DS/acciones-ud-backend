package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EvaluacionPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.WatchlistRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.QuitarDeWatchlistUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Simbolo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.WatchlistItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuitarDeWatchlistUseCaseTest {

    private static final long USUARIO = 99103L;
    private static final Instant INICIO = Instant.parse("2026-11-22T00:00:00Z");
    private static final Instant FIN = Instant.parse("2027-11-22T00:00:00Z");

    @Mock
    private WatchlistRepositoryPort repositorio;

    @Mock
    private EvaluadorPremium evaluador;

    private QuitarDeWatchlistUseCase casoDeUso;

    @BeforeEach
    void preparar() {
        casoDeUso = new QuitarDeWatchlistUseCase(repositorio, evaluador,
                Clock.fixed(INICIO, ZoneOffset.UTC));
    }

    @Test
    void sinPremiumResponde403() {
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(false, null));

        assertThatThrownBy(() -> casoDeUso.ejecutar(USUARIO, "AAPL"))
                .isInstanceOf(PremiumRequeridoException.class);
        verify(repositorio, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void quitaUnSimboloNormalizado() {
        WatchlistItem item = WatchlistItem.crear(USUARIO, Simbolo.desde("AAPL"), INICIO);
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(true, FIN));
        when(repositorio.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.of(item));

        casoDeUso.ejecutar(USUARIO, "aapl");

        verify(repositorio).delete(item);
    }

    @Test
    void quitarUnSimboloInexistenteNoHaceNada() {
        when(evaluador.evaluar(USUARIO, INICIO)).thenReturn(new EvaluacionPremium(true, FIN));
        when(repositorio.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.empty());

        casoDeUso.ejecutar(USUARIO, "AAPL");

        verify(repositorio, never()).delete(org.mockito.ArgumentMatchers.any());
    }
}
