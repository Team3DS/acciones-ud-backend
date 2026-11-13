# Subida manual de M3 y espera de aprobación

El código está en una rama local. Todavía no existe un PR remoto de esta entrega. Estas instrucciones permiten publicarlo para revisión sin fusionarlo a `main`.

## 1 Revisar la copia local

Abrir PowerShell en el repositorio:

```powershell
Set-Location -LiteralPath 'C:\Users\user\Documents\Proyecto Instrumento Financiero-20261023T101507Z-1-001\acciones-ud-backend'
git status
git branch --show-current
git diff --check
```

La rama debe ser `feat/m3-catalogo-mercados`. Leer `m3-catalogo-mercados/docs/VALIDACION.md`, `REVISION_REGLAS_Y_WIKI.md` y `ADR-M3-001.md`. La ampliación incluye edición administrativa y nuevos tipos por solicitud de Jamyson; su integración y aceptación siguen pendientes del equipo.

La carpeta ya contiene los cambios; no hace falta descomprimir el ZIP encima. El ZIP sirve para entregar una copia a otra persona. Si se utiliza en otra copia del repositorio, conservar su estructura y revisar el manifiesto antes de incorporar archivos.

## 2 Ejecutar la verificación

Con Java 21 y Maven disponibles, cerrar cualquier M3 que esté usando el JAR antes de `clean` y ejecutar:

```powershell
mvn -B -pl m3-catalogo-mercados -am clean verify
```

En esta máquina también están disponibles herramientas portátiles. Si `mvn` o `java` no se reconocen, preparar solo la sesión actual:

```powershell
$m3ToolRoot=Join-Path $env:USERPROFILE '.codex\tmp\acciones-tools'
$env:JAVA_HOME=Join-Path $m3ToolRoot 'jdk-21.0.13+9'
$env:Path="$env:JAVA_HOME\bin;$(Join-Path $m3ToolRoot 'apache-maven-3.9.9\bin');$env:Path"
mvn -B -pl m3-catalogo-mercados -am clean verify
```

Para repetir todas las integraciones y los análisis de calidad, usar los comandos del README de M3. El recorrido con RabbitMQ real necesita Docker y activa el grupo `broker`. No se debe marcar ese recorrido o Docker Build como aprobado por el mero hecho de que las pruebas locales pasen.

## 3 Preparar el commit local

```powershell
git add -- README.md docker-compose.yml gateway/src/main/resources/application.yml .github/workflows/m3-catalogo.yml m3-catalogo-mercados
git diff --cached --stat
git diff --cached --check
```

Revisar que la lista incluya únicamente M3, su workflow, los ajustes de rutas/configuración y la documentación. `target/`, `.env`, herramientas, tokens y bases temporales quedan fuera.

Crear el commit manualmente:

```powershell
git commit -m "feat(m3): implementar catalogo administrable y eventos"
```

## 4 Comprobar la base y publicar la rama

```powershell
git fetch origin
git log --oneline HEAD..origin/main
```

Si aparecen cambios nuevos de `main`, incorporarlos y resolver sus conflictos antes de continuar, conservando los cambios del resto del equipo. Volver a ejecutar las pruebas si hay cambios o conflictos en el código. No usar force push.

Cuando la rama esté revisada:

```powershell
git push -u origin feat/m3-catalogo-mercados
```

Esto publica la rama, no fusiona el código a `main`. Si tu cuenta no tiene permisos de escritura, crear un fork en GitHub, añadirlo como remoto y subir allí la misma rama; el PR se abre desde ese fork hacia `Team3DS/acciones-ud-backend:main`.

## 5 Abrir el PR y solicitar revisión

En GitHub, pulsar **Compare & pull request**, o abrir [la comparación de la rama](https://github.com/Team3DS/acciones-ud-backend/compare/main...feat/m3-catalogo-mercados?expand=1) después del push.

Comprobar:

- **Base repository:** `Team3DS/acciones-ud-backend`.
- **Base:** `main`.
- **Compare:** `feat/m3-catalogo-mercados`, en la organización o en tu fork.
- **Título:** `feat(m3): implementar catalogo administrable y eventos`.

Copiar como descripción el contenido de `docs/PROPUESTA_PR_M3.md`. Seleccionar en **Reviewers** a Juan Carlos o al revisor que el equipo haya designado; seleccionarlo solo si aparece su cuenta correcta. Crear un PR normal listo para revisión para que puedan aprobarlo. Un Draft se utiliza si el equipo decide que todavía no debe solicitarse una aprobación formal.

## 6 Mantenerlo pendiente de aprobación

Dejar el PR abierto, con la revisión solicitada. No pulsar **Merge pull request** ni activar **Auto-merge**. Esperar los checks y la respuesta del revisor.

Pedir que revisen expresamente V3, la ampliación frente al SAD, la compatibilidad con M4/M5, los eventos y la emisión del claim `roles` por M1. El M1 actual no incluye roles en sus tokens; esa integración es necesaria para la administración real. Si el revisor pide cambios, realizarlos en la misma rama, ejecutar sus comprobaciones y subir otro commit; el PR se actualiza automáticamente.

La API pública muestra `main` como protegida, pero no permite leer todos los detalles de su protección. Si el equipo necesita que la aprobación sea una condición obligatoria, un administrador debe comprobar esa opción en las reglas de la rama. Dejar el PR abierto no sustituye esa configuración.

Solo después de la aprobación y los checks exigidos, la persona autorizada podrá fusionar siguiendo la regla documentada de **merge commit**. Este paso no forma parte de la subida solicitada: el objetivo de estas instrucciones termina con el PR pendiente de revisión.
