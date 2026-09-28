# Dolphin 8B con respaldo por memoria

Salve prepara **Dolphin 3.0 Llama 3.1 8B Q4_K_M** como principal y mide la RAM real
antes de cargarlo. El catálogo fija revisión, tamaño (4.920.749.472 bytes) y SHA-256.
El respaldo **Dolphin 3.0 Llama 3.2 3B Q4_K_M** conserva el mismo archivo de la versión
anterior (2.019.382.400 bytes). Si 8B funciona, se guarda también el archivo 3B para
poder cambiar sin conexión, sin cargar ambos motores a la vez. Aproximadamente
6,94 GB de almacenamiento para ambos, además de la app y los datos existentes.
Gemma 4 E2B se descarga/activa solamente después de un fallo del 3B.

La actualización solicita la nueva preparación una vez, aunque ya exista el 3B o Gemma.
Descargas externas al APK, reanudables y con verificación completa SHA-256. Wi-Fi sin
límite de datos por defecto; datos móviles solo desde el botón correspondiente.
La selección anterior se conserva hasta que una prueba real del candidato devuelve texto.
Si falta RAM o falla la descarga, verificación, carga o inferencia: **8B → 3B → Gemma**,
un intento por etapa. Pausar/cancelar, contexto excesivo y solicitudes de visión a
Dolphin no cuentan como averías del modelo.

## RAM y cambios automáticos

Se consulta `ActivityManager.getMemoryInfo`: `availMem`, `totalMem`, `threshold` y
`lowMemory`. No se usa la RAM comercial del teléfono ni el límite del heap Java.
Presupuestos conservadores de admisión, pendientes de calibrar en el teléfono:

| Candidato | Pesos | Margen de contexto/runtime | Reserva de Android |
| --- | --- | --- | --- |
| 8B | 4.920.749.472 bytes | 1.024 MiB | Máximo entre 768 MiB y `threshold` |
| 3B | 2.019.382.400 bytes | 512 MiB | La misma reserva |
| Gemma | 2.588.147.712 bytes | 512 MiB | La misma reserva |

El motor anterior se libera **antes** de medir para una nueva carga. Si el 8B ya está
cargado, se exige solo margen de contexto y reserva: no se cuentan los pesos dos veces.
Durante carga/inferencia 8B, el callback nativo revisa presión como máximo una vez por
segundo. Si Android indica memoria baja o se pierde la reserva, aborta de forma controlada,
libera el modelo y prueba el respaldo. Durante evaluación, el contexto ya está asignado,
por lo que tampoco se cuenta dos veces. Una medición desconocida no autoriza la carga.

El cambio a un archivo ya guardado verifica su SHA-256 y funciona sin conexión. Si falta
el respaldo, WorkManager lo prepara por Wi-Fi y el estado informa de la espera. Cuando
hay un respaldo válido, se reintenta el texto del turno después del cambio; las herramientas
solo reciben la respuesta final. Gemma no se intenta saltándose un 3B aún sin descargar.
Una transferencia vieja no puede reemplazar un motor recuperado. Las solicitudes
coalescidas durante la descarga del 3B se atienden al terminar la transferencia.

El 3B permanece seleccionado tras bajar de tamaño para evitar oscilaciones. Para volver
a probar el principal: **IA y cámara → Descargar o reanudar Dolphin 3.0 Llama 3.1 8B**;
se reutiliza el archivo guardado y se vuelve a medir. No se promete que 12 GB de RAM
comercial garanticen la carga. Si el sistema mata el proceso en vez de devolver una
excepción, un marcador persistente de operación 8B interrumpida permite preparar 3B
en el próximo arranque. También puede reflejar un cierre forzado; no se atribuye a OOM
sin evidencia. No puede evitar un cierre abrupto del sistema ni cambiar modelos en un
proceso que ya ha terminado.

## Saber qué modelo está respondiendo

La escena y el chat muestran el nombre y fase real: seleccionado, cargando, comprobando,
activo o no disponible. «Activo» requiere respuesta real; guardar el archivo no basta.
En **Estado** se muestra RAM, motivo del cambio y último proveedor de conversación.
Preguntar «¿Qué modelo estás usando?» funciona por texto y voz continua y lee el estado
de la aplicación directamente. Cada inferencia recibe la identidad actual del motor;
llama.cpp la recibe en el mensaje de sistema. El resultado captura el proveedor para
que una recarga posterior no cambie la atribución. Gemini se distingue como ruta remota;
no se presenta un modelo local preparado como proveedor de una respuesta de Gemini.

Fuentes técnicas: [Android MemoryInfo](https://developer.android.com/reference/android/app/ActivityManager.MemoryInfo),
[modelo original 8B](https://huggingface.co/dphn/Dolphin3.0-Llama3.1-8B),
[archivo 8B fijado](https://huggingface.co/bartowski/Dolphin3.0-Llama3.1-8B-GGUF/blob/fd2736a6e6f4e637b2242b06e267572495d88e2f/Dolphin3.0-Llama3.1-8B-Q4_K_M.gguf).

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

## Resultado anterior del 3B en Linux

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

## Recuperación y concurrencia

Los fallos se guardan por nivel. Un 8B recuperado invalida respaldos pendientes; un 3B
recuperado invalida la petición de Gemma. Se comprueba la necesidad del respaldo dentro
del mismo monitor que serializa carga e inferencia. La generación de selección evita
que un fallo de descarga antiguo sobrescriba una activación posterior. Una activación
cancelada o fallida restaura la selección anterior y no declara el candidato activo.
