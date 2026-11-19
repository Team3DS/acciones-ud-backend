package co.edu.udistrital.acciones.m3catalogomercados.unit;

import static org.assertj.core.api.Assertions.*;

import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security.JwtTokenValidator;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

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

    @Test
    void rechazaIdentidadFueraDelRangoLong() {
        assertThatThrownBy(
                        () ->
                                validator.autenticar(
                                        signed(
                                                "9223372036854775808",
                                                Instant.now().plusSeconds(60))))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenVencido() {
        assertThatThrownBy(() -> validator.autenticar(signed("42", Instant.now().minusSeconds(60))))
                .isInstanceOf(io.jsonwebtoken.ExpiredJwtException.class);
    }

    @Test
    void rechazaFirmaDeOtraClave() {
        String token =
                Jwts.builder()
                        .subject("42")
                        .expiration(Date.from(Instant.now().plusSeconds(60)))
                        .signWith(
                                Keys.hmacShaKeyFor(
                                        (secret + "-otra-clave").getBytes(StandardCharsets.UTF_8)))
                        .compact();
        assertThatThrownBy(() -> validator.autenticar(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void rechazaTokenSinFirmaYAunNoValido() {
        String unsigned =
                Jwts.builder()
                        .subject("42")
                        .expiration(Date.from(Instant.now().plusSeconds(60)))
                        .compact();
        String future =
                Jwts.builder()
                        .subject("42")
                        .notBefore(Date.from(Instant.now().plusSeconds(120)))
                        .expiration(Date.from(Instant.now().plusSeconds(600)))
                        .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                        .compact();
        assertThatThrownBy(() -> validator.autenticar(unsigned)).isInstanceOf(JwtException.class);
        assertThatThrownBy(() -> validator.autenticar(future))
                .isInstanceOf(io.jsonwebtoken.PrematureJwtException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"no-es-jwt", "e30.e30.", "a.b.c"})
    void rechazaTokensMalFormados(String token) {
        assertThatThrownBy(() -> validator.autenticar(token))
                .isInstanceOfAny(JwtException.class, IllegalArgumentException.class);
    }

    @Test
    void normalizaRolesYEliminaDuplicados() {
        var user =
                validator.autenticar(
                        conRoles(List.of("ADMINISTRADOR", "ROLE_ADMINISTRADOR", "INVERSIONISTA")));
        assertThat(user.id()).isEqualTo(42);
        assertThat(user.roles())
                .containsExactlyInAnyOrder("ROLE_ADMINISTRADOR", "ROLE_INVERSIONISTA");
        assertThatThrownBy(() -> user.roles().add("ROLE_OTRO"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void ausenciaOListaVaciaDeRolesNoConcedePermisosAdministrativos() {
        assertThat(validator.autenticar(signed("42", Instant.now().plusSeconds(60))).roles())
                .isEmpty();
        assertThat(validator.autenticar(conRoles(List.of())).roles()).isEmpty();
    }

    @ParameterizedTest
    @MethodSource("rolesInvalidos")
    void rechazaRolesConFormatoInvalido(Object roles) {
        assertThatThrownBy(() -> validator.autenticar(conRoles(roles)))
                .isInstanceOf(JwtException.class);
    }

    static Stream<Object> rolesInvalidos() {
        return Stream.of(
                "ADMINISTRADOR",
                123,
                List.of("ADMINISTRADOR", 1),
                List.of(""),
                List.of("administrador"),
                List.of("ROLE_"),
                List.of("ADMINISTRADOR "),
                List.of("A".repeat(65)),
                java.util.Collections.nCopies(33, "ADMINISTRADOR"));
    }

    String conRoles(Object roles) {
        return Jwts.builder()
                .subject("42")
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .claim("roles", roles)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
