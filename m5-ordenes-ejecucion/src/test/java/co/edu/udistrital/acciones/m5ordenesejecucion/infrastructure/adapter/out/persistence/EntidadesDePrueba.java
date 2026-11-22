package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.adapter.out.persistence;

import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoActor;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.VigenciaOrden;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Entidades completas para las pruebas de los adaptadores de persistencia. */
final class EntidadesDePrueba {

    static final Instant AHORA = Instant.parse("2026-11-22T15:00:00Z");

    private EntidadesDePrueba() {
    }

    static OrdenEntity orden(Long id, String ticker) {
        OrdenEntity entity = new OrdenEntity();
        entity.setId(id);
        entity.setUsuarioId(100L);
        entity.setAccionId(200L);
        entity.setTicker(ticker);
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
        entity.setClientOrderId(UUID.randomUUID());
        entity.setBrokerOrderId("BRK-" + id);
        entity.setReservaFondosId(1000L);
        entity.setReservaPosicionId(2000L);
        entity.setExpiraEn(AHORA.plusSeconds(3600));
        entity.setVersion(0L);
        entity.setCreadaEn(AHORA);
        entity.setActualizadaEn(AHORA);
        return entity;
    }

    static EjecucionOrdenEntity ejecucion(Long id, Long ordenId) {
        EjecucionOrdenEntity entity = new EjecucionOrdenEntity();
        entity.setId(id);
        entity.setOrdenId(ordenId);
        entity.setBrokerExecutionId("EXEC-" + id);
        entity.setCantidad(25);
        entity.setPrecioEjecucion(new BigDecimal("150.2500"));
        entity.setComision(new BigDecimal("5.00"));
        entity.setEjecutadaEn(AHORA);
        entity.setRegistradaEn(AHORA);
        return entity;
    }

    static HistorialEstadoOrdenEntity historial(Long id, Long ordenId, EstadoOrden estado) {
        HistorialEstadoOrdenEntity entity = new HistorialEstadoOrdenEntity();
        entity.setId(id);
        entity.setOrdenId(ordenId);
        entity.setEstadoOrden(estado);
        entity.setTipoActor(TipoActor.USUARIO);
        entity.setActorUsuarioId(50L);
        entity.setCausa("Cambio de estado");
        entity.setOcurridoEn(AHORA);
        return entity;
    }

    static ConfiguracionComisionEntity comision(Long id, String mercado) {
        ConfiguracionComisionEntity entity = new ConfiguracionComisionEntity();
        entity.setId(id);
        entity.setMercadoCodigo(mercado);
        entity.setPorcentaje(new BigDecimal("0.0200"));
        entity.setVigenteDesde(AHORA.minusSeconds(86400));
        entity.setVigenteHasta(null);
        entity.setMotivo("Comision estandar");
        entity.setCreadoPorUsuarioId(1L);
        entity.setCreadoEn(AHORA);
        return entity;
    }

    static OutboxEventoEntity outbox(UUID id, String tipoEvento) {
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setId(id);
        entity.setTipoAgregado("Orden");
        entity.setAgregadoId("1");
        entity.setTipoEvento(tipoEvento);
        entity.setCarga("{\"ordenId\":1}");
        entity.setCreadoEn(AHORA);
        entity.setPublicadoEn(null);
        entity.setIntentos(0);
        entity.setUltimoError(null);
        return entity;
    }

    static ConfirmacionEjecucionEntity confirmacion(Long ejecucionId, ServicioParticipante servicio) {
        ConfirmacionEjecucionEntity entity = new ConfirmacionEjecucionEntity();
        entity.setId(new ConfirmacionEjecucionId(ejecucionId, servicio));
        entity.setPrecioPromedioCosto(new BigDecimal("150.1234"));
        entity.setConfirmadaEn(AHORA);
        return entity;
    }
}
