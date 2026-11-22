package co.edu.udistrital.acciones.m5ordenesejecucion.application.port.out;

import java.math.BigDecimal;

public interface DatosMercadoClientPort {
    boolean esMercadoAbierto(String market);
    BigDecimal obtenerPrecioEstimado(String symbol);
}
