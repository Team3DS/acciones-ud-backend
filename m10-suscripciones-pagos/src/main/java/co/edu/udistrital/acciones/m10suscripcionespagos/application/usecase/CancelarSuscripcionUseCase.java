package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoEstadoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CancelarSuscripcionUseCase {

    private static final String PREFIJO_IDEMPOTENCIA = "m10-cancelar-";

    private final SuscripcionRepositoryPort suscripciones;
    private final PaymentProviderPort proveedor;
    private final SincronizadorSuscripciones sincronizador;
    private final BloqueoUsuarioPort bloqueo;
    private final StripeProperties propiedades;

    public CancelarSuscripcionUseCase(SuscripcionRepositoryPort suscripciones,
                                      PaymentProviderPort proveedor,
                                      SincronizadorSuscripciones sincronizador,
                                      BloqueoUsuarioPort bloqueo,
                                      StripeProperties propiedades) {
        this.suscripciones = suscripciones;
        this.proveedor = proveedor;
        this.sincronizador = sincronizador;
        this.bloqueo = bloqueo;
        this.propiedades = propiedades;
    }

    @Transactional
    public Suscripcion ejecutar(Long userId, Long suscripcionId) {
        validar(userId, suscripcionId);
        if (!propiedades.configurado()) {
            throw new ProveedorPagosNoDisponibleException("Stripe no esta configurado");
        }
        bloqueo.bloquear(userId);
        Suscripcion suscripcion = suscripciones.findById(suscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La suscripcion no existe"));
        verificarDueno(suscripcion, userId);
        verificarEstado(suscripcion);
        if (suscripcion.isCancelarAlFinal()) {
            return suscripcion;
        }
        SuscripcionProveedor remota = proveedor.programarCancelacion(
                suscripcion.getStripeSubscriptionId(), claveIdempotencia(suscripcion));
        return sincronizador.aplicar(remota);
    }

    private static void validar(Long userId, Long suscripcionId) {
        if (userId == null || userId <= 0) {
            throw new SolicitudInvalidaException("El usuario debe ser positivo");
        }
        if (suscripcionId == null || suscripcionId <= 0) {
            throw new SolicitudInvalidaException("El id de la suscripcion debe ser positivo");
        }
    }

    private static void verificarDueno(Suscripcion suscripcion, Long userId) {
        if (!Objects.equals(suscripcion.getUserId(), userId)) {
            throw new SuscripcionAjenaException("La suscripcion no pertenece al usuario");
        }
    }

    private static void verificarEstado(Suscripcion suscripcion) {
        EstadoSuscripcion estado = suscripcion.getEstado();
        if (estado != EstadoSuscripcion.ACTIVA && estado != EstadoSuscripcion.EN_MORA) {
            throw new ConflictoEstadoException("La suscripcion no permite programar la cancelacion");
        }
    }

    private static String claveIdempotencia(Suscripcion suscripcion) {
        return PREFIJO_IDEMPOTENCIA + suscripcion.getId() + "-" + suscripcion.getVersion();
    }
}
