# Salve · Modo conducción (Android Auto)

## Qué hace la integración

- Usa `androidx.car.app:app:1.7.0` y `CarConnection.getType()` para diferenciar proyección Android Auto (`2`), Android Automotive OS (`1`), desconectado (`0`) y desconocido.
- Al conectarse con Android Auto **mientras el proceso de Salve está vivo**, activa el perfil de conducción si está habilitado (activado por defecto). No inicia un segundo motor LLM, ni crea otra memoria.
- El motor conversacional reduce las respuestas **habladas** a 220 caracteres y pide frases breves sin instrucciones de mirar el móvil. El chat escrito permanece igual.
- Publica, si el usuario ha permitido notificaciones, una notificación de baja importancia que abre `MainActivity` con `ACTION_VOICE_COMMAND`; se reutiliza `LiveVoiceSession` y su `VoiceForegroundService` existente.
- Al desconectar, oculta el acceso y vuelve a la política normal de voz. En «Mi móvil → Modo conducción» se puede consultar el estado, desactivar o reactivar el perfil y abrir conversación por voz.

## Límites deliberados

- **No declara ninguna categoría ni `CarAppService` de navegación, IoT, media o mensajería**: Salve es un asistente general y hacerse pasar por otra categoría infringiría los requisitos de Android Auto. Este PR **no crea icono de Salve en la pantalla del Jeep**.
- La conexión solo puede observarse mientras el proceso existe. Si Android ha terminado completamente Salve, este cambio no tiene permisos para relanzarla automáticamente, iniciar una Activity o abrir un micrófono desde segundo plano. No se intenta eludirlo con un servicio persistente o una recepción USB no fiable.
- La notificación no inicia audio ni escucha por sí sola; se abre la voz por un toque explícito. El acceso puede requerir permiso de notificaciones de Android 13+.
- No confirma que la voz salga por los altavoces del coche ni que el botón del volante invoque Salve. Depende de audio focus, el host, Android Auto y los permisos.
- No ofrece teclado/chat en pantalla del vehículo, ni envío automático de mensajes.
- Para aparecer en la pantalla del coche necesitaría **una función auténtica de una categoría admitida**, el servicio/car descriptor conforme a su categoría, validación UX, pruebas DHU y distribución como app de origen confiable. No basta habilitar «Fuentes desconocidas» para las apps con Car App Library.

## Pruebas recomendadas

1. En Android sin coche: «Mi móvil» muestra «Sin conexión» o «Comprobando…» y no activa el perfil ni muestra notificación de conducción.
2. Con el móvil conectado a Android Auto en un coche estacionado: «Mi móvil» debe indicar «Android Auto conectado». La notificación permite abrir voz y debe mantener el mismo modelo/memoria.
3. Negar permisos de notificaciones: Salve sigue funcionando y no inicia el micrófono.
4. Desactivar en «Mi móvil»: sin respuestas de conducción ni notificación, aunque Android Auto esté conectado.
5. Desconectar el cable: la notificación desaparece y la política de voz regresa a la habitual.
6. Probar la voz con el vehículo estacionado; el sistema puede no dirigir el audio por los altavoces del coche.
7. Comprobar que Android Automotive (`1`) y USB normal no se interpretan como Android Auto proyectado.

Documentación oficial:
- https://developer.android.com/training/cars/apps/library/connection-api
- https://developer.android.com/training/cars
- https://developer.android.com/training/cars/testing
