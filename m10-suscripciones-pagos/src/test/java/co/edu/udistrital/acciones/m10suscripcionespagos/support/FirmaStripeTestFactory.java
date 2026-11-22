package co.edu.udistrital.acciones.m10suscripcionespagos.support;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public final class FirmaStripeTestFactory {

    private static final String ALGORITMO = "HmacSHA256";

    private FirmaStripeTestFactory() {
    }

    public static String firmar(String secreto, String cuerpo, long timestamp) {
        try {
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(secreto.getBytes(StandardCharsets.UTF_8), ALGORITMO));
            byte[] firma = mac.doFinal((timestamp + "." + cuerpo).getBytes(StandardCharsets.UTF_8));
            return "t=" + timestamp + ",v1=" + HexFormat.of().formatHex(firma);
        } catch (NoSuchAlgorithmException | InvalidKeyException ex) {
            throw new IllegalStateException("No se pudo firmar el webhook de prueba", ex);
        }
    }

    public static String firmar(String secreto, String cuerpo, Instant instante) {
        return firmar(secreto, cuerpo, instante.getEpochSecond());
    }

    public static String evento(String id, String tipo, String objetoId, boolean livemode) {
        return """
                {"id":"%s","object":"event","type":"%s","livemode":%s,"data":{"object":{"id":"%s"}}}
                """.formatted(id, tipo, livemode, objetoId).strip();
    }
}
