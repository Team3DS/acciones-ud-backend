package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.exception.AccesoDenegadoException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ActorTest {

    private static final long PROPIETARIO = 4101L;
    private static final long AJENO = 9999L;

    @Test
    void elActorInternoConsultaYOperaCualquierPortafolio() {
        Actor interno = Actor.interno();

        assertThat(interno.sistema()).isTrue();
        assertThat(interno.puedeVer(PROPIETARIO)).isTrue();
        assertThat(interno.puedeOperar(PROPIETARIO)).isTrue();
    }

    @Test
    void elAdministradorConsultaPortafoliosAjenosPeroNoOperaEnEllos() {
        Actor administrador = new Actor(7002L, true, false);

        assertThat(administrador.puedeVer(AJENO)).isTrue();
        assertThat(administrador.puedeOperar(AJENO)).isFalse();
    }

    @Test
    void elPropietarioConsultaYOperaSobreSuPortafolio() {
        Actor dueno = new Actor(PROPIETARIO, false, false);

        assertThat(dueno.puedeVer(PROPIETARIO)).isTrue();
        assertThat(dueno.puedeOperar(PROPIETARIO)).isTrue();
    }

    @Test
    void unUsuarioAjenoNoConsultaNiOperaSobreOtroPortafolio() {
        Actor otro = new Actor(5555L, false, false);

        assertThat(otro.puedeVer(PROPIETARIO)).isFalse();
        assertThat(otro.puedeOperar(PROPIETARIO)).isFalse();
    }

    @Test
    void unAdministradorSinIdentidadSoloConsulta() {
        Actor administradorTecnico = new Actor(null, true, false);

        assertThat(administradorTecnico.puedeVer(AJENO)).isTrue();
        assertThat(administradorTecnico.puedeOperar(AJENO)).isFalse();
    }

    @Test
    void verificarLecturaPermiteAlPropietarioYAlAdministrador() {
        assertThatCode(() -> new Actor(PROPIETARIO, false, false).verificarLectura(PROPIETARIO))
                .doesNotThrowAnyException();
        assertThatCode(() -> new Actor(AJENO, true, false).verificarLectura(PROPIETARIO))
                .doesNotThrowAnyException();
        assertThatCode(() -> Actor.interno().verificarLectura(PROPIETARIO))
                .doesNotThrowAnyException();
    }

    @Test
    void verificarLecturaRechazaAUnUsuarioAjeno() {
        Actor otro = new Actor(AJENO, false, false);

        assertThatThrownBy(() -> otro.verificarLectura(PROPIETARIO))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("consultar");
    }

    @Test
    void verificarOperacionPermiteAlPropietarioYAlSistema() {
        assertThatCode(() -> new Actor(PROPIETARIO, false, false).verificarOperacion(PROPIETARIO))
                .doesNotThrowAnyException();
        assertThatCode(() -> Actor.interno().verificarOperacion(PROPIETARIO))
                .doesNotThrowAnyException();
    }

    @Test
    void verificarOperacionRechazaAlAdministradorYAUnUsuarioAjeno() {
        Actor administrador = new Actor(AJENO, true, false);

        assertThatThrownBy(() -> administrador.verificarOperacion(PROPIETARIO))
                .isInstanceOf(AccesoDenegadoException.class)
                .hasMessageContaining("operar");
    }
}
