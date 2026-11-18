package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPortafolioUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ConsultarPosicionUseCase;
import co.edu.udistrital.acciones.m6portafolio.application.usecase.ListarPosicionesUseCase;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultasPortafolioUseCaseTest {

    private static final long USUARIO = 4101L;
    private static final long AJENO = 9999L;
    private static final Instant T0 = Instant.parse("2026-01-02T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-02T12:00:00Z"), ZoneOffset.UTC);

    @Mock
    private PosicionRepositoryPort posiciones;

    private final ValoracionService valoracion = new ValoracionService(CLOCK);
    private ConsultarPosicionUseCase consultarPosicion;
    private ListarPosicionesUseCase listarPosiciones;
    private ConsultarPortafolioUseCase consultarPortafolio;

    @BeforeEach
    void prepararCasos() {
        consultarPosicion = new ConsultarPosicionUseCase(posiciones, valoracion);
        listarPosiciones = new ListarPosicionesUseCase(posiciones, valoracion);
        consultarPortafolio = new ConsultarPortafolioUseCase(posiciones, valoracion);
    }

    @Test
    void elPropietarioConsultaSuPosicionNormalizandoElSimbolo() {
        Posicion posicion = posicionConCompra("AAPL", "10", "180.00");
        when(posiciones.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));

        PosicionResponse respuesta = consultarPosicion.ejecutar(USUARIO, "aapl",
                new Actor(USUARIO, false, false));

        assertThat(respuesta.symbol()).isEqualTo("AAPL");
        assertThat(respuesta.quantity()).isEqualByComparingTo("10");
    }

    @Test
    void unUsuarioAjenoNoConsultaLaPosicion() {
        Actor actor = new Actor(AJENO, false, false);

        assertThatThrownBy(() -> consultarPosicion.ejecutar(USUARIO, "AAPL", actor))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void unAdministradorConsultaLaPosicionDeOtroUsuario() {
        Posicion posicion = posicionConCompra("AAPL", "10", "180.00");
        when(posiciones.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.of(posicion));

        PosicionResponse respuesta = consultarPosicion.ejecutar(USUARIO, "AAPL",
                new Actor(AJENO, true, false));

        assertThat(respuesta.userId()).isEqualTo(USUARIO);
    }

    @Test
    void unaPosicionInexistenteRespondeNoEncontrado() {
        when(posiciones.findByUserIdAndSymbol(USUARIO, "AAPL")).thenReturn(Optional.empty());
        Actor actor = new Actor(USUARIO, false, false);

        assertThatThrownBy(() -> consultarPosicion.ejecutar(USUARIO, "AAPL", actor))
                .isInstanceOf(RecursoNoEncontradoException.class);
    }

    @Test
    void listaSoloLasPosicionesAbiertas() {
        when(posiciones.findAbiertasByUserId(USUARIO))
                .thenReturn(List.of(posicionConCompra("AAPL", "10", "180.00")));

        List<PosicionResponse> respuesta = listarPosiciones.ejecutar(USUARIO,
                new Actor(USUARIO, false, false));

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.getFirst().symbol()).isEqualTo("AAPL");
    }

    @Test
    void unUsuarioAjenoNoListaLasPosiciones() {
        Actor actor = new Actor(AJENO, false, false);

        assertThatThrownBy(() -> listarPosiciones.ejecutar(USUARIO, actor))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    @Test
    void elPortafolioExcluyeLasPosicionesEnCeroDeLaLista() {
        Posicion abierta = posicionConCompra("AAPL", "10", "180.00");
        Posicion cerrada = Posicion.abrir(USUARIO, "MSFT", T0);
        when(posiciones.findByUserId(USUARIO)).thenReturn(List.of(abierta, cerrada));

        PortafolioResponse respuesta = consultarPortafolio.ejecutar(USUARIO,
                new Actor(USUARIO, false, false));

        assertThat(respuesta.positions()).hasSize(1);
        assertThat(respuesta.positions().getFirst().symbol()).isEqualTo("AAPL");
        assertThat(respuesta.totalValue()).isEqualByComparingTo("1800.0000");
    }

    @Test
    void unUsuarioAjenoNoConsultaElPortafolio() {
        Actor actor = new Actor(AJENO, false, false);

        assertThatThrownBy(() -> consultarPortafolio.ejecutar(USUARIO, actor))
                .isInstanceOf(AccesoDenegadoException.class);
    }

    private static Posicion posicionConCompra(String symbol, String cantidad, String precio) {
        Posicion posicion = Posicion.abrir(USUARIO, symbol, T0);
        posicion.comprar(new BigDecimal(cantidad), new BigDecimal(precio), T0);
        return posicion;
    }
}
