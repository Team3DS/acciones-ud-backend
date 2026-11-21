package co.edu.udistrital.acciones.m6portafolio.application.service;

import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.application.exception.DatosMercadoNoDisponibleException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m6portafolio.domain.Montos;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ValoracionService {

    public static final String MONEDA = "USD";
    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private final DatosMercadoClientPort datosMercado;
    private final Clock clock;

    public ValoracionService(DatosMercadoClientPort datosMercado, Clock clock) {
        this.datosMercado = datosMercado;
        this.clock = clock;
    }

    public PosicionResponse valorar(Posicion posicion) {
        return valorar(posicion, cotizacionesVivas(Set.of(posicion.getSymbol())));
    }

    public List<PosicionResponse> valorarTodas(List<Posicion> posiciones) {
        Map<String, CotizacionMercado> cotizaciones = cotizacionesVivas(posiciones.stream()
                .map(Posicion::getSymbol)
                .collect(java.util.stream.Collectors.toSet()));
        return posiciones.stream().map(posicion -> valorar(posicion, cotizaciones)).toList();
    }

    private PosicionResponse valorar(Posicion posicion, Map<String, CotizacionMercado> cotizaciones) {
        CotizacionMercado cotizacion = cotizaciones.get(posicion.getSymbol());
        BigDecimal precio = cotizacion == null ? posicion.getPrecioActual() : cotizacion.price();
        Instant valoradoEn = cotizacion == null ? posicion.getValoradoEn() : cotizacion.timestamp();
        boolean stale = cotizacion == null;
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
                valoradoEn,
                stale,
                MONEDA);
    }

    public PortafolioResponse valorarPortafolio(Long userId, List<Posicion> abiertas, List<Posicion> todas) {
        Map<String, CotizacionMercado> cotizaciones = cotizacionesVivas(abiertas.stream()
                .map(Posicion::getSymbol)
                .collect(java.util.stream.Collectors.toSet()));
        List<PosicionResponse> posiciones = abiertas.stream()
                .map(posicion -> valorar(posicion, cotizaciones))
                .toList();
        BigDecimal totalCosto = Montos.cero();
        BigDecimal totalValor = Montos.cero();
        BigDecimal pnlNoRealizado = Montos.cero();
        boolean stale = false;
        boolean sinPrecio = false;
        for (PosicionResponse posicion : posiciones) {
            totalCosto = totalCosto.add(posicion.averageCost().multiply(posicion.quantity()));
            stale |= posicion.stale();
            if (posicion.currentPrice() == null) {
                sinPrecio = true;
            } else {
                totalValor = totalValor.add(posicion.currentPrice().multiply(posicion.quantity()));
                pnlNoRealizado = pnlNoRealizado
                        .add(posicion.currentPrice().subtract(posicion.averageCost())
                                .multiply(posicion.quantity()));
            }
        }
        BigDecimal realizado = todas.stream()
                .map(Posicion::getPnlRealizado)
                .reduce(Montos.cero(), BigDecimal::add);
        return new PortafolioResponse(
                userId,
                posiciones,
                sinPrecio ? null : totalValor.setScale(Montos.ESCALA, Montos.REDONDEO),
                totalCosto.setScale(Montos.ESCALA, Montos.REDONDEO),
                sinPrecio ? null : pnlNoRealizado.setScale(Montos.ESCALA, Montos.REDONDEO),
                realizado.setScale(Montos.ESCALA, Montos.REDONDEO),
                MONEDA,
                Instant.now(clock),
                stale || sinPrecio);
    }

    private Map<String, CotizacionMercado> cotizacionesVivas(Set<String> symbols) {
        if (symbols.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, CotizacionMercado> cotizaciones = datosMercado.obtenerCotizaciones(symbols);
            return cotizaciones == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(cotizaciones));
        } catch (DatosMercadoNoDisponibleException ex) {
            return Map.of();
        }
    }
}
