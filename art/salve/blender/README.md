# Salve: base de Blender y referencia técnica

Paquete de trabajo para continuar el modelado 3D de Salve conservando sus 20 vistas originales. **Es una base de producción: el personaje 3D terminado y su integración en la aplicación siguen pendientes.**

## Abrir o descargar

- [Archivo de Blender](Salve_base_modelado.blend): 3 escenas, 21 imágenes empaquetadas, esqueleto inicial de 92 huesos y 6 poses de calibración.
- [Hoja técnica en PDF](Salve_hoja_tecnica_modelado.pdf): 10 páginas con vistas originales, expresiones, poses, estructura ósea propuesta, catálogo de animaciones y diagrama de estados conversacionales.
- [Lámina de expresiones y visemas](design/Salve_expresiones_propuesta.png): propuesta artística de 11 expresiones y las vocales A/E/I/O/U. Generada con ImageGen integrado a partir del rostro original; [prompt completo](design/expresiones_prompt.txt).
- [20 referencias originales](references/): PNG intactos y [manifiesto](references/manifest.json) con dimensiones y SHA-256.

En GitHub, abre el archivo y usa **Download raw file / Descargar archivo original**. El `.blend` contiene las imágenes empaquetadas: se puede descargar y abrir por sí solo con Blender 5.2.2. Para regenerar el material, descarga también el resto de la carpeta.

## Contenido de Blender

| Escena | Contenido |
| --- | --- |
| `01_REFERENCIAS_20_ORIGINALES` | Galería de las veinte imágenes originales. |
| `02_RIG_BASE_PROPUESTA` | A-pose frontal, espalda y perfil junto al esqueleto inicial. |
| `03_EXPRESIONES_PROPUESTA` | Lámina facial de referencia; aún no contiene shape keys. |

Los fotogramas 1, 31, 61, 91, 121 y 151 contienen seis poses de calibración. **No son clips de animación terminados.** La escala de 1,76 m es provisional. Los auxiliares de hombros, codos, caderas y rodillas aún requieren pesos, controladores y correctivos. Las imágenes son referencias planas y no se deforman con los huesos.

## Estado real

Construido: referencias organizadas, hoja técnica, propuesta facial, plantilla de esqueleto y poses de comprobación.

Pendiente: malla corporal y facial, retopología, UV, materiales definitivos, skinning, IK/FK, shape keys y visemas, correctivos de articulación, física de cabello/accesorios, clips completos, exportación y pruebas de rendimiento en el motor y el S24 Ultra.

La publicación de esta carpeta **no incorpora un avatar 3D a Salve ni genera un APK nuevo**. El [PR #110](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/110), ya integrado, contiene las reacciones y posturas 2D existentes. Conservamos su catálogo NEUTRAL, WARM, CURIOUS, CONCERNED, SAD, ANGRY, SURPRISED y SHY, así como los gestos LAUGH, CRY y STARTLE.

## Procedencia y verificación

Las referencias son copias byte a byte de `app/src/main/assets/avatar/core` en la revisión `1ccd26e276925b95ec6f5becd7d53070e1c5498e` de este repositorio. Se incluyen juntas para facilitar la descarga del paquete. Los recursos utilizados por la aplicación permanecen intactos.

- 20/20 hashes de las referencias coinciden con el manifiesto original.
- Archivo abierto en Blender 5.2.2 LTS; imágenes empaquetadas, 92 huesos y seis marcadores comprobados.
- 10 páginas del PDF renderizadas y revisadas visualmente.
- [Informe del archivo de Blender](blender_verification.json) e [informe de referencias](reference_verification.json).
- `SHA256SUMS.txt` permite comprobar los archivos publicados de este paquete.

No se han validado deformaciones, física ni rendimiento: requieren la malla que sigue pendiente. No se ejecuta una compilación Android para certificar estos documentos y recursos artísticos.

## Regenerar los artefactos

Dependencias: Blender 5.2.2, Python 3, `reportlab`, `Pillow` y una fuente Arial, Liberation Sans o DejaVu Sans. Ejecute desde esta carpeta:

```text
python download_references.py
blender --background --factory-startup --python build_scene.py
python build_model_sheet.py
```

`download_references.py` comprueba primero los archivos existentes y sólo descarga si faltan. `build_scene.py` debe ejecutarse como proceso de fondo independiente: restablece la escena de ese proceso y sobrescribe el archivo generado. El generador PDF también sobrescribe su salida; las fuentes y la versión de Blender pueden alterar la composición al regenerar. La lámina facial es un archivo de diseño conservado, no se regenera mediante esos comandos.
