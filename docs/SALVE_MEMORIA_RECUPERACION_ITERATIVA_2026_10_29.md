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


## Continuación: medición de memoria sin crear un evaluador duplicado

El PR siguiente **amplía** `scripts/salve_eval_gate.py` (que ya compara
versiones en cinco dominios). No añade otra base, motor de búsqueda,
modelo, sincronizador o un segundo criterio de aprobación. La nueva
propiedad opcional `memory_evidence` por caso de dominio `memory`
permite medir calidad de la recuperación sin enviar texto de memorias.

### Formato de evidencias

En cada una de las dos revisiones del mismo conjunto congelado, los casos
de memoria pueden llevar un objeto adicional:

```json
"memory_evidence": {
  "gold": ["00000000000003e8"],
  "retrieved": ["00000000000003e8"],
  "cited": ["00000000000003e8"],
  "conflict": false,
  "acknowledged": false,
  "status": "OK"
}
```

- `gold`: IDs opacos **etiquetados por un evaluador independiente**, máximo 16.
- `retrieved`: IDs opacos de las fuentes realmente devueltas, **en orden**,
  máximo cuatro, provenientes del flujo actual de búsqueda (sin simular Gemma).
- `cited`: IDs opacos de las evidencias que la respuesta realmente citó,
  máximo cuatro. Citar algo no recuperado o ajeno al gold cuenta como error.
- `conflict`: el caso contiene evidencia contradictoria identificada antes
  del experimento; `acknowledged` indica si la respuesta lo reconoció.
  No se deduce de la opinión del propio LLM.
- `status`: `OK`, `EMPTY`, `PARTIAL` o `ERROR`. Una lectura no
  completa no puede aportar fuentes confiables a esta métrica.

Los IDs válidos son **solo hexadecimales de 16–64 caracteres**, no nombres,
preguntas, mensajes ni textos privados. Se aconseja usar un HMAC con clave
secreta local y un identificador lógico estable entre las dos versiones;
no incluir la clave en el recibo ni subir el historial personal a GitHub.
Estos IDs son de evaluación, **no** cambian los IDs de Room ni solucionan la
sincronización entre dispositivos.

### Criterios añadidos al comparador ya existente

- `recall_at_1` y `recall_at_4` sobre preguntas con `gold` etiquetado
  (media de cobertura por pregunta). Se informa el número de preguntas
  etiquetadas; no se asegura una mejora por un porcentaje aislado.
- Bloqueo de candidato si pierde cobertura en cualquier pregunta ya medida,
  si cita fuentes fuera del gold o de los resultados disponibles, si responde
  sobre recuerdos inexistentes según el gold, o si omite contradicciones
  recuperadas que debían señalarse.
- Se conserva el conjunto emparejado, SHA del modelo idéntico, revisiones
  inmutables, protección de privacidad, no regresión de casos que pasaban,
  presupuesto de latencia y revisión humana previa a cualquier despliegue.
- Compatibilidad: los recibos anteriores sin `memory_evidence` mantienen
  su comportamiento. Si un experimento adopta esta métrica, debe aportarla
  para **todos** los casos de memoria de ambas revisiones; no se puede elegir
  solo los ejemplos favorables. Gold y etiquetas de contradicción son fijos
  para ambas revisiones.

### Cómo evaluar de verdad en el Galaxy S24 Ultra

1. Congelar 30+ casos con al menos tres de cada dominio del evaluador;
   incluir preguntas de memoria positiva, vacía, contradicción, temporalidad,
   autoría y restauración cloud incompleta.
2. Etiquetar por separado qué recuerdos son respuestas correctas; asignar
   IDs HMAC anónimos. Nunca subir a CI el texto de recuerdos personales.
3. Ejecutar **la misma batería** en dos revisiones de Salve, con exactamente
   los mismos pesos Gemma y las mismas políticas de permisos.
4. Obtener los IDs de `MemorySearchService` / `ConversationMemoryGrounding`
   y contrastar las fuentes citadas en la respuesta con etiquetas humanas.
   Registrar duración y fallos reales, no éxitos inventados.
5. Ejecutar `python3 scripts/salve_eval_gate.py --baseline base.json
   --candidate candidato.json --report decision.json`. Revisar manualmente
   el resultado antes de fusionar o instalar.

**No se ha generado aquí ninguna medición real del teléfono**, ni se
activa automáticamente el registro de recuerdos en la nube. Es una fase
de validación reproducible y privada para decidir de forma responsable
si ampliar después la recuperación o modificar el planificador.

## PR #297 — Observación real de recuperación en Android, sin replicar motores

### Revisión previa de código existente
- **Se conserva el motor único:** `MemoriaEmocional.recuperarContextoConversacional`
  espera escrituras, usa `ConversationMemoryGrounding` / Room FTS / E5,
  y consulta el estado de recuperación de pCloud. No se añade otro DAO,
  buscador, sincronizador ni cambio de modelo.
- **Se conserva la evaluación general:** `ConversationOutcomeAudit` ya observa
  el estado de respuestas, y `scripts/salve_eval_gate.py` compara revisiones.
  El nuevo `MemoryEvaluationProbe` es únicamente el adaptador de métricas
  de fuentes de memoria realmente emitidas, sin emitir veredictos.
- **Se conserva la fuente:** el `Result` ya tenía estado y contexto. Ahora
  mantiene internamente los IDs de episodios Room que **sí caben** en el
  contexto, excluyendo candidatos recortados, nodos y relaciones del grafo.
  `withCloudStatus` conserva esos IDs, pero si el resultado es `PARTIAL`
  no se exportan como evidencia completa.

### Activación explícita y privacidad
`MemoriaEmocional.evaluarRecuperacionLocal(consulta, claveLocal)` ejecuta
la MISMA recuperación que usa la conversación. Requiere acceso privado
verificado mediante `IdentityManager` y una clave HMAC local de 32+ bytes;
esta función no se invoca automáticamente en conversaciones normales.

El adaptador `MemoryEvaluationProbe.capture` solo devuelve:
`status` (`OK`, `EMPTY`, `PARTIAL`, `ERROR`),
`duration_ms` e identificadores hexadecimales de 32 caracteres obtenidos
con HMAC-SHA256 sobre el ID de un episodio Room. No incluye consultas,
respuestas, memoria textual, identificación personal, GPS, claves ni
referencias al grafo. La clave no se guarda ni se envía. Para comparar dos
revisiones del MISMO conjunto congelado debe usarse la misma clave aleatoria
guardada de forma segura en el dispositivo; una clave nueva produce códigos
distintos. Los IDs Room no son globalmente estables entre dispositivos.

No se genera ningún `gold` ni `cited` ficticio: la observación tiene
`human_verified=false`. El etiquetado independiente de los recuerdos
correctos, citas realmente expresadas por Gemma y contradicciones es un paso
de evaluación separado y obligatorio antes de alimentar el comparador #296.
No se acepta una respuesta autodeclarada como fuente verificada.

### Prueba en Android con base aislada
La prueba `MemoryEvidenceDeviceTest`, inspirada en las pruebas de
instrumentación opt-in ya existentes (`MindContinuityRealTest`), crea una
base Room **en memoria** con frases sintéticas, ejecuta la recuperación
FTS real y mide latencia y estado de hallazgo/ausencia. No abre
`memoria.db` de producción ni usa recuerdos del dueño. No prueba
los pesos Gemma, la respuesta final ni el comportamiento de E5. Se ejecuta
en un dispositivo o emulador Android conectado:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.runMemoryEvidence=true \
  -Pandroid.testInstrumentationRunnerArguments.class=salve.core.MemoryEvidenceDeviceTest
```

El test Java de JVM verifica también que la sonda no filtra texto,
que la nube `PARTIAL` no es `OK`, que grafo solo no equivale a episodio
y que una clave HMAC corta se rechaza.

### Qué falta para afirmar una mejora de inteligencia
1. Crear un conjunto de casos congelados **con consentimiento**, etiquetar
   las respuestas correctas sin publicarlas y conservar una clave de
   pseudonimización secreta y estable para los dos ensayos.
2. Ejecutar ambas versiones en el Galaxy S24 Ultra con los mismos pesos
   Gemma; capturar fuentes desde el controlador y las citas presentes en
   la respuesta final, sin inventar verificación por suposición del LLM.
3. Adjuntar `gold`, `cited`, `conflict`, `acknowledged` y `passed`
   independientemente para producir recibos estrictamente compatibles
   con #296. Mantener los textos privados fuera de CI.
4. Comparar resultados y p95; solo entonces considerar adaptaciones de
   búsqueda. Ninguna observación permite que el modelo autoedite políticas,
   acceda a memorias sin autenticar o fusionar un PR por sí mismo.

**Límite explícito:** esta fase sí conecta una medición con el código Android
real, pero NO demuestra aún recall humano ni veracidad de respuestas de Gemma.
La prueba instrumental de base sintética no sustituye la validación completa
en el dispositivo físico.
