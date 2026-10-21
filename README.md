# Salve para Android

> **Estado del proyecto (21 de octubre de 2026):** empieza por
> [Estado actual y límites de validación](docs/ESTADO_ACTUAL.md).
> Distingue funciones en `main`, experimentos históricos y cambios aún sin fusionar
> (PR [#199](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/199)
> y [#200](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/200)).
> La presencia de código no demuestra que haya pasado una prueba real de teléfono.

> La arquitectura, los límites de autonomía y la hoja de ruta de evolución segura de
> Salve se documentan en [GOBERNANZA_Y_EVOLUCION.md](GOBERNANZA_Y_EVOLUCION.md).

## Mente persistente

Salve mantiene un modelo de sí misma con fuentes (narrativa, creencias, preguntas abiertas y
pensamientos), sueña cada noche para consolidar recuerdos, reflexiona sobre lo vivido entre
conversaciones y lo retoma en cada turno. Di «tu mente» para verlo. Arquitectura, límites
medidos y evaluación en [MENTE_PERSISTENTE.md](docs/MENTE_PERSISTENTE.md).

## Continuidad y aprendizaje desde búsquedas

Salve conserva el contexto reciente del chat al cerrar y abrir la aplicación. Las
búsquedas conversacionales con fuentes se archivan en su memoria indexada, igual
que las investigaciones persistentes. **«Mi última búsqueda»** recupera el resultado
guardado sin volver a consultar internet. La sincronización utiliza únicamente la
configuración de nube que ya esté habilitada.

Consulta [funcionamiento, límites y pruebas de continuidad](docs/CONTINUIDAD_Y_APRENDIZAJE.md).

## Nodos propios de cómputo

En **IA y cámara → Nodos de cómputo** puedes registrar equipos autorizados para
delegar cuentos, reflexiones y otras tareas de texto pesadas, con respaldo local.
Incluye el microcomponente Python, límites y prueba de conexión.
Consulta [instalación y alcance del cómputo distribuido](docs/COMPUTO_DISTRIBUIDO.md).

## Modelos de conversación y visión

La política de selección automática en `LocalModelPolicy.kt` es **Dolphin 3.0
Llama 3.1 8B Q4_K_M → Dolphin 3.0 Llama 3.2 3B Q4_K_M → Gemma 4 E2B**. Dolphin
es texto local mediante llama.cpp; Gemma E2B conserva la ruta de visión cuando está
activo. **Gemma 4 E4B es opcional**, fuera de esa escalera. Disponer de 12 GB de
RAM comercial no garantiza poder cargar el 8B: se mide la memoria libre real y se
requiere una prueba de generación antes de presentar un motor como activo.

El archivo `app/src/main/assets/config/models.json` contiene el artefacto fijado
de Gemma E2B; no representa por sí solo todos los modelos admitidos. Para cifras,
recuperación, cambios automáticos y validaciones:
[Modelos Dolphin y respaldo](docs/dolphin-android.md).

## Memoria semántica en español

Salve cuenta con una vía de memoria híbrida (consulta por palabras y candidatos
semánticos). H2 incorpora el contrato **multilingual-e5-small INT8** y su
tokenizador SentencePiece, descargados y verificados fuera del APK. Se activan
solo si están disponibles y superan la calibración; la prueba ARM64 en el
teléfono sigue siendo necesaria.

Consulta [H2: memoria multilingüe](docs/H2_MEMORIA_MULTILINGUE.md) y
[mente persistente](docs/MENTE_PERSISTENTE.md).

## Investigación y decisiones situacionales

La investigación pública puede planificar consultas, leer páginas una a una y
conservar evidencia; consulta [investigación guiada](scripts/RESEARCH_INTENT.md).

**Todavía en propuestas no fusionadas:** la [PR #199](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/199)
añade registro literal de ejecución y la
[PR #200](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/200)
plantea deliberación situacional con evaluación de prioridades y riesgos.
No confundas sus documentos de rama con funciones ya presentes en `main`.

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

## Integraciones heredadas y opcionales

Los apartados históricos de MLC/Qwen y los ejemplos de Live2D no describen el
modelo local predeterminado ni el renderizador principal de Salve. Las clases
de compatibilidad pueden seguir presentes para pruebas e importaciones, pero no
deben confundirse con una activación demostrada. La ruta principal actual está
descrita en [Modelos Dolphin](docs/dolphin-android.md) y
[Avatar realista 2D](docs/SALVE_AVATAR_2D.md).

## Mapa de documentación

Consulta [Estado actual](docs/ESTADO_ACTUAL.md) como índice de las capacidades,
evidencia de pruebas, funcionalidades propuestas y auditorías históricas.
