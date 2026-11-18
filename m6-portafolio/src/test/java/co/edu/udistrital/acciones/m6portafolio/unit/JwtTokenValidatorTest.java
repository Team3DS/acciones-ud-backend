package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m6portafolio.support.TokenTestFactory;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenValidatorTest {

    private final JwtTokenValidator validador = new JwtTokenValidator(TokenTestFactory.propiedades());

    @Test
    void validaUnTokenHmac256ConVariosRoles() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.token(42, "ADMINISTRADOR", "SISTEMA"));

        assertThat(usuario.userId()).isEqualTo(42L);
        assertThat(usuario.roles()).containsExactlyInAnyOrder("ADMINISTRADOR", "SISTEMA");
        assertThat(usuario.esAdministrador()).isTrue();
        assertThat(usuario.esSistema()).isTrue();
    }

    @Test
    void validaUnTokenHmac512ComoM1() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.tokenHmac512(7, "INVERSIONISTA"));

        assertThat(usuario.userId()).isEqualTo(7L);
        assertThat(usuario.roles()).containsExactly("INVERSIONISTA");
    }

    @Test
    void eliminaElPrefijoRoleDeLosRoles() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.token(9, "ROLE_SISTEMA"));

        assertThat(usuario.roles()).containsExactly("SISTEMA");
        assertThat(usuario.esSistema()).isTrue();
    }

    @Test
    void devuelveRolesVaciosCuandoElClaimNoExiste() {
        AuthenticatedUser usuario = validador.validar(TokenTestFactory.sinRoles(9));

        assertThat(usuario.roles()).isEmpty();
    }

    @Test
    void omiteRolesQueNoSonTextoYRolesEnBlanco() {
        AuthenticatedUser usuario = validador.validar(
                TokenTestFactory.conRoles(List.of(5, " ", "ADMINISTRADOR")));

        assertThat(usuario.roles()).containsExactly("ADMINISTRADOR");
    }

    @Test
    void exigeFechaDeExpiracion() {
        String token = TokenTestFactory.sinExpiracion(1);

        assertThatThrownBy(() -> validador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expiracion");
    }

    @Test
    void rechazaUnTokenExpirado() {
        String token = TokenTestFactory.expirado(1);

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnaFirmaDeOtraClave() {
        String token = TokenTestFactory.firmaAjena(1);

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnSubjectNoNumerico() {
        String token = TokenTestFactory.conSubject("usuario-uno");

        assertThatThrownBy(() -> validador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("numerica");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "99999999999999999999"})
    void rechazaIdentidadesFueraDelRangoPermitido(String subject) {
        String token = TokenTestFactory.conSubject(subject);

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenMalformado() {
        String token = TokenTestFactory.malformado();

        assertThatThrownBy(() -> validador.validar(token)).isInstanceOf(JwtException.class);
    }
}
