package co.edu.udistrital.acciones.m1identidad.infrastructure.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    @Bean
    public Queue outboxQueue() {
        return new Queue("m1.outbox", true);
    }
}
