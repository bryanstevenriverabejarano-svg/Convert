# Salve — supervisión unificada de autonomía, objetivos y tareas (PR siguiente a #298)

## Alcance y arquitectura reutilizada

**Buscar → inspeccionar → reutilizar → conectar → desarrollar solo lo faltante → probar.**

Ya existen tres diarios y tres ejecutores con semánticas diferentes:

| Diario existente | Control / propietario del estado | Progreso que puede afirmarse |
| --- | --- | --- |
| `AgentRun` en Room | `AgentEngine`, `RoomAgentStore`, `AgentDao` y `AgentWorker` | Recibos de herramientas, bloqueo, reintentos, concesión, verificación independiente `AgentGoalVerifier` |
| `ResearchTask` en Room | `ResearchTaskEngine`, `RoomResearchTaskStore`, `ResearchTaskWorker` | Pendiente/ejecutándose, resultado por archivar, archivado, parcial o fallido; **archivar no verifica la verdad** |
| `GoalAutonomy` en `autonomy/goals.json` | `GoalAutonomyRuntime`, `GoalAutonomy`, trabajador de reflexión | Objetivos configurados, pausados, elegibles para **preparar** reflexión y propuestas pendientes de revisión; **no son tareas externas realizadas** |

También existía `AgentProgressSupervisor.inspect(AgentRun)` en PR #292 y se mostraba en `AgentRuntime`. Esta fase **amplía esa misma clase**, no añade un motor de planes, una cola, una base de datos, un scheduler ni un verificador nuevo.

## Cambios

1. `GoalAutonomy.overview()` proporciona un agregado inmutable, protegido por el bloqueo del diario, con **conteos** de objetivos, pausas y propuestas. No entrega descripciones, notas, hipótesis, identidades ni textos originales. Si la lectura del diario falla, indica `unavailable`, en lugar de fingir cero objetivos.
2. `GoalAutonomyRuntime.overview()` expone ese agregado desde el runtime único que ya utiliza la aplicación.
3. `AgentProgressSupervisor.inspectAt(run, now)` reutiliza `inspect` y detecta concesiones `RUNNING` vencidas solo con el checkpoint real del agente. Señalar un lease vencido **no** recupera ni reejecuta un plan.
4. `AgentProgressSupervisor.inspectAll` recibe listas acotadas de los registros **existentes** y produce conteos por estado. No se basa en el texto del LLM ni confunde `SUCCEEDED` de una investigación con `VERIFIED` de un objetivo del agente.
5. `AgentRuntime` añade ese resumen a los comandos que ya existen: **«estado del agente»** y **«mis planes»**. Si un diario no se puede leer, comunica «supervisión conjunta no disponible» sin inventar información ni filtrar la excepción.

## Invariantes

- No se generan nuevas metas, herramientas, investigaciones ni ciclos de sueño.
- No se concede permiso, no se llama al modelo, no se modifica el plan, no se fuerza la reanudación ni se toca una concesión desde el supervisor.
- La pausa, cancelación, programación WorkManager, reglas de idempotencia y reclamación de leases siguen en sus propietarios originales.
- Los estados se calculan **solo sobre los registros recientes suministrados**, no son estadísticas de todos los datos históricos. La interfaz usa hasta 50 planes y 20 tareas de investigación.
- Un plan `SUCCEEDED` sin `goalVerification=VERIFIED` figura **sin comprobación independiente**. Una reflexión aceptada no acredita que se cumplió el objetivo en el mundo. `ResearchTask.SUCCEEDED` indica archivado, no veracidad.
- El texto del resumen no incluye objetivo, pregunta, herramienta, recibo, URL, excepción, nota ni contenido de memoria; los flujos anteriores siguen sujetos a sus propios permisos y protecciones de visualización.
- La supervisión indica concesiones expiradas, pero **no** llama a `recover()` ni `schedule()`; solo el ejecutor original decide cuándo corresponde reclamar o reanudar una tarea.
- Ante un diario inaccesible, se informa explícitamente del fallo; nunca se asume que no hay trabajo.

## Pruebas y límites

`AgentProgressSupervisorTest` amplía los contratos de supervisor original con:
- consolidación de planes, investigaciones y objetivos conservando sus diferencias;
- detección de leases vencidos sin cambios en el estado de ejecución;
- conservación de notas privadas y ausencia de llamadas al LLM;
- reconocimiento explícito de diario de objetivos dañado o inaccesible.

Las verificaciones de GitHub Actions prueban compilación y regresiones; no equivalen a medir autonomía real en un Galaxy S24 Ultra. Sigue pendiente ensayar replanificación persistente supervisada en condiciones reales, con límites de batería/RAM, permisos, entrega de resultados y necesidad de aprobación explícita cuando se salgan de las capacidades autorizadas.
