package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import java.time.Instant;

public interface EventoProcesadoPort {

    boolean registrarSiNuevo(String eventId, String eventType, Instant procesadoEn);
}
