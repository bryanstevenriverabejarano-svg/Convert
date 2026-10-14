# Mente persistente de Salve

Esta capa da a Salve continuidad entre conversaciones: lo que cree, lo que se pregunta y lo que
ha pensado mientras nadie le hablaba. Sustituye a mecanismos que simulaban actividad interna sin
producir conocimiento. Nada de esto demuestra conciencia; son procesos medibles con procedencia.

## Qué cambió y por qué

| Antes | Problema verificado | Ahora |
|---|---|---|
| `SYSTEM_STATE` solo con hora, RAM y acción pendiente | El modelo empezaba cada turno sin saber quién había sido Salve | Campo `SELF_MODEL` en cada turno, con presupuesto propio (≤1.400 caracteres) |
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

## Limitación importante: el modelo de embeddings actual no entiende español

Medido con ONNX Runtime sobre el `gte-small-int8.onnx` incluido y la prueba de calibración:

| Pooling | Acierto (8 paráfrasis) | Margen medio | Coseno paráfrasis | Coseno sin relación |
|---|---|---|---|---|
| Original (incluye PAD) | 38 % | −0,011 | 0,913 | 0,908 |
| Corregido (solo tokens) | 38 % | −0,003 | 0,859 | 0,846 |

gte-small se entrenó en inglés: en español todo se parece a todo. Por eso **la memoria por
significado y la señal de sorpresa quedan desactivadas automáticamente** hasta disponer de un
modelo multilingüe que supere la calibración. No se indexa ni se predice con un modelo que
inyectaría ruido con forma de recuerdo. «Tu mente» muestra el estado de la calibración.

Siguiente paso: incorporar un modelo multilingüe compacto (candidatos a medir con la misma
calibración: `multilingual-e5-small`, `paraphrase-multilingual-MiniLM-L12-v2`). Ambos usan
tokenizador SentencePiece/XLM-R, no el `WordPiece` actual, así que requieren un tokenizador nuevo
o calcular embeddings con llama.cpp (que ya está en la app) usando un GGUF de embeddings.

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
- `SELF_MODEL` ocupa hasta 1.400 caracteres del prompt.

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
