package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EvaluacionPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.MiSuscripcionResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.SuscripcionResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.service.EvaluadorPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.domain.Suscripcion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class ConsultarMiSuscripcionUseCase {

    private final SuscripcionRepositoryPort suscripciones;
    private final EvaluadorPremium evaluador;
    private final Clock clock;

    public ConsultarMiSuscripcionUseCase(SuscripcionRepositoryPort suscripciones,
                                         EvaluadorPremium evaluador,
                                         Clock clock) {
        this.suscripciones = suscripciones;
        this.evaluador = evaluador;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MiSuscripcionResponse ejecutar(Long userId) {
        Instant ahora = clock.instant();
        EvaluacionPremium evaluacion = evaluador.evaluar(userId, ahora);
        Suscripcion suscripcion = suscripciones.findVigenteByUserId(userId)
                .or(() -> suscripciones.findUltimaByUserId(userId))
                .orElse(null);
        return new MiSuscripcionResponse(
                userId,
                evaluacion.premium(),
                evaluacion.validUntil(),
                suscripcion == null ? null : SuscripcionResponse.de(suscripcion));
    }
}
