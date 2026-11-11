# Cortes de voz: trazas generales de TTS y síntesis neuronal

El registro «Conversación · rendimiento» mide **inferencia del LLM**,
no cuánto tiempo suena la voz. Incluso con la respuesta completa en el chat,
un callback de Android puede detener audio antes de finalizar.

Esta corrección agrega dos tareas técnicas en
**Registro de Salve → Ejecución**, desde cualquier modo (teclado,
dictado, conversación en vivo y coche):

- **Voz · respuesta del chat**: tamaño aproximado en caracteres, TTS aceptado,
  `TTS_STARTED`, `TTS_DONE`, `TTS_STOPPED`, código de error y, cuando
  corresponda, `APP_NEW_TURN_STOP` o `APP_LISTEN_STOP`.
- **Voz · síntesis neuronal**: creación del motor, inicio de buffer, número
  de trozos generados, rechazo de buffer, petición `ENGINE_STOP_REQUESTED`,
  síntesis interrumpida o terminada.

También puede aparecer **Voz · fragmento de flujo** cuando Salve habla mientras
está escribiendo una respuesta larga.

No se almacenan texto, instrucciones, clips, voz, respuestas ni prompts.
Los registros numéricos se escriben en un hilo de I/O. Se conservan dentro
del límite compartido de los últimos 20 registros.

## Error corregido

Antes, `NeuralTtsService` interpretaba
`SynthesisCallback.audioAvailable != SUCCESS` como parada silenciosa;
podía salir sin `callback.done()` **ni** `callback.error()`.
Ahora informa a Android `ERROR_OUTPUT`, registra `OUTPUT_BUFFER_REJECTED`
y cierra el diagnóstico como `ERROR`.
La cancelación genuina de la sesión mantiene su comportamiento existente.

Esto no demuestra que el problema visto en un Galaxy S24 Ultra haya tenido
esa causa. Para el próximo corte, comparar:

- `APP_NEW_TURN_STOP` o `APP_LISTEN_STOP` antes de `TTS_STOPPED`:
  fue la app la que solicitó cancelar el turno.
- `ENGINE_STOP_REQUESTED`: Android envió una cancelación al servicio.
- `OUTPUT_BUFFER_REJECTED`: Android dejó de aceptar muestras PCM; ahora
  se notifica el error a TTS.
- `SYNTH_EXCEPTION` y `TTS_ERROR_CODE`: error de síntesis.
- `CALLBACK_DONE` y `TTS_DONE`: motor y cliente informaron finalización.
- Registro incompleto: el proceso pudo finalizar o no emitirse el callback.

## Prueba después de instalar una compilación válida

1. En el chat, di «¿cómo estás?» u otra pregunta que genere más de 100 palabras.
2. Si se corta después de unas palabras, abre Registro → Ejecución.
3. Comparte los registros «Voz · respuesta del chat» y
   «Voz · síntesis neuronal». 
4. Repite con la voz neuronal desactivada (voz Android), y compara.

La prueba no requiere usar Android Auto ni activar el micrófono dúplex.
