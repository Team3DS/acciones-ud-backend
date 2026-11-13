package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class RabbitConfig {
    @Bean
    public TopicExchange catalogoExchange() {
        return new TopicExchange("acciones.ud", true, false);
    }
}
