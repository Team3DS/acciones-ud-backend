package co.edu.udistrital.acciones.m10suscripcionespagos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "recibo")
public class Recibo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "suscripcion_id", nullable = false)
    private Long suscripcionId;

    @Column(name = "stripe_invoice_id", nullable = false, length = 100)
    private String stripeInvoiceId;

    @Column(length = 50)
    private String numero;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monto;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(nullable = false, length = 3)
    private String moneda;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoRecibo estado;

    @Column(nullable = false)
    private int intento;

    @Column(name = "periodo_inicio")
    private Instant periodoInicio;

    @Column(name = "periodo_fin")
    private Instant periodoFin;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "url_recibo")
    private String urlRecibo;

    @JdbcTypeCode(SqlTypes.LONGVARCHAR)
    @Column(name = "url_pdf")
    private String urlPdf;

    @Column(name = "emitido_en", nullable = false)
    private Instant emitidoEn;

    @Column(name = "pagado_en")
    private Instant pagadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Recibo() {
    }

    public static Recibo crear(Long suscripcionId, String stripeInvoiceId, Dinero monto, EstadoRecibo estado,
                               int intento, Instant emitidoEn, Instant ahora) {
        Recibo recibo = new Recibo();
        recibo.suscripcionId = Objects.requireNonNull(suscripcionId, "La suscripcion es obligatoria");
        if (stripeInvoiceId == null || stripeInvoiceId.isBlank()) {
            throw new IllegalArgumentException("El recibo de Stripe es obligatorio");
        }
        recibo.stripeInvoiceId = stripeInvoiceId.strip();
        recibo.aplicarMonto(monto);
        recibo.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        recibo.intento = exigirNoNegativo(intento);
        recibo.emitidoEn = Objects.requireNonNull(emitidoEn, "La emision es obligatoria");
        recibo.actualizadoEn = Objects.requireNonNull(ahora, "El instante es obligatorio");
        return recibo;
    }

    public boolean actualizar(Dinero monto, EstadoRecibo estado, int intento, String numero,
                              Instant periodoInicio, Instant periodoFin, Instant pagadoEn,
                              String urlRecibo, String urlPdf, Instant ahora) {
        int intentoAnterior = this.intento;
        aplicarMonto(monto);
        this.estado = Objects.requireNonNull(estado, "El estado es obligatorio");
        this.intento = exigirNoNegativo(intento);
        this.numero = numero;
        this.periodoInicio = periodoInicio;
        this.periodoFin = periodoFin;
        this.pagadoEn = pagadoEn;
        this.urlRecibo = urlRecibo;
        this.urlPdf = urlPdf;
        this.actualizadoEn = Objects.requireNonNull(ahora, "El instante es obligatorio");
        return this.estado == EstadoRecibo.FALLIDO && this.intento > intentoAnterior;
    }

    private void aplicarMonto(Dinero monto) {
        Dinero valor = Objects.requireNonNull(monto, "El monto es obligatorio");
        this.monto = valor.monto();
        this.moneda = valor.moneda();
    }

    private static int exigirNoNegativo(int intento) {
        if (intento < 0) {
            throw new IllegalArgumentException("El intento no puede ser negativo");
        }
        return intento;
    }

    public Long getId() {
        return id;
    }

    public Long getSuscripcionId() {
        return suscripcionId;
    }

    public String getStripeInvoiceId() {
        return stripeInvoiceId;
    }

    public String getNumero() {
        return numero;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public String getMoneda() {
        return moneda;
    }

    public EstadoRecibo getEstado() {
        return estado;
    }

    public int getIntento() {
        return intento;
    }

    public Instant getPeriodoInicio() {
        return periodoInicio;
    }

    public Instant getPeriodoFin() {
        return periodoFin;
    }

    public String getUrlRecibo() {
        return urlRecibo;
    }

    public String getUrlPdf() {
        return urlPdf;
    }

    public Instant getEmitidoEn() {
        return emitidoEn;
    }

    public Instant getPagadoEn() {
        return pagadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
