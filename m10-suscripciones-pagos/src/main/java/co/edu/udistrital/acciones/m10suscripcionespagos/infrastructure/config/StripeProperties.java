package co.edu.udistrital.acciones.m10suscripcionespagos.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.stripe")
public record StripeProperties(
        String apiKey,
        String webhookSecret,
        String apiBase,
        Duration connectTimeout,
        Duration readTimeout,
        int maxNetworkRetries,
        Duration webhookTolerance,
        Duration checkoutTtl,
        Duration preciosCacheTtl,
        String successUrl,
        String cancelUrl) {

    public static final String PREFIJO_PRUEBA_SECRETA = "sk_test_";
    public static final String PREFIJO_PRUEBA_RESTRINGIDA = "rk_test_";
    public static final String API_BASE_POR_DEFECTO = "https://api.stripe.com";
    public static final String URL_EXITO_POR_DEFECTO =
            "http://127.0.0.1:5173/?billing=success&session_id={CHECKOUT_SESSION_ID}";
    public static final String URL_CANCELACION_POR_DEFECTO =
            "http://127.0.0.1:5173/?billing=cancel";

    public StripeProperties {
        apiKey = apiKey == null ? "" : apiKey.strip();
        webhookSecret = webhookSecret == null ? "" : webhookSecret.strip();
        apiBase = apiBase == null || apiBase.isBlank() ? API_BASE_POR_DEFECTO : apiBase.strip();
        connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
        readTimeout = readTimeout == null ? Duration.ofSeconds(10) : readTimeout;
        webhookTolerance = webhookTolerance == null ? Duration.ofMinutes(5) : webhookTolerance;
        checkoutTtl = checkoutTtl == null ? Duration.ofHours(1) : checkoutTtl;
        preciosCacheTtl = preciosCacheTtl == null ? Duration.ofMinutes(10) : preciosCacheTtl;
        successUrl = successUrl == null || successUrl.isBlank() ? URL_EXITO_POR_DEFECTO : successUrl.strip();
        cancelUrl = cancelUrl == null || cancelUrl.isBlank() ? URL_CANCELACION_POR_DEFECTO : cancelUrl.strip();
        if (!apiKey.isBlank() && !esClaveDePrueba(apiKey)) {
            throw new IllegalStateException(
                    "app.stripe.api-key debe ser una clave de prueba sk_test_ o rk_test_");
        }
    }

    public boolean configurado() {
        return !apiKey.isBlank();
    }

    public boolean webhookConfigurado() {
        return !webhookSecret.isBlank();
    }

    public int connectTimeoutMs() {
        return Math.toIntExact(connectTimeout.toMillis());
    }

    public int readTimeoutMs() {
        return Math.toIntExact(readTimeout.toMillis());
    }

    private static boolean esClaveDePrueba(String clave) {
        return clave.startsWith(PREFIJO_PRUEBA_SECRETA) || clave.startsWith(PREFIJO_PRUEBA_RESTRINGIDA);
    }
}
