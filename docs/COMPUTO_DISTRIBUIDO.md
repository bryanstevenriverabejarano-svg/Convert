# Nodos de cómputo de Salve

Salve puede enviar una tarea de texto completa a un equipo propio y recoger su respuesta.
El móvil mantiene la identidad, memoria, planificación, permisos y ejecución de acciones.
El nodo es un trabajador de inferencia con un modelo instalado por su propietario.
No copia la aplicación, no escanea webs, no crea cuentas y no contrata máquinas.

## Qué funciona en esta versión

- **IA y cámara → Nodos de cómputo**: registrar hasta cuatro endpoints HTTPS, probarlos,
  retirarlos y activar/desactivar el modo de nodos. El registro viene vacío y desactivado.
- Cuentos (`NARRADOR`), reflexiones, síntesis, evaluaciones y creación se consideran trabajos
  pesados. Otros roles se delegan cuando el prompt alcanza 6.000 caracteres.
- Se consulta la disponibilidad de los nodos registrados (máximo dos segundos por nodo),
  se descartan los ocupados o los que no admitan el tamaño de entrada y se elige el que
  anuncie más plazas libres. En empate se conserva el orden del registro.
- Se envía a **un solo nodo** por tarea. Timeout, error o respuesta vacía/grande llevan
  al modelo del teléfono. Un nodo con errores descansa 60 segundos antes de volver a probarlo.
- Desactivar, cambiar el registro o cancelar el turno cancela la petición HTTP y descarta
  una respuesta tardía. Una cancelación no inicia inferencia local adicional.
- El proveedor de cada respuesta queda identificado como `Nodo nombre / modelo`.
- La consolidación de sueño se beneficia a través de sus llamadas de inferencia; la base
  de datos y la aplicación de cambios en memoria siguen en el teléfono.

Es distribución **por tareas**, no reparto de las capas de un LLM entre varias GPU.
Cada tarea usa la capacidad de un nodo. El motor Android conserva su serialización de
turnos: esta versión no suma el rendimiento de todos los nodos para un único cuento ni
lanza varias generaciones simultáneas desde un teléfono. El contexto del chat conserva
su presupuesto actual para permitir el respaldo local; no se amplía automáticamente.
La calidad y velocidad dependen del modelo, hardware y red, y requieren medición real.

## Preparar un nodo propio

Requisitos: equipo/servidor con permiso de administración, Python 3.10 o superior,
`llama-server` y un modelo GGUF compatible ya instalado. Un hosting compartido PHP o
una carpeta pCloud/Terabox no sustituyen un servidor capaz de ejecutar el modelo.

1. Inicia el modelo **solo en loopback**. Ajusta el contexto a tu RAM/VRAM:

   ```bash
   llama-server -m /ruta/modelo.gguf --alias salve-node --host 127.0.0.1 --port 8080 -c 8192 --parallel 1
   ```

2. Guarda un token privado distinto para cada nodo. No lo metas en Git ni en mensajes:

   ```bash
   python3 - <<'PY'
   import os, secrets
   from pathlib import Path
   directory = Path.home() / '.config' / 'salve-compute'
   directory.mkdir(parents=True, exist_ok=True)
   path = directory / 'token'
   descriptor = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
   with os.fdopen(descriptor, 'w') as stream:
       stream.write(secrets.token_urlsafe(32))
   print('Token creado en', path)
   PY
   ```

3. Ejecuta el microcomponente desde este repositorio:

   ```bash
   python3 scripts/salve_compute_node.py \
     --token-file "$HOME/.config/salve-compute/token" \
     --backend http://127.0.0.1:8080 --model salve-node \
     --slots 1 --hourly-limit 60 --max-prompt-chars 12000
   ```

   El proceso escucha en `127.0.0.1:8766`. El backend está fijado por el operador:
   una tarea no puede cambiar su URL, modelo, herramientas o límites. El backend tiene
   hasta 40 segundos para terminar y un máximo de 2.048 tokens de salida.
   `--max-prompt-chars` es una cota de transporte, no una garantía del tokenizador:
   la entrada más la salida deben caber también en el contexto real de llama.cpp.

4. Publica **solo ese puerto** mediante tu proxy HTTPS administrado y certificado válido
   para el nombre que usarás en Android. El proxy debe conservar `Authorization`, limitar
   el cuerpo a 300.000 bytes, limitar conexiones y aplicar timeouts. Por ejemplo, si ya
   administras Caddy con un dominio propio:

   ```caddyfile
   nodo.tu-dominio.example {
       request_body {
           max_size 300000
       }
       reverse_proxy 127.0.0.1:8766
   }
   ```

   Sustituye el dominio de ejemplo por el tuyo. No expongas el puerto 8080 del backend.
   También puedes usar una red privada/VPN con HTTPS válido. Android no acepta certificados
   autofirmados sin confianza ni se desactiva la verificación TLS. El servidor Python
   incluido es un servidor sencillo: para exponerlo a Internet usa un proxy mantenido
   con límites de conexiones y peticiones, firewall y operación supervisada.

5. En Salve, añade nombre, URL HTTPS del nodo y token. Pulsa **Probar**: consulta su estado
   y solicita un saludo de prueba. Después activa **nodos propios** para permitir envíos
   automáticos de tareas pesadas. No necesita una clave de Gemini.

No se han desplegado servidores ni comprado recursos con este parche. El usuario decide
qué equipos registra. Registrar una URL no instala nada en ella. Para añadir potencia,
instala este mismo componente y un modelo adecuado en otro equipo y registra su endpoint.

## Datos, autorización y retirada

El prompt puede contener fragmentos de historial, recuerdos y contexto personal necesarios
para la tarea. Activar el modo informa de ello. No se sincroniza la base completa ni se envían
fotos, credenciales del teléfono o archivos del usuario. El nodo no recibe herramientas ni
permisos de Android; una respuesta propuesta pasa por el controlador habitual del teléfono.

Tokens y endpoints se almacenan cifrados con AES-GCM y una clave de Android Keystore. El
registro se excluye del backup y la migración de dispositivo. Tras reinstalar/restaurar
puede ser necesario volver a registrar los nodos. No se registran prompts o tokens en logs.
HTTPS verifica el destino, no sigue redirecciones ni proxies del sistema, y desactiva los
reintentos automáticos. La política de navegación web pública no se modifica.

- **Usar modelo local**: desactiva nodos y Gemini.
- **Activar nodos propios**: texto pesado a nodos, resto/respaldo al móvil; fotos locales.
- **Usar Gemini**: desactiva nodos y conserva el comportamiento de Gemini anterior.
- **Retirar nodo**: revoca su selección y cancela la petición en curso desde este teléfono.
  Rota también su token y para el servicio si quieres revocar a otros clientes.

La cancelación del cliente no garantiza parar una generación que el backend ya haya recibido.
El servicio tiene su propio plazo y límite de concurrencia. Tras un timeout el teléfono
puede generar localmente mientras termina la petición remota; el resultado remoto tardío
no se aplica. No hay herramientas con efectos externos en el nodo.

El servidor limita concurrencia (1 por defecto, máximo 4), conexiones (12) y generaciones
por hora (60 por defecto). Las cuotas y los 1.024 recibos de UUID viven en RAM y se reinician
al parar el servicio. Un UUID repetido dentro de esa ventana devuelve 409 y no repite la
inferencia; no es una garantía de ejecución exactamente una vez ni un sistema de facturación.
No hay autoescalado, descarga de modelos, ejecución de shell ni réplicas autónomas.

## Protocolo y validación

`GET /v1/health` y `POST /v1/generate` requieren `Authorization: Bearer …`.
Health comprueba `/health` de llama.cpp y devuelve protocolo, modelo fijo, plazas libres y
límites. Generate acepta únicamente `protocol`, UUID `id`, `role`, `prompt` y `max_tokens`.
La respuesta debe coincidir con el UUID, ser `SUCCESS` y contener texto no vacío y acotado.
El servicio rechaza salidas truncadas (`finish_reason` distinto de `stop`).

```bash
python3 -m unittest discover -s scripts -p test_salve_compute_node.py -v
bash scripts/test-compute-router.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Las pruebas del worker incluyen HTTP real contra un backend de prueba (no una inferencia
LLM real), autenticación, contrato, errores, duplicados concurrentes, cuota y límites. Las
pruebas Java cubren selección, desconexión, ocupación, capacidad, cancelación y respaldo.
La compilación Android/CI comprueba la integración; la prueba desde el S24 Ultra a un nodo
real, la calidad del modelo remoto y sus tiempos siguen siendo validaciones de despliegue.

Referencias del adaptador:
- [API de llama.cpp](https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md)
- [Configuración del proxy Caddy](https://caddyserver.com/docs/caddyfile/directives/reverse_proxy)
