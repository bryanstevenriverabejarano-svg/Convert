# Búsqueda pública y recuperación de recuerdos

## Problema observado

La captura de «Qué es la colada?» muestra un fallo web con Gemma activo. El enrutador anterior mandaba automáticamente cualquier definición («qué es», «qué significa», «quién es») al lector de Wikipedia. Una definición cotidiana ahora continúa por el modelo conversacional con su memoria. La captura no contiene el error de transporte, por lo que no demuestra falta de red, bloqueo del proveedor o fallo DNS concreto.

## Buscadores y lectura

- **IA y cámara → Buscadores web** permite elegir Google, Bing, DuckDuckGo, Wikipedia, modo automático u otra URL HTTPS con `{query}` en el parámetro de búsqueda.
- «Busca en Google…», «Busca en Bing…» y «Busca en DuckDuckGo…» seleccionan explícitamente ese proveedor. Una selección explícita no se cambia a escondidas por otro buscador.
- El modo automático prueba Google → Bing → DuckDuckGo. Wikipedia queda como respaldo identificado si no obtiene páginas utilizables.
- Se extraen enlaces de resultados públicos y se leen hasta tres páginas HTTPS. El resultado del buscador no se presenta como si se hubiese leído el artículo. Se conservan límites de bytes, tiempo, redirecciones y validación del destino público.
- La consulta pública recibe el tema solicitado, no los recuerdos personales de la conversación. Los resultados siguen siendo datos que el modelo debe citar; no son instrucciones para ejecutar acciones.
- El lector de Wikipedia usa búsqueda de texto completo y extractos JSON. Analiza el JSON completo antes de recortar cada extracto; evita buscar URLs mediante expresiones regulares sobre un JSON truncado.
- Los errores HTTP y bloqueos identificables aparecen en el diagnóstico. **Abrir última búsqueda** permite continuar en el navegador del móvil.

Esto no es un controlador completo de Chrome ni un renderizador JavaScript en el móvil. Las páginas dinámicas, consentimientos, accesos con cuenta y CAPTCHA pueden requerir intervención en el navegador. Configurar otro buscador permite intentar su página pública; no garantiza compatibilidad con cualquier diseño web. Salve no lee automáticamente una sesión privada del navegador. El puente de herramientas existente sigue siendo opcional.

## Memoria pCloud

- La conexión elegida sigue siendo pCloud; no se envían recuerdos al servidor Namecheap de ejemplo.
- OAuth comprueba `userinfo` antes de guardar una nueva autorización. Cancelar antes de guardarla conserva la anterior. El token continúa cifrado con Android Keystore.
- **Nube pCloud → Comprobar conexión** hace una lectura autenticada real y distingue autorización inválida/caducada, permisos, limitación del proveedor y fallo de conexión. Tener un token guardado no se anuncia como conexión comprobada.
- El worker recupera primero los eventos antiguos y los importa al almacén Room común a los modelos. Las fechas, etiquetas de origen, deduplicación y protección frente a resucitar perfiles olvidados se conservan.
- Una revisión paginada y reiniciable copia a la cola los recuerdos locales escritos durante una pausa, además de perfiles y fotos. Los registros ya recuperados de pCloud no se vuelven a enviar por esta revisión.
- Los eventos con veinte intentos fallidos permanecen pendientes. Los nuevos eventos tienen oportunidad de enviarse antes que los que fallan reiteradamente; no se elimina el diario local.
- «Recupera tus recuerdos» solicita el trabajo de recuperación cuando pCloud está conectado y activo. «Estado de tus recuerdos» muestra su progreso real. La pausa se conserva hasta reactivarla desde el menú.

## Validación y límites

Las pruebas incluyen la frase de la captura, selección de buscador, enlaces codificados, redirecciones a destinos privados rechazadas, Google 429 seguido de resultados de Bing, rechazo de CAPTCHA como evidencia, JSON largo/Unicode, cancelación y errores HTTP. Las pruebas Room cubren cola durante pausa, reanudación tras reiniciar, duplicados, veinte reintentos y recuperación al almacén consultado por los modelos.

Sondeo público desde el entorno de desarrollo el 12/10/2026: Wikipedia devolvió HTTP 200 con resultados para «colada»; Google devolvió 429, Bing 200 y DuckDuckGo 202. Recibir un HTTP exitoso no demuestra que un buscador proporcione resultados utilizables. No se ha accedido a la cuenta pCloud del usuario ni al S24 Ultra. La verificación completa de cuenta, subida con checksum, reinicio y recuperación debe ejecutarse en el móvil desde la nueva pantalla.

Referencias de implementación: [MediaWiki Search](https://www.mediawiki.org/wiki/API:Search), [TextExtracts](https://www.mediawiki.org/wiki/Extension:TextExtracts), [pCloud userinfo](https://docs.pcloud.com/methods/general/userinfo.html), [pCloud getfilelink](https://docs.pcloud.com/methods/streaming/getfilelink.html).
