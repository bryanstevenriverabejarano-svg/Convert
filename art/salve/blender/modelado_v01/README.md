# Salve: primera reconstrucción 3D

Esta carpeta conserva la entrega local del 30 de septiembre de 2026 para continuar trabajando en Blender. **Es una versión inicial estilizada; todavía no cumple la fidelidad final a las veinte ilustraciones.** No sustituye la base original publicada en el PR #111 ni incorpora un avatar 3D en la aplicación.

## Descargar y revisar

- [Archivo Blender de trabajo](outputs/Salve_modelado_v01.blend): geometría, materiales, rig y controles faciales, junto con las tres escenas originales de referencias y plantilla.
- [Entrega completa en ZIP](outputs/Salve_entrega_v01.zip): archivo Blender, respaldo, veinte renders, veinte imágenes faciales, hojas visuales e informes. En GitHub, use **Download raw file / Descargar archivo original**.
- [Respaldo de la base abierta](outputs/Salve_base_respaldo.blend): conserva también los cambios que estaban sin guardar al comenzar la sesión.
- [Comparación de las veinte referencias con la malla](outputs/Salve_comparacion_20.png).
- [Hoja de las veinte vistas](outputs/Salve_20_vistas.png) y [hoja de expresiones y visemas](outputs/Salve_hoja_expresiones.png).
- [Instrucciones, comprobaciones y pendientes](outputs/LEEME_Salve_entrega.md).
- [Validación técnica](outputs/Salve_validacion.json), [manifest de vistas](outputs/vistas_manifest.json), [presets faciales](outputs/expresiones_controles.json) y [sumas de la entrega](outputs/SHA256SUMS.txt).

## Alcance real

La escena nueva contiene cuerpo y traje fusionados, rostro, ojos, labios, dientes, lengua, dedos, botas/tacones, cabello de mechones sólidos, auriculares, paneles negros y circuitos azules. Los materiales son procedurales. El rig contiene 100 huesos —92 de la plantilla y ocho controles adicionales— y pesos de deformación. Incluye FK, restricciones IK opcionales y controles faciales.

Se han abierto y renderizado los archivos en Blender 5.2.2 LTS. Las veinte referencias locales coinciden con los hashes del manifiesto. Se comprobaron pesos, evaluación de nueve poses y controles faciales; los cuarenta renders se decodificaron y las hojas se revisaron visualmente.

**Pendiente:** corrección artística sustancial de rostro, anatomía, manos, botas, peinado y paneles; reproducción exacta de las poses; expresiones finas, sonrojo y gesto de mano en la barbilla; retopología, UV/atlas y texturas finales; pesos revisados a mano, correctivos, calibración IK y colisiones del cabello. Las comprobaciones numéricas no certifican estas cualidades. Tampoco se certifican física, clips completos, exportación, integración Android ni rendimiento en el móvil.

La escala de 1,76 m es provisional. La propuesta facial del paquete original no tiene una aprobación artística independiente documentada.

## Fuentes y conservación

Referencias y documentación: [PR #111](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/111), revisión `922495162c8fd14f8a084ca7c9d6713be8a677d9`. Reacciones 2D: [PR #110](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/110).

`outputs/` reproduce los archivos entregados sin cambios de contenido; su `.gitattributes` evita normalizar los saltos de línea, para conservar las sumas. El ZIP también se conserva tal como se entregó. El informe de revisión de fuentes registra el bloqueo inicial y una actualización que distingue ese diagnóstico del estado posterior.

`source_scripts/` conserva las diez copias exactas de los scripts de construcción, reparación, renderizado y comprobación usados en la sesión. Son un registro de trabajo: contienen rutas absolutas del equipo original y requieren adaptarlas antes de ejecutarlos en otro ordenador. No forman una reconstrucción portátil automática ni sustituyen al archivo Blender guardado. No ejecute scripts que sobrescriben sus salidas sobre trabajo ajeno.

`publication_manifest.json` registra tamaños y SHA-256 de cada archivo de la entrega y de los scripts copiados, para verificar esta publicación.
