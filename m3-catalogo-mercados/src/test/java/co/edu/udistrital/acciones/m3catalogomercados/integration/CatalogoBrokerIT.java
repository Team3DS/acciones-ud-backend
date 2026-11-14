package co.edu.udistrital.acciones.m3catalogomercados.integration;

import static org.assertj.core.api.Assertions.*;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.in.scheduling.CatalogoOutboxRelay;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.adapter.out.messaging.RabbitCatalogoPublisher;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Tag("broker")
class CatalogoBrokerIT extends CatalogoIntegrationSupport {
    @Autowired RabbitTemplate rabbitTemplate;
    @Autowired PlatformTransactionManager manager;

    @Test
    void confirmaEntregaRealAntesDeMarcarOutboxPublicado() {
        RabbitAdmin admin = new RabbitAdmin(rabbitTemplate.getConnectionFactory());
        TopicExchange exchange = new TopicExchange("acciones.ud", true, false);
        Queue queue = new Queue("m3-test-" + UUID.randomUUID(), false, false, true);
        admin.declareExchange(exchange);
        admin.declareQueue(queue);
        admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with("m3.instrumento.creado"));
        UUID id = UUID.randomUUID();
        String json =
                "{\"eventId\":\""
                        + id
                        + "\",\"eventType\":\"m3.instrumento.creado\",\"payload\":{\"symbol\":\"TEST\"}}";
        jdbc.update(
                "INSERT INTO outbox_evento(id,routing_key,body) VALUES(?,?,CAST(? AS jsonb))",
                id,
                "m3.instrumento.creado",
                json);
        try {
            new CatalogoOutboxRelay(jdbc, new RabbitCatalogoPublisher(rabbitTemplate), manager)
                    .publicarPendientes();
            var received = rabbitTemplate.receive(queue.getName(), 10000);
            assertThat(received).isNotNull();
            assertThat(new String(received.getBody(), StandardCharsets.UTF_8))
                    .contains(id.toString());
            assertThat(
                            jdbc.queryForObject(
                                    "SELECT publicado_en IS NOT NULL FROM outbox_evento WHERE id=?",
                                    Boolean.class,
                                    id))
                    .isTrue();
        } finally {
            admin.deleteQueue(queue.getName());
        }
    }
}
