package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "evento_stripe")
public class EventoStripe {

    private static final int LONGITUD_MAXIMA_ERROR = 2000;

    @Id
    @Column(name = "id", nullable = false, length = 100)
    private String id;

    @Column(name = "tipo", nullable = false, length = 100)
    private String tipo;

    @Column(name = "objeto_id", nullable = false, length = 100)
    private String objetoId;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "recibido_en", nullable = false)
    private Instant recibidoEn;

    @Column(name = "procesado_en")
    private Instant procesadoEn;

    @Column(name = "intentos", nullable = false)
    private int intentos;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "ultimo_error")
    private String ultimoError;

    @Column(name = "siguiente_intento_en", nullable = false)
    private Instant siguienteIntentoEn;

    protected EventoStripe() {
    }

    public static EventoStripe registrar(String id, String tipo, String objetoId, String payload, Instant ahora) {
        if (id == null || id.isBlank() || id.length() > 100) {
            throw new IllegalArgumentException("El id del evento no es valido");
        }
        if (tipo == null || tipo.isBlank() || tipo.length() > 100) {
            throw new IllegalArgumentException("El tipo del evento no es valido");
        }
        if (objetoId == null || objetoId.isBlank() || objetoId.length() > 100) {
            throw new IllegalArgumentException("El objeto del evento no es valido");
        }
        if (payload == null || payload.isBlank()) {
            throw new IllegalArgumentException("El cuerpo del evento es obligatorio");
        }
        Instant recibido = Objects.requireNonNull(ahora, "El instante de recepcion es obligatorio");
        EventoStripe evento = new EventoStripe();
        evento.id = id.strip();
        evento.tipo = tipo.strip();
        evento.objetoId = objetoId.strip();
        evento.payload = payload;
        evento.recibidoEn = recibido;
        evento.intentos = 0;
        evento.siguienteIntentoEn = recibido;
        return evento;
    }

    public void registrarFallo(String error, Instant ahora, Duration backoff) {
        if (procesadoEn != null) {
            return;
        }
        Instant instante = Objects.requireNonNull(ahora, "El instante del fallo es obligatorio");
        Duration espera = Objects.requireNonNull(backoff, "El backoff es obligatorio");
        if (espera.isNegative() || espera.isZero()) {
            throw new IllegalArgumentException("El backoff debe ser positivo");
        }
        intentos = intentos + 1;
        ultimoError = recortar(error);
        siguienteIntentoEn = instante.plus(espera);
    }

    public void marcarProcesado(Instant ahora) {
        if (procesadoEn != null) {
            return;
        }
        procesadoEn = Objects.requireNonNull(ahora, "El instante de procesamiento es obligatorio");
        ultimoError = null;
    }

    public boolean estaProcesado() {
        return procesadoEn != null;
    }

    public boolean estaPendiente(Instant ahora, int maxIntentos) {
        return !estaProcesado() && intentos < maxIntentos && !siguienteIntentoEn.isAfter(ahora);
    }

    private static String recortar(String error) {
        if (error == null || error.isBlank()) {
            return "Error sin detalle";
        }
        String limpio = error.strip();
        return limpio.length() <= LONGITUD_MAXIMA_ERROR
                ? limpio
                : limpio.substring(0, LONGITUD_MAXIMA_ERROR);
    }

    public String getId() {
        return id;
    }

    public String getTipo() {
        return tipo;
    }

    public String getObjetoId() {
        return objetoId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getRecibidoEn() {
        return recibidoEn;
    }

    public Instant getProcesadoEn() {
        return procesadoEn;
    }

    public int getIntentos() {
        return intentos;
    }

    public String getUltimoError() {
        return ultimoError;
    }

    public Instant getSiguienteIntentoEn() {
        return siguienteIntentoEn;
    }
}
