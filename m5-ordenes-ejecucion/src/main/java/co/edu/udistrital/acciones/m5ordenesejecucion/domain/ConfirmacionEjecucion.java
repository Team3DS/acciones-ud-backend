package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import java.math.BigDecimal;
import java.time.Instant;

public class ConfirmacionEjecucion {
    private Long ejecucionOrdenId;
    private ServicioParticipante servicioParticipante;
    private BigDecimal precioPromedioCosto;
    private Instant confirmadaEn;

    public ConfirmacionEjecucion() {}

    public ConfirmacionEjecucion(Long ejecucionOrdenId, ServicioParticipante servicioParticipante,
                                 BigDecimal precioPromedioCosto, Instant confirmadaEn) {
        this.ejecucionOrdenId = ejecucionOrdenId;
        this.servicioParticipante = servicioParticipante;
        this.precioPromedioCosto = precioPromedioCosto;
        this.confirmadaEn = confirmadaEn != null ? confirmadaEn : Instant.now();
    }

    public Long getEjecucionOrdenId() { return ejecucionOrdenId; }
    public void setEjecucionOrdenId(Long ejecucionOrdenId) { this.ejecucionOrdenId = ejecucionOrdenId; }
    public ServicioParticipante getServicioParticipante() { return servicioParticipante; }
    public void setServicioParticipante(ServicioParticipante servicioParticipante) { this.servicioParticipante = servicioParticipante; }
    public BigDecimal getPrecioPromedioCosto() { return precioPromedioCosto; }
    public void setPrecioPromedioCosto(BigDecimal precioPromedioCosto) { this.precioPromedioCosto = precioPromedioCosto; }
    public Instant getConfirmadaEn() { return confirmadaEn; }
    public void setConfirmadaEn(Instant confirmadaEn) { this.confirmadaEn = confirmadaEn; }
}
