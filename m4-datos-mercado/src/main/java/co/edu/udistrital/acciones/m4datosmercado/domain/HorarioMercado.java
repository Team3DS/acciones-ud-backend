package co.edu.udistrital.acciones.m4datosmercado.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Entity
@Table(name = "horario_mercado", uniqueConstraints = @UniqueConstraint(columnNames = {"mercado_mic", "dia_semana"}))
public class HorarioMercado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mercado_mic", nullable = false, length = 20)
    private String mercadoMic;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(name = "apertura", nullable = false)
    private LocalTime apertura;

    @Column(name = "cierre", nullable = false)
    private LocalTime cierre;

    @Column(name = "pre_market")
    private LocalTime preMarket;

    @Column(name = "after_hours")
    private LocalTime afterHours;

    protected HorarioMercado() {}

    public HorarioMercado(String mercadoMic, int diaSemana, LocalTime apertura, LocalTime cierre,
                          LocalTime preMarket, LocalTime afterHours) {
        this.mercadoMic = mercadoMic;
        this.diaSemana = diaSemana;
        this.apertura = apertura;
        this.cierre = cierre;
        this.preMarket = preMarket;
        this.afterHours = afterHours;
    }

    public boolean esDiaLaborable(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneId.of("America/New_York"));
        return zdt.getDayOfWeek().getValue() == diaSemana;
    }

    public EstadoMercado obtenerEstado(Instant instant) {
        ZonedDateTime zdt = instant.atZone(ZoneId.of("America/New_York"));
        LocalTime time = zdt.toLocalTime();

        if (zdt.getDayOfWeek().getValue() != diaSemana) {
            return EstadoMercado.CERRADO;
        }

        if (preMarket != null && !time.isBefore(preMarket) && time.isBefore(apertura)) {
            return EstadoMercado.PRE_MARKET;
        }

        if (!time.isBefore(apertura) && time.isBefore(cierre)) {
            return EstadoMercado.ABIERTO;
        }

        if (afterHours != null && !time.isBefore(cierre) && time.isBefore(afterHours)) {
            return EstadoMercado.AFTER_HOURS;
        }

        return EstadoMercado.CERRADO;
    }

    public Long getId() { return id; }
    public String getMercadoMic() { return mercadoMic; }
    public int getDiaSemana() { return diaSemana; }
    public LocalTime getApertura() { return apertura; }
    public LocalTime getCierre() { return cierre; }
    public LocalTime getPreMarket() { return preMarket; }
    public LocalTime getAfterHours() { return afterHours; }
}