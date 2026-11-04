package co.edu.udistrital.acciones.m1identidad.infrastructure.service;

import co.edu.udistrital.acciones.m1identidad.infrastructure.config.JwtProperties;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

@Service
public class JwtService {
    private final JwtProperties props;
    public JwtService(JwtProperties props) { this.props = props; }

    public String generateAccessToken(Long userId) {
        long exp = Instant.now().getEpochSecond() + props.getAccessTtlSeconds();
        String payload = userId + ":" + exp;
        String sig = hmac(payload);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((payload + "." + sig).getBytes(StandardCharsets.UTF_8));
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(props.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
        } catch (Exception e) { throw new RuntimeException(e); }
    }
}
