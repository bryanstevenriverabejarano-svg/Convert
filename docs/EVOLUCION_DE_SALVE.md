# Evolución del cuerpo de Salve

| Versión | Carpeta | Aprobada | Qué tiene |
| --- | --- | --- | --- |
| **Salve actual** (por defecto) | `assets/avatar/realistic/` | 26-10-2026 | 32 ilustraciones aprobadas (1024×1536) montadas por `scripts/build_salve_sheets.py`: `base` (la 01) y las hojas `views` (01–08), `walk` (09–16), `dance` (17–24) y `poses` (25–32: sentada, de rodillas, agachada, tumbada, saludo, explicar, escuchar y ojos cerrados), con el mismo orden que la v1. Más su capa astral |
| **Salve v1** (registro) | `assets/avatar/realistic_v1/` | 12-10-2026 | Su primer cuerpo realista, intacto: `base`, `views`, `walk`, `dance`, `poses` y su capa astral (los mismos bytes que antes, solo cambiados de carpeta) |

Para cambiar de versión, en el Vestuario: *Opciones → Vestuario y habitación → Evolución de Salve*. La elección se guarda en `salve_avatar_body`. La ropa, los gestos y la plantilla «Núcleo» no cambian: solo cambia el cuerpo que se dibuja (`AvatarBodyVersions`).

## Cuerpo vivo de la versión actual

`python3 scripts/build_astral_body.py realistic` genera, a partir de `base.png`, tres archivos:

- **`astral_eyes.png`**: párpados medio cerrados (sintéticos) y cerrados. Los cerrados son los dibujados en la ilustración 32, alineada con la 01 al píxel y fundida con borde suave; el recorte está en `scripts/astral_sources/`.
- **`astral.json`**: la malla 34×52 con los pesos del pelo y de las colas del traje (ondean) y de la cabeza (se inclina desde el cuello), además de cuello, núcleo y pies. Brazos, torso y piernas quedan fijos. Incluye `hairTop` y `hairSpan`, el inicio y el largo de la caída del pelo; la v1 usa por defecto 300 y 700.
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

## Animaciones

Con sus 32 ilustraciones, la versión actual tiene hojas propias para girarse, andar, bailar y todas sus posturas, igual que la v1. La luz que recorre las líneas, el parpadeo y el pelo vivo se aplican a la base, que es la postura de reposo en la que pasa la mayor parte del tiempo.

Si a una versión futura le faltara alguna hoja, `RealisticSpriteRenderer` usa su cuerpo vivo en esa postura: de pie, con balanceo al bailar y tumbada con los ojos cerrados al dormir.
