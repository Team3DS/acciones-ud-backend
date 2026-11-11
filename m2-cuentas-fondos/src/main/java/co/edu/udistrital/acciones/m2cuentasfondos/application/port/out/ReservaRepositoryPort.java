package co.edu.udistrital.acciones.m2cuentasfondos.application.port.out;

import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;

import java.util.Optional;

public interface ReservaRepositoryPort {

    Reserva save(Reserva reserva);

    Optional<Reserva> findById(Long id);

    Optional<Reserva> findByIdForUpdate(Long id);

    Optional<Reserva> findByOrderId(String orderId);

    Optional<Reserva> findByIdempotencyKey(String idempotencyKey);
}
