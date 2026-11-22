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
    @Column(nullable = false)
    private UUID id;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 100)
    private String aggregateId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(nullable = false)
    private String payload;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(nullable = false)
    private boolean publicado;

    @Column(name = "procesado_en")
    private Instant procesadoEn;

    @Column(nullable = false)
    private int reintentos;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "ultimo_error")
    private String ultimoError;

    protected OutboxEvento() {
    }

    public static OutboxEvento pendiente(UUID id, String aggregateType, String aggregateId,
                                         String eventType, String payload,
                                         String correlationId, Instant creadoEn) {
        OutboxEvento nuevo = new OutboxEvento();
        nuevo.id = requerido(id, "El id del evento es obligatorio");
        nuevo.eventType = texto(agregar(eventType, "El tipo de evento es obligatorio"));
        nuevo.aggregateType = texto(agregar(aggregateType, "El tipo de agregado es obligatorio"));
        nuevo.aggregateId = texto(agregar(aggregateId, "El id del agregado es obligatorio"));
        nuevo.payload = texto(agregar(payload, "El payload es obligatorio"));
        nuevo.correlationId = correlationId;
        nuevo.creadoEn = requerido(creadoEn, "El instante de creacion es obligatorio");
        nuevo.publicado = false;
        nuevo.reintentos = 0;
        return nuevo;
    }

    public void confirmarPublicacion(Instant procesadoEn) {
        if (this.publicado) {
            return;
        }
        this.publicado = true;
        this.procesadoEn = requerido(procesadoEn, "El instante de publicacion es obligatorio");
        this.ultimoError = null;
    }

    public void registrarReintento(String detalleError) {
        this.reintentos += 1;
        this.ultimoError = detalleError;
    }

    private static String agregar(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException(mensaje);
        }
        return valor;
    }

    private static String texto(String valor) {
        return valor;
    }

    private static <T> T requerido(T valor, String mensaje) {
        return Objects.requireNonNull(valor, mensaje);
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public String getAggregateId() {
        return aggregateId;
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

    public boolean isPublicado() {
        return publicado;
    }

    public Instant getProcesadoEn() {
        return procesadoEn;
    }

    public int getReintentos() {
        return reintentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }
}
