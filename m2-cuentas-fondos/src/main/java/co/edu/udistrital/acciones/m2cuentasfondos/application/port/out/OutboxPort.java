package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

public interface OutboxPort {

    void registrar(String aggregateType, String aggregateId, String routingKey, Object payload);
}
