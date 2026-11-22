package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.ProveedorPagosNoDisponibleException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.exception.WebhookInvalidoException;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.EventoStripeRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EventoStripe;
import co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config.StripeProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.net.Webhook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

@Service
public class RegistrarEventoStripeUseCase {

    private static final Logger log = LoggerFactory.getLogger(RegistrarEventoStripeUseCase.class);
    private static final String HEADER_FIRMA = "Stripe-Signature";
    private static final String CAMPO_ID = "id";
    private static final String CAMPO_TIPO = "type";
    private static final String CAMPO_LIVEMODE = "livemode";
    private static final String CAMPO_DATA = "data";
    private static final String CAMPO_OBJECT = "object";

    private final StripeProperties stripe;
    private final EventoStripeRepositoryPort eventos;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public RegistrarEventoStripeUseCase(StripeProperties stripe,
                                        EventoStripeRepositoryPort eventos,
                                        ObjectMapper objectMapper,
                                        Clock clock) {
        this.stripe = stripe;
        this.eventos = eventos;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public boolean ejecutar(byte[] cuerpo, String firma) {
        if (!stripe.webhookConfigurado()) {
            throw new ProveedorPagosNoDisponibleException("El webhook de Stripe no esta configurado");
        }
        if (cuerpo == null || cuerpo.length == 0) {
            throw new WebhookInvalidoException("El cuerpo del webhook es obligatorio");
        }
        if (firma == null || firma.isBlank()) {
            throw new WebhookInvalidoException("Falta el header " + HEADER_FIRMA);
        }
        String texto = new String(cuerpo, StandardCharsets.UTF_8);
        if (!firmaValida(texto, firma)) {
            throw new WebhookInvalidoException("La firma del webhook no es valida");
        }
        EventoStripe evento = leer(texto);
        if (evento == null) {
            return false;
        }
        boolean registrado = eventos.registrarSiNuevo(evento);
        if (!registrado) {
            log.info("El evento {} de Stripe ya estaba registrado", evento.getId());
        }
        return registrado;
    }

    private boolean firmaValida(String cuerpo, String firma) {
        try {
            return Webhook.Signature.verifyHeader(cuerpo, firma, stripe.webhookSecret(),
                    stripe.webhookTolerance().toSeconds());
        } catch (SignatureVerificationException ex) {
            log.warn("La firma del webhook de Stripe no es valida: {}", ex.getMessage());
            return false;
        }
    }

    private EventoStripe leer(String texto) {
        JsonNode raiz;
        try {
            raiz = objectMapper.readTree(texto);
        } catch (JsonProcessingException ex) {
            throw new WebhookInvalidoException("El cuerpo del webhook no es un JSON valido");
        }
        if (raiz == null || !raiz.isObject()) {
            throw new WebhookInvalidoException("El cuerpo del webhook no es un objeto JSON");
        }
        String id = textoObligatorio(raiz, CAMPO_ID);
        String tipo = textoObligatorio(raiz, CAMPO_TIPO);
        if (raiz.path(CAMPO_LIVEMODE).asBoolean(false)) {
            log.warn("Se descarta el evento {} de Stripe porque es de modo live", id);
            return null;
        }
        String objetoId = raiz.path(CAMPO_DATA).path(CAMPO_OBJECT).path(CAMPO_ID).asText("");
        if (objetoId.isBlank()) {
            throw new WebhookInvalidoException("El evento no trae " + CAMPO_DATA + "." + CAMPO_OBJECT + "." + CAMPO_ID);
        }
        return EventoStripe.registrar(id, tipo, objetoId, texto, Instant.now(clock));
    }

    private static String textoObligatorio(JsonNode raiz, String campo) {
        String valor = raiz.path(campo).asText("");
        if (valor.isBlank()) {
            throw new WebhookInvalidoException("El evento no trae el campo " + campo);
        }
        return valor;
    }
}
