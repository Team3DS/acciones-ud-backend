package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.EmisorTokenInterno;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.mock.http.client.MockClientHttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    private final EmisorTokenInterno emisor =
            new EmisorTokenInterno(new JwtProperties("clave-de-prueba-de-al-menos-32-caracteres"));
    private final ClientHttpRequestExecution respuestaOk =
            (request, body) -> new MockClientHttpResponse(new byte[0], HttpStatus.OK);

    @Test
    void agregaElTokenDeSistemaSiLaPeticionNoTraeUno() throws Exception {
        MockClientHttpRequest peticion = new MockClientHttpRequest();

        RestClientConfig.tokenDeSistema(emisor).intercept(peticion, new byte[0], respuestaOk);

        assertThat(peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).startsWith("Bearer ");
    }

    @Test
    void respetaElTokenQueYaTraeLaPeticion() throws Exception {
        MockClientHttpRequest peticion = new MockClientHttpRequest();
        peticion.getHeaders().setBearerAuth("token-del-usuario");

        RestClientConfig.tokenDeSistema(emisor).intercept(peticion, new byte[0], respuestaOk);

        assertThat(peticion.getHeaders().get(HttpHeaders.AUTHORIZATION)).containsExactly("Bearer token-del-usuario");
    }

    @Test
    void soloElClienteInternoLlevaElInterceptor() {
        RestClientConfig config = new RestClientConfig();

        assertThat(config.restTemplate(new RestTemplateBuilder()).getInterceptors()).isEmpty();
        assertThat(config.restTemplateInterno(new RestTemplateBuilder(), emisor).getInterceptors()).hasSize(1);
    }
}
