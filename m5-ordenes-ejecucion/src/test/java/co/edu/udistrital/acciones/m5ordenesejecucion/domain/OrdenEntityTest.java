package co.edu.udistrital.acciones.m5ordenesejecucion.domain;

import co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence.OrdenEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class OrdenEntityTest {

    private OrdenEntity entity;
    private Instant ahora;
    private UUID clientOrderId;

    @BeforeEach
    void setUp() {
        ahora = Instant.now();
        clientOrderId = UUID.randomUUID();
        entity = new OrdenEntity();
        entity.setId(1L);
        entity.setUsuarioId(100L);
        entity.setAccionId(200L);
        entity.setTicker("AAPL");
        entity.setTipoOrden(TipoOrden.LIMIT);
        entity.setLadoOrden(LadoOrden.COMPRA);
        entity.setVigenciaOrden(VigenciaOrden.DIA);
        entity.setEstadoOrden(EstadoOrden.PENDIENTE);
        entity.setConfiguracionComisionId(10L);
        entity.setCantidad(50);
        entity.setPrecioLimite(new BigDecimal("150.0000"));
        entity.setPrecioActivacion(new BigDecimal("149.0000"));
        entity.setPrecioEstimado(new BigDecimal("150.0000"));
        entity.setRequiereEncolamiento(false);
        entity.setConfirmadoEncolamientoEn(null);
        entity.setClientOrderId(clientOrderId);
        entity.setBrokerOrderId("BRK123");
        entity.setReservaFondosId(1000L);
        entity.setReservaPosicionId(2000L);
        entity.setExpiraEn(ahora.plusSeconds(3600));
        entity.setVersion(0L);
        entity.setCreadaEn(ahora);
        entity.setActualizadaEn(ahora);
    }

    @Test
    void gettersYSetters() {
        assertThat(entity.getId()).isEqualTo(1L);
        assertThat(entity.getUsuarioId()).isEqualTo(100L);
        assertThat(entity.getAccionId()).isEqualTo(200L);
        assertThat(entity.getTicker()).isEqualTo("AAPL");
        assertThat(entity.getTipoOrden()).isEqualTo(TipoOrden.LIMIT);
        assertThat(entity.getLadoOrden()).isEqualTo(LadoOrden.COMPRA);
        assertThat(entity.getVigenciaOrden()).isEqualTo(VigenciaOrden.DIA);
        assertThat(entity.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
        assertThat(entity.getConfiguracionComisionId()).isEqualTo(10L);
        assertThat(entity.getCantidad()).isEqualTo(50);
        assertThat(entity.getPrecioLimite()).isEqualTo(new BigDecimal("150.0000"));
        assertThat(entity.getPrecioActivacion()).isEqualTo(new BigDecimal("149.0000"));
        assertThat(entity.getPrecioEstimado()).isEqualTo(new BigDecimal("150.0000"));
        assertThat(entity.getRequiereEncolamiento()).isFalse();
        assertThat(entity.getConfirmadoEncolamientoEn()).isNull();
        assertThat(entity.getClientOrderId()).isEqualTo(clientOrderId);
        assertThat(entity.getBrokerOrderId()).isEqualTo("BRK123");
        assertThat(entity.getReservaFondosId()).isEqualTo(1000L);
        assertThat(entity.getReservaPosicionId()).isEqualTo(2000L);
        assertThat(entity.getExpiraEn()).isEqualTo(ahora.plusSeconds(3600));
        assertThat(entity.getVersion()).isEqualTo(0L);
        assertThat(entity.getCreadaEn()).isEqualTo(ahora);
        assertThat(entity.getActualizadaEn()).isEqualTo(ahora);
    }

    @Test
    void toDomain() {
        Orden domain = entity.toDomain();

        assertThat(domain.getId()).isEqualTo(1L);
        assertThat(domain.getUsuarioId()).isEqualTo(100L);
        assertThat(domain.getTicker()).isEqualTo("AAPL");
        assertThat(domain.getTipoOrden()).isEqualTo(TipoOrden.LIMIT);
        assertThat(domain.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
    }

    @Test
    void fromDomain() {
        Orden orden = new Orden(
                1L, 100L, 200L, "AAPL", TipoOrden.LIMIT, LadoOrden.COMPRA,
                VigenciaOrden.DIA, EstadoOrden.PENDIENTE, 10L, 50,
                new BigDecimal("150.0000"), new BigDecimal("149.0000"),
                new BigDecimal("150.0000"), false, null,
                clientOrderId, "BRK123", 1000L, 2000L,
                ahora.plusSeconds(3600), 0L, ahora, ahora
        );

        OrdenEntity entityFromDomain = OrdenEntity.fromDomain(orden);

        assertThat(entityFromDomain.getId()).isEqualTo(1L);
        assertThat(entityFromDomain.getUsuarioId()).isEqualTo(100L);
        assertThat(entityFromDomain.getTicker()).isEqualTo("AAPL");
        assertThat(entityFromDomain.getTipoOrden()).isEqualTo(TipoOrden.LIMIT);
        assertThat(entityFromDomain.getEstadoOrden()).isEqualTo(EstadoOrden.PENDIENTE);
    }
}