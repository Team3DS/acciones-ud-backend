package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI accionesUDOpenAPI() {
        return new OpenAPI()
                .info(new io.swagger.v3.oas.models.info.Info()
                        .title("M5 Órdenes y Ejecución")
                        .version("1.0")
                        .description("Ciclo de vida de órdenes, coordinación de reservas M2/M6, envío a Alpaca, conciliación de ejecuciones"));
    }
}
