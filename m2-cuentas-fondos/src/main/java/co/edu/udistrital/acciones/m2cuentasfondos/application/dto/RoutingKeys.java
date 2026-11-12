package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

public final class RoutingKeys {

    public static final String EXCHANGE = "acciones.ud";
    public static final String MOVIMIENTO_CREADO = "m2.movimiento.creado";
    public static final String FONDOS_RESERVADOS = "m2.fondos.reservados";
    public static final String FONDOS_CONFIRMADOS = "m2.fondos.confirmados";
    public static final String FONDOS_LIBERADOS = "m2.fondos.liberados";

    public static final String M5_ORDEN_EJECUTADA = "m5.orden.ejecutada";
    public static final String M5_ORDEN_CANCELADA = "m5.orden.cancelada";
    public static final String M5_ORDEN_RECHAZADA = "m5.orden.rechazada";
    public static final String M10_SUSCRIPCION_ACTIVADA = "m10.suscripcion.activada";

    public static final String COLA_MOVIMIENTOS = "m2.movimientos";
    public static final String EXCHANGE_DLX = "acciones.ud.dlx";
    public static final String COLA_MOVIMIENTOS_DLQ = "m2.movimientos.dlq";

    private RoutingKeys() {
    }
}
