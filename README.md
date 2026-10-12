# Salve para Android

> La arquitectura, los límites de autonomía y la hoja de ruta de evolución segura de
> Salve se documentan en [GOBERNANZA_Y_EVOLUCION.md](GOBERNANZA_Y_EVOLUCION.md).

## Continuidad y aprendizaje desde búsquedas

Salve conserva el contexto reciente del chat al cerrar y abrir la aplicación. Las
búsquedas conversacionales con fuentes se archivan en su memoria indexada, igual
que las investigaciones persistentes. **«Mi última búsqueda»** recupera el resultado
guardado sin volver a consultar internet. La sincronización utiliza únicamente la
configuración de nube que ya esté habilitada.

Consulta [funcionamiento, límites y pruebas de continuidad](docs/CONTINUIDAD_Y_APRENDIZAJE.md).

## Modelo local principal

Salve prioriza Dolphin 3.0 Llama 3.2 3B Q4_K_M (2,02 GB), con llama.cpp para
Android. Gemma solo se prepara tras un fallo de Dolphin y se reutiliza si ya está
descargado. Consulta [instalación, límites y pruebas](docs/dolphin-android.md).

## Apariencia de Salve

La nueva base utiliza sprites realistas transparentes, con vistas, caminata y baile.
El visor y las entregas 3D anteriores se retiran del árbol actual.
Consulta [alcance y validación del avatar 2D](docs/SALVE_AVATAR_2D.md).

## Instalar desde el móvil

En una ejecución correcta de **Actions → Android tests and APK**, descarga el
artefacto `salve-preview-arm64-…`, extrae el ZIP y abre `app-preview.apk`.
Aparecerá como **Salve (prueba)**, con identificador `com.salve.app.preview`.

Esta copia puede convivir con Salve (`com.salve.app`) aunque las APK estén firmadas
con certificados distintos. Tiene recuerdos, ajustes y modelos descargados propios;
no migra automáticamente los datos de la instalación anterior. No es necesario
desinstalar Salve ni borrar sus datos para probarla.

El artefacto `salve-debug-arm64-…` conserva el identificador original. Para usarlo
como actualización de una instalación existente, necesita el mismo certificado
de firma que esa instalación (además de una versión compatible). **«Aplicación no
instalada» es un aviso genérico**, no una confirmación del motivo.

Estas APK de CI usan la clave de depuración del runner. El certificado puede
cambiar entre ejecuciones, incluso para la copia de prueba. No se garantiza
actualizar una APK de otra ejecución: para distribuir actualizaciones hay que
configurar una clave persistente mediante almacenamiento privado de secretos.
Para actualizar la Salve instalada desde Android Studio sin perder sus datos,
hay que recuperar su clave original. No publiques ningún `debug.keystore` o
clave privada en este repositorio.

Cada ZIP incluye la huella de la APK (`.sha256`), la información pública del
certificado (`.signing.txt`) y la identidad del paquete (`.badging.txt`).
CI verifica ambas variantes, su firma, arquitectura ARM64, bibliotecas de 16 KB
y los sprites transparentes empaquetados. La prueba de instalación en un teléfono sigue siendo
una comprobación aparte.

## Preparar el Android SDK

El proyecto compila con Android API 36. En Linux se puede instalar y configurar el
SDK necesario de forma reproducible desde la raíz del repositorio:

```bash
git submodule update --init --recursive
./scripts/setup-android-sdk.sh
source ~/.config/convert/android-sdk-env.sh
./gradlew tasks
```

El instalador descarga las herramientas oficiales de línea de comandos, acepta las
licencias e instala `platform-tools`, `platforms;android-36` y
`build-tools;36.0.0`, NDK `27.2.12479018` y CMake `3.22.1`. También genera el archivo local (no versionado)
`local.properties` con `sdk.dir` y un archivo de entorno que define tanto
`ANDROID_HOME` como `ANDROID_SDK_ROOT`.

La ruta predeterminada es `/opt/android-sdk`. Si el usuario no puede escribir ahí
o prefiere guardar el SDK en su directorio personal, puede elegir otra antes de
ejecutar el instalador:

```bash
ANDROID_SDK_ROOT="$HOME/Android/Sdk" ./scripts/setup-android-sdk.sh
```

Para una instalación ya existente no hace falta descargar nada: se ejecuta el
mismo comando indicando su ruta y el script reutiliza `sdkmanager`. Como alternativa
manual, se puede copiar `local.properties.example` a `local.properties` y cambiar
`sdk.dir` por la ruta absoluta real del SDK.

## Investigación web

Salve puede leer hasta tres URLs HTTPS públicas incluidas en una pregunta. Si no se incluye
ninguna URL, Wikipedia sigue siendo el mecanismo gratuito de descubrimiento inicial. La lectura
abierta de documentos no habilita descargas ejecutables: los modelos solo pueden proceder del
catálogo de hosts autorizado. Los destinos locales, privados y reservados se rechazan también
después de resolver DNS.

## Ejecutor externo de auto-mejora

Las propuestas creadas por Salve se guardan como JSON de esquema 3. Cada una
incluye un UUID estable para que los reintentos detecten un PR ya creado y no lo
dupliquen. El ejecutor aplica el diff en un worktree temporal y exporta el árbol
candidato a un contenedor Docker sin red. Git y GitHub CLI permanecen en la
estación; el contenedor solo recibe un archivo de código versionado de lectura.
Antes de utilizarlo, prepara la imagen y configura `SALVE_SANDBOX_IMAGE` según
[la guía del sandbox](scripts/sandbox/README.md). Si falta Docker o la imagen,
el runner se detiene: no ejecuta Gradle directamente en la estación.

Tras ejecutar `testDebugUnitTest` en el contenedor, comprueba que el árbol del
commit coincida con el probado y publica la rama y el PR:

```bash
python3 scripts/auto_improvement_runner.py /ruta/a/propuesta.json --repo .
```

Para comprobar la aplicación y las pruebas sin publicar ni crear el PR:

```bash
python3 scripts/auto_improvement_runner.py /ruta/a/propuesta.json --repo . --dry-run
```

En un dispositivo conectado con una compilación depurable, el puente ADB puede
leer una propuesta directamente desde la bandeja privada. Procesa solo la más
antigua y, después de crear correctamente el PR, la mueve a
`files/auto-improvement/processed` para conservar la auditoría:

```bash
python3 scripts/auto_improvement_adb_bridge.py --repo .
```

Este puente depende de `adb run-as`, por lo que está limitado deliberadamente a
builds depurables. Las compilaciones de producción no deben exponer su
almacenamiento privado mediante ADB.

Para mantener el circuito activo en una estación de desarrollo conectada, puede
usarse el modo supervisor:

```bash
python3 scripts/auto_improvement_adb_bridge.py --repo . --watch \
  --interval-seconds 60 --max-consecutive-failures 3
```

Solo puede existir un supervisor por repositorio. Procesa una propuesta por
ciclo, aplica retroceso exponencial tras errores y se detiene al alcanzar el
límite configurado de fallos consecutivos.

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
