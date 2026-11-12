package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ActorTest {

    @Test
    void elDuenoPuedeVerYOperarSuCuenta() {
        Actor dueno = new Actor(5L, false);

        assertThat(dueno.puedeVer(5L)).isTrue();
        assertThat(dueno.puedeOperar(5L)).isTrue();
    }

    @Test
    void unAdministradorPuedeVerPeroNoOperarCuentasAjenas() {
        Actor administrador = new Actor(1L, true);

        assertThat(administrador.puedeVer(5L)).isTrue();
        assertThat(administrador.puedeOperar(5L)).isFalse();
    }

    @Test
    void unUsuarioAjenoNoPuedeVerNiOperar() {
        Actor ajeno = new Actor(9L, false);

        assertThat(ajeno.puedeVer(5L)).isFalse();
        assertThat(ajeno.puedeOperar(5L)).isFalse();
    }

    @Test
    void elSistemaPuedeVerYOperarCualquierCuenta() {
        Actor sistema = Actor.sistema();

        assertThat(sistema.esSistema()).isTrue();
        assertThat(sistema.puedeVer(123L)).isTrue();
        assertThat(sistema.puedeOperar(123L)).isTrue();
    }
}
