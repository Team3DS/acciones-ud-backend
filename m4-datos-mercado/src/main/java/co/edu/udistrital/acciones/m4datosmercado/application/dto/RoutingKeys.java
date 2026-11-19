package co.edu.udistrital.acciones.m4datosmercado.application.dto;

public final class RoutingKeys {

    public static final String EXCHANGE = "acciones.ud";
    public static final String M3_INSTRUMENTO_CREADO = "m3.instrumento.creado";
    public static final String M3_INSTRUMENTO_ACTUALIZADO = "m3.instrumento.actualizado";
    public static final String M3_MERCADO_CREADO = "m3.mercado.creado";
    public static final String M3_MERCADO_ACTUALIZADO = "m3.mercado.actualizado";

    public static final String COLA_M4_EVENTOS = "m4.eventos";
    public static final String EXCHANGE_DLX = "acciones.ud.dlx";
    public static final String COLA_M4_EVENTOS_DLQ = "m4.eventos.dlq";

    private RoutingKeys() {}
}