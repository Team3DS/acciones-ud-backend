package co.edu.udistrital.acciones.m6portafolio.application.port.out;

import co.edu.udistrital.acciones.m6portafolio.domain.ReservaPosicion;

import java.util.Optional;

public interface ReservaPosicionRepositoryPort {

    Optional<ReservaPosicion> findById(Long id);

    Optional<ReservaPosicion> findByIdForUpdate(Long id);

    Optional<ReservaPosicion> findByOrderId(String orderId);

    Optional<ReservaPosicion> findByOrderIdForUpdate(String orderId);

    Optional<ReservaPosicion> findByIdempotencyKey(String idempotencyKey);

    ReservaPosicion save(ReservaPosicion reserva);
}
