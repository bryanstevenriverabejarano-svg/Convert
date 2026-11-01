# SALVE VoiceLab — diagnóstico de voz

## Estado de implementación
Primera pieza: `VoiceLabReport`, registrador local opt-in, limitado a 500 eventos, con exportación PDF a un destino privado elegido por el llamador. **Todavía no está conectado a LiveVoiceSession, no hay botón para ejecutar el examen ni para compartir el PDF.** No confundir este PR inicial con un examen funcional.

## Integración siguiente (sin duplicaciones)
- Conectar el registrador a `LiveVoiceSession` y `VoiceTurnMetrics`, sin segundo reconocedor, segundo TTS ni nuevo servicio de micrófono.
- Exponer consentimiento explícito, iniciar/detener examen y exportar mediante el selector de documentos Android.
- Registrar los eventos de estado, inicio/fin de TTS, inicio/fin de ASR, código de error, origen de interrupción (manual o detector), cambio de ruta de audio y latencias.
- Evitar registrar texto de conversación por defecto. Para transcripciones y respuestas, solicitar consentimiento independiente y ofrecer revisión y eliminación antes de exportar.
- Añadir pruebas JVM de límites, orden, token obsoleto y exportación Android instrumentada; no simular un AEC real en JVM.
- Añadir un modo de examen guiado: silencio con altavoz alto, silencio con auriculares, interrupción voluntaria, pausa de frase y despedida. Las pruebas de silencio requieren confirmación humana.
- Diferenciar observación de causa inferida. Ningún informe puede certificar por sí solo que hubo eco sin referencia acústica real.

## Precauciones
No fusionar con el PR experimental de barge-in hasta verificar las interrupciones espontáneas. No almacenar audio PCM ni subir informes automáticamente. Limitar tiempo y tamaño de sesión y limpiar los archivos temporales.
