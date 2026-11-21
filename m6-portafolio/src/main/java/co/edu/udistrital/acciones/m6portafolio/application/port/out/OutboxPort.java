package co.edu.udistrital.acciones.m6portafolio.application.port.out;

public interface OutboxPort {

    void registrar(String aggregateType, String aggregateId, String eventType, Object payload);
}
