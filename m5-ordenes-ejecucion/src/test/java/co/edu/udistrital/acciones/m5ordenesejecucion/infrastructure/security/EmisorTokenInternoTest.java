package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmisorTokenInternoTest {

    private static final JwtProperties PROPIEDADES = new JwtProperties("clave-de-prueba-de-al-menos-32-caracteres");
    private final EmisorTokenInterno emisor = new EmisorTokenInterno(PROPIEDADES);
    private final JwtTokenValidator validador = new JwtTokenValidator(PROPIEDADES);

    @Test
    void elTokenDeUsuarioLlevaSuIdYRolDeInversionista() {
        AuthenticatedUser usuario = validador.validar(emisor.paraUsuario(42L));

        assertThat(usuario.userId()).isEqualTo(42L);
        assertThat(usuario.roles()).contains("INVERSIONISTA");
        assertThat(usuario.esSistema()).isFalse();
    }

    @Test
    void elTokenDeSistemaLlevaElRolSistema() {
        AuthenticatedUser usuario = validador.validar(emisor.deSistema());

        assertThat(usuario.userId()).isEqualTo(Long.valueOf(EmisorTokenInterno.ID_SISTEMA));
        assertThat(usuario.esSistema()).isTrue();
    }
}
