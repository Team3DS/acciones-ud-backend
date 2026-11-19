package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class ConfirmacionEjecucion {
    private Long id;
    private Long ejecucionOrdenId;
    private String servicioParticipante;
    private BigDecimal precioPromedioCosto;
    private Instant confirmadoEn;

    public ConfirmacionEjecucion() {}

    public ConfirmacionEjecucion(Long id, Long ejecucionOrdenId, String servicioParticipante,
                                 BigDecimal precioPromedioCosto, Instant confirmadoEn) {
        this.id = id;
        this.ejecucionOrdenId = ejecucionOrdenId;
        this.servicioParticipante = servicioParticipante;
        this.precioPromedioCosto = precioPromedioCosto;
        this.confirmadoEn = confirmadoEn;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getEjecucionOrdenId() { return ejecucionOrdenId; }
    public void setEjecucionOrdenId(Long ejecucionOrdenId) { this.ejecucionOrdenId = ejecucionOrdenId; }
    public String getServicioParticipante() { return servicioParticipante; }
    public void setServicioParticipante(String servicioParticipante) { this.servicioParticipante = servicioParticipante; }
    public BigDecimal getPrecioPromedioCosto() { return precioPromedioCosto; }
    public void setPrecioPromedioCosto(BigDecimal precioPromedioCosto) { this.precioPromedioCosto = precioPromedioCosto; }
    public Instant getConfirmadoEn() { return confirmadoEn; }
    public void setConfirmadoEn(Instant confirmadoEn) { this.confirmadoEn = confirmadoEn; }
}
