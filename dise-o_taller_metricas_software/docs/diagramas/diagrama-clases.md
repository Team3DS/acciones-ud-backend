# Diagrama de clases

Generado a partir del codigo fuente en `backend/src/main/java/com/gfc` (paquetes `domain.model`,
`domain.port` e `infrastructure.security`). Incluye unicamente clases realmente implementadas;
no hay controladores, servicios de aplicacion ni repositorios todavia.

```mermaid
classDiagram
    class BaseEntity {
        <<abstract>>
        -Long id
        -Long version
        +getId() Long
    }

    class Usuario {
        -String nombreUsuario
        -String contrasena
        -Rol rol
        -CodificadorContrasena codificador
        +comprobarContraseña(String) boolean
        +configurarCodificador(CodificadorContrasena) void
        +getNombreUsuario() String
        +getRol() Rol
        +getIdUsuario() Long
    }

    class Cliente {
        -TipoDocumento tipoDocumento
        -String documento
        -String email
        -String nombre
        +getTipoDocumento() TipoDocumento
        +getDocumento() String
        +getEmail() String
        +getNombre() String
        +getIdCliente() Long
    }

    class Comida {
        -String imagenURL
        -String nombre
        -BigDecimal precio
        -List~ConfiguracionIngrediente~ ingredientes
        +configurarIngrediente(Long, Ingrediente, int, boolean, boolean, boolean, BigDecimal) ConfiguracionIngrediente
        +getIngredientes() List~ConfiguracionIngrediente~
    }

    class Ingrediente {
        -String nombreIngrediente
        -UnidadMedida unidadMedida
        -int stockActual
        +aumentarStock(int) void
        +disminuirStock(int) void
        +hayStock(int) boolean
        +getIdIngrediente() Long
    }

    class ConfiguracionIngrediente {
        -int cantidadBase
        -boolean incluidoPorDefecto
        -boolean permiteAdicional
        -boolean permiteRetirar
        -BigDecimal precioAdicional
        +permitePersonalizacion(TipoPersonalizacion) boolean
    }

    class DetallePedido {
        -int cantidad
        -BigDecimal precioUnitario
        -List~ConfiguracionIngrediente~ receta
        -List~PersonalizacionIngrediente~ detallePedidoIngrediente
        +agregarPersonalizacion(PersonalizacionIngrediente) void
        +eliminarPersonalizacion(PersonalizacionIngrediente) void
        +calcularSubtotal() BigDecimal
        +puedePrepararse() boolean
        +cantidadesNecesarias() Map~Ingrediente,Integer~
    }

    class PersonalizacionIngrediente {
        -int cantidad
        -BigDecimal precioAdicionalUnitario
        -TipoPersonalizacion tipoAdicion
        +calcularCostoAdicional() BigDecimal
    }

    class Pedido {
        -LocalDateTime fecha
        -List~DetallePedido~ detalle
        +agregarDetalle(DetallePedido) void
        +eliminarDetalle(DetallePedido) void
        +calcularTotal() BigDecimal
        +puedePrepararse() boolean
        +getValorTotal() BigDecimal
    }

    class MovimientoInventario {
        -int cantidad
        -LocalDateTime fecha
        -String motivo
        -TipoMovimiento tipo
        -boolean aplicado
        +aplicar() void
    }

    class Validaciones {
        <<utility>>
        +texto(String, String) String$
        +positivo(int, String) int$
        +noNegativo(int, String) int$
        +dinero(BigDecimal, String) BigDecimal$
    }

    class CodificadorContrasena {
        <<interface>>
        +codificar(String) String
        +comprobar(String, String) boolean
    }

    class Pbkdf2CodificadorContrasena {
        <<component>>
        -int ITERACIONES$
        +codificar(String) String
        +comprobar(String, String) boolean
    }

    class UsuarioEntityListener {
        <<component>>
        +restaurarCodificador(Usuario) void
    }

    class Rol {
        <<enumeration>>
        EMPLEADO
        CLIENTE
    }

    class TipoMovimiento {
        <<enumeration>>
        ENTRADA
        SALIDA
        AJUSTE
    }

    class TipoPersonalizacion {
        <<enumeration>>
        ADICIONAL
        RETIRADO
    }

    class UnidadMedida {
        <<enumeration>>
        UNIDAD
        KILOGRAMO
        GRAMO
        LITRO
        MILILITRO
    }

    class TipoDocumento {
        <<enumeration>>
        CC
        NIT
        TI
    }

    BaseEntity <|-- Usuario
    Usuario <|-- Cliente
    Cliente ..> TipoDocumento
    BaseEntity <|-- Comida
    BaseEntity <|-- Ingrediente
    BaseEntity <|-- ConfiguracionIngrediente
    BaseEntity <|-- DetallePedido
    BaseEntity <|-- PersonalizacionIngrediente
    BaseEntity <|-- Pedido
    BaseEntity <|-- MovimientoInventario

    CodificadorContrasena <|.. Pbkdf2CodificadorContrasena
    Usuario o-- CodificadorContrasena : usa (transient)
    Usuario ..> Rol
    UsuarioEntityListener ..> Usuario : @PostLoad restaura
    UsuarioEntityListener --> CodificadorContrasena

    Comida "1" *-- "0..*" ConfiguracionIngrediente : compone (cascade ALL)
    ConfiguracionIngrediente "0..*" --> "1" Ingrediente : referencia
    ConfiguracionIngrediente ..> TipoPersonalizacion : valida

    Cliente "1" o-- "0..*" Pedido : realiza
    Pedido "1" *-- "1..*" DetallePedido : contiene (cascade ALL, orphanRemoval)
    DetallePedido "0..*" --> "1" Comida : pide
    DetallePedido "0..*" --> "0..*" ConfiguracionIngrediente : receta congelada (detalle_receta)
    DetallePedido "1" *-- "0..*" PersonalizacionIngrediente : personaliza (cascade ALL, orphanRemoval)
    PersonalizacionIngrediente "0..*" --> "1" ConfiguracionIngrediente : de
    PersonalizacionIngrediente ..> TipoPersonalizacion

    Ingrediente "1" o-- "0..*" MovimientoInventario : historial
    MovimientoInventario ..> TipoMovimiento

    Comida ..> Validaciones
    Ingrediente ..> Validaciones
    Pedido ..> Validaciones
    DetallePedido ..> Validaciones
    ConfiguracionIngrediente ..> Validaciones
    PersonalizacionIngrediente ..> Validaciones
    MovimientoInventario ..> Validaciones
    Usuario ..> Validaciones
    Cliente ..> Validaciones
```

## Lectura del diagrama

- **Herencia**: `Cliente` extiende `Usuario`, que extiende la superclase mapeada `BaseEntity` (id + version, bloqueo optimista). El resto de entidades tambien hereda de `BaseEntity`.
- **Composicion fuerte (`*--`)**: `Pedido` posee sus `DetallePedido` y cada `DetallePedido` posee sus `PersonalizacionIngrediente` — se crean/eliminan junto al propietario (`cascade = ALL`, `orphanRemoval = true`). `Comida` posee sus `ConfiguracionIngrediente` de forma similar, pero sin `orphanRemoval`.
- **Agregacion (`o--`)**: `Cliente` con sus `Pedido` e `Ingrediente` con sus `MovimientoInventario` — existen independientemente uno del otro; no se cascada su ciclo de vida desde el "todo", y el "todo" tampoco es dueño exclusivo de esos hijos.
- **Asociacion simple (`-->`)**: referencias de solo lectura sin due;o del ciclo de vida, por ejemplo `DetallePedido --> Comida` o `ConfiguracionIngrediente --> Ingrediente`.
- **Dependencia (`..>`)**: uso puntual de un tipo (enumerados, `Validaciones`), sin mantenerlo como atributo permanente.
- **`Validaciones`** es una clase de utilidad *package-private* (no expuesta fuera de `domain.model`) usada por casi todas las entidades para centralizar las reglas de validacion (texto obligatorio, enteros positivos/no negativos, montos en COP con `DECIMAL(19,2)`).
- **Puerto y adaptador**: `CodificadorContrasena` es un puerto del dominio (no depende de ninguna libreria de seguridad); `Pbkdf2CodificadorContrasena` es el unico adaptador, registrado como `@Component` de Spring e inyectado en `Usuario` y en `UsuarioEntityListener` (que restaura el codificador transitorio tras `@PostLoad`, ya que Hibernate no serializa campos `@Transient`).
