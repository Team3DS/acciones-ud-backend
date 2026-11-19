package co.edu.udistrital.acciones.m8notificaciones.application.port.out;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.util.List;

public interface ServiciosPort {
    boolean premium(long userId);

    Instrumento instrumento(long id);

    Cotizacion cotizacion(String symbol);

    String correoUsuario(long userId);

    List<Destinatario> suscriptoresReporte(int page);

    ResumenMensual resumen(long userId, YearMonth periodo);

    record Instrumento(long id, String symbol, boolean enabled) {}

    record Cotizacion(BigDecimal price, Instant timestamp) {}

    record Destinatario(long userId, String email) {}

    record ResumenMensual(
            BigDecimal grossProfit, BigDecimal commissions, String currency, String assets) {}
}
