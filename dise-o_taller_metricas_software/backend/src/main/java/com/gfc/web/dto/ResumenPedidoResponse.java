package com.gfc.web.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** El "resumen de pago": lo pedido, las adiciones y los retiros ya valorados (retirar no descuenta). */
public record ResumenPedidoResponse(Long id, LocalDateTime fecha, List<DetalleResumen> detalle,
                                     BigDecimal valorTotal) {

    public record DetalleResumen(String comida, int cantidad, BigDecimal precioUnitario,
                                  List<PersonalizacionResumen> personalizaciones, BigDecimal subtotal) {
    }

    public record PersonalizacionResumen(String ingrediente, String tipoAdicion, int cantidad,
                                          BigDecimal costoAdicional) {
    }
}
