package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
