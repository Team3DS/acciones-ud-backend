package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.ConsultarEjecucionesUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ConfirmacionEjecucionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.EjecucionOrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ConfirmacionEjecucion;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EjecucionOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.ServicioParticipante;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class ConsultarEjecucionesUseCaseImpl implements ConsultarEjecucionesUseCase {

    private final EjecucionOrdenRepositoryPort ejecucionRepository;
    private final ConfirmacionEjecucionRepositoryPort confirmacionRepository;
    private final OrdenRepositoryPort ordenRepository;

    public ConsultarEjecucionesUseCaseImpl(EjecucionOrdenRepositoryPort ejecucionRepository,
                                           ConfirmacionEjecucionRepositoryPort confirmacionRepository,
                                           OrdenRepositoryPort ordenRepository) {
        this.ejecucionRepository = ejecucionRepository;
        this.confirmacionRepository = confirmacionRepository;
        this.ordenRepository = ordenRepository;
    }

    @Override
    public EjecucionOrden buscarPorId(Long executionId) {
        return ejecucionRepository.findById(executionId)
                .orElseThrow(() -> new IllegalArgumentException("Ejecución no encontrada: " + executionId));
    }

    @Override
    public ResumenEjecucionesResponse resumen(Long usuarioId, Instant desde, Instant hasta) {
        List<EjecucionOrden> ejecuciones = ejecucionRepository.findByUsuarioIdAndPeriodo(usuarioId, desde, hasta);

        BigDecimal gananciaBrutaRealizada = BigDecimal.ZERO;
        BigDecimal comisionesCompra = BigDecimal.ZERO;
        BigDecimal comisionesVenta = BigDecimal.ZERO;
        int ventasPendientes = 0;

        for (EjecucionOrden ejecucion : ejecuciones) {
            Orden orden = ordenRepository.findById(ejecucion.getOrdenId()).orElse(null);
            if (orden == null) {
                continue;
            }

            if (orden.getLadoOrden() == LadoOrden.COMPRA) {
                if (ejecucion.getComision() != null) {
                    comisionesCompra = comisionesCompra.add(ejecucion.getComision());
                }
            } else if (orden.getLadoOrden() == LadoOrden.VENTA) {
                if (ejecucion.getComision() != null) {
                    comisionesVenta = comisionesVenta.add(ejecucion.getComision());
                }

                // G/P Bruta calculada a partir de confirmación PORTAFOLIO con precioPromedioCosto
                Optional<ConfirmacionEjecucion> confOpt = confirmacionRepository.findByEjecucionIdAndServicio(
                        ejecucion.getId(), ServicioParticipante.PORTAFOLIO);

                if (confOpt.isPresent() && confOpt.get().getPrecioPromedioCosto() != null
                        && confOpt.get().getPrecioPromedioCosto().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal costoPromedio = confOpt.get().getPrecioPromedioCosto();
                    BigDecimal deltaPrecio = ejecucion.getPrecioEjecucion().subtract(costoPromedio);
                    BigDecimal ganancia = deltaPrecio.multiply(BigDecimal.valueOf(ejecucion.getCantidad()))
                            .setScale(2, RoundingMode.HALF_UP);
                    gananciaBrutaRealizada = gananciaBrutaRealizada.add(ganancia);
                } else {
                    ventasPendientes++;
                }
            }
        }

        return new ResumenEjecucionesResponse(
                usuarioId,
                desde,
                hasta,
                gananciaBrutaRealizada.setScale(2, RoundingMode.HALF_UP),
                comisionesCompra.setScale(2, RoundingMode.HALF_UP),
                comisionesVenta.setScale(2, RoundingMode.HALF_UP),
                ventasPendientes
        );
    }
}
