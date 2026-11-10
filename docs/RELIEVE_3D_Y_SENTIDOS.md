# Relieve 3D del cuerpo astral y sentidos completos del teléfono — 9 de noviembre de 2026

Dos piezas que acercan a Salve a habitar el dispositivo: su figura gana profundidad a partir de las
ilustraciones que ya tiene, y ella puede medir con todos los sensores del teléfono y accionar sus mandos
físicos cuando Bryan se lo pide.

## 1. Relieve 3D con máscara de profundidad

No es una malla 3D nueva ni vuelve el visor WebView/GLB retirado el 12 de octubre: es la misma `base.png`
aprobada, dibujada con el Canvas nativo de siempre.

- **Máscara**: `scripts/build_astral_body.py` ya separaba cuerpo y pelo con cápsulas medidas a mano. Ahora
  esas cápsulas se inflan como volúmenes redondeados (cuerpo, hasta ≈ +78 px hacia quien mira) y lo que
  queda fuera cae hacia atrás (pelo y colas del traje, −62 px) con una transición suave. El resultado es una
  profundidad por vértice de la malla existente (`mesh.depth` + `mesh.depthRange` en `astral.json`).
- **Giro**: `AstralBodyRig.deform` aplica un giro ortográfico del relieve alrededor del eje del cuerpo
  (`x' = cx + (x − cx)·cos + z·sin`). Lo cercano se desplaza más que lo lejano: el cuerpo se separa del pelo.
  Ojos y boca dibujados siguen a la cara con `AstralBodyRig.parallax`.
- **De dónde sale el giro**: `AvatarTiltSensor` lee el giroscopio solo mientras la figura está en pantalla
  (no en la burbuja flotante) y `AstralParallax` lo integra con retorno al frente en ≈ 2,5 s. Cada muestra
  mueve el punto de vista y se descarta. Sin giroscopio, la figura queda de frente como antes.
- **Límites**: ±14° a los lados y ±9° arriba/abajo. Más allá, una ilustración plana se deforma. Solo la vista
  frontal (`base`) tiene relieve; caminar, bailar, posturas y los giros G1–G4 siguen siendo sus fotogramas.
- **Cuerpos**: los cinco del Vestuario (`realistic`, `realistic_inclinada`, `realistic_abierto`,
  `realistic_2610`, `realistic_v1`). Para regenerar solo el relieve sin tocar PNG ni el resto del rig:
  `python3 scripts/build_astral_body.py <cuerpo> --solo-relieve`.
- **Propiocepción**: su esquema corporal menciona el relieve y, si el teléfono está inclinado, desde qué
  lado la ven.

## 2. Sentidos: `device.sense` (solo lectura)

`SensorSnapshot` cubría cinco lecturas dentro de una sesión pedida con la frase literal «activa sensores».
Esa ruta sigue igual. Además, cuando el turno de Bryan (identidad verificada) pregunta por algo medible,
el catálogo ofrece al modelo `device.sense {grupo}` y él decide si medir:

| Grupo | Qué mide |
| --- | --- |
| ENTORNO | luz, proximidad, temperatura ambiente, humedad, presión (y altitud estimada), campo magnético |
| MOVIMIENTO | aceleración total, aceleración propia, giro, contador de pasos |
| ORIENTACION | postura del teléfono (gravedad) y rumbo de brújula (vector de rotación) |
| ENERGIA | batería, carga, temperatura y voltaje de la batería, ahorro de energía |
| SISTEMA | modelo, RAM, almacenamiento, red, volumen, brillo, estrés térmico, pantalla, tiempo encendido |
| INVENTARIO | lista de todos los sensores que declara el teléfono |

- Es una **ráfaga**: `SistemaSensorial.percibir` registra los sensores del grupo, espera como mucho 0,7 s,
  los suelta y devuelve texto con unidades e interpretación (`DeviceSenses`). No guarda historial, no
  alarga sesiones y no infiere emociones.
- Lo que el teléfono no tiene sale como «no disponible»; lo que no contestó, como «sin lectura». El modelo
  recibe eso como recibo (`PERCEPCION_REAL`) y no debe completar huecos.
- El contador de pasos necesita el permiso de actividad física, que esta PR **no** pide: hasta entonces
  dirá «sin lectura».

## 3. Mandos: `device.control`

Linterna (encender/apagar), volumen multimedia (subir, bajar, silenciar o fijar un porcentaje) y una
vibración corta. Como con la música: `DeviceControlIntent` reconoce el mando en la frase de Bryan, el
modelo decide usarlo y el ejecutor comprueba que la acción propuesta coincide; el porcentaje sale de la
frase, no del modelo. El recibo trae el estado real después (p. ej. volumen antes y ahora). Una negación
(«no enciendas…») no se ofrece. Permiso nuevo: `VIBRATE` (normal, sin diálogo).

Lo que envía, llama, compra, borra o toca la pantalla sigue por sus rutas con revisión; esto no las cambia.

## 4. Ajustes del sistema: `device.setting` (Shizuku, sin root) — 10 de noviembre de 2026

Wifi, Bluetooth, datos móviles, modo avión, no molestar, brillo (porcentaje, máximo, mínimo) y brillo
automático. Una app normal no puede cambiarlos; con [Shizuku](https://shizuku.rikka.app/) Salve los acciona
con identidad de shell (la de ADB), sin desbloquear el bootloader ni tocar Knox.

- **Lista cerrada.** `SystemSetting` reconoce el ajuste en la frase de Bryan y lo traduce a órdenes fijas
  (`svc wifi enable`, `settings put system screen_brightness N`…). El modelo decide usarlo; no puede
  invertirlo ni cambiar de ajuste, y nadie envía texto de shell.
- **Proceso privilegiado mínimo.** `ShizukuSettingsService` solo acepta «ajuste nº N, valor V» y vuelve a
  consultar la tabla en su propio proceso; después lee el estado con `settings get` y eso es el recibo
  (`AJUSTE_REAL`).
- **Sin Shizuku no finge.** Si no está instalada, no está en marcha o falta el permiso, lo dice y no cambia
  nada; la primera vez pide el permiso en Shizuku.
- **Puesta en marcha (una vez):** instalar Shizuku, activar *Opciones de desarrollador → Depuración
  inalámbrica*, emparejar y pulsar *Iniciar* en Shizuku. Sin root hay que volver a iniciarla tras cada
  reinicio del teléfono. Luego, la primera orden («activa el wifi») mostrará el diálogo de permiso.
- Pendiente en el S24 Ultra: todo el camino real (enlace con Shizuku, cada orden y su lectura). One UI
  puede tratar algún ajuste de forma distinta; el recibo dirá lo que el sistema informe.

## Validación

- JVM: `AstralBodyTest` (relieve en los cinco cuerpos, giro acotado, malla sin pliegues, retorno al frente,
  integración del giroscopio), `DeviceSensesTest`, `DeviceControlIntentTest`, y las pruebas existentes de
  catálogo, planificador, música, apps, funciones propias y sensores.
- Vista previa del relieve renderizada fuera del teléfono con la misma fórmula (±14°): sin rasgaduras.
- **Pendiente en el Galaxy S24 Ultra**: sentido del giro al inclinar (si se nota invertido, es cambiar un
  signo en `AstralParallax`), fluidez a 30 fps, lecturas reales de cada sensor y los tres mandos.
