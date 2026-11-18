package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ListarPosicionesUseCase {

    private final PosicionRepositoryPort posiciones;
    private final ValoracionService valoracion;

    public ListarPosicionesUseCase(PosicionRepositoryPort posiciones, ValoracionService valoracion) {
        this.posiciones = posiciones;
        this.valoracion = valoracion;
    }

    @Transactional(readOnly = true)
    public List<PosicionResponse> ejecutar(Long userId, Actor actor) {
        actor.verificarLectura(userId);
        return valoracion.valorarTodas(posiciones.findAbiertasByUserId(userId));
    }
}
