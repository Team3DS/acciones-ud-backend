package co.edu.udistrital.acciones.m1identidad.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    private String secret = "change-me";
    private long accessTtlSeconds = 900;
    private long refreshTtlSeconds = 604800;
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public long getAccessTtlSeconds() { return accessTtlSeconds; }
    public void setAccessTtlSeconds(long accessTtlSeconds) { this.accessTtlSeconds = accessTtlSeconds; }
    public long getRefreshTtlSeconds() { return refreshTtlSeconds; }
    public void setRefreshTtlSeconds(long refreshTtlSeconds) { this.refreshTtlSeconds = refreshTtlSeconds; }
}
