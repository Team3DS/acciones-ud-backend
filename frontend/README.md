<div align="center">

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:7aa2f7,100:bb9af7&height=200&section=header&text=Soltech&fontSize=45&fontAlignY=35&desc=Frontend%20%C2%B7%20Team3DS&descAlignY=57&descSize=16" width="100%" alt="Soltech · Team3DS"/>

<img src="public/soltech.svg" width="72" alt="Soltech: símbolo de sol"/>

<img src="https://github.com/Team3DS.png" width="110" alt="Team3DS"/>

[![React](https://img.shields.io/badge/React-7dcfff?style=for-the-badge)](https://react.dev)
[![TypeScript](https://img.shields.io/badge/TypeScript-7aa2f7?style=for-the-badge)](https://www.typescriptlang.org)
[![Vite](https://img.shields.io/badge/Vite-9ece6a?style=for-the-badge)](https://vite.dev)
[![Team3DS](https://img.shields.io/badge/Team3DS-73daca?style=for-the-badge)](https://github.com/Team3DS)

**Tu futuro empieza aquí.**

Interfaz en español · Diseño responsivo · Inversión simulada · Alpaca Paper

</div>

> ### `Sobre el repositorio`

**Soltech** es el prototipo de la plataforma Acciones UD desarrollado por Team3DS. Está ajustado al borrador de requerimientos **v0.2, del 12 de octubre de 2026**, entregado por el equipo: solo **NYSE y NASDAQ**, diez acciones y moneda USD.

Incluye frontend React y **backend local básico en Node.js** para consultar y operar con una cuenta Alpaca Paper de desarrollo. **No es todavía el backend multiusuario completo del documento.** El registro, correo, MFA y fondeo son prototipos de interfaz; la creación automática de cuentas Broker, la autenticación real y el cobro de comisión están pendientes.

La [matriz de alineación de los 106 requisitos](docs/ALINEACION_REQUISITOS_V02.md) registra qué está parcial, qué solo tiene diseño y qué sigue pendiente. El PDF de mockups revisado se encuentra en [output/pdf/Soltech_Mockups_Interfaces_V02.pdf](output/pdf/Soltech_Mockups_Interfaces_V02.pdf).

> ### `Stack`

1. React con TypeScript y Vite.
2. Alias `@` hacia `src`.
3. CSS propio, iconos SVG y diseño adaptable desde móvil.
4. Node.js para API local y claves de proveedores.
5. Alpaca Trading API / Market Data en paper; Finnhub como respaldo opcional.
6. Node Test Runner para las pruebas controladas.

Recomendado: **Node.js 24** con npm. El proyecto declara también pnpm en `packageManager`; esta guía utiliza el `package-lock.json` incluido.

> ### `Características y estado actual`

| Área | Implementado en el prototipo | Integración pendiente |
| --- | --- | --- |
| Registro RF-01 | Nombre, apellido, documento de prueba, celular, correo, nivel, mercados, sectores, país e interés Premium | Correo único, alta segura y persistencia por usuario |
| MFA RF-02 | Vista OTP por correo, seis dígitos, vencimiento a los cinco minutos y bloqueo visual tras cinco fallos | OTP real de un solo uso, correo, bloqueo seguro y sesiones de servidor |
| Cuenta RF-06 / RF-08 | Pasos visuales de cuenta y fondeo sandbox | Alta automática por Broker API y movimientos de fondos por usuario |
| Mercados RF-11 / RF-12 | Diez acciones NYSE/NASDAQ; sectores; reloj de Alpaca y calendario del proveedor | Parametrización completa y metadatos que no entregue el feed |
| Dashboard RF-18 | Nombre, nivel, intereses y cuenta del proveedor, cuando está conectada | Aislamiento de usuarios y series históricas reales |
| Cotizaciones RF-17 | Bid, ask, spread, último precio, variación y volumen cuando el proveedor los devuelve; fuente y fecha | Capitalización e históricos. Se muestra «No disponible» cuando falta un dato |
| Market / Limit RF-25 / RF-26 | Cantidad entera, revisión previa, precio límite, DAY/GTC y envío a Alpaca | Libro de reservas y liquidación transaccional propios |
| Comisión RF-30 | Cálculo de referencia al 2 %, half-up a dos decimales; compra suma y venta resta | Cobro real en sandbox, tarifas por mercado, vigencias y registro por usuario |
| Cola RF-13 | Confirmación explícita fuera de sesión; envío sin horario extendido para que Alpaca gestione la cola | Persistir la confirmación y los eventos con actor y causa |
| Cancelación RF-34 | Solicitud de cancelación de órdenes Soltech pendientes y sin ejecución parcial | Libro propio que registre la liberación de valor más comisión |
| Historial RF-35 | Filtros por estado, acción, mercado y fechas; detalle y fechas conocidas del proveedor | Historial completo de transiciones; la lista usa las últimas 100 órdenes |
| Portafolio RF-38 | Cantidad, precio medio, actual, valor y ganancia/pérdida absoluta y porcentual | Consistencia ACID con cuentas y órdenes por usuario |
| Notificaciones RF-49 | Centro de sesión con contador de no leídas | Correo transaccional, persistencia y reintentos asíncronos |
| Perfil RF-57 | Edición de datos e intereses y solicitud de reporte mensual, guardadas localmente | Identidad y preferencias de servidor; generación y envío mensual |
| Premium RF-59 / RF-60 | Plan mensual USD 12 y anual USD 120; favoritos y alertas no se ofrecen como gratuitos | Stripe test, webhooks y autorización de servidor |
| Reportes RF-64 / RF-65 | Fechas, agrupación, ejecuciones, cargos del proveedor y pestaña de comisión estimada de Soltech | Comisiones efectivamente cobradas, reportes de órdenes completos y consolidado por usuario autenticado |
| CSV RF-69 | Exportación completa del período, UTF-8 BOM y protección de texto contra fórmulas | Exportación financiera a PDF; el PDF de mockups es documentación de diseño |

**Sin mercados simulados:** el flujo activo ya no usa el motor local de operaciones con precios fijos. Sin claves puedes navegar, completar el prototipo de registro y revisar los estados vacíos; para cotizar y enviar órdenes debes conectar Alpaca Paper. Los módulos anteriores de demo se conservan como código legado y no son la entrada de la aplicación.

> ### `Puesta en marcha`

#### 1. Requisitos

Instala Node.js 24 y Git si vas a clonar el repositorio. Abre una terminal nueva y verifica:

```sh
node --version
npm --version
git --version
```

#### 2. Obtener e instalar

Sustituye `URL_DEL_REPOSITORIO` por la URL de GitHub → Code:

```sh
git clone URL_DEL_REPOSITORIO soltech
cd soltech
npm ci
npm run dev
```

Si ya descargaste el proyecto, abre la terminal en la carpeta donde está `package.json` y ejecuta solo `npm ci` y `npm run dev`.

Abre [http://127.0.0.1:5173/](http://127.0.0.1:5173/), o la URL que indique Vite si cambia el puerto. Mantén la terminal abierta; **Ctrl + C** detiene el servidor. Frontend y API arrancan juntos.

#### 3. Navegar sin credenciales

Usa **«Explorar el prototipo sin autenticarme»** para abrir el espacio de trabajo, o completa el formulario con **datos ficticios**. El registro solicita documento y celular de prueba, nivel e intereses.

| Pantalla | Código de prueba |
| --- | --- |
| Verificación de correo | `123456` |
| OTP MFA por correo | `654321` |

No se envían correos ni se comprueban identidades. La caducidad y el bloqueo de códigos son estados de demostración en pantalla; no deben tratarse como medidas de seguridad reales. La contraseña no se almacena. El perfil de prototipo sí se guarda en el navegador: no introduzcas documentos ni datos personales reales.

El paso de fondeo local no modifica Alpaca. La cuenta Broker por usuario y el historial de fondos están pendientes.

> ### `Configurar Alpaca Paper`

Cada compañero configura sus propias claves localmente. `.env` no se sube a Git.

Windows / PowerShell:

```powershell
if (!(Test-Path -LiteralPath .env)) { Copy-Item -LiteralPath .env.example -Destination .env }
```

macOS / Linux:

```sh
if [ ! -f .env ]; then cp .env.example .env; fi
```

Abre `.env` y completa:

```dotenv
ALPACA_API_KEY=
ALPACA_SECRET_KEY=
ALPACA_DATA_FEED=iex
FINNHUB_API_KEY=
PORT=3001
```

| Variable | Valor |
| --- | --- |
| `ALPACA_API_KEY` | Clave de API de la cuenta **Paper Trading** |
| `ALPACA_SECRET_KEY` | Clave secreta de esa misma cuenta paper |
| `ALPACA_DATA_FEED` | `iex` para comenzar; `sip` requiere permisos del plan |
| `FINNHUB_API_KEY` | Opcional, para respaldo de último precio; no proporciona bid/ask |
| `PORT` | Puerto de `npm start`, por defecto `3001`; no cambia el puerto de Vite |

Genera las claves desde el entorno **Paper Trading** de [Alpaca](https://app.alpaca.markets/). No uses claves live ni nombres `VITE_*`. Guarda `.env`, detén Vite y vuelve a iniciar `npm run dev`.

Consulta [http://127.0.0.1:5173/api/paper/status](http://127.0.0.1:5173/api/paper/status), ajustando el puerto si hace falta:

```json
{"configured":true,"feed":"iex","mode":"paper"}
```

`configured: true` solo indica que están presentes ambas claves. Para comprobar su validez, abre **Mercado y órdenes** y verifica que se cargue la cuenta. La conexión usa una cuenta paper de desarrollo compartida, no una cuenta distinta por perfil.

> ### `Mercados y operación`

| Bolsa | Acciones | Sesión regular |
| --- | --- | --- |
| NYSE | JNJ, JPM, XOM | 09:30–16:00, America/New_York |
| NASDAQ | AAPL, MSFT, GOOGL, AMZN, META, TSLA, NVDA | 09:30–16:00, America/New_York |

El reloj de Alpaca determina festivos, cierre y horario de verano. No se habilitan LSE, TSE, ASX, cripto, margen, cortos, opciones ni horario extendido desde el formulario.

1. Abre **Mercado y órdenes** y selecciona la acción.
2. Elige compra o venta, cantidad entera y tipo **Market** o **Limit**. Para Limit, indica precio de dos decimales y vigencia día o hasta cancelación.
3. Comprueba valor, comisión estimada del 2 % y total/neto. Con mercado cerrado, confirma explícitamente el encolamiento si deseas continuar.
4. Pulsa **Revisar orden** y después **Confirmar**. Solo entonces se envía a Alpaca Paper.

La compra exige efectivo y poder de compra suficientes para valor más comisión estimada, y un margen de variación del 3 % adicional para Market. La venta exige acciones disponibles y no permite posiciones cortas.

**Comisión:** para 3 acciones a USD 227,63, el valor es USD 682,89 y la comisión estimada USD 13,66. La compra muestra USD 696,55; la venta USD 669,23. La aplicación todavía **no cobra** esa comisión ni la descuenta del efectivo de Alpaca. La parametrización por mercado y el libro de comisiones siguen pendientes; no se considera cumplido RF-30 íntegramente.

Fuera de horario se envía `extended_hours=false` con consentimiento explícito; Alpaca gestiona la cola para la siguiente sesión hábil según sus reglas. No se ejecuta un motor de mercado ficticio. El registro durable de esa confirmación está pendiente.

El objetivo RNF-02 es acuse del proveedor en ≤5 s; una orden aceptada aún puede estar pendiente. No se garantiza llenado completo ni se certifica latencia con pruebas controladas. Un timeout conserva el identificador; no se reenvía automáticamente para evitar duplicados.

En **Mis órdenes** se solicita la cancelación de órdenes propias pendientes sin ejecución. La confirmación y la liberación de las reservas del proveedor deben venir de Alpaca. El detalle muestra fechas conocidas y comisión estimada; aún falta una línea de tiempo propia persistente con actor y causa.

> ### `Reportes por período`

Abre **Reportes**, selecciona origen y fechas, y agrupa por día o mes. Alpaca Paper es el origen predeterminado; el historial local anterior se conserva solo para consulta de datos legado.

| Pestaña | Contenido |
| --- | --- |
| Resumen por período | Ejecuciones, volumen, compras, ventas, cargos y flujo de caja del proveedor |
| Transacciones | Cantidad de cada ejecución, precio, importe bruto, fechas e identificadores |
| Comisión Soltech (estimada) | Regla de referencia al 2 %, dos decimales, total compra/neto venta y estado **No cobrada** |
| Cargos del proveedor | FEE, DIVFEE, PTC y PTR devueltos por Alpaca, separados de la comisión de la plataforma |

Las ejecuciones se filtran por día completo en America/Bogota. Los cargos usan la fecha contable informada por el proveedor. El flujo neto es ventas menos compras menos cargos más reembolsos; no representa rentabilidad ni saldo final e ignora fondeos, retiros, dividendos e intereses.

La comisión estimada usa la tasa actual de referencia, no una tarifa histórica versionada. No debe tratarse como cargo efectivo ni atribuirse a usuarios autenticados: el perfil sigue siendo local. RF-65 permanece parcial hasta tener el libro de comisiones real de sandbox.

El historial se consulta en páginas de hasta 100 actividades, con hasta 1.000 por lote. Usa **Continuar carga** cuando queden páginas; los totales y CSV se habilitan solo tras completar la carga. La exportación incluye todo el período, no solo las 15 filas visibles.

Los importes de origen del proveedor se conservan hasta seis decimales para consulta; los cálculos de comisión usan centavos exactos y half-up a dos decimales. La normalización monetaria completa de todos los módulos a RNF-14 requiere el libro transaccional.

> ### `Premium y preferencias`

**Premium y favoritos** presenta USD 12/mes y USD 120/año. Los botones Checkout están deshabilitados hasta integrar Stripe test y sus webhooks. Sin suscripción no se habilitan favoritos ni alertas como funciones operativas.

La opción de **vista previa del diseño Premium** permite revisar la maqueta de watchlist sin realizar pagos ni activar una suscripción. La evaluación de umbrales sigue pendiente. No es control de acceso seguro: requiere autorización en servidor.

**Mi perfil** permite editar preferencias locales. **Notificaciones** incluye mensajes de esta sesión y contador de no leídas. El envío por correo, reportes mensuales, RBAC, inactividad y recuperación de contraseña aún no están implementados.

> ### `Comandos disponibles`

| Comando | Función |
| --- | --- |
| `npm ci` | Instalar las versiones de `package-lock.json` |
| `npm run dev` | Frontend y API de desarrollo |
| `npm test` | Pruebas controladas sin órdenes externas |
| `npm run build` | TypeScript y compilación a `dist/` |
| `npm start` | Aplicación compilada y API local en [http://127.0.0.1:3001/](http://127.0.0.1:3001/) |
| `npm run preview` | Solo frontend compilado; no incluye la API |

Para la compilación completa:

```sh
npm ci
npm run build
npm start
```

Para pnpm, los equivalentes son `corepack pnpm install` y `corepack pnpm dev`. Para reproducir el bloqueo incluido, usa npm de forma consistente.

> ### `Estructura`

```text
public/soltech.svg         Marca provisional y favicon
server/                   API local y adaptador Alpaca
src/
├── components/           Marca e iconos
├── features/paper/       Cotizaciones y órdenes paper
├── features/reports/     Historial y reportes
├── pages/Onboarding.tsx  Registro y MFA de prototipo
├── pages/Workspace.tsx   Espacio activo ajustado a v0.2
├── pages/Dashboard.tsx   Vista anterior, sin uso en la entrada actual
├── services/             Clientes y servicios; simulador anterior legado
├── store/                Perfil y estado local
├── styles/               Estilos responsivos
├── types/                Contratos de datos
└── utils/commission.mjs  Cálculo exacto compartido con el servidor
    utils/reports.ts      Fechas, agregaciones y CSV
tests/                    Lógica, API, reportes y alineación v0.2
docs/                     Matriz de requisitos y brechas
output/pdf/               Mockups documentales
tmp/                      Intermedios locales excluidos de Git
.env.example              Plantilla sin claves
```

> ### `API local`

| Ruta | Función |
| --- | --- |
| `GET /api/paper/status` | Configuración sin secretos |
| `GET /api/paper/quote?symbol=AAPL` | Quote y datos complementarios disponibles |
| `GET /api/paper/snapshot` | Cuenta, posiciones, últimas 100 órdenes y reloj |
| `GET /api/paper/activities?pageToken=…` | Ejecuciones y cargos paginados |
| `POST /api/paper/orders` | Market o Limit, revisión de fondos y consentimiento de cola |
| `POST /api/paper/cancel` | Solicitud de cancelación por identificador Soltech |
| `GET /api/paper/order?clientOrderId=…` | Seguimiento y recuperación de orden |

Las rutas son locales y requieren origen de localhost/127.0.0.1 para las mutaciones. Esto no sustituye autenticación ni autorización por usuario. No expongas la API en Internet antes de completar esos controles.

> ### `Pruebas y validación`

La suite tiene **38 pruebas controladas** de lógica local legado, proveedor paper, comisión, half-up, Market/Limit, consentimiento de cola, cancelación, historial y CSV. Ejecuta `npm test` y `npm run build`.

Estas pruebas no envían órdenes a Alpaca, no certifican OTP real, no miden 1.500 usuarios concurrentes ni garantizan cobertura del 70 %. La prueba conectada requiere claves paper válidas. Las comprobaciones visuales del PDF no equivalen a una prueba automatizada del navegador.

> ### `Protección del repositorio`

`.gitignore` excluye `.env`, claves, dependencias, compilaciones, cachés, logs, temporales, configuraciones personales, exportaciones y estado local de herramientas. Conserva código, pruebas, `package-lock.json`, `.env.example`, documentación y recursos.

Cada compañero configura sus claves. No subas datos personales, contraseñas ni credenciales. Un archivo ya versionado no desaparece del historial por agregarlo a `.gitignore`.

> ### `Solución de problemas`

| Problema | Solución |
| --- | --- |
| Node o npm no se reconocen | Instala Node.js 24 y abre una terminal nueva. |
| PowerShell bloquea npm.ps1 | Usa `npm.cmd ci` y `npm.cmd run dev`. |
| No encuentra package.json | Abre la terminal en la raíz del repositorio. |
| No aparecen precios o importes | Configura las claves paper y revisa `/api/paper/status`; no se fabrican datos al faltar conexión. |
| No reconoce las claves nuevas | Guarda `.env` y reinicia Vite. Verifica que el archivo no sea `.env.txt`. |
| Alpaca rechaza credenciales o feed | Usa claves Paper correspondientes y `ALPACA_DATA_FEED=iex`, salvo permisos de SIP. |
| Mercado cerrado | Confirma la cola si quieres enviar para la próxima apertura; no se opera en horario extendido. |
| Fondos insuficientes | Incluye comisión estimada y margen de Market, no solo precio × cantidad. |
| Orden pendiente o timeout | Conserva el identificador y consulta Alpaca; no dupliques la compra. |
| No puedo cancelar | Solo se solicita cancelación de órdenes Soltech pendientes, sin llenado parcial. Alpaca confirma el resultado. |
| Reporte incompleto | Continúa o reintenta la carga del historial antes de exportar. |
| Premium no activa pagos | Checkout y webhooks están pendientes; solo existe vista previa de diseño. |
| No funciona la API en preview | Usa `npm run dev` o `npm run build` y después `npm start`. |

> ### `Vistas básicas de frontend`

El menú del prototipo incluye tres vistas complementarias del mockup, disponibles incluso sin credenciales de Alpaca:

| Vista | Recorrido disponible | Estado |
| --- | --- | --- |
| Fondos de práctica | Consulta de efectivo si existe conexión, monto de abono, importes rápidos y revisión del saldo proyectado | Solo frontend; no modifica saldos ni registra movimientos. |
| Centro de ayuda | Guías con navegación a las pantallas, búsqueda y preguntas frecuentes desplegables | Frontend funcional. |
| Órdenes de protección | Formularios Stop Loss y Take Profit, acción, cantidad, umbral, vigencia y revisión con comisión estimada | Solo diseño; no valida tenencia ni envía órdenes. |

El botón **Agregar fondos**, en la cabecera, abre el diálogo de abono. Los diálogos pueden cerrarse con Escape o con el botón de cierre. Las confirmaciones que requieren integración permanecen deshabilitadas. No se agregaron endpoints ni funciones de backend para estas vistas. Administración y KPI siguen pendientes.

> ### `Alcance pendiente`

La entrega corrige el prototipo y documenta las brechas; **no declara cumplidos los 106 requisitos**. Faltan identidad y correo reales, sesiones y roles, cuentas/fondos Broker por usuario, base relacional ACID/3FN, reserva y cobro de comisiones, historial durable, Stripe, alertas, históricos, reportes de ganancias, administración y KPI, despliegue HTTPS/Docker y pruebas de carga.

M7 Comisionistas y M12 Auditoría ampliada son **Could Have**. No se activan reparto 60/40, asesoría ni controles de cumplimiento fingidos. Internacionales, KYC/retiros reales, Bloomberg, WhatsApp, push móvil, cobros reales y operaciones apalancadas permanecen fuera del alcance documental.

Referencia técnica: [órdenes Alpaca](https://docs.alpaca.markets/us/docs/orders-at-alpaca), [snapshot de cotizaciones](https://docs.alpaca.markets/us/reference/stocksnapshotsingle) y [actividades](https://docs.alpaca.markets/us/reference/getaccountactivities-2).

<div align="center">

**Team3DS** · Soltech · 2026

<img src="https://capsule-render.vercel.app/api?type=waving&color=0:7aa2f7,100:bb9af7&height=120&section=footer" width="100%" alt=""/>

</div>
