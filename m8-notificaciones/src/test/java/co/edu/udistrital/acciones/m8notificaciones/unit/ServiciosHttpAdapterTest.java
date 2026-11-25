package co.edu.udistrital.acciones.m8notificaciones.unit;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.edu.udistrital.acciones.m8notificaciones.domain.AvisoException;
import co.edu.udistrital.acciones.m8notificaciones.infrastructure.adapter.out.ServiciosHttpAdapter;

import com.github.tomakehurst.wiremock.WireMockServer;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Date;

class ServiciosHttpAdapterTest {
    private static final String SECRET =
            "m8-test-only-jwt-secret-1234567890123456789012345678901234567890";
    private static final String INTERNAL_TOKEN = "fixture-service-token-for-m10-only";
    private final MutableClock clock = new MutableClock();
    private WireMockServer server;

    @BeforeEach
    void start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        stub(
                "/api/v1/instruments/id/1",
                "{\"id\":1,\"symbol\":\"AAPL\",\"enabled\":true,\"type\":\"ACCION\"}");
        stub(
                "/api/v1/quotes/AAPL",
                "{\"symbol\":\"AAPL\",\"price\":151.25,\"timestamp\":\"2026-11-25T12:00:00Z\"}");
        stub("/internal/users/7/notification-contact", "{\"email\":\"user@example.test\"}");
        stub(
                "/internal/subscriptions/7/entitlement",
                "{\"active\":true,\"validUntil\":\"2026-12-01T00:00:00Z\"}");
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void firmaJwtDeSistemaParaCatalogoCotizacionesYContactoSinFiltrarTokenDeM10() {
        var adapter = adapter(INTERNAL_TOKEN);

        assertThat(adapter.instrumento(1).symbol()).isEqualTo("AAPL");
        assertThat(adapter.cotizacion("AAPL").price()).isEqualByComparingTo("151.25");
        assertThat(adapter.correoUsuario(7)).isEqualTo("user@example.test");
        assertThat(adapter.premium(7)).isTrue();

        for (String path :
                new String[] {
                    "/api/v1/instruments/id/1",
                    "/api/v1/quotes/AAPL",
                    "/internal/users/7/notification-contact"
                }) {
            String token = authorization(path).substring("Bearer ".length());
            Claims claims = claims(token);
            assertThat(claims.getSubject()).isEqualTo("900000008");
            assertThat(claims.getIssuer()).isEqualTo("m8-notificaciones");
            assertThat(claims.get("roles", java.util.List.class)).containsExactly("SISTEMA");
            assertThat(claims.getIssuedAt().toInstant()).isEqualTo(clock.instant());
            assertThat(claims.getExpiration().toInstant())
                    .isEqualTo(clock.instant().plusSeconds(60));
            assertThat(token).isNotEqualTo(INTERNAL_TOKEN);
        }
        assertThat(authorization("/internal/subscriptions/7/entitlement"))
                .isEqualTo("Bearer " + INTERNAL_TOKEN);
    }

    @Test
    void renuevaElJwtDespuesDeVencerSinReiniciarElServicio() {
        var adapter = adapter(INTERNAL_TOKEN);
        adapter.instrumento(1);
        String first = authorization("/api/v1/instruments/id/1").substring("Bearer ".length());

        clock.advance(Duration.ofMinutes(2));
        server.resetRequests();
        adapter.instrumento(1);
        String renewed = authorization("/api/v1/instruments/id/1").substring("Bearer ".length());

        assertThat(renewed).isNotEqualTo(first);
        assertThatThrownBy(() -> claims(first)).isInstanceOf(ExpiredJwtException.class);
        assertThat(claims(renewed).getExpiration().toInstant())
                .isEqualTo(clock.instant().plusSeconds(60));
    }

    @Test
    void jwtDeServiciosNoDependeDelTokenInternoYNoLoSustituyeEnM10() {
        var adapter = adapter("");
        adapter.instrumento(1);
        assertThat(claims(authorization("/api/v1/instruments/id/1").substring(7)).getSubject())
                .isEqualTo("900000008");

        server.stubFor(
                get(urlEqualTo("/internal/subscriptions/7/entitlement"))
                        .willReturn(aResponse().withStatus(401)));
        assertThatThrownBy(() -> adapter.premium(7))
                .isInstanceOf(AvisoException.class)
                .satisfies(error -> assertThat(((AvisoException) error).status()).isEqualTo(503));
        assertThat(authorization("/internal/subscriptions/7/entitlement")).isNull();
    }

    private ServiciosHttpAdapter adapter(String token) {
        String base = server.baseUrl();
        return new ServiciosHttpAdapter(base, base, base, base, base, base, token, SECRET, clock);
    }

    private void stub(String path, String body) {
        server.stubFor(
                get(urlEqualTo(path))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(body)));
    }

    private String authorization(String path) {
        return server.findAll(getRequestedFor(urlEqualTo(path)))
                .getFirst()
                .getHeader("Authorization");
    }

    private Claims claims(String token) {
        return Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private static final class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-11-25T12:00:00Z");

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
