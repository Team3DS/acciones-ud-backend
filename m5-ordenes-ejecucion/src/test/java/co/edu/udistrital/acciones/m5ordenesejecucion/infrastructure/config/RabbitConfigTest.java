package co.edu.udistrital.acciones.m5ordenesejecucion.infrastructure.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitConfigTest {

    @Test
    void constantesExchanges() {
        assertThat(RabbitConfig.EXCHANGE).isEqualTo("acciones.ud");
        assertThat(RabbitConfig.EXCHANGE_DLX).isEqualTo("acciones.ud.dlx");
    }

    @Test
    void constantesColas() {
        assertThat(RabbitConfig.COLA_ORDENES).isEqualTo("m5.ordenes.queue");
        assertThat(RabbitConfig.COLA_ORDENES_DLQ).isEqualTo("m5.ordenes.dlq");
    }

    @Test
    void constantesRoutingKeysEntrada() {
        assertThat(RabbitConfig.ROUTING_M4_MERCADO_ABIERTO).isEqualTo("m4.mercado.abierto");
        assertThat(RabbitConfig.ROUTING_M2_FONDOS_CONFIRMADOS).isEqualTo("m2.fondos.confirmados");
        assertThat(RabbitConfig.ROUTING_M6_POSICIONES_ACTUALIZADAS).isEqualTo("m6.posiciones.actualizadas");
    }

    @Test
    void constantesRoutingKeysSalida() {
        assertThat(RabbitConfig.RK_ORDEN_CREADA).isEqualTo("m5.orden.creada");
        assertThat(RabbitConfig.RK_ORDEN_EJECUTADA).isEqualTo("m5.orden.ejecutada");
        assertThat(RabbitConfig.RK_ORDEN_CANCELADA).isEqualTo("m5.orden.cancelada");
        assertThat(RabbitConfig.RK_ORDEN_RECHAZADA).isEqualTo("m5.orden.rechazada");
    }
}