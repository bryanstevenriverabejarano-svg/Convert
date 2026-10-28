# Giros laterales G1–G4 de Salve
Las cuatro ilustraciones originales RGBA de 1024 × 1536 están en esta carpeta, dentro de la historia de Git.

| Imagen | Dirección | Transición |
| --- | --- | --- |
| G1.png | Derecha | 01 → 02 |
| G2.png | Derecha | 02 → 03 |
| G3.png | Izquierda | 01 → 08 |
| G4.png | Izquierda | 08 → 07 |

Los cuatro archivos se reutilizan **sin modificar sus bytes** en
`app/src/main/assets/avatar/realistic/turn_g1.png` a `turn_g4.png`, por eso
Git puede referenciar sus blobs existentes. El cargador Android los decodifica
con `inSampleSize = 2`, reduciendo memoria sin alterar su escala en pantalla.
`AvatarTurn` emite índices 8–11, que `RealisticSpriteRenderer` dibuja
con los cuatro PNG originales. No existe un reflejo horizontal para simular
el lado contrario.

Los antiguos G_a–G_d permanecen **solo como archivo histórico aquí**, nunca
se cargan en la app actual. Las vistas 01–08 y demás animaciones siguen intactas.

Verificar:
```bash
python3 scripts/verify_salve_sprites.py
bash scripts/test-salve-sprites.sh
```
El instalador temporal de PR #274 se retiró después de integrar los recursos.
