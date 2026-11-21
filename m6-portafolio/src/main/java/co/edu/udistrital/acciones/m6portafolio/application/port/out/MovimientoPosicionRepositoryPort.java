package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.domain.MovimientoPosicion;

public interface MovimientoPosicionRepositoryPort {

    MovimientoPosicion save(MovimientoPosicion movimiento);

    boolean existsByIdempotencyKey(String idempotencyKey);
}
