package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "acciones.ud";
    public static final String EXCHANGE_DLX = "acciones.ud.dlx";
    public static final String COLA_ORDENES = "m5.ordenes.queue";
    public static final String COLA_ORDENES_DLQ = "m5.ordenes.dlq";

    public static final String ROUTING_M4_MERCADO_ABIERTO = "m4.mercado.abierto";
    public static final String ROUTING_M2_FONDOS_CONFIRMADOS = "m2.fondos.confirmados";
    public static final String ROUTING_M6_POSICIONES_ACTUALIZADAS = "m6.posiciones.actualizadas";

    public static final String RK_ORDEN_CREADA = "m5.orden.creada";
    public static final String RK_ORDEN_EJECUTADA = "m5.orden.ejecutada";
    public static final String RK_ORDEN_CANCELADA = "m5.orden.cancelada";
    public static final String RK_ORDEN_RECHAZADA = "m5.orden.rechazada";

    @Bean
    public TopicExchange accionesExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange accionesDlx() {
        return new TopicExchange(EXCHANGE_DLX, true, false);
    }

    @Bean
    public Declarables ordenesTopology(TopicExchange accionesExchange, TopicExchange accionesDlx) {
        Queue cola = QueueBuilder.durable(COLA_ORDENES)
                .deadLetterExchange(EXCHANGE_DLX)
                .deadLetterRoutingKey(COLA_ORDENES_DLQ)
                .build();
        Queue dlq = QueueBuilder.durable(COLA_ORDENES_DLQ).build();

        var elements = new ArrayList<Declarable>();
        elements.add(cola);
        elements.add(dlq);
        elements.add(BindingBuilder.bind(cola).to(accionesExchange).with(ROUTING_M4_MERCADO_ABIERTO));
        elements.add(BindingBuilder.bind(cola).to(accionesExchange).with(ROUTING_M2_FONDOS_CONFIRMADOS));
        elements.add(BindingBuilder.bind(cola).to(accionesExchange).with(ROUTING_M6_POSICIONES_ACTUALIZADAS));
        elements.add(BindingBuilder.bind(dlq).to(accionesDlx).with(COLA_ORDENES_DLQ));

        return new Declarables(elements);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public SimpleRabbitListenerContainerFactory m5ListenerContainerFactory(
            SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        configurer.configure(factory, connectionFactory);
        factory.setMessageConverter(new SimpleMessageConverter());
        return factory;
    }
}
