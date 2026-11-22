package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "confirmacion_ejecucion")
public class ConfirmacionEjecucionEntity {

    @EmbeddedId
    private ConfirmacionEjecucionId id;

    @Column(name = "precio_promedio_costo", precision = 18, scale = 4)
    private BigDecimal precioPromedioCosto;

    @Column(name = "confirmada_en", nullable = false)
    private Instant confirmadaEn;

    public ConfirmacionEjecucionEntity() {}

    public static ConfirmacionEjecucionEntity fromDomain(ConfirmacionEjecucion domain) {
        ConfirmacionEjecucionEntity entity = new ConfirmacionEjecucionEntity();
        entity.id = new ConfirmacionEjecucionId(domain.getEjecucionOrdenId(), domain.getServicioParticipante());
        entity.precioPromedioCosto = domain.getPrecioPromedioCosto();
        entity.confirmadaEn = domain.getConfirmadaEn() != null ? domain.getConfirmadaEn() : Instant.now();
        return entity;
    }

    public ConfirmacionEjecucion toDomain() {
        return new ConfirmacionEjecucion(
                this.id.getEjecucionOrdenId(),
                this.id.getServicioParticipante(),
                this.precioPromedioCosto,
                this.confirmadaEn
        );
    }

    // Acceso a columnas (Hibernate usa acceso por campo; la conversión con el dominio está en fromDomain/toDomain).
    public ConfirmacionEjecucionId getId() { return id; }
    public BigDecimal getPrecioPromedioCosto() { return precioPromedioCosto; }
    public Instant getConfirmadaEn() { return confirmadaEn; }

    public void setId(ConfirmacionEjecucionId id) { this.id = id; }
    public void setPrecioPromedioCosto(BigDecimal precioPromedioCosto) { this.precioPromedioCosto = precioPromedioCosto; }
    public void setConfirmadaEn(Instant confirmadaEn) { this.confirmadaEn = confirmadaEn; }
}
