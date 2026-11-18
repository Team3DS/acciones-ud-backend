package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI portafolioOpenApi() {
        return new OpenAPI().info(new Info()
                .title("M6 Portafolio")
                .version("v1")
                .description("Posiciones, costo promedio, valoracion y P&L del portafolio"));
    }
}
