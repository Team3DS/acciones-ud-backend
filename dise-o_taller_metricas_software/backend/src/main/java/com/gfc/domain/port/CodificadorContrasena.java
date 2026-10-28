package com.gfc.domain.port;

/** Puerto para evitar que el dominio dependa de una biblioteca de seguridad. */
public interface CodificadorContrasena {
    String codificar(String contrasena);
    boolean comprobar(String contrasena, String hash);
}
