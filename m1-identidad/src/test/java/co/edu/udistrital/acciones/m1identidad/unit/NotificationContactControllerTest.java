package co.edu.udistrital.acciones.m1identidad.unit;

import co.edu.udistrital.acciones.m1identidad.application.service.UserApplicationService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.NotificationContactController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.in.UserController;
import co.edu.udistrital.acciones.m1identidad.infrastructure.adapter.out.UsuarioRolJpaRepository;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.JwtProperties;
import co.edu.udistrital.acciones.m1identidad.infrastructure.config.SecurityConfig;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.JwtService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {NotificationContactController.class, UserController.class},
        properties = "app.jwt.secret=" + NotificationContactControllerTest.SECRET)
@Import({SecurityConfig.class, JwtService.class, JwtProperties.class})
class NotificationContactControllerTest {
    static final String SECRET = "notification-contact-test-secret-at-least-32-bytes";
    private static final String PATH = "/internal/users/7/notification-contact";

    @Autowired MockMvc mvc;
    @MockBean UserApplicationService users;
    @MockBean UsuarioRolJpaRepository roles;

    @Test
    void m8RecibeSoloElCorreoSolicitado() throws Exception {
        when(users.notificationEmail(7L)).thenReturn(Optional.of("usuario@example.com"));
        mvc.perform(get(PATH).header("Authorization", "Bearer " + serviceToken(60)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("usuario@example.com"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void sinTokenNoExponeContactos() throws Exception {
        mvc.perform(get(PATH).header("X-User-Id", "7"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    void tokenDeUsuarioNoPuedeConsultarContactos() throws Exception {
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token("7", "m1-identidad", "INVERSIONISTA", 60, SECRET)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    @Test
    void otroServicioNoPuedeConsultarContactos() throws Exception {
        mvc.perform(get(PATH).header("Authorization", "Bearer " + token("900000004", "m4-datos-mercado", "SISTEMA", 60, SECRET)))
                .andExpect(status().isForbidden());
        verifyNoInteractions(users);
    }

    @Test
    void rechazaTokenVencido() throws Exception {
        mvc.perform(get(PATH).header("Authorization", "Bearer " + serviceToken(-60)))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    void rechazaFirmaIncorrecta() throws Exception {
        String forged = token("900000008", "m8-notificaciones", "SISTEMA", 60,
                "other-notification-secret-at-least-32-bytes");
        mvc.perform(get(PATH).header("Authorization", "Bearer " + forged))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    void usuarioDesconocidoRetorna404() throws Exception {
        when(users.notificationEmail(7L)).thenReturn(Optional.empty());
        mvc.perform(get(PATH).header("Authorization", "Bearer " + serviceToken(60)))
                .andExpect(status().isNotFound());
    }

    @Test
    void perfilNoAceptaIdSuplantadoSinJwt() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("X-User-Id", "7"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    @Test
    void perfilNoUsaIdSuplantadoConJwtInvalido() throws Exception {
        mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer invalid")
                        .header("X-User-Id", "7"))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(users);
    }

    private static String serviceToken(long seconds) {
        return token("900000008", "m8-notificaciones", "SISTEMA", seconds, SECRET);
    }

    private static String token(String subject, String issuer, String role, long seconds, String secret) {
        return Jwts.builder().subject(subject).issuer(issuer)
                .claim("roles", List.of(role))
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().plusSeconds(seconds)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
