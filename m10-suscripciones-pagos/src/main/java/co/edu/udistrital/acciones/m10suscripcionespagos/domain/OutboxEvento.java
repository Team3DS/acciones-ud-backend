package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "outbox_evento")
public class OutboxEvento {

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String payload;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "procesado_en")
    private Instant procesadoEn;

    @Column(nullable = false)
    private boolean publicado;

    @Column(nullable = false)
    private int reintentos;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "ultimo_error")
    private String ultimoError;

    protected OutboxEvento() {
    }

    public static OutboxEvento registrar(UUID id, String aggregateType, String aggregateId, String eventType,
                                         String payload, String correlationId, Instant creadoEn) {
        OutboxEvento evento = new OutboxEvento();
        evento.id = Objects.requireNonNull(id, "El id del evento es obligatorio");
        evento.aggregateType = exigirTexto(aggregateType, "El tipo de agregado es obligatorio");
        evento.aggregateId = exigirTexto(aggregateId, "El id del agregado es obligatorio");
        evento.eventType = exigirTexto(eventType, "El tipo de evento es obligatorio");
        evento.payload = exigirTexto(payload, "El payload es obligatorio");
        evento.correlationId = correlationId;
        evento.creadoEn = Objects.requireNonNull(creadoEn, "El instante de creacion es obligatorio");
        evento.publicado = false;
        evento.reintentos = 0;
        return evento;
    }

    public void publicar(Instant ahora) {
        if (publicado) {
            return;
        }
        this.publicado = true;
        this.procesadoEn = Objects.requireNonNull(ahora, "El instante de publicacion es obligatorio");
        this.ultimoError = null;
    }

    public void anotarFallo(String error) {
        this.reintentos = this.reintentos + 1;
        this.ultimoError = error;
    }

    private static String exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }

    public UUID getId() {
        return id;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getPayload() {
        return payload;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getProcesadoEn() {
        return procesadoEn;
    }

    public boolean isPublicado() {
        return publicado;
    }

    public int getReintentos() {
        return reintentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }
}
