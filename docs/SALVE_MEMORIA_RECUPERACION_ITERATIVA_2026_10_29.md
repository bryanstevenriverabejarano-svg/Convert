# Salve — Memoria: recuperación iterativa y límites verificables

## Problema observado

La memoria de Salve ya combina FTS, expansión léxica, E5, relevancia emocional,
fecha, autoría y grafo. Sin embargo, `MemorySearchQuery` solo conserva los
**primeros cuatro temas** de una petición. En preguntas largas, la conversación
introductoria podía agotar esos cuatro temas antes de mencionar el asunto
concreto al final.

La solución no es consultar toda la base indiscriminadamente ni incrementar
a ciegas los tokens del LLM.

## Cambio implementado (etapa siguiente a PR #290)

1. `MemorySearchService.searchWithRecovery` ejecuta la búsqueda existente
   sin cambiarla. Si obtiene evidencia válida, termina **sin más lecturas**.
2. Solo si no aparecen registros y la pregunta tenía cuatro términos originales,
   `MemorySearchQuery.tailFocus` extrae **uno o dos términos posteriores**
   que no fueron usados en la primera búsqueda.
3. Se hacen como máximo **dos consultas FTS adicionales**, con cada palabra
   sanitizada y entrecomillada por el parser existente. El total de búsquedas
   por petición permanece bajo `MAX_SEARCHES=8` (6 + 2).
4. Se mantienen el filtro de autoría personal, los máximos de cuatro
   recuerdos entregados, la preferencia temporal y la puntuación de evidencia.
   El LLM no puede convertir investigación de Internet en recuerdos personales.
5. `ConversationMemoryGrounding` etiqueta lo encontrado como
   `candidato_segunda_busqueda`. Su contexto advierte que el hallazgo puede
   ser una coincidencia superficial y exige comprobar la pregunta completa,
   personas, fechas y negaciones antes de concluir.

**No** se amplían automáticamente permisos, se recuperan recuerdos borrados,
se consulta nube externa ni se crean memorias a partir de candidatos. La
segunda pasada no recupera una historia de pCloud si esa restauración no está
completa. El límite de contexto de 3.200 caracteres sigue intacto.

## Pruebas

- Pregunta larga con «proyecto Orion» al final, después de cuatro términos
  introductorios: recupera el candidato si hay coincidencia guardada.
- Preguntas cortas o con evidencia inicial: **sin segunda pasada**.
- Registro externo «Orion» y declaración del usuario «Orion»: consulta personal
  muestra únicamente la declaración personal.
- Todas las lecturas fallan: se conserva estado de error, no se inventa
  recuerdo y las consultas siguen acotadas.
- Memoria vacía y saludo: ninguna búsqueda completa ni lectura de tabla.
- El contexto de conversación etiqueta la segunda pasada y conserva el
  presupuesto de caracteres y el origen.

## Etapas todavía pendientes

**No** es aún memoria jerárquica Hopfield/Hope ni recuperación multihop
general con LLM. La siguiente evolución requiere pruebas en teléfono de:
`recall@k` sobre recuerdos reales anonimizados, resolución de contradicciones,
referencias temporales, coherencia en diálogos largos y protección contra
contenidos web maliciosos. Se necesitará también medir latencia, uso de RAM y
coste energético con Gemma cargado, antes de ampliar la búsqueda a varias
rondas o generar planes de consulta mediante el modelo.

Los resultados de CI verifican contratos Java y Android, pero no demuestran
por sí solos una mejora general de inteligencia ni ejecución física en el S24
Ultra.
