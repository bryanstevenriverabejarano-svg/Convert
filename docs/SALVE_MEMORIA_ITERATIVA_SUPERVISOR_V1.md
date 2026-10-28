# Salve — Memoria iterativa y supervisión de objetivos (etapa siguiente)

**Objetivo:** continuar la etapa cognitiva posterior al PR #290, ya fusionado,
sin sustituir Gemma, migrar la base Room ni fingir una AGI. Se reutilizan
`ConversationMemoryGrounding`, `MemorySearchService`, `MemoryRecall`,
`AgentEngine` y `AgentRun`.

## A. Recuperación de recuerdos con segundo intento verificable

`MemoryRetryPlanner.secondQuery` entra en juego **solo** si:
- El dueño está verificado y la pregunta pide explícitamente recordar algo.
- La recuperación primaria devuelve `Status.EMPTY` (sin evidencia y sin error).
- No es una consulta de cronología («primer/último recuerdo»), GPS o borrado.
- Se pueden extraer de la búsqueda original **2–4 términos temáticos** que
  forman una consulta distinta a la frase inicial.

Se hace **una** segunda consulta mediante la misma infraestructura local de
recuerdos/embeddings, con el tema reducido a 100 caracteres. No inventa
sinónimos, no pregunta al LLM por hechos ausentes y no busca en internet.
Solo reemplaza la evidencia si el resultado contiene registros efectivamente
recuperados. La nota operativa indica qué tipo de intento se hizo, pero no
publica recuerdos privados en el registro de ejecución.

Si no hay evidencia, el sistema dirá que **no ha encontrado coincidencias
en los registros disponibles**, no que la memoria nunca existió. El registro
puede estar todavía incompleto si faltan recuerdos de pCloud.

### Invariantes
- Las consultas de perfil y primer/último recuerdo no cambian.
- Las lecturas `ERROR` y `PARTIAL` nunca se convierten en éxito ni se
  interpretan como almacenamiento vacío.
- Solo el usuario verificado puede disparar esta recuperación privada.
- No se modifica ningún recuerdo, índice, peso del modelo ni permiso.

## B. Supervisor de progreso sobre el agente persistente

`AgentProgressSupervisor.inspect(AgentRun)` reutiliza **solo** los
checkpoints y recibos guardados por el agente. Clasifica:

| Fase | Interpretación |
|---|---|
| PENDING | Pendiente: Android puede aplazar el trabajo |
| EXECUTING | En ejecución con puntos de control |
| STALLED | Dos o más intentos repetidos evitados |
| NEEDS_RESOURCES | Bloqueo explícito de recurso/modelo/permiso |
| PARTIAL | Completado parcialmente, falta evidencia de objetivo |
| VERIFIED | Finalizado y validado por comprobador independiente |
| UNVERIFIED | El modelo cree haber terminado, pero no se ha verificado |
| STOPPED | Cancelado o terminal sin confirmación |

Se ve en «mis planes» y «resultado de mi último plan».
**No es una nueva ruta de acciones:** no resume un plan por su cuenta, no
reinicia tareas fallidas, no modifica `AgentEngine`, y no puede ampliar
`AgentPolicy` ni el presupuesto de herramientas.

## C. Límites y pruebas siguientes

La recuperación iterativa propuesta reduce errores de consulta, pero **no
demuestra un aumento general de recall** sin comparar preguntas históricas
con sus registros de referencia. La supervisión mejora la observabilidad;
no ha creado nuevas habilidades.

Tras integrar y validar la APK ARM64, medir sobre el teléfono:
- Recall@1/4, porcentaje de preguntas sin evidencia, citas correctas,
  contradicciones, cobertura de datos recuperados y tiempo extra del
  segundo intento.
- Estados de `AgentRun`, progreso tras interrupciones, no duplicación de
  efectos, y diferencia entre revisión del LLM y verificación real.
- Escenarios con memoria de la nube todavía incompleta y usuario no
  verificado.

**Evoluciones posteriores**: memoria multinivel de episodios y conceptos
con trazabilidad de origen/borrado; replanificación persistente supervisada
tras estancamiento, con objetivos reversibles y evaluadores independientes.
Estas capacidades no se declaran implementadas en esta etapa.
