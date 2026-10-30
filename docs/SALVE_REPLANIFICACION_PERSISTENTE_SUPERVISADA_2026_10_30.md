# Salve: replanificación persistente supervisada — 30 oct 2026

## Regla permanente de desarrollo
**Buscar → Inspeccionar → Reutilizar → Conectar → Desarrollar solo lo que falta → Probar.**

Antes de modificar el código se comprobó:
- `AgentEngine` ya ejecutaba bucles limitados planear→actuar→observar→revisar; sus propuestas `AgentDecision` se checkpointan antes de cada llamada a herramienta, con un ID idempotente.
- `AgentRun` ya persistía progreso, `pending`, recibos, corrección `feedback`, `skippedRepeats`, `modelCalls` y revisiones en **el mismo diario Room**.
- `AgentExperience.repeatFeedback` ya impedía repetir acciones útiles y bloqueadas; los fallos transitorios se reintentaban una vez como máximo en la misma ejecución.
- `AgentExperimentOpportunity.afterRepeat` ya proponía **una** lectura pública de la URL proporcionada por el dueño cuando dos búsquedas fallaban, sin ampliar capacidades.
- `AgentChoiceFrame`, `AgentProgressSupervisor`, `RoomAgentStore.resume`, `AgentPolicy`, `AgentGoalVerifier` y los tests de Robolectric existentes ya cubrían decisiones, evaluación, permisos, reanudación y recuperación tras reinicio.

**No se añade:** motor de planificación, nuevo verificador, tabla Room, worker, sistema de memoria, modelos LLM, herramientas o rutas de acción.

## Hueco detectado y conectado

La ejecución podía recibir sucesivas propuestas idénticas bloqueadas y seguir consultando al modelo hasta agotar el presupuesto global de inferencias, sin una pausa persistente de supervisión.

Ahora se añaden dos campos **acotados** al `AgentRun` que ya persiste `RoomAgentStore`:
- `replanAttempts` (máximo **2**) y
- `replanState`: `NONE`, `RECONSIDER`, `SAFE_FALLBACK`, `NEEDS_REVIEW`.

En una propuesta repetida:
1. El controlador consulta **exactamente** el `repeatFeedback` actual sobre recibos y huellas persistidos; no confía en la autodeclaración del LLM.
2. Se reutiliza `AgentExperimentOpportunity` si hay **una** alternativa pública exacta previamente autorizada; la alternativa se conserva como `pending` **antes** de ejecutarla, manteniendo su clave de idempotencia.
3. Si no existe alternativa válida, elimina únicamente la llamada repetida, conserva los recibos y deja una corrección breve persistente para que el modelo cambie de enfoque.
4. Tras dos propuestas redundantes/replanificaciones, el plan pasa a **BLOCKED / NEEDS_REVIEW**. No se gasta el resto de las inferencias insistiendo, no se ejecuta herramienta repetida y **no se declara éxito**.
5. Solo el **comando existente de reanudación explícita del dueño** puede restablecer `replanAttempts` y `skippedRepeats` dentro de la transacción Room habitual. El objetivo, permisos y recibos previos permanecen; las huellas de acciones exitosas también, evitando reejecución. Las huellas de fallos se pueden limpiar conforme a la política explícita de reintento ya existente.

## Supervisión, visibilidad y límites

- `AgentRuntime.describe` muestra conteo y estado sin incluir nuevas transcripciones.
- `AgentChoiceFrame` emite los metadatos `corrective_replans_in_original_journal` y `needs_owner_review_not_automatic_retry` sin texto personal; su decisión de controlador es `REPORT_LIMITS` para un plan que necesita revisión.
- `AgentProgressSupervisor` mantiene el estado `NEEDS_RESOURCES` y explica la necesidad de autorización humana; no llama a WorkManager ni recupera planes por su cuenta.
- El contrato anterior de `AgentRun` versión 1 continúa siendo legible; se validan los nuevos campos y se rechazan valores desconocidos o superiores al límite.
- Los objetivos, enlaces observados, credenciales, instrucciones de herramientas y datos privados siguen **exclusivamente** en sus propietarios y contratos existentes.
- Se mantiene el límite de 8 herramientas, 12 llamadas de modelo, permisos de `AgentPolicy`, control de acceso por leases, cancelación y comprobación independiente `AgentGoalVerifier`.
- Esta fase **no** habilita nuevas clases de herramientas, decisiones externas, escritura de código, ni reanuda planes automáticamente. Una reanudación no transforma un resultado del modelo en una verificación independiente.
- El uso de huellas SHA-256 es solo deduplicación local, no anonimización de secretos adivinables.

## Pruebas

Las suites existentes se amplían para:
- comprobar que un modelo que insiste en pedir `code.python` sin autorización termina **BLOCKED**, con **una sola** observación bloqueada y ninguna ejecución remota;
- comprobar que dos errores `memory.lookup` no provocan búsquedas ilimitadas;
- conservar el fallback seguro `web.read` previamente permitido, una sola vez y con el mismo objetivo;
- cerrar y reabrir Room verificando estado `NEEDS_REVIEW`, presupuesto, recibos, permisos, ausencia de ejecución y transición tras **reanuda plan ID**;
- validar la compatibilidad con diarios antiguos y rechazar metadatos corruptos;
- garantizar resumen de supervisor sin respuestas privadas ni falsa certificación.

**Límites de evidencia:** la CI puede aprobar el contrato y la APK, pero las políticas de replanificación todavía requieren pruebas reales en el Galaxy S24 Ultra (RAM, latencia, desconexión, apagado del proceso, permisos, recuperación pCloud y batería). El cambio acota un bucle de control, no demuestra AGI ni autoaprendizaje de pesos del modelo.
