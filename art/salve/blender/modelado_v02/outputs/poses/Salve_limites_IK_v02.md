# Límite del cambio FK / IK

Los renders usan FK, con las cuatro propiedades IK en 0. Activar IK conserva los objetivos de mano y tobillo, pero algunas articulaciones intermedias cambian de lugar. Los valores proceden de `Salve_poses_contactos_v02.json`; la medición de 311 se repitió después de colocar la mano delante del collar y ajustar su apoyo sobre la zona frontal de la barbilla.

| Pose | Articulación | Desplazamiento al activar IK |
|---|---|---:|
| 161, rodillas inclinada | Rodilla izquierda | 25,156 mm |
| 161, rodillas inclinada | Rodilla derecha | 25,157 mm |
| 311, pensativa | Codo derecho | 6,565 mm |
| 311, pensativa | Codo izquierdo | 0,002 mm |
| 91, sentada | Rodilla izquierda | 2,309 mm |
| 91, sentada | Rodilla derecha | 1,096 mm |

En 161, los tobillos mantienen su objetivo con un error de aproximadamente 2 micras; las manos conservan sus extremos y los codos cambian sólo 0,037 mm tras el ajuste de palmas. En 311, los errores de los extremos de manos se redondean a 0,000000 m, pero el codo derecho conserva un salto de 6,565 mm. Estas transiciones necesitan corrección antes de aprobar IK para producción.

La cifra de 311 se midió en el checkpoint actualizado con contacto superficial y collar despejado, incorporado al modelo final. El PNG pensativo repetido se revisó y muestra el apoyo de la yema, dedos conectados y collar despejado. El inspector final aporta la comprobación independiente del archivo guardado. El primer ajuste de superficie dio un error de codo de 1,659 mm, pero dejaba penetraciones en el collar. El estado anterior al ajuste de mano tenía un salto de 20,793 mm. Son etapas distintas y no se deben mezclar.

El diagnóstico detallado de huesos se tomó antes de las últimas correcciones de apoyos, palmas y mano pensativa. Sus coordenadas absolutas pertenecen a esa etapa; el mismo archivo incluye aparte los errores actuales en `latest_snap_checks`.

En la medición de 161 no hay separación entre muslo y tibia: coinciden en reposo y la diferencia entre extremos en pose es menor que una micra. Las longitudes se conservan, con diferencias de unos pocos micrómetros entre FK e IK; el inicio de la cadena tampoco se desplaza. Aunque los huesos tengan `use_connect = false`, estos datos no muestran un hueco geométrico. Los bloqueos y límites IK están desactivados, la rigidez es 0 y el estiramiento está desactivado. Elevar las iteraciones de 96 a 256 no cambia el error.

Con inicio, extremo y longitudes prácticamente iguales, la rodilla se mueve alrededor del eje cadera–tobillo. El cambio de dirección del plano de flexión medido entonces es aproximadamente 3,788° en la rodilla izquierda de 161. El valor de 5,119° del codo derecho de 311 pertenece exclusivamente al diagnóstico anterior de 20,793 mm. La discrepancia se concentra en la orientación del plano de flexión y el polo; no se explica por un cambio de longitud, un límite activo ni falta de iteraciones. Esta es una inferencia geométrica de las mediciones. Aún no identifica qué convención del polo, orientación del control o transformación del solver produce el desfase.

El siguiente ajuste debe comprobar las posiciones y ejes del control de polo y la convención de orientación del solver, manteniendo la pose FK y los apoyos ya medidos. No se ha certificado continuidad de movimiento, mezclas intermedias IK/FK ni deformación general de producción.
