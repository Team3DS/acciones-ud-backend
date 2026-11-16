package co.edu.udistrital.acciones.m8notificaciones.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.security.CifradoCorreo;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Base64;

class CifradoCorreoTest {
    private final CifradoCorreo crypto =
            new CifradoCorreo("MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=", new ObjectMapper());

    @Test
    void protegeContenidoYUsaNonceDistinto() {
        Correo correo = new Correo(1, "fixture@example.test", "Código", "123456");
        String first = crypto.cifrar(correo);
        assertThat(first).isNotEqualTo(crypto.cifrar(correo)).doesNotContain("123456", correo.to());
        assertThat(crypto.descifrar(first)).isEqualTo(correo);
        assertThat(correo.toString()).doesNotContain("123456", correo.to());
    }

    @Test
    void detectaManipulacion() {
        byte[] bytes =
                Base64.getDecoder()
                        .decode(
                                crypto.cifrar(
                                        new Correo(1, "a@example.test", "Aviso", "Contenido")));
        bytes[bytes.length - 1] ^= 1;
        assertThatThrownBy(() -> crypto.descifrar(Base64.getEncoder().encodeToString(bytes)))
                .isInstanceOf(IllegalStateException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "garbage", "AAAA"})
    void rechazaContenidoInvalido(String value) {
        assertThatThrownBy(() -> crypto.descifrar(value)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void exigeClaveDe256Bits() {
        assertThatThrownBy(() -> new CifradoCorreo("AAAA", new ObjectMapper()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
