package com.gfc.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gfc.seed")
public record GfcSeedProperties(UsuarioSeed empleado, ClienteSeed cliente, ClienteSeed empresa) {
    public record UsuarioSeed(String usuario, String clave) {}
    public record ClienteSeed(String usuario, String clave, String nombre, String documento, String email) {}
}
