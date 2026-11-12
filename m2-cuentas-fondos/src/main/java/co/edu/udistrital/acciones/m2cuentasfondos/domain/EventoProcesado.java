package co.edu.udistrital.acciones.m2cuentasfondos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "evento_procesado")
public class EventoProcesado {

    @Id
    @Column(name = "event_id", length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "procesado_en", nullable = false)
    private Instant procesadoEn;

    protected EventoProcesado() {
    }

    public static EventoProcesado crear(String eventId, String eventType, Instant procesadoEn) {
        EventoProcesado evento = new EventoProcesado();
        evento.eventId = Objects.requireNonNull(eventId, "El eventId es obligatorio");
        evento.eventType = Objects.requireNonNull(eventType, "El eventType es obligatorio");
        evento.procesadoEn = Objects.requireNonNull(procesadoEn, "El instante de procesamiento es obligatorio");
        return evento;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcesadoEn() {
        return procesadoEn;
    }
}
