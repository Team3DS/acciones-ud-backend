package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EntitlementResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.MiSuscripcionResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarEntitlementUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ConsultarMiSuscripcionUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.DatosSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.EstadoSuscripcion;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConsultasSuscripcionUseCaseTest {

    private static final long USUARIO = 4101L;
    private static final Instant INICIO = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant FIN = Instant.parse("2026-02-01T00:00:00Z");
    private static final Instant DENTRO = Instant.parse("2026-01-15T00:00:00Z");
    private static final Instant INICIO_ANTIGUO = Instant.parse("2025-12-01T00:00:00Z");
    private static final Instant FIN_ANTIGUO = Instant.parse("2026-01-01T00:00:00Z");

    @Mock
    private SuscripcionRepositoryPort suscripciones;

    private ConsultarMiSuscripcionUseCase miSuscripcion;
    private ConsultarEntitlementUseCase entitlement;

    @BeforeEach
    void prepararCasos() {
        Clock clock = Clock.fixed(DENTRO, ZoneOffset.UTC);
        EvaluadorPremium evaluador = new EvaluadorPremium(suscripciones);
        miSuscripcion = new ConsultarMiSuscripcionUseCase(suscripciones, evaluador, clock);
        entitlement = new ConsultarEntitlementUseCase(evaluador, clock);
    }

    @Test
    void sinSuscripcionDevuelvePremiumFalsoYSubscriptionNula() {
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.empty());
        when(suscripciones.findUltimaByUserId(USUARIO)).thenReturn(Optional.empty());

        MiSuscripcionResponse respuesta = miSuscripcion.ejecutar(USUARIO);

        assertThat(respuesta.userId()).isEqualTo(USUARIO);
        assertThat(respuesta.premium()).isFalse();
        assertThat(respuesta.validUntil()).isNull();
        assertThat(respuesta.subscription()).isNull();
    }

    @Test
    void unaSuscripcionActivaDevuelvePremiumConSuPeriodoFin() {
        Suscripcion activa = suscripcion(EstadoSuscripcion.ACTIVA, INICIO, FIN);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.of(activa));

        MiSuscripcionResponse respuesta = miSuscripcion.ejecutar(USUARIO);

        assertThat(respuesta.premium()).isTrue();
        assertThat(respuesta.validUntil()).isEqualTo(FIN);
        assertThat(respuesta.subscription()).isNotNull();
        assertThat(respuesta.subscription().status()).isEqualTo(EstadoSuscripcion.ACTIVA);
        assertThat(respuesta.subscription().plan()).isEqualTo("Acciones UD Premium");
        assertThat(respuesta.subscription().interval()).isEqualTo(IntervaloPlan.MENSUAL);
        assertThat(respuesta.subscription().amount()).isEqualByComparingTo("12.00");
        assertThat(respuesta.subscription().currency()).isEqualTo("USD");
    }

    @Test
    void unaSuscripcionEnMoraVigenteSigueSiendoPremium() {
        Suscripcion enMora = suscripcion(EstadoSuscripcion.EN_MORA, INICIO, FIN);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.of(enMora));

        MiSuscripcionResponse respuesta = miSuscripcion.ejecutar(USUARIO);

        assertThat(respuesta.premium()).isTrue();
        assertThat(respuesta.validUntil()).isEqualTo(FIN);
    }

    @Test
    void unaSuscripcionEnMoraVencidaNoEsPremium() {
        Suscripcion enMora = suscripcion(EstadoSuscripcion.EN_MORA, INICIO_ANTIGUO, FIN_ANTIGUO);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.of(enMora));

        MiSuscripcionResponse respuesta = miSuscripcion.ejecutar(USUARIO);

        assertThat(respuesta.premium()).isFalse();
        assertThat(respuesta.validUntil()).isNull();
        assertThat(respuesta.subscription()).isNotNull();
        assertThat(respuesta.subscription().status()).isEqualTo(EstadoSuscripcion.EN_MORA);
    }

    @Test
    void unaSuscripcionCanceladaConservaElHistorialPeroNoEsPremium() {
        Suscripcion cancelada = suscripcion(EstadoSuscripcion.CANCELADA, INICIO, FIN);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.empty());
        when(suscripciones.findUltimaByUserId(USUARIO)).thenReturn(Optional.of(cancelada));

        MiSuscripcionResponse respuesta = miSuscripcion.ejecutar(USUARIO);

        assertThat(respuesta.premium()).isFalse();
        assertThat(respuesta.validUntil()).isNull();
        assertThat(respuesta.subscription()).isNotNull();
        assertThat(respuesta.subscription().status()).isEqualTo(EstadoSuscripcion.CANCELADA);
    }

    @Test
    void conVariasSuscripcionesHistoricasDevuelveLaUltima() {
        Suscripcion vieja = suscripcion(EstadoSuscripcion.CANCELADA, INICIO, FIN);
        Suscripcion reciente = suscripcion(EstadoSuscripcion.VENCIDA, FIN, FIN.plusSeconds(86400));
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.empty());
        when(suscripciones.findUltimaByUserId(USUARIO)).thenReturn(Optional.of(reciente));

        MiSuscripcionResponse respuesta = miSuscripcion.ejecutar(USUARIO);

        assertThat(respuesta.subscription()).isNotNull();
        assertThat(respuesta.subscription().status()).isEqualTo(EstadoSuscripcion.VENCIDA);
        assertThat(respuesta.subscription().currentPeriodEnd()).isEqualTo(reciente.getPeriodoFin());
        assertThat(vieja.getEstado()).isEqualTo(EstadoSuscripcion.CANCELADA);
    }

    @Test
    void elEntitlementDeUnUsuarioActivoTraeValidUntil() {
        Suscripcion activa = suscripcion(EstadoSuscripcion.ACTIVA, INICIO, FIN);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.of(activa));

        EntitlementResponse respuesta = entitlement.ejecutar(USUARIO);

        assertThat(respuesta.active()).isTrue();
        assertThat(respuesta.validUntil()).isEqualTo(FIN);
    }

    @Test
    void elEntitlementSinSuscripcionDevuelveValidUntilNulo() {
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.empty());

        EntitlementResponse respuesta = entitlement.ejecutar(USUARIO);

        assertThat(respuesta.active()).isFalse();
        assertThat(respuesta.validUntil()).isNull();
    }

    @Test
    void elEntitlementEnElLimiteDelPeriodoNoEstaActivo() {
        Suscripcion activa = suscripcion(EstadoSuscripcion.ACTIVA, INICIO, DENTRO);
        when(suscripciones.findVigenteByUserId(USUARIO)).thenReturn(Optional.of(activa));

        EntitlementResponse respuesta = entitlement.ejecutar(USUARIO);

        assertThat(respuesta.active()).isFalse();
        assertThat(respuesta.validUntil()).isNull();
    }

    private static Suscripcion suscripcion(EstadoSuscripcion estado, Instant inicio, Instant fin) {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas de precio");
        PrecioPlan precio = PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
        return Suscripcion.crear(USUARIO,
                new DatosSuscripcion("sub_123", precio, estado, inicio, fin, false, null, null),
                inicio);
    }
}
