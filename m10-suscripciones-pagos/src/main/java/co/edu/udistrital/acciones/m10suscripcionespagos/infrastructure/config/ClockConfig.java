package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock relojUtc() {
        return Clock.systemUTC();
    }
}
