# Contratos backend - frontend

## Protocolo de compra de comida

1. Iniciar sesión
2. Ver comida
   - Ver detalle de comida
   - Sin paginación a nivel de backend
3. Pasarela de pago
4. Log-out

## Protocolo de login

### Protocolo de registro

Registro de persona:

1. Entra a la interfaz
   - Opción de logueo
   - Opción de registro
2. Interfaz de registro
   - Nombre
   - Tipo de documento (el backend recibe):
     - CC
     - NIT
     - TI
   - Número de documento
   - Correo
   - Contraseña
   - **Loguea automáticamente**

# Gestión de inventario

> **Primera gestión de inventario en las compras por parte del usuario.**
>
> Se asume que al hacer el pedido se retira del inventario directamente.

## Carga de inventario

1. Empleado se loguea
2. El empleado entra a la interfaz de inventario
3. El empleado hace una carga de inventario
4. El empleado se desloguea

## Descarga de inventario

1. Empleado se loguea
2. El empleado entra a la interfaz de inventario
3. El empleado hace una descarga de inventario; adicionalmente coloca la fecha y la razón por la que se retiró sin ser un pedido
4. El empleado se desloguea

## Visor global de inventario

1. El empleado se loguea
2. Entra a la sección de inventario, visualiza qué hay y qué no hay
3. Se desloguea

