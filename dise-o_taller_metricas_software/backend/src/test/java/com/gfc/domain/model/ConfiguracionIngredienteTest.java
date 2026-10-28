package com.gfc.domain.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ConfiguracionIngredienteTest {

    private Comida crearComidaBasica() {
        return new Comida(null, "Hamburguesa", new BigDecimal("18000"), null);
    }

    private Ingrediente crearIngrediente(String nombre) {
        return new Ingrediente(null, nombre, 100, UnidadMedida.UNIDAD);
    }

    @Test
    void configuracionValidaSeCreaSinProblemas() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Queso");
        assertDoesNotThrow(() -> new ConfiguracionIngrediente(
                null, comida, ingrediente, 2, true, true, true, new BigDecimal("1500")));
    }

    @Test
    void incluidoPorDefectoConCantidadBaseCeroLanzaExcepcion() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Queso");
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionIngrediente(
                null, comida, ingrediente, 0, true, true, false, new BigDecimal("1500")));
    }

    @Test
    void permiteRetirarSinIncluidoPorDefectoLanzaExcepcion() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Jalapenos");
        assertThrows(IllegalArgumentException.class, () -> new ConfiguracionIngrediente(
                null, comida, ingrediente, 1, false, true, true, new BigDecimal("500")));
    }

    @Test
    void noIncluidoConCantidadBaseCeroEsValido() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Jalapenos");
        assertDoesNotThrow(() -> new ConfiguracionIngrediente(
                null, comida, ingrediente, 0, false, true, false, new BigDecimal("500")));
    }

    @Test
    void permitePersonalizacionAdicionalCuandoEstaHabilitado() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Queso");
        ConfiguracionIngrediente config = new ConfiguracionIngrediente(
                null, comida, ingrediente, 2, true, true, true, new BigDecimal("1500"));
        assertTrue(config.permitePersonalizacion(TipoPersonalizacion.ADICIONAL));
    }

    @Test
    void noPermitePersonalizacionAdicionalCuandoEstaDeshabilitado() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Carne");
        ConfiguracionIngrediente config = new ConfiguracionIngrediente(
                null, comida, ingrediente, 200, true, false, false, new BigDecimal("4000"));
        assertFalse(config.permitePersonalizacion(TipoPersonalizacion.ADICIONAL));
    }

    @Test
    void permitePersonalizacionRetiradoCuandoIncluidoYHabilitado() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Lechuga");
        ConfiguracionIngrediente config = new ConfiguracionIngrediente(
                null, comida, ingrediente, 1, true, true, true, new BigDecimal("500"));
        assertTrue(config.permitePersonalizacion(TipoPersonalizacion.RETIRADO));
    }

    @Test
    void noPermiteRetirarSiNoEstaIncluidoPorDefecto() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Extra");
        ConfiguracionIngrediente config = new ConfiguracionIngrediente(
                null, comida, ingrediente, 0, false, true, false, new BigDecimal("500"));
        assertFalse(config.permitePersonalizacion(TipoPersonalizacion.RETIRADO));
    }

    @Test
    void noPermiteRetirarSiPermiteRetirarEsFalse() {
        Comida comida = crearComidaBasica();
        Ingrediente ingrediente = crearIngrediente("Carne");
        ConfiguracionIngrediente config = new ConfiguracionIngrediente(
                null, comida, ingrediente, 200, true, true, false, new BigDecimal("4000"));
        assertFalse(config.permitePersonalizacion(TipoPersonalizacion.RETIRADO));
    }
}
