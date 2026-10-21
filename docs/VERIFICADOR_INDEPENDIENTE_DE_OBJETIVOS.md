# Verificador de objetivos de Salve — etapa 3

## Qué se verifica

La revisión del propio modelo y los recibos de las herramientas son dos señales
**diferentes**. Salve ahora registra ambas sin equipararlas:

- `AgentRun.status`: estado de ejecución y revisión del plan existente,
  por ejemplo `SUCCEEDED` cuando el modelo acepta una respuesta con recibos.
  No representa por sí solo verdad externa.
- `AgentRun.goalVerification`: comprobación determinista independiente:
  `VERIFIED`, `PARTIAL`, `NOT_VERIFIED` o `UNKNOWN`.
- `verificationCriterion` y `verificationEvidence`: código de criterio y
  identificador del recibo; no incluyen datos privados ni texto de las páginas.

## Primer criterio habilitado: PUBLIC_READ

Para frases muy delimitadas del usuario como `Lee https://example.org/manual`,
`Abre https://example.org/manual` o `Consulta https://example.org/manual`,
`AgentGoalVerifier` comprueba que:

1. El objetivo original **solo solicita la lectura de esa URL HTTPS exacta**.
   Una petición de comparar, investigar, resumir o ejecutar código NO cumple
   este contrato y permanece `UNKNOWN`.
2. `AgentPolicy` autoriza ese destino de red pública.
3. El diario contiene un recibo de `web.read` con la huella SHA-256 de la
   invocación original, calculada por el controlador, no por el modelo.
4. La lectura se completó sin truncado (`SUCCESS`), y la lista de fuentes
   del recibo incluye exactamente la misma URL.
5. La respuesta cita el identificador real de ese recibo.

Solo con esos cinco requisitos el estado se convierte en `VERIFIED`. Si
la lectura quedó truncada, es `PARTIAL`; una lectura bloqueada, una URL
diferente, una cita fabricada o un fallo son `NOT_VERIFIED`. Objetivos
semánticos amplios o no reconocidos permanecen `UNKNOWN`.

**Este criterio certifica que se leyó una página concreta.** No demuestra
que sus afirmaciones sean verdaderas ni que Salve haya entendido correctamente
todo el contenido. No permite ningún acceso nuevo.

## Implementación

`AgentEngine.finish` calcula el estado antes de la transacción Room final.
`AgentRun` lo persiste con validación de valores y sigue leyendo diarios
antiguos. `AgentRuntime` muestra el estado separado tanto en la lista
como en el detalle del plan.

`AgentExperience.contextFor` añade `objetivos_verificados` únicamente si
el estado guardado dice `VERIFIED` **y** un nuevo cálculo determinista lo
confirma. Los contadores históricos de herramientas no se convierten en
objetivos verificados. `AgentExperience.valid` sigue aceptando los documentos
anteriores de tres campos para no perder recuerdos operativos.

Las pruebas de JVM cubren objetivo reconocido, contenido no verificado,
procedencia, huellas, cita, privacidad, códigos antiguos y rechazos. Una
prueba Room recorre selección de herramienta → lectura → respuesta con cita →
revisión → persistencia y reapertura de la base.

## Límites actuales y próximas pruebas

- La mayoría de los objetivos siguen en `UNKNOWN`; no se les fabrica una
  puntuación de éxito. Tampoco se aprenden pesos neuronales.
- Ampliar criterios exige un esquema explícito del usuario, controles de
  procedencia y *holdouts* no usados durante el aprendizaje.
- Medir falsos positivos, falsos negativos y tareas desconocidas en el móvil.
- Los campos de verificación no conceden permisos ni cambian las herramientas.
