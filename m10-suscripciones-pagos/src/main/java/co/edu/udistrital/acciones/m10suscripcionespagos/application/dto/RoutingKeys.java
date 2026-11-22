package co.edu.udistrital.acciones.m10suscripcionespagos.application.dto;

public final class RoutingKeys {

    public static final String EXCHANGE = "acciones.ud";
    public static final String ORIGEN = "M10";

    public static final String SUSCRIPCION_ACTIVADA = "m10.suscripcion.activada";
    public static final String SUSCRIPCION_CANCELADA = "m10.suscripcion.cancelada";
    public static final String SUSCRIPCION_VENCIDA = "m10.suscripcion.vencida";
    public static final String PAGO_FALLIDO = "m10.pago.fallido";

    public static final String TIPO_AGREGADO_SUSCRIPCION = "suscripcion";
    public static final String TIPO_AGREGADO_RECIBO = "recibo";

    private RoutingKeys() {
    }
}
