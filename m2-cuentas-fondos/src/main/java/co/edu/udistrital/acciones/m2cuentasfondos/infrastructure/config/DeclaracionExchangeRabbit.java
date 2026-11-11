package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DeclaracionExchangeRabbit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeclaracionExchangeRabbit.class);

    private final AmqpAdmin amqpAdmin;
    private final TopicExchange accionesExchange;

    public DeclaracionExchangeRabbit(AmqpAdmin amqpAdmin, TopicExchange accionesExchange) {
        this.amqpAdmin = amqpAdmin;
        this.accionesExchange = accionesExchange;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            amqpAdmin.declareExchange(accionesExchange);
        } catch (RuntimeException ex) {
            log.warn("No se pudo declarar el exchange {} al arrancar; se reintentará en la próxima conexión",
                    RoutingKeys.EXCHANGE, ex);
        }
    }
}
