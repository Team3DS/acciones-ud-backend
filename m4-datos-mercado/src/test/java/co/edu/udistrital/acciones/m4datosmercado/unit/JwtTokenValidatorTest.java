package co.edu.udistrital.acciones.m4datosmercado.unit;

import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m4datosmercado.infrastructure.security.UsuarioAutenticado;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

import javax.crypto.SecretKey;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenValidatorTest {

    private JwtTokenValidator validator;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties("clave-secreta-para-testing-minimo-32-chars");
        validator = new JwtTokenValidator(props);
        key = Keys.hmacShaKeyFor(props.secret().getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void tokenValidoRetornaUsuario() {
        String token = Jwts.builder()
                .subject("123")
                .claim("roles", Set.of("ROLE_USUARIO"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        UsuarioAutenticado user = validator.autenticar(token);

        assertEquals(123L, user.id());
        assertTrue(user.roles().contains("ROLE_USUARIO"));
    }

    @Test
    void tokenSinExpiracionLanzaExcepcion() {
        String token = Jwts.builder()
                .subject("123")
                .claim("roles", Set.of("ROLE_USUARIO"))
                .issuedAt(Date.from(Instant.now()))
                .signWith(key)
                .compact();

        assertThrows(io.jsonwebtoken.JwtException.class, () -> validator.autenticar(token));
    }

    @Test
    void subjectInvalidoLanzaExcepcion() {
        String token = Jwts.builder()
                .subject("no-numero")
                .claim("roles", Set.of("ROLE_USUARIO"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        assertThrows(io.jsonwebtoken.JwtException.class, () -> validator.autenticar(token));
    }

    @Test
    void validarRetornaId() {
        String token = Jwts.builder()
                .subject("456")
                .claim("roles", Set.of("ROLE_ADMIN"))
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(3600)))
                .signWith(key)
                .compact();

        long id = validator.validar(token);
        assertEquals(456L, id);
    }
}