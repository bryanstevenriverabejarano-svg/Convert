# Auditoría funcional de Salve — 2026-09-21

Base inspeccionada: `87a9e5204a7d51725c6516438dd5d22bec40f1a7` de `main`.
Este documento sustituye el diagnóstico anterior: tener una clase o una dependencia no demuestra que la función esté operativa. Se distingue entre código conectado, prototipos y pruebas realizadas. No se ha ejecutado el APK en un teléfono durante esta revisión.

## Diagnóstico basado en el código

| Área | Evidencia en el repositorio | Estado antes de este cambio |
|---|---|---|
| Conversación | `MotorConversacional`, `conversation/ConversationSession`, `IntentRecognizer` | Hay cola de turnos, historial y contexto. Tras Gemini se intenta `CognitiveCore.verbalize` antes del LLM conversacional. `Verbalizer` puede devolver plantillas y restringe al LLM a verbalizar estados experimentales, sin el historial conversacional completo. |
| LLM local MLC | `SalveLLM`, `BasicLocalLlm`, `mlc4j`, `mlc-package-config.json` | Existe intención de inferencia nativa y un manifiesto de empaquetado. No hay bibliotecas `.so` versionadas; `fetchTvmRuntime` es opcional y no lo exige `preBuild`. Descargar pesos no proporciona el runtime y la biblioteca compilada necesarios. |
| Puente MLC | `mlc4j/.../JSONFFIEngine.java`, `org/apache/tvm` | El puente busca nombres de funciones globales por coincidencias y trata el retorno de chat como respuesta. El upstream inspeccionado crea un módulo con `mlc.json_ffi.CreateJSONFFIEngine`, obtiene funciones del módulo y registra un callback nativo. La integración local necesita una versión y un ABI compatibles, compilación y prueba real. |
| Modelos descargables | `assets/config/models.json`, `ModelDownloader`, `ModelDownloadWorker` | El catálogo apunta a Qwen2.5-0.5B MLC. Hay descarga y comprobaciones de archivos, pero eso no verifica inferencia. El escáner de MainActivity acepta GGUF aunque SalveLLM no tenga ejecutor GGUF; `checkModelsAndNotify` no tiene llamadas. |
| MediaPipe local | `LiteRTLlm.kt`, dependencia `tasks-genai` | Llama realmente a `LlmInference.createFromOptions` y `generateResponse`. Requiere un `.task`/`.litertlm` compatible y recursos suficientes. No viene incluido un modelo generativo listo para esta ruta. El límite actual de 1024 tokens incluye entrada y salida. |
| Estado del modelo | `BasicLocalLlm.init`, `LiteRTLlm.init`, `SalveLLM.initEngineIfNeeded` | Los wrappers ocultaban fallos; el contenedor marcaba el motor inicializado incondicionalmente. Algunos errores de generación eran texto aparentemente exitoso. MainActivity afirmaba usar Gemma-3 sin comprobarlo. |
| Gemini | `GeminiService` | Había llamadas reales, una clave en preferencias y un modelo fijo `gemini-1.5-flash`, con SDK Android antiguo. No se encontró una llamada a `setApiKey` desde la interfaz. Configurar una clave tampoco comprueba conectividad o cuota. |
| Entrada de voz | `MainActivity.iniciarEscuchaContinua` | Usa `SpeechRecognizer` real. El nombre es engañoso: reconoce un turno, no mantiene una sesión continua. Se iniciaba antes de inicializar vistas si ya existían permisos, y faltaba visibilidad del servicio en el manifest. La ejecución puede depender del servicio de voz instalado y de red. |
| Salida de voz | `MotorConversacional`, `TTSManager`, `SalveAccessibilityService` | Usan TTS real de Android. Hay varias instancias, sin coordinación global. En el chat no se comprobaba la disponibilidad del idioma ni se detenía la salida al abrir el micrófono. |
| Cámara | `VideoAnalysisManager.startAnalysis` y MainActivity | Hay implementación CameraX, pero no se encontró ningún llamador de `startAnalysis`. La cámara continua está expresamente desactivada; el antiguo resultado de foto solo atendía una comprobación facial. El chat pedía frames a un buffer sin productor. |
| Reconocimiento facial | `ReconocimientoFacial`, `ADNVisual` | ML Kit detecta rostros, pero el código acepta el primero y compara una codificación propia de landmarks. Esto no acredita una identidad ni equivale a autenticación biométrica. |
| Memoria de conversación | `conversation/ConversationSession` | Cola en RAM de 16 mensajes. No es persistente ni tiene presupuesto por tokens; las conversaciones largas necesitan resumen y selección del contexto. |
| Memoria duradera | `MemoriaEmocional`, Room/DAO, `memory/*`, grafo | Existe persistencia y políticas selectivas de perfil y olvido. Conviven varios almacenes: hay que verificar consistencia y eliminación entre ellos. |
| Memoria visual | `cognitive/HipocampoSemantico` | La colección está en RAM; cargar/guardar en base de datos conserva TODO. No acredita recuerdos visuales entre reinicios. |
| Embeddings | `EmbeddingsIndex`, ONNX Runtime y asset `gte-small-int8.onnx` | Existe un archivo binario de unos 69 MB y código de inferencia ONNX. Es un modelo de embeddings, no un LLM conversacional. Su carga no se ha probado en dispositivo aquí. |
| Emoción e identidad | `DetectorEmociones`, `IdentidadNucleo`, `ConsciousnessState`, `identity/*` | El detector devuelve siempre `neutro` y su asset no está presente. La identidad persistente es estado funcional simulado; los nombres de clases y niveles no demuestran consciencia ni aprendizaje neuronal. |
| Código experimental | `LocalLlmEngine`, `FallbackEngine`, `Verbalizer`, `LaboratorioSimulado` | `LocalLlmEngine` es un stub sin llamadas encontradas desde el resto del código. Las plantillas de `Verbalizer` sí estaban en la ruta del chat. Un experimento narrado por un modelo no es un experimento ejecutado. |
| Mejora autónoma | `AutoImprovementManager`, outbox y `scripts/auto_improvement_*` | Hay infraestructura de propuestas, aislamiento y PR. No demuestra que los motores de conversación, voz o visión funcionen. No se amplía en este cambio. |

## Problemas prioritarios por categoría

- **Conversación:** plantillas y atajos por palabras como «Bryan», «estado» o «esto es» desplazaban consultas normales. Faltaba informar de que no había un LLM operativo.
- **Razonamiento:** se presentaban hipótesis y estados de módulos experimentales como conclusiones. Hace falta evaluación por tareas, evidencia de herramientas y resultados tipados; no cadenas de pensamiento expuestas.
- **Memoria:** límite por mensajes sin presupuesto de tokens; memoria visual volátil; distintos sistemas de perfil, grafo, diario y sincronización requieren reglas comunes de actualización y olvido.
- **Voz:** arranque prematuro, ausencia de estado compartido para todos los TTS, sin wake word ni VAD propio ni generación/síntesis incremental. No hay interrupción integral de inferencia y audio.
- **Arquitectura:** MainActivity y MotorConversacional concentran muchas responsabilidades; existen varios caminos de generación, detección de modelos y síntesis. Conviene consolidar después de verificar el recorrido básico.
- **Rendimiento:** carga nativa/embeddings, prompts extensos y trabajo autónomo pueden competir con la conversación. No se dispone de mediciones en teléfono. MLC puede esperar un stream sin final; requiere cancelación/timeout con liberación correcta del runtime.
- **Seguridad:** un modelo no verifica identidad. La clave de Gemini es personal y permanece en preferencias privadas de la app; para distribuir un APK con credenciales compartidas hace falta un backend o Firebase AI Logic. Los permisos, versiones y revisión de cambios autónomos siguen siendo necesarios.
- **Experiencia de usuario:** detectar un archivo no significa usarlo. Había estados y mensajes de capacidad sin evidencia, y controles ausentes para conectar un proveedor y analizar fotos.

## Arquitectura elegida y alternativas

Se conserva la aplicación y sus almacenes. El chat utiliza una ruta con resultados tipados. Gemini configurado se intenta primero, manteniendo el orden de la aplicación; si falla, una consulta de texto puede usar el adaptador local. Las fotos requieren el proveedor multimodal y no se degradan a un modelo que no recibió imagen.

```mermaid
flowchart TD
    V["Micrófono · SpeechRecognizer"] --> C["Turno y contexto"]
    U["Texto del usuario"] --> C
    C --> R["Enrutador de inferencia"]
    R --> G["Gemini · texto e imagen"]
    R --> L["MLC o MediaPipe · texto"]
    F["Foto solicitada por el usuario"] --> G
    G --> Q["Resultado o error tipado"]
    L --> Q
    Q --> S["Pantalla y TTS"]
```

| Alternativa | Ventaja | Coste o limitación | Decisión |
|---|---|---|---|
| Gemini mediante REST y dependencias existentes | Texto e imagen con un servicio de inferencia; modelo configurable | Clave, red, cuota y procesamiento externo | Conectar esta vía y permitir probarla desde la app. |
| MediaPipe local existente | Inferencia en dispositivo, sin clave cloud para texto | Modelo compatible, RAM y contexto limitado; API en mantenimiento | Conservar y añadir selección explícita y fallos verificables. Evaluar migración a LiteRT-LM por separado. |
| Reparar MLC completo | Ejecución local con catálogo propio | Empaquetar runtime/bibliotecas y alinear puente Java/JNI con upstream | Trabajo posterior con un único modelo/dispositivo de referencia. No simular éxito mientras falten artefactos. |
| Más capas de verbalización experimental | Permiten investigar estados internos | No sustituyen un modelo entrenado ni la validación de sus resultados | Sacarlas de la respuesta conversacional principal. |

La identidad sigue siendo configuración, memoria y estado funcional. La adaptación futura debe basarse en evaluaciones, versiones de configuración y rollback, sin inferir consciencia a partir de esos mecanismos.

## Cambios de este grupo

1. `MotorConversacional` usa `ConversationModelRouter` y adaptadores de inferencia. Se retira `CognitiveCore/Verbalizer` de esa cascada, la falsa detección emocional y los atajos amplios de identidad/estado. Sin modelo, muestra el fallo; las acciones deterministas conservan su resultado.
2. `SalveLLM`, `BasicLocalLlm` y `LiteRTLlm` propagan fallos y comprueban inicialización. La generación tipada deja de interpretar mensajes de error como texto. La API antigua `generate` conserva cadenas de error para no romper sus consumidores antiguos. La carga nativa inicial se difiere al trabajador; recarga y generación local se serializan. No se inventa `libpenguin.so` ni se selecciona otra biblioteca al azar.
3. `GeminiService` usa REST `generateContent` con OkHttp/Gson existentes y `GeminiProtocol`. El modelo predeterminado es configurable; se usa `gemini-2.5-flash` como valor inicial. Se eliminó la dependencia del SDK Android antiguo. La clave va en cabecera, sin logs; no se siguen redirecciones ni reintentos automáticos. La llamada tiene límite de 45 s. HTTP fallido, bloqueo, truncamiento, JSON inválido y ausencia de texto son errores. Las partes marcadas `thought` no se muestran.
4. MainActivity ofrece **IA y cámara**: configurar/desactivar Gemini, probarlo, seleccionar un archivo local compatible, probar el modelo local y tomar una foto para preguntar. La prueba solicita inferencia real y muestra resultado/latencia o fallo; no marca como verificada una clave guardada. El informe verbal de modelo ya no afirma usar Gemma-3.
5. La consulta visual usa `TakePicturePreview`, el prompt y el historial de texto, y envía los píxeles JPEG al proveedor. Es una foto de resolución reducida, no vídeo, OCR garantizado ni percepción continua. No se adjuntan frames ambientales a todos los turnos. La foto se libera después y no se persiste automáticamente en memoria visual.
6. Voz: permisos bajo demanda, comprobación del servicio, manifest con visibilidad de reconocimiento/TTS/cámara, callbacks de sesiones antiguas ignorados y limpieza al pausar. Se retiran los umbrales agresivos de silencio. El TTS del chat comprueba español y se detiene al empezar a escuchar; otros motores de voz todavía deben unificarse. El botón de frase de prueba se identifica como **Probar voz**.
7. Se excluyen las preferencias con la clave de las copias de seguridad y transferencias Android.

## Tareas y comprobación

| Prioridad / estado | Problema y solución | Archivos principales | Dificultad / riesgo | Comprobación |
|---|---|---|---|---|
| P0, implementado | Plantillas sustituyen inferencia: enrutamiento y errores tipados | `MotorConversacional`, `ConversationModelRouter` | Media; consumidores antiguos conservados | Pruebas de fallback, cancelación y ausencia de modelo; revisar conversación en dispositivo. |
| P0, implementado | Proveedor inaccesible: configuración, REST y prueba real | `GeminiService`, `GeminiProtocol`, `MainActivity`, `app/build.gradle` | Media; red/cuota/compatibilidad de API | Contrato JSON/errores + prueba optativa de texto e imagen contra API real. |
| P0, implementado parcialmente | Modelo local detectado sin ejecutor: selector y carga honesta | `MainActivity`, `SalveLLM`, wrappers Kotlin | Media/alta; formato y memoria del teléfono | Importar modelo conocido y ejecutar inferencia; MLC aún requiere empaquetado compatible. |
| P0, implementado parcialmente | Cámara sin ruta útil y voz frágil: foto explícita y ciclo de vida | MainActivity, MotorConversacional, manifest/layout | Media; proveedores Android y resolución de foto | Cámara, permisos, retorno/cancelación, pausa, TTS y micrófono en equipo real. |
| P1, pendiente | Elegir y consolidar runtime local | `mlc4j`, `LiteRTLlm`, catálogo y build | Alta; JNI, ABI y memoria | Un APK reproducible, sin red, 20 consultas nuevas y 20 turnos con contexto. |
| P1, pendiente | Contexto excede ventana: presupuestar tokens/resumir | `conversation/*`, adaptadores | Media; pérdida de hechos al resumir | Casos largos con correcciones y recuperación de hechos comprobables. |
| P1, pendiente | Voz completa: una sesión con cancelación y streaming | TTSManager, MotorConversacional, MainActivity | Alta; ecos y respuestas obsoletas | Medir fin de habla→primer audio, interrupciones y p50/p95 en dispositivo. |
| P1, pendiente | Memoria visual y olvidos entre almacenes | HipocampoSemantico, Room, memoria/grafo/sync | Alta; privacidad y contradicciones | Guardar, reiniciar, recuperar, actualizar y borrar un recuerdo en todos los almacenes. |
| P2, pendiente | Evaluación de calidad del asistente | `evaluation/*`, casos de prueba | Media; no confundir métricas con comprensión | Evaluar conversaciones y herramientas con respuestas esperadas y revisión humana. |

## Pruebas realizadas y límites

- **52 pruebas JUnit JVM pasaron**, compiladas con objetivo Java 11: 16 nuevas de `GeminiProtocol` y `ConversationModelRouter`, más regresiones de sesión, análisis conversacional, razonamiento, memoria, voz, herramientas, identidad y evaluación. Prueban lógica y contratos; sus proveedores controlados no acreditan inferencia de un LLM.
- `GeminiService`, `GeminiProtocol` y `ModelResult` pasan una comprobación de tipos aislada con objetivo Java 11 contra los JARs publicados de Android, OkHttp 5.3.2, sus dependencias y Gson 2.13.2; no se ejecutó Android en esta comprobación.
- Los siete archivos Java nuevos/modificados de producción/instrumentación pasan análisis sintáctico; los cuatro XML modificados se parsean. Esto no sustituye la comprobación de tipos Android/Kotlin ni el ensamblado del APK.
- `bash gradlew :app:testDebugUnitTest --no-daemon` falló antes de compilar: no se pudo descargar `gradle-9.6.0-bin.zip` (`Network is unreachable`). No se declara un build Android correcto.
- Se añadieron tres pruebas instrumentadas optativas en `RealInferenceTest`: texto Gemini, imagen con píxeles conocidos enviada a Gemini y aritmética con el runtime local. **No se ejecutaron aquí**: faltan un teléfono/emulador, los artefactos/modelo y credenciales del usuario.

En un entorno Android configurado:

```sh
./gradlew :app:testDebugUnitTest
# Configurar la clave en la app antes de habilitar llamadas reales (consumen cuota).
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=salve.core.RealInferenceTest -Pandroid.testInstrumentationRunnerArguments.runRealGemini=true
# Seleccionar un modelo compatible en la app antes de esta prueba local.
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=salve.core.RealInferenceTest -Pandroid.testInstrumentationRunnerArguments.runRealLocal=true
```

Antes de aceptar el APK: probar clave inválida/sin red, ausencia de modelo, una conversación con corrección y referencia al turno anterior, una foto real de objetos desconocidos por el chat, cancelación de cámara, permisos denegados, apertura del micrófono mientras habla y pausa/reanudación. Registrar dispositivo, Android, proveedor de voz, modelo y latencias observadas. MLC sin bibliotecas debe fallar de forma explícita.

El grupo elimina vías concretas de respuesta simulada y conecta controles utilizables. Siguen pendientes la validación integral en teléfono, el empaquetado MLC, streaming, interrupción integral, presupuesto de contexto, consolidación de voz y persistencia visual. No se presenta esta revisión como un asistente completamente operativo.

## Referencias primarias consultadas

- [Gemini generateContent](https://ai.google.dev/api/generate-content): contrato REST.
- [Modelos y retiradas](https://ai.google.dev/gemini-api/docs/deprecations): selección de modelo configurable.
- [SDKs Gemini](https://ai.google.dev/gemini-api/docs/libraries): SDK Android antiguo sin mantenimiento; Firebase AI Logic como alternativa para apps distribuidas.
- [MediaPipe Android](https://developers.google.com/edge/mediapipe/solutions/genai/llm_inference/android): inferencia local, límite de tokens y migración recomendada a LiteRT-LM.
- [Puente MLC upstream](https://github.com/mlc-ai/mlc-llm/blob/main/android/mlc4j/src/main/java/ai/mlc/mlcllm/JSONFFIEngine.java): módulo, funciones y callback nativo.
- [SpeechRecognizer](https://developer.android.com/reference/android/speech/SpeechRecognizer) y [TextToSpeech](https://developer.android.com/reference/android/speech/tts/TextToSpeech): disponibilidad, visibilidad y ciclo de vida.
