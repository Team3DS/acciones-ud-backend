package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FondosLiberadosPayload;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.ReservaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.service.LibroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Reserva;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.TipoMovimiento;
import co.edu.udistrital.acciones.m2cuentasfondos.infrastructure.config.ReservasProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class ExpirarReservasUseCase {

    public static final String MOTIVO = "EXPIRADA";

    private static final Logger log = LoggerFactory.getLogger(ExpirarReservasUseCase.class);

    private final ReservaRepositoryPort reservas;
    private final CuentaRepositoryPort cuentas;
    private final LibroMovimientos libro;
    private final OutboxPort outbox;
    private final TransactionTemplate transactionTemplate;
    private final ReservasProperties properties;
    private final Clock clock;

    public ExpirarReservasUseCase(ReservaRepositoryPort reservas, CuentaRepositoryPort cuentas,
                                  LibroMovimientos libro, OutboxPort outbox,
                                  TransactionTemplate transactionTemplate, ReservasProperties properties,
                                  Clock clock) {
        this.reservas = reservas;
        this.cuentas = cuentas;
        this.libro = libro;
        this.outbox = outbox;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
        this.clock = clock;
    }

    public int ejecutar() {
        Instant limite = Instant.now(clock).minus(properties.getTtl());
        List<Reserva> vencidas = reservas.findVencidas(limite, properties.getExpiracion().getLote());
        int expiradas = 0;
        for (Reserva candidata : vencidas) {
            try {
                Boolean expirada = transactionTemplate.execute(estado -> expirar(candidata.getId(), limite));
                if (Boolean.TRUE.equals(expirada)) {
                    expiradas++;
                }
            } catch (RuntimeException ex) {
                log.warn("No se pudo expirar la reserva {}: {}", candidata.getId(), ex.getMessage(), ex);
            }
        }
        if (expiradas > 0) {
            log.info("Se expiraron {} reservas vencidas", expiradas);
        }
        return expiradas;
    }

    private boolean expirar(Long reservaId, Instant limite) {
        Reserva referencia = reservas.findById(reservaId).orElse(null);
        if (referencia == null) {
            return false;
        }
        Cuenta cuenta = cuentas.findByIdForUpdate(referencia.getCuentaId())
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la cuenta de la reserva " + reservaId));
        Reserva reserva = reservas.findByIdForUpdate(reservaId).orElse(null);
        if (reserva == null || !reserva.estaVencida(limite)) {
            return false;
        }

        Instant ahora = Instant.now(clock);
        cuenta.liberarReserva(reserva.getMonto(), ahora);
        cuentas.save(cuenta);

        reserva.expirar(ahora);
        reservas.save(reserva);

        libro.registrar(cuenta, TipoMovimiento.LIBERACION, reserva.getMonto(), TipoMovimiento.LIBERACION.name(),
                LiberarReservaUseCase.PREFIJO_MOVIMIENTO + reserva.getId(),
                "Expiración de la reserva " + reserva.getId());

        outbox.registrar("Reserva", reserva.getId().toString(), RoutingKeys.FONDOS_LIBERADOS,
                new FondosLiberadosPayload(reserva.getId(), MOTIVO));
        return true;
    }
}
