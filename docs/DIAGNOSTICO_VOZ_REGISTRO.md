# Diagnóstico de cortes de voz de Salve

## Dónde verlo

Abre **Registro de Salve → Ejecución**, elige una tarea de tipo
**Conversación · diagnóstico de voz** y pulsa **Informe PDF** o **Copiar**.
Cada sesión de voz guarda una entrada técnica separada de los registros de dibujo,
investigación y rendimiento del modelo. El registro tiene el mismo límite de las
últimas 20 tareas y se conserva en `noBackupFilesDir`.

No se almacenan grabaciones, transcripciones del usuario, texto generado ni prompts.
Solo códigos de evento de la app, tokens de turno, datos técnicos numéricos y
duraciones en milisegundos. El informe de VoiceLab sigue disponible.

## Cómo interpretar un corte

- `INTERRUPT_AUTOMATIC` seguido de `MIC_DETECTOR_TRIGGERED` y
  `TTS_STOPPED`: el detector acústico activó la ruta de interrupción.
  **No prueba** que haya hablado una persona: podría ser eco o ruido.
- `INTERRUPT_MANUAL`: se pulsó el botón de interrupción.
- `TTS_ERROR` o `TTS_ERROR_CODE`: el motor de síntesis informó un error;
  `code` contiene el error numérico de Android.
- `TTS_STOPPED`: Android notificó una parada. `code=1` corresponde al
  parámetro `interrupted=true` del callback, no a una causa demostrada.
- `TTS_DONE` y `TTS_FINISHED_OK`: el sistema de TTS notificó terminación.
- `TTS_ENQUEUE_FAILED`: falló el envío de la respuesta al TTS.
- `AUDIO_FOCUS_CHANGE` / `LOOP_STOP`: el sistema o el controlador de
  conversación cambió de estado. Para `LOOP_STOP`, `code` es el ordinal de
  `VoiceConversationLoop.StopReason`; se conserva el valor sin traducir para
  no confundirlo con otra causa.
- `MIC_CAPTURE_ACTIVE code=1`: captura con AEC activo; `code=3` significa
  AEC + supresor de ruido activos. La ausencia de AEC impide el monitor
  automático, pero conserva la interrupción manual.
- `MIC_AEC_UNAVAILABLE`, `MIC_AEC_CREATE_FAILED` o
  `MIC_AEC_ENABLE_FAILED`: el monitor no pudo activar cancelación de eco.
- `MIC_DETECTOR_TRIGGERED` sin `INTERRUPT_AUTOMATIC`: una detección llegó
  tarde o el token de turno ya no era válido.

La línea `MÉTRICAS DE VOZ` muestra las duraciones de ASR, respuesta y TTS.
Un evento ausente no significa necesariamente que no ocurrió: una cancelación
puede revocar callbacks antiguos, y Android puede finalizar el proceso sin avisar.

## Prueba en el Galaxy S24 Ultra

1. Pide a Salve que dé una respuesta larga **sin interrumpirla**.
2. Repite con el volumen del altavoz bajo.
3. Repite con auriculares.
4. Si se corta, entra en Ejecución, copia el diagnóstico y compara el orden de
   los eventos (sesión, token, milisegundos, detector, TTS y foco de audio).

Esta mejora es instrumentación, **no una reparación verificada** del corte.
No modifica umbrales del detector, sincronización TTS, permisos ni política de
interrupción. Se necesitan pruebas reales en el teléfono.
