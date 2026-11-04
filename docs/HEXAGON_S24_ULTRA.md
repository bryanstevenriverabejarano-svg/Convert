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

Requiere un entorno de desarrollo Android con el SDK/NDK de este proyecto y el
**Qualcomm Hexagon SDK** compatible con la revisión fijada de llama.cpp. Qualcomm
publica una imagen de toolchain de Snapdragon con NDK y Hexagon:
`ghcr.io/snapdragon-toolchain/arm64-android:v0.8`.

1. `git submodule update --init --recursive`. No actualizar el submódulo a
   otra revisión sin revisar la API JNI; actualmente está fijado en
   `07ec7c8af09b576792063411d7dc2ec60b2f3a6f`.
2. Compilar con ese submódulo la biblioteca de DSP
   `libggml-htp-v75.so` para arquitectura **Hexagon V75**. El mecanismo
   oficial es el preset `arm64-android-snapdragon-release` de
   `third_party/llama.cpp/docs/backend/snapdragon/CMakeUserPresets.json`
   y su target `htp-v75` en el toolchain Qualcomm. **No confundir** el
   binario DSP con la biblioteca ARM64 `libggml-hexagon.so`.
3. Compilar Salve en un entorno con Android y Hexagon SDK exportando:

   ```sh
   export SALVE_ENABLE_HEXAGON=1
   export HEXAGON_SDK_ROOT=/ruta/al/Hexagon-SDK
   export SALVE_HEXAGON_SKEL_V75=/ruta/al/libggml-htp-v75.so
   ./gradlew :app:assemblePreview
   ```

   Gradle compila `ggml-hexagon` además de los motores existentes y
   empaqueta el kernel DSP como asset `hexagon/libggml-htp-v75.so`.
   En el primer intento compatible, `HexagonDspAssets` lo extrae al espacio
   privado de la aplicación y JNI configura `ADSP_LIBRARY_PATH` para FastRPC.

4. **Verificar el APK**, por ejemplo:
   ```sh
   unzip -l app/build/outputs/apk/preview/app-preview.apk | grep -E 'ggml-hexagon|hexagon/libggml-htp-v75'
   ```
   Ambas entradas son obligatorias: una es CPU-side ARM64 y otra DSP-side.
   Si falta cualquiera, **no** afirmar que la APK puede utilizar Hexagon.

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
- Las variantes corrientes de CI **no** llevan el binario Hexagon.
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
