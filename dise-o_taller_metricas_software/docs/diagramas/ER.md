# Diagrama Entidad-Relacion

Generado a partir del modelo JPA implementado en `backend/src/main/java/com/gfc/domain/model`.
Refleja las tablas que Hibernate crea hoy en H2 (unica base de datos del proyecto).

```mermaid
erDiagram
    USUARIOS {
        bigint id PK
        bigint version
        varchar nombre_usuario UK "not null"
        varchar contrasena "hash PBKDF2, not null"
        varchar rol "EMPLEADO | CLIENTE, not null"
    }

    CLIENTES {
        bigint id PK, FK "= usuarios.id (JOINED)"
        varchar tipo_documento "CC | NIT | TI, not null"
        varchar documento UK "not null"
        varchar email UK "not null"
        varchar nombre "not null"
    }

    COMIDAS {
        bigint id PK
        bigint version
        varchar nombre "not null"
        decimal_19_2 precio "not null, COP"
        varchar imagen_url "nullable"
    }

    INGREDIENTES {
        bigint id PK
        bigint version
        varchar nombre_ingrediente "not null"
        varchar unidad_medida "UNIDAD | KILOGRAMO | GRAMO | LITRO | MILILITRO"
        int stock_actual "not null, >= 0"
    }

    CONFIGURACIONES_INGREDIENTE {
        bigint id PK
        bigint version
        bigint comida_id FK
        bigint ingrediente_id FK
        int cantidad_base "not null, >= 0"
        boolean incluido_por_defecto
        boolean permite_adicional
        boolean permite_retirar
        decimal_19_2 precio_adicional "not null"
    }

    PEDIDOS {
        bigint id PK
        bigint version
        bigint cliente_id FK
        timestamp fecha "not null"
    }

    DETALLES_PEDIDO {
        bigint id PK
        bigint version
        bigint pedido_id FK
        bigint comida_id FK
        int cantidad "not null, > 0"
        decimal_19_2 precio_unitario "not null, copiado de comidas.precio"
    }

    DETALLE_RECETA {
        bigint detalle_id FK
        bigint configuracion_id FK
    }

    PERSONALIZACIONES_INGREDIENTE {
        bigint id PK
        bigint version
        bigint detalle_id FK
        bigint configuracion_id FK
        int cantidad "not null, > 0"
        varchar tipo_adicion "ADICIONAL | RETIRADO"
        decimal_19_2 precio_adicional_unitario "not null"
    }

    MOVIMIENTOS_INVENTARIO {
        bigint id PK
        bigint version
        bigint ingrediente_id FK
        int cantidad "not null (signo segun tipo)"
        timestamp fecha "not null"
        varchar motivo "not null"
        varchar tipo "ENTRADA | SALIDA | AJUSTE"
        boolean aplicado "not null"
    }

    USUARIOS ||--o| CLIENTES : "hereda (JOINED, mismo id)"
    CLIENTES ||--o{ PEDIDOS : realiza
    COMIDAS ||--o{ CONFIGURACIONES_INGREDIENTE : "compone (cascade ALL)"
    INGREDIENTES ||--o{ CONFIGURACIONES_INGREDIENTE : "se usa en"
    COMIDAS ||--o{ DETALLES_PEDIDO : "se pide como"
    PEDIDOS ||--o{ DETALLES_PEDIDO : "contiene (cascade ALL, orphanRemoval)"
    DETALLES_PEDIDO ||--o{ DETALLE_RECETA : ""
    CONFIGURACIONES_INGREDIENTE ||--o{ DETALLE_RECETA : ""
    DETALLES_PEDIDO ||--o{ PERSONALIZACIONES_INGREDIENTE : "personaliza (cascade ALL, orphanRemoval)"
    CONFIGURACIONES_INGREDIENTE ||--o{ PERSONALIZACIONES_INGREDIENTE : "de"
    INGREDIENTES ||--o{ MOVIMIENTOS_INVENTARIO : "historial de"
```

## Notas del mapeo

- **Herencia `USUARIOS` / `CLIENTES`**: estrategia `InheritanceType.JOINED`. Cada `Cliente` es una fila en `usuarios` (con `rol = CLIENTE`) mas una fila en `clientes` que comparte la misma clave primaria. Hoy no existen empleados persistidos como subtipo: `Rol.EMPLEADO` existe en el enumerado pero no tiene una tabla propia todavia.
- **`DETALLE_RECETA`**: tabla intermedia de la relacion `@ManyToMany` entre `DetallePedido` y `ConfiguracionIngrediente`. Guarda una copia congelada de la receta vigente de la comida al momento de crear el detalle; no se actualiza si la comida cambia despues.
- **Cascada y borrado huerfano**: `pedidos -> detalles_pedido` y `detalles_pedido -> personalizaciones_ingrediente` usan `orphanRemoval = true` (al quitar un hijo de la coleccion en memoria, se borra su fila). `comidas -> configuraciones_ingrediente` usa cascada `ALL` pero sin `orphanRemoval`.
- **Restricciones unicas**: `usuarios.nombre_usuario`, `clientes.documento`, `clientes.email`, `(comida_id, ingrediente_id)` en `configuraciones_ingrediente` y `(detalle_id, configuracion_id)` en `personalizaciones_ingrediente`.
- **Bloqueo optimista**: toda tabla hereda `version` de `BaseEntity` (`@Version`) para detectar escrituras concurrentes.
- **Motor de base de datos**: unicamente H2, persistido en archivo (`jdbc:h2:file:./data/gfcdb`), ver `backend/src/main/resources/application.yml`. El soporte a PostgreSQL fue retirado del proyecto.
