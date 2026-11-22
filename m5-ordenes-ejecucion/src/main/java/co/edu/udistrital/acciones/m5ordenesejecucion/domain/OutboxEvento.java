package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.time.Instant;
import java.util.UUID;

public class OutboxEvento {
    private UUID id;
    private String tipoAgregado;
    private String agregadoId;
    private String tipoEvento;
    private String carga;
    private Instant creadoEn;
    private Instant publicadoEn;
    private Integer intentos;
    private String ultimoError;

    public OutboxEvento() {
        this.id = UUID.randomUUID();
        this.creadoEn = Instant.now();
        this.intentos = 0;
    }

    public OutboxEvento(UUID id, String tipoAgregado, String agregadoId, String tipoEvento,
                        String carga, Instant creadoEn, Instant publicadoEn,
                        Integer intentos, String ultimoError) {
        this.id = id != null ? id : UUID.randomUUID();
        this.tipoAgregado = tipoAgregado;
        this.agregadoId = agregadoId;
        this.tipoEvento = tipoEvento;
        this.carga = carga;
        this.creadoEn = creadoEn != null ? creadoEn : Instant.now();
        this.publicadoEn = publicadoEn;
        this.intentos = intentos != null ? intentos : 0;
        this.ultimoError = ultimoError;
    }

    public static OutboxEvento crear(String tipoAgregado, String agregadoId, String tipoEvento, String cargaJson) {
        return new OutboxEvento(UUID.randomUUID(), tipoAgregado, agregadoId, tipoEvento, cargaJson, Instant.now(), null, 0, null);
    }

    public void marcarPublicado() {
        this.publicadoEn = Instant.now();
    }

    public void registrarFallo(String error) {
        this.intentos++;
        this.ultimoError = error != null && error.length() > 500 ? error.substring(0, 500) : error;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getTipoAgregado() { return tipoAgregado; }
    public void setTipoAgregado(String tipoAgregado) { this.tipoAgregado = tipoAgregado; }
    public String getAgregadoId() { return agregadoId; }
    public void setAgregadoId(String agregadoId) { this.agregadoId = agregadoId; }
    public String getTipoEvento() { return tipoEvento; }
    public void setTipoEvento(String tipoEvento) { this.tipoEvento = tipoEvento; }
    public String getCarga() { return carga; }
    public void setCarga(String carga) { this.carga = carga; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getPublicadoEn() { return publicadoEn; }
    public void setPublicadoEn(Instant publicadoEn) { this.publicadoEn = publicadoEn; }
    public Integer getIntentos() { return intentos; }
    public void setIntentos(Integer intentos) { this.intentos = intentos; }
    public String getUltimoError() { return ultimoError; }
    public void setUltimoError(String ultimoError) { this.ultimoError = ultimoError; }
}
