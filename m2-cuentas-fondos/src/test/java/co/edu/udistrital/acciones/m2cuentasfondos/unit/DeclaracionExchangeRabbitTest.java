package co.edu.udistrital.acciones.m2cuentasfondos.unit;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.DeclaracionExchangeRabbit;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.AmqpConnectException;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.TopicExchange;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DeclaracionExchangeRabbitTest {

    private final AmqpAdmin amqpAdmin = mock(AmqpAdmin.class);
    private final TopicExchange exchange = new TopicExchange(RoutingKeys.EXCHANGE, true, false);
    private final TopicExchange dlx = new TopicExchange(RoutingKeys.EXCHANGE_DLX, true, false);
    private final DeclaracionExchangeRabbit declaracion = new DeclaracionExchangeRabbit(amqpAdmin, exchange, dlx);

    @Test
    void declaraLosExchangesAlArrancar() {
        declaracion.run(null);

        verify(amqpAdmin).declareExchange(exchange);
        verify(amqpAdmin).declareExchange(dlx);
    }

    @Test
    void noDetieneElArranqueSiElBrokerNoResponde() {
        doThrow(new AmqpConnectException(new ConnectException("sin broker")))
                .when(amqpAdmin).declareExchange(exchange);

        assertThatCode(() -> declaracion.run(null)).doesNotThrowAnyException();
        verify(amqpAdmin).declareExchange(exchange);
        verify(amqpAdmin).declareExchange(dlx);
    }
}
