package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.adapter.out.stripe;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.RecursoProveedorNoEncontradoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PaymentProviderPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.PrecioProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.ReciboProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SesionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SolicitudCheckout;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.proveedor.SuscripcionProveedor;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import com.stripe.StripeClient;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.model.Price;
import com.stripe.model.StripeCollection;
import com.stripe.model.Subscription;
import com.stripe.model.SubscriptionItem;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.PriceListParams;
import com.stripe.param.SubscriptionUpdateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Component
public class StripePaymentProviderAdapter implements PaymentProviderPort {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentProviderAdapter.class);
    private static final String METADATA_USUARIO = "userId";
    private static final int NO_ENCONTRADO = 404;

    private final StripeClient cliente;
    private final StripeProperties propiedades;

    public StripePaymentProviderAdapter(StripeClient cliente, StripeProperties propiedades) {
        this.cliente = cliente;
        this.propiedades = propiedades;
    }

    @Override
    public SesionProveedor crearSesionCheckout(SolicitudCheckout solicitud) {
        verificarConfigurado();
        SessionCreateParams.Builder parametros = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                .setSuccessUrl(solicitud.successUrl())
                .setCancelUrl(solicitud.cancelUrl())
                .setClientReferenceId(String.valueOf(solicitud.userId()))
                .setExpiresAt(solicitud.expiraEn().getEpochSecond())
                .putMetadata(METADATA_USUARIO, String.valueOf(solicitud.userId()))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPrice(solicitud.stripePriceId())
                        .setQuantity(1L)
                        .build())
                .setSubscriptionData(SessionCreateParams.SubscriptionData.builder()
                        .putMetadata(METADATA_USUARIO, String.valueOf(solicitud.userId()))
                        .build());
        if (solicitud.customerId() != null && !solicitud.customerId().isBlank()) {
            parametros.setCustomer(solicitud.customerId());
        }
        Session sesion = ejecutar(() -> cliente.v1().checkout().sessions()
                .create(parametros.build(), opciones(solicitud.idempotencyKey())));
        return mapear(sesion);
    }

    @Override
    public void expirarSesion(String stripeSessionId) {
        verificarConfigurado();
        ejecutar(() -> cliente.v1().checkout().sessions().expire(stripeSessionId));
    }

    @Override
    public SesionProveedor obtenerSesion(String stripeSessionId) {
        verificarConfigurado();
        return mapear(ejecutar(() -> cliente.v1().checkout().sessions().retrieve(stripeSessionId)));
    }

    @Override
    public SuscripcionProveedor obtenerSuscripcion(String stripeSubscriptionId) {
        verificarConfigurado();
        return mapear(ejecutar(() -> cliente.v1().subscriptions().retrieve(stripeSubscriptionId)));
    }

    @Override
    public SuscripcionProveedor programarCancelacion(String stripeSubscriptionId, String idempotencyKey) {
        verificarConfigurado();
        SubscriptionUpdateParams parametros = SubscriptionUpdateParams.builder()
                .setCancelAtPeriodEnd(true)
                .build();
        return mapear(ejecutar(() -> cliente.v1().subscriptions()
                .update(stripeSubscriptionId, parametros, opciones(idempotencyKey))));
    }

    @Override
    public SuscripcionProveedor cambiarPrecio(String stripeSubscriptionId, String itemId,
                                              String nuevoPrecioId, String idempotencyKey) {
        verificarConfigurado();
        SubscriptionUpdateParams parametros = SubscriptionUpdateParams.builder()
                .addItem(SubscriptionUpdateParams.Item.builder()
                        .setId(itemId)
                        .setPrice(nuevoPrecioId)
                        .build())
                .setProrationBehavior(SubscriptionUpdateParams.ProrationBehavior.ALWAYS_INVOICE)
                .build();
        return mapear(ejecutar(() -> cliente.v1().subscriptions()
                .update(stripeSubscriptionId, parametros, opciones(idempotencyKey))));
    }

    @Override
    public ReciboProveedor obtenerRecibo(String stripeInvoiceId) {
        verificarConfigurado();
        return mapear(ejecutar(() -> cliente.v1().invoices().retrieve(stripeInvoiceId)));
    }

    @Override
    public PrecioProveedor precioPorLookupKey(String lookupKey) {
        verificarConfigurado();
        StripeCollection<Price> precios = ejecutar(() -> cliente.v1().prices().list(
                PriceListParams.builder()
                        .addLookupKey(lookupKey)
                        .setLimit(1L)
                        .build()));
        Price precio = precios.getData().stream()
                .filter(candidato -> lookupKey.equals(candidato.getLookupKey()))
                .findFirst()
                .or(() -> precios.getData().stream().findFirst())
                .orElseThrow(() -> new RecursoProveedorNoEncontradoException(
                        "No existe el precio " + lookupKey + " en Stripe"));
        long monto = precio.getUnitAmount() == null ? 0L : precio.getUnitAmount();
        return new PrecioProveedor(precio.getId(), monto, precio.getCurrency(), precio.getLookupKey());
    }

    private SesionProveedor mapear(Session sesion) {
        return new SesionProveedor(
                sesion.getId(),
                sesion.getUrl(),
                aInstante(sesion.getExpiresAt()),
                sesion.getCustomer(),
                sesion.getSubscription());
    }

    private SuscripcionProveedor mapear(Subscription suscripcion) {
        List<SubscriptionItem> items = suscripcion.getItems() == null
                ? List.of()
                : suscripcion.getItems().getData();
        SubscriptionItem item = items.isEmpty() ? null : items.getFirst();
        String lookupKey = item == null || item.getPrice() == null ? null : item.getPrice().getLookupKey();
        String motivo = suscripcion.getCancellationDetails() == null
                ? null
                : suscripcion.getCancellationDetails().getReason();
        return new SuscripcionProveedor(
                suscripcion.getId(),
                suscripcion.getStatus(),
                Boolean.TRUE.equals(suscripcion.getCancelAtPeriodEnd()),
                aInstante(suscripcion.getCanceledAt()),
                aInstante(suscripcion.getEndedAt()),
                motivo,
                usuarioDeMetadata(suscripcion.getMetadata()),
                suscripcion.getCustomer(),
                lookupKey,
                item == null ? null : aInstante(item.getCurrentPeriodStart()),
                item == null ? null : aInstante(item.getCurrentPeriodEnd()));
    }

    private ReciboProveedor mapear(Invoice factura) {
        String suscripcion = factura.getParent() == null
                || factura.getParent().getSubscriptionDetails() == null
                ? null
                : factura.getParent().getSubscriptionDetails().getSubscription();
        Instant pagadoEn = factura.getStatusTransitions() == null
                ? null
                : aInstante(factura.getStatusTransitions().getPaidAt());
        return new ReciboProveedor(
                factura.getId(),
                factura.getNumber(),
                factura.getTotal() == null ? 0L : factura.getTotal(),
                factura.getCurrency(),
                factura.getStatus(),
                aInstante(factura.getPeriodStart()),
                aInstante(factura.getPeriodEnd()),
                aInstante(factura.getCreated()),
                pagadoEn,
                factura.getAttemptCount() == null ? 0L : factura.getAttemptCount(),
                factura.getHostedInvoiceUrl(),
                factura.getInvoicePdf(),
                suscripcion);
    }

    private RequestOptions opciones(String idempotencyKey) {
        return RequestOptions.builder().setIdempotencyKey(idempotencyKey).build();
    }

    private void verificarConfigurado() {
        if (!propiedades.configurado()) {
            throw new ProveedorPagosNoDisponibleException("Stripe no esta configurado");
        }
    }

    private <T> T ejecutar(AccionStripe<T> accion) {
        try {
            return accion.ejecutar();
        } catch (InvalidRequestException ex) {
            if (esNoEncontrado(ex)) {
                throw new RecursoProveedorNoEncontradoException("El recurso no existe en Stripe");
            }
            log.error("Stripe rechazo la peticion", ex);
            throw new ProveedorPagosNoDisponibleException("Stripe no pudo procesar la peticion", ex);
        } catch (StripeException ex) {
            log.error("Fallo la comunicacion con Stripe", ex);
            throw new ProveedorPagosNoDisponibleException("Stripe no esta disponible", ex);
        }
    }

    private static boolean esNoEncontrado(StripeException ex) {
        return ex.getStatusCode() != null && ex.getStatusCode() == NO_ENCONTRADO;
    }

    private static Instant aInstante(Long segundos) {
        return segundos == null ? null : Instant.ofEpochSecond(segundos);
    }

    private static Long usuarioDeMetadata(Map<String, String> metadata) {
        if (metadata == null) {
            return null;
        }
        String valor = metadata.get(METADATA_USUARIO);
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(valor);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @FunctionalInterface
    private interface AccionStripe<T> {
        T ejecutar() throws StripeException;
    }
}
