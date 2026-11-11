# Salve: Spotify y órdenes por voz en modo coche

## Problema observado

El usuario dijo «Salve, pon música en Spotify», luego «Lluvia, Cepeda»,
«sí, pon esa canción» y «abre Spotify». Gemma respondió hablando pero
no produjo la acción solicitada. Los mensajes aparecieron en el chat;
eso demuestra la entrada ASR y la respuesta textual, **no** la ejecución de una herramienta.

## Causa encontrada en el código

En `MotorConversacional`, `modelFirstDevice` omitía el ejecutor explícito
cuando había modelo e identidad privada, incluso si la orden encajaba ya en
`AppActionCommand`. `CognitiveTurn` permite que el modelo elija `respond`
sin herramientas, de modo que el turno finalizaba con una promesa verbal.
Los nombres de canciones pronunciados como respuestas a «¿qué canción?»
tampoco tenían una ruta contextual acotada.

## Cambios

- Los imperativos inequívocos siguen usando el **mismo** `PhoneActionsDesk`
  que ya estaba en la app; no se crean ejecutores duplicados.
- Las expresiones naturales no reconocidas siguen usando Gemma y el
  catálogo de herramientas autorizadas.
- `MusicFollowupCommand` solo autoriza una canción mencionada
  recientemente por el usuario cuando Salve había preguntado qué canción
  quería, y la conversación anterior contiene una petición real de música.
  «Pon la que tú quieras» usa `Tus me gusta`, no inventa un tema.
- Los controles claros «play», «pausa», «siguiente» y «anterior»
  utilizan el `MusicController` existente sin depender de una selección LLM.
- En **modo coche** `OPEN_APP` y `CALL` no abren pantallas.
  Una búsqueda musical solo se envía a la sesión multimedia **ya activa**
  mediante `MusicController.playFromSearch`. Si no existe sesión o permiso,
  Salve lo dice y no abre la interfaz para distraer.
- El registro **Ejecución → Control móvil** conserva códigos de acción
  y resultado, sin guardar artista, título, transcripción ni mensaje privado.
  Las escrituras van por un hilo de I/O.
- `PhoneActions.findApp` reconoce el paquete conocido de Spotify antes
  de enumerar el lanzador.

## Límites reales

`playFromSearch` solo confirma que Android aceptó la petición al
controlador; no verifica que Spotify haya empezado a sonar ni qué canción
eligió. Para buscar canciones mientras conduces, Salve necesita poder ver
una sesión multimedia autorizada de Spotify, normalmente con
**Acceso a notificaciones** concedido cuando el vehículo está detenido.

## Pruebas en el Galaxy S24 Ultra

1. Estacionado, abre Salve y di «abre Spotify»: la pantalla debería abrirse.
2. Di «pon música en Spotify»: debe iniciar la ruta de Spotify y no pedir
   «¿qué canción?» sin intentar una acción.
3. Pide una canción y, tras la pregunta de Salve, responde solo «Lluvia Cepeda».
4. Después di «sí, pon esa canción»: deberá usar el nombre que tú dijiste
   en el turno anterior, no inventar otro.
5. Con modo coche activo, di «abre Spotify»: debe explicar que no abrirá una
   pantalla. «Pon música...» intentará controlar el reproductor existente.
6. Comprueba **Registro de Salve → Ejecución → Control móvil**: el estado
   debe ser `MEDIA_REQUESTED`, `NO_MEDIA_SESSION_OR_PERMISSION` o
   `DRIVING_BLOCKED`, según corresponda.
7. Si falta acceso a notificaciones, concédelo desde los ajustes solo
   cuando estés estacionado y repite la prueba.

No hemos medido el funcionamiento real con Spotify ni el ASR del vehículo;
la corrección requiere validación física.
