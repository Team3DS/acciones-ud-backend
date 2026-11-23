package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in;

/** Revisa las órdenes enviadas al broker y registra las ejecuciones nuevas (RF de ejecución de órdenes). */
public interface SincronizarEjecucionesUseCase {

    /** @return cantidad de ejecuciones registradas en esta pasada */
    int sincronizar();
}
