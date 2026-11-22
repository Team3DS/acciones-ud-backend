package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.PlanResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.PlanRepositoryPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarPlanesUseCase {

    private final PlanRepositoryPort planes;

    public ListarPlanesUseCase(PlanRepositoryPort planes) {
        this.planes = planes;
    }

    @Transactional(readOnly = true)
    public List<PlanResponse> ejecutar() {
        return planes.findActivos().stream().map(PlanResponse::de).toList();
    }
}
