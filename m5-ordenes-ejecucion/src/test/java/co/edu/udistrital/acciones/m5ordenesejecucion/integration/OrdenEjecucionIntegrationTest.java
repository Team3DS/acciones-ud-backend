package co.edu.udistrital.acciones.m5ordenesejecucion.integration;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ProcesarEjecucionUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfiguracionComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfiguracionComision;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class OrdenEjecucionIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("m5_test")
            .withUsername("test")
            .withPassword("test");

    @Container
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbit::getHost);
        registry.add("spring.rabbitmq.port", rabbit::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbit::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbit::getAdminPassword);
        registry.add("app.outbox.lote", () -> "10");
        registry.add("app.outbox.intervalo-ms", () -> "500");
    }

    @Autowired
    private CrearOrdenUseCase crearOrdenUseCase;

    @Autowired
    private ProcesarEjecucionUseCase procesarEjecucionUseCase;

    @Autowired
    private ConfiguracionComisionRepositoryPort comisionRepository;

    @Autowired
    private PublicadorOrdenesPort publicador;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    private UUID clientOrderId;

    @BeforeEach
    void setUp() {
        clientOrderId = UUID.randomUUID();
        ConfiguracionComision comision = new ConfiguracionComision();
        comision.setMercadoCodigo("NYSE");
        comision.setPorcentaje(new BigDecimal("0.0020"));
        comision.setVigenteDesde(Instant.now().minus(1, ChronoUnit.DAYS));
        comision.setVigenteHasta(Instant.now().plus(30, ChronoUnit.DAYS));
        comisionRepository.save(comision);
    }

    @Test
    void crearOrdenEjecutarYPublicarEvento() {
        CrearOrdenUseCase.CrearOrdenCommand cmd = new CrearOrdenUseCase.CrearOrdenCommand(
                1L, 1L, "AAPL", TipoOrden.MARKET, LadoOrden.COMPRA,
                VigenciaOrden.DIA, 10, null, null, null, clientOrderId
        );

        Orden orden = crearOrdenUseCase.ejecutar(cmd);

        assertThat(orden).isNotNull();
        assertThat(orden.getId()).isNotNull();
        assertThat(orden.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(orden.getRequiereEncolamiento()).isTrue();

        UUID executionId = UUID.randomUUID();
        ProcesarEjecucionUseCase.ProcesarEjecucionCommand execCmd = new ProcesarEjecucionUseCase.ProcesarEjecucionCommand(
                orden.getId(),
                executionId.toString(),
                null,
                10,
                new BigDecimal("150.25"),
                null,
                Instant.now()
        );

        var ejecucion = procesarEjecucionUseCase.ejecutar(execCmd);

        assertThat(ejecucion).isNotNull();
        assertThat(ejecucion.getId()).isNotNull();
        assertThat(ejecucion.getCantidad()).isEqualTo(10);
        assertThat(ejecucion.getPrecioEjecucion()).isEqualByComparingTo("150.25");

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            Message received = rabbitTemplate.receive("m5.ordenes.queue");
            assertThat(received).isNotNull();
            String body = new String(received.getBody());
            assertThat(body).contains("m5.orden.ejecutada");
            assertThat(body).contains(executionId.toString());
            assertThat(body).contains(String.valueOf(orden.getId()));
        });
    }
}