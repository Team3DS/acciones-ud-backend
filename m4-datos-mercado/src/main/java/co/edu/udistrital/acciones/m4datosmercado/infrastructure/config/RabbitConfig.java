package co.edu.udistrital.acciones.m4datosmercado.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "acciones.ud";
    public static final String QUEUE_CATALOGO = "m4.catalogo.eventos";

    @Bean
    public TopicExchange accionesExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue colaCatalogoEventos() {
        return new Queue(QUEUE_CATALOGO, true);
    }

    @Bean
    public Binding bindingInstrumentoCreado(Queue colaCatalogoEventos, TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaCatalogoEventos).to(accionesExchange).with("m3.instrumento.creado");
    }

    @Bean
    public Binding bindingInstrumentoActualizado(Queue colaCatalogoEventos, TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaCatalogoEventos).to(accionesExchange).with("m3.instrumento.actualizado");
    }

    @Bean
    public Binding bindingMercadoCreado(Queue colaCatalogoEventos, TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaCatalogoEventos).to(accionesExchange).with("m3.mercado.creado");
    }

    @Bean
    public Binding bindingMercadoActualizado(Queue colaCatalogoEventos, TopicExchange accionesExchange) {
        return BindingBuilder.bind(colaCatalogoEventos).to(accionesExchange).with("m3.mercado.actualizado");
    }
}
