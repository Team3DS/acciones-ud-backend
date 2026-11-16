package co.edu.udistrital.acciones.m3catalogomercados.application.service;

import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.port.out.CatalogoAdminPort;
import co.edu.udistrital.acciones.m3catalogomercados.domain.HorarioReferencia;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;
import co.edu.udistrital.acciones.m3catalogomercados.domain.ResultadoComando;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@Transactional
public class CatalogoAdminService {
    private final CatalogoAdminPort admin;

    public CatalogoAdminService(CatalogoAdminPort admin) {
        this.admin = admin;
    }

    public ResultadoComando<Mercado> crearMercado(
            CrearMercadoCommand command, String key, long actor) {
        try {
            ZoneId.of(command.timeZone());
        } catch (java.time.DateTimeException exception) {
            throw new IllegalArgumentException(
                    "La zona horaria debe ser un identificador IANA válido.");
        }
        validarHorario(command.tradingHours());
        return admin.ejecutar(
                "CREAR_MERCADO",
                key,
                actor,
                command,
                Mercado.class,
                () -> {
                    Mercado market = admin.crearMercado(command);
                    admin.registrarEvento(
                            "m3.mercado.creado", market.code(), mercadoPayload(market));
                    return new ResultadoComando<>(
                            201, "/api/v1/catalog/markets/" + market.code(), market, false);
                });
    }

    public ResultadoComando<Mercado> actualizarMercado(
            String code, ActualizarMercadoCommand command, String key, long actor) {
        if (command.name() == null
                && command.enabled() == null
                && command.country() == null
                && command.tradingHours() == null)
            throw new IllegalArgumentException("El cambio no puede estar vacío.");
        if (command.name() != null && command.name().isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        validarHorario(command.tradingHours());
        return admin.ejecutar(
                "ACTUALIZAR_MERCADO:" + code,
                key,
                actor,
                command,
                Mercado.class,
                () -> {
                    Mercado market = admin.actualizarMercado(code, command);
                    admin.registrarEvento(
                            "m3.mercado.actualizado", market.code(), mercadoPayload(market));
                    return new ResultadoComando<>(
                            200, "/api/v1/catalog/markets/" + market.code(), market, false);
                });
    }

    public ResultadoComando<Instrumento> crearInstrumento(
            CrearInstrumentoCommand command, String key, long actor) {
        return admin.ejecutar(
                "CREAR_INSTRUMENTO",
                key,
                actor,
                command,
                Instrumento.class,
                () -> {
                    Instrumento instrument = admin.crearInstrumento(command);
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("symbol", instrument.symbol());
                    payload.put("name", instrument.companyName());
                    payload.put("market", instrument.market().code());
                    payload.put("currency", instrument.market().currency());
                    payload.put("lotSize", instrument.lotSize());
                    payload.put("tickSize", instrument.tickSize());
                    payload.put("type", instrument.type());
                    admin.registrarEvento(
                            "m3.instrumento.creado", Long.toString(instrument.id()), payload);
                    return new ResultadoComando<>(
                            201,
                            "/api/v1/instruments/"
                                    + instrument.symbol()
                                    + "?market="
                                    + instrument.market().code(),
                            instrument,
                            false);
                });
    }

    public ResultadoComando<Instrumento> actualizarInstrumento(
            String symbol,
            String market,
            ActualizarInstrumentoCommand command,
            String key,
            long actor) {
        if (command.name() == null
                && command.lotSize() == null
                && command.tickSize() == null
                && command.state() == null)
            throw new IllegalArgumentException("El cambio no puede estar vacío.");
        if (command.name() != null && command.name().isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío.");
        String operation =
                "ACTUALIZAR_INSTRUMENTO:" + symbol + ":" + (market == null ? "" : market);
        return admin.ejecutar(
                operation,
                key,
                actor,
                command,
                Instrumento.class,
                () -> {
                    Instrumento instrument = admin.actualizarInstrumento(symbol, market, command);
                    admin.registrarEvento(
                            "m3.instrumento.actualizado",
                            Long.toString(instrument.id()),
                            Map.of("symbol", instrument.symbol(), "changes", command));
                    return new ResultadoComando<>(
                            200,
                            "/api/v1/instruments/"
                                    + instrument.symbol()
                                    + "?market="
                                    + instrument.market().code(),
                            instrument,
                            false);
                });
    }

    private static Map<String, Object> mercadoPayload(Mercado market) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("code", market.code());
        payload.put("mic", market.mic());
        payload.put("name", market.name());
        payload.put("timezone", market.timeZone());
        payload.put("currency", market.currency());
        payload.put("country", market.country());
        payload.put("tradingHours", market.tradingHours());
        payload.put("enabled", market.enabled());
        return payload;
    }

    private static void validarHorario(HorarioReferencia horario) {
        if (horario != null && (horario.open() == null || horario.close() == null))
            throw new IllegalArgumentException(
                    "El horario de referencia requiere apertura y cierre.");
    }
}
