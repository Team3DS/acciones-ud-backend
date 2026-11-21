package co.edu.udistrital.acciones.m4datosmercado.infrastructure.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        @NotNull @Min(1) long quoteTtlSeconds,
        @NotNull @Min(1) long snapshotTtlSeconds) {}