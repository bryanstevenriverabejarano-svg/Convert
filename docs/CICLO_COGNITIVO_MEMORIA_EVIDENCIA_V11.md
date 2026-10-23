# Salve v11 — Segunda consulta de memoria y evidencia unificada

## Objetivo

Ampliar la reforma v10 ya fusionada, sin aumentar privilegios y sin confundir una
respuesta generada con una conclusión demostrada.

## Recorrido real

1. El usuario introduce una tarea. Las órdenes urgentes, la comprobación de
   identidad y el control de permisos siguen ejecutándose antes del modelo.
2. MotorConversacional recupera el contexto y los recuerdos pertinentes antes
   de la inferencia, como en v10. El modelo puede solicitar memory.lookup.
3. **Nueva consulta de memoria refinada**: memory.lookup acepta un campo opcional
   query. MemoryProbePolicy solo permite temas anclados a la petición actual o
   a los tres mensajes recientes del usuario, no al texto de páginas web ni
   a respuestas del asistente. Rechaza URLs, saltos de línea, consultas ajenas
   y valores desproporcionados. IdentityManager.privateAccess sigue siendo
   obligatorio. El motor puede realizar una segunda lectura de memoria con
   una consulta realmente diferente, nunca una tercera (MAX_TOOLS=2,
   MAX_INFERENCES=4), y no vuelve a ejecutar herramientas con efectos.
4. Las rutas especializadas de investigación pública y respuestas directas
   derivadas de memoria ahora adjuntan ConversationOutcomeEvidence al
   evaluador común de entrega. Usa datos del controlador: estado de lectura,
   presencia de fuentes y cantidad de búsquedas, no frases autodeclaradas
   por el modelo. La traza de ejecución registra únicamente metadatos.
5. ConversationOutcomeAudit puede distinguir EVIDENCE_UNAVAILABLE de
   EVIDENCE_OBSERVED_NOT_VERIFIED. **Observar una fuente o encontrar un
   recuerdo NO confirma que el mensaje final sea verdadero**.
6. No se reentrenan pesos de Gemma; las estrategias solo reciben una señal
   positiva cuando existe corrección o aprobación explícita o evaluación
   independiente, según la política v10.

## Evidencia y datos privados

- No se guardan en la auditoría prompts, URLs, nombres, texto de recuerdos,
  contenido web ni razonamiento interno.
- Si una consulta de memoria no está vinculada al turno, el controlador la
  rechaza antes de leerla; una orden escondida en una página no amplía el
  alcance del usuario.
- El estado FOUND en memoria es un **dato recuperado**, no una verificación
  independiente de su contenido.
- Los recibos de herramientas con estado FAILED no se consideran resultados
  completados. Los de handoff siguen requiriendo autorización.

## Pruebas

MemoryProbePolicyTest, CognitiveMemoryFeedbackTest,
ConversationOutcomeEvidenceTest y GroundingAuditAblationTest.
Se añaden regresiones para rechazo de inyección, lectura adicional acotada,
respuesta vacía, fuente ausente y clasificación de ocho estados de
investigación antes/después de adaptar sus metadatos.

La comparación de GroundingAuditAblationTest **solo mide capacidad de
distinguir estados en Java**, no calidad de respuestas del modelo ni
mejora porcentual de inteligencia.

## Pendiente: prueba A/B en el Galaxy S24 Ultra

Conseguir consentimiento y un conjunto de preguntas desconocidas para Salve,
comparar v10 vs v11 con el mismo modelo y contexto, registrar acierto factual,
continuidad, pertinencia de memoria y fuentes, latencia, RAM y batería.
No fusionar un benchmark A/B real con uno simulado.

## Siguiente paso

Unificar postcondiciones verificables de acciones sobre dispositivo, dibujo
y planes, sin perder el control de permisos ni ampliar autonomía silenciosa.
