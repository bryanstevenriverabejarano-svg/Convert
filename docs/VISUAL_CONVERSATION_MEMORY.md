# Fotos en la conversación y memoria visual

## Uso

1. En **IA y cámara → Analizar foto**, toma una foto, escribe el mensaje en **Foto y mensaje** y pulsa **Enviar**.
2. La foto queda visible sobre la entrada del chat. Las siguientes preguntas normales de texto o voz incluyen sus píxeles mientras siga seleccionada.
3. Toca la foto para declarar el nombre, la relación contigo y qué persona es (por ejemplo, «única persona» o «izquierda»). El mismo nombre completo, ignorando mayúsculas y acentos, agrupa sus fotos. Usa nombres distintos para personas que se llamen igual.
4. **Quitar** deja de adjuntarla a las respuestas siguientes, conservando el recuerdo. **Fotos guardadas** permite volver a seleccionarla después de cerrar la aplicación. «La última foto» o una referencia inequívoca al nombre también puede recuperarla; las referencias ambiguas se resuelven eligiendo una foto en el menú.
5. Para corregir una identidad, vuelve a tocar la foto. Un nombre vacío quita la identificación, conservando imagen y análisis.
6. Con pCloud conectado y la sincronización activa, el trabajador sube las fotos guardadas y sus revisiones, incluidas las capturadas con la sincronización pausada. En otro móvil, **Fotos guardadas → Recuperar de pCloud** busca en los últimos 1000 eventos sincronizados. Se puede repetir sin duplicar recuerdos visuales.

## Memoria y grafo

- El archivo privado `filesDir/visual_memory` conserva JPEG y JSON por UUID. Cada registro contiene pregunta, fecha, análisis, revisión y etiqueta declarada por el usuario.
- La memoria consultable distingue el análisis del modelo de la declaración de identidad. Un análisis que falla queda pendiente; el texto de error no se almacena como observación visual.
- La base Room existente recibe un recuerdo con etiqueta `visual:<UUID>`, un nodo `foto:<UUID>` y, cuando el usuario identifica a alguien, un nodo `persona_confirmada:<nombre normalizado>`.
- La relación `aparece_en_segun_usuario` une persona y foto. Su narración conserva relación y posición. Corregir una foto reemplaza únicamente su relación de identidad; no elimina las relaciones de otras fotos. No se requiere migración de esquema.
- El grafo exportado incluye estos nodos y relaciones. Guardar una foto no dispara otro análisis LLM para resumir el grafo.
- El contexto reserva espacio para los datos de la foto y carga sus píxeles desde el archivo privado en cada turno. El nombre declarado tiene prioridad sobre el texto original al limitar el contexto. Si faltan los píxeles, se informa del fallo.

## pCloud

- `/Salve/images/<UUID>.jpg`: imagen. `/Salve/events/<fecha>-<id>.json`: metadatos y revisión. La carpeta se crea mediante el proveedor existente, dentro del acceso autorizado a Salve.
- El evento se marca sincronizado solamente cuando imagen y JSON han pasado la comprobación remota de checksum. WorkManager reintenta mientras queden eventos pendientes, respetando el límite existente de 20 intentos.
- Los eventos visuales tienen una representación estable para no duplicar cada revisión al recorrer el archivo. La restauración vuelve a crear recuerdos y relaciones sin reencolar el evento. Una revisión antigua no sobrescribe una corrección posterior y puede reparar una imagen ausente.
- Las imágenes se descargan mediante [`getfilelink`](https://docs.pcloud.com/methods/streaming/getfilelink.html), con URL firmada HTTPS de pCloud y límite de tamaño. No se envía el token a los servidores de contenido. `gettextfile`, que convierte codificaciones de texto, no se usa para JPEG.
- El modo local de una foto impide enviarla posteriormente a Gemini aunque se cambie de modelo. La copia de seguridad en pCloud es una preferencia independiente, indicada en el diálogo de captura.

## Alcance

Esta entrega conserva contexto visual y etiquetas verificables; no implementa reconocimiento facial ni convierte una descripción del modelo en identidad confirmada. Se etiqueta una persona por foto. No hay agrupación biométrica ni inferencias de parentesco o atributos sensibles. El nombre y relación proceden del formulario del usuario.

La cámara usa el contrato existente `TakePicturePreview`: se conserva la imagen devuelta por la cámara, no una captura de resolución completa. Máximo 1600 píxeles por lado, JPEG de 4 MiB, 200 fotos y 100 MiB de archivo. Al alcanzar el límite se rechaza una captura nueva, sin borrar recuerdos automáticamente. Quitar del chat no borra el archivo ni las versiones históricas de pCloud.

La selección activa dura la sesión; el archivo sobrevive a reinicios. Las rutas especiales de herramientas (por ejemplo, investigación web) conservan sus propios flujos. Las ediciones simultáneas de la misma foto en varios móviles no tienen resolución de conflictos: usa un dispositivo para corregir identidades. Las fotos anteriores a esta entrega cuyos píxeles nunca se guardaron deben enviarse otra vez.

## Verificación

Pruebas JVM: persistencia tras reinicio, restauración fuera de orden y reparación de píxeles, identidad explícita, correcciones y eliminación de etiquetas, referencias ambiguas, metadatos corruptos, límites y rutas; memoria/grafo sin duplicación; reserva y escape del contexto; enrutamiento local/nube; rutas de descarga y listado de pCloud.

Comprobación en dispositivo:

1. Enviar foto + «¿Qué llevo puesto?»; preguntar después «¿Y de qué color es?» sin repetirla.
2. Identificar a la persona, cerrar y abrir Salve, elegir la foto guardada y preguntar de nuevo.
3. Etiquetar dos fotos con el mismo nombre y corregir solo una; verificar ambas en el grafo.
4. Quitar la foto y comprobar que desaparece la tarjeta, pero sigue en Fotos guardadas.
5. Probar modo local sin visión: debe explicar el límite, sin enviar la foto a Gemini.
6. Pausar pCloud, capturar, reactivar, esperar subida y recuperar en otro móvil; repetir la recuperación y comprobar que no duplica fotos ni revierte correcciones.

Las pruebas JVM y la compilación no sustituyen la comprobación de cámara, inferencia real y sincronización con una cuenta pCloud en dispositivo.
