# Salve — Ciclo cognitivo conversacional v10

## Objetivo

Primera integración real de memoria, interpretación de intención, observación de acciones y retroalimentación. Se conserva el enrutador existente: no se afirma que todas las rutas tengan un único planificador ni que Salve haya alcanzado inteligencia general.

## Recorrido incorporado

1. **Seguridad antes que modelo**. Órdenes urgentes, permisos, confirmaciones y controles del usuario siguen teniendo prioridad.
2. **Memoria antes de la respuesta del modelo**. El motor reconstruye el hilo y recupera el contexto autorizado. La recuperación semántica es opcional y dispone de respaldo lexical.
3. **Checkpoint de cada inferencia**. CognitivePrompt incorpora TASK_STATE.cognitive_cycle, calculado por CognitiveCycleFrame: clasifica el objetivo (causa, comparación, procedimiento, dato o charla), observa memoria disponible y estados reales de herramientas. Las fases son GROUND, EVALUATE_EVIDENCE, RECOVER y FINISH_WITH_LIMITS. Se recalcula al volver a inferir tras una herramienta.
4. **Reformulación controlada**. CognitiveTurn puede utilizar web.search una segunda vez con una consulta realmente distinta tras observar la primera búsqueda. Mantiene MAX_TOOLS=2 y MAX_INFERENCES=4; no repite la misma consulta ni acciones sobre dispositivo, nube u otras herramientas.
5. **Revisión del objetivo**. TurnChecks rechaza respuestas causales/comparativas cortas que no desarrollan la relación pedida. El revisor puede solicitar una sola reescritura adicional; una frase con «porque» no demuestra por sí sola verdad factual.
6. **Evaluación común de entrega**. ConversationOutcomeAudit observa las respuestas entregadas durante el hilo, incluidas rutas especializadas, y las respuestas del ciclo cognitivo con recibos. ExecutionTrace registra solo estado y número de recibos, nunca prompts, datos personales, resultados web completos ni pensamiento privado.
7. **Retroalimentación rigurosa**. ConversationSkillCoordinator deja las respuestas generadas como PENDIENTES: no transforma silencio ni un cambio de tema en éxito. Solo una corrección, aprobación explícita o verificación independiente autorizan aprendizaje positivo/negativo.
8. **Curiosidad más pertinente**. CuriosityCycle ahora extrae solo frases con coincidencia local de las palabras clave del objetivo, evitando seleccionar introducciones no relacionadas de páginas parcialmente coincidentes. Una coincidencia léxica todavía no es verificación semántica.

## Estados de auditoría

FAILED, PENDING_APPROVAL, NEEDS_REVISION, EVIDENCE_OBSERVED_NOT_VERIFIED, RESPONSE_UNVERIFIED.

Se consultan mediante el comando existente «registro de ejecución» / «notas de esta tarea». Nunca equivalen a afirmar que el objetivo global se completó.

## Pruebas

- CognitiveCycleFrameTest: contexto previo, fases tras recibos, cancelación de afirmaciones sin evidencia y protección de contenido.
- ConversationOutcomeAuditTest: explicación insuficiente, recibo no implica éxito, acción pendiente de aprobación.
- ConversationSkillFeedbackSafetyTest: silencio y cambio de tema no son feedback positivo; corrección/aprobación explícita sí.
- TurnDemandAlignmentTest: preguntas causales, comparaciones y falsos positivos básicos.
- CuriosityEvidenceSelectionTest: rechazo de fuente superficial, extracción de afirmación pertinente.

## Límites y siguiente etapa

Esta PR no aumenta permisos, herramientas, presupuesto de ejecución ni modifica pesos de Gemma/Dolphin. Las rutas especializadas aún requieren adaptadores de postcondiciones y evaluadores independientes por dominio. Antes de migrarlas se necesita ensayo A/B en Android real, exactitud, fiabilidad, memoria, tiempo y batería. No se declara AGI ni comprensión universal.
