package co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out;

public interface OutboxPort {

    void registrar(String aggregateType, String aggregateId, String eventType, Object payload);
}
