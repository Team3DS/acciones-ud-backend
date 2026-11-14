package co.edu.udistrital.acciones.m3catalogomercados.application.port.out;

import java.util.UUID;

public interface PublicadorCatalogoPort {
    void publicar(UUID id, String routingKey, String json);
}
