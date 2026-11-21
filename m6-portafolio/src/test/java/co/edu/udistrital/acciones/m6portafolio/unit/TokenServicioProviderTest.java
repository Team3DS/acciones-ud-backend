package co.edu.udistrital.acciones.m6portafolio.unit;

import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.AuthenticatedUser;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtProperties;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.JwtTokenValidator;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.ServicioProperties;
import co.edu.udistrital.acciones.m6portafolio.infrastructure.security.TokenServicioProvider;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class TokenServicioProviderTest {

    private static final String SECRETO = "m6-test-jwt-secret-0123456789abcdef0123456789abcdef0123456789abcdef";
    @Test
    void firmaUnTokenDeSistemaConElUsuarioYLaExpiracionConfigurados() {
        TokenServicioProvider provider = new TokenServicioProvider(new JwtProperties(SECRETO),
                new ServicioProperties(900000006L, Duration.ofSeconds(60)), Clock.systemUTC());

        String token = provider.obtenerToken();
        AuthenticatedUser usuario = new JwtTokenValidator(new JwtProperties(SECRETO)).validar(token);

        assertThat(usuario.userId()).isEqualTo(900000006L);
        assertThat(usuario.roles()).containsExactly("SISTEMA");
    }

    @Test
    void reutilizaElTokenMientrasNoEsteCercaDeExpirar() {
        TokenServicioProvider provider = new TokenServicioProvider(new JwtProperties(SECRETO),
                new ServicioProperties(900000006L, Duration.ofSeconds(60)), Clock.systemUTC());

        assertThat(provider.obtenerToken()).isEqualTo(provider.obtenerToken());
    }

    @Test
    void renuevaElTokenDiezSegundosAntesDeSuExpiracion() {
        RelojMutable clock = new RelojMutable(Instant.parse("2026-03-01T10:00:00Z"));
        TokenServicioProvider provider = new TokenServicioProvider(new JwtProperties(SECRETO),
                new ServicioProperties(900000006L, Duration.ofSeconds(60)), clock);

        String inicial = provider.obtenerToken();
        clock.avanzarSegundos(49);
        assertThat(provider.obtenerToken()).isEqualTo(inicial);

        clock.avanzarSegundos(1);
        assertThat(provider.obtenerToken()).isNotEqualTo(inicial);
    }

    private static final class RelojMutable extends Clock {
        private Instant instant;

        private RelojMutable(Instant instant) {
            this.instant = instant;
        }

        private void avanzarSegundos(long segundos) {
            instant = instant.plusSeconds(segundos);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
