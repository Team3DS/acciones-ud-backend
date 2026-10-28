# Funcionalidades implementadas en el backend

Estado actual de `backend/` (Spring Boot 3.3.5, Java 21). Este documento describe **solo lo que
existe en el codigo hoy**, no el alcance final del taller. Ver tambien [ER.md](diagramas/ER.md),
[diagrama-clases.md](diagramas/diagrama-clases.md) y el contrato de API completo en
[endpoints-propuestos.md](endpoints-propuestos.md).

## 1. Arranque de la aplicacion

- Aplicacion Spring Boot estandar (`GfcApplication`) expuesta en el puerto `8080`.
- Base de datos unica: **H2 persistido en archivo** (`jdbc:h2:file:./data/gfcdb`, es decir
  `backend/data/gfcdb.mv.db`), con consola habilitada en `/h2-console`. Los datos sobreviven a un
  reinicio del proceso. El soporte a PostgreSQL fue retirado por completo. Las pruebas usan un H2
  en memoria aparte (`src/test/resources/application.yml`) para no ensuciar `backend/data`.
- Documentacion interactiva de la API vía **Swagger UI** en `/swagger-ui.html`.
- Empaquetado en **Docker** (`backend/Dockerfile`, build multi-stage Maven + JRE Alpine).

## 2. Pagina de error 404 personalizada

- `static/error/404.html`: pagina de error con la identidad del proyecto (GFC · GLUD Fried
  Chicken), servida automaticamente por Spring Boot para clientes que aceptan HTML.
- Los clientes que piden JSON (`Accept: application/json`) reciben el `ErrorResponse` uniforme del
  `GlobalExceptionHandler` (ver seccion 5).

## 3. Seguridad: JWT y autorizacion por rol

- `infrastructure/security/jwt/JwtService`: emite y valida los JWT (HMAC, libreria `jjwt`); el
  `subject` es el `nombreUsuario` y lleva el `rol` como claim. Secreto y expiracion configurables
  en `application.yml` (`gfc.jwt.secret`, `gfc.jwt.expiracion-minutos`).
- `JwtAuthenticationFilter`: valida el header `Authorization: Bearer` en cada peticion y puebla el
  contexto de seguridad; un token de un usuario que ya no existe se ignora sin romper la peticion.
- `UsuarioDetailsService`: adapta `Usuario` (via `UsuarioRepository`) a `UserDetails` con la
  autoridad `ROLE_<rol>`. La contrasena **no** se valida con el `PasswordEncoder` de Spring
  Security: eso ya lo hace el dominio (`Usuario.comprobarContraseña`, PBKDF2 propio); el login
  compara ahi y solo despues emite el JWT.
- `SecurityConfig`: sesiones `STATELESS`, CSRF deshabilitado. `` /api/auth/** `` y
  `GET /api/comidas/**` son publicos; `/api/pedidos/**` exige rol `CLIENTE`;
  `/api/ingredientes/**` exige rol `EMPLEADO`. Sin token -> `401`; con rol incorrecto -> `403`
  (entry point y access-denied handler propios, en vez del comportamiento por defecto de Spring
  Security).
- `logout` no mantiene lista de revocacion (JWT sin estado): solo responde `204` y el cliente
  descarta el token.

## 4. Modelo de dominio (persistente via JPA)

Modelo de dominio rico, mapeado directamente con anotaciones JPA en `com.gfc.domain.model`:

### Usuarios y clientes
- `Usuario`: identidad base con nombre de usuario unico, contrasena **hasheada** y rol
  (`EMPLEADO` o `CLIENTE`). El hash se calcula en el constructor mediante un
  `CodificadorContrasena` inyectado; nunca se expone en texto plano. **No existe una clase
  `Empleado`**: un empleado es un `Usuario` persistido con `Rol.EMPLEADO`, sin subtipo propio,
  porque no tiene atributos adicionales.
- `Cliente` extiende `Usuario` (herencia `JOINED`) y agrega `TipoDocumento` (`CC`/`NIT`/`TI`),
  documento, correo y nombre, con restricciones de unicidad y validacion basica de formato de
  email.
- Autenticacion de contrasena: `comprobarContraseña(String)` compara contra el hash almacenado.
- Adaptador de hashing: `Pbkdf2CodificadorContrasena` (PBKDF2-HMAC-SHA256, salt aleatorio de 16
  bytes, 600 000 iteraciones, comparacion en tiempo constante). El dominio depende solo del
  puerto `CodificadorContrasena`.
- `UsuarioEntityListener` (`@PostLoad`): reconecta el codificador (campo `@Transient`, gestionado
  por Spring) cada vez que Hibernate carga un `Usuario`, sin recalcular ni tocar el hash.

### Catalogo de menu
- `Comida`: nombre, precio en COP (`BigDecimal`, `DECIMAL(19,2)`) e imagen opcional.
- `Ingrediente`: nombre, unidad de medida y stock actual (no puede ser negativo).
- `ConfiguracionIngrediente`: vincula un ingrediente a una comida (composicion, sin duplicados),
  con las mismas reglas de siempre (incluido por defecto exige cantidad base positiva, solo se
  retira lo incluido por defecto, sin ingredientes repetidos).

### Pedidos y personalizacion
- `Pedido`/`DetallePedido`/`PersonalizacionIngrediente`: igual que antes — un pedido conserva al
  menos un detalle, cada detalle congela precio y receta al crearse, las personalizaciones respetan
  los permisos de la receta y el retiro nunca cuesta ni descuenta.
- `DetallePedido.puedePrepararse()`/`Pedido.puedePrepararse()` siguen siendo **de solo lectura**:
  calculan si hay stock suficiente sin descontarlo. El descuento real vive en la capa de servicio
  (seccion 5), por decision explicita del equipo: el dominio no debe conocer que existe un
  "movimiento de inventario" disparado por un pedido.

### Inventario
- `MovimientoInventario`: `ENTRADA`/`SALIDA`/`AJUSTE`, con fecha y motivo obligatorios. `aplicar()`
  se ejecuta una sola vez y nunca deja el stock en negativo.

## 5. Capa de aplicacion y API REST

Contrato completo (bodies, respuestas y codigos de estado) en
[endpoints-propuestos.md](endpoints-propuestos.md). Resumen de las piezas nuevas:

- **Repositorios** (`infrastructure/persistence`): uno por entidad raiz —
  `UsuarioRepository`, `ClienteRepository`, `ComidaRepository`, `IngredienteRepository`,
  `PedidoRepository`, `MovimientoInventarioRepository`. Las entidades poseidas por cascada
  (`DetallePedido`, `PersonalizacionIngrediente`, `ConfiguracionIngrediente`) se navegan desde su
  dueño, sin repositorio propio.
- **Servicios** (`application`):
  - `AuthService`: registra clientes (valida unicidad antes de tocar la base, para devolver `409`
    limpio) y hace login, emitiendo el JWT en ambos casos.
  - `ComidaService`: listado y detalle del catalogo.
  - `PedidoService`: arma el `Pedido` a partir del request, valida `puedePrepararse()`, y **en esta
    capa** agrega las cantidades necesarias por ingrediente y aplica un `MovimientoInventario` de
    `SALIDA` por cada uno, todo dentro de la misma transaccion antes de persistir el pedido.
    Devuelve el "resumen de pago": el compilado de comida + adiciones + retiros ya valorado (no hay
    pasarela de pago externa).
  - `InventarioService`: listado de ingredientes y registro de entradas/salidas.
- **Controladores** (`web`): `AuthController`, `ComidaController`, `PedidoController`,
  `IngredienteController`, mapeando 1:1 el contrato.
- **`GlobalExceptionHandler`**: traduce excepciones a HTTP — `IllegalArgumentException`/
  `NullPointerException` (validaciones del dominio) a `400`; `IllegalStateException`/
  `ConflictoDatosException`/`StockInsuficienteException` a `409`; `RecursoNoEncontradoException` a
  `404`; `CredencialesInvalidasException` a `401`.

## 6. Datos de demostracion al arrancar

`infrastructure/startup/DatosDemoSeeder` (un `CommandLineRunner`) siembra, solo si no existen:

- Un empleado (`nombreUsuario: empleado`, `contrasena: empleado123`) para poder probar login y los
  endpoints de inventario sin flujo de alta (el acuerdo solo define auto-registro de clientes).
- Un catalogo minimo: un ingrediente ("Queso", stock 50) y una comida ("Hamburguesa Clasica") con
  una configuracion de ese ingrediente, para poder ejercitar `GET /api/comidas` y
  `POST /api/pedidos` de punta a punta sin cargar datos a mano.

## 7. Persistencia

- Todas las entidades usan `@MappedSuperclass BaseEntity` con `id` autogenerado (`IDENTITY`) y
  `@Version` para bloqueo optimista.
- Relaciones JPA con cascada/`orphanRemoval` donde el dominio es dueño exclusivo de sus hijos
  (pedido/detalle, detalle/personalizacion); el resto son referencias simples navegadas via
  repositorio.

## 8. Pruebas automatizadas

No hay pruebas automatizadas en `backend/src/test` por decision del equipo (se eliminaron las
que cubrian dominio, persistencia, el adaptador de hashing y los cuatro controladores). Como
compensacion, todas las clases publicas de `domain.model`, `application` y `web` llevan Javadoc
explicando su rol, reglas de negocio y los codigos de estado que producen. La coleccion Postman en
[`postman/GFC-Backend.postman_collection.json`](../postman/GFC-Backend.postman_collection.json)
sigue siendo la forma de verificar manualmente (o con `newman run`) que el backend funciona de
punta a punta.

## 9. Lo que todavia NO esta implementado

- No hay un flujo de **alta formal de empleados**: solo el seeder de demostracion (seccion 6).
- No hay **revocacion de tokens** (blacklist/refresh tokens): el JWT es valido hasta que expira.
- No hay endpoints para **consultar el historial de pedidos** de un cliente ni de movimientos de
  inventario (solo creacion y el visor de stock actual).
- No hay manejo explicito de **conflictos de bloqueo optimista** (`@Version`) mas alla de que
  Hibernate los detecte a nivel de base de datos (no se traduce a un `409` especifico todavia).
