# Mente persistente de Salve

> **Actualización de estado (21-10-2026):** esta guía explica la arquitectura de
> continuidad. Su diagnóstico original de `gte-small` es **histórico**. Para el
> contrato actual de memoria semántica, descarga, pruebas y límites consulta
> [H2: memoria multilingüe](H2_MEMORIA_MULTILINGUE.md) y
> [estado actual del proyecto](ESTADO_ACTUAL.md).

Esta capa da a Salve continuidad entre conversaciones: lo que cree, lo que se pregunta y lo que
ha pensado mientras nadie le hablaba. Sustituye a mecanismos que simulaban actividad interna sin
producir conocimiento. Nada de esto demuestra conciencia; son procesos medibles con procedencia.

## Qué cambió y por qué

| Antes | Problema verificado | Ahora |
|---|---|---|
| `SYSTEM_STATE` solo con hora, RAM y acción pendiente | El modelo empezaba cada turno sin saber quién había sido Salve | Campo `SELF_MODEL` en cada turno, con presupuesto propio (hasta 1.700 caracteres, sujeto al presupuesto del turno) |
| Reflexión solo si Bryan escribía notas, cada 6 h y con aprobación | Sin notas no había ningún pensamiento propio | `ReflectionCycle`: piensa sobre lo vivido y sus preguntas abiertas; interno, sin aprobación, cada ≥1 h con el móvil cargando |
| `generarReflexionesSilenciosas` | Copiaba recuerdos al azar como "reflexión" con certeza aleatoria | Eliminado |
| `conectarRecuerdosSimilares` | Enlazaba recuerdos por la primera palabra y una emoción que siempre era «no evaluada» | Eliminado; las conexiones son creencias con fuentes |
| Sueño solo tras reconocimiento facial | El mantenimiento de memoria dependía de una foto | `DreamWorker` diario (cargando y en reposo) y orden «sueña ahora» |
| `CognitiveCore`/`LiquidNeuralLayer` en el sueño | Vectores aleatorios por frase completa; su estado no influía en ninguna respuesta | Retirados del recorrido; código conservado en `salve.core.cognitive` como laboratorio |
| `integrarExperiencia(..., "empatia")` en cada turno | +0,007 por mensaje: «empatía» llegaba a 1,0 en ~72 mensajes | Se cuenta la experiencia, no se inflan rasgos |
| Narrativa por defecto «Soy Salve, un asistente de IA…» | Contradecía el objetivo `identidad` | Narrativa abierta, igual en `IdentidadNucleo` y `SelfModel` |
| `EmbeddingsIndex` sin `token_type_ids` | gte-small exige esa entrada: **ningún embedding se calculaba** (comprobado con ONNX Runtime) | Entrada añadida y media solo sobre tokens reales |
| Clases sin uso (`DecisionAutonoma`, `CicloConciencia`, `AprendizajeAutonomo`, `BucleCognitivoAutonomo`, `InstintoSupervivencia`, …) | Inalcanzables desde el manifest | Eliminadas |

## Componentes (`salve.core.mind`)

- **`SelfModelJournal`**: narrativa, creencias (`bryan`/`salve`/`mundo`) con fuentes y confianza,
  preguntas abiertas, pensamientos y métricas. Escritura atómica en `noBackupFilesDir/mind/`.
  Reglas fijas:
  - sin fuente no hay creencia; la confianza máxima crece con el número de fuentes (0,6 → 0,9);
  - una contradicción resta 0,25; por debajo de 0,15 la creencia se retira con registro;
  - creencias sobre Salve y pensamientos pasan `FunctionalIdentityPolicy`;
  - **la narrativa central nunca cambia sola**: queda pendiente hasta «acepta tu nueva identidad»;
  - un archivo ilegible bloquea las escrituras en vez de empezar de cero;
  - al borrar recuerdos se retiran las creencias y pensamientos que dependían solo de ellos.
- **`DreamCycle`**: consolida los recuerdos nuevos desde el último sueño. Toda fuente citada se
  comprueba contra los episodios dados; el modelo no puede inventar evidencia. Una salida inválida
  se reintenta una vez y no avanza el cursor.
- **`ReflectionCycle`**: un pensamiento interno con fuentes y, si procede, una pregunta nueva o la
  resolución de la pregunta en foco.
- **`PredictionLoop`**: tras responder (solo texto, no voz), Salve anticipa la siguiente
  intervención. Al llegar la real, la distancia de significado es la sorpresa. Solo las sorpresas
  atípicas respecto a su propia historia (media + 1,5 desviaciones) abren una pregunta.
- **`EmbeddingCalibration`**: prueba fija de 8 paráfrasis en español que decide si el modelo de
  embeddings sirve y calibra sus umbrales.
- **Memoria híbrida**: `ConversationMemoryGrounding` fusiona por rango recíproco (RRF) la búsqueda
  por palabras (FTS) y la búsqueda por significado (`MemoryVectorStore`, vectores int8). Los
  registros recuperados solo por significado se marcan como tales en el prompt.

## Memoria semántica: modelo actual y diagnóstico histórico

**Situación del código de `main` en octubre de 2026:** el contrato de H2 usa
`multilingual-e5-small INT8` (384 dimensiones) y tokenización
**SentencePiece 0.2.1**, con prefijos `query: ` y `passage: `. El modelo ONNX
de 118.346.824 bytes y el tokenizador de 5.069.051 bytes se descargan fuera de
la APK, verificando tamaño y SHA-256. La búsqueda léxica sigue disponible sin
esos artefactos; **la semántica no se activa por el solo hecho de existir código**:
hace falta preparar los archivos, superar calibración y disponer de RAM en el dispositivo.

La evaluación H2 en host, sobre 32 pares congelados, dio **26/32 en primera
posición** para E5 INT8 oficial. Es un ensayo de recuperación sintética, no
una puntuación de respuestas finales, y no certifica rendimiento en el S24 Ultra.
La ejecución real de este ONNX/SentencePiece en Android ARM64, su memoria y
coexistencia con el LLM requieren prueba en teléfono. Consulta
[los resultados, errores y pasos de verificación](H2_MEMORIA_MULTILINGUE.md).

**Diagnóstico anterior (histórico, no estado actual):** el viejo `gte-small`
obtuvo 38 % de aciertos en 8 paráfrasis españolas, tanto con el pooling original
como después de corregirlo. Aquella limitación llevó a desactivar recuperación
semántica con ese modelo y motivó H2. Las indicaciones anteriores de «incorporar
un modelo multilingüe» o «escribir un tokenizador nuevo» quedaron superadas por
el adaptador SentencePiece ya implementado; no se deben ejecutar como tareas
pendientes.

## Órdenes

| Frase | Efecto |
|---|---|
| «tu mente», «¿en qué has pensado?», «¿qué crees de ti?» | Muestra narrativa, creencias, preguntas, pensamientos, métricas y estado de embeddings |
| «acepta tu nueva identidad» / «rechaza tu nueva identidad» | Decide sobre la narrativa propuesta en el sueño |
| «pausa tu mente» / «reanuda tu mente» | Detiene o reanuda reflexión, sueño y predicción |
| «sueña ahora» | Programa un sueño (requiere carga) |
| «exporta tu mente» | Escribe `salve-knowledge.jsonl` y `salve-episodes.jsonl` en `Android/data/<paquete>/files/exports` |

## Coste y recursos

- Sueño: una inferencia del modelo local (más un reintento si falla), diaria, cargando y en reposo.
- Reflexión: una inferencia cada ≥1 h, solo cargando, cuando hay experiencia nueva.
- Predicción: una inferencia breve tras cada respuesta de texto, solo con embeddings calibrados,
  sin turno activo y sin memoria baja. Si Bryan escribe mientras se genera, su turno espera a que
  termine (el motor local atiende una petición a la vez).
- `SELF_MODEL` se solicita con límite de 1.700 caracteres, pero puede recibir menos por el presupuesto total de inferencia y el historial.

## Adaptador LoRA personal (la experiencia cambia pesos)

1. En el móvil: «exporta tu mente». Copiar los dos `.jsonl` al PC.
2. `python scripts/mind_lora/prepare_dataset.py --export-dir <carpeta> --output train.jsonl`
3. En un PC con GPU: `python scripts/mind_lora/train_lora.py --base <modelo HF exacto del GGUF> --data train.jsonl --output adaptador/`
4. `python third_party/llama.cpp/convert_lora_to_gguf.py --base <modelo HF> --outfile salve-lora.gguf adaptador/`
5. `python scripts/mind_lora/make_manifest.py --adapter salve-lora.gguf --base-filename <nombre exacto del GGUF del móvil>`
6. Copiar `salve-lora.gguf` y `salve-lora.json` a `Android/data/<paquete>/files/adapters/`.

La app solo aplica el adaptador a modelos GGUF (llama.cpp), si el nombre del modelo base coincide
y el SHA-256 es correcto. Antes de usarlo a diario conviene comparar con
`MindContinuityRealTest` y conversaciones reales si mejora o degrada. El entrenamiento no se ha
ejecutado en esta revisión: el script prepara datos y llama a PEFT/TRL, pero no se ha probado con GPU.

## Evaluación

- JVM (`./gradlew :app:testDebugUnitTest`): `salve.core.mind.*`, `SemanticMemoryTest`,
  `CognitivePromptSelfModelTest`. `MindContinuityTest` comprueba la tubería de extremo a extremo con
  un modelo guionizado: lo consolidado el día 1 aparece con su fuente en el prompt del día 8.
  Prueba la plomería, no la inteligencia.
- Dispositivo, con el modelo real (opcional):

  ```bash
  ./gradlew :app:connectedDebugAndroidTest \
    -Pandroid.testInstrumentationRunnerArguments.runRealMind=true \
    -Pandroid.testInstrumentationRunnerArguments.class=salve.core.MindContinuityRealTest
  ```

  Afirma invariantes (fuentes reales, sin identidad inyectada) y registra en logcat
  (`Salve/MindEval`) latencias, creencias y la calibración de embeddings para comparar versiones.
