# Vista previa del rig de Salve

Sirve la raíz del repositorio con un servidor HTTP local, por ejemplo:

```sh
python3 -m http.server 8000 --bind 127.0.0.1
```

Abre `http://127.0.0.1:8000/tools/avatar-preview/index.html` en un navegador con WebGL para revisar 12 poses fijas, o `http://127.0.0.1:8000/tools/avatar-preview/demo.html` para reproducir la secuencia breve.

La vista utiliza el PNG original, las dos prendas frontales y el rig de `app/src/main/assets/avatar/rig.json`. La malla WebGL reproduce las mismas fórmulas, pesos y jerarquía que `AvatarRig`, con vértices compartidos entre triángulos. Se retiraron los recortes independientes y el pelo de relleno de este renderer. Las 18 muestras incluyen las prendas y acentos; el compositor conserva la cabeza original. No usa recursos web externos. No es una ejecución de Android ni una medición de su rendimiento.

`motion-frames.json` se exporta desde `AvatarMotion`, el director de producción. Contiene un escenario programado de saludo, escucha y explicación, sin modelo ni audio reales. La demostración selecciona las tres prendas en momentos fijados para revisar su aspecto; esas selecciones no son decisiones tomadas por un modelo en el navegador. Para actualizarlo tras cambiar el director, usa un JDK y una carpeta temporal de clases:

```sh
mkdir -p /tmp/salve-preview-classes
javac --release 11 -d /tmp/salve-preview-classes app/src/main/java/salve/avatar/MotionPreferenceProfile.java app/src/main/java/salve/avatar/AvatarMotion.java app/src/main/java/salve/avatar/AvatarMotionProtocol.java tools/avatar-preview/ExportMotionFrames.java
java -cp /tmp/salve-preview-classes ExportMotionFrames > tools/avatar-preview/motion-frames.json
```

La comparación automática entre los canvas Original y Neutro se hace a 512 × 768, incluyendo alfa. La validación inicial obtuvo 0 diferencias en 393.216 píxeles. También se revisan expresiones y poses extremas: la igualdad del reposo no acredita por sí sola la calidad del movimiento.
