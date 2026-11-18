package co.edu.udistrital.acciones.m6portafolio.domain;

import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.ReservaNoDisponibleException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.UsuarioInvalidoException;
import co.edu.udistrital.acciones.m6portafolio.domain.excepcion.VentaNoDisponibleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "posicion")
public class Posicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidad;

    @Column(name = "cantidad_reservada", nullable = false, precision = 19, scale = 8)
    private BigDecimal cantidadReservada;

    @Column(name = "costo_promedio", nullable = false, precision = 19, scale = 4)
    private BigDecimal costoPromedio;

    @Column(name = "precio_actual", precision = 19, scale = 4)
    private BigDecimal precioActual;

    @Column(name = "valor_actual", precision = 19, scale = 4)
    private BigDecimal valorActual;

    @Column(name = "pnl_no_realizado", precision = 19, scale = 4)
    private BigDecimal pnlNoRealizado;

    @Column(name = "pnl_realizado", nullable = false, precision = 19, scale = 4)
    private BigDecimal pnlRealizado;

    @Column(name = "valorado_en")
    private Instant valoradoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private Instant actualizadoEn;

    protected Posicion() {
    }

    public static Posicion abrir(Long userId, String symbol, Instant ahora) {
        if (userId == null || userId <= 0) {
            throw new UsuarioInvalidoException("El usuario debe ser positivo");
        }
        Posicion posicion = new Posicion();
        posicion.userId = userId;
        posicion.symbol = Simbolos.normalizar(symbol);
        posicion.cantidad = Cantidades.cero();
        posicion.cantidadReservada = Cantidades.cero();
        posicion.costoPromedio = Montos.cero();
        posicion.pnlRealizado = Montos.cero();
        posicion.creadoEn = Objects.requireNonNull(ahora, "El instante de creacion es obligatorio");
        posicion.actualizadoEn = ahora;
        return posicion;
    }

    public void comprar(BigDecimal cantidadComprada, BigDecimal precioCompra, Instant ocurridoEn) {
        BigDecimal compra = Cantidades.validarPositiva(cantidadComprada);
        BigDecimal precio = Montos.validarPositivo(precioCompra);
        Instant instante = Objects.requireNonNull(ocurridoEn, "El instante de la compra es obligatorio");
        BigDecimal nuevaCantidad = this.cantidad.add(compra);
        BigDecimal costoPrevio = this.costoPromedio.multiply(this.cantidad);
        BigDecimal costoCompra = precio.multiply(compra);
        this.costoPromedio = costoPrevio.add(costoCompra)
                .divide(nuevaCantidad, Montos.ESCALA, Montos.REDONDEO);
        this.cantidad = nuevaCantidad;
        if (this.valoradoEn == null || instante.isAfter(this.valoradoEn)) {
            this.precioActual = precio;
            this.valoradoEn = instante;
        }
        this.actualizadoEn = instante;
    }

    public void reservar(BigDecimal cantidadReservada, Instant ahora) {
        BigDecimal bloqueo = Cantidades.validarPositiva(cantidadReservada);
        Instant instante = Objects.requireNonNull(ahora, "El instante de la reserva es obligatorio");
        if (disponible().compareTo(bloqueo) < 0) {
            throw new ReservaNoDisponibleException("No hay acciones disponibles suficientes para reservar");
        }
        this.cantidadReservada = this.cantidadReservada.add(bloqueo);
        this.actualizadoEn = instante;
    }

    public void liberarReserva(BigDecimal cantidadLiberada, Instant ahora) {
        BigDecimal liberacion = Cantidades.validarPositiva(cantidadLiberada);
        Instant instante = Objects.requireNonNull(ahora, "El instante de la liberacion es obligatorio");
        if (this.cantidadReservada.compareTo(liberacion) < 0) {
            throw new ReservaNoDisponibleException("No hay acciones reservadas suficientes para liberar");
        }
        this.cantidadReservada = this.cantidadReservada.subtract(liberacion);
        this.actualizadoEn = instante;
    }

    public void venderReservado(BigDecimal cantidadVendida, BigDecimal precioVenta, Instant ocurridoEn) {
        BigDecimal venta = Cantidades.validarPositiva(cantidadVendida);
        if (this.cantidadReservada.compareTo(venta) < 0) {
            throw new VentaNoDisponibleException("La venta supera las acciones reservadas");
        }
        vender(venta, precioVenta, ocurridoEn);
        this.cantidadReservada = this.cantidadReservada.subtract(venta);
    }

    public void venderDisponible(BigDecimal cantidadVendida, BigDecimal precioVenta, Instant ocurridoEn) {
        BigDecimal venta = Cantidades.validarPositiva(cantidadVendida);
        if (disponible().compareTo(venta) < 0) {
            throw new VentaNoDisponibleException("No hay acciones disponibles suficientes para vender");
        }
        vender(venta, precioVenta, ocurridoEn);
    }

    private void vender(BigDecimal cantidadVendida, BigDecimal precioVenta, Instant ocurridoEn) {
        BigDecimal precio = Montos.validarPositivo(precioVenta);
        Instant instante = Objects.requireNonNull(ocurridoEn, "El instante de la venta es obligatorio");
        if (this.cantidad.compareTo(cantidadVendida) < 0) {
            throw new VentaNoDisponibleException("No hay acciones suficientes para vender");
        }
        BigDecimal pnl = precio.subtract(this.costoPromedio).multiply(cantidadVendida)
                .setScale(Montos.ESCALA, Montos.REDONDEO);
        this.cantidad = this.cantidad.subtract(cantidadVendida);
        this.pnlRealizado = this.pnlRealizado.add(pnl).setScale(Montos.ESCALA, Montos.REDONDEO);
        if (this.cantidad.signum() == 0) {
            this.costoPromedio = Montos.cero();
        }
        if (this.valoradoEn == null || instante.isAfter(this.valoradoEn)) {
            this.precioActual = precio;
            this.valoradoEn = instante;
        }
        this.actualizadoEn = instante;
    }

    public BigDecimal disponible() {
        return this.cantidad.subtract(this.cantidadReservada);
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getSymbol() {
        return symbol;
    }

    public BigDecimal getCantidad() {
        return cantidad;
    }

    public BigDecimal getCantidadReservada() {
        return cantidadReservada;
    }

    public BigDecimal getCostoPromedio() {
        return costoPromedio;
    }

    public BigDecimal getPrecioActual() {
        return precioActual;
    }

    public BigDecimal getValorActual() {
        return valorActual;
    }

    public BigDecimal getPnlNoRealizado() {
        return pnlNoRealizado;
    }

    public BigDecimal getPnlRealizado() {
        return pnlRealizado;
    }

    public Instant getValoradoEn() {
        return valoradoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public Instant getActualizadoEn() {
        return actualizadoEn;
    }
}
