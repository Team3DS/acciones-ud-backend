package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class ConfirmacionEjecucionId implements Serializable {

    @Column(name = "ejecucion_orden_id", nullable = false)
    private Long ejecucionOrdenId;

    @Enumerated(EnumType.STRING)
    @Column(name = "servicio_participante", nullable = false, length = 30)
    private ServicioParticipante servicioParticipante;

    public ConfirmacionEjecucionId() {}

    public ConfirmacionEjecucionId(Long ejecucionOrdenId, ServicioParticipante servicioParticipante) {
        this.ejecucionOrdenId = ejecucionOrdenId;
        this.servicioParticipante = servicioParticipante;
    }

    public Long getEjecucionOrdenId() { return ejecucionOrdenId; }
    public void setEjecucionOrdenId(Long ejecucionOrdenId) { this.ejecucionOrdenId = ejecucionOrdenId; }
    public ServicioParticipante getServicioParticipante() { return servicioParticipante; }
    public void setServicioParticipante(ServicioParticipante servicioParticipante) { this.servicioParticipante = servicioParticipante; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ConfirmacionEjecucionId that)) return false;
        return Objects.equals(ejecucionOrdenId, that.ejecucionOrdenId) && servicioParticipante == that.servicioParticipante;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ejecucionOrdenId, servicioParticipante);
    }
}
