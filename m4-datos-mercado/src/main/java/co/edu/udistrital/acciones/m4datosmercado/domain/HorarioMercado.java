package co.edu.udistrital.acciones.m4datosmercado.domain;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "horario_mercado")
public class HorarioMercado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mercado_mic", nullable = false, length = 20)
    private String mercadoMic;

    @Column(name = "dia_semana", nullable = false)
    private int diaSemana;

    @Column(nullable = false)
    private LocalTime apertura;

    @Column(nullable = false)
    private LocalTime cierre;

    @Column(name = "pre_market")
    private LocalTime preMarket;

    @Column(name = "after_hours")
    private LocalTime afterHours;

    public HorarioMercado() {}

    public HorarioMercado(Long id, String mercadoMic, int diaSemana, LocalTime apertura, LocalTime cierre, LocalTime preMarket, LocalTime afterHours) {
        this.id = id;
        this.mercadoMic = mercadoMic;
        this.diaSemana = diaSemana;
        this.apertura = apertura;
        this.cierre = cierre;
        this.preMarket = preMarket;
        this.afterHours = afterHours;
    }

    public Long getId() { return id; }
    public String getMercadoMic() { return mercadoMic; }
    public int getDiaSemana() { return diaSemana; }
    public LocalTime getApertura() { return apertura; }
    public LocalTime getCierre() { return cierre; }
    public LocalTime getPreMarket() { return preMarket; }
    public LocalTime getAfterHours() { return afterHours; }

    public boolean esDiaLaborable(Instant instant) {
        DayOfWeek dow = instant.atZone(ZoneOffset.UTC).getDayOfWeek();
        return dow.getValue() >= 1 && dow.getValue() <= 5;
    }

    public EstadoMercado obtenerEstado(Instant instant) {
        LocalTime time = instant.atZone(ZoneOffset.UTC).toLocalTime();
        if (preMarket != null && time.isBefore(preMarket)) return EstadoMercado.CERRADO;
        if (preMarket != null && !time.isBefore(preMarket) && time.isBefore(apertura)) return EstadoMercado.PRE_MARKET;
        if (!time.isBefore(apertura) && !time.isAfter(cierre)) return EstadoMercado.ABIERTO;
        if (afterHours != null && time.isAfter(cierre) && time.isBefore(afterHours)) return EstadoMercado.AFTER_HOURS;
        return EstadoMercado.CERRADO;
    }
}
