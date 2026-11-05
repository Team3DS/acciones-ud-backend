package co.edu.udistrital.acciones.m1identidad.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/health").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMINISTRADOR")
                .requestMatchers("/api/v1/directivo/**").hasRole("DIRECTIVO")
                .requestMatchers("/api/v1/comisionista/**").hasRole("COMISIONISTA")
                .requestMatchers("/api/v1/cumplimiento/**").hasRole("CUMPLIMIENTO")
                .requestMatchers("/api/v1/inversionista/**").hasRole("INVERSIONISTA")
                .anyRequest().authenticated()
            );
        return http.build();
    }
}