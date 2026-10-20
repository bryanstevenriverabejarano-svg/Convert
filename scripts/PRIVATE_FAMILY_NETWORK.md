# Conexión segura de Salve con sus nodos (TLS mutuo, sin VPN)

Salve habla con cada nodo de cómputo por **HTTPS con TLS mutuo (mTLS)** directamente entre
aplicaciones. No hay WireGuard, VPN, drivers ni adaptadores de red virtuales, y no hace falta
instalar ningún programa externo en el PC.

- El nodo solo acepta conexiones que presenten el **certificado del móvil**, firmado por la CA
  propia del nodo. Un cliente sin ese certificado no completa ni el saludo TLS.
- Salve solo acepta el **certificado del nodo** firmado por esa misma CA (no confía en CA
  públicas para los nodos), así que nadie puede hacerse pasar por tu PC.
- Encima del TLS, cada petición lleva además el token del nodo.
- Salve **no envía su ubicación** a los nodos. Cada nodo le dice la ubicación que fijó su dueño.

## Opción 1 · Windows con doble clic: `SalveNodo.exe`

`SalveNodo.exe` es el nodo empaquetado con PyInstaller: no necesita Python. Se construye en
GitHub Actions (workflow «Salve Nodo para Windows») y se descarga como artefacto
`SalveNodo-windows-<commit>`.

1. Copia `SalveNodo.exe` a una carpeta. Si quieres que el PC ejecute el modelo, pon también
   `llama-server.exe` (de llama.cpp) en esa carpeta y ten a mano el archivo `.gguf`.
2. Doble clic. La primera vez pregunta:
   - nombre del nodo,
   - IP de este PC en tu red (la detecta),
   - IP pública o dominio DDNS para usarlo fuera de casa (opcional),
   - puerto (8443 por defecto),
   - ubicación fija que verá Salve: latitud, longitud, ciudad, país y zona (opcional),
   - ruta del modelo `.gguf` y de `llama-server` (opcional si ya lo arrancas tú en 127.0.0.1:8080).
3. Genera en `%USERPROFILE%\.salve\red` la CA, los certificados y el perfil, y abre el **QR**.
4. En Salve: **Nodos de cómputo → Añadir un nodo → Escanear QR → Guardar**. También puedes pasar
   `salve-perfil.json` al móvil e importarlo en **Nodos de cómputo → Conexión segura → Importar perfil**.
5. Deja la ventana abierta: es el nodo. Si Windows pregunta por el cortafuegos, permite el acceso
   en **redes privadas**.

Otras órdenes:

```
SalveNodo.exe --reconfigurar   volver a responder las preguntas
SalveNodo.exe --qr             volver a abrir el QR
SalveNodo.exe --autoprueba     comprobar certificados y TLS mutuo sin tocar tu configuración
SalveNodo.exe --version
```

## Opción 2 · Con Python (Windows, Linux o macOS)

```
python -m pip install -r requirements-family.txt
python salve_nodo_app.py
```

Es el mismo asistente que el `.exe`.

## Uso fuera de casa (IP pública o DDNS)

1. Indica la IP pública o el dominio DDNS (por ejemplo `mipc.duckdns.org`) y el puerto al configurar.
   El certificado del nodo incluye esa dirección y la IP local.
2. En el router, reenvía **TCP 8443** (o el puerto elegido) hacia la IP local del PC.
3. Salve prueba primero la dirección local y, si no responde, la remota. Recuerda la que funcionó.

Abrir un puerto expone el nodo a Internet, pero sin el certificado del móvil nadie pasa del saludo
TLS. Aun así, mantén Windows actualizado y no compartas el QR ni `salve-perfil.json`: contienen la
clave privada del móvil. Si se filtran, vuelve a crear la red (`--rehacer`) y escanea el QR nuevo.

Si cambias la IP pública o el DDNS, ejecuta `SalveNodo.exe --reconfigurar`: solo se reemite el
certificado del nodo y hay que volver a escanear el QR.

## Guardián ciego (copias de los Salventes)

Un nodo puede ser también **Guardián**: guarda copias de los Salventes que **no puede leer**.

- Actívalo respondiendo «s» a «¿Usar este PC también como Guardián?» en el asistente (o
  `--vault-directory <carpeta>` en modo manual). Las copias quedan en `~/.salve/red/guardian`.
- En Salve: **Salventes (génesis) → Copia al Guardián**. El móvil sella la copia con AES-256-GCM y una
  clave derivada (PBKDF2) de una frase que eliges tú; solo esos bytes viajan por TLS mutuo.
- El Guardián **rechaza cualquier cosa que no llegue sellada** (cabecera `SLVB1`), conserva la versión
  anterior de cada copia y aplica cuotas (96 MB por copia, 768 MB en total).
- Para recuperar los Salventes en un móvil nuevo: escanea el QR del nodo y pulsa **Restaurar** con tu frase.
- **La memoria de la propia Salve** también: **IA y cámara → Continuidad (Guardián)**. Copia a mano o cada
  noche (cargando y con Wi-Fi). Restaurar se aplica al reabrir Salve y conserva la memoria anterior.
- La frase no se guarda en ningún sitio. Si la olvidas, la copia es irrecuperable: es el precio de que
  quien entre en tu PC no pueda leer nada.

## Hogar (Home Assistant local)

Salve puede encender o apagar luces, enchufes y ventiladores, mover persianas, ajustar el clima y
activar escenas a través de tu **Home Assistant local**, siempre con tu confirmación.

- En el asistente, responde «s» a «¿Conectar con Home Assistant?». Indica la URL local (por ejemplo,
  `http://homeassistant.local:8123`), un token de larga duración (Home Assistant → tu perfil →
  Seguridad) y las entidades permitidas, como `light.salon,switch.cafetera,cover.dormitorio`.
- El token se guarda en `~/.salve/red/ha-token` y **no sale del PC**. Solo se aceptan esas entidades y
  esas acciones; las cerraduras no se admiten.
- En Salve: «enciende la luz del salón» → «¿Enciendo «Luz salón»? Di sí para hacerlo». También desde
  **IA y cámara → Mi casa**.
- Modo manual: `--ha-url --ha-token-file --ha-entities`.

## Ubicación del nodo

- **Fija**: la que escribes en el asistente (o `--lat --lon --ciudad --pais --zona`). Salve la
  pinta tal cual en la ficha de la constelación. No se calcula por IP.
- **Desde el móvil del dueño**: si no fijas ninguna, el dueño puede enviarla desde
  **Conexión segura → Ubicación de mi nodo** (GPS puntual revisado o coordenadas a mano), con el
  token de `token-ubicacion`. Con ubicación fija el nodo rechaza estos cambios.
- Mientras la constelación está abierta, Salve pregunta cada 10 s enviando la versión que ya tiene;
  el nodo solo responde con datos si la ubicación cambió.

## Modo manual

```
python salve_private_network.py crear --host-local 192.168.1.20 --host-remoto mipc.duckdns.org --puerto 8443 --nombre "Mi PC"
python salve_compute_node.py --port 8443 \
  --tls-cert ~/.salve/red/nodo.pem --tls-key ~/.salve/red/nodo.key --client-ca ~/.salve/red/ca.pem \
  --token-file ~/.salve/red/token --model mi-modelo --backend http://127.0.0.1:8080 \
  --lat 42.8467 --lon -2.6716 --ciudad Vitoria-Gasteiz --pais España
```

Archivos en `~/.salve/red`: `ca.key/ca.pem` (CA propia, no sale del PC), `nodo.key/nodo.pem`
(servidor), `movil.key/movil.pem` (cliente, viaja en el perfil), `token`, `token-ubicacion`,
`salve-perfil.json` y `salve-perfil.svg` (QR). Se crean con permisos privados.

`--allow-ip` limita además qué direcciones pueden conectar. El backend del modelo debe escuchar en
127.0.0.1 (http o https); nunca se expone directamente.

## Formato del perfil (v2)

```json
{"salve":"nodo","v":2,"n":"Mi PC","u":"https://192.168.1.20:8443","r":"https://mipc.duckdns.org:8443",
 "t":"<token>","ca":"<DER base64>","c":"<cert móvil DER base64>","k":"<clave PKCS8 DER base64>"}
```

En el móvil se guarda cifrado con el almacén de claves de Android y la pantalla bloquea capturas.

## Límites reales

- El TLS mutuo protege el canal y la identidad, no un PC comprometido: quien controle el PC
  controla el nodo.
- Algunos operadores usan CG-NAT y no permiten abrir puertos; ahí la conexión remota no funcionará
  sin un servidor intermedio, que este diseño evita a propósito.
- Los nodos registrados antes de esta versión siguen funcionando con HTTPS normal hasta que
  escanees su QR nuevo.
