package co.edu.udistrital.acciones.m2cuentasfondos.application.usecase;

import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.Actor;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.FiltroMovimientos;
import co.edu.udistrital.acciones.m2cuentasfondos.application.dto.MovimientoResponse;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.AccesoDenegadoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.exception.OrdenInvalidoException;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.CuentaRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.application.port.out.MovimientoRepositoryPort;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Cuenta;
import co.edu.udistrital.acciones.m2cuentasfondos.domain.Movimiento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ListarMovimientosUseCase {

    private static final Map<String, String> CAMPOS_ORDEN = Map.of(
            "createdAt", "creadoEn",
            "amount", "monto");
    private static final String ORDEN_POR_DEFECTO = "creadoEn";

    private final CuentaRepositoryPort cuentas;
    private final MovimientoRepositoryPort movimientos;

    public ListarMovimientosUseCase(CuentaRepositoryPort cuentas, MovimientoRepositoryPort movimientos) {
        this.cuentas = cuentas;
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public Page<MovimientoResponse> ejecutar(Long userId, FiltroMovimientos filtro, Pageable pageable, Actor actor) {
        if (!actor.puedeVer(userId)) {
            throw new AccesoDenegadoException("No tiene permiso para consultar los movimientos de esta cuenta");
        }
        Pageable ordenado = ordenar(pageable);
        Optional<Cuenta> cuenta = cuentas.findByUserId(userId);
        if (cuenta.isEmpty()) {
            return Page.empty(ordenado);
        }
        return movimientos.buscar(cuenta.get().getId(), filtro, ordenado).map(this::respuesta);
    }

    private Pageable ordenar(Pageable pageable) {
        Sort sort = pageable.getSort();
        if (sort.isUnsorted()) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, ORDEN_POR_DEFECTO));
        }
        List<Sort.Order> ordenes = sort.stream().map(this::mapear).toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(ordenes));
    }

    private Sort.Order mapear(Sort.Order orden) {
        String campo = CAMPOS_ORDEN.get(orden.getProperty());
        if (campo == null) {
            throw new OrdenInvalidoException("Orden no permitido: " + orden.getProperty());
        }
        return new Sort.Order(orden.getDirection(), campo);
    }

    private MovimientoResponse respuesta(Movimiento movimiento) {
        return new MovimientoResponse(movimiento.getId(), movimiento.getTipo().name(), movimiento.getMonto(),
                movimiento.getSaldoPosterior(), movimiento.getReferencia(), movimiento.getDescripcion(),
                movimiento.getCreadoEn());
    }
}
