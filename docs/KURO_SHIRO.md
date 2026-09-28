# Vestuario Kuro y Shiro

Kuro (negro) es el nuevo vestido predeterminado y la imagen base. Shiro (blanco) está incluido en el armario. Se conservan los PNG originales del usuario, su transparencia y colores.

Ambos se pueden seleccionar en la habitación y por nombre mediante AVATAR_WEAR. No ocupan la cuota de diseños personalizados ni se pueden borrar. La selección persiste en wardrobe.json versión 2. La versión 1 migra el antiguo valor original a Kuro; selecciones personalizadas permanecen intactas. Seleccionar el vestido antiguo explícitamente en versión 2 se conserva.

Las ilustraciones nuevas tienen anclajes frontales propios y no reciben la cabeza antigua. El taller experimental de articulación mantiene las capas antiguas; no se han creado capas segmentadas para los nuevos trajes. La malla sigue siendo 2D y requiere comprobación visual en Android.

Validación local: dimensiones 1024×1536, RGBA y transparencia comprobadas, identidad de los archivos de entrada y git diff --check. Se actualizaron pruebas JVM del armario y referencia del rig antiguo; no se pudieron ejecutar ni compilar Android en este entorno (sin compilador Java/SDK preparados).
