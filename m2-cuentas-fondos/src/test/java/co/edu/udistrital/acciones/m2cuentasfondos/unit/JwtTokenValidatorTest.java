package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m2cuentasfondos.support.TokenTestFactory;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private final JwtTokenValidator validador = new JwtTokenValidator(TokenTestFactory.propiedades());

    @Test
    void validaUnTokenFirmadoConHmac256() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.token(42, "INVERSIONISTA", "ADMINISTRADOR"));

        assertThat(usuario.userId()).isEqualTo(42L);
        assertThat(usuario.roles()).containsExactlyInAnyOrder("INVERSIONISTA", "ADMINISTRADOR");
        assertThat(usuario.esAdministrador()).isTrue();
    }

    @Test
    void validaUnTokenFirmadoConHmac512ComoM1() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.tokenHmac512(7, "INVERSIONISTA"));

        assertThat(usuario.userId()).isEqualTo(7L);
        assertThat(usuario.roles()).containsExactly("INVERSIONISTA");
        assertThat(usuario.esAdministrador()).isFalse();
    }

    @Test
    void quitaElPrefijoRoleDeLosRoles() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.token(5, "ROLE_ADMINISTRADOR"));

        assertThat(usuario.roles()).containsExactly("ADMINISTRADOR");
        assertThat(usuario.esAdministrador()).isTrue();
    }

    @Test
    void devuelveRolesVaciosSiElClaimNoExiste() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.tokenSinRoles(5));

        assertThat(usuario.roles()).isEmpty();
    }

    @Test
    void ignoraElementosNoTextoDelClaimRoles() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.tokenConRolesNoTexto(5));

        assertThat(usuario.roles()).containsExactly("INVERSIONISTA");
    }

    @Test
    void rechazaUnTokenExpirado() {
        String token = TokenTestFactory.tokenExpirado(1);

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenConFirmaDistinta() {
        String token = TokenTestFactory.tokenConFirmaDistinta(1);

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenMalformado() {
        String token = TokenTestFactory.tokenMalformado();

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnSubjectNoNumerico() {
        String token = TokenTestFactory.tokenConSubject("usuario-uno");

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(NumberFormatException.class);
    }
}
