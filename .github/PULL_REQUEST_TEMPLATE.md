## Que

Descripcion breve del cambio.

## Por que

Motivo o issue relacionado (numero de issue si aplica).

## Como probar

Pasos para verificar el cambio localmente.

## Checklist

- [ ] `mvn -B verify` pasando en el modulo afectado
- [ ] Pruebas unitarias escritas o actualizadas
- [ ] Cobertura JaCoCo >= 70% en logica crítica
- [ ] Imagen Docker construible (`docker build`)
- [ ] Endpoint `/actuator/health` respondiendo
- [ ] Sin secretos ni `.env` commiteados (revisar `git status` / `git diff`)
- [ ] Sin emojis en código ni docs
- [ ] Sin comentarios de código salvo que se pidan explícitamente
- [ ] Mensaje de commit: `tipo(scope): descripcion en español`
