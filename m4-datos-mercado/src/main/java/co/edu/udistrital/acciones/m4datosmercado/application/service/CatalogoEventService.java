package co.edu.udistrital.acciones.m4datosmercado.application.service;

import co.edu.udistrital.acciones.m4datosmercado.application.dto.InstrumentoCreadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.InstrumentoActualizadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.MercadoCreadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.dto.MercadoActualizadoPayload;
import co.edu.udistrital.acciones.m4datosmercado.application.port.out.CacheCotizacionesPort;
import co.edu.udistrital.acciones.m4datosmercado.domain.HorarioMercado;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;

@Service
@Transactional
public class CatalogoEventService {

    private static final Logger log = LoggerFactory.getLogger(CatalogoEventService.class);

    private final CacheCotizacionesPort cachePort;

    public CatalogoEventService(CacheCotizacionesPort cachePort) {
        this.cachePort = cachePort;
    }

    public void procesarInstrumentoCreado(InstrumentoCreadoPayload payload) {
        log.info("Instrumento creado recibido: {}", payload.symbol());
    }

    public void procesarInstrumentoActualizado(InstrumentoActualizadoPayload payload) {
        log.info("Instrumento actualizado recibido: {}", payload.symbol());
        cachePort.evict(payload.symbol());
    }

    public void procesarMercadoCreado(MercadoCreadoPayload payload) {
        log.info("Mercado creado recibido: {}", payload.mic());
    }

    public void procesarMercadoActualizado(MercadoActualizadoPayload payload) {
        log.info("Mercado actualizado recibido: {}", payload.mic());
    }
}