package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.config;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtAuthenticationFilter;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtTokenValidator;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {
    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http, JwtTokenValidator validator, ObjectMapper mapper) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/actuator/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.POST,
                                                "/api/v1/catalog/markets",
                                                "/api/v1/markets",
                                                "/api/v1/instruments")
                                        .hasRole("ADMINISTRADOR")
                                        .requestMatchers(
                                                org.springframework.http.HttpMethod.PATCH,
                                                "/api/v1/catalog/markets/**",
                                                "/api/v1/markets/**",
                                                "/api/v1/instruments/**")
                                        .hasRole("ADMINISTRADOR")
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.accessDeniedHandler(
                                                (request, response, exception) -> {
                                                    response.setStatus(403);
                                                    response.setContentType(
                                                            MediaType
                                                                    .APPLICATION_PROBLEM_JSON_VALUE);
                                                    mapper.writeValue(
                                                            response.getOutputStream(),
                                                            ApiProblems.create(
                                                                    403,
                                                                    "ACCESO_DENEGADO",
                                                                    "Se requiere el rol"
                                                                        + " ADMINISTRADOR para"
                                                                        + " modificar el catálogo.",
                                                                    request));
                                                })
                                        .authenticationEntryPoint(
                                                (request, response, exception) -> {
                                                    response.setStatus(401);
                                                    response.setContentType(
                                                            MediaType
                                                                    .APPLICATION_PROBLEM_JSON_VALUE);
                                                    mapper.writeValue(
                                                            response.getOutputStream(),
                                                            ApiProblems.create(
                                                                    401,
                                                                    "AUTENTICACION_REQUERIDA",
                                                                    "Envíe un token Bearer de M1.",
                                                                    request));
                                                }))
                .addFilterBefore(
                        new JwtAuthenticationFilter(validator, mapper),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
