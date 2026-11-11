package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private static final String UUID_REGEX =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @AfterEach
    void limpiarMdc() {
        MDC.clear();
    }

    @Test
    void conservaUnCorrelationIdValido() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/accounts/1/balance");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "abc_123-XYZ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> vistoEnMdc = new AtomicReference<>();

        filtro.doFilter(request, response, cadenaQueCapturaMdc(vistoEnMdc));

        assertThat(vistoEnMdc.get()).isEqualTo("abc_123-XYZ");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo("abc_123-XYZ");
    }

    @Test
    void aceptaUnCorrelationIdDeSesentaYCuatroCaracteres() throws Exception {
        String sesentaYCuatro = "a".repeat(64);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, sesentaYCuatro);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).isEqualTo(sesentaYCuatro);
    }

    @Test
    void generaUnUuidSiNoVieneElHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).matches(UUID_REGEX);
    }

    @Test
    void generaUnUuidSiElHeaderEsMuyLargo() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "a".repeat(65));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME))
                .matches(UUID_REGEX)
                .isNotEqualTo("a".repeat(65));
    }

    @Test
    void generaUnUuidSiElHeaderTieneCaracteresInvalidos() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        request.addHeader(CorrelationIdFilter.HEADER_NAME, "valor con espacios");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER_NAME)).matches(UUID_REGEX);
    }

    @Test
    void limpiaElMdcAlTerminar() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, (req, res) -> { });

        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    @Test
    void limpiaElMdcAunqueLaCadenaFalle() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain cadenaQueFalla = (req, res) -> {
            throw new IllegalStateException("fallo simulado");
        };

        assertThatThrownBy(() -> filtro.doFilter(request, response, cadenaQueFalla))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(CorrelationIdFilter.MDC_KEY)).isNull();
    }

    private FilterChain cadenaQueCapturaMdc(AtomicReference<String> vistoEnMdc) {
        return (req, res) -> vistoEnMdc.set(MDC.get(CorrelationIdFilter.MDC_KEY));
    }
}
