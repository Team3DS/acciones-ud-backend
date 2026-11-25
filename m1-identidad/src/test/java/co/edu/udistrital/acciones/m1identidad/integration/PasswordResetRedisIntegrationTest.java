package co.edu.udistrital.acciones.m1identidad.integration;

import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;
import co.edu.udistrital.acciones.m1identidad.infrastructure.service.PasswordResetTokenService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** OTP de recuperación y claims reales en Redis; no carga Spring ni consulta datos de usuarios. */
class PasswordResetRedisIntegrationTest {
    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private static PasswordResetTokenService service;

    @BeforeAll
    static void conectarRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate();
        redis.setConnectionFactory(connectionFactory);
        redis.afterPropertiesSet();
        service = new PasswordResetTokenService(redis);
    }

    @AfterAll
    static void cerrarRedis() {
        if (connectionFactory != null) connectionFactory.destroy();
        REDIS.stop();
    }

    @Test
    void permiteCorregirUnCodigoErroneoYCompleteImpideSuReutilizacion() {
        long user = 901;
        String code = service.issue(user).orElseThrow().code();
        assertThat(service.claim(user, incorrecto(code))).isEmpty();

        String claim = service.claim(user, code).orElseThrow();
        assertThat(service.claim(user, code)).isEmpty();
        service.complete(user, claim);
        assertThat(service.claim(user, code)).isEmpty();
        assertSinDesafio(user);
    }

    @Test
    void cincoErroresInvalidanElDesafio() {
        long user = 902;
        String code = service.issue(user).orElseThrow().code();
        for (int i = 0; i < 5; i++) {
            assertThat(service.claim(user, incorrecto(code))).isEmpty();
        }
        assertThat(service.claim(user, code)).isEmpty();
        assertThat(redis.hasKey(key(user, "code"))).isFalse();
    }

    @Test
    void elCodigoCorrectoEnElQuintoIntentoTodaviaEsValido() {
        long user = 903;
        String code = service.issue(user).orElseThrow().code();
        for (int i = 0; i < 4; i++) {
            assertThat(service.claim(user, incorrecto(code))).isEmpty();
        }
        String claim = service.claim(user, code).orElseThrow();
        service.complete(user, claim);
        assertSinDesafio(user);
    }

    @Test
    void dosConfirmacionesConcurrentesSoloObtienenUnClaim() throws Exception {
        long user = 904;
        String code = service.issue(user).orElseThrow().code();
        CountDownLatch listas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var primera = executor.submit(() -> claimAlIniciar(user, code, listas, iniciar));
            var segunda = executor.submit(() -> claimAlIniciar(user, code, listas, iniciar));
            assertThat(listas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();
            Optional<String> primerResultado = primera.get(10, TimeUnit.SECONDS);
            Optional<String> segundoResultado = segunda.get(10, TimeUnit.SECONDS);
            assertThat((primerResultado.isPresent() ? 1 : 0) + (segundoResultado.isPresent() ? 1 : 0))
                    .isEqualTo(1);
            service.complete(user, primerResultado.or(() -> segundoResultado).orElseThrow());
            assertSinDesafio(user);
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void cooldownDeUnMinutoNoReemplazaElCodigoNiSuVigenciaDeCincoMinutos() {
        long user = 905;
        Instant before = Instant.now();
        var challenge = service.issue(user).orElseThrow();
        assertThat(challenge.code()).matches("[0-9]{6}");
        assertThat(challenge.expiresAt()).isAfter(before.plusSeconds(290));
        assertThat(challenge.expiresAt()).isBefore(Instant.now().plusSeconds(301));
        assertThat(redis.getExpire(key(user, "code"), TimeUnit.SECONDS)).isBetween(290L, 300L);
        assertThat(redis.getExpire(key(user, "cooldown"), TimeUnit.SECONDS)).isBetween(50L, 60L);

        assertThat(service.issue(user)).isEmpty();
        assertThat(redis.opsForValue().get(key(user, "code"))).isEqualTo(challenge.code());
        String claim = service.claim(user, challenge.code()).orElseThrow();
        service.complete(user, claim);
    }

    @Test
    void nuevoDesafioTrasCooldownReiniciaLosIntentos() {
        long user = 906;
        String oldCode = service.issue(user).orElseThrow().code();
        for (int i = 0; i < 4; i++) {
            assertThat(service.claim(user, incorrecto(oldCode))).isEmpty();
        }
        assertThat(redis.expire(key(user, "cooldown"), Duration.ZERO)).isTrue();
        String newCode = service.issue(user).orElseThrow().code();
        for (int i = 0; i < 4; i++) {
            assertThat(service.claim(user, incorrecto(newCode))).isEmpty();
        }
        service.complete(user, service.claim(user, newCode).orElseThrow());
        assertSinDesafio(user);
    }

    @Test
    void claimActivoImpideReemplazoYSoloSuTokenPuedeLiberarloOCompletarlo() {
        long user = 907;
        String code = service.issue(user).orElseThrow().code();
        String claim = service.claim(user, code).orElseThrow();
        assertThat(redis.expire(key(user, "cooldown"), Duration.ZERO)).isTrue();
        assertThat(service.issue(user)).isEmpty();
        assertThat(redis.opsForValue().get(key(user, "code"))).isEqualTo(code);

        service.release(user, "token-ajeno");
        service.complete(user, "token-ajeno");
        assertThat(service.claim(user, code)).isEmpty();
        assertThat(redis.opsForValue().get(key(user, "claim"))).isEqualTo(claim);

        service.release(user, claim);
        String retryClaim = service.claim(user, code).orElseThrow();
        service.complete(user, retryClaim);
        assertSinDesafio(user);
    }

    @Test
    void claimUsaTtlRestanteYRollbackNoResucitaUnCodigoExpirado() {
        long user = 908;
        String code = service.issue(user).orElseThrow().code();
        assertThat(redis.expire(key(user, "code"), Duration.ofSeconds(2))).isTrue();
        String claim = service.claim(user, code).orElseThrow();
        assertThat(redis.getExpire(key(user, "claim"), TimeUnit.MILLISECONDS)).isBetween(1L, 2000L);
        assertThat(redis.expire(key(user, "code"), Duration.ZERO)).isTrue();
        service.release(user, claim);
        assertThat(service.claim(user, code)).isEmpty();
        assertThat(redis.hasKey(key(user, "code"))).isFalse();
    }

    @Test
    void recuperacionYLoginMfaTienenPropositosSeparados() {
        long user = 909;
        MfaService mfa = new MfaService(redis);
        String resetCode = service.issue(user).orElseThrow().code();
        mfa.generateAndStore(user);
        // Evita que una colisión aleatoria de seis dígitos altere la prueba de aislamiento.
        String loginCode = incorrecto(resetCode);
        redis.opsForValue().set("mfa:" + user, loginCode, MfaService.TTL);

        assertThat(service.claim(user, loginCode)).isEmpty();
        assertThat(mfa.verify(user, resetCode)).isFalse();
        String resetClaim = service.claim(user, resetCode).orElseThrow();
        service.complete(user, resetClaim);
        assertThat(mfa.verify(user, loginCode)).isTrue();
        assertSinDesafio(user);
    }

    private static Optional<String> claimAlIniciar(long user, String code, CountDownLatch listas,
                                                   CountDownLatch iniciar) throws InterruptedException {
        listas.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Inicio concurrente no recibido");
        return service.claim(user, code);
    }

    private static String incorrecto(String code) {
        return "000000".equals(code) ? "111111" : "000000";
    }

    private static String key(long user, String suffix) {
        return "password-reset:{" + user + "}:" + suffix;
    }

    private static void assertSinDesafio(long user) {
        for (String suffix : new String[] {"code", "attempts", "claim"}) {
            assertThat(redis.hasKey(key(user, suffix))).as(suffix).isFalse();
        }
    }
}
