package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.security;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JwtTokenValidatorTest {

    private static final String SECRET_BASE64 = "bXktdmVyeS1zZWNyZXQta2V5LWZvci10ZXN0aW5nLXB1cnBvc2VzLW9ubHk";
    private JwtTokenValidator validador;
    private SecretKey clave;

    @BeforeEach
    void setUp() throws Exception {
        JwtProperties propiedades = new JwtProperties(SECRET_BASE64);
        validador = new JwtTokenValidator(propiedades);
        Field field = JwtTokenValidator.class.getDeclaredField("clave");
        field.setAccessible(true);
        clave = (SecretKey) field.get(validador);
    }

    @Test
    void validarTokenValido() {
        String token = Jwts.builder()
                .subject("1")
                .claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(clave)
                .compact();

        AuthenticatedUser usuario = validador.validar(token);

        assertThat(usuario).isNotNull();
        assertThat(usuario.userId()).isEqualTo(1L);
        assertThat(usuario.roles()).containsExactly("ADMIN");
        assertThat(usuario.esAdministrador()).isTrue();
    }

    @Test
    void validarTokenSinExpiracion() {
        String token = Jwts.builder()
                .subject("1")
                .claim("roles", List.of("ADMIN"))
                .signWith(clave)
                .compact();

        assertThatThrownBy(() -> validador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expiracion");
    }

    @Test
    void validarTokenSubjectNoNumerico() {
        String token = Jwts.builder()
                .subject("abc")
                .claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(clave)
                .compact();

        assertThatThrownBy(() -> validador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("numerica");
    }

    @Test
    void validarTokenSubjectNegativo() {
        String token = Jwts.builder()
                .subject("-5")
                .claim("roles", List.of("ADMIN"))
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(clave)
                .compact();

        assertThatThrownBy(() -> validador.validar(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("positiva");
    }

    @Test
    void validarTokenSinRoles() {
        String token = Jwts.builder()
                .subject("1")
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(clave)
                .compact();

        AuthenticatedUser usuario = validador.validar(token);

        assertThat(usuario).isNotNull();
        assertThat(usuario.userId()).isEqualTo(1L);
        assertThat(usuario.roles()).isEmpty();
    }

    @Test
    void constructorSecretMenos32Bytes() {
        JwtProperties propiedades = new JwtProperties("corto");

        assertThatThrownBy(() -> new JwtTokenValidator(propiedades))
                .isInstanceOf(io.jsonwebtoken.security.WeakKeyException.class);
    }
}