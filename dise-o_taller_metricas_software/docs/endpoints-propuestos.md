# Endpoints de la API

Contrato de API REST derivado de los flujos de negocio de [`Acuerdos contratos.md`](Acuerdos%20contratos.md),
implementado sobre el modelo de dominio en `backend/src/main/java/com/gfc/domain/model` (ver
[diagrama-clases.md](diagramas/diagrama-clases.md) y [ER.md](diagramas/ER.md)).

> Estos endpoints ya estan implementados: controladores en `backend/src/main/java/com/gfc/web`,
> servicios en `backend/src/main/java/com/gfc/application` y seguridad JWT en
> `backend/src/main/java/com/gfc/infrastructure/security/jwt`. Ver
> [funcionalidades.md](funcionalidades.md) para el detalle completo de la capa de aplicacion, y
> [postman/GFC-Backend.postman_collection.json](../postman/GFC-Backend.postman_collection.json)
> para probarlos de punta a punta (importable en Postman o ejecutable con `newman run`).

Todas las rutas bajo `/api/pedidos/**` exigen rol `CLIENTE` y `/api/ingredientes/**` exige rol
`EMPLEADO` (JWT en `Authorization: Bearer <token>`); una peticion sin token valido responde `401`
y una con rol incorrecto responde `403`. `/api/auth/**` y `GET /api/comidas/**` son publicos.

## 1. Autenticacion (`/api/auth`)

### `POST /api/auth/registro`

Registro de cliente — "loguea automaticamente" segun el contrato (la respuesta ya trae el token).

**Request body**
```json
{
  "nombreUsuario": "ana",
  "nombre": "Ana Torres",
  "tipoDocumento": "CC",
  "numeroDocumento": "1032456789",
  "correo": "ana@example.com",
  "contrasena": "una-clave-segura"
}
```
`tipoDocumento`: `CC` | `NIT` | `TI`

**Response `201 Created`**
```json
{
  "token": "eyJhbGciOi...",
  "nombreUsuario": "ana",
  "rol": "CLIENTE",
  "idCliente": 1,
  "nombre": "Ana Torres",
  "documento": "1032456789",
  "email": "ana@example.com"
}
```

| Status | Causa |
|---|---|
| 201 | Registro y login automatico exitosos |
| 400 | Campo faltante/formato invalido (email, tipo de documento) |
| 409 | Nombre de usuario, correo o documento ya existen |

### `POST /api/auth/login`

**Request body**
```json
{ "identificador": "ana", "contrasena": "una-clave-segura" }
```
`identificador` acepta el `nombreUsuario` o el `correo` (solo para clientes). El empleado sembrado
al arrancar (ver [funcionalidades.md](funcionalidades.md)) inicia sesion con `nombreUsuario`.

**Response `200 OK`**: mismo shape que el registro. Cuando quien inicia sesion es un empleado
(`Usuario` sin subtipo `Cliente`), `idCliente`, `nombre`, `documento` y `email` vienen `null`.

| Status | Causa |
|---|---|
| 200 | Credenciales validas |
| 401 | Usuario o contrasena incorrectos |

### `POST /api/auth/logout`

**Request**: header `Authorization: Bearer <token>`, sin body.

Al ser JWT sin estado, no hay lista de revocacion en el servidor: `logout` solo responde `204` y
es responsabilidad del cliente descartar el token.

| Status | Causa |
|---|---|
| 204 | Siempre (con o sin token) |

## 2. Catalogo de comida (`/api/comidas`) — flujo "Ver comida", publico

### `GET /api/comidas`

Sin paginacion (explicito en el acuerdo).

**Response `200 OK`**
```json
[
  { "id": 1, "nombre": "Hamburguesa Clasica", "precio": 18000.00, "imagenURL": null }
]
```

### `GET /api/comidas/{id}`

Detalle de comida, incluyendo su receta configurable.

**Response `200 OK`**
```json
{
  "id": 1,
  "nombre": "Hamburguesa Clasica",
  "precio": 18000.00,
  "imagenURL": null,
  "ingredientes": [
    {
      "id": 1,
      "ingrediente": { "id": 1, "nombreIngrediente": "Queso", "unidadMedida": "UNIDAD" },
      "cantidadBase": 1,
      "incluidoPorDefecto": true,
      "permiteAdicional": true,
      "permiteRetirar": true,
      "precioAdicional": 2000.00
    }
  ]
}
```

| Status | Causa |
|---|---|
| 200 | Encontrada |
| 404 | `id` no existe |

## 3. Pedido / resumen de pago (`/api/pedidos`) — rol `CLIENTE`

### `POST /api/pedidos`

Crea el pedido y **descuenta el inventario en el mismo momento** (regla del acuerdo: "se asume que
al hacer el pedido se retira del inventario directamente"), en `PedidoService` — el dominio solo
valida disponibilidad, quien aplica los `MovimientoInventario` de `SALIDA` es la capa de servicio.
No existe una pasarela de pago externa: la respuesta **es** el resumen de pago, un compilado de lo
pedido (comida, adiciones y retiros ya valorados). Retirar un ingrediente nunca baja el precio;
agregarlo siempre lo sube (lo aplica el propio dominio en `PersonalizacionIngrediente`).

**Request body**
```json
{
  "detalles": [
    {
      "comidaId": 1,
      "cantidad": 2,
      "personalizaciones": [
        { "configuracionId": 1, "tipoAdicion": "ADICIONAL", "cantidad": 1 }
      ]
    }
  ]
}
```

**Response `201 Created`** (el "resumen de pago")
```json
{
  "id": 1,
  "fecha": "2026-09-07T12:10:06.075913726",
  "detalle": [
    {
      "comida": "Hamburguesa Clasica",
      "cantidad": 2,
      "precioUnitario": 18000.00,
      "personalizaciones": [
        { "ingrediente": "Queso", "tipoAdicion": "ADICIONAL", "cantidad": 1, "costoAdicional": 2000.00 }
      ],
      "subtotal": 40000.00
    }
  ],
  "valorTotal": 40000.00
}
```

| Status | Causa |
|---|---|
| 201 | Pedido creado y stock descontado |
| 400 | Body invalido (cantidad <= 0, personalizacion no permitida por la receta) |
| 401 | Sin sesion |
| 404 | La comida o la configuracion de ingrediente no existen |
| 409 | Stock insuficiente (`puedePrepararse()` = false) |

## 4. Inventario (`/api/ingredientes`) — rol `EMPLEADO`

### `GET /api/ingredientes` — "Visor global de inventario"

**Response `200 OK`**
```json
[
  { "id": 1, "nombreIngrediente": "Queso", "unidadMedida": "UNIDAD", "stockActual": 56 }
]
```

| Status | Causa |
|---|---|
| 200 | OK |
| 401/403 | Sin sesion / rol distinto de `EMPLEADO` |

### `POST /api/ingredientes/{id}/entradas` — "Carga de inventario"

**Request body** (`fecha` es opcional, usa la actual si no viene)
```json
{ "cantidad": 50, "motivo": "Reabastecimiento proveedor" }
```

**Response `201 Created`**
```json
{
  "movimiento": { "id": 2, "tipo": "ENTRADA", "cantidad": 50, "fecha": "2026-09-07T12:10:06.25", "aplicado": true },
  "stockActual": 106
}
```

| Status | Causa |
|---|---|
| 201 | Movimiento aplicado |
| 400 | Cantidad <= 0, motivo vacio |
| 404 | Ingrediente no existe |
| 401/403 | Sin sesion / no es empleado |

### `POST /api/ingredientes/{id}/salidas` — "Descarga de inventario"

El acuerdo pide explicitamente **fecha y motivo** aqui ("coloca la fecha y la razon por la que se
retiro sin ser un pedido").

**Request body**
```json
{ "cantidad": 5, "fecha": "2026-09-07T09:00:00", "motivo": "Producto vencido" }
```

**Response `201 Created`**: igual forma que la entrada.

| Status | Causa |
|---|---|
| 201 | Movimiento aplicado |
| 400 | Cantidad <= 0, motivo vacio |
| 404 | Ingrediente no existe |
| 409 | Stock insuficiente (`disminuirStock` lanza `IllegalStateException`) |
| 401/403 | Sin sesion / no es empleado |

## Decisiones que cerraron las brechas iniciales

La primera version de este documento listaba 7 brechas entre el contrato y el dominio. Se
resolvieron asi (detalle completo en [dominio.md](dominio.md) y [funcionalidades.md](funcionalidades.md)):

1. `TipoDocumento` (CC/NIT/TI) se agrego como enum, enlazado a `Cliente`.
2. El registro pide `nombreUsuario` explicito, no se deriva del correo.
3. El descuento de stock vive en `PedidoService` (capa de aplicacion), no en el dominio.
4. Seguridad JWT implementada (`infrastructure/security/jwt`), endpoints protegidos por rol.
5. La "pasarela de pago" es el resumen compilado que devuelve `POST /api/pedidos`, no una
   integracion externa.
6. `Empleado` no tiene subtipo: es un `Usuario` con `Rol.EMPLEADO`.
7. Se construyeron repositorios, servicios y controladores sobre el dominio existente.

El unico punto sin cerrar: no hay un flujo de alta formal de empleados (el contrato solo define
auto-registro de clientes); se sembra uno de demostracion al arrancar la aplicacion.
