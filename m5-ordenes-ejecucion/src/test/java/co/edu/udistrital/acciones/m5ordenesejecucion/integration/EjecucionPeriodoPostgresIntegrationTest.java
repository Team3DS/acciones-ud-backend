package co.edu.udistrital.acciones.m5ordenesejecucion.integration;

import static org.assertj.core.api.Assertions.assertThat;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.EjecucionOrdenEntity;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.EjecucionOrdenJpaRepository;
import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.OrdenEntity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

@DataJpaTest(
        properties = {
            "spring.flyway.enabled=false",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.jpa.properties.hibernate.jdbc.time_zone=UTC",
            "debug=false",
            "logging.level.root=WARN"
        })
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EjecucionPeriodoPostgresIntegrationTest {
    private static final Instant EARLY = Instant.parse("2026-10-01T12:00:00Z");
    private static final Instant MIDDLE = Instant.parse("2026-10-15T12:00:00Z");
    private static final Instant LATE = Instant.parse("2026-10-31T12:00:00Z");

    @Container
    static PostgreSQLContainer<?> database =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("m5_periodo_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", database::getJdbcUrl);
        registry.add("spring.datasource.username", database::getUsername);
        registry.add("spring.datasource.password", database::getPassword);
    }

    @Autowired TestEntityManager entities;
    @Autowired EjecucionOrdenJpaRepository repository;

    @BeforeEach
    void seedOwnerSeparatedFills() {
        Long ownerOrder = persistOrder(42L);
        Long otherOrder = persistOrder(99L);
        persistFill(ownerOrder, "fill-middle", MIDDLE);
        persistFill(ownerOrder, "fill-early", EARLY);
        persistFill(ownerOrder, "fill-late", LATE);
        persistFill(otherOrder, "fill-other-owner", LATE);
        entities.flush();
        entities.clear();
    }

    static Stream<Arguments> periods() {
        return Stream.of(
                Arguments.of(null, null, List.of("fill-late", "fill-middle", "fill-early")),
                Arguments.of(MIDDLE, null, List.of("fill-late", "fill-middle")),
                Arguments.of(null, MIDDLE, List.of("fill-middle", "fill-early")),
                Arguments.of(MIDDLE, LATE, List.of("fill-late", "fill-middle")));
    }

    @ParameterizedTest
    @MethodSource("periods")
    void optionalBoundsUsePostgresKeepOwnerAndOrderDescending(
            Instant from, Instant to, List<String> expected) {
        assertThat(repository.findByUsuarioIdAndPeriodo(42L, from, to))
                .extracting(EjecucionOrdenEntity::getBrokerExecutionId)
                .containsExactlyElementsOf(expected);
    }

    @Test
    void anotherOwnerOnlyReceivesTheirOwnExecutions() {
        assertThat(repository.findByUsuarioIdAndPeriodo(99L, null, null))
                .extracting(EjecucionOrdenEntity::getBrokerExecutionId)
                .containsExactly("fill-other-owner");
    }

    @Test
    void unknownOwnerAndEmptyPeriodReturnNoRows() {
        assertThat(repository.findByUsuarioIdAndPeriodo(404L, null, null)).isEmpty();
        assertThat(
                        repository.findByUsuarioIdAndPeriodo(
                                42L,
                                Instant.parse("2027-01-01T00:00:00Z"),
                                Instant.parse("2027-01-31T23:59:59Z")))
                .isEmpty();
    }

    private Long persistOrder(Long owner) {
        Orden order = new Orden();
        order.setUsuarioId(owner);
        order.setAccionId(1L);
        order.setTicker("AAPL");
        order.setTipoOrden(TipoOrden.MARKET);
        order.setLadoOrden(LadoOrden.COMPRA);
        order.setVigenciaOrden(VigenciaOrden.DIA);
        order.setEstadoOrden(EstadoOrden.EJECUTADA);
        order.setConfiguracionComisionId(1L);
        order.setCantidad(6);
        order.setPrecioEstimado(new BigDecimal("100.00"));
        order.setClientOrderId(UUID.randomUUID());
        order.setCreadaEn(EARLY.minusSeconds(60));
        order.setActualizadaEn(LATE);
        return entities.persistAndFlush(OrdenEntity.fromDomain(order)).getId();
    }

    private void persistFill(Long orderId, String executionId, Instant executedAt) {
        EjecucionOrden fill =
                new EjecucionOrden(
                        null,
                        orderId,
                        executionId,
                        2,
                        new BigDecimal("100.00"),
                        new BigDecimal("0.40"),
                        executedAt,
                        executedAt);
        entities.persist(EjecucionOrdenEntity.fromDomain(fill));
    }
}
