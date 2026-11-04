package co.edu.udistrital.acciones.m1identidad.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
            .title("M1 Identidad y Accesos")
            .version("v1")
            .description("Gestión de usuarios, roles y MFA"));
    }
}
