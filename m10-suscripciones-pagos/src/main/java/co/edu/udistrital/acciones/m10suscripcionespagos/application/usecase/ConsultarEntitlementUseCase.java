package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EntitlementResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EvaluacionPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;

@Service
public class ConsultarEntitlementUseCase {

    private final EvaluadorPremium evaluador;
    private final Clock clock;

    public ConsultarEntitlementUseCase(EvaluadorPremium evaluador, Clock clock) {
        this.evaluador = evaluador;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public EntitlementResponse ejecutar(Long userId) {
        EvaluacionPremium evaluacion = evaluador.evaluar(userId, clock.instant());
        return new EntitlementResponse(evaluacion.premium(), evaluacion.validUntil());
    }
}
