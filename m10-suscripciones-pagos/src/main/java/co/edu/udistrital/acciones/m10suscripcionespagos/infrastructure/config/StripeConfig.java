package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import com.stripe.StripeClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(StripeProperties.class)
public class StripeConfig {

    private static final String CLAVE_NO_CONFIGURADA = "sk_test_no_configurada";

    @Bean
    public StripeClient stripeClient(StripeProperties propiedades) {
        String clave = propiedades.configurado() ? propiedades.apiKey() : CLAVE_NO_CONFIGURADA;
        return StripeClient.builder()
                .setApiKey(clave)
                .setApiBase(propiedades.apiBase())
                .setConnectTimeout(propiedades.connectTimeoutMs())
                .setReadTimeout(propiedades.readTimeoutMs())
                .setMaxNetworkRetries(propiedades.maxNetworkRetries())
                .build();
    }
}
