package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.security.ServicioProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({ClientesHttpProperties.class, ServicioProperties.class})
public class ClientesHttpConfig {

    @Bean
    public RestClient m3RestClient(RestClient.Builder builder, ClientesHttpProperties propiedades) {
        return construir(builder, propiedades.m3BaseUrl(), propiedades.timeoutSeconds());
    }

    @Bean
    public RestClient m4RestClient(RestClient.Builder builder, ClientesHttpProperties propiedades) {
        return construir(builder, propiedades.m4BaseUrl(), propiedades.timeoutSeconds());
    }

    private static RestClient construir(RestClient.Builder builder, String baseUrl, int timeoutSeconds) {
        Duration timeout = Duration.ofSeconds(timeoutSeconds);
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        return builder.clone()
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(baseUrl)
                .build();
    }
}
