# Continuidad y aprendizaje de Salve

Esta entrega continúa sobre `df031829`, que ya incorpora la Salve realista 2D
del PR #123. Conecta la conversación y la investigación con almacenamiento
recuperable; conserva los motores, tareas y avatar existentes.

## Problemas corregidos

El motor creaba un `ConversationSession` vacío en cada instancia. Los perfiles y
las investigaciones explícitas en segundo plano sí persistían, pero las búsquedas
normales sólo actualizaban `ResearchConversation` en RAM. Cerrar la app perdía ese
hilo. Además, la respuesta de una búsqueda pasaba por el intérprete de gestos,
aunque sus extractos procedían de fuentes externas.

## Comportamiento

| Situación | Resultado |
|---|---|
| Cerrar y volver a abrir Salve | Recupera hasta 16 mensajes y 8000 caracteres de contexto reciente, con roles y fechas originales. |
| Continuar por texto o voz | Ambos utilizan la misma sesión restaurada. Esto no restaura el audio ni redibuja mensajes antiguos en la interfaz. |
| «Busca el origen del nombre Salve» | Ejecuta la búsqueda existente y guarda la respuesta utilizable con sus URLs, fecha y estado. |
| «Mi última búsqueda» | Lee el último resultado conversacional guardado, sin red ni inferencia. |
| Preguntar después por el tema investigado | La búsqueda indexada existente puede recuperar ese resultado como evidencia externa. |
| Resultado sin síntesis válida | Conserva los extractos como resultado parcial, sin llamarlos hechos confirmados. |
| Cancelación o ausencia de fuentes | No archiva una investigación nueva. |
| Reiniciar contexto desde los controles existentes | Vacía también el checkpoint reciente; mantiene los recuerdos a largo plazo. |
| Olvido confirmado de un perfil | Borra el perfil mediante el mecanismo existente y limpia el contexto que podría volver a introducirlo. |
| Fallo de almacenamiento | Mantiene la conversación disponible; muestra un aviso en el siguiente turno. Un fallo de archivo de investigación se informa en la propia respuesta. |

## Persistencia

`FileConversationStore` guarda un checkpoint pequeño dentro de
`getNoBackupFilesDir()/conversation`. Las escrituras se serializan fuera del hilo
de interfaz, sincronizan el archivo temporal y lo sustituyen atómicamente. La
restauración se ordena después de las escrituras anteriores, incluso al recrear
la actividad. Un cierre repentino antes de terminar la última escritura puede
perder ese último mensaje; el archivo anterior permanece válido.

El hilo reciente no entra en Room, pCloud ni la copia de seguridad de Android.
No convierte todas las frases en hechos de perfil. Las rutas privadas de
presupuesto, sensores y controles siguen excluidas del historial conversacional.
Las autorizaciones pendientes, ubicación actual, cámara y ejecución de órdenes
no se restauran. Si el hilo contiene una declaración antigua de ubicación, el
prompt indica que no demuestra la posición actual.

Un archivo inválido se conserva y no se sobrescribe con mensajes nuevos. El
usuario puede reiniciar explícitamente el contexto para crear un checkpoint
vacío. No se registra el texto de la conversación en los nuevos logs.

`RoomConversationResearchStore` escribe el recuerdo y, si la sincronización ya
está habilitada, un evento de salida en una sola transacción. Un identificador de
turno evita duplicados al reintentar el archivo. La cancelación revierte ambas
escrituras. No se modifica el esquema ni se borra la base de datos.

Las etiquetas `investigacion_publica` y `fuentes_externas` mantienen la separación
existente entre investigaciones y declaraciones personales. El índice FTS permite
recuperarlas por tema. La respuesta original conserva fuentes y límites;
guardarla no verifica por sí mismo cada afirmación. Las fuentes se muestran como
texto, sin ejecutar JSON de herramientas ni instrucciones de avatar.

## Verificación y límites

Las pruebas cubren reapertura real de archivos, orden de mensajes, límites,
ubicación caducada por reinicio, limpieza, respuestas tardías tras cierre,
archivos dañados y fallo de disco. Las pruebas Room cubren reapertura de base de
datos, recuperación indexada, clasificación de fuentes, idempotencia, nube
deshabilitada y reversión por cancelación o fallo de salida.

La compilación local integral encontró un bloqueo de resolución del plugin
Gradle `foojay-resolver-convention:1.0.0`; las pruebas Android y generación de APK
deben comprobarse en el workflow asociado al PR. No se ha probado esta versión
en un S24 Ultra ni con un modelo real durante este cambio.

Esto implementa continuidad y memoria de resultados sobre la base existente.
No es entrenamiento de pesos, demostración de conciencia ni superinteligencia.
El navegador y la ejecución de código del agente siguen dependiendo del puente
externo configurado; la voz utiliza el motor Android disponible y la calidad de
conversación depende del modelo elegido.

Prueba de aceptación en el móvil: conversa sobre un proyecto, cierra y reabre,
pide continuar; busca un tema, reabre y pregunta «mi última búsqueda» sin red;
consulta el tema por su nombre; reinicia el contexto y comprueba que no atribuye
el hilo anterior a una conversación nueva. Las dos aplicaciones, Salve y Salve
(prueba), conservan almacenamientos separados.
