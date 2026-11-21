package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtPropertiesTest {

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"corta", "0123456789012345678901234567890"})
    void rechazaSecretosConMenosDeTreintaYDosBytes(String secret) {
        assertThatThrownBy(() -> new JwtProperties(secret))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void aceptaUnSecretoDeExactamenteTreintaYDosBytes() {
        String secret = "a".repeat(JwtProperties.BYTES_MINIMOS);

        assertThat(new JwtProperties(secret).secret()).isEqualTo(secret);
    }

    @Test
    void mideBytesYNoCaracteres() {
        String secret = "á".repeat(16);

        assertThat(new JwtProperties(secret).secret()).hasSize(16);
    }
}
