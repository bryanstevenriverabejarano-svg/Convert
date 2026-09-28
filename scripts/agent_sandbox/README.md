# Servicio opcional de herramientas de Salve

La app ya puede consultar memoria, descubrir fuentes en Wikipedia y leer páginas o APIs públicas HTTPS sin este servicio. El puente añade búsqueda general (Brave, clave opcional), ejecución Python y renderizado Chromium. La inferencia sigue en el móvil; este servicio no aloja otro LLM.

## Despliegue

Requiere un servidor Linux propio con Docker, Python 3.13 mantenido, dominio HTTPS y un servidor WSGI mantenido (por ejemplo Gunicorn). No se despliega ni contrata infraestructura desde la app. Instala y actualiza esas dependencias con tu procedimiento habitual.

```bash
docker build --iidfile /tmp/salve-agent-image scripts/agent_sandbox
export SALVE_AGENT_IMAGE="$(cat /tmp/salve-agent-image)"
export SALVE_AGENT_TOKEN="$(python3 -c 'import secrets; print(secrets.token_urlsafe(48))')"
export SALVE_AGENT_DB=/var/lib/salve-agent/receipts.sqlite
# SALVE_BRAVE_SEARCH_KEY es opcional; usa la clave privada de tu cuenta si la tienes.
gunicorn --chdir scripts --bind 127.0.0.1:8765 --workers 1 --threads 2 --timeout 45 agent_bridge:application
```

Ejecuta en un anfitrión dedicado, con la cuenta del servicio autorizada para Docker y un directorio de estado escribible únicamente por esa cuenta. El acceso al daemon Docker es privilegiado; no coloques otras cargas sensibles en ese anfitrión. No publiques el puerto 8765. Configura un proxy HTTPS hacia loopback con límite de cuerpo de 32 KB, timeout de 45 segundos y límites de peticiones. Conserva los secretos en la configuración privada del servicio, sin registrarlos en accesos, scripts versionados ni el chat.

`SALVE_AGENT_IMAGE` debe ser un ID `sha256:...` o referencia `imagen@sha256:...` existente localmente. No se aceptan etiquetas mutables ni descargas durante una tarea. La imagen se construye desde la etiqueta mantenida indicada en Dockerfile; registra el ID construido para cada despliegue y reconstruye con actualizaciones. No se afirma una compilación reproducible de los paquetes apt.

En Salve escribe **configura herramientas**. Introduce el origen HTTPS y el token en esa pantalla privada. El token se cifra con Android Keystore y queda excluido de las copias de seguridad. Los planes conservan su destino original; cambiar el servidor no traslada automáticamente tareas antiguas. **Desconectar servicio** invalida nuevas llamadas desde la app, pero una petición ya enviada puede terminar en el servidor.

Para desarrollo exclusivamente, `python3 scripts/agent_bridge.py` escucha en loopback con WSGI de referencia; no es el servidor de producción.

## Contrato y aislamiento

- `GET /v1/capabilities`: configuración declarada; la disponibilidad efectiva se comprueba al ejecutar.
- `POST /v1/execute`: `{"tool":"web.read","input":"https://..."}`, bearer, `Idempotency-Key` UUID. Respuesta con `status`, `text`, `sources`, `links`.
- Herramientas: `web.search`, `web.read`, `api.get`, `browser.render`, `code.python`. Sin envío de mensajes, compras, cuentas autenticadas ni escrituras en APIs.
- Lecturas HTTPS: IP pública validada y fijada al socket, hostname TLS original, redirecciones revalidadas, sin proxies, sin cookies ni credenciales de páginas, 256 KB por página. La clave Brave va únicamente al endpoint fijo del proveedor, sin redirecciones.
- Python y Chromium se ejecutan en un contenedor nuevo: sin red, sin montajes del anfitrión, raíz de sólo lectura, UID 65534, capacidades retiradas, 1 CPU, 768 MB, 96 procesos, `/tmp` de 192 MB, 20 segundos y 24 KB de salida. El código generado nunca se ejecuta en Python del anfitrión.
- Chromium procesa el HTML obtenido por el lector público y JavaScript incrustado. No descarga scripts, estilos, imágenes ni datos de APIs adicionales; no navega sesiones autenticadas. El resultado se marca **PARTIAL** por ese límite. El perfil, configuración XDG y caché viven en el tmpfs; la raíz permanece de sólo lectura. Su `--no-sandbox` depende de la barrera del contenedor exterior; no se presenta como aislamiento de microVM ni protección absoluta frente a fallos del kernel.
- Recibos SQLite con retención de siete días: mismo ID/cuerpo reutiliza resultado, cuerpo cambiado obtiene 409. Tras muerte del servidor una llamada sin recibo puede repetirse después de 60 segundos. Son lecturas o cálculos sin efectos exteriores; no se promete “exactamente una vez” frente a todos los fallos.
- Sólo se guardan digest y recibo, pero un recibo puede incluir datos que el usuario haya incluido explícitamente en el objetivo. Protege el archivo de estado. Dos llamadas concurrentes por proceso; mantener un worker evita multiplicar ese límite.

## Verificación

```bash
python3 -m unittest discover -s scripts -p test_agent_bridge.py -v
SALVE_RUN_AGENT_DOCKER=1 python3 -m unittest discover -s scripts -p test_agent_bridge.py -v
```

El segundo comando exige la imagen local configurada. Ejecuta Python y Chromium reales; comprueba UID, raíz de sólo lectura, falta de red/secretos, DOM modificado por JavaScript, límites de salida/tiempo y limpieza de contenedores. CI tiene un trabajo Linux dedicado a esta verificación. Las pruebas unitarias restantes simulan respuestas públicas; no certifican disponibilidad de Brave ni de un servidor privado.

Documentación primaria: [Docker run](https://docs.docker.com/engine/containers/run/) · [Brave Search](https://api-dashboard.search.brave.com/app/documentation/web-search).
