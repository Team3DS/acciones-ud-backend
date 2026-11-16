# Subida manual de M3 y espera de aprobación

Actualizado el 16 de noviembre de 2026. El código y la corrección de CI ya están guardados en commits locales en `feat/m3-catalogo-mercados`. La corrección de Docker está en `6081249`. Estas instrucciones publican el trabajo en una rama del repositorio del equipo.

El PR [#17](https://github.com/Team3DS/acciones-ud-backend/pull/17) se abrió desde el fork `JamysonRolon/acciones-ud-backend`. Para usar una rama dentro de `Team3DS/acciones-ud-backend` hay que abrir un PR nuevo después de la subida indicada aquí.

## 1 Revisar la copia local

Abrir PowerShell en el repositorio:

```powershell
Set-Location -LiteralPath 'C:\Users\user\Documents\Proyecto Instrumento Financiero-20261023T101507Z-1-001\acciones-ud-backend'
git switch feat/m3-catalogo-mercados
git status
git remote get-url origin
git log -5 --oneline
```

La rama debe ser `feat/m3-catalogo-mercados`, sin cambios pendientes. `origin` debe mostrar `https://github.com/Team3DS/acciones-ud-backend.git`. La rama aún puede mostrar seguimiento de `mi-fork`; el comando del paso 4 lo cambia a `origin`. Leer `m3-catalogo-mercados/docs/VALIDACION.md`, `REVISION_REGLAS_Y_WIKI.md` y `ADR-M3-001.md`. La integración y aceptación del alcance ampliado siguen pendientes del equipo.

La carpeta ya contiene los cambios; no hace falta descomprimir el ZIP encima. El ZIP sirve para entregar una copia a otra persona. Si se utiliza en otra copia del repositorio, conservar su estructura y revisar el manifiesto antes de incorporar archivos.

## 2 Verificación realizada y cómo repetirla

El 16 de noviembre pasaron 36 unitarias y 44 integraciones PostgreSQL por JDK, tanto en Java 21 como en Java 25, con cero fallos y errores. Checkstyle, SpotBugs, JaCoCo y la revisión de los workflows con actionlint también fueron correctos. Los registros están indicados en `VALIDACION.md`. No es necesario repetir estas pruebas para subir los commits ya preparados.

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

## 3 Commits preparados

La implementación, los ajustes anteriores de Java 25 y la corrección de Docker ya tienen commits locales. No volver a aplicar el patch anterior ni crear de nuevo el commit de implementación. `target/`, `.env`, herramientas, tokens y bases temporales quedan fuera de los commits.

## 4 Comprobar la base y publicar la rama

```powershell
git fetch origin
git log --oneline HEAD..origin/main
```

Al preparar esta entrega, `origin/main` estaba actualizado y era antecesor de la rama. Si posteriormente aparecen commits nuevos, incorporarlos con `git merge origin/main`, resolver sus conflictos y repetir las pruebas antes de continuar. No usar force push.

Cuando la rama esté revisada:

```powershell
git push -u origin feat/m3-catalogo-mercados
git branch -vv
```

Esto crea la rama dentro del repositorio del equipo y sube también sus commits anteriores. Después debe aparecer `[origin/feat/m3-catalogo-mercados]` como seguimiento. No usar simplemente `git push` antes de este paso: el seguimiento anterior apunta al fork. No subir directamente a `main`.

Si GitHub solicita iniciar sesión, usar la cuenta `JamysonRolon` con acceso al equipo. Si devuelve un error, conservar su mensaje y resolverlo antes de continuar; no cambiar el destino al fork como alternativa automática.

## 5 Abrir el PR y solicitar revisión

En GitHub, pulsar **Compare & pull request**, o abrir [la comparación de la rama](https://github.com/Team3DS/acciones-ud-backend/compare/main...feat/m3-catalogo-mercados?expand=1) después del push.

Comprobar:

- **Base repository:** `Team3DS/acciones-ud-backend`.
- **Base:** `main`.
- **Head repository:** `Team3DS/acciones-ud-backend`.
- **Compare:** `feat/m3-catalogo-mercados`, dentro del repositorio del equipo.
- **Título:** `feat(m3): implementar catalogo administrable y eventos`.

Copiar como descripción el contenido de `docs/PROPUESTA_PR_M3.md`. Seleccionar en **Reviewers** a Juan Carlos o al revisor que el equipo haya designado; seleccionarlo solo si aparece su cuenta correcta. Crear un PR normal listo para revisión para que puedan aprobarlo. Un Draft se utiliza si el equipo decide que todavía no debe solicitarse una aprobación formal.

El repositorio usa actualmente `main` como destino y no tiene rama `dev`. Después de crear y verificar el PR nuevo, cerrar el #17 como reemplazado, incluyendo el enlace al nuevo PR. Subir a `origin` no actualiza el #17 porque su origen sigue siendo el fork.

## 6 Mantenerlo pendiente de aprobación

Dejar el PR abierto, con la revisión solicitada. No pulsar **Merge pull request** ni activar **Auto-merge**. Esperar los checks y la respuesta del revisor.

La subida de esta rama activa `M3 Catalogo`; el PR activa además `CI Backend - Trunk Based`. Comprobar que `verify-m3 (21)` y `verify-m3 (25)` terminan correctamente, incluida la integración con PostgreSQL/RabbitMQ y la construcción de la imagen M3. Este equipo no tiene Docker: RabbitMQ real y Docker Build siguen pendientes del recorrido remoto. Esperar también los demás checks exigidos del PR.

Pedir que revisen expresamente V3, la ampliación frente al SAD, la compatibilidad con M4/M5, los eventos y la emisión del claim `roles` por M1. El M1 actual no incluye roles en sus tokens; esa integración es necesaria para la administración real. Si el revisor pide cambios, realizarlos en la misma rama, ejecutar sus comprobaciones y subir otro commit; el PR se actualiza automáticamente.

La API pública muestra `main` como protegida, pero no permite leer todos los detalles de su protección. Si el equipo necesita que la aprobación sea una condición obligatoria, un administrador debe comprobar esa opción en las reglas de la rama. Dejar el PR abierto no sustituye esa configuración.

Solo después de la aprobación y los checks exigidos, la persona autorizada podrá fusionar siguiendo la regla documentada de **merge commit**. Este paso no forma parte de la subida solicitada: el objetivo de estas instrucciones termina con el PR pendiente de revisión.
