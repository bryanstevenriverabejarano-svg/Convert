# Respaldo recuperable de Salve V02

Este snapshot conserva el avance del 30 de septiembre de 2026. La identidad
ilustrativa exacta y la preparación final de producción siguen pendientes.
No sustituye los recursos aprobados de Salve ni incorpora el candidato al producto.

## Abrir el trabajo

Abre `outputs/Salve_refinado_v02.blend` con Blender 5.2.2 LTS, escena
`04_SALVE_MODELO_3D`. Es el modelo V02 editable auditado y abierto en Blender
según `outputs/Salve_apertura_GUI_v02.json`; conserva materiales y texturas
empaquetadas. Consulta el informe y la guía de controles dentro de `outputs`.

`outputs/Salve_sesion_respaldo_2026-09-30.blend` es únicamente la copia adicional
de la sesión GUI abierta, acompañada por su JSON. No es una versión nueva
aprobada y no reemplaza V02 ni los PNG ya revisados. También se conservan
`.blend1` y los dos respaldos GUI anteriores, que el ZIP de entrega excluye.

Hay 20 renders corporales transparentes y 21 retratos de expresiones, sus
comparaciones y revisiones. Son un avance con diferencias documentadas, sin
aceptación artística final ni certificación de producción. La limpieza facial
rechazada se registra como intento no aplicado; no se deben repetir sus helpers
suponiendo que forman parte del modelo entregado.

## Integridad de los archivos

Todos los archivos de `outputs` se copian byte por byte. En particular,
`outputs/Salve_entrega_v02.zip` se conserva intacto mediante Git LFS: este script
no lo reconstruye. `outputs/SHA256SUMS_v02.txt` corresponde al paquete original
de entrega; no se regenera y no cubre los respaldos GUI o la nueva sesión.

`SHA256SUMS_respaldo.txt` cubre el respaldo ampliado, su manifiesto y este README
con rutas relativas a esta carpeta. Excluye únicamente su propio archivo para
evitar autorreferencia. `BACKUP_MANIFEST.json` vincula cada archivo copiado con
su ruta original, ruta en el repositorio, tamaño y SHA-256, y registra que los
orígenes y el árbol histórico permanecieron intactos antes y después de copiar.
El manifiesto no intenta incluir el hash de sí mismo o del listado de hashes;
su hash sí queda cubierto por `SHA256SUMS_respaldo.txt`.

El escáner básico revisa textos y textos dentro del ZIP para credenciales
reconocibles y rechaza cachés, ejecutables, llaves y rutas ajenas al contrato.
No representa una auditoría exhaustiva de secretos dentro de binarios.

## Fuentes históricas y restauración

El repositorio conserva V01 en `../modelado_v01/outputs/Salve_modelado_v01.blend`,
las 20 referencias originales en `../references`, la propuesta secundaria de
expresiones en `../design`, y las hojas, comparación, informes y fuentes previas
dentro de `../modelado_v01`. Se mantienen sus rutas existentes sin duplicar
clones, cachés o entornos de `work`.

1. Clona `bryanstevenriverabejarano-svg/Convert` y selecciona la rama `backup/salve-v02-2026-09-30`
   en GitHub Desktop, o clona esa rama con Git.
2. Instala o habilita Git LFS y descarga los objetos LFS con `git lfs pull`.
   Comprueba que el ZIP es el binario real y no un pequeño archivo pointer.
3. Verifica `SHA256SUMS_respaldo.txt` antes de continuar editando y abre el modelo
   V02 indicado arriba. Los HTML usan enlaces relativos al árbol completo.
4. Si vuelves a ejecutar fuentes Python, adapta `ROOT` y otras rutas absolutas
   registradas para tu ubicación nueva. Los scripts se conservan como procedencia
   del trabajo; no son una orden para reejecutarlos todos sobre el modelo final.
   `procedencia/source_integrity.json` registra rutas originales de esta máquina;
   usa sus hashes y las rutas históricas del repositorio para localizar las fuentes.

`procedencia` conserva los tres inputs de verificación/paquete y el script que
guardó la sesión GUI. El candidato Java, los PNG de staging y sus pruebas son
material de revisión: su copia aquí no publica ni modifica el producto.
