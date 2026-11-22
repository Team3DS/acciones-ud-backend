package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CorrelationIdFilterTest {

    private static final String PATRON_UUID =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private final CorrelationIdFilter filtro = new CorrelationIdFilter();

    @AfterEach
    void limpiarContexto() {
        MDC.clear();
    }

    @Test
    void propagaElIdentificadorRecibidoAlMdcYALaRespuesta() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/plans");
        request.addHeader(CorrelationIdFilter.HEADER, "pago-456_XYZ");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> enMdc = new AtomicReference<>();

        filtro.doFilter(request, response, cadenaQueCaptura(enMdc));

        assertThat(enMdc.get()).isEqualTo("pago-456_XYZ");
        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).isEqualTo("pago-456_XYZ");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "con espacios", "simbolo/raros", "acentuado-á"})
    void reemplazaIdentificadoresConFormatoInvalido(String recibido) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/plans");
        request.addHeader(CorrelationIdFilter.HEADER, recibido);

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches(PATRON_UUID);
    }

    @Test
    void reemplazaIdentificadoresMasLargosQueSesentaYCuatroCaracteres() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/payments/plans");
        request.addHeader(CorrelationIdFilter.HEADER, "b".repeat(65));

        filtro.doFilter(request, response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches(PATRON_UUID);
    }

    @Test
    void generaIdentificadorCuandoNoLlegaElHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, (req, res) -> { });

        assertThat(response.getHeader(CorrelationIdFilter.HEADER)).matches(PATRON_UUID);
    }

    @Test
    void retiraElIdentificadorDelMdcAlTerminar() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filtro.doFilter(new MockHttpServletRequest("GET", "/actuator/health"), response, (req, res) -> { });

        assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isNull();
    }

    @Test
    void retiraElIdentificadorDelMdcAunqueLaCadenaFalle() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain cadenaQueFalla = (req, res) -> {
            throw new IllegalStateException("fallo simulado");
        };

        assertThatThrownBy(() -> filtro.doFilter(
                new MockHttpServletRequest("GET", "/actuator/health"), response, cadenaQueFalla))
                .isInstanceOf(IllegalStateException.class);
        assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isNull();
    }

    private FilterChain cadenaQueCaptura(AtomicReference<String> destino) {
        return (req, res) -> destino.set(MDC.get(CorrelationIdFilter.CLAVE_MDC));
    }
}
