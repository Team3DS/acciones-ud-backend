package com.gfc.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class IngredienteTest {

    @Test
    void aumentarStockSumaCorrectamente() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 10, UnidadMedida.UNIDAD);
        ingrediente.aumentarStock(5);
        assertEquals(15, ingrediente.getStockActual());
    }

    @Test
    void disminuirStockRestaCorrectamente() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 10, UnidadMedida.UNIDAD);
        ingrediente.disminuirStock(3);
        assertEquals(7, ingrediente.getStockActual());
    }

    @Test
    void hayStockTrueCuandoAlcanza() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 10, UnidadMedida.UNIDAD);
        assertTrue(ingrediente.hayStock(10));
        assertTrue(ingrediente.hayStock(0));
    }

    @Test
    void hayStockFalseCuandoNoAlcanza() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 10, UnidadMedida.UNIDAD);
        assertFalse(ingrediente.hayStock(11));
    }

    @Test
    void disminuirStockConStockInsuficienteLanzaExcepcion() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 5, UnidadMedida.UNIDAD);
        assertThrows(IllegalStateException.class, () -> ingrediente.disminuirStock(6));
        assertEquals(5, ingrediente.getStockActual());
    }

    @Test
    void disminuirStockConStockInsuficienteNoDescuentaNada() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 5, UnidadMedida.UNIDAD);
        try {
            ingrediente.disminuirStock(10);
        } catch (IllegalStateException ignored) {
        }
        assertEquals(5, ingrediente.getStockActual());
    }

    @Test
    void disminuirStockEnCeroLanzaExcepcion() {
        Ingrediente ingrediente = new Ingrediente(null, "Queso", 0, UnidadMedida.UNIDAD);
        assertThrows(IllegalStateException.class, () -> ingrediente.disminuirStock(1));
    }

    @Test
    void stockInicialNoNegativo() {
        assertThrows(IllegalArgumentException.class,
                () -> new Ingrediente(null, "Queso", -1, UnidadMedida.UNIDAD));
    }
}
