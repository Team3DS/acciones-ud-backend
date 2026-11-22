package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ResultadoCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.CatalogoInconsistenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ConflictoIdempotenciaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoProveedorNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SolicitudInvalidaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionVigenteException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.BloqueoUsuarioPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ClienteStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SesionCheckoutRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SesionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SolicitudCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.CatalogoPreciosProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.ClienteStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Dinero;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.SesionCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
public class IniciarSuscripcionUseCase {

    private static final Logger log = LoggerFactory.getLogger(IniciarSuscripcionUseCase.class);
    private static final Pattern CLAVE_IDEMPOTENCIA = Pattern.compile("[A-Za-z0-9_-]{1,64}");
    private static final String PLAN_PREMIUM = "PREMIUM";

    private final SuscripcionRepositoryPort suscripciones;
    private final SesionCheckoutRepositoryPort sesiones;
    private final ClienteStripeRepositoryPort clientes;
    private final PlanRepositoryPort planes;
    private final PaymentProviderPort proveedor;
    private final CatalogoPreciosProveedor catalogo;
    private final BloqueoUsuarioPort bloqueo;
    private final StripeProperties propiedades;
    private final Clock clock;

    public IniciarSuscripcionUseCase(SuscripcionRepositoryPort suscripciones,
                                     SesionCheckoutRepositoryPort sesiones,
                                     ClienteStripeRepositoryPort clientes,
                                     PlanRepositoryPort planes,
                                     PaymentProviderPort proveedor,
                                     CatalogoPreciosProveedor catalogo,
                                     BloqueoUsuarioPort bloqueo,
                                     StripeProperties propiedades,
                                     Clock clock) {
        this.suscripciones = suscripciones;
        this.sesiones = sesiones;
        this.clientes = clientes;
        this.planes = planes;
        this.proveedor = proveedor;
        this.catalogo = catalogo;
        this.bloqueo = bloqueo;
        this.propiedades = propiedades;
        this.clock = clock;
    }

    @Transactional
    public ResultadoCheckout ejecutar(Long userId, String idempotencyKey, IntervaloPlan intervalo) {
        validar(userId, idempotencyKey, intervalo);
        if (!propiedades.configurado()) {
            throw new ProveedorPagosNoDisponibleException("Stripe no esta configurado");
        }
        bloqueo.bloquear(userId);
        Instant ahora = clock.instant();
        if (suscripciones.findVigenteByUserId(userId).isPresent()) {
            throw new SuscripcionVigenteException("El usuario ya tiene una suscripcion vigente");
        }
        Optional<SesionCheckout> porClave = sesiones.findByIdempotencyKey(userId, idempotencyKey);
        if (porClave.isPresent()) {
            return reutilizarPorClave(porClave.get(), intervalo);
        }
        PrecioPlan precio = planes.findPrecioActivo(PLAN_PREMIUM, intervalo)
                .orElseThrow(() -> new CatalogoInconsistenteException(
                        "No hay precio activo para el intervalo " + intervalo));
        String stripePriceId = verificarPrecio(precio);
        Optional<SesionCheckout> abierta = sesiones.findAbiertaVigente(userId, precio.getId(), ahora);
        if (abierta.isPresent()) {
            return new ResultadoCheckout(abierta.get(), true);
        }
        expirarOtras(userId, precio, ahora);
        String customerId = clientes.findById(userId)
                .map(ClienteStripe::getStripeCustomerId)
                .orElse(null);
        Instant expiraEn = ahora.plus(propiedades.checkoutTtl());
        SesionProveedor creada = proveedor.crearSesionCheckout(new SolicitudCheckout(
                userId,
                stripePriceId,
                expiraEn,
                propiedades.successUrl(),
                propiedades.cancelUrl(),
                customerId,
                "m10-checkout-" + userId + "-" + idempotencyKey));
        SesionCheckout sesion = SesionCheckout.crear(
                userId,
                precio,
                creada.id(),
                creada.url(),
                idempotencyKey,
                ahora,
                creada.expiraEn() == null ? expiraEn : creada.expiraEn());
        sesiones.save(sesion);
        return new ResultadoCheckout(sesion, false);
    }

    private ResultadoCheckout reutilizarPorClave(SesionCheckout sesion, IntervaloPlan intervalo) {
        if (sesion.getPrecioPlan().getIntervalo() != intervalo) {
            throw new ConflictoIdempotenciaException(
                    "La clave de idempotencia ya se uso con otro intervalo");
        }
        return new ResultadoCheckout(sesion, true);
    }

    private String verificarPrecio(PrecioPlan local) {
        PrecioProveedor remoto = catalogo.obtener(local.getStripeLookupKey());
        Dinero dinero = Dinero.desdeCentavos(remoto.montoCentavos());
        if (remoto.id() == null || remoto.id().isBlank()
                || dinero.monto().compareTo(local.getMonto()) != 0
                || !dinero.moneda().equals(local.dinero().moneda())) {
            log.error("El precio {} de Stripe no coincide con el catalogo local", local.getStripeLookupKey());
            throw new CatalogoInconsistenteException(
                    "El precio de Stripe no coincide con el catalogo local");
        }
        return remoto.id();
    }

    private void expirarOtras(Long userId, PrecioPlan precio, Instant ahora) {
        for (SesionCheckout sesion : sesiones.findAbiertasByUserId(userId, ahora)) {
            if (sesion.esDelPrecio(precio)) {
                continue;
            }
            try {
                proveedor.expirarSesion(sesion.getStripeSessionId());
            } catch (RecursoProveedorNoEncontradoException ex) {
                log.warn("La sesion {} ya no existe en Stripe", sesion.getStripeSessionId());
            }
            sesion.marcarExpirada();
            sesiones.save(sesion);
        }
    }

    private static void validar(Long userId, String idempotencyKey, IntervaloPlan intervalo) {
        if (userId == null || userId <= 0) {
            throw new SolicitudInvalidaException("El usuario debe ser positivo");
        }
        if (idempotencyKey == null || !CLAVE_IDEMPOTENCIA.matcher(idempotencyKey).matches()) {
            throw new SolicitudInvalidaException("La clave de idempotencia no es valida");
        }
        if (intervalo == null) {
            throw new SolicitudInvalidaException("El intervalo es obligatorio");
        }
    }
}
