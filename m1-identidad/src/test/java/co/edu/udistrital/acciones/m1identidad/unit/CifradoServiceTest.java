package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.service.CifradoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CifradoServiceTest {

    private CifradoService service;

    @BeforeEach
    void setUp() {
        String keyBase64 = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
        service = new CifradoService(keyBase64);
    }

    @Test
    void encryptDecryptRoundtrip() {
        String plaintext = "dato-sensible-123";

        byte[] encrypted = service.encrypt(plaintext);
        String decrypted = service.decrypt(encrypted);

        assertEquals(plaintext, decrypted);
    }

    @Test
    void encryptTwiceProducesDifferentCiphertext() {
        String plaintext = "mismo-texto";

        byte[] encrypted1 = service.encrypt(plaintext);
        byte[] encrypted2 = service.encrypt(plaintext);

        assertFalse(Arrays.equals(encrypted1, encrypted2), "IV aleatorio debe producir ciphertext distinto");
        assertEquals(plaintext, service.decrypt(encrypted1));
        assertEquals(plaintext, service.decrypt(encrypted2));
    }

    @Test
    void hmacSha256IsDeterministicAnd44Base64Chars() {
        String input = "dato-para-hmac";

        String hmac1 = service.hmacSha256(input);
        String hmac2 = service.hmacSha256(input);

        assertEquals(hmac1, hmac2);
        assertEquals(44, hmac1.length(), "HMAC SHA-256 en Base64 debe tener 44 caracteres");
        assertTrue(hmac1.matches("[A-Za-z0-9+/]+={0,2}"), "HMAC debe ser Base64 válido");
    }

    @Test
    void decryptCorruptedBytesThrowsException() {
        byte[] corrupted = new byte[]{1, 2, 3, 4, 5};

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.decrypt(corrupted));

        assertTrue(ex.getMessage().contains("Error al descifrar") || ex.getCause() != null);
    }
}