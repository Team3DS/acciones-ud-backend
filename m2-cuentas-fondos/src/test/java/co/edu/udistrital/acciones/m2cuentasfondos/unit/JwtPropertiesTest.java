package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @Test
    void fallaConSecretoNulo() {
        JwtProperties propiedades = new JwtProperties();

        assertThatThrownBy(propiedades::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void fallaConSecretoCorto() {
        JwtProperties propiedades = new JwtProperties();
        propiedades.setSecret("a".repeat(31));

        assertThatThrownBy(propiedades::validar)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void aceptaSecretoDeTreintaYDosBytes() {
        JwtProperties propiedades = new JwtProperties();
        propiedades.setSecret("a".repeat(32));

        assertThatCode(propiedades::validar).doesNotThrowAnyException();
        assertThat(propiedades.getSecret()).hasSize(32);
    }
}
