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
- Desactivar, cambiar el registro o cancelar el turno cancela la petición HTTPS y descarta
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

Sigue la [guía de red privada familiar](../scripts/PRIVATE_FAMILY_NETWORK.md) para configurar
WireGuard propio, HTTPS con CA privada, backend local HTTPS, registro por IPv4 privada y
coordenadas voluntarias. El servidor de esta versión requiere Linux y una interfaz WireGuard
real; las URL públicas antiguas deben migrarse. No se usan proxies HTTP sin cifrar.

El nodo mantiene su modelo fijo, límites de plazas, 60 tareas por hora por defecto,
cota de entrada configurable y hasta 2.048 tokens de salida. El backend tiene hasta 40
segundos para terminar; el contexto debe caber en su modelo. El móvil elige entre nodos
registrados con permiso y vuelve al modelo local si no hay ninguno utilizable.

Registrar una dirección no instala nada en ella. El propietario prepara cada equipo;
no se crean ni contratan recursos automáticamente.

## Datos, autorización y retirada

El prompt puede contener fragmentos de historial, recuerdos y contexto personal necesarios
para la tarea. Activar el modo informa de ello. No se sincroniza la base completa ni se envían
fotos, credenciales del teléfono o archivos del usuario. El nodo no recibe herramientas ni
permisos de Android; una respuesta propuesta pasa por el controlador habitual del teléfono.

Tokens y endpoints se almacenan cifrados con AES-GCM y una clave de Android Keystore. El
registro se excluye del backup y la migración de dispositivo. Tras reinstalar/restaurar
puede ser necesario volver a registrar los nodos. No se registran prompts o tokens en logs.
HTTPS verifica la IP SAN con la CA importada, no sigue redirecciones ni proxies del sistema,
y desactiva los reintentos automáticos. El socket queda vinculado al túnel de Salve. Android
bloquea tráfico HTTP sin cifrar; la navegación HTTPS conserva su política anterior.

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
python3 -m pip install -r scripts/requirements-family.txt
python3 -m unittest discover -s scripts -p test_salve_compute_node.py -v
bash scripts/test-compute-router.sh
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Las pruebas del worker incluyen HTTPS real contra un backend de prueba (no una inferencia
LLM real), autenticación, contrato, errores, duplicados concurrentes, cuota y límites. Las
pruebas Java cubren selección, desconexión, ocupación, capacidad, cancelación y respaldo.
La compilación Android/CI comprueba la integración; la prueba desde el S24 Ultra a un nodo
real, la calidad del modelo remoto y sus tiempos siguen siendo validaciones de despliegue.

Referencias del adaptador:
- [API de llama.cpp](https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md)
- [Red privada familiar y TLS](../scripts/PRIVATE_FAMILY_NETWORK.md)
