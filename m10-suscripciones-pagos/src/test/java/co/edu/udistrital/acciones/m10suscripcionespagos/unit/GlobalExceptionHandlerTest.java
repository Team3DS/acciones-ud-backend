package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoEstadoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.PremiumRequeridoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.NegocioException;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.GlobalExceptionHandler;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final EscritorProblemas escritor = new EscritorProblemas(
            objectMapper, Clock.fixed(Instant.parse("2026-05-06T07:08:09Z"), ZoneOffset.UTC));
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ControladorDePrueba())
            .setControllerAdvice(new GlobalExceptionHandler(escritor))
            .build();

    @BeforeEach
    void prepararTrace() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "traza-handler");
    }

    @AfterEach
    void limpiarTrace() {
        MDC.clear();
    }

    @Test
    void responde400CuandoFallaLaValidacion() throws Exception {
        mockMvc.perform(post("/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").value("traza-handler"))
                .andExpect(jsonPath("$.message").value("nombre: no debe estar vacio"));
    }

    @Test
    void responde400CuandoElJsonEsIlegible() throws Exception {
        mockMvc.perform(post("/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{no-es-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("El cuerpo de la peticion no es valido"));
    }

    @Test
    void responde400CuandoElParametroTieneOtroTipo() throws Exception {
        mockMvc.perform(get("/parametro").param("valor", "texto"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Parametro invalido: texto"));
    }

    @Test
    void responde400CuandoFaltaUnHeaderObligatorio() throws Exception {
        mockMvc.perform(get("/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void responde403ConElCodigoDePremiumRequerido() throws Exception {
        mockMvc.perform(get("/premium"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value(PremiumRequeridoException.CODIGO));
    }

    @Test
    void responde404CuandoElRecursoNoExiste() throws Exception {
        mockMvc.perform(get("/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void responde409CuandoHayConflictoDeEstado() throws Exception {
        mockMvc.perform(get("/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void responde422CuandoSeRompeUnaReglaDeNegocio() throws Exception {
        mockMvc.perform(get("/negocio"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("No se puede cambiar de intervalo"));
    }

    @Test
    void responde503ConElCodigoDelProveedor() throws Exception {
        mockMvc.perform(get("/proveedor"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.code").value(ProveedorPagosNoDisponibleException.CODIGO));
    }

    @Test
    void responde500ConMensajeGenericoYNoExponeElDetalle() throws Exception {
        mockMvc.perform(get("/inesperado"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(content().string(org.hamcrest.Matchers.not(
                        org.hamcrest.Matchers.containsString("detalle interno"))));
    }

    record Cuerpo(@NotBlank(message = "no debe estar vacio") String nombre) {
    }

    @RestController
    static class ControladorDePrueba {

        @PostMapping("/validacion")
        void validacion(@Valid @RequestBody Cuerpo cuerpo) {
        }

        @PostMapping("/json")
        void json(@RequestBody Cuerpo cuerpo) {
        }

        @GetMapping("/parametro")
        void parametro(@RequestParam int valor) {
        }

        @GetMapping("/header")
        void header(@RequestHeader("Idempotency-Key") String clave) {
        }

        @GetMapping("/premium")
        void premium() {
            throw new PremiumRequeridoException();
        }

        @GetMapping("/inexistente")
        void inexistente() {
            throw new RecursoNoEncontradoException("La suscripcion no existe");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictoEstadoException("La suscripcion no esta activa");
        }

        @GetMapping("/negocio")
        void negocio() {
            throw new NegocioException("No se puede cambiar de intervalo");
        }

        @GetMapping("/proveedor")
        void proveedor() {
            throw new ProveedorPagosNoDisponibleException("Stripe no responde");
        }

        @GetMapping("/inesperado")
        void inesperado() {
            throw new IllegalStateException("detalle interno");
        }
    }
}
