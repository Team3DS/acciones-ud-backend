package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.ForbiddenAccessHandler;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtAuthenticationFilter;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security.UnauthorizedEntryPoint;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

    private static final String[] RUTAS_PUBLICAS = {
            "/actuator/**",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html",
            "/error",
            "/api/v1/webhooks/**"
    };

    private final JwtTokenValidator validador;
    private final UnauthorizedEntryPoint unauthorizedEntryPoint;
    private final ForbiddenAccessHandler forbiddenAccessHandler;

    public SecurityConfig(JwtTokenValidator validador,
                          UnauthorizedEntryPoint unauthorizedEntryPoint,
                          ForbiddenAccessHandler forbiddenAccessHandler) {
        this.validador = validador;
        this.unauthorizedEntryPoint = unauthorizedEntryPoint;
        this.forbiddenAccessHandler = forbiddenAccessHandler;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(peticiones -> peticiones
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .anyRequest().authenticated())
                .exceptionHandling(manejo -> manejo
                        .authenticationEntryPoint(unauthorizedEntryPoint)
                        .accessDeniedHandler(forbiddenAccessHandler))
                .addFilterBefore(new JwtAuthenticationFilter(validador),
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
