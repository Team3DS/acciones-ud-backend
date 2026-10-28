# Modelo de dominio

Implementacion del diagrama de clases para Java 21 y Spring Boot 3.3.5. Todos los importes representan pesos colombianos (COP).

## Organizacion

- `backend/src/main/java/com/gfc/domain/model`: las nueve clases del diagrama, los cuatro enumerados y validaciones del dominio.
- `backend/src/main/java/com/gfc/domain/port`: contrato para codificar y comprobar contrasenas.
- `backend/src/main/java/com/gfc/infrastructure/security`: adaptador PBKDF2 registrado como componente Spring.
- `backend/src/test/java/com/gfc`: pruebas del dominio, del adaptador y del arranque Spring existente.

Se conserva `BaseEntity` en `domain/model`, ahora con `@MappedSuperclass`, `@Id` de Jakarta Persistence, generacion `IDENTITY` y `@Version` para bloqueo optimista. Los identificadores son `Long`, positivos o `null` antes de persistir. JPA asigna el identificador al guardar; los alias como `getIdCliente()` devuelven la identidad heredada.

Por decision del proyecto, los objetos de dominio llevan directamente anotaciones JPA. Se mantienen las carpetas de Clean Architecture, aceptando esta dependencia de persistencia para simplificar el proyecto. Las nueve clases tienen `@Entity`, tablas explicitas, constructores protegidos sin argumentos y acceso JPA por campos; las modificaciones de negocio siguen pasando por los metodos existentes.

`Usuario` y `Cliente` usan herencia `JOINED`: tablas separadas con identidad compartida. Los enumerados se guardan como cadenas. Pedido persiste sus detalles y personalizaciones en cascada; eliminarlos de su propietario elimina sus filas (`orphanRemoval`). Los catalogos y clientes referenciados no se eliminan por cascada desde el pedido. Comida persiste sus configuraciones en cascada. La receta capturada por un detalle se conserva en una tabla de relacion `detalle_receta`.

El hash de contrasena se persiste; el codificador es `@Transient`. Un listener `@PostLoad`, gestionado por Spring, restaura el servicio de comprobacion al cargar usuarios sin recalcular el hash. La marca `aplicado` de los movimientos tambien se persiste. Las operaciones de inventario deben guardar el movimiento y el ingrediente dentro de la misma transaccion; `@Version` permite detectar escrituras concurrentes sobre filas existentes.

La configuracion existente permite crear las tablas en H2 y PostgreSQL mediante Hibernate. La integracion se verifica en H2. No se agregan endpoints ni repositorios de aplicacion en este cambio.

## Reglas y decisiones del diagrama

- Todos los importes usan `BigDecimal` en COP y las columnas monetarias usan `DECIMAL(19,2)`. Crear importes a partir de cadenas, por ejemplo `new BigDecimal("20000")` para veinte mil pesos. Se normalizan a dos decimales sin redondear; valores con fracciones menores de 0.01 COP o fuera de la precision de almacenamiento se rechazan.
- Las cantidades son enteras, expresadas en la unidad del ingrediente. No hay conversion implicita entre gramos y kilogramos. Stock y precios no pueden ser negativos; cantidades de pedidos, adicionales, retiros, entradas y salidas deben ser positivas. Los desbordamientos enteros se rechazan.
- `Usuario` permite los roles EMPLEADO y CLIENTE; `Cliente` hereda de Usuario y fija CLIENTE. Se validan nombre, documento y formato basico del email. Documento, email y nombre de usuario tienen restricciones de unicidad en la base de datos. `Cliente` ademas guarda `TipoDocumento` (`CC`, `NIT`, `TI`); la unicidad sigue siendo solo sobre el numero de `documento`, sin combinarla con el tipo.
- No existe una clase `Empleado`. Un empleado es simplemente un `Usuario` persistido con `Rol.EMPLEADO` (sin subtipo ni tabla propia), ya que no tiene atributos adicionales sobre los de `Usuario`.
- `comprobarContraseña(String)` conserva el nombre del diagrama. El constructor recibe una contrasena y un `CodificadorContrasena`; almacena solamente el hash sin exponerlo mediante getters. El adaptador usa PBKDF2-HMAC-SHA256, salt aleatorio de 16 bytes, 600000 iteraciones y comparacion de tiempo constante del resultado. No recorta espacios de las contrasenas.
- `Comida.configurarIngrediente(...)` crea y vincula cada configuracion a su comida; no permite repetir un ingrediente por instancia o identificador persistido. La configuracion no expone mutadores de negocio. Un ingrediente incluido por defecto necesita cantidad base positiva; uno opcional no consume stock hasta agregarse. Solo se permite retirar ingredientes incluidos por defecto.
- La cantidad de una personalizacion representa unidades del ingrediente **por unidad de comida**, no por pedido ni por porcion base. Por ejemplo, dos comidas con tres unidades adicionales cada una necesitan seis unidades adicionales del ingrediente.
- Se admite una personalizacion por configuracion en cada detalle. Para reemplazarla, eliminar la anterior y agregar una nueva. Debe estar permitida por la receta; no se puede retirar mas que la cantidad base. Un retiro no tiene costo ni genera descuento.
- `costoAdicional = cantidadPersonalizacion * precioAdicionalUnitario`; `subtotal = cantidadComidas * (precioUnitario + sumaCostosAdicionales)`; `total = sumaSubtotales`. Los getters de subtotal y total calculan el valor vigente para evitar datos desactualizados al modificar las personalizaciones.
- Cada detalle conserva el precio y una copia de la lista de configuraciones vigentes al crearse. Nuevas configuraciones de la comida no alteran detalles existentes.
- `DetallePedido.puedePrepararse()` calcula el consumo de ingredientes incluidos, adicionales y retiros, multiplicado por la cantidad de comidas. Consulta el stock sin reservarlo ni descontarlo. `Pedido.puedePrepararse()` ademas acumula ingredientes compartidos entre detalles, incluso por identificador persistido.
- Un pedido se crea con su primer detalle y no permite eliminar el ultimo: se respeta la multiplicidad `1..*`. Detalles y personalizaciones no se comparten entre dos propietarios; Al persistir, la eliminacion borra los hijos huerfanos; para otro propietario se crea una nueva instancia. Las colecciones se exponen como copias inmutables.
- Crear un `MovimientoInventario` no modifica stock. `aplicar()` realiza ENTRADA (suma), SALIDA (resta) o AJUSTE (diferencia con signo); exige motivo y fecha. No permite stock negativo ni volver a aplicar un movimiento cuya marca de aplicacion ya fue guardada. Una aplicacion fallida deja stock y marca de aplicacion intactos.
- Las validaciones de argumentos lanzan `IllegalArgumentException` (o `NullPointerException` para referencias requeridas); las operaciones incompatibles con el estado lanzan `IllegalStateException`.

Las operaciones sobre entidades cargadas, incluidas sus colecciones, deben ejecutarse dentro de una transaccion. La consulta de disponibilidad no reserva stock. La reserva/consumo completo de pedidos y el tratamiento de conflictos de bloqueo optimista corresponden a los futuros casos de uso. No se deben reintentar movimientos creando nuevas filas para el mismo evento.

## Como interpretar la cantidad de una personalizacion

Un detalle agrupa comidas que se preparan de la misma manera. Si el detalle tiene dos hamburguesas y su personalizacion dice tres lonchas extra de queso, cada hamburguesa lleva tres lonchas extra: seis en total. Si cada loncha cuesta 1000 COP, se cobran 6000 COP adicionales. La cantidad base de queso de la receta se suma aparte al consumo del inventario.

Si una hamburguesa lleva extras y otra no, se crean dos detalles de cantidad uno. Esto permite describir cada preparacion sin repartir automaticamente los extras entre las comidas.

## Ejemplo

```java
var codificador = new Pbkdf2CodificadorContrasena(); // Inyectado por Spring en un caso de uso.
var cliente = new Cliente(null, "ana", "una-clave", TipoDocumento.CC, "123", "ana@example.com", "Ana", codificador);
var queso = new Ingrediente(null, "Queso", 20, UnidadMedida.UNIDAD);
var comida = new Comida(null, "Hamburguesa", new BigDecimal("20000"), null);
var configuracion = comida.configurarIngrediente(null, queso, 2, true, true, true, new BigDecimal("1000"));
var detalle = new DetallePedido(null, comida, 3);
detalle.agregarPersonalizacion(new PersonalizacionIngrediente(null, configuracion, 2, TipoPersonalizacion.ADICIONAL));
var pedido = new Pedido(null, cliente, LocalDateTime.now(), detalle);
pedido.calcularTotal();   // 66000.00 COP
pedido.puedePrepararse(); // true: requiere 12 unidades de queso; no las descuenta.
```

## Verificacion

No hay pruebas automatizadas en el proyecto (se eliminaron por decision del equipo); las clases
del dominio llevan Javadoc con las reglas y excepciones de cada operacion. Para verificar el
comportamiento de punta a punta, usar la coleccion Postman en
[`postman/GFC-Backend.postman_collection.json`](../postman/GFC-Backend.postman_collection.json)
contra el backend corriendo (`mvn -B spring-boot:run` desde `backend` con JDK 21 o compatible).
