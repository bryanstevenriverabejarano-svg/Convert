# Salve 3D en la pantalla

El avatar Núcleo usa el modelo real del PR #114 en la conversación, la habitación y
la burbuja flotante. El GLB original se conserva en
`art/salve/cabello-cerrado-animada-v2/SALVE_Animada.glb`; Gradle verifica su SHA-256
y lo empaqueta como asset. No hay descargas en ejecución ni un visor remoto.

## Comportamiento

- Respiración y parpadeo en reposo; mirada al escuchar/pensar.
- Caminar, sentarse, arrodillarse, agacharse, saludar y bailar con los clips del GLB.
- Sentarse, arrodillarse y agacharse mantienen el último fotograma.
- La postura tiene prioridad sobre un saludo, la escucha y el habla. Hablar no reinicia el clip.
- Expresiones y apertura de boca usan los morph targets del modelo, controlados por
  `AvatarMotionController`. La boca se cierra al finalizar/cancelar el TTS.
- La boca sigue la temporización existente de TTS; no es sincronización fonética precisa.
- La vista conserva el doble toque para conversar, pulsación larga para opciones y
  el arrastre existente de la burbuja.
- Los otros trajes y diseños personalizados siguen usando sus ilustraciones: sólo
  Núcleo original dispone de este modelo. Dormir conserva la escena ilustrada con cama,
  porque el GLB no incluye un clip de sueño. Las ilustraciones no se modifican.
- Si WebGL, las texturas o el proceso WebView fallan, se muestra el avatar ilustrado.
  Salir y volver a crear la pantalla permite reintentar la carga.

## Implementación

`AvatarView` es el contenedor compartido. Un WebView local transparente muestra
Three.js 0.170.0 y GLTFLoader empaquetados en `viewer.js`; todas las solicitudes se
limitan a tres rutas de assets bajo un origen HTTPS sintético. Las solicitudes no
permitidas reciben 403: nunca se delegan a la red. No hay interfaz JavaScript nativa,
acceso a archivos, credenciales ni texto de conversación en el visor.

El protocolo `Avatar3dState` sólo publica enums y números acotados. Se quitan las
pistas faciales de los clips corporales para que el TTS y las expresiones posean
esos canales. El personaje se ajusta al ancho/alto y limita la resolución a 1,5x.
El bucle apunta a 30 fps, se pausa con la pantalla y caduca si el propietario deja
de enviar estado. El WebView se destruye al retirar la vista.

## Construcción y pruebas

El modelo añade 22.831.928 bytes antes de compresión; el visor añade aproximadamente
566 KiB. Los pesos de los LLM siguen fuera del APK.

Para reconstruir el JavaScript y probar el GLB:

```sh
npm ci --prefix tools/avatar3d --ignore-scripts
node tools/avatar3d/node_modules/playwright/cli.js install chromium
npm run build --prefix tools/avatar3d
npm test --prefix tools/avatar3d
```

El harness usa Chromium y WebGL real con renderizado por software. Comprueba:
81 huesos y diez clips, posturas durante el habla, conservación del último fotograma,
ausencia de deriva en la cabeza, cierre de boca, pausa/reanudación, pérdida del
propietario, burbuja compacta y ausencia de solicitudes externas. Produce capturas
y `result.json` en `build/avatar3d-review/`. CI ejecuta este mismo control, las pruebas
JVM y la construcción Android, y adjunta las capturas a los informes.

Verificación local del 12-10-2026: harness completo aprobado; capturas de reposo,
sentarse, arrodillarse y agacharse inspeccionadas; 27 pruebas JUnit aprobadas sobre
estado 3D, movimiento y rendimiento lógico. Todas las clases del paquete avatar
compiladas contra Android 15 (API 35) y Java 11 con R auxiliar; esa comprobación de
tipos no sustituye la construcción completa de la APK de CI.

Pendiente: medir rendimiento, consumo y composición transparente del WebView en
el S24 Ultra, probar micrófono/altavoz reales y verificar el arrastre del overlay en
el dispositivo. El modelo sigue usando las deformaciones y animaciones revisadas
del PR #114; no se ha rehecho el rig, añadido física de cabello ni creado nuevos trajes 3D.

Fuentes de implementación: [GLTFLoader](https://threejs.org/docs/pages/GLTFLoader.html),
[AnimationMixer](https://threejs.org/docs/pages/AnimationMixer.html) y
[contenido local en WebView](https://developer.android.com/develop/ui/views/layout/webapps/load-local-content).
