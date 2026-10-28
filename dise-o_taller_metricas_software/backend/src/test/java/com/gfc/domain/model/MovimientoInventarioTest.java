package com.gfc.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class MovimientoInventarioTest {

    private Ingrediente crearIngredienteConStock(int stock) {
        return new Ingrediente(null, "Queso", stock, UnidadMedida.UNIDAD);
    }

    private Comida crearComidaBasica() {
        return new Comida(null, "Hamburguesa", new BigDecimal("18000"), null);
    }

    @Test
    void entradaAumentaStock() {
        Ingrediente ingrediente = crearIngredienteConStock(10);
        MovimientoInventario movimiento = new MovimientoInventario(
                null, ingrediente, 5, LocalDateTime.now(), "Compra", TipoMovimiento.ENTRADA);
        movimiento.aplicar();
        assertEquals(15, ingrediente.getStockActual());
        assertTrue(movimiento.isAplicado());
    }

    @Test
    void salidaDisminuyeStock() {
        Ingrediente ingrediente = crearIngredienteConStock(10);
        MovimientoInventario movimiento = new MovimientoInventario(
                null, ingrediente, 3, LocalDateTime.now(), "Uso", TipoMovimiento.SALIDA);
        movimiento.aplicar();
        assertEquals(7, ingrediente.getStockActual());
        assertTrue(movimiento.isAplicado());
    }

    @Test
    void aplicarDosVecesLanzaExcepcion() {
        Ingrediente ingrediente = crearIngredienteConStock(10);
        MovimientoInventario movimiento = new MovimientoInventario(
                null, ingrediente, 3, LocalDateTime.now(), "Uso", TipoMovimiento.SALIDA);
        movimiento.aplicar();
        assertThrows(IllegalStateException.class, movimiento::aplicar);
    }

    @Test
    void ajustePositivoSumaStock() {
        Ingrediente ingrediente = crearIngredienteConStock(10);
        MovimientoInventario movimiento = new MovimientoInventario(
                null, ingrediente, 5, LocalDateTime.now(), "Correccion", TipoMovimiento.AJUSTE);
        movimiento.aplicar();
        assertEquals(15, ingrediente.getStockActual());
    }

    @Test
    void ajusteNegativoRestaStock() {
        Ingrediente ingrediente = crearIngredienteConStock(10);
        MovimientoInventario movimiento = new MovimientoInventario(
                null, ingrediente, -4, LocalDateTime.now(), "Correccion", TipoMovimiento.AJUSTE);
        movimiento.aplicar();
        assertEquals(6, ingrediente.getStockActual());
    }

    @Test
    void ajusteEnCeroLanzaExcepcion() {
        Ingrediente ingrediente = crearIngredienteConStock(10);
        assertThrows(IllegalArgumentException.class, () ->
                new MovimientoInventario(null, ingrediente, 0, LocalDateTime.now(),
                        "Correccion", TipoMovimiento.AJUSTE));
    }

    @Test
    void salidaConStockInsuficienteLanzaExcepcion() {
        Ingrediente ingrediente = crearIngredienteConStock(2);
        MovimientoInventario movimiento = new MovimientoInventario(
                null, ingrediente, 5, LocalDateTime.now(), "Uso", TipoMovimiento.SALIDA);
        assertThrows(IllegalStateException.class, movimiento::aplicar);
        assertEquals(2, ingrediente.getStockActual());
        assertFalse(movimiento.isAplicado());
    }
}
