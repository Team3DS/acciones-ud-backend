package co.edu.udistrital.acciones.m4datosmercado.infrastructure.config;

import co.edu.udistrital.acciones.m4datosmercado.application.dto.RoutingKeys;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Map;

@Configuration
@EnableScheduling
public class RabbitConfig {

    @Bean
    public TopicExchange accionesExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE, true, false);
    }

    @Bean
    public TopicExchange dlxExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE_DLX, true, false);
    }

    @Bean
    public Queue m4EventosQueue() {
        return new Queue(RoutingKeys.COLA_M4_EVENTOS, true, false, false,
                Map.of("x-dead-letter-exchange", RoutingKeys.EXCHANGE_DLX,
                       "x-dead-letter-routing-key", RoutingKeys.COLA_M4_EVENTOS_DLQ));
    }

    @Bean
    public Queue m4EventosDlq() {
        return new Queue(RoutingKeys.COLA_M4_EVENTOS_DLQ, true, false, false);
    }

    @Bean
    public Binding bindingInstrumentoCreado(TopicExchange accionesExchange, Queue m4EventosQueue) {
        return BindingBuilder.bind(m4EventosQueue).to(accionesExchange).with(RoutingKeys.M3_INSTRUMENTO_CREADO);
    }

    @Bean
    public Binding bindingInstrumentoActualizado(TopicExchange accionesExchange, Queue m4EventosQueue) {
        return BindingBuilder.bind(m4EventosQueue).to(accionesExchange).with(RoutingKeys.M3_INSTRUMENTO_ACTUALIZADO);
    }

    @Bean
    public Binding bindingMercadoCreado(TopicExchange accionesExchange, Queue m4EventosQueue) {
        return BindingBuilder.bind(m4EventosQueue).to(accionesExchange).with(RoutingKeys.M3_MERCADO_CREADO);
    }

    @Bean
    public Binding bindingMercadoActualizado(TopicExchange accionesExchange, Queue m4EventosQueue) {
        return BindingBuilder.bind(m4EventosQueue).to(accionesExchange).with(RoutingKeys.M3_MERCADO_ACTUALIZADO);
    }

    @Bean
    public Binding bindingDlq(TopicExchange dlxExchange, Queue m4EventosDlq) {
        return BindingBuilder.bind(m4EventosDlq).to(dlxExchange).with(RoutingKeys.COLA_M4_EVENTOS_DLQ);
    }
}