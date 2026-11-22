package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class CorrelationIdFilterTest {

    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    private String filtrar(String recibido, AtomicReference<String> enMdc) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/orders");
        if (recibido != null) {
            request.addHeader(CorrelationIdFilter.HEADER, recibido);
        }
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> enMdc.set(MDC.get(CorrelationIdFilter.CLAVE_MDC));
        filter.doFilter(request, response, chain);
        return response.getHeader(CorrelationIdFilter.HEADER);
    }

    @Test
    void conservaUnIdentificadorValidoYLoPoneEnElMdcDuranteLaSolicitud() throws Exception {
        AtomicReference<String> enMdc = new AtomicReference<>();

        String devuelto = filtrar("abc-123_XYZ", enMdc);

        assertThat(devuelto).isEqualTo("abc-123_XYZ");
        assertThat(enMdc.get()).isEqualTo("abc-123_XYZ");
        assertThat(MDC.get(CorrelationIdFilter.CLAVE_MDC)).isNull();
    }

    @Test
    void generaUnUuidSiNoLlegaEncabezado() throws Exception {
        AtomicReference<String> enMdc = new AtomicReference<>();

        String devuelto = filtrar(null, enMdc);

        assertThatCode(() -> UUID.fromString(devuelto)).doesNotThrowAnyException();
        assertThat(enMdc.get()).isEqualTo(devuelto);
    }

    @Test
    void reemplazaValoresVaciosLargosOConCaracteresNoPermitidos() throws Exception {
        for (String invalido : new String[]{" ", "a".repeat(65), "id con espacios", "id;DROP", "ñandú"}) {
            String devuelto = filtrar(invalido, new AtomicReference<>());
            assertThat(devuelto).as(invalido).isNotEqualTo(invalido);
            assertThatCode(() -> UUID.fromString(devuelto)).as(invalido).doesNotThrowAnyException();
        }
    }

    @Test
    void aceptaElLargoMaximo() throws Exception {
        String maximo = "a".repeat(64);

        assertThat(filtrar(maximo, new AtomicReference<>())).isEqualTo(maximo);
    }
}
