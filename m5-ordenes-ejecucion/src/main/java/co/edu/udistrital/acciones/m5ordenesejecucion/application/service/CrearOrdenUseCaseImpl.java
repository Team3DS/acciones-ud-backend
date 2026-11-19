package co.edu.udistrital.acciones.m5ordenesejecucion.application.service;

import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.CrearOrdenCommand;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.in.CrearOrdenUseCase.OrdenResultado;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.ComisionRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.OrdenRepositoryPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out.PublicadorOrdenesPort;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.EstadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.LadoOrden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.Orden;
import co.edu.udistrital.acciones.m5ordenesejecucion.domain.TipoOrden;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class CrearOrdenUseCaseImpl implements CrearOrdenUseCase {

    private final OrdenRepositoryPort ordenRepository;
    private final ComisionRepositoryPort comisionRepository;
    private final PublicadorOrdenesPort publicador;

    public CrearOrdenUseCaseImpl(OrdenRepositoryPort ordenRepository,
                                 ComisionRepositoryPort comisionRepository,
                                 PublicadorOrdenesPort publicador) {
        this.ordenRepository = ordenRepository;
        this.comisionRepository = comisionRepository;
        this.publicador = publicador;
    }

    @Override
    public Orden ejecutar(CrearOrdenCommand comando) {
        if (ordenRepository.findByClientOrderId(comando.idempotencyKey()).isPresent()) {
            throw new IllegalArgumentException("Orden duplicada: idempotencyKey ya existe");
        }

        Orden orden = new Orden();
        orden.setUsuarioId(comando.usuarioId());
        orden.setSimbolo(comando.simbolo());
        orden.setMercado("NYSE");
        orden.setLado(LadoOrden.valueOf(comando.lado()));
        orden.setTipo(TipoOrden.valueOf(comando.tipo()));
        orden.setCantidad(BigDecimal.valueOf(comando.cantidad()));
        orden.setPrecioLimite(comando.precio());
        orden.setEstado(EstadoOrden.PENDIENTE);
        orden.setClientOrderId(comando.idempotencyKey());
        orden.setCreadoEn(Instant.now());
        orden.setActualizadoEn(Instant.now());
        orden.setCantidadEjecutada(0);
        orden.setCantidadRemanente(comando.cantidad());

        BigDecimal comision = calcularComision(comando.precio(), comando.cantidad());
        orden.setComision(comision);

        BigDecimal valorTotal = comando.precio().multiply(BigDecimal.valueOf(comando.cantidad()))
                .add(comision);
        orden.setValorTotal(valorTotal);

        orden = ordenRepository.save(orden);

        publicador.publicarOrdenCreada(
                orden.getId(), orden.getUsuarioId(), orden.getSimbolo(),
                orden.getLado().name(), orden.getCantidad().intValue(), orden.getTipo().name());

        return orden;
    }

    private BigDecimal calcularComision(BigDecimal precio, Integer cantidad) {
        BigDecimal monto = precio.multiply(BigDecimal.valueOf(cantidad));
        return monto.multiply(new BigDecimal("0.02")).setScale(2, java.math.RoundingMode.HALF_UP);
    }

    @Override
    public OrdenResultado convertir(Orden orden) {
        return new OrdenResultado(
                orden.getId(),
                orden.getSimbolo(),
                orden.getLado().name(),
                orden.getTipo().name(),
                orden.getCantidad().intValue(),
                orden.getPrecioLimite(),
                orden.getComision(),
                orden.getValorTotal(),
                orden.getEstado().name(),
                orden.getClientOrderId()
        );
    }
}
