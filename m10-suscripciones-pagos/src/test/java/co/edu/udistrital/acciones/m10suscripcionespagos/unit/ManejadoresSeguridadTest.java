package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.EscritorProblemas;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ManejadorAccesoDenegado;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ManejadorNoAutorizado;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ManejadoresSeguridadTest {

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final EscritorProblemas escritor = new EscritorProblemas(
            objectMapper, Clock.fixed(Instant.parse("2026-05-06T07:08:09Z"), ZoneOffset.UTC));
    private final ManejadorNoAutorizado noAutorizado = new ManejadorNoAutorizado(escritor);
    private final ManejadorAccesoDenegado accesoDenegado = new ManejadorAccesoDenegado(escritor);

    @BeforeEach
    void prepararTrace() {
        MDC.put(CorrelationIdFilter.CLAVE_MDC, "traza-seguridad");
    }

    @AfterEach
    void limpiarTrace() {
        MDC.clear();
    }

    @Test
    void elEntryPointResponde401ConWwwAuthenticate() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/subscriptions/me");
        MockHttpServletResponse response = new MockHttpServletResponse();

        noAutorizado.commence(request, response, new InsufficientAuthenticationException("sin token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getHeader(HttpHeaders.WWW_AUTHENTICATE)).isEqualTo("Bearer");
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> cuerpo = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
        assertThat(cuerpo)
                .containsEntry("status", 401)
                .containsEntry("error", "Unauthorized")
                .containsEntry("path", "/api/v1/subscriptions/me")
                .containsEntry("traceId", "traza-seguridad");
    }

    @Test
    void elAccessDeniedHandlerResponde403EnProblemJson() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/subscriptions/1/entitlement");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accesoDenegado.handle(request, response, new AccessDeniedException("sin permisos"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        Map<String, Object> cuerpo = objectMapper.readValue(response.getContentAsByteArray(), Map.class);
        assertThat(cuerpo)
                .containsEntry("status", 403)
                .containsEntry("error", "Forbidden")
                .containsEntry("path", "/internal/subscriptions/1/entitlement")
                .containsEntry("traceId", "traza-seguridad");
    }
}
