# Salve — v11: memoria iterativa y evaluación transversal de evidencias

## Qué mejora

Se continúa desde la PR #231 (fusionada). En el bucle de inferencias la herramienta de memoria ya no busca forzosamente solo las palabras del primer mensaje: `memory.lookup` admite una `query` opcional y el modelo puede reformular una lectura para explorar una parte relevante del mismo objetivo. La segunda consulta sigue compartiendo el límite de **2 herramientas / 4 inferencias** por ciclo. Repetir exactamente la consulta no genera otra operación. Los controles de identidad y acceso a recuerdos privados continúan fuera del LLM.

La clase `MemoryProbePolicy` valida las consultas contra los términos del mensaje actual, el tema reconstruido y un máximo de tres mensajes anteriores del **usuario**, nunca contra instrucciones contenidas en páginas web ni en respuestas del modelo. Las consultas demasiado extensas, con URLs, instrucciones multilinea o desviaciones de tema son rechazadas; para aceptar un refinamiento debe compartir un porcentaje suficiente de términos sustantivos con el objetivo original.

## Evaluación común de rutas especializadas

`ConversationOutcomeEvidence` transforma estados del controlador de memoria o del coordinador de investigación en metadatos de evidencia:
- `OBSERVED`: hay alguna evidencia/resultado observado, **no equivale a haber probado la respuesta**.
- `PARTIAL`: operación con resultados parciales.
- `ABSENT`: no se recuperaron evidencias pertinentes.
- `ERROR`: no pudo consultarse la fuente. No equivale a ausencia de recuerdos.

`ResearchDesk` adjunta estados de investigación (número de fuentes, búsquedas y estado) y `MotorConversacional` adjunta los de respuestas directas de memoria al **mismo** evaluador `ConversationOutcomeAudit` incorporado en v10. Una respuesta puede terminar en `EVIDENCE_OBSERVED_NOT_VERIFIED`, `EVIDENCE_UNAVAILABLE`, `NEEDS_REVISION` o `RESPONSE_UNVERIFIED`. La respuesta no recibe crédito factual automático ni aprendizaje positivo.

La auditoría común registra **solo estados y contadores** en `ExecutionTrace`; no replica las frases privadas ni páginas web. Las trazas anteriores de investigación conservan su comportamiento previo y deben auditarse aparte si se requiere una política de minimización más estricta. Las observaciones se aíslan mediante un `ThreadLocal` por turno y se consumen al entregar la respuesta; nunca se atribuyen a turnos posteriores.

## Pruebas

- `MemoryProbePolicyTest`: consultas refinadas relacionadas, pregunta sobre identidad, continuidad con el historial del usuario, rechazo de URLs, líneas con instrucciones y desviación hacia secretos.
- `CognitiveMemoryFeedbackTest`: primera consulta de memoria y segundo refinamiento; la misma consulta no se repite; se preserva el límite de dos acciones.
- `ConversationOutcomeEvidenceTest`: distingue falta de fuentes, estado parcial, fallo de lectura de memoria, fuentes observadas sin verificación de verdad y texto vacío.

## Límites

Este trabajo **no** hace que todo el enrutador de Salve esté ya unificado bajo un único planificador. Tampoco constituye comprobación semántica independiente de verdad, retentrenamiento del LLM ni AGI.

Pendiente: comparar respuestas en Samsung Galaxy S24 Ultra con el motor anterior y actual, instrumentar oráculos por dominio y medir tanto exactitud como RAM/latencia. El ensayo de dispositivo debe usar preguntas inéditas y datos con consentimiento, manteniendo las rutas urgentes sin bloqueo de LLM.
