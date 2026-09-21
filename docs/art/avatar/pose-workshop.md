# Taller de posturas articuladas

Acceso: **La habitación de Salve → Taller de posturas**. Es una actividad experimental no exportada. No sustituye el avatar conversacional ni cambia su movimiento al guardar una postura.

## Cambio

Se reutilizan las ilustraciones existentes de mangas, vestido y pelo. El rostro y las piernas proceden de la imagen original, recortados durante la carga. Las cuatro copias de texturas añaden aproximadamente 4 MB de recursos; no se incorpora otro modelo ni dependencia.

`ArticulatedPose` contiene diez ángulos acotados, cinco ejemplos, interpolación suave y JSON versionado estricto. `ArticulatedRig` calibra las capas y aplica transformaciones jerárquicas y mezcla continua de dos huesos en codos y rodillas. `ArticulatedPoseView` dibuja las mallas de 32 × 48 celdas con Canvas; carga texturas fuera del hilo de interfaz y las libera al cerrar. `AvatarPoseActivity` permite editar, guardar y recuperar una postura. Las convenciones izquierda/derecha son las de la pantalla.

La alternativa de deformar una única imagen mantiene unidos todos los píxeles, pero arrastra el torso al levantar los brazos. Separar capas permite amplitudes mayores, a cambio de tener que calibrar uniones, oclusiones y pliegues. Un cuerpo 3D con esqueleto permitiría nuevas perspectivas, pero requiere modelado y validación de recursos que todavía no existen.

## Verificación de este cambio

- 9 pruebas JUnit pasan: persistencia de todas las posturas, entradas corruptas, límites e inmutabilidad, transiciones entre todos los ejemplos, extremos temporales, pivotes compartidos, reposo sin deformación, continuidad de la mezcla y referencia del suelo.
- Nuevas clases Android compiladas para comprobar tipos contra el jar Android disponible, con R mínimo auxiliar. **Esto no equivale a compilar toda la aplicación ni a una prueba instrumentada.**
- `tools/avatar/RenderArticulatedPose.java` produce una lámina de las cinco posturas usando el mismo cálculo Java y las mismas texturas. Revisión visual realizada: se corrigieron manos ocultas por el vestido, orientación del saludo y flexión excesiva de piernas.
- No se generó un APK nuevo ni se probó en un teléfono físico en este grupo de cambios.

Para reproducir la lámina, compilar las dos clases puras y el renderizador con Gson en el classpath y ejecutar `RenderArticulatedPose <raíz-repositorio> <salida.png>`. El renderizador usa Java2D; el resultado de Canvas en Android debe verificarse por separado.

## Límites y siguientes comprobaciones

Las uniones y el sombreado de las ilustraciones aún pueden notarse. Hay deformación de volumen en flexiones grandes: compartir pivotes no conserva automáticamente el volumen de músculos o tejidos. El pelo trasero sigue la cabeza como una capa rígida; la falda no tiene simulación de tela. El suelo usa una referencia aproximada de las suelas, sin colisiones ni cinemática inversa. Los ejemplos frontales no permiten adoptar cualquier postura ni girar libremente en 3D.

Antes de llevarlo al avatar habitual: probar apertura/cierre y rotación del teléfono, restauración y guardado, cambios rápidos de sliders y presets; comprobar todos los límites de las articulaciones en Android, memoria y tiempo de fotograma en el S24; calibrar hombros, costuras y volumen; añadir control de pies y pliegues. No hay aprendizaje de movimiento ni cambios de pesos: guardar una pose conserva únicamente sus ángulos.

Referencias técnicas: [Canvas.drawBitmapMesh](https://developer.android.com/reference/android/graphics/Canvas), [relaciones padre-hijo de deformadores](https://docs.live2d.com/en/cubism-editor-manual/system-of-parent-child-relation/).
