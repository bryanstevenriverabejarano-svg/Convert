# Panel móvil de Salve

Ventana flotante con navegación web real y controles revisados sobre la aplicación visible. Es un avance de autonomía acotada; no es un controlador universal del móvil ni ejecuta cualquier objetivo dentro de todas las aplicaciones.

## Uso

- Chat: `abre tu panel flotante`.
- Búsqueda visible: `busca en una mini ventana: en Google qué es la colada`.
- Borrador: `prepara un WhatsApp a +34612345678: Hola, llego a las ocho.`. Se revisan número y texto antes de abrir WhatsApp; preparar no envía.
- Menú **Mi móvil → Abrir panel flotante**. Android puede pedir **Mostrar sobre otras apps**.
- **IA y cámara → Buscadores web → Abrir última búsqueda → Mini ventana de Salve** permite abrir la búsqueda del lector público.

Se arrastra por el título, se minimiza con **−** y se cierra con **×** o desde la notificación. Se oculta y pausa al bloquear el móvil. No se reinicia solo tras morir el proceso. No cambia el avatar existente.

## Web y recuperación

Android WebView permite JavaScript y navegación visible, con el buscador elegido, un buscador personalizado HTTPS con `{query}`, o una URL HTTPS directa. Bing muestra HTML, no RSS. Se pueden abrir enlaces y pasar la página al navegador del teléfono.

La búsqueda inicial puede esperar una conexión validada. Se recupera un GET sin interacción hasta dos veces, con 25 segundos por intento y máximo dos minutos desde la solicitud. Cada intento sustituye la instancia anterior de WebView para aislar callbacks antiguos. Tocar la página, seguir otro destino o detectar un método distinto de GET elimina la repetición automática. Los errores HTTP/TLS y la caída del renderizador detienen la recuperación. Los formularios no se reenvían automáticamente. Tras un cierre de WebView, otra búsqueda explícita crea la nueva instancia.

Los sitios pueden exigir consentimiento, CAPTCHA, inicio de sesión o navegador externo; el panel no los omite. Mostrar la página no significa que el LLM la haya leído o verificado. El lector público del chat y sus citas siguen funcionando por separado. No se envían páginas autenticadas ni cookies al LLM o pCloud.

No hay interfaz JavaScript hacia Android ni acceso a archivos o content URIs. Se rechazan esquemas de apps, HTTP, autoridades con credenciales y destinos locales literales/reservados. WebView usa la resolución/red de Android: su comprobación de URL no equivale al fijado de DNS del lector HTTP. Archivos y permisos web de cámara/micrófono se dejan al navegador externo.

## Acciones sobre el móvil

En **Móvil**, prepara el texto, toca el campo de la otra app y pulsa **Revisar escritura**. La tarjeta muestra el contenido exacto antes de reemplazar ese campo. **Revisar toque** busca una etiqueta exacta y única. Si eliges `Enviar`, el toque confirmado puede enviar el mensaje: revisa el destinatario y texto visibles en WhatsApp. No se adivinan contactos por nombre.

Requiere **Sistema Motor de Salve** en Accesibilidad y controles activos en Mi móvil. El panel se retira temporalmente para restaurar el foco de la app de destino. Se mantienen las comprobaciones existentes de paquete, ventana, huella del contenido, campo y caducidad. Pantalla cambiada, bloqueo, etiqueta ambigua o interrupción requieren volver a preparar/revisar; una escritura o envío nunca se repite automáticamente. Que Android acepte el toque no prueba que el mensaje se haya entregado.

Hay accesos **Atrás**, **Inicio**, **Recientes** y **Leer pantalla actual**. Esta última lee explícitamente el árbol accesible y muestra el texto solo en memoria, dentro del panel. No hay lectura continua ni almacenamiento del texto en nube. Las apps que no exponen controles accesibles pueden no ser compatibles; no se sortean permisos ni pantallas protegidas.

## Verificación y límites

15 pruebas JUnit locales pasan y cubren comandos, URLs, timeouts, límite de reintentos, pausa, métodos, redirecciones y navegación manual después de un error. El servicio flotante, la actividad de dispositivos, el servicio de accesibilidad y sus dependencias directas compilan localmente contra las API Android con Java 11.

Se han añadido pruebas Robolectric para creación/cierre, permisos WebView, retirada tras caída del renderizador y no reenvío de formularios. La PR ejecuta la suite Android completa y genera los APK Debug/Preview ARM64. El estado de CI y los enlaces a los APK se registran en la PR; la compilación parcial local no sustituye ese resultado.

No hay acceso al S24 Ultra desde este entorno. Queda comprobar en el dispositivo el teclado flotante, accesibilidad, confirmación sobre la versión instalada de WhatsApp, Google/consentimiento y corte real de Wi-Fi. No se han enviado mensajes durante las pruebas.

Referencias: [WindowManager](https://developer.android.com/reference/android/view/WindowManager.LayoutParams), [WebViewClient](https://developer.android.com/reference/android/webkit/WebViewClient).
