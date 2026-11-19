package co.edu.udistrital.acciones.m8notificaciones.infrastructure.config;

import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Configuration
public class RabbitConfig {
    public static final String QUEUE = "m8.notificaciones";
    public static final String DLQ = "m8.notificaciones.dlq";
    public static final String RETRY_QUEUE = "m8.notificaciones.retry";

    @Bean
    Declarables avisosTopology() {
        var exchange = new TopicExchange("acciones.ud", true, false);
        var dead = new TopicExchange("acciones.ud.dlx", true, false);
        var retry = new TopicExchange("acciones.ud.retry", true, false);
        var queue =
                QueueBuilder.durable(QUEUE)
                        .deadLetterExchange("acciones.ud.dlx")
                        .deadLetterRoutingKey("m8.dead")
                        .build();
        var dlq = QueueBuilder.durable(DLQ).build();
        var delayed =
                QueueBuilder.durable(RETRY_QUEUE)
                        .ttl(10000)
                        .deadLetterExchange("acciones.ud")
                        .deadLetterRoutingKey("m8.retry")
                        .build();
        var elements = new ArrayList<Declarable>();
        elements.add(exchange);
        elements.add(dead);
        elements.add(retry);
        elements.add(queue);
        elements.add(dlq);
        elements.add(delayed);
        elements.add(BindingBuilder.bind(dlq).to(dead).with("m8.dead"));
        elements.add(BindingBuilder.bind(delayed).to(retry).with("m8.retry"));
        elements.add(BindingBuilder.bind(queue).to(exchange).with("m8.retry"));
        for (String key :
                new String[] {
                    "m1.usuario.registrado",
                    "m1.mfa.solicitado",
                    "m1.verificacion.solicitada",
                    "m1.recuperacion.solicitada",
                    "m2.movimiento.creado",
                    "m2.fondos.reservados",
                    "m2.fondos.confirmados",
                    "m2.fondos.liberados",
                    "m4.mercado.abierto",
                    "m4.mercado.cerrado",
                    "m4.mercado.horario.cambiado",
                    "m5.orden.creada",
                    "m5.orden.ejecutada",
                    "m5.orden.cancelada",
                    "m5.orden.rechazada",
                    "m5.orden.actualizada",
                    "m10.suscripcion.activada",
                    "m10.suscripcion.cancelada",
                    "m10.suscripcion.vencida",
                    "m10.pago.fallido"
                }) {
            elements.add(BindingBuilder.bind(queue).to(exchange).with(key));
        }
        return new Declarables(elements);
    }
}
