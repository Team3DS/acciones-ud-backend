package com.gfc.infrastructure.security;

import com.gfc.domain.port.CodificadorContrasena;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import org.springframework.stereotype.Component;

@Component
public final class Pbkdf2CodificadorContrasena implements CodificadorContrasena {
    private static final int ITERACIONES = 600_000;
    private static final String PREFIJO = "pbkdf2-sha256";
    private final SecureRandom random = new SecureRandom();

    @Override
    public String codificar(String contrasena) {
        if (contrasena == null || contrasena.isBlank()) {
            throw new IllegalArgumentException("La contrasena es obligatoria");
        }
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        return PREFIJO + "$" + ITERACIONES + "$" + Base64.getEncoder().encodeToString(salt)
                + "$" + Base64.getEncoder().encodeToString(derivar(contrasena, salt));
    }

    @Override
    public boolean comprobar(String contrasena, String hash) {
        if (contrasena == null || hash == null) return false;
        String[] partes = hash.split("\\$", -1);
        if (partes.length != 4 || !PREFIJO.equals(partes[0])
                || !Integer.toString(ITERACIONES).equals(partes[1])) return false;
        try {
            byte[] salt = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            return salt.length == 16 && esperado.length == 32
                    && MessageDigest.isEqual(esperado, derivar(contrasena, salt));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private byte[] derivar(String contrasena, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(contrasena.toCharArray(), salt, ITERACIONES, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("PBKDF2 no esta disponible", ex);
        } finally {
            spec.clearPassword();
        }
    }
}
