# Salve — marcha con 16 fotogramas

Las ilustraciones **A1.png–A8.png** son fuentes RGBA independientes a **1024 × 1536**. Son dibujos intermedios incorporados el 2026-10-26.

| Secuencia reproducida | Par de referencias |
| --- | --- |
| 09 → A1 → 10 | A1 entre 09 y 10 |
| 10 → A2 → 11 | A2 entre 10 y 11 |
| 11 → A3 → 12 | A3 entre 11 y 12 |
| 12 → A4 → 13 | A4 entre 12 y 13 |
| 13 → A5 → 14 | A5 entre 13 y 14 |
| 14 → A6 → 15 | A6 entre 14 y 15 |
| 15 → A7 → 16 | A7 entre 15 y 16 |
| 16 → A8 → 09 | A8 cierra el ciclo |

En tiempo de ejecución, los fotogramas pares 0,2,…,14 de `RealisticSpriteAnimation` se toman de `walk.png` (09–16), mientras los impares 1,3,…,15 se toman de `walk_inbetween.png` (A1–A8). El atlas de transición es 4 columnas × 2 filas, con celdas RGBA **448 × 672**, y está declarado en `manifest.json`. Los PNG fuente completos se guardan aquí para futuras correcciones y regeneraciones; no se cargan todos en la memoria del dispositivo.

El ritmo global de caminata sigue el reloj de locomoción original: se incrementa la resolución temporal, no la velocidad. Las versiones antiguas que no incluyan `walk_inbetween.png` conservan sus ocho fotogramas, sin fallar ni saltar al sprite de reposo.

**Revisión visual pendiente:** estas ocho imágenes se generaron tomando la imagen 01 como referencia de identidad y pose. No se crearon calculando diferencias exactas entre los fotogramas 09–16; es necesario comprobar en la APK la continuidad anatómica y corregir cualquier salto de piernas, pelo o colas. Tampoco se ha realizado una prueba de compilación Android en esta operación.
