package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.ProblemDetailsWriter;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.RestAccessDeniedHandler;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.security.RestAuthenticationEntryPoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.assertj.core.api.Assertions.assertThat;

class ProblemDetailsHandlersTest {

    private static final String TRACE_ID = "trace-handler";

    private final ObjectMapper objectMapper = Jackson2ObjectMapperBuilder.json().build();
    private final ProblemDetailsWriter writer = new ProblemDetailsWriter(objectMapper);
    private final RestAuthenticationEntryPoint entryPoint = new RestAuthenticationEntryPoint(writer);
    private final RestAccessDeniedHandler accessDeniedHandler = new RestAccessDeniedHandler(writer);

    @BeforeEach
    void preparar() {
        MDC.put(CorrelationIdFilter.MDC_KEY, TRACE_ID);
    }

    @AfterEach
    void limpiar() {
        MDC.clear();
    }

    @Test
    void elEntryPointResponde401EnProblemDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts/1/balance");
        MockHttpServletResponse response = new MockHttpServletResponse();

        entryPoint.commence(request, response, new InsufficientAuthenticationException("sin token"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":401")
                .contains("\"path\":\"/api/v1/accounts/1/balance\"")
                .contains("\"traceId\":\"" + TRACE_ID + "\"");
    }

    @Test
    void elHandlerResponde403EnProblemDetail() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/funds/deposits");
        MockHttpServletResponse response = new MockHttpServletResponse();

        accessDeniedHandler.handle(request, response, new AccessDeniedException("sin rol"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        assertThat(response.getContentAsString())
                .contains("\"status\":403")
                .contains("\"path\":\"/api/v1/funds/deposits\"")
                .contains("\"traceId\":\"" + TRACE_ID + "\"");
    }
}
