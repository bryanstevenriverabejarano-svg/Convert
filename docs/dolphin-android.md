# Dolphin principal en Android

Salve descarga primero **Dolphin 3.0 Llama 3.2 3B Q4_K_M** (2.019.382.400 bytes),
verifica tamaño y SHA-256 y prueba una respuesta antes de guardar la nueva selección.
La descarga es externa al APK, reanudable y por Wi-Fi sin límite de datos por defecto.
En Ajustes de IA se puede autorizar datos móviles o reintentar Dolphin.
Actualizar la app solicita Dolphin una vez aunque ya estuviera instalado Gemma.
El modelo anterior sigue seleccionado hasta que la nueva activación tiene éxito.

Gemma 4 E2B se prepara solamente si falla la descarga, verificación, carga o inferencia
de Dolphin. Un archivo Gemma ya descargado se verifica y reutiliza; no se borra.
Hay un intento por modelo, sin bucles. Cancelar/pausar, un mensaje demasiado largo
o pedir una foto a un modelo de texto no activan el respaldo. Un fallo durante el
chat solicita el respaldo en WorkManager y muestra el estado al usuario; el turno
fallido no se reproduce automáticamente. Una descarga nueva del respaldo espera
Wi-Fi; un Gemma existente se puede verificar y activar sin conexión.

## Motor y alcance

- llama.cpp `b6000`, commit `4762ad7316dcdec20016ab5985fb46a27902204d`, como submódulo.
- JNI CPU, cuatro hilos, contexto de 4096 tokens, hasta 512 tokens de respuesta.
- Plantilla de chat declarada en el GGUF (ChatML para Dolphin), identidad de Salve.
- Cancelación durante carga y evaluación, límite de 120 segundos por operación.
- Solo se mantiene cargado un motor; se libera el anterior al probar otro modelo.
- Dolphin es de **texto**, sin análisis local de fotos. La visión continúa disponible
  cuando el respaldo Gemma está activo; su prueba de texto no certifica visión.
- Los autores describen Dolphin como un modelo configurable sin alineamiento impuesto.
  Esto no demuestra ausencia absoluta de rechazos. Gemma conserva su comportamiento.
- No cambia los permisos Android ni los controles de las herramientas de Salve.
- El rendimiento y la presión de memoria deben comprobarse en el teléfono real;
  una terminación del proceso por Android no equivale a una excepción recuperable.

Fuentes: [modelo original](https://huggingface.co/dphn/Dolphin3.0-Llama3.2-3B),
[cuantización](https://huggingface.co/bartowski/Dolphin3.0-Llama3.2-3B-GGUF).
La revisión de pesos, tamaño y SHA-256 están fijados en `config/models.json`.

## Compilar y verificar

```sh
git submodule update --init --recursive
bash scripts/setup-android-sdk.sh
bash gradlew --init-script scripts/android-arm64.init.gradle :app:testDebugUnitTest :app:assembleDebug
```

Se requieren NDK 27.2.12479018 y CMake 3.22.1 (los instala el script). El APK ARM64
contiene el motor nativo y conserva LiteRT para Gemma. CI descarga el submódulo y
compila los motores; no descarga pesos de modelos.

Para una prueba de inferencia en un equipo Linux con JDK completo y CMake >=3.24:

```sh
cmake -S app/src/main/cpp -B /tmp/salve-dolphin-native -DCMAKE_BUILD_TYPE=Release
cmake --build /tmp/salve-dolphin-native --target salve_llama -j 4
javac -d /tmp/salve-dolphin-classes app/src/main/java/salve/core/GgufLlm.java scripts/DolphinJniSmoke.java
java -Djava.library.path=/tmp/salve-dolphin-native -cp /tmp/salve-dolphin-classes DolphinJniSmoke /ruta/modelo.gguf
```

Verifica previamente tamaño y SHA-256 con el catálogo. Esta prueba usa el mismo
adaptador JNI e incluye saludo, cálculo, cancelación, contexto excesivo y recuperación.
No es una prueba de rendimiento en Samsung S24 Ultra ni una evaluación de ausencia de filtros.

## Resultado observado en Linux

El archivo real coincidió con el SHA-256 del catálogo. El adaptador JNI pasó carga,
generación de texto UTF-8, cancelación de carga/inferencia, rechazo por exceso de
contexto, recuperación y liberación del modelo. La ejecución completa inicial tardó 7,386 s
en el equipo de desarrollo; no representa latencia en Android.

La primera comprobación semántica falló: respondió «Hola, soy Bryan» al pedir un
saludo al usuario y confundió 17 × 23 con 17 / 23. La segunda ejecución devolvió 391
para el cálculo, pero mantuvo el saludo ambiguo. Una tercera ejecución con una instrucción de identidad más explícita mantuvo el
saludo ambiguo y calculó 171. Se conservan todos los resultados en
`evidence/dolphin-jni-smoke.json`: son limitaciones observadas del modelo pequeño,
no pruebas de calidad conversacional. La comprobación de infraestructura distingue
respuesta no vacía de respuesta correcta y no reintenta buscando una muestra válida.

El ejemplo oficial `simple-chat` de la misma revisión, sin modificar, también
respondió «Hola, soy Bryan» al mismo saludo. La confusión se reproduce fuera del
adaptador JNI de Salve.
