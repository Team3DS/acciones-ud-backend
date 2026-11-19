package co.edu.udistrital.acciones.m3catalogomercados.infrastructure.security;

import java.util.Set;

public record UsuarioAutenticado(long id, Set<String> roles) {}
