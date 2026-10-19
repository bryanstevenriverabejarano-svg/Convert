# Red privada y ubicación familiar de Salve

Esta versión incorpora WireGuard dentro de Android. Los nodos se registran manualmente y solo se contactan mediante **HTTPS sobre la VPN propia**. No consulta servicios de geolocalización, geocodificación, DNS para los nodos ni servidores de mapas. Los nodos se ven en la constelación de Salve; la ubicación que comparte cada nodo aparece en su ficha.

## Alcance y migración

- Android 8+; hasta cuatro nodos y cuatro peers, con IPv4 RFC1918 y rutas `/32`. No admite perfiles de túnel completo, DNS, IPv6, listas de apps ni rutas de subred. Salve aplica el túnel únicamente a su propia app.
- El servidor funciona en **Windows, Linux y macOS** con Python 3.10+ y WireGuard (el programa oficial, sin cuenta ni servidor de coordinación). En Linux se exige además que la dirección pertenezca a una interfaz WireGuard (iproute2) y el socket se ata a ella; en Windows/macOS el nodo solo escucha en la IP del túnel, que solo existe mientras el túnel está activo, y solo acepta las IP de los móviles autorizados.
- Los nodos antiguos con URL pública/Tailscale dejan de recibir peticiones. Retíralos y regístralos con su IP privada después de configurar WireGuard y HTTPS. Las tareas vuelven al modelo local cuando no hay un nodo utilizable.
- La conexión entre viviendas necesita conectividad UDP hasta un endpoint propio (por ejemplo, un router con puerto UDP autorizado). Esa dirección exterior puede ser pública; **no se usa para obtener ni inferir ubicación**. Dentro del túnel, los nodos tienen direcciones privadas. No se crean relés, máquinas ni cuentas automáticamente.
- Android permite una VPN activa por perfil. Esta integración se inicia mediante el permiso de Android y muestra una notificación con Desconectar. No arranca automáticamente al reiniciar ni habilita Always-on.
- Ciudad y zona son texto que escribe el propietario. Las coordenadas estáticas son declaraciones, no comprobaciones de presencia. El GPS es una lectura puntual local, con permiso de primer plano, vista previa y envío explícito; caduca a los 15 minutos. No hay permiso de ubicación en segundo plano ni historial.

## Camino rápido: un solo script

En el PC del nodo (Windows, Linux o macOS):

```sh
python -m pip install -r requirements-family.txt
python salve_private_network.py crear --endpoint IPV4_DEL_PC_O_DEL_ROUTER
```

Genera la CA y el certificado del nodo (con identificadores AKI/SKI, que exigen Python 3.13+ y Android), las claves WireGuard del PC y del móvil, el túnel del PC (`wg-nodo-salve.conf`), el perfil del móvil (`salve-movil.conf`) y los tokens, e indica los pasos restantes y el comando exacto para arrancar el nodo. El modelo local puede escuchar en `http://127.0.0.1:8080`: ese tráfico no sale del PC.

Las secciones siguientes explican el procedimiento manual equivalente.

## 1. Crear una VPN propia

En cada equipo, genera claves WireGuard y conserva privadas las claves secretas. Ejemplo para un nodo Linux y un móvil:

```sh
umask 077
wg genkey > nodo.key
wg pubkey < nodo.key > nodo.pub
wg genkey > movil.key
wg pubkey < movil.key > movil.pub
```

Configura el nodo como `wg0` mediante las herramientas de tu sistema. Ejemplo `/etc/wireguard/wg0.conf` (sustituye las claves):

```ini
[Interface]
PrivateKey = CLAVE_PRIVADA_DEL_NODO
Address = 10.77.0.2/32
ListenPort = 51820

[Peer]
PublicKey = CLAVE_PUBLICA_DEL_MOVIL
AllowedIPs = 10.77.0.1/32
```

Activa la interfaz con `sudo wg-quick up wg0`. En tu firewall permite UDP 51820 desde donde debas conectarte y TCP 8766 **solo por wg0** desde `10.77.0.1`. No expongas TCP 8766 ni el backend del modelo en la interfaz pública. No necesitas habilitar forwarding para un nodo directo.

Perfil `.conf` para importar en Salve:

```ini
[Interface]
PrivateKey = CLAVE_PRIVADA_DEL_MOVIL
Address = 10.77.0.1/32

[Peer]
PublicKey = CLAVE_PUBLICA_DEL_NODO
AllowedIPs = 10.77.0.2/32
Endpoint = IPV4_NUMERICA_DEL_ROUTER_O_NODO:51820
PersistentKeepalive = 25
```

Para otro nodo añade otro peer con su clave, endpoint y otra dirección privada `/32`; no reutilices claves de propietarios distintos. Entrega cada perfil y la CA mediante un canal privado. Borra las copias temporales del perfil importado cuando ya no las necesites. El archivo de origen queda bajo tu control; la app no lo borra.

## 2. HTTPS con una CA tuya

Necesitas OpenSSL y una versión de `llama-server` con soporte TLS (`LLAMA_OPENSSL`). Estos comandos crean una CA, un certificado para la IP del nodo y otro para el backend local. Ejecútalos en una carpeta nueva privada; no sobreescribas claves existentes:

```sh
umask 077
mkdir -m 700 tls
cd tls
openssl req -x509 -newkey rsa:3072 -nodes -days 3650 -keyout ca.key -out ca.pem -subj '/CN=Salve Family CA' -addext 'basicConstraints=critical,CA:TRUE' -addext 'keyUsage=critical,keyCertSign,cRLSign'
openssl req -newkey rsa:2048 -nodes -keyout server.key -out server.csr -subj '/CN=Salve node'
printf 'subjectAltName=IP:10.77.0.2\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid,issuer\n' > server.ext
openssl x509 -req -in server.csr -CA ca.pem -CAkey ca.key -CAcreateserial -days 365 -out server.pem -extfile server.ext
openssl req -newkey rsa:2048 -nodes -keyout backend.key -out backend.csr -subj '/CN=Salve local model'
printf 'subjectAltName=IP:127.0.0.1\nbasicConstraints=critical,CA:FALSE\nkeyUsage=critical,digitalSignature,keyEncipherment\nextendedKeyUsage=serverAuth\nsubjectKeyIdentifier=hash\nauthorityKeyIdentifier=keyid,issuer\n' > backend.ext
openssl x509 -req -in backend.csr -CA ca.pem -CAkey ca.key -CAcreateserial -days 365 -out backend.pem -extfile backend.ext
cd ..
```

Importa **solo `ca.pem`** en la app. Mantén `ca.key` fuera del servicio del nodo, en almacenamiento privado que controles; el servicio solo necesita los certificados y sus claves de servidor. Si usas una CA por familiar, puedes concatenar hasta cuatro CA PEM al importar. Renueva los certificados antes de caducar. No se desactiva la validación de certificados ni la comprobación de la IP SAN.

Arranca el modelo local cifrado (ajusta la ruta al GGUF y el alias):

```sh
llama-server -m modelo.gguf --alias modelo-familiar --host 127.0.0.1 --port 8080 --ssl-key-file tls/backend.key --ssl-cert-file tls/backend.pem
```

No hay tramo HTTP sin cifrar, ni siquiera en loopback.

## 3. Preparar y arrancar el nodo

Desde la carpeta con los scripts adjuntos, crea un entorno Python 3.10+ e instala la dependencia fijada:

```sh
python3 -m venv .venv
.venv/bin/python -m pip install -r requirements-family.txt
.venv/bin/python salve_node_setup.py preparar --url https://10.77.0.2:8766 --nombre 'Mi PC' --token-file node_token
```

El asistente recomienda un tamaño de modelo según hardware y genera el token de consulta en modo 600. Opcionalmente instala `qrcode` en ese entorno para mostrar un QR local; la URL y el token también se pueden introducir a mano. No se anuncia el nodo por mDNS.

El servidor usa `SO_BINDTODEVICE` para aceptar paquetes solamente por `wg0` y comprueba que la interfaz sea WireGuard, que tenga la IP configurada y que el cliente figure en `--allow-client`. El kernel Linux requiere `CAP_NET_RAW` para esa restricción. Usa un servicio systemd con usuario dedicado (no concedas capacidades al intérprete Python global). Ejemplo de unidad, ajustando usuario y rutas absolutas existentes:

```ini
[Unit]
Description=Salve private family node
After=network-online.target wg-quick@wg0.service
Requires=wg-quick@wg0.service

[Service]
Type=simple
User=salvenode
Group=salvenode
WorkingDirectory=/srv/salve
ExecStart=/srv/salve/.venv/bin/python /srv/salve/salve_compute_node.py --token-file /srv/salve/node_token --model modelo-familiar --bind 10.77.0.2 --interface wg0 --allow-client 10.77.0.1 --tls-cert /srv/salve/tls/server.pem --tls-key /srv/salve/tls/server.key --backend-ca /srv/salve/tls/ca.pem
AmbientCapabilities=CAP_NET_RAW
CapabilityBoundingSet=CAP_NET_RAW
NoNewPrivileges=true
PrivateTmp=true
ProtectSystem=strict
ProtectHome=true
ReadWritePaths=/srv/salve/family
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Crea `/srv/salve/family` con propietario `salvenode` y modo 700, incluso si dejas la ubicación desactivada. El usuario del servicio necesita leer los scripts, certificados y token; las claves y tokens deben tener modo 600. Instala/inicia la unidad según tu administración habitual. El modelo se arranca aparte, como se muestra arriba. El servidor rechaza el inicio si no puede enlazarse a WireGuard; no cambia a `0.0.0.0` ni a HTTP.

## 4. Ubicación voluntaria y cifrada

Por defecto `/v1/family/status` no incluye coordenadas. Para compartirlas, añade `--location-directory /srv/salve/family` a `ExecStart`. El almacén crea `location.key` y conserva solo la última posición en `location.enc` (AES-256-GCM, archivos 600, carpeta 700). La protección de la clave depende también de la cuenta y del disco del propietario.

Para una configuración estática, ejecuta como el mismo usuario del servicio:

```sh
.venv/bin/python salve_private_location.py --directory /srv/salve/family
```

Solicita latitud, longitud, ciudad, zona y confirmación explícita; no busca la ciudad ni consulta una IP. Para retirar la ubicación:

```sh
.venv/bin/python salve_private_location.py --directory /srv/salve/family --withdraw
```

Para permitir que **el propietario** envíe una lectura GPS desde su propio Android, crea un token de escritura distinto (como el usuario del servicio, con archivo privado):

```sh
.venv/bin/python -c "from pathlib import Path; from salve_node_setup import create_token; create_token(Path('location_token'))"
```

Añade `--location-token-file /srv/salve/location_token` al servicio. El propietario introduce este token en **Mi ubicación**, nunca en el registro de consulta ni en el QR de otros familiares. El token de consulta no puede modificar coordenadas; el de ubicación no puede leer estados ni ejecutar inferencia.

En Android, **Mi ubicación** permite escribir coordenadas estáticas o pulsar **Leer mi GPS una vez**. La app usa exclusivamente `LocationManager.GPS_PROVIDER`, solicita permiso preciso, espera hasta 30 segundos y detiene la lectura al salir. Revisa y pulsa **Compartir**. Para publicar la ubicación de otro familiar debe hacerlo esa persona en su dispositivo o configurar su propio nodo.

Retirar la ubicación sustituye el registro por `null`. Los móviles conectados borran la copia en la siguiente consulta. Un móvil sin conexión puede conservar la última posición compartida hasta consultar, retirar el nodo o borrar la red; el borrado remoto no puede revocar información ya recibida.

## 5. Conectar y ver los nodos en Android

1. Abre **Nodos de cómputo → Red privada (VPN) → Configurar VPN**. Importa el `.conf` y la CA PEM. Guarda, pulsa **Conectar** y acepta el diálogo de VPN de Android.
2. Registra el nodo con `https://10.77.0.2:8766` y el token de consulta (QR o manual). Las direcciones privadas deben figurar exactamente en `AllowedIPs`.
3. Abre **Ver constelación de nodos**. Mientras está abierta, Salve consulta los nodos activos cada 10 segundos y solo descarga la ubicación si ha cambiado (el móvil envía la versión que ya tiene y el nodo responde «sin cambios»). Con la pantalla cerrada no se consulta nada en segundo plano.
4. Toca un nodo para ver su ficha: estado, modelo, plazas, latencia y, si su propietario la comparte, su ubicación (ciudad, zona, coordenadas, origen y fecha).

El perfil VPN, la CA y la última ubicación recibida se guardan cifrados con Android Keystore en `noBackupFilesDir`. No se exportan a la copia de seguridad, al LLM ni al registro de diagnóstico. Cada petición tiene socket vinculado a la red VPN de Salve, ruta privada explícita, CA propia, validación de IP, sin proxy, redirecciones ni reintentos. La revocación/desconexión cancela la petición; no se vuelve a la red física para enviar datos de nodos. El móvil no envía su propia ubicación a los nodos.

Para revocar acceso, retira la clave pública del móvil del peer WireGuard del nodo y rota su token de consulta. Para desconectar localmente usa la pantalla o la notificación. **Borrar red** elimina perfil, CA y ubicaciones cifradas del móvil; no modifica los nodos de otros propietarios.

## Validación

Las pruebas automáticas cubren TLS real (incluido el backend), rechazo de HTTP/CA desconocida, tokens separados, retirada de ubicación, cifrado y manipulación del almacén, coordenadas inválidas/GPS caducado, caída de interfaz/cliente no autorizado, nodo en Windows sin iproute2, ubicación versionada (sin cambios = sin coordenadas), generación de CA/certificados/claves WireGuard y límites del perfil.

```sh
python3 -m pip install -r scripts/requirements-family.txt
python3 -m unittest discover -s scripts -p 'test_*.py'
bash scripts/test-compute-router.sh
bash ./gradlew :app:testDebugUnitTest :app:assembleDebug
```

Antes de usarlo en familia, valida en un Android y un nodo reales (Windows o Linux): permiso VPN rechazado/aceptado, handshake, CA incorrecta, revocación de permiso, cambio Wi-Fi/datos, modo avión, reinicio, ahorro de batería, retirada de un nodo durante una petición, GPS denegado/caducado y captura de tráfico sin coordenadas ni peticiones de mapas fuera del túnel. La CI no reemplaza esta validación de red y dispositivo.

## Referencias y licencias

- WireGuard Android y biblioteca embebida (Apache-2.0): https://git.zx2c4.com/wireguard-android/about/
- Android VPN: https://developer.android.com/develop/connectivity/vpn
- WireGuard Quick Start: https://www.wireguard.com/quickstart/
- AES-GCM: https://cryptography.io/en/stable/hazmat/primitives/aead/
- llama-server TLS: https://github.com/ggml-org/llama.cpp/blob/master/tools/server/README.md
