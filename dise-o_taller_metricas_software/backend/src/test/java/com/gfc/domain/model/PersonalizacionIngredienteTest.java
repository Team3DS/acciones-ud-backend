package com.gfc.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PersonalizacionIngredienteTest {

    private Comida crearComidaBasica() {
        return new Comida(null, "Hamburguesa", new BigDecimal("18000"), null);
    }

    private Ingrediente crearIngrediente(String nombre) {
        return new Ingrediente(null, nombre, 100, UnidadMedida.UNIDAD);
    }

    private ConfiguracionIngrediente crearConfig(Comida comida, Ingrediente ingrediente,
                                                  int cantidadBase, boolean incluido,
                                                  boolean permiteAdicional, boolean permiteRetirar,
                                                  BigDecimal precioAdicional) {
        return new ConfiguracionIngrediente(null, comida, ingrediente, cantidadBase,
                incluido, permiteAdicional, permiteRetirar, precioAdicional);
    }

    @Test
    void adicionalConPermiteAdicionalTrueFunciona() {
        Comida comida = crearComidaBasica();
        Ingrediente queso = crearIngrediente("Queso");
        ConfiguracionIngrediente config = crearConfig(comida, queso, 2, true, true, true,
                new BigDecimal("1500"));

        PersonalizacionIngrediente personalizacion = new PersonalizacionIngrediente(
                null, config, 1, TipoPersonalizacion.ADICIONAL);

        assertEquals(1, personalizacion.getCantidad());
        assertEquals(0, new BigDecimal("1500").compareTo(personalizacion.getPrecioAdicionalUnitario()));
        assertEquals(0, new BigDecimal("1500").compareTo(personalizacion.calcularCostoAdicional()));
        assertTrue(personalizacion.getTipoAdicion() == TipoPersonalizacion.ADICIONAL);
    }

    @Test
    void adicionalConPermiteAdicionalFalseLanzaExcepcion() {
        Comida comida = crearComidaBasica();
        Ingrediente carne = crearIngrediente("Carne");
        ConfiguracionIngrediente config = crearConfig(comida, carne, 200, true, false, false,
                new BigDecimal("4000"));

        assertThrows(IllegalArgumentException.class, () ->
                new PersonalizacionIngrediente(null, config, 1, TipoPersonalizacion.ADICIONAL));
    }

    @Test
    void retiradoConPermiteRetirarTrueEIncluidoFunciona() {
        Comida comida = crearComidaBasica();
        Ingrediente lechuga = crearIngrediente("Lechuga");
        ConfiguracionIngrediente config = crearConfig(comida, lechuga, 1, true, true, true,
                new BigDecimal("500"));

        PersonalizacionIngrediente personalizacion = new PersonalizacionIngrediente(
                null, config, 1, TipoPersonalizacion.RETIRADO);

        assertEquals(1, personalizacion.getCantidad());
        assertEquals(BigDecimal.ZERO, personalizacion.getPrecioAdicionalUnitario());
        assertEquals(BigDecimal.ZERO, personalizacion.calcularCostoAdicional());
    }

    @Test
    void retiradoConPermiteRetirarFalseLanzaExcepcion() {
        Comida comida = crearComidaBasica();
        Ingrediente carne = crearIngrediente("Carne");
        ConfiguracionIngrediente config = crearConfig(comida, carne, 200, true, true, false,
                new BigDecimal("4000"));

        assertThrows(IllegalArgumentException.class, () ->
                new PersonalizacionIngrediente(null, config, 1, TipoPersonalizacion.RETIRADO));
    }

    @Test
    void retiradoConCantidadMayorQueCantidadBaseLanzaExcepcion() {
        Comida comida = crearComidaBasica();
        Ingrediente lechuga = crearIngrediente("Lechuga");
        ConfiguracionIngrediente config = crearConfig(comida, lechuga, 1, true, true, true,
                new BigDecimal("500"));

        assertThrows(IllegalArgumentException.class, () ->
                new PersonalizacionIngrediente(null, config, 2, TipoPersonalizacion.RETIRADO));
    }

    @Test
    void retiradoConIncluidoPorDefectoFalseLanzaExcepcion() {
        Comida comida = crearComidaBasica();
        Ingrediente extra = crearIngrediente("Jalapenos");
        ConfiguracionIngrediente config = crearConfig(comida, extra, 0, false, true, false,
                new BigDecimal("500"));

        assertThrows(IllegalArgumentException.class, () ->
                new PersonalizacionIngrediente(null, config, 1, TipoPersonalizacion.RETIRADO));
    }

    @Test
    void adicionalMultiplicaCostoPorCantidad() {
        Comida comida = crearComidaBasica();
        Ingrediente queso = crearIngrediente("Queso");
        ConfiguracionIngrediente config = crearConfig(comida, queso, 2, true, true, true,
                new BigDecimal("1500"));

        PersonalizacionIngrediente personalizacion = new PersonalizacionIngrediente(
                null, config, 3, TipoPersonalizacion.ADICIONAL);

        assertEquals(0, new BigDecimal("4500.00").compareTo(personalizacion.calcularCostoAdicional()));
    }
}
