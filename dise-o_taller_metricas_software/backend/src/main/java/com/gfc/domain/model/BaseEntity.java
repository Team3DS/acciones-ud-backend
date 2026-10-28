package com.gfc.domain.model;

import jakarta.persistence.*;

/** Identidad comun del dominio; null identifica un objeto todavia no persistido. */
@MappedSuperclass
@Access(AccessType.FIELD)
public abstract class BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    private Long version;

    /** Requerido por Hibernate para reconstruir la entidad al leerla de la base de datos. */
    protected BaseEntity() { }

    /**
     * @param id identificador persistido, o {@code null} para una entidad nueva sin guardar.
     * @throws IllegalArgumentException si {@code id} no es {@code null} y no es positivo.
     */
    protected BaseEntity(Long id) {
        if (id != null && id <= 0) {
            throw new IllegalArgumentException("El id debe ser positivo");
        }
        this.id = id;
    }

    /** @return el identificador persistido, o {@code null} si la entidad aun no se ha guardado. */
    public Long getId() {
        return id;
    }
}
