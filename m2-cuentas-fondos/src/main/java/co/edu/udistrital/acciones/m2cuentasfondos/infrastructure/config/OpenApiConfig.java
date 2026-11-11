package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
            .title("M2 Cuentas y Fondos")
            .version("v1")
            .description("Saldos, reservas de efectivo y movimientos"));
    }
}
