# Giros laterales reales G1–G4 — integración Android
**Estado:** los PNG subidos en el PR #274 ya están integrados como recursos activos
en `main` tras fusionar el PR de implementación. Este cambio reemplaza el atlas
`avatar/realistic/turn.png` antiguo: los cuatro PNG son los nuevos fotogramas
individuales y no se reflejan artificialmente.

`AvatarTurn` usa:
- Derecha: 01 → G1 → 02 → G2 → 03.
- Izquierda: 01 → G3 → 08 → G4 → 07.
- `SPIN` conserva las vistas de espalda y enlaza ambos lados.

**Recursos Android:** `app/src/main/assets/avatar/realistic/turn_g1.png` a
`turn_g4.png`; `manifest.json` declara cada SHA-256 y el fotograma completo.
Al decodificar para Canvas se utiliza un muestreo 2× para limitar RAM.
Las variantes históricas siguen usando `views` como respaldo; no se alteran
`walk`, `walk_inbetween`, `dance`, `poses`, `sleep` ni `wake`.

**Pruebas:** `python3 scripts/verify_salve_sprites.py` valida integridad,
tamaño, transparencia y fotogramas; `bash scripts/test-salve-sprites.sh`
comprueba el orden de giro y el comportamiento existente. CI debe verificar
la compilación final. La continuidad visual en un teléfono físico sigue
pendiente de una revisión de la APK.
