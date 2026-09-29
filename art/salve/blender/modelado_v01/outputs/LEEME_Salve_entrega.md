# Salve: versión inicial de modelado 3D

Fecha: 30 de septiembre de 2026.

**Estado: reconstrucción estilizada inicial, no modelo final aprobado.** El archivo ya contiene geometría 3D, materiales, skinning y controles; la comparación visual revela diferencias importantes frente a las referencias. No se cumple todavía la definición de completado del encargo.

## Archivos

- `Salve_modelado_v01.blend`: archivo de trabajo con las tres escenas originales y una cuarta escena nueva `04_SALVE_MODELO_3D`.
- `Salve_base_respaldo.blend`: copia del estado abierto original, incluidos sus cambios sin guardar. No se sobrescribió el archivo original.
- `vistas_20/`: veinte renders numerados según el manifiesto original; algunos corresponden a poses, no sólo a cámaras.
- `expresiones/`: veinte renders faciales con emociones, gestos y vocales.
- `Salve_20_vistas.png`: hoja general de las veinte imágenes.
- `Salve_hoja_expresiones.png`: hoja de expresiones y visemas.
- `Salve_comparacion_20.png`: original a la izquierda y reconstrucción a la derecha en cada par.
- `Salve_validacion.json`: comprobaciones técnicas y limitaciones.
- `expresiones_controles.json` y `vistas_manifest.json`: correspondencia de controles, fotogramas y cámaras.

## Qué se ha construido

Cuerpo y traje blanco mediante una superficie fusionada, rostro con ojos independientes, boca con labios, dientes y lengua, dedos, botas y tacones, cabello de mechones sólidos, auriculares con puntas y aros, paneles negros y circuitos azules. Los materiales son procedurales editables; no se han creado texturas pintadas definitivas.

Rig de 100 huesos: los 92 de la plantilla más ocho controles. La malla tiene pesos; hay controles FK y restricciones IK opcionales. El rig no se presenta como un sistema IK/FK de producción terminado. Se conserva el esqueleto original por separado.

Controles faciales: parpadeo independiente, cejas, apertura de boca, sonrisa, fruncido, anchura y redondeo de labios, más lágrimas. Presets: NEUTRAL, WARM, CURIOUS, CONCERNED, SAD, ANGRY, SURPRISED, SHY, LAUGH, CRY, STARTLE, THINK, WINK, A, E, I, O, U, MBP y SILENCE. Son representaciones iniciales y no equivalentes exactos de la lámina artística.

## Abrir y continuar

Abra `Salve_modelado_v01.blend` en Blender 5.2.2. La escena `04_SALVE_MODELO_3D` contiene seis colecciones: cuerpo/traje, rostro, cabello, tecnología, rig y estudio.

Las poses de comparación están en los fotogramas 1, 31, 61, 91, 121, 151, 161, 171 y 181. Las expresiones están en los marcadores `FACE_...` desde el fotograma 201. Para editar controles, muestre `Salve_Rig` en la colección 05 y use sus propiedades personalizadas. Las propiedades animadas se evalúan según el fotograma; quite o edite sus claves para controlar una pose manualmente. La influencia IK es cero por defecto; sus ángulos de polo requieren calibración.

## Comprobaciones realizadas

El archivo guardado se reabrió en un proceso independiente de Blender 5.2.2 para producir las imágenes. Los veinte PNG originales locales coinciden con los SHA-256 del manifiesto. Los pesos de las mallas vinculadas no dejan vértices sin influencia. Se comprobaron desplazamientos no nulos en seis controles faciales y se corrigió la actualización de controles al renderizar las expresiones.

La evaluación numérica de nueve poses no produjo vértices inválidos. Esto no certifica ausencia de intersecciones entre superficies ni deformaciones correctas. Los cuarenta renders se decodificaron y las hojas se revisaron visualmente.

## Pendientes frente al encargo

- Ajuste artístico sustancial del rostro, proporciones, manos, botas, cabello y patrón de paneles; la similitud con las ilustraciones es insuficiente para aprobar el modelo final.
- Refinar todas las expresiones, incluido el sonrojo de timidez y el gesto de mano en la barbilla de la referencia pensativa.
- Reproducir exactamente los contactos y posturas de las ilustraciones de sentada, cuclillas, rodillas e inclinada; las poses incluidas son aproximaciones.
- Retopología de producción, UV desplegadas, atlas y texturas definitivas. La malla corporal fusionada es una base de trabajo y las UV iniciales no son un desplegado final.
- Pesos revisados a mano, correctivos de hombros/caderas/rodillas/codos, calibración completa de IK, controladores finales y colisiones del cabello.
- Física, animaciones completas, exportación e integración en motor si se decide incluirlas posteriormente. No se han certificado ni forman parte de los renders estáticos.

## Fuentes y decisiones

[Convert PR #111](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/111), revisión del paquete `922495162c8fd14f8a084ca7c9d6713be8a677d9`: README, manifiesto, generador de hoja técnica, propuesta facial y archivos locales originales. Se leyeron también la descripción del PR #110 y la discusión/revisiones del #111; las dos últimas no contenían entradas. La integración del PR no constituye una aprobación artística independiente de la lámina facial.

La frontal y las vistas A se tomaron como base; las variantes de cabello y paneles se reconciliaron de forma aproximada. La altura de 1,76 m sigue siendo provisional. No se inventaron fichas ni medidas canónicas. No se revisó visualmente el PDF original: se consultó el código fuente completo que genera sus contenidos.

Blender MCP quedó operativo después de la autorización del usuario. GitHub Desktop siguió rechazando el control automático; las consultas se realizaron mediante el conector GitHub. No se fusionaron PR ni se publicaron cambios. Los generadores 3D externos estaban deshabilitados y no tenían claves configuradas; no se activaron ni se contrataron servicios.
