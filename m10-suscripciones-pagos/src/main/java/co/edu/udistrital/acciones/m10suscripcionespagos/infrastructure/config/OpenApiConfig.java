package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String ESQUEMA_BEARER = "bearerAuth";
    public static final String ESQUEMA_TOKEN_INTERNO = "tokenInterno";

    @Bean
    public OpenAPI suscripcionesPagosOpenApi() {
        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("JWT emitido por M1");
        SecurityScheme tokenInterno = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("Token interno")
                .description("Token compartido para las rutas /internal/**");
        return new OpenAPI()
                .info(new Info()
                        .title("M10 Suscripciones y Pagos")
                        .version("v1")
                        .description("Planes, suscripciones, pagos y estado premium"))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_BEARER, bearer)
                        .addSecuritySchemes(ESQUEMA_TOKEN_INTERNO, tokenInterno))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_BEARER));
    }
}
