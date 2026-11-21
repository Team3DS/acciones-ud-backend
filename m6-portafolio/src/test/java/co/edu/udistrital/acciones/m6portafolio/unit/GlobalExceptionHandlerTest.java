package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.NegocioException;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.GlobalExceptionHandler;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.ApiProblemWriter;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private static final String TRACE = "traza-handler";

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ControladorDePruebas())
            .setControllerAdvice(new GlobalExceptionHandler(new ApiProblemWriter(
                    Jackson2ObjectMapperBuilder.json().build(), Clock.systemUTC())))
            .build();

    @BeforeEach
    void prepararTrace() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, TRACE);
    }

    @AfterEach
    void limpiarTrace() {
        MDC.clear();
    }

    @Test
    void unaValidacionFallidaRespondeCuatrocientos() throws Exception {
        mockMvc.perform(post("/pruebas/validacion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message", containsString("nombre")))
                .andExpect(jsonPath("$.path").value("/pruebas/validacion"))
                .andExpect(jsonPath("$.traceId").value(TRACE));
    }

    @Test
    void unJsonIlegibleRespondeCuatrocientos() throws Exception {
        mockMvc.perform(post("/pruebas/json")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la peticion no es valido"));
    }

    @Test
    void unParametroConTipoInvalidoRespondeCuatrocientos() throws Exception {
        mockMvc.perform(get("/pruebas/tipo/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("abc")));
    }

    @Test
    void unHeaderObligatorioFaltanteRespondeCuatrocientos() throws Exception {
        mockMvc.perform(get("/pruebas/cabecera"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("Idempotency-Key")));
    }

    @Test
    void unAccesoDenegadoRespondeCuatrocientosTres() throws Exception {
        mockMvc.perform(get("/pruebas/acceso"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("sin permiso"));
    }

    @Test
    void unRecursoInexistenteRespondeCuatrocientosCuatro() throws Exception {
        mockMvc.perform(get("/pruebas/inexistente"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("no existe"));
    }

    @Test
    void unConflictoDeIdempotenciaRespondeCuatrocientosNueve() throws Exception {
        mockMvc.perform(get("/pruebas/conflicto"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("clave repetida"));
    }

    @Test
    void unaReglaDeNegocioRespondeCuatrocientosVeintidos() throws Exception {
        mockMvc.perform(get("/pruebas/negocio"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("regla incumplida"));
    }

    @Test
    void unErrorNoControladoRespondeQuinientosSinExponerElDetalle() throws Exception {
        mockMvc.perform(get("/pruebas/interno"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Error interno del servidor"));
    }

    @RestController
    @RequestMapping("/pruebas")
    static class ControladorDePruebas {

        @PostMapping("/validacion")
        void validacion(@Valid @RequestBody Cuerpo cuerpo) {
        }

        @PostMapping("/json")
        void json(@RequestBody Cuerpo cuerpo) {
        }

        @GetMapping("/tipo/{id}")
        void tipo(@PathVariable int id) {
        }

        @GetMapping("/cabecera")
        void cabecera(@RequestHeader("Idempotency-Key") String clave) {
        }

        @GetMapping("/acceso")
        void acceso() {
            throw new AccesoDenegadoException("sin permiso");
        }

        @GetMapping("/inexistente")
        void inexistente() {
            throw new RecursoNoEncontradoException("no existe");
        }

        @GetMapping("/conflicto")
        void conflicto() {
            throw new ConflictoIdempotenciaException("clave repetida");
        }

        @GetMapping("/negocio")
        void negocio() {
            throw new NegocioException("regla incumplida");
        }

        @GetMapping("/interno")
        void interno() {
            throw new IllegalStateException("detalle sensible");
        }
    }

    record Cuerpo(@NotBlank String nombre) {
    }
}
