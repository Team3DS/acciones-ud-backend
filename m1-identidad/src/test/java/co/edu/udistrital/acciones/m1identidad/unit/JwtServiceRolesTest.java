package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out.UsuarioRolJpaRepository;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.JwtProperties;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JwtServiceRolesTest {

    private static final String SECRETO = "test-secret-key-for-testing-purposes-only-32-chars-minimum";

    private static JwtProperties propiedades() {
        JwtProperties props = new JwtProperties();
        props.setSecret(SECRETO);
        props.setAccessTtlSeconds(900);
        return props;
    }

    private static Object rolesDelToken(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("roles");
    }

    @Test
    void todoUsuarioEsInversionista() {
        UsuarioRolJpaRepository roles = mock(UsuarioRolJpaRepository.class);
        when(roles.findRolesByUsuarioId(7L)).thenReturn(List.of());

        String token = new JwtService(propiedades(), roles).generateAccessToken(7L);

        assertEquals(List.of("INVERSIONISTA"), rolesDelToken(token));
    }

    @Test
    void incluyeLosRolesAsignadosEnUsuarioRol() {
        UsuarioRolJpaRepository roles = mock(UsuarioRolJpaRepository.class);
        when(roles.findRolesByUsuarioId(1L)).thenReturn(List.of("ADMINISTRADOR"));

        String token = new JwtService(propiedades(), roles).generateAccessToken(1L);

        assertEquals(List.of("ADMINISTRADOR", "INVERSIONISTA"), rolesDelToken(token));
    }

    @Test
    void sinRepositorioSoloQuedaInversionista() {
        String token = new JwtService(propiedades()).generateAccessToken(3L);

        assertEquals(List.of("INVERSIONISTA"), rolesDelToken(token));
    }
}
