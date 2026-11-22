package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.TokenInternoProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PropiedadesSeguridadTest {

    @Test
    void jwtRechazaUnSecretoAusente() {
        assertThatThrownBy(() -> new JwtProperties(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void jwtRechazaUnSecretoCorto() {
        assertThatThrownBy(() -> new JwtProperties("clave-corta"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void jwtAceptaUnSecretoDe32Bytes() {
        JwtProperties propiedades = new JwtProperties("a".repeat(32));

        assertThat(propiedades.secret()).hasSize(32);
    }

    @Test
    void tokenInternoTrataElValorAusenteComoNoConfigurado() {
        TokenInternoProperties propiedades = new TokenInternoProperties(null);

        assertThat(propiedades.token()).isEmpty();
        assertThat(propiedades.configurado()).isFalse();
    }

    @Test
    void tokenInternoTrataElValorEnBlancoComoNoConfigurado() {
        TokenInternoProperties propiedades = new TokenInternoProperties("   ");

        assertThat(propiedades.configurado()).isFalse();
    }

    @Test
    void tokenInternoRechazaUnValorCortoCuandoEstaConfigurado() {
        assertThatThrownBy(() -> new TokenInternoProperties("token-corto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }

    @Test
    void tokenInternoAcepta32Caracteres() {
        TokenInternoProperties propiedades = new TokenInternoProperties("b".repeat(32));

        assertThat(propiedades.configurado()).isTrue();
    }
}
