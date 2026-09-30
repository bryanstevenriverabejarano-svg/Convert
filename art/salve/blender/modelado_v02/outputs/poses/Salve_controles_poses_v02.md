# Poses y controles de Salve v02

La biblioteca conserva los nombres, posiciones de reposo y jerarquía del esqueleto existente. Las poses se guardan como fotogramas estáticos con interpolación constante. No representan una animación continua.

| Fotograma | Pose |
|---:|---|
| 1 | Frontal neutral de pie |
| 11 | Frontal tres cuartos, apoyo asimétrico |
| 21 | Frontal opuesta, apoyo asimétrico |
| 31 | A |
| 41 | Frontal variante |
| 61 | A con cabello y brazos abiertos |
| 91 | Sentada, una pierna elevada y otra plegada |
| 121 | Cuclillas |
| 151 | Rodillas, pelvis baja y tibias hacia atrás |
| 161 | Rodillas inclinada, ambas manos en suelo |
| 171 | Inclinada |
| 181 | Inclinada variante |
| 311 | Pensativa, índice derecho sobre la zona frontal de la barbilla |

Para editar, mostrar y seleccionar `Salve_Rig` en la colección del rig. Los controles de mano y pie son `CTRL_hand.L/R` y `CTRL_foot.L/R`; los polos de codo y rodilla son `CTRL_elbow_pole.L/R` y `CTRL_knee_pole.L/R`. `.L` y `.R` son lados anatómicos; no significan izquierda y derecha de la imagen.

Las propiedades `IK_hand.L/R` e `IK_foot.L/R` van de 0 (FK) a 1 (IK). Los renders estáticos usan 0. El objetivo de cada controlador coincide con el extremo FK del fotograma; la orientación de la palma o suela sigue el controlador al activar IK. El estiramiento está desactivado. Los ángulos de polo están guardados individualmente por pose.

Al cambiar de fotograma se recuperan los valores animados. Una modificación manual sólo permanece en esa sesión hasta que se inserte un nuevo fotograma clave o se desactive la acción. Cambiar de FK a IK puede alterar la posición de codos y rodillas aun cuando el objetivo permanezca en su sitio. Los saltos medidos de rodillas en 161 y codo derecho en 311 son aproximadamente 25,16 y 6,57 mm: estas transiciones necesitan corrección y no se consideran listas para producción. La cifra actual de 311 se midió en el checkpoint con mano delante del collar, incorporado al modelo final. El PNG pensativo repetido ya se revisó: el apoyo se lee y no se ven fragmentos aislados sobre el cuello. El inspector final aporta la comprobación independiente del archivo guardado.

Los correctivos `PoseClearance_*` pertenecen a los mechones y surcos. Son claves de forma editables que se activan en cada fotograma estático; la corrección neutral también se usa en las expresiones de pie. El ajuste de la masa larga detrás de los hombros está incorporado en Basis. Este método no simula gravedad ni colisiones dinámicas.

La pose 311 usa además `THINKSkin_311` en los cinco dedos y uñas derechos. Estas claves suavizan el barrido estático de falanges y colocan las uñas sobre su superficie; se activan sólo en 311. La muñeca se inclina hacia delante para despejar el collar mientras la yema se aproxima a 1 mm de la barbilla frontal. El mechón facial `Mechon_rostro_00.R` tiene un ajuste local específico en `PoseClearance_311`. Son correcciones de esta pose, no una validación general de todos los movimientos de mano.

El plano de apoyo se evalúa en Z = 0 metros. El informe de contactos mide vértices de la malla deformada y distingue pies, regiones de rodilla y manos. Un mínimo próximo al suelo no prueba por sí solo que toda la palma, suela o rodilla tenga un apoyo correcto. El informe de cabello comprueba cruces de superficies contra el traje; quedan fuera los contactos intencionales con cuero cabelludo y los volúmenes completamente encerrados que no cruzan una superficie.

Consultar los informes numéricos y la revisión visual de las seis poses. Persisten diferencias respecto de las ilustraciones y las verificaciones pendientes deben resolverse antes de aprobar el modelo para producción.
