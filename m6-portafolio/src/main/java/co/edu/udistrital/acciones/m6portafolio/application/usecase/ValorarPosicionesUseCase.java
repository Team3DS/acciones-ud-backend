package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.CotizacionMercado;
import co.edu.udistrital.acciones.m6portafolio.application.dto.EstadoMercado;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionValoradaPayload;
import co.edu.udistrital.acciones.m6portafolio.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.config.ValoracionProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ValorarPosicionesUseCase {

    private static final Logger log = LoggerFactory.getLogger(ValorarPosicionesUseCase.class);
    private static final String MERCADO_ABIERTO = "ABIERTO";

    private final PosicionRepositoryPort posiciones;
    private final DatosMercadoClientPort datosMercado;
    private final OutboxPort outbox;
    private final ValoracionProperties propiedades;
    private final TransactionTemplate transacciones;
    private final Clock clock;

    public ValorarPosicionesUseCase(PosicionRepositoryPort posiciones, DatosMercadoClientPort datosMercado,
                                    OutboxPort outbox, ValoracionProperties propiedades,
                                    PlatformTransactionManager gestorTransacciones, Clock clock) {
        this.posiciones = posiciones;
        this.datosMercado = datosMercado;
        this.outbox = outbox;
        this.propiedades = propiedades;
        this.transacciones = new TransactionTemplate(gestorTransacciones);
        this.clock = clock;
    }

    public int ejecutar() {
        if (!debeValorar()) {
            return 0;
        }
        Map<String, CotizacionMercado> cotizaciones = obtenerCotizaciones();
        int valoradas = 0;
        for (Map.Entry<String, CotizacionMercado> cotizacion : cotizaciones.entrySet()) {
            try {
                valoradas += valorarSymbol(cotizacion.getKey(), cotizacion.getValue());
            } catch (RuntimeException ex) {
                log.warn("No se pudo valorar el simbolo {}: {}", cotizacion.getKey(), ex.getMessage(), ex);
            }
        }
        return valoradas;
    }

    private boolean debeValorar() {
        if (!propiedades.soloMercadoAbierto()) {
            return true;
        }
        try {
            EstadoMercado estado = datosMercado.estadoMercado();
            return estado != null && estado.estado() != null
                    && MERCADO_ABIERTO.equalsIgnoreCase(estado.estado().trim());
        } catch (RuntimeException ex) {
            log.warn("No se pudo consultar el estado del mercado: {}", ex.getMessage());
            return false;
        }
    }

    private Map<String, CotizacionMercado> obtenerCotizaciones() {
        Set<String> symbols = new LinkedHashSet<>(posiciones.findSymbolsConCantidad());
        if (symbols.isEmpty()) {
            return Map.of();
        }
        try {
            Map<String, CotizacionMercado> cotizaciones = datosMercado.obtenerCotizaciones(symbols);
            return cotizaciones == null ? Map.of() : cotizaciones;
        } catch (RuntimeException ex) {
            log.warn("No se pudieron obtener las cotizaciones: {}", ex.getMessage());
            return Map.of();
        }
    }

    private int valorarSymbol(String symbol, CotizacionMercado cotizacion) {
        int valoradas = 0;
        while (true) {
            List<Posicion> lote = transacciones.execute(estado -> valorarLote(symbol, cotizacion.price()));
            if (lote == null || lote.isEmpty()) {
                return valoradas;
            }
            valoradas += lote.size();
            if (lote.size() < propiedades.lote()) {
                return valoradas;
            }
        }
    }

    private List<Posicion> valorarLote(String symbol, BigDecimal precio) {
        List<Posicion> lote = posiciones.findAbiertasPorSymbolConPrecioDistinto(symbol, precio,
                propiedades.lote());
        if (lote.isEmpty()) {
            return lote;
        }
        Instant ahora = Instant.now(clock);
        for (Posicion posicion : lote) {
            posicion.valorar(precio, ahora);
            posiciones.save(posicion);
            outbox.registrar(RoutingKeys.TIPO_AGREGADO_POSICION, String.valueOf(posicion.getId()),
                    RoutingKeys.POSICION_VALORADA, new PosicionValoradaPayload(
                            posicion.getUserId(),
                            posicion.getSymbol(),
                            posicion.getCantidad(),
                            posicion.getPrecioActual(),
                            posicion.getValorActual(),
                            posicion.getPnlNoRealizado(),
                            posicion.getValoradoEn()));
        }
        return lote;
    }
}
