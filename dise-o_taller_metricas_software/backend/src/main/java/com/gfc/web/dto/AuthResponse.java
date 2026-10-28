package com.gfc.web.dto;

/** Datos de Cliente van null cuando quien se autentica es un empleado (Usuario sin subtipo). */
public record AuthResponse(String token, String nombreUsuario, String rol, Long idCliente,
                            String nombre, String documento, String email) {
}
