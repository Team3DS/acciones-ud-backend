package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoRecibo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoReciboTest {

    @Test
    void mapeaLosEstadosDeStripe() {
        assertThat(EstadoRecibo.desdeStripe("paid", 1)).isEqualTo(EstadoRecibo.PAGADO);
        assertThat(EstadoRecibo.desdeStripe("void", 0)).isEqualTo(EstadoRecibo.ANULADO);
        assertThat(EstadoRecibo.desdeStripe("uncollectible", 3)).isEqualTo(EstadoRecibo.INCOBRABLE);
    }

    @Test
    void openEsFallidoSoloCuandoHuboIntentos() {
        assertThat(EstadoRecibo.desdeStripe("open", 0)).isEqualTo(EstadoRecibo.ABIERTO);
        assertThat(EstadoRecibo.desdeStripe("open", 2)).isEqualTo(EstadoRecibo.FALLIDO);
    }

    @Test
    void unEstadoDesconocidoONuloEsAbierto() {
        assertThat(EstadoRecibo.desdeStripe(null, 0)).isEqualTo(EstadoRecibo.ABIERTO);
        assertThat(EstadoRecibo.desdeStripe("draft", 0)).isEqualTo(EstadoRecibo.ABIERTO);
    }
}
