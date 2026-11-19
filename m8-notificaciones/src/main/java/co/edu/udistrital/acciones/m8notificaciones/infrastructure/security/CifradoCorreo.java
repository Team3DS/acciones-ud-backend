package co.edu.udistrital.acciones.m8notificaciones.infrastructure.security;

import co.edu.udistrital.acciones.m8notificaciones.domain.Correo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@Component
public class CifradoCorreo {
    private final SecretKeySpec key;
    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();

    public CifradoCorreo(
            @Value("${app.mail.encryption-key}") String encodedKey, ObjectMapper mapper) {
        byte[] decoded = Base64.getDecoder().decode(encodedKey);
        if (decoded.length != 32) {
            throw new IllegalArgumentException("M8_MAIL_KEY debe contener 32 bytes en Base64.");
        }
        key = new SecretKeySpec(decoded, "AES");
        this.mapper = mapper;
    }

    public String cifrar(Correo mail) {
        byte[] nonce = new byte[12];
        random.nextBytes(nonce);
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(128, nonce));
            byte[] payload = cipher.doFinal(mapper.writeValueAsBytes(mail));
            return Base64.getEncoder()
                    .encodeToString(
                            ByteBuffer.allocate(nonce.length + payload.length)
                                    .put(nonce)
                                    .put(payload)
                                    .array());
        } catch (GeneralSecurityException | JsonProcessingException exception) {
            throw new IllegalStateException("No se pudo proteger el correo.");
        }
    }

    public Correo descifrar(String encrypted) {
        try {
            byte[] bytes = Base64.getDecoder().decode(encrypted);
            if (bytes.length < 28) {
                throw new IllegalArgumentException();
            }
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(128, bytes, 0, 12));
            return mapper.readValue(cipher.doFinal(bytes, 12, bytes.length - 12), Correo.class);
        } catch (GeneralSecurityException
                | java.io.IOException
                | IllegalArgumentException exception) {
            throw new IllegalStateException("Contenido de correo inválido.");
        }
    }
}
