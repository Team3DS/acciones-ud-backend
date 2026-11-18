package co.edu.udistrital.acciones.m6portafolio.application.usecase;

import co.edu.udistrital.acciones.m6portafolio.application.dto.Actor;
import co.edu.udistrital.acciones.m6portafolio.application.dto.PosicionResponse;
import co.edu.udistrital.acciones.m6portafolio.application.exception.RecursoNoEncontradoException;
import co.edu.udistrital.acciones.m6portafolio.application.port.out.PosicionRepositoryPort;
import co.edu.udistrital.acciones.m6portafolio.application.service.ValoracionService;
import co.edu.udistrital.acciones.m6portafolio.domain.Simbolos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConsultarPosicionUseCase {

    private final PosicionRepositoryPort posiciones;
    private final ValoracionService valoracion;

    public ConsultarPosicionUseCase(PosicionRepositoryPort posiciones, ValoracionService valoracion) {
        this.posiciones = posiciones;
        this.valoracion = valoracion;
    }

    @Transactional(readOnly = true)
    public PosicionResponse ejecutar(Long userId, String symbol, Actor actor) {
        actor.verificarLectura(userId);
        String normalizado = Simbolos.normalizar(symbol);
        return posiciones.findByUserIdAndSymbol(userId, normalizado)
                .map(valoracion::valorar)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "No existe la posicion " + normalizado + " del usuario " + userId));
    }
}
