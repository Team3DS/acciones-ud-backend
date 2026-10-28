package com.gfc.domain.model;

import com.gfc.domain.port.CodificadorContrasena;

public class TestCodificador implements CodificadorContrasena {
    @Override
    public String codificar(String contrasena) {
        return "hash-" + contrasena;
    }

    @Override
    public boolean comprobar(String contrasena, String hash) {
        return hash.equals("hash-" + contrasena);
    }
}
