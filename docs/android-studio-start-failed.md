# Recuperar Android Studio y mantener Dolphin

La captura muestra `DirectoryLock$CannotActivateException`: `studio64.exe` (PID 5432)
sigue ejecutándose y no responde. Es un bloqueo del IDE en Windows, antes de abrir
el proyecto. La captura no demuestra un fallo del modelo. El PR #100 y su integración
en main pasaron las comprobaciones de Android.

## En el ordenador de la captura

Cierra la ventana «Start Failed» con **Close** y ejecuta
`scripts/recuperar-android-studio.cmd` haciendo doble clic. Está preparado para la
ruta exacta de esa captura: `C:\Program Files\Android\Android Studio1\bin\studio64.exe`.

El archivo verifica nombre y ruta antes de actuar sobre el PID 5432. Solicita un
cierre normal y espera diez segundos. Si el proceso continúa sin responder, lo
termina y espera antes de solicitar un nuevo arranque. Un cierre forzado puede perder
cambios sin guardar. Si el editor responde, se detiene para que puedas guardar lo
pendiente. Si ese PID ya pertenece a otro proceso, rechaza la operación. Si ya hay
otra instancia en la misma ruta, no abre otra.

Conserva proyectos, configuración, plugins y archivos de modelos. No borra archivos
`.lock` ni cambia permisos, políticas de ejecución o ajustes globales de Windows.
No requiere credenciales ni se ejecuta automáticamente al abrir o compilar Salve.

Si la ruta o el PID del error cambian, no reutilices otro número sin verificar el
proceso. Usa Administrador de tareas → Detalles para identificar `studio64.exe`,
o envía la nueva captura. Si sigue fallando después de cerrar el proceso correcto,
reinicia Windows y conserva el nuevo error para diagnosticarlo.

Fuente del bloqueo: [JetBrains, DirectoryLock / CannotActivateException](https://youtrack.jetbrains.com/projects/WI/articles/SUPPORT-A-4102/IDE-fails-to-start-DirectoryLock-CannotActivateException-with-suppressed-BindException).

## Corrección independiente en Salve

Había una incidencia en la recuperación de Dolphin: el error persistido bloqueaba
las siguientes inferencias incluso tras una recarga nativa correcta. Ahora la opción
**Probar modelo local** permite una respuesta de comprobación y solo elimina ese error
si Dolphin devuelve texto. Cancelar, obtener una respuesta vacía o volver a fallar
conserva la evidencia de fallo. Dolphin sigue siendo el modelo principal y Gemma
continúa como respaldo tras un fallo técnico.

Además, antes de activar Gemma se comprueba de nuevo, bajo el mismo bloqueo que la
recarga de modelos, que Dolphin siga teniendo un fallo pendiente. Así un
respaldo que estaba descargándose no sustituye a un Dolphin que acaba de recuperarse.
Si Dolphin vuelve a fallar durante esa descarga, el mismo respaldo puede atender el
nuevo fallo sin perder la solicitud que WorkManager agrupa con la anterior.

Para una copia del repositorio descargada sin submódulos, antes de sincronizar Gradle:

```sh
git submodule update --init --recursive
```

Esto prepara el código de llama.cpp; no arregla el bloqueo de un proceso de Android
Studio ni descarga de nuevo los pesos de Dolphin.
