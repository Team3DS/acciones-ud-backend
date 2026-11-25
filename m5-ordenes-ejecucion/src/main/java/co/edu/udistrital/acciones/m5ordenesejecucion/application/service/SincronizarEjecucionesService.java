package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase.ProcesarEjecucionCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.SincronizarEjecucionesUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.AlpacaOrderPort.AlpacaOrderResponse;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.DatosMercadoClientPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

/**
 * Registra las ejecuciones de las órdenes enviadas al broker.
 * <ul>
 *   <li>Órdenes en Alpaca: consulta {@code filled_qty} y {@code filled_avg_price} y registra lo ejecutado desde
 *   la última pasada. El precio de esa parte se obtiene del promedio de Alpaca y lo ya registrado, así el costo
 *   total coincide con el de Alpaca.</li>
 *   <li>Órdenes del simulador (sin claves de Alpaca): se ejecuta lo pendiente con el precio actual de M4 cuando
 *   se cumple la condición del tipo de orden.</li>
 * </ul>
 * El id de cada ejecución es determinista, así una pasada repetida no registra dos veces lo mismo
 * (ProcesarEjecucion descarta brokerExecutionId repetidos).
 */
@Service
public class SincronizarEjecucionesService implements SincronizarEjecucionesUseCase {

    private static final Logger log = LoggerFactory.getLogger(SincronizarEjecucionesService.class);
    private static final String SUFIJO_SIMULADO = "-sim";

    private final OrdenRepositoryPort ordenRepository;
    private final EjecucionOrdenRepositoryPort ejecucionRepository;
    private final AlpacaOrderPort alpaca;
    private final DatosMercadoClientPort datosMercado;
    private final ProcesarEjecucionUseCase procesarEjecucion;

    public SincronizarEjecucionesService(OrdenRepositoryPort ordenRepository,
                                         EjecucionOrdenRepositoryPort ejecucionRepository,
                                         AlpacaOrderPort alpaca,
                                         DatosMercadoClientPort datosMercado,
                                         ProcesarEjecucionUseCase procesarEjecucion) {
        this.ordenRepository = ordenRepository;
        this.ejecucionRepository = ejecucionRepository;
        this.alpaca = alpaca;
        this.datosMercado = datosMercado;
        this.procesarEjecucion = procesarEjecucion;
    }

    @Override
    public int sincronizar() {
        int registradas = 0;
        for (Orden orden : ordenRepository.findPendientesDeEjecucion()) {
            try {
                if (sincronizar(orden)) {
                    registradas++;
                }
            } catch (Exception ex) {
                // Una orden con problemas no detiene las demás; se reintenta en la siguiente pasada.
                log.warn("No se pudo sincronizar la orden {} ({}): {}", orden.getId(), orden.getBrokerOrderId(), ex.getMessage());
            }
        }
        return registradas;
    }

    private boolean sincronizar(Orden orden) {
        String brokerOrderId = orden.getBrokerOrderId();
        if (brokerOrderId == null || brokerOrderId.isBlank() || orden.getCantidad() == null) {
            return false;
        }
        List<EjecucionOrden> previas = ejecucionRepository.findByOrdenId(orden.getId());
        int ejecutado = previas.stream().mapToInt(EjecucionOrden::getCantidad).sum();
        int pendiente = orden.getCantidad() - ejecutado;
        if (pendiente <= 0) {
            return false;
        }
        return brokerOrderId.startsWith(AlpacaOrderPort.PREFIJO_SIMULADO)
                ? simular(orden, pendiente)
                : consultarAlpaca(orden, previas, ejecutado);
    }

    private boolean simular(Orden orden, int pendiente) {
        BigDecimal precio = datosMercado.obtenerPrecioEstimado(orden.getTicker());
        if (precio == null || precio.signum() <= 0 || !seActiva(orden, precio)) {
            return false;
        }
        registrar(orden, orden.getBrokerOrderId() + SUFIJO_SIMULADO, pendiente, precio);
        return true;
    }

    private boolean consultarAlpaca(Orden orden, List<EjecucionOrden> previas, int ejecutado) {
        AlpacaOrderResponse estado = alpaca.consultarOrden(orden.getBrokerOrderId());
        if (estado != null && orden.getBrokerOrderId().startsWith(AlpacaOrderPort.PREFIJO_PENDIENTE)
                && estado.id() != null && !estado.id().startsWith(AlpacaOrderPort.PREFIJO_PENDIENTE)
                && !estado.id().startsWith(AlpacaOrderPort.PREFIJO_SIMULADO)) {
            orden.setBrokerOrderId(estado.id());
            orden.setActualizadaEn(Instant.now());
            ordenRepository.save(orden);
        }
        if (estado == null || estado.filledQty() == null || estado.filledAvgPrice() == null) {
            return false;
        }
        int ejecutadoBroker = Math.min(estado.filledQty(), orden.getCantidad());
        int nuevo = ejecutadoBroker - ejecutado;
        if (nuevo <= 0) {
            return false;
        }
        BigDecimal precio = precioDeLaParteNueva(estado.filledAvgPrice(), ejecutadoBroker, previas, nuevo);
        registrar(orden, orden.getBrokerOrderId() + "-" + ejecutadoBroker, nuevo, precio);
        return true;
    }

    /**
     * Precio de la parte nueva = (promedio de Alpaca × total ejecutado − costo ya registrado) / cantidad nueva.
     * Si el resultado no es positivo (redondeos), se usa el promedio de Alpaca.
     */
    static BigDecimal precioDeLaParteNueva(BigDecimal promedio, int totalEjecutado, List<EjecucionOrden> previas, int nuevo) {
        BigDecimal costoPrevio = previas.stream()
                .map(e -> e.getPrecioEjecucion().multiply(BigDecimal.valueOf(e.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal precio = promedio.multiply(BigDecimal.valueOf(totalEjecutado)).subtract(costoPrevio)
                .divide(BigDecimal.valueOf(nuevo), 4, RoundingMode.HALF_UP);
        return precio.signum() > 0 ? precio : promedio;
    }

    /** Condición de ejecución del simulador según el tipo de orden. */
    static boolean seActiva(Orden orden, BigDecimal precio) {
        boolean compra = orden.getLadoOrden() == LadoOrden.COMPRA;
        BigDecimal limite = orden.getPrecioLimite();
        BigDecimal activacion = orden.getPrecioActivacion();
        return switch (orden.getTipoOrden()) {
            case MARKET -> true;
            case LIMIT -> limite != null && (compra ? precio.compareTo(limite) <= 0 : precio.compareTo(limite) >= 0);
            // Stop loss: en venta se activa al caer hasta el precio; en compra, al subir.
            case STOP_LOSS -> activacion != null && (compra ? precio.compareTo(activacion) >= 0 : precio.compareTo(activacion) <= 0);
            // Take profit: en venta se activa al subir hasta el objetivo; en compra, al bajar.
            case TAKE_PROFIT -> activacion != null && (compra ? precio.compareTo(activacion) <= 0 : precio.compareTo(activacion) >= 0);
        };
    }

    private void registrar(Orden orden, String brokerExecutionId, int cantidad, BigDecimal precio) {
        log.info("Ejecución de la orden {}: {} x {} a {} ({})", orden.getId(), cantidad, orden.getTicker(), precio, brokerExecutionId);
        procesarEjecucion.ejecutar(new ProcesarEjecucionCommand(
                orden.getId(), brokerExecutionId, orden.getBrokerOrderId(), cantidad, precio, null, Instant.now()));
    }
}
