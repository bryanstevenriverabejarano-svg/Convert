# Salve — PR #298 · Selección experimental de estrategias de memoria

## Objetivo y frontera de validez

La investigación sobre aprendizaje en línea, metaaprendizaje y estrategias
revisables motiva medir **si** una variante concreta de búsqueda tiene menos
coste sin perder recuerdos, autoría, fechas ni honestidad sobre los resultados.
No implica entrenar Gemma, crear una nueva memoria ni acreditar AGI.

**El PR #297 ya registró métricas Android opt-in, pero no produjo todavía un
benchmark completo, etiquetado por humanos, del Galaxy S24 Ultra. Por eso
este PR NO activa selección adaptativa en la conversación normal.**

## Buscar → Inspeccionar → Reutilizar → Conectar → Desarrollar solo lo faltante → Probar

| Componente existente | Qué se mantiene | Brecha cubierta aquí |
| --- | --- | --- |
| `MemorySearchService` | FTS, expansión, 8 búsquedas máximas, autoría, saliencia y ranking | Overload explícito de estrategia y contador de consultas reales, sin modificar las llamadas normales |
| `MemorySearchQuery` | Sanitización, cuatro términos, sin FTS crudo ni búsquedas masivas | Ninguna segunda consulta ni extractor |
| `ConversationMemoryGrounding` + E5 + Room | Evidencia y procedencia en contexto; reintento lexical y fusión semántica | **Sin modificación** en este PR |
| `MemoryRetryPlanner` | Reintento semántico tras recuperación vacía y dueño verificado | **Sin modificación** |
| `VerifiedStrategyPolicy` | Hasta 96 observaciones, huellas SHA-256 de caso y escenario, pares comprobados, veto a fallos y reversión | Se reutiliza mediante `MemoryStrategyExperiment`, no se crea otro learner |
| `SkillLab` / `AutonomousToolLab` | Aprendizaje simbólico, versiones y regresiones en herramientas | No se modifica; el experimento de memoria no promueve herramientas |
| `salve_eval_gate.py` (PR #296) | >=30 casos, 3+ por dominio, mismas revisiones de modelo, métricas de citas y rechazo de regresiones | **No se duplica**: revisión independiente obligatoria para cualquier promoción |
| `MemoryEvaluationProbe` (PR #297) | Tiempos y fuentes HMAC sin texto personal | Recibos para ensayos futuros, no son un gold humano |

## Experimento A/B con código de búsqueda real

La ruta actual usa `BASELINE`. Nueva opción de **laboratorio solamente**:
`EXACT_FAST_PATH`. Si la consulta FTS de todos los términos ya devolvió
al menos el número de recuerdos solicitado y hay al menos dos términos, el
experimento permite saltarse algunas consultas de variantes/sinónimos.
La búsqueda y el ranking siguen siendo **los mismos** componentes; el
parámetro de estrategia no altera permisos, persistencia, peso del LLM ni
la normalización de origen.

Este atajo puede perder recuerdos más pertinentes, incluso si encontró
cuatro candidatos: **no presupone que sea mejor**. Debe perder ante el
oráculo independiente si se equivoca.

Se añade `indexReads` a los recibos de `MemorySearchService.Result`:
cuenta las lecturas FTS (incluso intentos fallidos). No sustituye a
latencia, batería, RAM ni veracidad de la respuesta final. Al recuperar
por términos posteriores, suma también esas lecturas. Presupuesto máximo
habitual: ocho lecturas.

## Reutilización del aprendizaje ya existente

`MemoryStrategyExperiment` se construye sobre una instancia de
`VerifiedStrategyPolicy`. Solo acepta los identificadores registrados
`BASELINE` y `EXACT_FAST_PATH`, para contextos `memory:personal` y
`memory:general`.

Las observaciones solo entran si un llamador de ensayo presenta un
`gold` independiente (en memoria; máximo cuatro IDs), una huella de
problema y snapshot de ensayo de 64 caracteres hexadecimales, un resultado
de la búsqueda real y la indicación explícita de verificación externa.
La igualdad exacta entre fuentes recuperadas y gold es deliberadamente
conservadora: candidatos extra no se consideran «verdaderos» porque lo
afirme el propio modelo. No se almacenan preguntas, textos, nombres, IDs
Room, fotografías ni respuestas; la política previa guarda solamente
huellas, nombre de estrategia, resultado y número de consultas.

`candidateForManualReview` puede sugerir una variante cuando la política
ya existente vea >=3 pares sin regresiones y >=10 % menos consultas. La
sugerencia **no se instala ni conecta a producción**. `resetContext`
revoca el historial experimental para volver a la opción base, sin tocar
recuerdos personales. No se añade otra base ni nuevo formato de
sincronización.

## Controles de promoción adicionales, aún no satisfechos

1. Completar las pruebas en el teléfono real sobre un corpus congelado,
   consentido y anónimo. Controlar diferencias de Gemma, Room, E5,
   restauración pCloud, autoría personal, orden temporal y consumo.
2. Etiquetar las fuentes correctas **independientemente** del resultado
   del LLM y de las estrategias, sin subir texto personal a GitHub.
3. Comparar las dos revisiones por `scripts/salve_eval_gate.py`; exigir
   CANDIDATE, cero regresiones de recuperación/citas, cero incidentes,
   p95 bajo control y aprobación humana de un informe detallado.
4. Una eventual adopción real exigirá un PR posterior, revocable, con
   guardas de error/cancelación, rollback y prueba de dispositivo.
   Un `candidateForManualReview` positivo **no** acredita que esos
   controles ya se cumplan.

## Pruebas

`MemoryStrategyExperimentTest` cubre:
- La ruta estándar sigue realizando exactamente las búsquedas existentes
  y no se ve afectada por observaciones del laboratorio.
- La variante limitada usa menos lecturas en ejemplos sintéticos donde
  la primera consulta ya recuperó los cuatro recuerdos correctos.
- Tres pares congelados y correctamente verificados solo producen una
  **candidatura para revisión**, nunca una promoción automática.
- Fallos, gold incorrecto, resultados sin verificación, duplicados de
  un mismo problema y contextos distintos no fabrican mejoras.
- Si no hay suficientes fuentes en la primera consulta, la variante
  continúa el mismo conjunto de búsquedas del servicio original.
- Revertir las observaciones experimentales deja intacta la memoria
  y restaura la preferencia de laboratorio BASELINE.

Se requieren GitHub Actions en Android y controles de memoria; la
prueba aislada no demuestra mejora real del LLM ni de la batería.
