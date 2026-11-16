package co.edu.udistrital.acciones.m3catalogomercados.application.port.out;

import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.ActualizarMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearInstrumentoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.application.command.CrearMercadoCommand;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Instrumento;
import co.edu.udistrital.acciones.m3catalogomercados.domain.Mercado;
import co.edu.udistrital.acciones.m3catalogomercados.domain.ResultadoComando;

import java.util.function.Supplier;

public interface CatalogoAdminPort {
    <T> ResultadoComando<T> ejecutar(
            String operation,
            String key,
            long actor,
            Object request,
            Class<T> type,
            Supplier<ResultadoComando<T>> action);

    Mercado crearMercado(CrearMercadoCommand command);

    Mercado actualizarMercado(String code, ActualizarMercadoCommand command);

    Instrumento crearInstrumento(CrearInstrumentoCommand command);

    Instrumento actualizarInstrumento(
            String symbol, String market, ActualizarInstrumentoCommand command);

    void registrarEvento(String routingKey, String aggregate, Object payload);
}
