package co.edu.udistrital.acciones.m8notificaciones.unit;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.udistrital.acciones.m8notificaciones.domain.DireccionCorreo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DireccionCorreoTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "fixture@example.test",
                "nombre.apellido@example.co",
                "avisos+orden@sub.example.com"
            })
    void aceptaDestinatariosSinEjecutarExpresionesRegulares(String value) {
        assertThat(DireccionCorreo.valida(value)).isTrue();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {
                "sin-arroba",
                "@example.test",
                "a@",
                "a@host",
                "a@host.",
                "a@.test",
                "a@@example.test",
                "a b@example.test",
                "a@example.test\r\nBcc:otro@example.test",
                "a<@example.test",
                "a>@example.test",
                "a\u0000@example.test",
                "a\u2003b@example.test"
            })
    void rechazaDireccionesInvalidasYControles(String value) {
        assertThat(DireccionCorreo.valida(value)).isFalse();
    }

    @Test
    void limitaLongitudAntesDeRecorrerLaEntrada() {
        assertThat(DireccionCorreo.valida("a".repeat(241) + "@example.test")).isTrue();
        assertThat(DireccionCorreo.valida("a".repeat(242) + "@example.test")).isFalse();
        assertThat(DireccionCorreo.valida("a".repeat(100000))).isFalse();
    }
}
