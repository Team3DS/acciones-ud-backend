package com.gfc.web.dto;

import com.gfc.domain.model.TipoPersonalizacion;
import java.util.List;

public record CrearPedidoRequest(List<DetalleRequest> detalles) {

    public record DetalleRequest(Long comidaId, int cantidad, List<PersonalizacionRequest> personalizaciones) {
    }

    public record PersonalizacionRequest(Long configuracionId, TipoPersonalizacion tipoAdicion, int cantidad) {
    }
}
