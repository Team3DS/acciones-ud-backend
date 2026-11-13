package co.edu.udistrital.acciones.m3catalogomercados.unit;

import static org.assertj.core.api.Assertions.*;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtTokenValidator;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

class JwtTokenValidatorTest {
    private final String secret =
            "m3-test-only-key-not-for-deployment-1234567890123456789012345678901234";
    private final JwtTokenValidator validator = new JwtTokenValidator(new JwtProperties(secret));

    String signed(String subject, Instant expiration) {
        var builder = Jwts.builder().subject(subject);
        if (expiration != null) builder.expiration(Date.from(expiration));
        return builder.signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    @Test
    void aceptaElFormatoDeM1SinExigirRolesEnLectura() {
        assertThat(validator.validar(signed("42", Instant.now().plusSeconds(60)))).isEqualTo(42);
    }

    @Test
    void exigeExpiracion() {
        assertThatThrownBy(() -> validator.validar(signed("42", null)))
                .isInstanceOf(JwtException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"abc", "0", "-1"})
    void exigeIdentidadNumericaPositiva(String subject) {
        assertThatThrownBy(() -> validator.validar(signed(subject, Instant.now().plusSeconds(60))))
                .isInstanceOf(JwtException.class);
    }
}
