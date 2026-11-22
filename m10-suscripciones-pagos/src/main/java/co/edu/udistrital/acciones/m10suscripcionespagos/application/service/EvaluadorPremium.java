package co.edu.udistrital.acciones.m10suscripcionespagos.application.service;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.EvaluacionPremium;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.SuscripcionRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class EvaluadorPremium {

    private final SuscripcionRepositoryPort suscripciones;

    public EvaluadorPremium(SuscripcionRepositoryPort suscripciones) {
        this.suscripciones = suscripciones;
    }

    @Transactional(readOnly = true)
    public EvaluacionPremium evaluar(Long userId, Instant ahora) {
        if (userId == null || userId <= 0) {
            return new EvaluacionPremium(false, null);
        }
        return suscripciones.findVigenteByUserId(userId)
                .filter(suscripcion -> suscripcion.otorgaPremium(ahora))
                .map(suscripcion -> new EvaluacionPremium(true, suscripcion.getPeriodoFin()))
                .orElseGet(() -> new EvaluacionPremium(false, null));
    }
}
