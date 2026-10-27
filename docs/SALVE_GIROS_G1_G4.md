# Sustitución de giros laterales de Salve (G1–G4)

Esta rama contiene el instalador seguro para cambiar la hoja de giros intermedios de la **Salve actual**, `avatar/realistic`, conservando las demás ilustraciones y los cuerpos históricos.

**Estado: pendiente de agregar los cuatro archivos binarios G1.png, G2.png, G3.png y G4.png.** Son los PNG originales de 1024×1536 entregados el 27-10-2026; no están en este PR todavía. No se debe fusionar como «integración terminada».

## Asignación

| Fotograma en `turn.png` | Imagen | Transición |
| --- | --- | --- |
| 0 | G1 | vista 01 → 02 (derecha) |
| 1 | G2 | vista 02 → 03 (derecha) |
| 2 | G3 | vista 01 → 08 (izquierda) |
| 3 | G4 | vista 08 → 07 (izquierda) |

`AvatarTurn` recorrerá `0→G1→02→G2→03` y `0→G3→08→G4→07`. El giro completo seguirá incluyendo la espalda. No se reflejan los originales para simular el lado opuesto.

## Para terminar la integración

1. Guardar los cuatro PNG en una carpeta local, sin renombrarlos.
2. Ejecutar desde una copia actualizada de Convert: `python3 -m pip install Pillow` y `python3 scripts/install_salve_turns_g1_g4.py /ruta/a/carpeta`.
3. Comprobar que se han añadido los 4 originales a `tools/avatar-realistic/turn-frames`, se ha sustituido `avatar/realistic/turn.png`, actualizado su SHA-256 en `manifest.json` y los recorridos de `AvatarTurn.java`.
4. Ejecutar `python3 scripts/verify_salve_sprites.py` y `bash scripts/test-salve-sprites.sh`, y compilar la APK mediante CI.
5. Validar visualmente en el teléfono que no hay saltos del cabello, rostro ni calzado, y que la versión elegida del cuerpo conserva sus animaciones restantes.

Se conservan por ahora los originales `G_a–G_d` antiguos como registro, para no borrar recursos que pueda usar otra rama.
