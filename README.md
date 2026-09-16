# MLC-LLM Android

> La arquitectura, los límites de autonomía y la hoja de ruta de evolución segura de
> Salve se documentan en [GOBERNANZA_Y_EVOLUCION.md](GOBERNANZA_Y_EVOLUCION.md).

## Investigación web

Salve puede leer hasta tres URLs HTTPS públicas incluidas en una pregunta. Si no se incluye
ninguna URL, Wikipedia sigue siendo el mecanismo gratuito de descubrimiento inicial. La lectura
abierta de documentos no habilita descargas ejecutables: los modelos solo pueden proceder del
catálogo de hosts autorizado. Los destinos locales, privados y reservados se rechazan también
después de resolver DNS.

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
