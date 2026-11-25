package co.edu.udistrital.acciones.m8notificaciones.unit;

import co.edu.udistrital.acciones.m8notificaciones.application.service.EnlaceRecuperacion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EnlaceRecuperacionTest {
    @Test
    void soloElFragmentoContieneElCorreoCodificadoYCodigo() {
        String link = new EnlaceRecuperacion("https://accionesud.site/")
                .crear("persona+cuenta@example.test", "000123");
        assertThat(link).isEqualTo("https://accionesud.site/#password-reset?correo=persona%2Bcuenta%40example.test&code=000123");
        URI uri = URI.create(link);
        assertThat(uri.getRawQuery()).isNull();
        assertThat(uri.getRawPath()).isEqualTo("/");
        assertThat(uri.getRawFragment()).startsWith("password-reset?");
    }

    @Test
    void utilizaSoloElOrigenConfiguradoSinInventarUnHostDeLaSolicitud() {
        assertThat(new EnlaceRecuperacion("https://app.example.test").crear("a@example.test", "123456"))
                .startsWith("https://app.example.test/#password-reset?");
    }

    @ParameterizedTest
    @ValueSource(strings = {"http://accionesud.site", "//accionesud.site", "javascript:alert(1)",
            "https://user:secret@accionesud.site", "https://accionesud.site?redirect=evil",
            "https://accionesud.site#evil", "https://accionesud.site/path", "https://", " https://accionesud.site"})
    void rechazaOrigenesInvalidosSinIncluirLaConfiguracionEnElError(String origin) {
        assertThatThrownBy(() -> new EnlaceRecuperacion(origin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("APP_PUBLIC_URL")
                .hasMessageNotContaining(origin);
    }
}
