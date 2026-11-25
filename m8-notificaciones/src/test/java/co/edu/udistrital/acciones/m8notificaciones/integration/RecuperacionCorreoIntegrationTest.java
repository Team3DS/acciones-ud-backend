package co.edu.udistrital.acciones.m8notificaciones.integration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

/** Real PostgreSQL and a private HTTP mail provider; never sends real email. */
@ExtendWith(OutputCaptureExtension.class)
class RecuperacionCorreoIntegrationTest extends IntegrationSupport {
    @Autowired Declarables avisosTopology;
    @Test
    void enlaceDeRecuperacionEstaCifradoEsIdempotenteYNoApareceEnAppNiLogs(CapturedOutput output) throws Exception {
        var event = event("m1.recuperacion.solicitada", Map.of(
                "userId", 1, "email", "persona+cuenta@example.test", "code", "731946",
                "expiresAt", Instant.now().plusSeconds(300).toString(), "publicUrl", "https://untrusted.example"));
        events.procesar(event);
        events.procesar(event);
        assertThat(count("inbox_evento")).isEqualTo(1);
        assertThat(count("entrega_correo")).isEqualTo(1);
        assertThat(count("notificacion")).isZero();
        assertThat(jdbc.queryForObject("SELECT contenido_cifrado FROM entrega_correo", String.class))
                .doesNotContain("731946", "persona+cuenta", "#password-reset");
        assertThat(deliveries.procesar(10)).isEqualTo(1);
        var requests = HTTP.findAll(postRequestedFor(urlEqualTo("/emails")));
        assertThat(requests).hasSize(1);
        String html = mapper.readTree(requests.getFirst().getBodyAsString()).path("html").asText();
        assertThat(html).contains("https://accionesud.site/#password-reset?correo=persona%2Bcuenta%40example.test&amp;code=731946")
                .doesNotContain("untrusted.example");
        assertThat(jdbc.queryForObject("SELECT contenido_cifrado FROM entrega_correo", String.class)).isEmpty();
        assertThat(output.getAll()).doesNotContain("731946", "persona+cuenta@example.test", "#password-reset?");
    }

    @Test
    void solicitudVencidaNoEncolaCorreoYEntregaVencidaNoLlamaAlProveedor() {
        events.procesar(event("m1.recuperacion.solicitada", Map.of("userId", 1, "email", "expiry@example.test",
                "code", "821947", "expiresAt", Instant.now().minusSeconds(1).toString())));
        assertThat(count("entrega_correo")).isZero();
        events.procesar(event("m1.recuperacion.solicitada", Map.of("userId", 1, "email", "expiry@example.test",
                "code", "821948", "expiresAt", Instant.now().plusSeconds(300).toString())));
        jdbc.update("UPDATE entrega_correo SET expira_en=CURRENT_TIMESTAMP-INTERVAL '1 second'");
        deliveries.procesar(10);
        assertThat(jdbc.queryForObject("SELECT estado FROM entrega_correo", String.class)).isEqualTo("EXPIRADO");
        HTTP.verify(0, postRequestedFor(urlEqualTo("/emails")));
        assertThat(count("notificacion")).isZero();
    }

    @Test
    void cambioCompletadoAvisaSoloAlPropietarioSinCodigoNiAppYDeduplica(CapturedOutput output) throws Exception {
        assertThat(avisosTopology.getDeclarablesByType(Binding.class))
                .extracting(Binding::getRoutingKey).contains("m1.recuperacion.completada");
        var event = event("m1.recuperacion.completada", Map.of("userId", 42, "email", "owner@example.test"));
        events.procesar(event);
        events.procesar(event);
        assertThat(count("entrega_correo")).isEqualTo(1);
        assertThat(count("notificacion")).isZero();
        assertThat(jdbc.queryForObject("SELECT usuario_id FROM entrega_correo", Long.class)).isEqualTo(42L);
        assertThat(jdbc.queryForObject("SELECT contenido_cifrado FROM entrega_correo", String.class))
                .doesNotContain("owner@example.test", "Contraseña");
        deliveries.procesar(10);
        var requests = HTTP.findAll(postRequestedFor(urlEqualTo("/emails")));
        assertThat(requests).hasSize(1);
        var body = mapper.readTree(requests.getFirst().getBodyAsString());
        assertThat(body.path("to").toString()).contains("owner@example.test");
        assertThat(body.path("subject").asText()).isEqualTo("Contraseña actualizada");
        assertThat(body.path("html").asText()).doesNotContain("#password-reset", "Tu código es");
        assertThat(jdbc.queryForObject("SELECT contenido_cifrado FROM entrega_correo", String.class)).isEmpty();
        assertThat(output.getAll()).doesNotContain("owner@example.test");
    }
}
