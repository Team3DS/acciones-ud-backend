package co.edu.udistrital.acciones.m6portafolio.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.MovimientoInvalidoException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "movimiento_posicion")
public class MovimientoPosicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "posicion_id", nullable = false)
    private Long posicionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimientoPosicion tipo;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidad;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal precio;

    @Column(name = "execution_id", length = 100)
    private String executionId;

    @Column(name = "order_id", length = 100)
    private String orderId;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 110)
    private String idempotencyKey;

    @Column(name = "ocurrido_en", nullable = false)
    private Instant ocurridoEn;

    protected MovimientoPosicion() {
    }

    public static MovimientoPosicion compra(Long posicionId, BigDecimal cantidad, BigDecimal precio,
                                            String executionId, String orderId, String idempotencyKey,
                                            Instant ocurridoEn) {
        return registrar(posicionId, TipoMovimientoPosicion.COMPRA, cantidad, precio, executionId, orderId,
                idempotencyKey, ocurridoEn);
    }

    public static MovimientoPosicion venta(Long posicionId, BigDecimal cantidad, BigDecimal precio,
                                           String executionId, String orderId, String idempotencyKey,
                                           Instant ocurridoEn) {
        return registrar(posicionId, TipoMovimientoPosicion.VENTA, cantidad, precio, executionId, orderId,
                idempotencyKey, ocurridoEn);
    }

    public static MovimientoPosicion reserva(Long posicionId, BigDecimal cantidad, BigDecimal precio,
                                             String orderId, String idempotencyKey, Instant ocurridoEn) {
        return registrar(posicionId, TipoMovimientoPosicion.RESERVA, cantidad, precio, null, orderId,
                idempotencyKey, ocurridoEn);
    }

    public static MovimientoPosicion liberacion(Long posicionId, BigDecimal cantidad, BigDecimal precio,
                                                String orderId, String idempotencyKey, Instant ocurridoEn) {
        return registrar(posicionId, TipoMovimientoPosicion.LIBERACION, cantidad, precio, null, orderId,
                idempotencyKey, ocurridoEn);
    }

    private static MovimientoPosicion registrar(Long posicionId, TipoMovimientoPosicion tipo,
                                                BigDecimal cantidad, BigDecimal precio, String executionId,
                                                String orderId, String idempotencyKey, Instant ocurridoEn) {
        MovimientoPosicion movimiento = new MovimientoPosicion();
        movimiento.posicionId = Objects.requireNonNull(posicionId, "La posicion es obligatoria");
        movimiento.tipo = Objects.requireNonNull(tipo, "El tipo de movimiento es obligatorio");
        movimiento.cantidad = Cantidades.validarPositiva(cantidad);
        movimiento.precio = Montos.validarPositivo(precio);
        movimiento.executionId = exigeEjecucion(tipo)
                ? exigirTexto(executionId, "La ejecucion es obligatoria")
                : executionId;
        movimiento.orderId = exigirTexto(orderId, "La orden es obligatoria");
        movimiento.idempotencyKey = exigirTexto(idempotencyKey, "La clave de idempotencia es obligatoria");
        movimiento.ocurridoEn = Objects.requireNonNull(ocurridoEn, "El instante del movimiento es obligatorio");
        return movimiento;
    }

    private static boolean exigeEjecucion(TipoMovimientoPosicion tipo) {
        return tipo == TipoMovimientoPosicion.COMPRA || tipo == TipoMovimientoPosicion.VENTA;
    }

    private static String exigirTexto(String valor, String mensaje) {
        if (valor == null || valor.isBlank()) {
            throw new MovimientoInvalidoException(mensaje);
        }
        return valor;
    }

    public Long getId() {
        return id;
    }

    public Long getPosicionId() {
        return posicionId;
    }

    public TipoMovimientoPosicion getTipo() {
        return tipo;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public String getExecutionId() {
        return executionId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getOcurridoEn() {
        return ocurridoEn;
    }
}
