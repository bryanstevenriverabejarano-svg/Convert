# Modelos, historia en pCloud y fotos del chat

## Qué cambia

- Recupera la política solicitada en #102, revertida por #103: Dolphin 8B, Dolphin 3B si falta RAM o falla el principal, Gemma como último respaldo. El motor se identifica por el archivo activado después de una inferencia real, con el motivo del cambio y el proveedor de la última respuesta.
- NDK r27 compila `salve_llama` con alineación LOAD y GNU_RELRO de 16 KB. LiteRT 1.4.2 conserva la API Interpreter. CI inspecciona **todas** las bibliotecas de 64 bits dentro del APK y su posición en el ZIP; no basta con que la biblioteca exista. Para RELRO comprueba si redondear la protección a 16 KB invade datos escribibles: un final no alineado sobre relleno vacío, sin invasión, es válido según `_phdr_table_set_gnu_relro_prot` de Bionic. No se omite ninguna biblioteca por nombre.
- La sincronización ahora descarga eventos antiguos por lotes, conserva su fecha original y los incorpora a `recuerdos`, el almacén que consultan todos los modelos. Los recibos permiten continuar tras un reinicio, sin limitar el historial a los últimos 1000 eventos ni duplicar lo ya importado.
- Los eventos que una versión anterior descargó sin indexar se reparan localmente. Los perfiles exportan el texto completo y la fecha; el worker también respalda los perfiles locales anteriores. Las revisiones y los borrados impiden que una copia antigua restaure un dato olvidado.
- Migración Room 4→5 aditiva: añade `memory_sync_state`, conserva recuerdos, grafo y diario. No borra ni sustituye la base existente.
- «Primer recuerdo conmigo» usa la cronología guardada y excluye los registros conocidos de configuración. Una recuperación pendiente se comunica como incompleta, no como una primera vivencia confirmada.
- Chat → Adjuntar ofrece galería, cámara, fotos guardadas y PDF. Foto, mensaje y etiqueta de persona se envían juntos. «Nombre» + relación «yo» + posición explícita actualizan el perfil, sin reconocimiento facial automático.
- Dolphin conserva la foto y usa las anotaciones textuales: no recibe píxeles ni se presenta como un modelo visual. La conversación sigue funcionando con la foto seleccionada. Un motor local con visión o Gemini pueden analizarla según el modo elegido; el modo local no envía la imagen a Gemini.

## Actualización y comprobación en el móvil

1. Actualizar desde la misma instalación de Android Studio / clave de firma, conservando los datos de Salve.
2. Abrir **IA y cámara → Nube pCloud → Sincronizar ahora**. La recuperación también se programa al abrir Salve. El estado indica registros recuperados, pendientes y no legibles; con mucho historial puede requerir varios lotes.
3. Esperar a que termine y preguntar «¿Cuál fue tu primer recuerdo conmigo?» y «¿Quién soy yo?». Desactivar Internet y repetir después de un cambio de modelo: el historial recuperado permanece en Room.
4. En el chat, usar **Adjuntar → Foto de la galería**, añadir nombre, relación «yo» y posición. El aviso previo indica si se analizará localmente, con Google o si solo se guardarán foto y anotaciones.
5. Comprobar el nombre del motor en el chat y **Estado de los motores**. Las descargas de modelos se conservan; un cambio por falta de RAM no borra recuerdos.

## Límites de la verificación

Las pruebas reproducen importación, cronología, perfiles corregidos/borrados, duplicados, etiquetas visuales y políticas de modelos. CI compila el APK ARM64 y comprueba ELF/ZIP. No se dispone del S24 Ultra ni de las credenciales pCloud del usuario: resta comprobar carga real y sincronización de su cuenta en el teléfono. Los eventos remotos que nunca se subieron, o los antiguos registros que guardan solo una categoría sin su valor, no permiten reconstruir información perdida. El importador no inventa su contenido ni trata resúmenes parciales del grafo como recuerdos completos.

Referencias de compilación: https://developer.android.com/guide/practices/page-sizes

Referencia del cargador para RELRO: https://github.com/aosp-mirror/platform_bionic/blob/master/linker/linker_phdr.cpp
