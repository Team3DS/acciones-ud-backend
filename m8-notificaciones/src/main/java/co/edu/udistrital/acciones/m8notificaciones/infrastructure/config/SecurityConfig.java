package co.edu.udistrital.acciones.m8notificaciones.infrastructure.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.crypto.SecretKey;

@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain security(HttpSecurity http, @Value("${app.jwt.secret}") String secret)
            throws Exception {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return http.csrf(csrf -> csrf.disable())
                .sessionManagement(
                        session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(
                        auth ->
                                auth.requestMatchers(
                                                "/actuator/health",
                                                "/actuator/health/**",
                                                "/v3/api-docs/**",
                                                "/swagger-ui/**",
                                                "/swagger-ui.html")
                                        .permitAll()
                                        .anyRequest()
                                        .authenticated())
                .exceptionHandling(
                        errors ->
                                errors.authenticationEntryPoint(
                                        (request, response, exception) -> unauthorized(response)))
                .addFilterBefore(new JwtFilter(key), UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    private static void unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setContentType("application/problem+json");
        response.getWriter().write("{\"status\":401,\"title\":\"Autenticación requerida\"}");
    }

    private static final class JwtFilter extends OncePerRequestFilter {
        private final SecretKey key;

        private JwtFilter(SecretKey key) {
            this.key = key;
        }

        @Override
        protected void doFilterInternal(
                HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            String authorization = request.getHeader("Authorization");
            if (authorization != null) {
                try {
                    if (!authorization.startsWith("Bearer ") || authorization.length() > 8192) {
                        throw new IllegalArgumentException();
                    }
                    Claims claims =
                            Jwts.parser()
                                    .verifyWith(key)
                                    .build()
                                    .parseSignedClaims(authorization.substring(7))
                                    .getPayload();
                    long user = Long.parseLong(claims.getSubject());
                    if (user <= 0 || claims.getExpiration() == null) {
                        throw new IllegalArgumentException();
                    }
                    SecurityContextHolder.getContext()
                            .setAuthentication(
                                    new UsernamePasswordAuthenticationToken(
                                            Long.toString(user), null, List.of()));
                } catch (JwtException | IllegalArgumentException exception) {
                    unauthorized(response);
                    return;
                }
            }
            chain.doFilter(request, response);
        }
    }
}
