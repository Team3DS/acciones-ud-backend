package co.edu.udistrital.acciones.m1identidad.unit;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.udistrital.acciones.m1identidad.application.dto.RegisterUserRequest;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RegisterUserRequestJsonTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @ParameterizedTest
    @ValueSource(strings = {"celular", "telefono"})
    void aceptaElNombreCanonicoYElUsadoEnProduccion(String field) throws Exception {
        RegisterUserRequest request =
                mapper.readValue(
                        "{\"nombres\":\"Ana\",\"correo\":\"ana@example.test\",\""
                                + field
                                + "\":\"3001234567\"}",
                        RegisterUserRequest.class);

        assertThat(request.celular()).isEqualTo("3001234567");
        assertThat(request.nombres()).isEqualTo("Ana");
    }

    @Test
    void serializaConCelularYConservaElValorAlVolverADeserializar() throws Exception {
        RegisterUserRequest request =
                new RegisterUserRequest(
                        "Ana",
                        "Pérez",
                        "CC",
                        "1234",
                        "ana@example.test",
                        "fixture-password",
                        "PRINCIPIANTE",
                        "3001234567");

        String json = mapper.writeValueAsString(request);

        assertThat(mapper.readTree(json).path("celular").asText()).isEqualTo("3001234567");
        assertThat(mapper.readTree(json).has("telefono")).isFalse();
        assertThat(mapper.readValue(json, RegisterUserRequest.class)).isEqualTo(request);
    }
}
