package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.TransicionSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.NegocioException;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.excepcion.TransicionSuscripcionException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class SuscripcionTest {

    private static final long USUARIO = 4101L;
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant DENTRO = Instant.parse("2026-01-15T00:00:00Z");

    private final PrecioPlan precio = precioMensual();

    @Test
    void otorgaPremiumEnActivaYEnMora() {
        assertThat(suscripcion(EstadoSuscripcion.ACTIVA).otorgaPremium(DENTRO)).isTrue();
        assertThat(suscripcion(EstadoSuscripcion.EN_MORA).otorgaPremium(DENTRO)).isTrue();
    }

    @Test
    void noOtorgaPremiumEnIncompletaCanceladaOVencida() {
        assertThat(suscripcion(EstadoSuscripcion.INCOMPLETA).otorgaPremium(DENTRO)).isFalse();
        assertThat(suscripcion(EstadoSuscripcion.CANCELADA).otorgaPremium(DENTRO)).isFalse();
        assertThat(suscripcion(EstadoSuscripcion.VENCIDA).otorgaPremium(DENTRO)).isFalse();
    }

    @Test
    void noOtorgaPremiumEnElLimiteDelPeriodo() {
        assertThat(suscripcion(EstadoSuscripcion.ACTIVA).otorgaPremium(FIN)).isFalse();
    }

    @Test
    void noOtorgaPremiumCuandoElPeriodoYaPaso() {
        assertThat(suscripcion(EstadoSuscripcion.EN_MORA).otorgaPremium(FIN.plusSeconds(1))).isFalse();
    }

    @ParameterizedTest
    @MethodSource("transicionesValidas")
    void permiteLasTransicionesDelDiagrama(EstadoSuscripcion desde, EstadoSuscripcion hasta) {
        Suscripcion suscripcion = suscripcion(desde);

        TransicionSuscripcion transicion = suscripcion.sincronizar(datos(hasta), DENTRO);

        assertThat(suscripcion.getEstado()).isEqualTo(hasta);
        assertThat(transicion.anterior()).isEqualTo(desde);
        assertThat(transicion.nuevo()).isEqualTo(hasta);
    }

    @ParameterizedTest
    @MethodSource("transicionesInvalidas")
    void rechazaLasTransicionesQueNoEstanEnElDiagrama(EstadoSuscripcion desde, EstadoSuscripcion hasta) {
        Suscripcion suscripcion = suscripcion(desde);

        assertThatThrownBy(() -> suscripcion.sincronizar(datos(hasta), DENTRO))
                .isInstanceOf(TransicionSuscripcionException.class)
                .hasMessageContaining(desde.name())
                .hasMessageContaining(hasta.name());
    }

    @Test
    void sincronizarActualizaLosDatosYDevuelveLaTransicion() {
        Suscripcion suscripcion = suscripcion(EstadoSuscripcion.ACTIVA);
        PrecioPlan anual = precioAnual();
        Instant nuevoFin = FIN.plusSeconds(3600);

        TransicionSuscripcion transicion = suscripcion.sincronizar(
                new DatosSuscripcion("sub_renovada", anual, EstadoSuscripcion.EN_MORA,
                        FIN, nuevoFin, true, DENTRO, null), DENTRO);

        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.EN_MORA);
        assertThat(suscripcion.getStripeSubscriptionId()).isEqualTo("sub_renovada");
        assertThat(suscripcion.getPrecioPlan()).isSameAs(anual);
        assertThat(suscripcion.getPeriodoInicio()).isEqualTo(FIN);
        assertThat(suscripcion.getPeriodoFin()).isEqualTo(nuevoFin);
        assertThat(suscripcion.isCancelarAlFinal()).isTrue();
        assertThat(suscripcion.getCanceladaEn()).isEqualTo(DENTRO);
        assertThat(suscripcion.getActualizadoEn()).isEqualTo(DENTRO);
        assertThat(transicion.huboCambio()).isTrue();
        assertThat(transicion.veniaDePremium()).isTrue();
    }

    @Test
    void sincronizarEsIdempotenteEnElMismoEstado() {
        Suscripcion suscripcion = suscripcion(EstadoSuscripcion.ACTIVA);

        TransicionSuscripcion transicion = suscripcion.sincronizar(datos(EstadoSuscripcion.ACTIVA), DENTRO);

        assertThat(transicion.huboCambio()).isFalse();
        assertThat(suscripcion.getEstado()).isEqualTo(EstadoSuscripcion.ACTIVA);
    }

    @Test
    void alPasarAUnEstadoTerminalRegistraLaFinalizacion() {
        Suscripcion suscripcion = suscripcion(EstadoSuscripcion.ACTIVA);

        suscripcion.sincronizar(datos(EstadoSuscripcion.VENCIDA), DENTRO);

        assertThat(suscripcion.getFinalizadaEn()).isEqualTo(DENTRO);
    }

    @Test
    void conservaLaFinalizacionInformadaPorStripe() {
        Suscripcion suscripcion = suscripcion(EstadoSuscripcion.ACTIVA);
        Instant finalizada = FIN.plusSeconds(60);

        suscripcion.sincronizar(new DatosSuscripcion("sub_123", precio, EstadoSuscripcion.CANCELADA,
                INICIO, FIN, false, DENTRO, finalizada), DENTRO);

        assertThat(suscripcion.getFinalizadaEn()).isEqualTo(finalizada);
    }

    @Test
    void rechazaUnPeriodoInvertido() {
        DatosSuscripcion datos = new DatosSuscripcion("sub_123", precio, EstadoSuscripcion.ACTIVA,
                FIN, INICIO, false, null, null);

        assertThatThrownBy(() -> Suscripcion.crear(USUARIO, datos, INICIO))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("posterior");
    }

    @Test
    void rechazaUnUsuarioNoPositivo() {
        assertThatThrownBy(() -> Suscripcion.crear(0L, datos(EstadoSuscripcion.ACTIVA), INICIO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("positivo");
    }

    @Test
    void rechazaUnaSuscripcionSinIdentificadorDeStripe() {
        DatosSuscripcion datos = new DatosSuscripcion(" ", precio, EstadoSuscripcion.ACTIVA,
                INICIO, FIN, false, null, null);

        assertThatThrownBy(() -> Suscripcion.crear(USUARIO, datos, INICIO))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Stripe");
    }

    static Stream<Arguments> transicionesValidas() {
        return Stream.of(
                arguments(EstadoSuscripcion.INCOMPLETA, EstadoSuscripcion.INCOMPLETA),
                arguments(EstadoSuscripcion.INCOMPLETA, EstadoSuscripcion.ACTIVA),
                arguments(EstadoSuscripcion.INCOMPLETA, EstadoSuscripcion.VENCIDA),
                arguments(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.ACTIVA),
                arguments(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.EN_MORA),
                arguments(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.CANCELADA),
                arguments(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.VENCIDA),
                arguments(EstadoSuscripcion.EN_MORA, EstadoSuscripcion.EN_MORA),
                arguments(EstadoSuscripcion.EN_MORA, EstadoSuscripcion.ACTIVA),
                arguments(EstadoSuscripcion.EN_MORA, EstadoSuscripcion.CANCELADA),
                arguments(EstadoSuscripcion.EN_MORA, EstadoSuscripcion.VENCIDA),
                arguments(EstadoSuscripcion.CANCELADA, EstadoSuscripcion.CANCELADA),
                arguments(EstadoSuscripcion.VENCIDA, EstadoSuscripcion.VENCIDA));
    }

    static Stream<Arguments> transicionesInvalidas() {
        return Stream.of(
                arguments(EstadoSuscripcion.INCOMPLETA, EstadoSuscripcion.EN_MORA),
                arguments(EstadoSuscripcion.INCOMPLETA, EstadoSuscripcion.CANCELADA),
                arguments(EstadoSuscripcion.ACTIVA, EstadoSuscripcion.INCOMPLETA),
                arguments(EstadoSuscripcion.EN_MORA, EstadoSuscripcion.INCOMPLETA),
                arguments(EstadoSuscripcion.CANCELADA, EstadoSuscripcion.ACTIVA),
                arguments(EstadoSuscripcion.CANCELADA, EstadoSuscripcion.VENCIDA),
                arguments(EstadoSuscripcion.VENCIDA, EstadoSuscripcion.ACTIVA),
                arguments(EstadoSuscripcion.VENCIDA, EstadoSuscripcion.CANCELADA));
    }

    private Suscripcion suscripcion(EstadoSuscripcion estado) {
        return Suscripcion.crear(USUARIO, datos(estado), INICIO);
    }

    private DatosSuscripcion datos(EstadoSuscripcion estado) {
        return new DatosSuscripcion("sub_123", precio, estado, INICIO, FIN, false, null, null);
    }

    private static PrecioPlan precioMensual() {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas de precio");
        return PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
    }

    private static PrecioPlan precioAnual() {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas de precio");
        return PrecioPlan.crear(plan, IntervaloPlan.ANUAL, new BigDecimal("120.00"),
                "USD", "acciones_ud_premium_anual");
    }
}
