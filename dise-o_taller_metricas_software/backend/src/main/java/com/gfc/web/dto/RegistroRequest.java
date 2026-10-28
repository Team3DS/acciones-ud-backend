package com.gfc.web.dto;

import com.gfc.domain.model.TipoDocumento;

public record RegistroRequest(String nombreUsuario, String nombre, TipoDocumento tipoDocumento,
                               String numeroDocumento, String correo, String contrasena) {
}
