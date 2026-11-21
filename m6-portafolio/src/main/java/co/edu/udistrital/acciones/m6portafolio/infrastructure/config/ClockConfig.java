package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock reloj() {
        return Clock.systemUTC();
    }
}
