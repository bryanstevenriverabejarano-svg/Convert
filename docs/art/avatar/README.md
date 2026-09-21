# Material para articular a Salve

El objetivo de estas capas es construir un personaje capaz de adoptar posturas, conservando su identidad. **Estos PNG son material artístico para rigging, no un modelo 3D terminado ni un conjunto de capas ya alineado para producción.** El renderer Android de esta entrega utiliza la malla continua frontal y las dos prendas completas de `app/src/main/assets/avatar/wardrobe/`.

## Recursos preparados

| Archivo | Contenido y uso |
|---|---|
| [turnaround-eight-views.png](turnaround-eight-views.png) | Frontal, tres cuartos, perfiles y espalda: ocho vistas para modelar volúmenes y partes ocultas. No son ocho poses de una animación. |
| [head-front.png](head-front.png) | Cabeza, rostro y pelo frontal. Hay que separar ojos, párpados, cejas y boca para expresiones amplias. |
| [hair-back.png](hair-back.png) | Pelo trasero completo, incluyendo superficie antes oculta. |
| [torso-dress-front.png](torso-dress-front.png) | Torso vestido sin mangas, con zonas cubiertas antes por pelo y brazos. |
| [arm-screen-left.png](arm-screen-left.png) | Manga, brazo y mano del lado izquierdo de la imagen. |
| [arm-screen-right.png](arm-screen-right.png) | Manga, brazo y mano del lado derecho de la imagen. |
| [pajama-lower-body.png](pajama-lower-body.png) | Pantalón y zapatillas completos; material del pijama para cadera, rodillas y tobillos. |
| [parts-three-quarter.png](parts-three-quarter.png) | Atlas de piezas de tres cuartos: cabeza, pelo, cuerpo vestido, falda, mangas/manos y botas. |
| [parts-profile.png](parts-profile.png) | Atlas equivalente con cabeza y botas de perfil. La ropa aún necesita corregir su profundidad y orientación durante el modelado. |
| [parts-back.png](parts-back.png) | Piezas de espalda, incluyendo superficies antes ocultas por el pelo. |

Los seis PNG de capas tienen lienzo 1024 × 1536 y transparencia real; la lámina de ocho vistas mide 1448 × 1086. Se conservaron los archivos generados, sin recortarlos ni recomprimirlos. Las dimensiones solicitadas al generador no siempre se cumplieron: la geometría debe partir de las dimensiones medidas en [assets.json](assets.json), que también contiene huellas SHA-256 y límites visibles.

La referencia definitiva de cara, proporciones y vestido original sigue siendo [salve_imagen.png](../../../app/src/main/res/drawable/salve_imagen.png). Las capas generadas amplían material oculto, pero **no reproducen exactamente sus píxeles ni sus posiciones**. El primer torso generado incluía mangas y fue sustituido por la versión sin mangas.

Los tres atlas contienen ocho piezas cada uno, en dos filas, sobre un lienzo 1536 × 1024. Cada pieza está a su propia escala para facilitar su revisión. No se puede usar una celda directamente como parte articulada sin extraer su contorno, ajustar la escala y vincularla al hueso correspondiente. Las vistas son guías artísticas aproximadas: no garantizan consistencia geométrica de una reconstrucción 3D.

## Cómo convertirlo en un cuerpo articulado

[rigging-plan.json](rigging-plan.json) propone la jerarquía pelvis → torso → cuello/cabeza y hombro → codo → muñeca, junto con cadera → rodilla → tobillo. Incluye anclajes de referencia y estimaciones en las capas nuevas. Su estado es `artwork_prepared_rig_not_built`: las cifras son puntos de partida para ajustar, no datos exportados de un rig validado.

1. Ajustar cada capa a los anclajes del mismo personaje; reconstruir solapes de hombros, codos, caderas y rodillas. Separar el pantalón en las dos extremidades.
2. Construir mallas y pesos suaves que compartan la transformación de padres e hijos. Los recortes rígidos aislados volverían a producir el problema que motivó este cambio.
3. Preparar las superficies que aparecen al girar, doblar o cruzar extremidades. Una mano frontal no contiene la palma ni todos los dedos desde cualquier perspectiva.
4. Para posturas desde cualquier ángulo, crear una malla 3D con esqueleto y texturas usando las ocho vistas. Añadir articulación de dedos, formas faciales, límites, contacto de pies y colisiones de ropa/pelo.
5. Exportar un GLB validado y conectarlo a un renderer Android real. Probar levantar brazos, sentarse, agacharse, caminar, girar y acostarse antes de sustituir el personaje en la app.

Un personaje completo tampoco puede realizar literalmente cualquier postura sin restricciones anatómicas o de colisión. El objetivo técnico es un repertorio amplio construido sobre articulaciones coherentes, con control de poses y transiciones.

## Conservación y generación

Se utilizó el generador de imágenes integrado (`image_gen`), con la ilustración original como referencia. Los prompts seleccionados y correcciones están en [layer-generation-prompts.json](../../evidence/layer-generation-prompts.json); los de las prendas completas en [wardrobe-generation-prompts.json](../../evidence/wardrobe-generation-prompts.json).

El material de esta carpeta queda fuera del APK. Sólo se empaquetan las dos prendas frontales usadas actualmente. Añadir vistas de referencia al teléfono no le daría por sí solo capacidad de adoptar esas poses.

Fuentes técnicas: [separación de material en Cubism](https://docs.live2d.com/en/cubism-editor-manual/divide-the-material/), [jerarquías padre/hijo](https://docs.live2d.com/en/cubism-editor-manual/system-of-parent-child-relation/) y [skinning en glTF](https://registry.khronos.org/glTF/specs/2.0/glTF-2.0.html#skins).
