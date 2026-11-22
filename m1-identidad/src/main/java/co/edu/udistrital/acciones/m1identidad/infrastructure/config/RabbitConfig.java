package co.edu.udistrital.acciones.m1identidad.infrastructure.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    /** Exchange de eventos compartido. Se declara con los mismos parámetros que en M8 (durable, sin autoborrado). */
    public static final String EVENTS_EXCHANGE = "acciones.ud";

    @Bean
    public Queue outboxQueue() {
        return new Queue("m1.outbox", true);
    }

    @Bean
    public TopicExchange eventsExchange() {
        return new TopicExchange(EVENTS_EXCHANGE, true, false);
    }
}
