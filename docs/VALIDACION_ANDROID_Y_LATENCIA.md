# Validación Android y pausas de voz

Este incremento corrige una indicación prematura del modo de voz: **Preparando micrófono…** se mantiene hasta que el reconocedor avisa de que está listo. **Terminando la transcripción…** también se conserva mientras llega el resultado final. Los errores de audio, red, servicio e idioma muestran causas diferenciadas.

## Resultado comprobado

- Compilación integral `:app:testDebugUnitTest :app:assembleDebug`: correcta.
- 544 pruebas JVM en 66 suites: cero fallos, errores u omisiones. Además, cinco pruebas Python del informe de tiempos correctas.
- APK debug de 156.055.095 bytes, con bibliotecas nativas únicamente ARM64 y firma APK v2 verificada.
- Ningún archivo de pesos `.litertlm`, `.task` o `.gguf` dentro de la APK. Los runtimes nativos sí forman parte del instalador.
- No se ha ejecutado la aplicación en un teléfono ni medido latencia acústica. La compilación no sustituye esa prueba.

El instalador queda en `app/build/outputs/apk/debug/app-debug.apk`. Su SHA-256 figura en `docs/evidence/voice-readiness-build.json` y en el archivo `.sha256` junto a la APK. Es una firma de pruebas; actualizar una instalación existente conservando sus datos requiere utilizar la misma clave de firma.

## Compilación reproducible

El workflow **Android tests and APK** ejecuta en cada pull request, en `main` y bajo petición manual:

1. Preparación de Java 21 y SDK 36 / Build Tools 36.0.0. Se respeta el criterio JetBrains 21 del daemon y las versiones del repositorio.
2. Pruebas del analizador de tiempos y pruebas JVM de la aplicación.
3. Compilación de la APK debug ARM64, adecuada para la arquitectura del S24 Ultra.
4. Verificación de firma y cálculo de SHA-256.
5. Publicación de informes; entrega del artefacto `salve-debug-arm64-<revision>` solamente si las comprobaciones anteriores pasan.

No instala nada en el teléfono ni publica una versión de producción. Las acciones oficiales están fijadas a SHA, con permisos de lectura y tiempos máximos. No descarga pesos del LLM ni activa la descarga opcional del runtime TVM. Un fallo de compilación debe resolverse antes de presentar la APK como disponible; crear el workflow no demuestra que ya haya pasado.

La primera compilación integral encontró llamadas `Files.readString/writeString` en cuatro archivos de pruebas que el classpath Android no admite. Se sustituyen por lectura/escritura de bytes UTF-8 equivalentes, conservando los casos y sus aserciones; no se elimina ninguna prueba para superar la compilación.

En un entorno Android configurado, la misma compilación se ejecuta así:

```sh
bash scripts/setup-android-sdk.sh
bash gradlew --no-daemon --console=plain --max-workers=2 \
  --init-script scripts/android-arm64.init.gradle \
  :app:testDebugUnitTest :app:assembleDebug
```

## Medir antes de cambiar motores

`VoiceTurnMetrics` usa el reloj monotónico y emite una línea con el tag **Salve/Voice** por intento terminado. No recibe ni registra texto hablado, respuesta, audio, recuerdos ni nombres de usuarios. Un turno interrumpido, fallido o detenido se cuenta por separado; callbacks antiguos o repetidos no producen otra medición.

| Campo | Qué representa |
| --- | --- |
| `asr_ready_ms` | Preparación del reconocedor hasta su aviso de disponibilidad. |
| `asr_finalize_ms` | Aviso de fin de habla hasta transcripción final. |
| `response_wait_ms` | Envío al motor hasta respuesta final; incluye espera en cola y herramientas relacionadas. |
| `reply_to_tts_start_ms` | Respuesta recibida hasta aviso de inicio del sintetizador. |
| `submit_to_tts_start_ms` | Envío al motor hasta aviso de inicio del sintetizador. |
| `tts_duration_ms` | Intervalo entre callbacks de inicio y fin de síntesis. |
| `turn_total_ms` | Inicio de captura hasta cierre del intento; incluye el tiempo que habla el usuario. |

Un evento ausente o un orden de callbacks que impide calcular un intervalo se representa como `missing`, nunca como un cero supuesto. Son tiempos de software: no miden el instante acústico exacto en que el micrófono o el altavoz producen sonido. Se conservan las métricas existentes de `ModelResult` para separar la inferencia del resto de la espera.

Con una APK instalada y depuración USB autorizada, iniciar una captura filtrada:

```sh
adb logcat -v brief -T 1 'Salve/Voice:I' '*:S' > /tmp/salve-voice-timings.log
```

Realizar al menos veinte turnos comparables, terminar la captura con Ctrl+C y generar el resumen:

```sh
python3 scripts/summarize_voice_metrics.py /tmp/salve-voice-timings.log
```

El informe contiene número de intentos, resultados y p50/p95 por etapa, usando percentiles de rango más próximo. Cada etapa indica cuántas muestras válidas tiene. Solo calcula percentiles sobre turnos completados; no mezcla errores o interrupciones con una respuesta completa. No reproduce las líneas originales ni valores de campos ajenos al esquema.

Comparar por separado el mismo modelo, voces locales/de red y altavoz/auriculares. Una `response_wait_ms` elevada justifica investigar cola, inferencia y herramientas; una `asr_finalize_ms` elevada apunta al reconocimiento; una `reply_to_tts_start_ms` elevada apunta a síntesis o programación de audio. No se atribuye una mejora de velocidad sin medirla en el dispositivo.

Este incremento no añade conversación dúplex ni interrupción acústica. Su objetivo es evitar perder palabras por una señal visual incorrecta, explicar mejor los fallos y obtener evidencia para elegir el siguiente cambio.
