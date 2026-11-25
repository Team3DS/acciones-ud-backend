package co.edu.udistrital.acciones.m8notificaciones.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/** The recovery origin is deployment configuration, never event or request data. */
@Component
public final class EnlaceRecuperacion {
    private final String origin;

    public EnlaceRecuperacion(@Value("${app.public-url:https://accionesud.site}") String publicUrl) {
        URI uri;
        try {
            uri = URI.create(publicUrl);
        } catch (IllegalArgumentException | NullPointerException invalid) {
            throw invalidConfiguration();
        }
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getHost() == null
                || uri.getRawUserInfo() != null
                || uri.getRawQuery() != null
                || uri.getRawFragment() != null
                || (uri.getRawPath() != null && !uri.getRawPath().isEmpty()
                        && !"/".equals(uri.getRawPath()))) {
            throw invalidConfiguration();
        }
        origin = "https://" + uri.getRawAuthority() + "/";
    }

    public String crear(String email, String code) {
        return origin + "#password-reset?correo=" + encode(email) + "&code=" + encode(code);
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private static IllegalArgumentException invalidConfiguration() {
        return new IllegalArgumentException("APP_PUBLIC_URL debe ser un origen HTTPS sin credenciales, ruta, consulta ni fragmento");
    }
}
