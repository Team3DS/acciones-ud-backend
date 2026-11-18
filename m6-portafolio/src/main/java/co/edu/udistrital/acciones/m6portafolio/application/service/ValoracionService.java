package co.edu.udistrital.acciones.m6portafolio.application.service;

import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.domain.Montos;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ValoracionService {

    public static final String MONEDA = "USD";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final Clock clock;

    public ValoracionService(Clock clock) {
        this.clock = clock;
    }

    public PosicionResponse valorar(Posicion posicion) {
        BigDecimal precio = posicion.getPrecioActual();
        BigDecimal valorMercado = null;
        BigDecimal pnlNoRealizado = null;
        BigDecimal pnlPorcentual = null;
        if (precio != null) {
            valorMercado = precio.multiply(posicion.getCantidad()).setScale(Montos.ESCALA, Montos.REDONDEO);
            BigDecimal costo = posicion.getCostoPromedio().multiply(posicion.getCantidad());
            pnlNoRealizado = valorMercado.subtract(costo).setScale(Montos.ESCALA, Montos.REDONDEO);
            if (costo.signum() > 0) {
                pnlPorcentual = pnlNoRealizado.multiply(CIEN)
                        .divide(costo, Montos.ESCALA, Montos.REDONDEO);
            }
        }
        return new PosicionResponse(
                posicion.getUserId(),
                posicion.getSymbol(),
                posicion.getCantidad(),
                posicion.getCantidadReservada(),
                posicion.disponible(),
                posicion.getCostoPromedio(),
                precio,
                valorMercado,
                pnlNoRealizado,
                pnlPorcentual,
                posicion.getPnlRealizado(),
                posicion.getValoradoEn(),
                precio == null,
                MONEDA);
    }

    public PortafolioResponse valorarPortafolio(Long userId, List<Posicion> abiertas, List<Posicion> todas) {
        List<PosicionResponse> posiciones = abiertas.stream().map(this::valorar).toList();
        BigDecimal totalCosto = Montos.cero();
        BigDecimal totalValor = Montos.cero();
        BigDecimal pnlNoRealizado = Montos.cero();
        boolean stale = false;
        for (Posicion posicion : abiertas) {
            totalCosto = totalCosto.add(posicion.getCostoPromedio().multiply(posicion.getCantidad()));
            if (posicion.getPrecioActual() == null) {
                stale = true;
            } else {
                totalValor = totalValor.add(posicion.getPrecioActual().multiply(posicion.getCantidad()));
                pnlNoRealizado = pnlNoRealizado
                        .add(posicion.getPrecioActual().subtract(posicion.getCostoPromedio())
                                .multiply(posicion.getCantidad()));
            }
        }
        BigDecimal realizado = todas.stream()
                .map(Posicion::getPnlRealizado)
                .reduce(Montos.cero(), BigDecimal::add);
        return new PortafolioResponse(
                userId,
                posiciones,
                stale ? null : totalValor.setScale(Montos.ESCALA, Montos.REDONDEO),
                totalCosto.setScale(Montos.ESCALA, Montos.REDONDEO),
                stale ? null : pnlNoRealizado.setScale(Montos.ESCALA, Montos.REDONDEO),
                realizado.setScale(Montos.ESCALA, Montos.REDONDEO),
                MONEDA,
                Instant.now(clock),
                stale);
    }
}
