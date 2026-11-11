package co.edu.udistrital.acciones.m2cuentasfondos.application.dto;

public final class RoutingKeys {

    public static final String EXCHANGE = "acciones.ud";
    public static final String MOVIMIENTO_CREADO = "m2.movimiento.creado";
    public static final String FONDOS_RESERVADOS = "m2.fondos.reservados";
    public static final String FONDOS_CONFIRMADOS = "m2.fondos.confirmados";
    public static final String FONDOS_LIBERADOS = "m2.fondos.liberados";

    private RoutingKeys() {
    }
}
