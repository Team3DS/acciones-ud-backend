package co.edu.udistrital.acciones.m8notificaciones.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.in.EventoDecoder;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

class EventoDecoderTest {
    private final EventoDecoder decoder = new EventoDecoder(new ObjectMapper());
    private final String id = UUID.randomUUID().toString();
    private final String json =
            "{\"eventId\":\""
                    + id
                    + "\",\"eventType\":\"m2.movimiento.creado\",\"occurredAt\":\"2026-11-16T12:00:00Z\",\"payload\":{\"userId\":1}}";

    @Test
    void leeSobreCompatibleConM2() {
        var event =
                decoder.decode(json.getBytes(StandardCharsets.UTF_8), "m2.movimiento.creado", null);
        assertThat(event.eventId()).isEqualTo(UUID.fromString(id));
        assertThat(event.source()).isEqualTo("M2");
        assertThat(event.payload().path("userId").asInt()).isEqualTo(1);
        assertThat(event.toString()).doesNotContain("userId");
    }

    @Test
    void admitePayloadJsonCodificadoComoTexto() throws Exception {
        var mapper = new ObjectMapper();
        var root = (com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree(json);
        root.put("payload", "{\"userId\":2}");
        root.remove("eventId");
        assertThat(
                        decoder.decode(mapper.writeValueAsBytes(root), "m2.movimiento.creado", id)
                                .payload()
                                .path("userId")
                                .asInt())
                .isEqualTo(2);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"{}", "[]", "null", "invalid", "{\"eventType\":\"m2.movimiento.creado\"}"})
    void rechazaSobresInvalidos(String body) {
        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        body.getBytes(StandardCharsets.UTF_8),
                                        "m2.movimiento.creado",
                                        null))
                .isInstanceOf(AvisoException.class);
    }

    @Test
    void rechazaRutaOrigenIdentificadorYFechaManipulados() {
        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        json.getBytes(StandardCharsets.UTF_8),
                                        "m5.orden.creada",
                                        null))
                .isInstanceOf(AvisoException.class);
        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        json.replace(id, "1-1-1-1-1")
                                                .getBytes(StandardCharsets.UTF_8),
                                        "m2.movimiento.creado",
                                        null))
                .isInstanceOf(AvisoException.class);
        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        json.replace("12:00:00Z", "invalido")
                                                .getBytes(StandardCharsets.UTF_8),
                                        "m2.movimiento.creado",
                                        null))
                .isInstanceOf(AvisoException.class);
        assertThatThrownBy(
                        () ->
                                decoder.decode(
                                        json.replace(
                                                        "\"payload\":",
                                                        "\"source\":\"M1\",\"payload\":")
                                                .getBytes(StandardCharsets.UTF_8),
                                        "m2.movimiento.creado",
                                        null))
                .isInstanceOf(AvisoException.class);
        assertThatThrownBy(() -> decoder.decode(new byte[65537], "m2.movimiento.creado", id))
                .isInstanceOf(AvisoException.class);
    }
}
