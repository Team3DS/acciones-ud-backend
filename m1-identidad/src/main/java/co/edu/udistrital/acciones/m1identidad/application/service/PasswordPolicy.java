package co.edu.udistrital.acciones.m1identidad.application.service;

import java.nio.charset.StandardCharsets;

public final class PasswordPolicy {
    private PasswordPolicy() {}

    public static boolean isValidNewPassword(String password) {
        return password != null && !password.isBlank()
                && password.codePointCount(0, password.length()) >= 8
                && password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
