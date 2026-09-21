# Sandbox de auto-mejora

El runner ejecuta `testDebugUnitTest` en un contenedor Docker local con imagen
inmutable. El worktree se utiliza para Git; nunca se monta dentro del contenedor.
El cambio publicado debe tener el mismo SHA de árbol que el snapshot probado.

## Límites de ejecución

- Solo se monta `source.tar`, exportado del índice de Git, en modo de lectura.
  Quedan fuera `.git`, archivos no versionados, directorios personales,
  agentes SSH y sockets del anfitrión.
- El cliente Docker recibe un entorno mínimo y una configuración temporal vacía.
  No hereda tokens de GitHub ni variables proxy. El código tampoco recibe esas
  credenciales. La imagen y el código versionado deben estar libres de secretos.
- Usuario 65534, sin capacidades Linux adicionales, `no-new-privileges`, raíz
  de lectura, sin red, sin volúmenes persistentes de la imagen.
- Hasta 2 CPU, 6 GiB de memoria sin swap, 256 procesos y 600 segundos por prueba.
  `/work` y `/tmp` son temporales limitados a 4 GiB y 1 GiB respectivamente.
- Gradle utiliza `--offline --no-daemon --no-build-cache --rerun-tasks`. Las
  dependencias faltantes producen un fallo; no se activa acceso a Internet.
- Se retira el contenedor tras éxito, fallo, timeout, SIGTERM o interrupción de teclado.
  Un fallo de limpieza bloquea la publicación y muestra el nombre para revisión.
  Si se mata el supervisor con SIGKILL o cae el equipo, revisar contenedores
  `salve-test-*` antes de reiniciarlo.
- Se registra la imagen y el árbol en el PR. Por ahora, la salida de compilación
  se descarta para limitar consumo de disco/memoria por un parche; se informa el
  código de salida o timeout. La conservación de logs acotados queda pendiente.

Los contenedores comparten kernel con el anfitrión. Usar una estación dedicada y
Docker actualizado; esta implementación no afirma equivalencia a una VM ni
garantiza protección ante vulnerabilidades del runtime. Referencias oficiales:
[opciones de Docker run](https://docs.docker.com/reference/cli/docker/container/run/),
[red none](https://docs.docker.com/engine/network/drivers/none/).

## Preparar una imagen de confianza

La imagen se prepara desde una revisión ya revisada, antes de procesar propuestas.
El runner nunca construye ni descarga imágenes. Se incluye `Dockerfile` para
precargar la distribución Gradle y sus dependencias con una ejecución de baseline.
Si la baseline no compila, resolver ese fallo antes de activar el supervisor.

El operador debe proporcionar `SALVE_ANDROID_BASE` como referencia fija por
digest de una imagen Linux de confianza con:

- SDK Android API 36 y build-tools 36.0.0, con `ANDROID_HOME` configurado;
- Java compatible con el proyecto; actualmente el criterio de daemon exige
  JetBrains JDK 21, ver `gradle/gradle-daemon-jvm.properties`;
- `/bin/sh`, `tar`, `cp`, `mkdir`, `chmod` y herramientas estándar;
- ningún secreto, credencial de repositorio, `VOLUME` ni agente SSH incorporado.

No se suministra una imagen binaria validada en este PR. El Dockerfile usa la
misma base en ambas etapas y copia únicamente la caché Gradle precalentada a
`/opt/salve/gradle-home`, que debe poder leer el usuario 65534. La preparación
puede necesitar red; la evaluación de propuestas funciona sin ella.

Desde una revisión aprobada del repositorio, y con `SALVE_ANDROID_BASE` ya definido:

```bash
SALVE_BUILD_CONTEXT="$(mktemp -d)"
git archive HEAD | tar -xf - -C "$SALVE_BUILD_CONTEXT"
docker build --build-arg SALVE_ANDROID_BASE="$SALVE_ANDROID_BASE" \
  -f scripts/sandbox/Dockerfile -t salve-sandbox:verified "$SALVE_BUILD_CONTEXT"
export SALVE_SANDBOX_IMAGE="$(docker image inspect --format '{{.Id}}' salve-sandbox:verified)"
```

El contexto exportado evita enviar archivos locales privados a Docker. Conservar
el SHA de la revisión, el digest de la base y el ID de la imagen obtenida para
poder reproducir y revertir la configuración. Un cambio de dependencias requiere
reconstruir la imagen desde una nueva revisión aprobada.

El runner acepta un ID local `sha256:` de 64 caracteres hexadecimales o una
referencia `repositorio@sha256:...`. Una etiqueta mutable como `latest` se rechaza.
La imagen debe estar ya disponible en el motor local (`--pull never`).

Por defecto se utiliza `unix:///var/run/docker.sock`. Para Docker rootless,
configura `SALVE_DOCKER_HOST` con el socket Unix local de esa instalación. No se
heredan `DOCKER_CONTEXT`, `DOCKER_HOST` ni la configuración personal del cliente.

## Verificar antes de activar el supervisor

```bash
python3 -m unittest discover -s scripts -p 'test_auto_improvement*.py'
SALVE_RUN_DOCKER_SMOKE=1 python3 -m unittest discover -s scripts -p 'test_auto_improvement_sandbox.py'
python3 scripts/auto_improvement_runner.py /ruta/a/propuesta.json --repo . --dry-run
```

La suite normal simula Docker; la prueba opcional ejecuta un contenedor real y
comprueba red, usuario, montajes y ausencia de variables de credenciales. Esa
prueba de aislamiento tampoco equivale a compilar Android: se requiere además
el `--dry-run` con una propuesta aplicable y la imagen preparada.

No se activan servicios ni se fusionan PR automáticamente. La autorización para
generar PR se mantiene; las credenciales para publicarlos siguen en Git/GitHub CLI
del anfitrión. Tras comprobar los pasos anteriores se puede usar el puente y su
modo `--watch`, que leen la misma variable `SALVE_SANDBOX_IMAGE`.

El runner todavía no instala `generatedTests` de la propuesta; ejecuta las
pruebas ya versionadas. Las propuestas nuevas pueden proporcionar identidad de
fuente: el runner rechaza una fuente base distinta antes de aplicar el diff.
La app requiere una [instantánea de fuente exacta](../../docs/AUTO_MEJORA_CON_FUENTE.md)
para generarlas. Las propuestas antiguas sin huella mantienen la comprobación
de diff y las pruebas, con esa limitación indicada en el PR. Estas comprobaciones
no certifican el circuito completo en un teléfono.
