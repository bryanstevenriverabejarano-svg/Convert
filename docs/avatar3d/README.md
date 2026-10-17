# Avatar 3D de Salve (en pausa)

`salve.glb` (≈22 MB) y `app/src/main/java/salve/avatar/Avatar3dMotion.java` vienen del PR #170.

Se sacó del APK porque:
- Ninguna vista lo usa todavía (Avatar3dMotion no se llama desde ningún sitio).
- Filament 1.77.x exige compileSdk 37 y kotlin-stdlib 2.4 (el proyecto usa 36 y Kotlin 2.2).
- La CI prohíbe `.glb` en el APK (el 3D se retiró antes a propósito).

Para activarlo: elegir una versión de Filament compatible, conectar una vista 3D al escenario,
mover el `.glb` a `app/src/main/assets/` y actualizar la comprobación de la CI.
