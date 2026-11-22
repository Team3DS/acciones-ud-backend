package co.edu.udistrital.acciones.m10suscripcionespagos.application.service;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionActivadaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionCanceladaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.SuscripcionVencidaPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.SuscripcionAjenaException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ClienteStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SesionCheckoutRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.ClienteStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.TransicionSuscripcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class SincronizadorSuscripciones {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorSuscripciones.class);
    private static final String PLAN_PREMIUM = "PREMIUM";

    private final PaymentProviderPort proveedor;
    private final SuscripcionRepositoryPort suscripciones;
    private final PlanRepositoryPort planes;
    private final SesionCheckoutRepositoryPort sesiones;
    private final ClienteStripeRepositoryPort clientes;
    private final OutboxPort outbox;
    private final Clock clock;

    public SincronizadorSuscripciones(PaymentProviderPort proveedor,
                                      SuscripcionRepositoryPort suscripciones,
                                      PlanRepositoryPort planes,
                                      SesionCheckoutRepositoryPort sesiones,
                                      ClienteStripeRepositoryPort clientes,
                                      OutboxPort outbox,
                                      Clock clock) {
        this.proveedor = proveedor;
        this.suscripciones = suscripciones;
        this.planes = planes;
        this.sesiones = sesiones;
        this.clientes = clientes;
        this.outbox = outbox;
        this.clock = clock;
    }

    public Suscripcion sincronizar(String stripeSubscriptionId) {
        SuscripcionProveedor remota = proveedor.obtenerSuscripcion(stripeSubscriptionId);
        return aplicar(remota);
    }

    public Suscripcion aplicar(SuscripcionProveedor remota) {
        Instant ahora = clock.instant();
        PrecioPlan precio = planes.findPrecioPorLookupKey(remota.lookupKey())
                .orElseThrow(() -> new SuscripcionAjenaException(
                        "El precio " + remota.lookupKey() + " de la suscripcion " + remota.id()
                                + " no esta en el catalogo local"));
        EstadoSuscripcion estado = mapearEstado(remota);
        DatosSuscripcion datos = new DatosSuscripcion(
                remota.id(), precio, estado, remota.periodoInicio(), remota.periodoFin(),
                remota.cancelarAlFinal(), remota.canceladaEn(), remota.finalizadaEn());

        Optional<Suscripcion> existente = suscripciones.findByStripeSubscriptionIdForUpdate(remota.id());
        if (existente.isPresent()) {
            Suscripcion suscripcion = existente.get();
            TransicionSuscripcion transicion = suscripcion.sincronizar(datos, ahora);
            Suscripcion guardada = suscripciones.save(suscripcion);
            publicarEventos(guardada, transicion, remota);
            return guardada;
        }

        Long userId = resolverDueno(remota);
        guardarClienteSiFalta(userId, remota.customerId(), ahora);
        Suscripcion suscripcion = Suscripcion.crear(userId, datos, ahora);
        Suscripcion guardada = suscripciones.save(suscripcion);
        TransicionSuscripcion transicion = new TransicionSuscripcion(null, estado);
        publicarEventos(guardada, transicion, remota);
        return guardada;
    }

    private Long resolverDueno(SuscripcionProveedor remota) {
        Long userId = remota.userIdMetadata();
        if (userId == null || userId <= 0) {
            throw new SuscripcionAjenaException(
                    "La suscripcion " + remota.id() + " no trae userId en su metadata");
        }
        if (!sesiones.existePorUserId(userId)) {
            throw new SuscripcionAjenaException(
                    "El usuario " + userId + " de la suscripcion " + remota.id()
                            + " no tiene una sesion de checkout en M10");
        }
        return userId;
    }

    private void guardarClienteSiFalta(Long userId, String customerId, Instant ahora) {
        if (customerId == null || customerId.isBlank()) {
            return;
        }
        if (clientes.findById(userId).isEmpty()) {
            clientes.save(ClienteStripe.crear(userId, customerId, ahora));
        }
    }

    private void publicarEventos(Suscripcion suscripcion, TransicionSuscripcion transicion,
                                 SuscripcionProveedor remota) {
        String aggregateId = String.valueOf(suscripcion.getId());
        if (transicion.llegoA(EstadoSuscripcion.ACTIVA) && transicion.anterior() != EstadoSuscripcion.ACTIVA) {
            outbox.registrar(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, aggregateId,
                    RoutingKeys.SUSCRIPCION_ACTIVADA, new SuscripcionActivadaPayload(
                            suscripcion.getUserId(), PLAN_PREMIUM,
                            suscripcion.getPrecioPlan().getIntervalo().name(),
                            suscripcion.getStripeSubscriptionId(), suscripcion.getPeriodoFin()));
            return;
        }
        if (!transicion.veniaDePremium()) {
            return;
        }
        if (transicion.llegoA(EstadoSuscripcion.CANCELADA)) {
            outbox.registrar(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, aggregateId,
                    RoutingKeys.SUSCRIPCION_CANCELADA, new SuscripcionCanceladaPayload(
                            suscripcion.getUserId(), PLAN_PREMIUM, suscripcion.getFinalizadaEn()));
        } else if (transicion.llegoA(EstadoSuscripcion.VENCIDA)) {
            outbox.registrar(RoutingKeys.TIPO_AGREGADO_SUSCRIPCION, aggregateId,
                    RoutingKeys.SUSCRIPCION_VENCIDA, new SuscripcionVencidaPayload(
                            suscripcion.getUserId(), PLAN_PREMIUM, suscripcion.getFinalizadaEn(),
                            motivo(remota)));
        }
    }

    private static EstadoSuscripcion mapearEstado(SuscripcionProveedor remota) {
        String estado = remota.estado() == null ? "" : remota.estado().strip().toLowerCase();
        return switch (estado) {
            case "active", "trialing" -> EstadoSuscripcion.ACTIVA;
            case "past_due" -> EstadoSuscripcion.EN_MORA;
            case "incomplete" -> EstadoSuscripcion.INCOMPLETA;
            case "incomplete_expired", "unpaid" -> EstadoSuscripcion.VENCIDA;
            case "canceled" -> esImpago(remota) ? EstadoSuscripcion.VENCIDA : EstadoSuscripcion.CANCELADA;
            default -> {
                log.warn("Estado de Stripe desconocido '{}' para la suscripcion {}", estado, remota.id());
                yield EstadoSuscripcion.VENCIDA;
            }
        };
    }

    private static boolean esImpago(SuscripcionProveedor remota) {
        String motivo = motivo(remota);
        return "payment_failed".equals(motivo) || "payment_disputed".equals(motivo);
    }

    private static String motivo(SuscripcionProveedor remota) {
        return remota.motivoCancelacion() == null ? "" : remota.motivoCancelacion().strip().toLowerCase();
    }
}
