package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange accionesExchange() {
        return new TopicExchange(RoutingKeys.EXCHANGE, true, false);
    }

    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
