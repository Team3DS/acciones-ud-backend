package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoInconsistenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoEstadoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.CatalogoPreciosProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.SincronizadorSuscripciones;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Dinero;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
public class CambiarIntervaloUseCase {

    private static final String PLAN_PREMIUM = "PREMIUM";
    private static final String PREFIJO_IDEMPOTENCIA = "m10-cambiar-intervalo-";

    private final SuscripcionRepositoryPort suscripciones;
    private final PlanRepositoryPort planes;
    private final PaymentProviderPort proveedor;
    private final CatalogoPreciosProveedor catalogo;
    private final SincronizadorSuscripciones sincronizador;
    private final BloqueoUsuarioPort bloqueo;
    private final StripeProperties propiedades;

    public CambiarIntervaloUseCase(SuscripcionRepositoryPort suscripciones,
                                   PlanRepositoryPort planes,
                                   PaymentProviderPort proveedor,
                                   CatalogoPreciosProveedor catalogo,
                                   SincronizadorSuscripciones sincronizador,
                                   BloqueoUsuarioPort bloqueo,
                                   StripeProperties propiedades) {
        this.suscripciones = suscripciones;
        this.planes = planes;
        this.proveedor = proveedor;
        this.catalogo = catalogo;
        this.sincronizador = sincronizador;
        this.bloqueo = bloqueo;
        this.propiedades = propiedades;
    }

    @Transactional
    public Suscripcion ejecutar(Long userId, Long suscripcionId, IntervaloPlan intervalo) {
        validar(userId, suscripcionId, intervalo);
        if (!propiedades.configurado()) {
            throw new ProveedorPagosNoDisponibleException("Stripe no esta configurado");
        }
        bloqueo.bloquear(userId);
        Suscripcion suscripcion = suscripciones.findById(suscripcionId)
                .orElseThrow(() -> new RecursoNoEncontradoException("La suscripcion no existe"));
        verificarDueno(suscripcion, userId);
        if (suscripcion.getEstado() != EstadoSuscripcion.ACTIVA) {
            throw new ConflictoEstadoException("Solo se puede cambiar el intervalo de una suscripcion activa");
        }
        if (suscripcion.isCancelarAlFinal()) {
            throw new ConflictoEstadoException("La cancelacion ya esta programada");
        }
        if (suscripcion.getPrecioPlan().getIntervalo() == intervalo) {
            return suscripcion;
        }
        PrecioPlan precio = planes.findPrecioActivo(PLAN_PREMIUM, intervalo)
                .orElseThrow(() -> new CatalogoInconsistenteException(
                        "No hay precio activo para el intervalo " + intervalo));
        PrecioProveedor remoto = verificarPrecio(precio);
        String itemId = itemId(suscripcion);
        SuscripcionProveedor actualizada = proveedor.cambiarPrecio(
                suscripcion.getStripeSubscriptionId(), itemId, remoto.id(), claveIdempotencia(suscripcion));
        return sincronizador.aplicar(actualizada);
    }

    private PrecioProveedor verificarPrecio(PrecioPlan local) {
        PrecioProveedor remoto = catalogo.obtener(local.getStripeLookupKey());
        Dinero dinero = Dinero.desdeCentavos(remoto.montoCentavos());
        if (remoto.id() == null || remoto.id().isBlank()
                || dinero.monto().compareTo(local.getMonto()) != 0
                || !dinero.moneda().equals(local.dinero().moneda())) {
            throw new CatalogoInconsistenteException("El precio de Stripe no coincide con el catalogo local");
        }
        return remoto;
    }

    private String itemId(Suscripcion suscripcion) {
        SuscripcionProveedor actual = proveedor.obtenerSuscripcion(suscripcion.getStripeSubscriptionId());
        if (actual.itemId() == null || actual.itemId().isBlank()) {
            throw new CatalogoInconsistenteException("La suscripcion de Stripe no trae items");
        }
        return actual.itemId();
    }

    private static void validar(Long userId, Long suscripcionId, IntervaloPlan intervalo) {
        if (userId == null || userId <= 0) {
            throw new SolicitudInvalidaException("El usuario debe ser positivo");
        }
        if (suscripcionId == null || suscripcionId <= 0) {
            throw new SolicitudInvalidaException("El id de la suscripcion debe ser positivo");
        }
        if (intervalo == null) {
            throw new SolicitudInvalidaException("El intervalo es obligatorio");
        }
    }

    private static void verificarDueno(Suscripcion suscripcion, Long userId) {
        if (!Objects.equals(suscripcion.getUserId(), userId)) {
            throw new SuscripcionAjenaException("La suscripcion no pertenece al usuario");
        }
    }

    private static String claveIdempotencia(Suscripcion suscripcion) {
        return PREFIJO_IDEMPOTENCIA + suscripcion.getId() + "-" + suscripcion.getVersion();
    }
}
