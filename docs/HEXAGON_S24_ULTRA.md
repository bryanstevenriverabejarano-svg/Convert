# Salve en la NPU Hexagon del Galaxy S24 Ultra (SM8650 / V75)

Estado: **integración experimental implementada en el código, pendiente de compilar la
variante Hexagon y probarla en el teléfono real**. No se debe anunciar NPU activa a
partir del nombre del procesador, de una descarga completada o de un código fusionado.

## Qué usa Salve y qué cambia

- Salve ya usaba llama.cpp con OpenCL (Adreno) o CPU. La NPU no estaba conectada.
- El submódulo llama.cpp del proyecto incluye un backend `ggml-hexagon` que puede
  compilarse con el Hexagon SDK (necesita herramientas propietarias de Qualcomm).
- En **una APK compilada con Hexagon**, un modelo GGUF `*-Q4_0.gguf` cargado en
  un dispositivo que declara `Build.SOC_MODEL=SM8650*` intenta:
  **Hexagon HTP → Adreno OpenCL → CPU**. El backend realmente usado aparece en
  estado/diagnóstico y en estadísticas de generación.
- Los modelos que Salve utiliza hoy (`Q4_K_M`, `Q4_K_P`, `.litertlm`) conservan
  su ruta anterior. La rama **no** convierte los pesos existentes a Q4_0 ni
  convierte automáticamente modelos arbitrarios a la NPU.
- El modo local sigue siendo **100 % en el teléfono**, sin subir mensajes a servidores.
- Si el SDK o el kernel DSP no están disponibles, la compilación normal sigue sin
  Hexagon. No se incorporan binarios de fabricante ni pesos grandes al repositorio.

## Cómo generar la variante experimental

El workflow `.github/workflows/salve-hexagon-preview.yml` compila una **APK de prueba
independiente** desde un PR de la rama de integración. No cambia la release
`salve-ultima` ni sube esta compilación experimental como actualización obligatoria.

- Primero construye `libggml-hexagon.so` (biblioteca **Android ARM64**) y
  `libggml-htp-v75.so` (biblioteca **NPU DSP**) desde la **misma revisión fijada**
  del submódulo llama.cpp:
  `07ec7c8af09b576792063411d7dc2ec60b2f3a6f`.
- Utiliza la imagen oficial de herramientas de Qualcomm
  `ghcr.io/snapdragon-toolchain/arm64-android:v0.8`, que contiene el SDK
  Hexagon, el compilador DSP y el Android NDK. Un host Android normal **no**
  necesita ni debe incorporar estas herramientas propietarias.
- Luego usa Gradle/NDK habitual de Salve con `SALVE_ENABLE_HEXAGON=1` y
  copia ambos binarios precompilados al APK. No toca las versiones de las
  bibliotecas base, el código de memoria ni las otras rutas de inferencia.
- La publicación se limita a un artefacto de GitHub Actions de **dos días**,
  no a una nueva release. Es la APK `app-preview.apk` bajo
  `salve-hexagon-v75-preview-<commit>`.

### Reproducción local con Docker + Android SDK

```sh
git submodule update --init --recursive
mkdir -p build/qualcomm-dist
docker run --rm --platform linux/amd64 -v "$PWD:/workspace" \
  -w /workspace/third_party/llama.cpp \
  ghcr.io/snapdragon-toolchain/arm64-android:v0.8 bash -euc '
    cp docs/backend/snapdragon/CMakeUserPresets.json .
    cmake --preset arm64-android-snapdragon-release \
      -B /workspace/build/qualcomm-llama \
      -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
      -DGGML_BACKEND_DL=ON -DBUILD_SHARED_LIBS=ON \
      -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
    cmake --build /workspace/build/qualcomm-llama \
      --target ggml-hexagon htp-v75 --parallel 2
    cp /workspace/build/qualcomm-llama/bin/libggml-hexagon.so \
       /workspace/build/qualcomm-dist/
    cp /workspace/build/qualcomm-llama/ggml/src/ggml-hexagon/libggml-htp-v75.so \
       /workspace/build/qualcomm-dist/
  '
export SALVE_ENABLE_HEXAGON=1
export SALVE_HEXAGON_ARM64_LIB="$PWD/build/qualcomm-dist/libggml-hexagon.so"
export SALVE_HEXAGON_SKEL_V75="$PWD/build/qualcomm-dist/libggml-htp-v75.so"
bash scripts/setup-android-sdk.sh
./gradlew --init-script scripts/android-arm64.init.gradle :app:testPreviewUnitTest :app:assemblePreview
```

Comprueba el contenido:
```sh
unzip -l app/build/outputs/apk/preview/app-preview.apk \
  | grep -E 'lib/arm64-v8a/libggml-hexagon.so|assets/hexagon/libggml-htp-v75.so'
```
Ambos archivos son necesarios para intentar la NPU. Además el `libggml-hexagon.so`
**debe exportar `ggml_backend_init`**: si fue compilado sin
`GGML_BACKEND_DL=ON`, se puede empaquetar pero `ggml_backend_load()`
no podrá cargarlo. Si falta una biblioteca o el símbolo, no afirmar
que la APK utiliza Hexagon. En el primer intento compatible, `HexagonDspAssets`
extrae el binario DSP y configura `ADSP_LIBRARY_PATH` para FastRPC.
La integración de controladores con el firmware Samsung sigue requiriendo una
prueba en un S24 Ultra real.

## Validación física obligatoria (S24 Ultra)

1. Instalar la variante `preview` sin sustituir la versión principal.
2. Seleccionar/importar un GGUF **Q4_0** legítimo y validado, lo bastante
   pequeño para la RAM libre. Los `Q4_K_M` existentes sirven para comparar
   Adreno/CPU pero **no** para certificar NPU.
3. Ejecutar un mensaje corto y luego uno largo; revisar
   **Diagnóstico del modelo → Motor GGUF real: NPU Hexagon**.
   También revisar Logcat para `ggml-hex: Hexagon Arch version v75`,
   sesiones `HTP0` y capas descargadas a HTP.
4. Repetir en la misma APK con un modelo Q4_K_M y observar la GPU o CPU.
   Comparar latencia, tokens/s aproximados, memoria y temperatura.
5. Probar el respaldo ante un fallo normal y un cierre del proceso controlado:
   el guard independiente impide volver a entrar en un backend fallido hasta
   la siguiente instalación. Durante carga mixta un cierre nativo puede
   deshabilitar preventivamente NPU **y** GPU (respaldo CPU).
6. Verificar además cámara/visión si corresponde: no asumir que todo el
   proyector multimodal se ejecuta en la NPU.

## Límites y rollback

- No se ha probado en el Galaxy S24 Ultra ni se ha medido velocidad real.
- Las variantes ordinarias de CI **no** llevan el binario Hexagon; solo
  `Salve Hexagon V75 experimental APK` intenta empaquetarlo.
- Los drivers FastRPC, permisos del firmware Samsung y el acceso al DSP
  pueden impedir el arranque incluso con SDK, bibliotecas y modelos correctos.
- Esta rama no cambia almacenamiento de recuerdos, interfaz, herramientas
  autorizadas ni la escalera de modelos predeterminada.
- Para retroceder, usar la APK estándar (`SALVE_ENABLE_HEXAGON` sin activar).
- No usar root ni alterar la placa del teléfono.

Fuentes:
- [llama.cpp: compilación y ejecución Snapdragon](https://github.com/ggml-org/llama.cpp/blob/master/docs/backend/snapdragon/README.md)
- [Backend Hexagon: librerías CPU y DSP](https://github.com/ggml-org/llama.cpp/blob/master/docs/backend/snapdragon/developer.md)
- [Qualcomm GenieX Android, modelos y cuantizaciones](https://qualcomm.github.io/qidk/blogs/geniex_android.html)
- [SDK Qualcomm: SM8650 → Hexagon V75](https://docs.qualcomm.com/nav/home/QNN_general_overview.html?product=924033590759186372)
