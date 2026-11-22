package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.UsuarioAutenticado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.VerificadorJwt;
import co.edu.udistrital.acciones.m10suscripcionespagos.support.TokenTestFactory;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VerificadorJwtTest {

    private final VerificadorJwt verificador = new VerificadorJwt(TokenTestFactory.propiedades());

    @Test
    void validaUnTokenHmac256ConVariosRoles() {
        UsuarioAutenticado usuario = verificador.validar(TokenTestFactory.token(42, "ADMINISTRADOR", "SISTEMA"));

        assertThat(usuario.userId()).isEqualTo(42L);
        assertThat(usuario.roles()).containsExactlyInAnyOrder("ADMINISTRADOR", "SISTEMA");
        assertThat(usuario.tieneRol("SISTEMA")).isTrue();
    }

    @Test
    void validaUnTokenHmac512ComoM1() {
        UsuarioAutenticado usuario = verificador.validar(TokenTestFactory.tokenHmac512(7, "INVERSIONISTA"));

        assertThat(usuario.userId()).isEqualTo(7L);
        assertThat(usuario.roles()).containsExactly("INVERSIONISTA");
    }

    @Test
    void eliminaElPrefijoRoleDeLosRoles() {
        UsuarioAutenticado usuario = verificador.validar(TokenTestFactory.token(9, "ROLE_SISTEMA"));

        assertThat(usuario.roles()).containsExactly("SISTEMA");
    }

    @Test
    void devuelveRolesVaciosCuandoElClaimNoExiste() {
        UsuarioAutenticado usuario = verificador.validar(TokenTestFactory.sinRoles(9));

        assertThat(usuario.roles()).isEmpty();
    }

    @Test
    void omiteRolesQueNoSonTextoYRolesEnBlanco() {
        UsuarioAutenticado usuario = verificador.validar(
                TokenTestFactory.conRoles(List.of(5, " ", "ADMINISTRADOR")));

        assertThat(usuario.roles()).containsExactly("ADMINISTRADOR");
    }

    @Test
    void exigeFechaDeExpiracion() {
        String token = TokenTestFactory.sinExpiracion(1);

        assertThatThrownBy(() -> verificador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expiracion");
    }

    @Test
    void rechazaUnTokenExpirado() {
        String token = TokenTestFactory.expirado(1);

        assertThatThrownBy(() -> verificador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnaFirmaDeOtraClave() {
        String token = TokenTestFactory.firmaAjena(1);

        assertThatThrownBy(() -> verificador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnSubjectNoNumerico() {
        String token = TokenTestFactory.conSubject("usuario-uno");

        assertThatThrownBy(() -> verificador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("numerica");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "99999999999999999999"})
    void rechazaIdentidadesFueraDelRangoPermitido(String subject) {
        String token = TokenTestFactory.conSubject(subject);

        assertThatThrownBy(() -> verificador.validar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaUnTokenMalformado() {
        String token = TokenTestFactory.malformado();

        assertThatThrownBy(() -> verificador.validar(token)).isInstanceOf(JwtException.class);
    }
}
