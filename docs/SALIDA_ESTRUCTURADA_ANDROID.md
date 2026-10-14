# Salida estructurada y recuperación de conversación en Android

Las capturas mostraban un error del contrato de salida incluso al saludar o preguntar por Salve. El controlador rechazaba toda respuesta que no fuera el JSON exacto, y repetía la inferencia antes de mostrar un error. El cambio añade restricciones al generador y permite mostrar texto público normal sin reintento. El rechazo de formato no es un diagnóstico de falta de RAM.

## Capa de inferencia

| Motor real | Restricción conectada | Archivo |
|---|---|---|
| Gemma `.litertlm`, LiteRT-LM 0.16.1 | `ConversationConfig(enableResponseFormat = true)` y `ResponseFormat.json(schema)` en **cada** `sendMessageAsync` | `app/src/main/java/salve/core/LiteRTLlm.kt` |
| GGUF, llama.cpp fijado en el submódulo | `llama_sampler_init_grammar` antes de top-k/top-p; `llama_sampler_sample` acepta cada token una vez | `app/src/main/cpp/salve_llama.cpp` y `GgufLlm.java` |
| Importaciones antiguas `.task` / MLC | Sus adaptadores actuales no exponen estas restricciones; utilizan el fallback de conversación y la validación estricta de acciones | `CognitiveReply.java` |

Gemma no ejecuta GBNF: se usa su API real de JSON Schema, ya disponible en la dependencia fijada. No se actualizan las librerías de inferencia. Se mantienen desactivados tanto los canales de razonamiento como la ejecución automática de herramientas nativas.

Las restricciones se activan exclusivamente para prompts creados por el controlador cognitivo. `SalveLLM` transmite esa condición antes de anteponer la identidad del modelo, tanto para texto como para fotos. Investigación y generación de código con sus propios protocolos conservan el formato anterior.

Fuentes de las restricciones, incluidas como recursos de Java en el APK:

- `app/src/main/resources/salve/cognitive_reply.schema.json`: dos alternativas exclusivas. `respond` exige texto y `action:null`; `tool` exige `message:null` y una acción con `name` y objeto `arguments`. No admite campos adicionales en el sobre ni en la acción.
- `app/src/main/resources/salve/cognitive_reply.gbnf`: el mismo sobre, con orden de campos fijo para generar. Incluye strings escapadas, Unicode, arrays y objetos anidados en argumentos.
- `CognitiveOutputConstraints.java`: carga esos recursos de forma acotada. CI compara sus bytes con los incluidos en los APK.

La gramática no concede permisos ni prueba que los argumentos sean correctos. El controlador sigue rechazando herramientas ausentes, campos duplicados, argumentos incorrectos y acciones no autorizadas. Un límite de tokens, una cancelación o un error del motor todavía puede interrumpir la salida; no se da por válida una llamada incompleta.

Referencias de implementación de LiteRT-LM, fijadas a la versión usada:

- [Config.kt v0.16.1](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.16.1/kotlin/java/com/google/ai/edge/litertlm/Config.kt)
- [ResponseFormat.kt v0.16.1](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.16.1/kotlin/java/com/google/ai/edge/litertlm/ResponseFormat.kt)
- [Conversation.kt v0.16.1](https://github.com/google-ai-edge/LiteRT-LM/blob/v0.16.1/kotlin/java/com/google/ai/edge/litertlm/Conversation.kt)

## Capa de parser

`parse` continúa siendo estricto. El chat llama a `parseForConversation`:

1. Si el sobre cumple el contrato, se usa como antes. El texto de `message` permanece inerte aunque contenga ejemplos de comandos.
2. Si la salida es texto público normal, se convierte directamente en una respuesta, conservando el proveedor y sin volver a inferir. Un saludo como el de la captura ya no provoca un error JSON.
3. Se puede recuperar un sobre `respond` completo dentro de una única cerca Markdown. No se recuperan acciones de cercas ni de texto introductorio.
4. Las marcas de herramientas (`mode:tool`, `action` no nula, `arguments`, formatos antiguos y llamadas reconocibles) provocan un único intento de nueva salida. Se reconocen también nombres de campos con escapes Unicode. Sólo un JSON estricto posterior puede llegar al adaptador.
5. Si esa corrección falla, se informa de que la acción no pudo validarse y no se ejecutó. Las respuestas vacías, desmesuradas, con canales privados o sobres rotos no se muestran como texto público.

La detección es conservadora y sintáctica: no pretende adivinar todas las intenciones a partir del lenguaje natural. La barrera efectiva es que el fallback sólo produce texto, nunca acciones.

En LiteRT-LM y GGUF, la corrección usa hasta 256 tokens y 30 segundos (el turno normal conserva 512 tokens y su plazo propio). El flag se lee del `TASK_STATE` del controlador, no de una cadena parecida incluida por el usuario. Se mantienen los límites totales y la cancelación de `CognitiveTurn`.

## Pruebas reproducibles

- `CognitiveFallbackTest` y `CognitiveContractTest`: saludos/casos de las capturas, ausencia de reintentos para texto, llamadas corruptas, validación tras corrección, Markdown, Unicode, canales privados, tamaños, cancelación y presupuesto.
- `StructuredLocalOutputTest`: comprueba los dos puntos de activación de la API **real** de LiteRT-LM 0.16.1, que no se ejecuten herramientas nativas y que la corrección tenga menor presupuesto.
- `scripts/check_cognitive_constraints.py`: 26 casos compartidos contra JSON Schema y el motor nativo de gramática del mismo llama.cpp que usa la app. No descarga pesos ni simula el parser nativo.
- CI: suite completa, compilación Kotlin/Java/JNI, pruebas nativas de gramática, APK normal/prueba ARM64, empaquetado del esquema/gramática, firma y alineación de bibliotecas. También compila los tests de dispositivo.

```sh
cmake -S scripts/cognitive-grammar -B build/cognitive-grammar -DCMAKE_BUILD_TYPE=Release
cmake --build build/cognitive-grammar --target salve-grammar-check --parallel 2
python3 -m pip install 'jsonschema==4.25.1'
python3 scripts/check_cognitive_constraints.py --validator build/cognitive-grammar/salve-grammar-check
```

La prueba `RealInferenceTest.configuredLocalRuntimeHonorsCognitiveSchema` usa el modelo descargado y activado en el dispositivo, con las preguntas «Hola Salve, ¿cómo estás?» y «Me gustaría saber de ti». Valida la salida **cruda**, antes del fallback. Para ejecutarla con un teléfono conectado:

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=salve.core.RealInferenceTest#configuredLocalRuntimeHonorsCognitiveSchema \
  -Pandroid.testInstrumentationRunnerArguments.runRealStructuredLocal=true
```

Las pruebas JVM, de gramática y compilación no sustituyen esta inferencia en el S24 Ultra: no verifican su GPU, latencia ni todas las respuestas del modelo. Esta sesión no tiene acceso al teléfono. Desde el móvil se pueden comprobar las mismas dos preguntas en el APK y una petición de acción, revisando que el saludo salga normalmente y que la acción conserve su confirmación.
