package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PortafolioResponse;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.domain.Posicion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConsultarPortafolioUseCase {

    private final PosicionRepositoryPort posiciones;
    private final ValoracionService valoracion;

    public ConsultarPortafolioUseCase(PosicionRepositoryPort posiciones, ValoracionService valoracion) {
        this.posiciones = posiciones;
        this.valoracion = valoracion;
    }

    @Transactional(readOnly = true)
    public PortafolioResponse ejecutar(Long userId, Actor actor) {
        actor.verificarLectura(userId);
        List<Posicion> todas = posiciones.findByUserId(userId);
        List<Posicion> abiertas = todas.stream()
                .filter(posicion -> posicion.getCantidad().signum() > 0)
                .toList();
        return valoracion.valorarPortafolio(userId, abiertas, todas);
    }
}
