package com.gfc.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class DetallePedidoTest {

    private Ingrediente crearIngrediente(String nombre) {
        return new Ingrediente(null, nombre, 100, UnidadMedida.UNIDAD);
    }

    private Comida crearComidaConIngredientes() {
        Comida comida = new Comida(null, "Hamburguesa", new BigDecimal("18000"), null);
        Ingrediente pan = crearIngrediente("Pan");
        Ingrediente carne = crearIngrediente("Carne");
        comida.configurarIngrediente(null, pan, 1, true, true, true, new BigDecimal("2000"));
        comida.configurarIngrediente(null, carne, 200, true, true, false, new BigDecimal("4000"));
        return comida;
    }

    private void assertSubtotalEquals(BigDecimal esperado, DetallePedido detalle) {
        assertEquals(0, esperado.compareTo(detalle.calcularSubtotal()),
                "Esperado " + esperado + " pero fue " + detalle.calcularSubtotal());
    }

    @Test
    void calcularSubtotalSinPersonalizaciones() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 2);
        assertSubtotalEquals(new BigDecimal("36000.00"), detalle);
    }

    @Test
    void calcularSubtotalConUnAdicional() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 1);

        ConfiguracionIngrediente configPan = comida.getIngredientes().get(0);
        PersonalizacionIngrediente adicional = new PersonalizacionIngrediente(
                null, configPan, 1, TipoPersonalizacion.ADICIONAL);
        detalle.agregarPersonalizacion(adicional);

        assertSubtotalEquals(new BigDecimal("20000.00"), detalle);
    }

    @Test
    void calcularSubtotalConUnRetiradoNoDescuenta() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 1);

        ConfiguracionIngrediente configPan = comida.getIngredientes().get(0);
        PersonalizacionIngrediente retirado = new PersonalizacionIngrediente(
                null, configPan, 1, TipoPersonalizacion.RETIRADO);
        detalle.agregarPersonalizacion(retirado);

        assertSubtotalEquals(new BigDecimal("18000.00"), detalle);
    }

    @Test
    void calcularSubtotalConRetiradoNoDescuentaDelTotal() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 1);

        ConfiguracionIngrediente configPan = comida.getIngredientes().get(0);
        PersonalizacionIngrediente retirado = new PersonalizacionIngrediente(
                null, configPan, 1, TipoPersonalizacion.RETIRADO);
        detalle.agregarPersonalizacion(retirado);

        assertSubtotalEquals(new BigDecimal("18000.00"), detalle);
    }

    @Test
    void calcularSubtotalMultiplicaPorCantidad() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 3);
        assertSubtotalEquals(new BigDecimal("54000.00"), detalle);
    }

    @Test
    void agregarPersonalizacionDeIngredienteExternoLanzaExcepcion() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 1);

        Comida otraComida = new Comida(null, "Pizza", new BigDecimal("22000"), null);
        Ingrediente masa = crearIngrediente("Masa");
        otraComida.configurarIngrediente(null, masa, 1, true, true, true, new BigDecimal("3000"));
        ConfiguracionIngrediente configMasa = otraComida.getIngredientes().get(0);

        assertThrows(IllegalArgumentException.class, () -> {
            PersonalizacionIngrediente p = new PersonalizacionIngrediente(
                    null, configMasa, 1, TipoPersonalizacion.ADICIONAL);
            detalle.agregarPersonalizacion(p);
        });
    }

    @Test
    void duplicatePersonalizacionLanzaExcepcion() {
        Comida comida = crearComidaConIngredientes();
        DetallePedido detalle = new DetallePedido(null, comida, 1);

        ConfiguracionIngrediente configPan = comida.getIngredientes().get(0);
        PersonalizacionIngrediente p1 = new PersonalizacionIngrediente(
                null, configPan, 1, TipoPersonalizacion.ADICIONAL);
        detalle.agregarPersonalizacion(p1);

        PersonalizacionIngrediente p2 = new PersonalizacionIngrediente(
                null, configPan, 1, TipoPersonalizacion.ADICIONAL);
        assertThrows(IllegalArgumentException.class, () -> detalle.agregarPersonalizacion(p2));
    }
}
