package co.edu.udistrital.acciones.m6portafolio.application.dto;

public final class RoutingKeys {

    public static final String EXCHANGE = "acciones.ud";
    public static final String EXCHANGE_DLX = "acciones.ud.dlx";

    public static final String POSICIONES_ACTUALIZADAS = "m6.posiciones.actualizadas";
    public static final String POSICION_VALORADA = "m6.posicion.valorada";

    public static final String M5_ORDEN_EJECUTADA = "m5.orden.ejecutada";
    public static final String M5_ORDEN_CANCELADA = "m5.orden.cancelada";
    public static final String M5_ORDEN_RECHAZADA = "m5.orden.rechazada";

    public static final String COLA_POSICIONES = "m6.posiciones";
    public static final String COLA_POSICIONES_DLQ = "m6.posiciones.dlq";

    public static final String TIPO_AGREGADO_POSICION = "posicion";

    private RoutingKeys() {
    }
}
