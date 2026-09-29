# Salve: revisión de fuentes y bloqueo de ejecución

> Diagnóstico inicial conservado. Actualización del 30 de septiembre: Blender quedó accesible tras la autorización del usuario. Se revisaron visualmente los veinte PNG y se construyó una versión inicial 3D. El estado actualizado y sus limitaciones están en `LEEME_Salve_entrega.md`; los bloqueos de Blender descritos abajo pertenecen al inicio de la sesión. GitHub Desktop siguió rechazando el acceso.

## Fuentes consultadas

Repositorio: https://github.com/bryanstevenriverabejarano-svg/Convert

PR #111 integrado: https://github.com/bryanstevenriverabejarano-svg/Convert/pull/111

Revisión consultada del paquete: `922495162c8fd14f8a084ca7c9d6713be8a677d9`.

Se leyeron mediante GitHub el README del paquete, el manifiesto de referencias, el código fuente del generador de la hoja técnica y el prompt de la propuesta facial. Se consultaron las descripciones de los PR #111 y #110. No se inspeccionaron visualmente los PNG ni el PDF y no se verificaron sus hashes en esta sesión.

## Especificación encontrada

El generador de la hoja describe cabello blanco largo, ojos azules luminosos, traje blanco ajustado, paneles negros, circuitos azul neón y auriculares negros con aro azul y puntas tecnológicas. La frontal establece la identidad; las vistas A frontal y trasera sirven para construcción y simetría; los laterales definen profundidad, perfil y tacón.

El manifiesto enumera 20 referencias: frontal, frontal tres cuartos, lateral derecha, trasera tres cuartos derecha, trasera, trasera tres cuartos, lateral izquierda, frontal tres cuartos contrario, variante frontal derecha, superior, inferior, A frontal, A con cabello abierto, A trasera, sentada, agachada, de rodillas, variante de rodillas derecha, inclinada y variante inclinada. No son veinte ángulos de una única pose: incluyen variantes de cabello y posturas que requieren deformación.

El README declara una base con tres escenas, 21 imágenes empaquetadas, 92 huesos y seis poses de calibración. Declara pendientes malla corporal/facial, UV, materiales definitivos, skinning, IK/FK, shape keys y correctivos. Es información de la fuente, no comprobación actual del archivo.

Expresiones del catálogo 2D: NEUTRAL, WARM, CURIOUS, CONCERNED, SAD, ANGRY, SURPRISED y SHY; gestos LAUGH, CRY y STARTLE. La propuesta facial añade mirada pensativa, guiño y vocales A/E/I/O/U. La lámina se identifica expresamente como propuesta artística, sin evidencia de aprobación independiente.

## Discrepancias y decisiones que necesitan contraste visual

- La escala de 1,76 m es provisional, no una medida canónica.
- Las ilustraciones independientes varían en volumen de cabello, paneles y apertura de brazos; necesitan reconciliarse con el frontal y las vistas A.
- Las seis poses del esqueleto no son clips terminados ni prueba de deformación.
- Los objetivos de polígonos, materiales, tiempos y física son propuestas de producción, no propiedades verificadas de un modelo final.
- Las cinco vocales no cubren todos los fonemas; el documento propone además silencio, M/B/P, F/V y lengua según el sistema de voz.

## Estado de esta sesión

El inventario de aplicaciones mostró Blender 5.2.2 LTS con `Salve_base_modelado.blend` abierto y un indicador de cambios sin guardar. No se modificó ni guardó el archivo.

La conexión Blender MCP fue rechazada con: `MCP tool call requires approval, but approval policy is never`.

El acceso a GitHub Desktop fue rechazado con: `Computer Use was not approved to use GitHub Desktop`.

La identificación del repositorio y la revisión textual están realizadas. El modelado, la inspección visual de referencias, la validación del rig y los veinte renders siguen pendientes. No se entrega un personaje terminado ni se afirma que las referencias coincidan con geometría nueva.

Para continuar hace falta habilitar el acceso autorizado a Blender y GitHub Desktop en esta sesión. No hace falta que el usuario vuelva a identificar el repositorio.
