# Revisión de M8 antes de la integración

Fecha: 16 de noviembre de 2026. Rama: `feat/m8-notificaciones`; PR #19. Esta revisión conserva el código de M3 en su propia rama.

## Estado detectado en Sonar

La consulta de solo lectura al Quality Gate del PR #19 devolvió ERROR por cobertura nueva de 11.3 %, frente al 80 % requerido. Seguridad, fiabilidad y mantenibilidad figuraban en A. Se encontraron cuatro avisos `java:S8786` sobre la expresión regular de validación de correos: uno en `EventosService`, otro en `ResendCorreoAdapter` y dos en `ServiciosHttpAdapter`.

El workflow de M8 sí ejecutaba integraciones, pero el análisis de Sonar estaba en otro job que solo producía el informe de unitarias. Un check de integración verde no acreditaba que Sonar hubiera recibido esa cobertura.

## Correcciones

- El job que analiza Sonar ejecuta antes los perfiles de integración/calidad de M3 y M8 dentro de su propio runner. El reactor se comprobó con los POM actuales: en esta rama M3 sigue como esqueleto y M8 activa sus perfiles; cuando ambos estén implementados, el comando contempla ambos módulos.
- Sonar espera el resultado del Quality Gate y propaga sus fallos. Se mantiene el umbral remoto del 80 %; el perfil `quality` comprueba además 80 % de líneas y de ramas para M8.
- Las cuatro expresiones regulares de correo fueron sustituidas por `DireccionCorreo`, una comprobación sintáctica básica con longitud máxima de 254 caracteres y recorrido acotado. Rechaza destinatarios sin formato admitido, controles, espacios y caracteres de delimitación; la aceptación final del destinatario corresponde al proveedor.
- Se conserva la configuración de extensiones Oracle PL/SQL preparada para PostgreSQL en M3, sin modificar migraciones ni sus checksums.
- Se añadieron pruebas de contratos HTTP para respuestas inválidas de premium, instrumentos, cotizaciones, contactos, suscriptores y reportes; rechazos, respuestas sin confirmación y desconexiones de Resend; contenido cifrado manipulado y caída del origen del destinatario.

## Verificación local

| Comprobación | Java 21 | Java 25 |
| --- | --- | --- |
| Unitarias | 48 correctas | 48 correctas |
| Integración PostgreSQL y HTTP simulado | 84 correctas | 84 correctas |
| Fallos / errores | 0 / 0 | 0 / 0 |
| Checkstyle / SpotBugs | Correctos | Correctos |
| Umbral local de líneas y ramas >= 80 % | Correcto | Correcto |

JaCoCo local: 843/894 líneas (94.3 %) y 309/363 ramas (85.1 %). Los workflows pasaron actionlint. El recorrido con Java 25 se ejecutó desde `clean`.

Las integraciones locales seleccionan `AvisosIT,ContratosHttpIT` y usan bases PostgreSQL exclusivas mediante `m8.test.jdbc-url`; este equipo no tiene Docker. Los tres casos de RabbitMQ permanecen incluidos en el recorrido completo del workflow y no se omiten por falta de Docker. Los correos de las pruebas se envían únicamente a WireMock.

## Verificación remota pendiente

El usuario debe subir el commit y esperar la ejecución actualizada del PR #19. La cobertura local no se presenta como una nueva medición remota: comprobar Quality Gate aprobado, cobertura nueva >=80 % y los ratings A. Los resultados remotos anteriores de PostgreSQL/RabbitMQ y Docker pertenecen al commit anterior y no sustituyen esta nueva ejecución.

Los contratos pendientes con los productores M1/M2/M5/M6/M10 siguen documentados en [CONTRATOS.md](CONTRATOS.md). Las pruebas con WireMock no demuestran que esos servicios ya implementen los contratos propuestos. El scheduler mensual sigue desactivado hasta integrar sus fuentes.

Los registros se guardan fuera del repositorio, en la carpeta padre del checkout: `output/m8-sonar-java21-verify.log`, `output/m8-sonar-java25-verify.log` y `output/m8-sonar-reactor-profiles.log`.
