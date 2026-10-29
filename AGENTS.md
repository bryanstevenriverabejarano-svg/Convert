# Salve — Regla de desarrollo para agentes y colaboradores

Este archivo aplica a **todos los cambios futuros** del repositorio, incluidos los
realizados por agentes de IA, Codex y herramientas automatizadas.

## Regla obligatoria

**Buscar → inspeccionar → reutilizar → conectar → desarrollar solo lo que falte → probar.**

Antes de crear una clase, API, tabla, trabajador Android, servicio o herramienta:

1. **Buscar** implementaciones equivalentes, llamadas, pruebas y documentación.
2. **Inspeccionar** su comportamiento real, permisos, límites y persistencia.
3. **Reutilizar** lo que funcione y corregirlo antes de crear otra implementación.
4. **Conectar** los componentes existentes mediante interfaces pequeñas.
5. **Desarrollar solo lo que falte**, justificando cada componente nuevo.
6. **Probar** el camino nuevo y las regresiones del antiguo.

### Evidencia obligatoria en cada PR

Indicar rutas y métodos inspeccionados, capacidades reutilizadas, brecha que
faltaba, por qué las nuevas clases no duplican sistemas, tests ejecutados,
riesgos, compatibilidad y estado exacto de CI. No afirmar validación del
móvil, inferencia real o entrenamiento si solo se ejecutaron pruebas JVM.

### Invariantes

- Los recuerdos originales, las creencias inferidas, las lecciones y las
  estrategias poseen niveles de autoridad distintos; nunca fabricar fuentes.
- Al olvidar datos, revisar Room, índices, referencias derivadas, cachés,
  restauración parcial y tombstones de pCloud. Una ausencia local mientras
  se restaura la nube no prueba que un recuerdo fuera borrado.
- Gemma interpreta y propone; el controlador autoriza la herramienta, el
  presupuesto de ejecución y la exposición de datos privados.
- No alterar funcionalidades estables ni permisos para conseguir CI verde.
  Cuando las pruebas fallen, corregir la causa y repetir la ejecución.
- Toda mejora debe ser reversible, medible y con recibos reales.

Se exige esta revisión **antes de cada PR**, no únicamente en la etapa actual.
