# Registro de ejecución de Salve

Abre **IA y cámara → Registro de ejecución**, el botón del mismo nombre en **Salve en directo**, o pide **«muestra el registro de ejecución»**. Selecciona una tarea para leer sus entradas completas o copiarlas. La pantalla se actualiza mientras está abierta y permite borrar el registro.

## Qué registra

- La petición, la interpretación aplicada y el contexto de dibujo reutilizado (objeto anterior, foto seleccionada o concepto guardado).
- Plan de investigación antes de buscar, consultas, enlaces leídos, descartes, notas de evidencia validadas y resultado. Las notas de análisis se escriben antes de pasar al siguiente enlace.
- Intentos de búsqueda de imágenes, recepción de miniaturas, disponibilidad de visión, resultado de cada proveedor y candidatos. Una ventana desactivada o un tiempo agotado no se presentan como una búsqueda realizada con éxito.
- Fuente elegida, recortes, geometría, selección de contornos, suavizado, simplificación, ajuste de curvas y dibujo publicado.
- Notas de composición y valoraciones visuales del modelo, etiquetadas como interpretaciones.

Cada entrada conserva exactamente la cadena que entrega el código que la registra, incluidos saltos de línea y espacios. Los objetos de plan/evidencia se serializan desde las estructuras que utiliza la tarea; no son una copia del mensaje bruto del modelo. No se vuelven a resumir con un modelo para mostrarlos. No se recogen prompts, memoria completa, claves, canales de razonamiento privados ni todos los procesos de la aplicación.

## Explicación del dibujo

Las peticiones directas de dibujo y «¿por qué lo dibujaste así?» reciben una explicación construida a partir del registro, sin una segunda generación que pueda inventar el procedimiento. Una huella de la geometría vincula la nota con la versión mostrada. Las historias conservan su flujo conversacional.

Se distingue la procedencia (referencia, foto, composición o repertorio), el procesamiento y la animación con Fourier. Conservar 32 líneas no implica conservar todos sus puntos: se indican ambos recuentos. El suavizado se describe como algoritmo, no como dibujo a mano alzada. Las notas antiguas sin huella no se atribuyen a una versión concreta; las operaciones que nunca se registraron no se reconstruyen por conjetura.

## Retención y límites

Registro local en `getNoBackupFilesDir()/execution-trace/execution.json`, fuera de la sincronización de recuerdos y de la copia automática de Android. [Documentación de Android](https://developer.android.com/identity/data/autobackup).

Se conservan 20 tareas, con hasta 192 entradas y 65 536 caracteres por tarea. Si se alcanza un límite, se omite la entrada completa y se muestra el número de omisiones; no se presenta una transcripción incompleta como íntegra. Si falla el guardado, la pantalla indica que hay cambios sólo en memoria. Un registro ilegible se conserva sin sobrescribir hasta que el usuario lo borra. Las tareas abiertas al interrumpirse el proceso aparecen como interrumpidas al siguiente inicio.

Los identificadores y el ámbito por hilo evitan que una respuesta tardía o una tarea en segundo plano termine en el registro de otra petición. Una tarea borrada no puede reaparecer por una devolución tardía.

## Verificación

Pruebas de persistencia literal, límites visibles, reinicio, corrupción, fallos de almacenamiento, separación de tareas, huella de geometría, notas antiguas, recuento de puntos, orden plan/lectura/análisis, aislamiento entre hilos y copia literal desde la pantalla Android. La integración completa y los APK se verifican en el flujo `Android tests and APK`.

En el teléfono: pedir un dibujo nuevo con referencias; abrir el registro mientras trabaja; revisar Google y los proveedores incluso si dan cero candidatas; pedir «¿por qué lo dibujaste así?»; iniciar otra tarea y comprobar que cada una conserva sus notas. La calidad perceptiva del dibujo y de las valoraciones del modelo sigue dependiendo del modelo y las referencias.
