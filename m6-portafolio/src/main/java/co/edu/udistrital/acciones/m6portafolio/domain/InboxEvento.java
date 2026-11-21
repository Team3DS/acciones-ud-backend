package co.edu.udistrital.acciones.m6portafolio.domain;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "inbox_evento")
public class InboxEvento {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", columnDefinition = "jsonb")
    private JsonNode payload;

    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;

    protected InboxEvento() {
    }

    public InboxEvento(String eventId, String eventType, String aggregateId, String correlationId, JsonNode payload, Instant recibidoEn) {
        this.eventId = Objects.requireNonNull(eventId, "eventId es obligatorio");
        this.eventType = Objects.requireNonNull(eventType, "eventType es obligatorio");
        this.aggregateId = aggregateId;
        this.correlationId = correlationId;
        this.payload = payload;
        this.recibidoEn = Objects.requireNonNull(recibidoEn, "recibidoEn es obligatorio");
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public Instant getRecibidoEn() {
        return recibidoEn;
    }
}