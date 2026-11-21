package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.ServicioProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties({M4ClientProperties.class, ServicioProperties.class})
public class M4ClientConfig {

    @Bean
    public RestClient m4RestClient(RestClient.Builder builder, M4ClientProperties propiedades) {
        Duration timeout = Duration.ofSeconds(propiedades.timeoutSeconds());
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        JdkClientHttpRequestFactory fabrica = new JdkClientHttpRequestFactory(httpClient);
        fabrica.setReadTimeout(timeout);
        return builder.baseUrl(propiedades.baseUrl()).requestFactory(fabrica).build();
    }
}
