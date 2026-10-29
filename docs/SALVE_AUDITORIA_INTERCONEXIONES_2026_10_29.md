# Salve — auditoría ejecutable de integración, 29 de octubre de 2026

> Referencia: revisión AGI de octubre (PR #290–#295), comprobación de main después de #296–#298 y protocolo obligatorio [ENGINEERING_REUSE_PROTOCOL.md](ENGINEERING_REUSE_PROTOCOL.md). Esta auditoría **no** acredita AGI, benchmark en el Galaxy S24 Ultra ni borrado remoto completo.

## Regla de diseño

**Buscar → Inspeccionar → Reutilizar → Conectar → desarrollar solo lo faltante → Probar**.
La arquitectura existente ya tiene un modelo de inferencia, controladores de herramientas, almacenes de evidencia, workers y evaluadores. No introducir un segundo LLM, memoria, selector principal, planificador de objetivos ni evaluador.

## Conexiones revisadas

| Productor | Consumidor / contrato real | Fuente de autoridad | Riesgo de duplicación / brecha |
| --- | --- | --- | --- |
| `MotorConversacional` | `CognitivePrompt.prioritize` → `limitTools` → `CognitiveTurn` → `CognitiveToolExecutor` | Allowlist y permisos Android, no texto del modelo | El catálogo de 500 caracteres ocultaba herramientas; se ajusta al presupuesto conocido sin ampliar permisos |
| `MemoriaEmocional` | `ConversationMemoryGrounding` → `MindRuntime.memoryLayersForPrompt` | Room y fuentes comprobadas | No crear otro recuperador; usar pruebas #296–298 antes de adoptar estrategias |
| `MindRuntime.local` | `SalveLLM.generateLocalResult` | Datos internos del teléfono | El enrutador genérico podía seleccionar nodos pese al comentario de privacidad; corregido con ruta existente |
| `GoalAutonomyRuntime` | `SalveLLM.generateLocalResult` | Diario privado de objetivos | Misma corrección; no otra interfaz de inferencia |
| `SleepFocusWorker` | `MindRuntime.dream`, `reflectIfDue`, `play`, `SleepImprovementRuntime` | Pose guardada, consentimiento de sueño, `BackgroundMindGate` | Coordinación del modo concentrado ya existente |
| `DreamWorker`, `PlayWorker` | `SleepFocusPolicy.allowStandalone` → mismas rutinas de mente | Petición manual o modo persistido | Trabajos periódicos ceden ante el sueño concentrado; órdenes explícitas no desaparecen |
| `ThinkWorker` | `GoalAutonomyRuntime`, `reflectIfDue` | `BackgroundMindGate` y cooldown de `ReflectionCycle` | No duplicar un supervisor global; evaluar interacciones en móvil |
| `AgentRuntime` | `AgentEngine` → `AgentProgressSupervisor` | `AgentPolicy`, `RoomAgentStore`, recibos | Clasificador de progreso no reemplaza verificador de objetivo |
| `ResearchTaskRuntime` | `ResearchTaskEngine`, `PublicResearchCoordinator` | Almacén de tareas y fuentes web | Es una ruta especializada; no fusionar a ciegas con `AgentEngine` |
| `MemoryStrategyExperiment` | `VerifiedStrategyPolicy`, `MemorySearchService` | Gold independiente + `salve_eval_gate.py` | #298 es laboratorio, no habilitado en conversación |
| `CloudSyncManager` | Eventos Room, pCloud, restauración y tombstones | Consentimiento, identidad, acuses remotos | Falta confirmar revocación de todos los respaldos externos |
| `DistributedCompute` | `SalveLLM` para inferencias explícitamente delegadas | Configuración del usuario, mTLS, política de datos | Los pensamientos privados usan la vía telefónica; evaluar clasificación de prompts externos |

## Cambios aplicados en esta rama

1. **Privacidad:** `MindRuntime.local` y reflexión de `GoalAutonomyRuntime` reutilizan `SalveLLM.generateLocalResult(prompt, role, null)`. Esta función existe y está sincronizada con la carga/generación del modelo. No se añade motor.
2. **Sueño sin solapamiento lógico:** `DreamWorker` y `PlayWorker` consultan el estado *persistido* de `SleepFocusWorker`; si hay sueño concentrado activo, no inician su ruta periódica. El trabajo solicitado explícitamente conserva una bandera y sigue permitido. `BackgroundMindGate` continúa siendo el único bloqueo de simultaneidad.
3. **Descubrimiento:** `CognitivePrompt.limitTools` amplía únicamente el catálogo ya autorizado, con límites de 500 / 650 / 850 / 1100 caracteres según presupuesto del turno; conserva su variante compacta anterior. El ejecutor sigue revalidando permisos.
4. **Prevención de regresiones:** `SleepFocusPolicyTest`, `CapabilityPlannerTest` y `scripts/test_architecture_integration_contract.py` cubren reglas funcionales y límites estructurales. No reemplazan un ensayo end-to-end.

## Pendientes y aceptación por etapas

### P0 — completar integración y comprobar dispositivo
- Compilar/debug/preview, memoria multilingüe y Windows; verificar firma, APK actualizable y regresiones con los dos modelos habilitados.
- Probar herramienta al principio/final del catálogo, preguntas multiintención, contextos pequeños, permisos y simulaciones.
- Medir en Galaxy S24 Ultra consumo, latencia p95, RAM y evidencia de sueño con modelo realmente inferido.

### P1 — objetivos y agentes
- Dibujar rutas entre `CognitiveTurn`, `AgentEngine`, `ResearchTaskRuntime` y `GoalAutonomyRuntime`, con ID de intención, cancelación y recibos sin repetir efectos.
- Comprobar si el usuario pide dos veces un objetivo, si el resultado es parcial y si una investigación invoca un agente ya activo. Integrar `AgentProgressSupervisor` donde exista consumidor, sin convertirlo en autoridad.
- No cambiar permisos ni reemplazar `RoomAgentStore`.

### P1 — memoria, aprendizaje, respaldo
- Usar 30+ casos congelados para `salve_eval_gate.py`, prueba #297 opt-in y comparaciones #298; aprobar estrategias solo con verificación independiente.
- Probar recuperación tras restauración parcial, memorias contradictorias y borrado de IDs entre Room, embeddings, grafo, diario y pCloud. No declarar borrado total hasta recibir acuse remoto.
- Comprobar aislamiento de prompts privados frente a nodos y que el cambio de modelo no invalida automáticamente un éxito previo.

### P2 — investigación avanzada
- Antes de LoRA/MoE o modelo del mundo, diseñar experimentos aislados con corpus reservado, comparación sin regresiones y reversión. La práctica y la memoria simbólica no son aprendizaje de pesos.
- No publicar código ni adoptar políticas automáticamente a partir de autocrítica o sueño.

## Verificación y reversión

La PR se deja para revisión con Android CI, JVM, Python, memoria multilingüe y Windows. Los checks demuestran compilación/contratos, no calidad de Gemma en hardware. Cambios reversibles: se mantienen las rutas anteriores de recuperación y el catálogo compacto; los workers conservan sus mismos IDs WorkManager y la misma puerta cognitiva. No hay migraciones, permisos nuevos, bases adicionales ni cambios en modelos.
