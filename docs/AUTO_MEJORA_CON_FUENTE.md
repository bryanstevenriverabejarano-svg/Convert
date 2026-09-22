# Auto-mejora con código fuente identificable

La app ya no pide al modelo que invente un diff a partir del nombre de una
clase. Necesita una instantánea exportada de una revisión Git, con el texto
completo y SHA-256 de cada archivo. La huella comprueba integridad; no es una
firma digital ni demuestra que la revisión sea la misma que generó el APK.

## Preparación del operador

Desde el checkout, selecciona de uno a ocho archivos pequeños del núcleo:

```bash
python3 scripts/export_auto_improvement_sources.py \
  app/src/main/java/salve/core/BlueprintAprendizajeContinuo.java \
  --revision HEAD --output /tmp/salve-source-snapshot.json
adb exec-in run-as com.salve.app sh -c 'mkdir -p files/auto-improvement && cat > files/auto-improvement/source-snapshot.json.tmp && mv files/auto-improvement/source-snapshot.json.tmp files/auto-improvement/source-snapshot.json' < /tmp/salve-source-snapshot.json
```

El segundo comando necesita un teléfono conectado por ADB con una instalación
depurable de Salve. No se ha verificado en un teléfono en este cambio. La app
lee el archivo privado al iniciar la siguiente solicitud de auto-evolución.
No hay descarga de fuentes, escritura del APK ni ejecución del contenido de
esta instantánea. El exportador lee los bytes del commit; excluye cambios sin
confirmar y archivos no versionados, y no modifica el checkout.

No se incluyen fuentes automáticamente en el APK. Se rechazan enlaces
simbólicos, rutas fuera de `app/src/main/java/salve/core`, fuentes vacías o de
más de 8.000 caracteres UTF-16, archivos repetidos, JSON ambiguo, huellas
incorrectas y revisiones mal formadas. La instantánea completa admite hasta
256.000 bytes. Los archivos demasiado grandes se rechazan sin truncarlos.
El límite de caracteres no garantiza que el prompt quepa en el contexto de
cada modelo; el runtime puede rechazarlo o fallar por falta de capacidad.

## Qué hace Salve con la fuente

1. Inspecciona las firmas de las clases instaladas correspondientes a la lista
   explícita. No depende de `@CoreComponent`, que no estaba aplicada a ninguna
   clase. Esta inspección sólo detecta métodos con más de cuatro parámetros;
   no equivale a un análisis semántico ni certifica errores.
2. Envía al proveedor elegido el diagnóstico, la fuente completa, su revisión
   y huella. El modo local continúa sin enviar código a la nube. Se intentan
   como máximo tres problemas por ejecución.
3. Rechaza texto que no tenga forma de diff del archivo solicitado y conserva
   una propuesta en la bandeja habitual. Las pruebas generadas y el prechequeo
   sintáctico no certifican que ese parche funcione.
4. Devuelve el estado real: falta de fuente, ausencia de objetivos, inspección
   sin problemas de firmas, ningún parche utilizable, propuesta guardada o error.

Las propuestas nuevas incluyen `sourceRevision` y `sourceSha256`. El ejecutor
externo compara el SHA-256 del archivo en su revisión base con esa fuente
**antes de aplicar el diff**. Si cambió cualquier byte, incluso fuera del
fragmento modificado, se debe exportar una instantánea nueva y regenerar la
propuesta. No se intenta adaptar silenciosamente un parche obsoleto.

Las propuestas antiguas de esquema 3 sin esos dos campos conservan su ruta
previa de comprobación de diff y sandbox; el PR lo indica explícitamente.
Una propuesta que incluye sólo un campo de identidad o valores inválidos se
rechaza. La revisión queda registrada como procedencia declarada; la prueba
mecánica de coincidencia utiliza la huella del archivo completo.

## Qué sigue faltando para un ciclo autónomo operativo

Se mantiene el [ejecutor externo con sandbox](../scripts/sandbox/README.md):
ADB recoge la bandeja, Docker aplica las pruebas sobre un snapshot sin red,
y Git/GitHub del anfitrión publican una rama separada. Se requiere preparar una
imagen Docker inmutable, dependencias y credenciales del anfitrión. Este cambio
no activa ese servicio, fusiona PR ni instala actualizaciones automáticamente.
El runner añade ahora `generatedTests`, cuando contiene una suite válida, antes
de exportar el snapshot para Docker. La ruta se deriva del objetivo:
`app/src/test/java/<paquete>/<Clase>TestHarness.java`. Si falta la declaración
de paquete, incorpora la del objetivo; rechaza un paquete distinto, clases
incompatibles, `@Ignore`, enlaces y colisiones con archivos existentes. No
sobrescribe pruebas anteriores: una segunda propuesta para la misma clase puede
necesitar consolidación manual de su suite. El prechequeo es estructural; la
compilación y ejecución de las pruebas corresponden a Gradle dentro de Docker.
El PR registra la ruta y el SHA-256 del test indexado, además del árbol exacto
que contiene el parche y la suite. Las propuestas de esquema 3 sin
`generatedTests`, o con ese campo vacío, conservan el flujo anterior y el PR
indica que sólo se ejecutaron las pruebas ya versionadas.

No se ha ejecutado aquí el sandbox Docker ni código Java propuesto: las pruebas
del runner usan Git y snapshots reales, con Docker y publicación sustituidos
explícitamente. No se ha probado el circuito completo con inferencia local,
Docker Android y teléfono físico; si Docker no está disponible, el runner
rechaza la ejecución y no compila la propuesta en el anfitrión.

Pruebas reproducibles del exportador y runner:

```bash
python3 -m unittest discover -s scripts -p 'test_*auto_improvement*.py'
```

Los tests de integración usan Git y worktrees reales; Docker y GitHub se
sustituyen explícitamente. La prueba Docker opcional sigue separada. Las pruebas
JVM de instantánea, generador y bandeja comprueban el contexto exacto, fallos,
límites, selección de proveedor y serialización; no ejecutan un modelo real.
