package co.edu.udistrital.acciones.m10suscripcionespagos.application.service;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.RoutingKeys;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.payload.PagoFallidoPayload;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.OutboxPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ReciboRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.ReciboProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Dinero;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoRecibo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Recibo;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

@Service
public class SincronizadorRecibos {

    private static final Logger log = LoggerFactory.getLogger(SincronizadorRecibos.class);

    private final PaymentProviderPort proveedor;
    private final ReciboRepositoryPort recibos;
    private final SincronizadorSuscripciones sincronizadorSuscripciones;
    private final OutboxPort outbox;
    private final Clock clock;

    public SincronizadorRecibos(PaymentProviderPort proveedor,
                                ReciboRepositoryPort recibos,
                                SincronizadorSuscripciones sincronizadorSuscripciones,
                                OutboxPort outbox,
                                Clock clock) {
        this.proveedor = proveedor;
        this.recibos = recibos;
        this.sincronizadorSuscripciones = sincronizadorSuscripciones;
        this.outbox = outbox;
        this.clock = clock;
    }

    public void sincronizar(String stripeInvoiceId) {
        ReciboProveedor remoto = proveedor.obtenerRecibo(stripeInvoiceId);
        if (remoto.stripeSubscriptionId() == null || remoto.stripeSubscriptionId().isBlank()) {
            log.info("El recibo {} no esta asociado a una suscripcion; se descarta", remoto.id());
            return;
        }
        Suscripcion suscripcion = sincronizadorSuscripciones.sincronizar(remoto.stripeSubscriptionId());
        Instant ahora = clock.instant();
        boolean nuevoFallo = guardar(suscripcion.getId(), remoto, ahora);
        if (nuevoFallo) {
            publicarPagoFallido(suscripcion, remoto, ahora);
        }
    }

    private boolean guardar(Long suscripcionId, ReciboProveedor remoto, Instant ahora) {
        Dinero monto = Dinero.desdeCentavos(remoto.totalCentavos());
        EstadoRecibo estado = EstadoRecibo.desdeStripe(remoto.estado(), remoto.intento());
        int intento = Math.toIntExact(remoto.intento());
        Optional<Recibo> existente = recibos.findByStripeInvoiceId(remoto.id());
        if (existente.isPresent()) {
            Recibo recibo = existente.get();
            boolean nuevoFallo = recibo.actualizar(monto, estado, intento, remoto.numero(),
                    remoto.periodoInicio(), remoto.periodoFin(), remoto.pagadoEn(),
                    remoto.urlRecibo(), remoto.urlPdf(), ahora);
            recibos.save(recibo);
            return nuevoFallo;
        }
        Recibo recibo = Recibo.crear(suscripcionId, remoto.id(), monto, estado, intento,
                remoto.emitidoEn() == null ? ahora : remoto.emitidoEn(), ahora);
        recibo.actualizar(monto, estado, intento, remoto.numero(), remoto.periodoInicio(),
                remoto.periodoFin(), remoto.pagadoEn(), remoto.urlRecibo(), remoto.urlPdf(), ahora);
        recibos.save(recibo);
        return estado == EstadoRecibo.FALLIDO;
    }

    private void publicarPagoFallido(Suscripcion suscripcion, ReciboProveedor remoto, Instant ahora) {
        Recibo recibo = recibos.findByStripeInvoiceId(remoto.id()).orElseThrow();
        outbox.registrar(RoutingKeys.TIPO_AGREGADO_RECIBO, String.valueOf(recibo.getId()),
                RoutingKeys.PAGO_FALLIDO, new PagoFallidoPayload(
                        suscripcion.getUserId(), recibo.getId(), recibo.getIntento(), ahora));
    }
}
