# MLC-LLM Android

> La arquitectura, los límites de autonomía y la hoja de ruta de evolución segura de
> Salve se documentan en [GOBERNANZA_Y_EVOLUCION.md](GOBERNANZA_Y_EVOLUCION.md).

## Investigación web

Salve puede leer hasta tres URLs HTTPS públicas incluidas en una pregunta. Si no se incluye
ninguna URL, Wikipedia sigue siendo el mecanismo gratuito de descubrimiento inicial. La lectura
abierta de documentos no habilita descargas ejecutables: los modelos solo pueden proceder del
catálogo de hosts autorizado. Los destinos locales, privados y reservados se rechazan también
después de resolver DNS.

## Modelos locales compatibles

Salve usa **MLC en Android**. Un GGUF de Ollama no es intercambiable con este motor y
no basta con descargar la pagina HTML de Hugging Face o un archivo de pesos aislado.
La descarga integrada obtiene ahora el manifiesto del repositorio, baja el
`mlc-chat-config.json`, el tokenizador y todos los `params_shard_*.bin`, permite
reanudar los archivos parciales y activa el modelo al terminar.

El modelo predeterminado es pequeño para funcionar en mas telefonos:

* [Qwen2.5 0.5B Instruct, cuantizado para MLC](https://huggingface.co/mlc-ai/Qwen2.5-0.5B-Instruct-q4f16_1-MLC): conversación, español y tareas generales.

Alternativas oficiales para preparar en una compilación propia, si el dispositivo
tiene suficiente RAM y almacenamiento:

* [Qwen2.5 Coder 1.5B Instruct](https://huggingface.co/Qwen/Qwen2.5-Coder-1.5B-Instruct): mejor para aprender y explicar programación; hay que convertirlo/empaquetarlo con MLC antes de usarlo en esta app.
* [Catálogo de modelos preconvertidos de MLC](https://huggingface.co/mlc-ai/models): permite elegir otra cuantización MLC compatible.
* [Guía oficial de empaquetado para Android](https://llm.mlc.ai/docs/deploy/android.html): el `model_lib` del modelo elegido debe incluirse al crear la APK.

Para cambiar el modelo descargado automáticamente, modifica
`app/src/main/assets/config/models.json` usando `huggingFaceRepo` (formato
`organización/repositorio`) y vuelve a empaquetar su librería con `mlc_llm package`.
OneDrive puede servir como copia de seguridad, pero no como catálogo automático:
sus enlaces compartidos cambian o devuelven HTML y no garantizan reanudación ni un
manifiesto verificable.

Checkout [Documentation page](https://llm.mlc.ai/docs/deploy/android.html) for more information.

- run `mlc_llm package`
- open this `MLCChat/` folder as a project in Android Studio

## Live2D Cubism SDK (Android)

Para animar el personaje con Live2D Cubism SDK for Native:

1. Descarga el SDK desde el portal oficial de Live2D (requiere cuenta). Extrae los binarios y headers del paquete `CubismSdkForNative`.
2. Añade el SDK como módulo o dependencia local en el proyecto Android. La app incluye un wrapper neutral en `app/src/main/java/salve/live2d` para evitar dependencias directas mientras el SDK no esté presente.
3. Exporta el modelo desde Cubism Editor:
   - Genera el archivo `.moc3`.
   - Configura la física del cabello (flequillo y mechones) con grupos de péndulo y parámetros de entrada (inclinación de cabeza, movimientos X/Y).
   - Exporta el archivo `*.physics3.json`.
4. Copia el `.moc3`, texturas, y `*.physics3.json` dentro de `app/src/main/assets/live2d/` o una ruta accesible por la app.
5. Implementa el puente con el SDK en una clase que implemente `Live2DParameterSink` y úsala con `Live2DTouchController` para enlazar la interacción táctil a parámetros como `ParamAngleX` y `ParamAngleY`.

El ejemplo `Live2DCanvasView` muestra cómo enganchar un controlador sin depender directamente del SDK y mantiene el proyecto compilable hasta que se agreguen las librerías.
