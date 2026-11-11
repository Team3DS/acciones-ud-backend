package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.ConflictoReservaException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.excepcion.FondosInsuficientesException;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.GlobalExceptionHandler;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String TRACE_ID = "trace-prueba";

    private MockMvc mockMvc;

    @BeforeEach
    void preparar() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ControladorDePrueba())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        MDC.put(CorrelationIdFilter.MDC_KEY, TRACE_ID);
    }

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void respondeBadRequestCuandoFallaLaValidacion() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("orderId")))
                .andExpect(jsonPath("$.path").value("/prueba/validacion"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeBadRequestConJsonInvalido() throws Exception {
        mockMvc.perform(post("/prueba/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"orderId\": "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeBadRequestCuandoFaltaUnHeader() throws Exception {
        mockMvc.perform(get("/prueba/header"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeBadRequestCuandoElParametroTieneOtroTipo() throws Exception {
        mockMvc.perform(get("/prueba/tipo").param("valor", "no-es-un-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeForbiddenCuandoElAccesoEsDenegado() throws Exception {
        mockMvc.perform(get("/prueba/denegado"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("sin permiso"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeNotFoundCuandoElRecursoNoExiste() throws Exception {
        mockMvc.perform(get("/prueba/no-encontrado"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("no existe"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeConflictCuandoHayConflictoDeIdempotencia() throws Exception {
        mockMvc.perform(get("/prueba/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("clave repetida"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeBadRequestCuandoLaSolicitudEsInvalida() throws Exception {
        mockMvc.perform(get("/prueba/solicitud-invalida"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("clave obligatoria"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeConflictCuandoLaReservaYaExiste() throws Exception {
        mockMvc.perform(get("/prueba/reserva-duplicada"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("orden duplicada"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeUnprocessableCuandoNoHayFondos() throws Exception {
        mockMvc.perform(get("/prueba/fondos"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("fondos insuficientes"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    @Test
    void respondeErrorInternoSinExponerDetalles() throws Exception {
        mockMvc.perform(get("/prueba/error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"))
                .andExpect(jsonPath("$.traceId").value(TRACE_ID));
    }

    record Cuerpo(@NotBlank String orderId) {
    }

    @RestController
    static class ControladorDePrueba {

        @PostMapping("/prueba/validacion")
        void validacion(@Valid @RequestBody Cuerpo cuerpo) {
        }

        @GetMapping("/prueba/header")
        void header(@RequestHeader("Idempotency-Key") String clave) {
        }

        @GetMapping("/prueba/tipo")
        void tipo(@RequestParam int valor) {
        }

        @GetMapping("/prueba/denegado")
        void denegado() {
            throw new AccesoDenegadoException("sin permiso");
        }

        @GetMapping("/prueba/no-encontrado")
        void noEncontrado() {
            throw new RecursoNoEncontradoException("no existe");
        }

        @GetMapping("/prueba/conflicto")
        void conflicto() {
            throw new ConflictoIdempotenciaException("clave repetida");
        }

        @GetMapping("/prueba/solicitud-invalida")
        void solicitudInvalida() {
            throw new SolicitudInvalidaException("clave obligatoria");
        }

        @GetMapping("/prueba/reserva-duplicada")
        void reservaDuplicada() {
            throw new ConflictoReservaException("orden duplicada");
        }

        @GetMapping("/prueba/fondos")
        void fondos() {
            throw new FondosInsuficientesException("fondos insuficientes");
        }

        @GetMapping("/prueba/error")
        void error() {
            throw new IllegalStateException("fallo simulado");
        }
    }
}
