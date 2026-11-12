package co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DeclaracionExchangeRabbit implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DeclaracionExchangeRabbit.class);

    private final AmqpAdmin amqpAdmin;
    private final TopicExchange accionesExchange;
    private final TopicExchange accionesDlx;

    public DeclaracionExchangeRabbit(AmqpAdmin amqpAdmin,
                                     @Qualifier("accionesExchange") TopicExchange accionesExchange,
                                     @Qualifier("accionesDlx") TopicExchange accionesDlx) {
        this.amqpAdmin = amqpAdmin;
        this.accionesExchange = accionesExchange;
        this.accionesDlx = accionesDlx;
    }

    @Override
    public void run(ApplicationArguments args) {
        declarar(accionesExchange, RoutingKeys.EXCHANGE);
        declarar(accionesDlx, RoutingKeys.EXCHANGE_DLX);
    }

    private void declarar(TopicExchange exchange, String nombre) {
        try {
            amqpAdmin.declareExchange(exchange);
        } catch (RuntimeException ex) {
            log.warn("No se pudo declarar el exchange {} al arrancar; se reintentará en la próxima conexión",
                    nombre, ex);
        }
    }
}
