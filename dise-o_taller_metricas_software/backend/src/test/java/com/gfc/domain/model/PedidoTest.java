package com.gfc.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PedidoTest {

    private Ingrediente crearIngrediente(String nombre) {
        return new Ingrediente(null, nombre, 100, UnidadMedida.UNIDAD);
    }

    private Comida crearComida(String nombre, BigDecimal precio) {
        Comida comida = new Comida(null, nombre, precio, null);
        Ingrediente pan = crearIngrediente("Pan");
        comida.configurarIngrediente(null, pan, 1, true, true, true, new BigDecimal("2000"));
        return comida;
    }

    private Cliente crearCliente() {
        return new Cliente(null, "cliente1", "pass123", TipoDocumento.CC,
                "1234567890", "test@test.com", "Cliente Test", new TestCodificador());
    }

    private void assertTotalEquals(BigDecimal esperado, Pedido pedido) {
        assertEquals(0, esperado.compareTo(pedido.calcularTotal()),
                "Esperado " + esperado + " pero fue " + pedido.calcularTotal());
    }

    @Test
    void calcularTotalConUnDetalle() {
        Cliente cliente = crearCliente();
        Comida hamburguesa = crearComida("Hamburguesa", new BigDecimal("18000"));
        DetallePedido detalle = new DetallePedido(null, hamburguesa, 2);
        Pedido pedido = new Pedido(null, cliente, LocalDateTime.now(), detalle);

        assertTotalEquals(new BigDecimal("36000.00"), pedido);
    }

    @Test
    void calcularTotalConVariosDetalles() {
        Cliente cliente = crearCliente();
        Comida hamburguesa = crearComida("Hamburguesa", new BigDecimal("18000"));
        Comida perro = crearComida("Perro", new BigDecimal("12000"));

        DetallePedido d1 = new DetallePedido(null, hamburguesa, 1);
        DetallePedido d2 = new DetallePedido(null, perro, 2);

        Pedido pedido = new Pedido(null, cliente, LocalDateTime.now(), d1);
        pedido.agregarDetalle(d2);

        assertTotalEquals(new BigDecimal("42000.00"), pedido);
    }

    @Test
    void calcularTotalConPersonalizaciones() {
        Cliente cliente = crearCliente();
        Comida hamburguesa = crearComida("Hamburguesa", new BigDecimal("18000"));

        DetallePedido d1 = new DetallePedido(null, hamburguesa, 1);
        ConfiguracionIngrediente configPan = hamburguesa.getIngredientes().get(0);
        PersonalizacionIngrediente adicional = new PersonalizacionIngrediente(
                null, configPan, 1, TipoPersonalizacion.ADICIONAL);
        d1.agregarPersonalizacion(adicional);

        Pedido pedido = new Pedido(null, cliente, LocalDateTime.now(), d1);
        assertTotalEquals(new BigDecimal("20000.00"), pedido);
    }

    @Test
    void eliminarUltimoDetalleLanzaExcepcion() {
        Cliente cliente = crearCliente();
        Comida hamburguesa = crearComida("Hamburguesa", new BigDecimal("18000"));
        DetallePedido d1 = new DetallePedido(null, hamburguesa, 1);
        Pedido pedido = new Pedido(null, cliente, LocalDateTime.now(), d1);

        assertThrows(IllegalStateException.class, () -> pedido.eliminarDetalle(d1));
    }
}
