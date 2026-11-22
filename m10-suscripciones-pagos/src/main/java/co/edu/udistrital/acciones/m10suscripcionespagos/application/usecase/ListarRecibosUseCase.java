package co.edu.udistrital.acciones.m10suscripcionespagos.application.usecase;

import co.edu.udistrital.acciones.m10suscripcionespagos.application.dto.ReciboResponse;
import co.edu.udistrital.acciones.m10suscripcionespagos.application.port.out.ReciboRepositoryPort;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ListarRecibosUseCase {

    private static final int TAMANO_MAXIMO = 100;
    private static final int TAMANO_POR_DEFECTO = 20;

    private final ReciboRepositoryPort recibos;

    public ListarRecibosUseCase(ReciboRepositoryPort recibos) {
        this.recibos = recibos;
    }

    @Transactional(readOnly = true)
    public Page<ReciboResponse> ejecutar(Long userId, int page, int size) {
        int pagina = Math.max(page, 0);
        int tamano = size <= 0 ? TAMANO_POR_DEFECTO : Math.min(size, TAMANO_MAXIMO);
        Pageable pageable = PageRequest.of(pagina, tamano,
                Sort.by(Sort.Direction.DESC, "emitidoEn").and(Sort.by(Sort.Direction.DESC, "id")));
        return recibos.findByUserId(userId, pageable).map(ReciboResponse::de);
    }
}
