package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
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
    public Queue colaMovimientos() {
        return QueueBuilder.durable(RoutingKeys.COLA_MOVIMIENTOS)
                .deadLetterExchange(RoutingKeys.EXCHANGE_DLX)
                .deadLetterRoutingKey(RoutingKeys.COLA_MOVIMIENTOS_DLQ)
                .build();
    }

    @Bean
    public Queue colaMovimientosDlq() {
        return QueueBuilder.durable(RoutingKeys.COLA_MOVIMIENTOS_DLQ).build();
    }

    @Bean
    public Binding bindingOrdenEjecutada(
            @Qualifier("colaMovimientos") Queue colaMovimientos,
            @Qualifier("accionesExchange") TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaMovimientos).to(accionesExchange)
                .with(RoutingKeys.M5_ORDEN_EJECUTADA);
    }

    @Bean
    public Binding bindingOrdenCancelada(
            @Qualifier("colaMovimientos") Queue colaMovimientos,
            @Qualifier("accionesExchange") TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaMovimientos).to(accionesExchange)
                .with(RoutingKeys.M5_ORDEN_CANCELADA);
    }

    @Bean
    public Binding bindingOrdenRechazada(
            @Qualifier("colaMovimientos") Queue colaMovimientos,
            @Qualifier("accionesExchange") TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaMovimientos).to(accionesExchange)
                .with(RoutingKeys.M5_ORDEN_RECHAZADA);
    }

    @Bean
    public Binding bindingSuscripcionActivada(
            @Qualifier("colaMovimientos") Queue colaMovimientos,
            @Qualifier("accionesExchange") TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaMovimientos).to(accionesExchange)
                .with(RoutingKeys.M10_SUSCRIPCION_ACTIVADA);
    }

    @Bean
    public Binding bindingColaDlq(
            @Qualifier("colaMovimientosDlq") Queue colaMovimientosDlq,
            @Qualifier("accionesDlx") TopicExchange accionesDlx) {
        return BindingBuilder.bind(colaMovimientosDlq).to(accionesDlx)
                .with(RoutingKeys.COLA_MOVIMIENTOS_DLQ);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory fondosListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer,
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        return factory;
    }
}
