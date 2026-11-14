package co.edu.udistrital.acciones.m3catalogomercados.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.out.messaging.RabbitCatalogoPublisher;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.UUID;

class RabbitCatalogoPublisherTest {
    @Test
    void esperaConfirmacionPositivaDelBroker() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(
                        call -> {
                            ((CorrelationData) call.getArgument(3))
                                    .getFuture()
                                    .complete(new CorrelationData.Confirm(true, null));
                            return null;
                        })
                .when(rabbit)
                .send(
                        eq("acciones.ud"),
                        anyString(),
                        any(Message.class),
                        any(CorrelationData.class));
        assertThatCode(
                        () ->
                                new RabbitCatalogoPublisher(rabbit)
                                        .publicar(UUID.randomUUID(), "m3.instrumento.creado", "{}"))
                .doesNotThrowAnyException();
    }

    @Test
    void noConfundeNackNiMensajeSinRutaConPublicacionCorrecta() {
        for (boolean returned : new boolean[] {false, true}) {
            RabbitTemplate rabbit = mock(RabbitTemplate.class);
            doAnswer(
                            call -> {
                                CorrelationData data = call.getArgument(3);
                                if (returned)
                                    data.setReturned(
                                            new ReturnedMessage(
                                                    call.getArgument(2),
                                                    312,
                                                    "NO_ROUTE",
                                                    "acciones.ud",
                                                    "m3.test"));
                                data.getFuture()
                                        .complete(new CorrelationData.Confirm(returned, null));
                                return null;
                            })
                    .when(rabbit)
                    .send(
                            eq("acciones.ud"),
                            anyString(),
                            any(Message.class),
                            any(CorrelationData.class));
            assertThatThrownBy(
                            () ->
                                    new RabbitCatalogoPublisher(rabbit)
                                            .publicar(UUID.randomUUID(), "m3.test", "{}"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void propagaFalloDeConfirmacion() {
        RabbitTemplate rabbit = mock(RabbitTemplate.class);
        doAnswer(
                        call -> {
                            ((CorrelationData) call.getArgument(3))
                                    .getFuture()
                                    .completeExceptionally(new IllegalStateException("offline"));
                            return null;
                        })
                .when(rabbit)
                .send(
                        eq("acciones.ud"),
                        anyString(),
                        any(Message.class),
                        any(CorrelationData.class));
        assertThatThrownBy(
                        () ->
                                new RabbitCatalogoPublisher(rabbit)
                                        .publicar(UUID.randomUUID(), "m3.test", "{}"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("confirmación");
    }
}
