# Evolución del cuerpo de Salve

| Versión | Carpeta | Aprobada | Qué tiene |
| --- | --- | --- | --- |
| **Salve actual** (por defecto) | `assets/avatar/realistic/` | 26-10-2026 | `base.png` 1141×1700 (recortada y escalada desde la ilustración aprobada de 2731×4096) y capa astral propia |
| **Salve v1** (registro) | `assets/avatar/realistic_v1/` | 12-10-2026 | Su primer cuerpo realista, intacto: `base`, `views`, `walk`, `dance`, `poses` y su capa astral (los mismos bytes que antes, solo cambiados de carpeta) |

Para cambiar de versión, en el Vestuario: *Opciones → Vestuario y habitación → Evolución de Salve*. La elección se guarda en `salve_avatar_body`. La ropa, los gestos y la plantilla «Núcleo» no cambian: solo cambia el cuerpo que se dibuja (`AvatarBodyVersions`).

## Cuerpo vivo de la versión actual

`python3 scripts/build_astral_body.py realistic` genera, a partir de `base.png`, tres archivos:

- **`astral_eyes.png`**: párpados medio cerrados y cerrados, para el parpadeo natural.
- **`astral.json`**: la malla 38×56 con los pesos del pelo (ondea) y de la cabeza (se inclina desde el cuello), además de cuello, núcleo y pies. Brazos, torso y piernas quedan fijos. Incluye `hairTop` y `hairSpan`, el inicio y el largo de la caída del pelo; la v1 usa por defecto 300 y 700.
- **`astral_glow.png`**: la máscara de las líneas que brillan. Recoge el azul intenso de los paneles (H 88–128, S > 110) y el cian de las líneas finas (H 80–104, S > 40), y deja fuera el pelo lavanda.

Con `realistic_v1` el mismo script reproduce bit a bit los archivos de la v1.

## Luz que recorre las líneas

`AstralBody` avanza una fase `flow` (0–1) sin parar, con una velocidad y una intensidad que dependen de lo que hace:

| Estado | Velocidad |
| --- | --- |
| Reposo | 0,26 vueltas/s |
| Escuchar | 0,38 |
| Hablar | 0,42 + boca |
| Pensar | 0,6 |
| Dibujar | 0,75 |

`RealisticSpriteRenderer.drawFlow` pinta la máscara en una capa y la recorta con un degradado radial (DST_IN) que sale del núcleo del cuello. Así dos frentes cian bajan por las líneas hasta manos y pies, con bordes suaves.

## Posturas sin hojas propias

La versión actual solo tiene `base.png`, así que `RealisticSpriteRenderer` usa su cuerpo vivo en todas las posturas:

- **Andar, gestos y escuchar:** de pie, con pelo, cabeza, respiración y luz.
- **Bailar:** el mismo cuerpo con balanceo al ritmo.
- **Dormir:** tumbada en la cama con los ojos cerrados.

Si más adelante se aprueban hojas de pasos, baile o posturas para ella, basta con añadirlas a su `manifest.json`.
