package co.edu.udistrital.acciones.m10suscripcionespagos.unit;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.PlanResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.PrecioPlanResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase.ListarPlanesUseCase;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.IntervaloPlan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Plan;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.PrecioPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListarPlanesUseCaseTest {

    @Mock
    private PlanRepositoryPort planes;

    @Test
    void devuelveElCatalogoConLosPreciosOrdenados() {
        Plan plan = Plan.crear("PREMIUM", "Acciones UD Premium", "Watchlist y alertas de precio");
        PrecioPlan.crear(plan, IntervaloPlan.MENSUAL, new BigDecimal("12.00"),
                "USD", "acciones_ud_premium_mensual");
        PrecioPlan.crear(plan, IntervaloPlan.ANUAL, new BigDecimal("120.00"),
                "USD", "acciones_ud_premium_anual");
        when(planes.findActivos()).thenReturn(List.of(plan));

        List<PlanResponse> respuesta = new ListarPlanesUseCase(planes).ejecutar();

        assertThat(respuesta).hasSize(1);
        PlanResponse catalogo = respuesta.getFirst();
        assertThat(catalogo.code()).isEqualTo("PREMIUM");
        assertThat(catalogo.name()).isEqualTo("Acciones UD Premium");
        assertThat(catalogo.prices())
                .extracting(PrecioPlanResponse::interval)
                .containsExactly(IntervaloPlan.MENSUAL, IntervaloPlan.ANUAL);
        assertThat(catalogo.prices())
                .extracting(PrecioPlanResponse::amount)
                .containsExactly(new BigDecimal("12.00"), new BigDecimal("120.00"));
        assertThat(catalogo.prices())
                .extracting(PrecioPlanResponse::currency)
                .containsOnly("USD");
    }

    @Test
    void devuelveUnaListaVaciaCuandoNoHayPlanesActivos() {
        when(planes.findActivos()).thenReturn(List.of());

        assertThat(new ListarPlanesUseCase(planes).ejecutar()).isEmpty();
    }
}
