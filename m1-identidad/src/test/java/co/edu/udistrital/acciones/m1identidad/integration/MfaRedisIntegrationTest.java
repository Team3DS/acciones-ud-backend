package co.edu.udistrital.acciones.m1identidad.integration;

import co.edu.udistrital.acciones.m1identidad.infrastructure.service.MfaService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifica el consumo y los intentos del OTP contra Redis real, sin contexto Spring ni base SQL. */
class MfaRedisIntegrationTest {

    private static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);
    private static LettuceConnectionFactory connectionFactory;
    private static StringRedisTemplate redis;
    private static MfaService service;

    @BeforeAll
    static void conectarRedis() {
        REDIS.start();
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate();
        redis.setConnectionFactory(connectionFactory);
        redis.afterPropertiesSet();
        service = new MfaService(redis);
    }

    @AfterAll
    static void cerrarRedis() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        REDIS.stop();
    }

    @Test
    void unErrorNoConsumeElCodigoYPermiteCorregirlo() {
        long userId = 701;
        String code = service.generateAndStore(userId);

        assertThat(service.verify(userId, incorrecto(code))).isFalse();
        assertThat(service.verify(userId, code)).isTrue();
        assertSinDesafio(userId);
    }

    @Test
    void elQuintoErrorInvalidaElCodigoAunqueDespuesSeEnvieElCorrecto() {
        long userId = 702;
        String code = service.generateAndStore(userId);

        for (int intento = 0; intento < 5; intento++) {
            assertThat(service.verify(userId, incorrecto(code))).isFalse();
        }

        assertSinDesafio(userId);
        assertThat(service.verify(userId, code)).isFalse();
    }

    @Test
    void elCodigoCorrectoEnElQuintoIntentoTodaviaEsValido() {
        long userId = 703;
        String code = service.generateAndStore(userId);
        for (int intento = 0; intento < 4; intento++) {
            assertThat(service.verify(userId, incorrecto(code))).isFalse();
        }

        assertThat(service.verify(userId, code)).isTrue();
        assertSinDesafio(userId);
    }

    @Test
    void unCodigoVerificadoNoSePuedeReutilizar() {
        long userId = 704;
        String code = service.generateAndStore(userId);

        assertThat(service.verify(userId, code)).isTrue();
        assertThat(service.verify(userId, code)).isFalse();
        assertSinDesafio(userId);
    }

    @Test
    void dosVerificacionesConcurrentesSoloConcedenUnAcceso() throws Exception {
        long userId = 705;
        String code = service.generateAndStore(userId);
        CountDownLatch listas = new CountDownLatch(2);
        CountDownLatch iniciar = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> primera = executor.submit(() -> verificarAlIniciar(userId, code, listas, iniciar));
            Future<Boolean> segunda = executor.submit(() -> verificarAlIniciar(userId, code, listas, iniciar));
            assertThat(listas.await(10, TimeUnit.SECONDS)).isTrue();
            iniciar.countDown();

            boolean primerResultado = primera.get(10, TimeUnit.SECONDS);
            boolean segundoResultado = segunda.get(10, TimeUnit.SECONDS);
            assertThat((primerResultado ? 1 : 0) + (segundoResultado ? 1 : 0)).isEqualTo(1);
            assertSinDesafio(userId);
        } finally {
            iniciar.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void regenerarTrasCuatroErroresRestableceLosIntentosYLaVigenciaDeCincoMinutos() {
        long userId = 706;
        String anterior = service.generateAndStore(userId);
        for (int intento = 0; intento < 4; intento++) {
            assertThat(service.verify(userId, incorrecto(anterior))).isFalse();
        }

        String nuevo = service.generateAndStore(userId);
        assertThat(nuevo).matches("\\d{6}");
        assertThat(redis.getExpire("mfa:" + userId, TimeUnit.SECONDS)).isBetween(290L, 300L);
        for (int intento = 0; intento < 4; intento++) {
            assertThat(service.verify(userId, incorrecto(nuevo))).isFalse();
        }
        assertThat(service.verify(userId, nuevo)).isTrue();
        assertSinDesafio(userId);
    }

    @Test
    void unCodigoExpiradoNoConcedeAcceso() {
        long userId = 707;
        String code = service.generateAndStore(userId);
        assertThat(redis.expire("mfa:" + userId, Duration.ZERO)).isTrue();

        assertThat(service.verify(userId, code)).isFalse();
        assertThat(redis.hasKey("mfa:" + userId)).isFalse();
    }

    private static boolean verificarAlIniciar(long userId, String code, CountDownLatch listas,
                                              CountDownLatch iniciar) throws InterruptedException {
        listas.countDown();
        if (!iniciar.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("No se inició la verificación concurrente");
        }
        return service.verify(userId, code);
    }

    private static String incorrecto(String code) {
        return "000000".equals(code) ? "111111" : "000000";
    }

    private static void assertSinDesafio(long userId) {
        assertThat(redis.hasKey("mfa:" + userId)).isFalse();
        assertThat(redis.hasKey("mfa:attempts:" + userId)).isFalse();
    }
}
