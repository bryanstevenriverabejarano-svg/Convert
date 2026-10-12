# Salve realista 2D — 12 de octubre de 2026

Esta sustitución toma como referencia la imagen entregada por el propietario el 12 de octubre: cabello plateado largo, ojos azules, auriculares tecnológicos y traje metálico blanco/negro con luz cian. Los PNG generados tienen canal alfa real; no incluyen pedestal ni texto.

## Cambios

- `AvatarView` usa el dibujado nativo Canvas compartido en conversación, habitación y burbuja. Se retiran WebView, Three.js, el estado del visor, las correcciones de materiales/cabello/cámara y la tarea Gradle que empaquetaba el GLB.
- Se retiran del árbol actual las entregas 3D `art/salve/cabello-cerrado-animada-v2` y `art/salve/blender/modelado_v02`. Sus versiones anteriores siguen recuperables mediante el historial de Git. No se reescribe el historial.
- La base, ocho direcciones, ocho fotogramas de caminar, ocho de baile y ocho poses de apoyo están en `app/src/main/assets/avatar/realistic/` (10,11 MiB). Las ilustraciones anteriores de `app/src/main/assets/avatar/core/` y los otros trajes permanecen disponibles.
- Caminar usa la distancia recorrida para el ciclo y refleja el perfil al moverse a la izquierda. El baile usa la misma secuencia a dos velocidades. Hablar no cancela caminar, bailar o mantener una postura.
- La galería del núcleo muestra las ocho nuevas direcciones. La carga de los cinco PNG se hace fuera del hilo principal y se comparte entre vistas (aprox. 30 MiB de bitmaps RGBA decodificados).
- Se conserva la identidad de aplicación y la variante de prueba. Su nombre visible pasa a `Salve (prueba)`. CI comprueba los nuevos sprites y la ausencia del GLB dentro de la APK.

## Alcance visual

Es animación 2D por fotogramas. No es una malla, ni contiene interpolación esquelética libre, física de cabello o sincronización labial por fonemas. Los gestos de saludo, conversación y escucha son poses ilustradas. La postura acostada de la hoja es una referencia; el descanso dentro de la habitación conserva el sistema de cama existente y usa la pose de ojos cerrados. Los dos bailes comparten una secuencia con distinta cadencia; no son dos coreografías independientes.

Los atlas son imágenes generadas, con variaciones pequeñas de proporción entre poses. `manifest.json` especifica las regiones reales y su escala de referencia para evitar fragmentos de la celda contigua. Los PNG se conservan sin retocar sus píxeles. No se aplica el rig frontal antiguo a estas nuevas imágenes. Las personalizaciones de otros trajes siguen usando su renderizador anterior.

## Verificación realizada

- SHA-256, dimensiones, canal alfa y contenido de los 33 cuadros: correctos.
- Pruebas Java ejecutadas: todos los cuadros de caminata, ambas direcciones, los dos tempos de baile, prioridad del cuerpo mientras hay audio, posturas mantenidas, cierre de sesión, valores de reloj inválidos, inmovilidad y encuadre en pantallas estrechas.
- `AvatarView`, su renderizador, la galería y `AvatarCoreActivity` compilan como Java 11 contra clases Android API 35 y Gson, usando una clase R auxiliar para los dos recursos existentes. Esta comprobación no equivale a compilar toda la APK.
- Dibujado de los recursos y recortes revisado con Canvas en fondos claro y oscuro. El navegador local no pudo iniciarse por restricciones del entorno; la vista HTML no tiene una prueba completa de navegador.
- `git diff --check`: correcto.

Pendientes: compilación Gradle de debug/preview en CI, pruebas Android completas, instalación, audio, fluidez y consumo en el S24 Ultra. La publicación en GitHub fue autorizada explícitamente por el propietario tras preparar esta entrega. La compilación completa y los artefactos APK se comprobarán en el PR; esta revisión local no genera una APK.

## Reproducir

```sh
python3 -m pip install 'Pillow==11.3.0'
python3 scripts/verify_salve_sprites.py
bash scripts/test-salve-sprites.sh
python3 -m http.server 8765
# Abrir http://127.0.0.1:8765/tools/avatar-realistic/
```

La vista previa usa los mismos archivos y regiones. El paquete de entrega también incluye una copia HTML autónoma que incorpora todos los recursos para abrirla sin servidor.

## Generación de imágenes

Se utilizó la herramienta integrada de imágenes, con la fotografía adjunta para la base y la nueva base para las hojas derivadas. Los prompts exigieron preservar rostro, ojos, cabello, traje y colores; cuerpo completo; fondo transparente real; ausencia de pedestal, texto, sombras de suelo y recortes; escala consistente. Cada atlas se solicitó como cuatro columnas por dos filas:

1. Base: frontal de pie, gesto neutro, brazos relajados, cuerpo y pies completos.
2. Vistas: frontal, tres cuartos derecha, perfil derecha, trasera derecha, trasera, trasera izquierda, perfil izquierda, tres cuartos izquierda.
3. Caminata: ocho fases sucesivas de contacto, descenso, paso y ascenso de cada pierna, siempre de perfil hacia la derecha y con balanceo de brazos opuesto.
4. Baile: desplazamiento lateral izquierdo, brazos hacia izquierda y arriba, regreso al centro; repetición a derecha; cámara frontal fija.
5. Poses: sentada, arrodillada, en cuclillas, acostada, saludo, explicación, escucha y ojos cerrados.
