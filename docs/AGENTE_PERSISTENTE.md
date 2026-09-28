# Primera entrega: memoria protegida e investigación recuperable

## Resultado implementado

Esta entrega conecta una petición explícita a una investigación pública real, un checkpoint de fuentes y un recuerdo persistente. La tarea se conserva en Room y WorkManager la reanuda cuando dispone de red y batería suficiente. Usa el lector y el coordinador de investigación existentes: URLs HTTPS públicas y descubrimiento mediante Wikipedia. La síntesis se ejecuta exclusivamente con el proveedor local configurado; no se añade un envío automático a Gemini.

No es todavía un planificador general de herramientas ni un navegador DOM. Las búsquedas conversacionales normales conservan su ruta inmediata. La nueva ruta persistente se solicita explícitamente para que el usuario sepa que continuará en segundo plano y archivará su resultado.

## Uso por chat o voz

| Petición | Resultado |
|---|---|
| `investiga y recuerda: origen del nombre Salve` | Crea una tarea persistente sin volver a pedir permiso |
| `investiga en segundo plano: https://example.org/Topic` | Conserva la URL original y prepara una lectura pública |
| `mis tareas` | Muestra las diez tareas más recientes y sus identificadores |
| `resultado de mi última tarea` | Recupera respuesta, fuentes y proveedores usados; permite continuar hablando sobre ellas |
| `resultado tarea 12345678` | Consulta una tarea concreta usando su identificador mostrado |
| `cancela tarea 12345678` | Invalida al ejecutor; sus resultados tardíos no se archivan |
| `reanuda tarea 12345678` | Renueva el presupuesto de una tarea fallida o cancelada |
| `pausa tus tareas` / `reanuda tus tareas` | Control persistente de las investigaciones |
| `pausa tu autonomía` / `reanuda tu autonomía` | También aplica a estas tareas, además del ciclo de objetivos |

No hay voz espontánea ni notificaciones nuevas en segundo plano. El resultado se consulta desde el chat. El texto de las fuentes nunca se interpreta como comandos, JSON de herramientas o instrucciones del avatar.

## Estado y garantías

`QUEUED → RUNNING → STAGED → SUCCEEDED/PARTIAL`

Los errores pueden llevar a una nueva espera o a `FAILED`; el usuario puede cancelar. `SUCCEEDED` significa que el coordinador produjo su respuesta con fuentes y que el archivo local terminó. No certifica la verdad semántica de cada frase. Los extractos sin síntesis y las respuestas incompletas se marcan `PARTIAL`.

- Hasta 20 tareas pendientes y tres intentos automáticos de investigación por tarea; cada intento conserva los límites del coordinador (tres búsquedas y cuatro llamadas al modelo).
- Una concesión de ejecución de 15 minutos identifica al propietario. Otro proceso puede recuperarla al expirar. Una pausa o cancelación retira el propietario inmediatamente en la transacción de Room.
- Al crear el control de tareas por primera vez se conserva una pausa de autonomía ya guardada en la instalación. Los cambios posteriores se conservan en Room y no se sobrescriben al abrir Salve.
- Las lecturas públicas pueden repetirse después de una caída anterior al checkpoint. No se prometen efectos externos «exactamente una vez».
- Tras el checkpoint `STAGED`, se reutiliza el recibo y no se repite la investigación. La finalización, el recuerdo y el evento de salida pCloud se confirman en **una transacción**. Si falla la escritura, todo se revierte.
- La programación inicial puede fallar después de guardar la tarea. Se informa y el arranque siguiente vuelve a programarla. WorkManager recupera trabajos ya encolados tras reinicios; una detención forzada por Android puede requerir abrir la aplicación.
- Reanudar explícitamente encadena un sucesor con `APPEND_OR_REPLACE`, evitando perder el despertar si el worker anterior aún está terminando. La recuperación ordinaria usa `KEEP`.
- La conversación tiene prioridad antes de cargar un paso nuevo y entre llamadas. La inferencia nativa ya iniciada puede tardar en terminar; esta entrega no promete preempción instantánea.
- El diario conserva aproximadamente las últimas 50 tareas finalizadas, además de las pendientes. La limpieza del diario no borra los recuerdos archivados.

## Memoria y pCloud

Los recuerdos se etiquetan `investigacion_publica`, `fuentes_externas` y `task:<id>`. Se conserva la pregunta y la respuesta con sus URLs. El recibo local conserva también extractos y el proveedor realmente devuelto por cada inferencia, su estado y duración. Una inferencia fallida sin proveedor confirmado se identifica como tal; no se deduce que la ejecutó un modelo concreto.

No se crean perfiles personales desde resultados web. Si pCloud está habilitado al archivar, el evento se guarda atómicamente en la cola existente y SyncWorker lo enviará. Con pCloud deshabilitado se archiva sólo localmente; esta entrega no añade una subida retroactiva de esos resultados al activar la nube después. Las credenciales continúan fuera del prompt y del diario de tareas. El diario operativo no se restaura en otro teléfono: sí se restaura el recuerdo sincronizado mediante el importador existente.

## Actualizaciones de Room

La base pasa a versión 6. Las rutas comprobables son `4 → 5 → 6` y `5 → 6`. La versión 4 está en el commit inicial `b479065`; los tipos/campos de sus siete entidades se conservan. Se elimina `fallbackToDestructiveMigration`, también para evitar borrados ante futuras rutas olvidadas o downgrades. Una versión desconocida falla al abrir sin reconstruir la base: requiere una migración basada en su esquema real, no borrar datos de la app.

Se activa la exportación de esquemas Room. El fixture de pruebas `memory-v4.sql` documenta el esquema histórico; las pruebas abren el archivo antiguo con el builder de producción y ejecutan la validación real de Room. El marcador v3 en una prueba es deliberadamente una versión no admitida, no una reconstrucción inventada de un esquema histórico.

`app/schemas/salve.data.db.MemoriaDatabase/6.json` es el esquema generado por el compilador de Room en CI, conservado en control de versiones.

## Evaluación

- JUnit puro: autorización explícita del comando, acentos, URLs sensibles a mayúsculas, contratos y límites de recibos.
- Robolectric + Room: migraciones, conservación de perfiles/grafo/outbox, reinicio con checkpoint, exclusión entre ejecutores, cancelación, pausa, presupuesto persistente, resultados parciales y fallo inyectado de escritura con rollback completo.
- CI habitual: todas las pruebas JVM/Python, APK ARM64, firma/alineación nativa y evaluaciones del laboratorio de algoritmos.
- La calidad de respuestas Dolphin, batería/temperatura, ciclo real de Android y subida/restauración con OAuth requieren prueba en el S24 Ultra. Las pruebas de persistencia usan fuentes y modelos controlados; no acreditan inteligencia general.

## Continuación de la arquitectura

La [segunda entrega de recuperación de memoria](MEMORIA_INDEXADA.md) implementa búsqueda local indexada, expansión controlada de vocabulario, procedencia y propagación al índice de correcciones/borrados. La búsqueda vectorial y el historial bitemporal siguen pendientes.

1. Ampliar MemoryService con búsqueda híbrida, temporalidad, contradicciones y borrado propagado.
2. Generalizar TaskStore a planes con ejecutores tipados y un PolicyEngine común; conservar checkpoints y recibos.
3. Añadir búsqueda general y navegador externo, seguido de código aislado y APIs.
4. Incorporar suscripciones a eventos autorizados y aprendizaje evaluado con casos reservados.

Los modelos, el avatar, la identidad y la memoria siguen siendo componentes independientes. Esta entrega permite ejecutar y recuperar una tarea acotada real como base de esa evolución.

La [entrega de planes y eventos](AGENTE_PLANIFICADOR.md) conecta el modelo local con herramientas, revisión/corrección, diario por paso y vigilancias autorizadas. Incluye un estado explícito del programa completo y del despliegue pendiente.
