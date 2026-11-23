package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.EmisorTokenInterno;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    public static final String REST_TEMPLATE_INTERNO = "restTemplateInterno";

    /** Cliente general (Alpaca y llamadas que ya fijan sus cabeceras). */
    @Bean
    @Primary
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return base(builder).build();
    }

    /** Cliente para otros microservicios: agrega el token de sistema si la petición no trae uno. */
    @Bean(REST_TEMPLATE_INTERNO)
    public RestTemplate restTemplateInterno(RestTemplateBuilder builder, EmisorTokenInterno emisor) {
        return base(builder).additionalInterceptors(tokenDeSistema(emisor)).build();
    }

    static ClientHttpRequestInterceptor tokenDeSistema(EmisorTokenInterno emisor) {
        return (request, body, execution) -> {
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                request.getHeaders().setBearerAuth(emisor.deSistema());
            }
            return execution.execute(request, body);
        };
    }

    private static RestTemplateBuilder base(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5));
    }
}
