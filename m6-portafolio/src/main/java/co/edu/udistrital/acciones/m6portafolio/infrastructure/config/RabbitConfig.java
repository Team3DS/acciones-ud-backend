package co.edu.udistrital.acciones.m6portafolio.infrastructure.config;

import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange accionesExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange accionesDlx() {
        return new TopicExchange(RoutingKeys.EXCHANGE_DLX, true, false);
    }

    @Bean
    public Declarables colasDePortafolio(TopicExchange accionesExchange, TopicExchange accionesDlx) {
        Queue cola = QueueBuilder.durable(RoutingKeys.COLA_POSICIONES)
                .deadLetterExchange(RoutingKeys.EXCHANGE_DLX)
                .deadLetterRoutingKey(RoutingKeys.COLA_POSICIONES_DLQ)
                .build();
        Queue colaMuerta = QueueBuilder.durable(RoutingKeys.COLA_POSICIONES_DLQ).build();
        return new Declarables(cola, colaMuerta,
                BindingBuilder.bind(cola).to(accionesExchange).with(RoutingKeys.M5_ORDEN_EJECUTADA),
                BindingBuilder.bind(cola).to(accionesExchange).with(RoutingKeys.M5_ORDEN_CANCELADA),
                BindingBuilder.bind(cola).to(accionesExchange).with(RoutingKeys.M5_ORDEN_RECHAZADA),
                BindingBuilder.bind(colaMuerta).to(accionesDlx).with(RoutingKeys.COLA_POSICIONES_DLQ));
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory portafolioListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        return factory;
    }
}
