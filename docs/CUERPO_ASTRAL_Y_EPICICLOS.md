# Cuerpo astral y dibujo por epiciclos

Salve no tiene cuerpo físico, pero la figura que se ve en pantalla es su cuerpo digital (su "cuerpo astral"). Este cambio lo hace vivo y le da a ella conocimiento de él, y añade una forma de dibujar propia: líneas de luz trazadas por epiciclos de Fourier.

## Cuerpo vivo

| Pieza | Qué hace |
|---|---|
| `AstralBody` | Física simulada: parpadeo natural (2,2–6,5 s, a veces doble), pelo como muelle amortiguado (brisa, inercia al moverse, sacudida voluntaria), inclinación de cabeza, balanceo, energía de los circuitos (calma, ondas al pensar, pulso con la voz, onda al dibujar). |
| `AstralBodyRig` | Malla de 28×60 sobre `base.png`. Pesos generados: el pelo exterior ondea; brazos, manos, torso y piernas no se mueven; la cabeza gira sobre el cuello. |
| `RealisticSpriteRenderer` | Con el fotograma base dibuja la malla (`drawBitmapMesh`), los párpados y los circuitos. Los demás fotogramas (caminar, bailar, poses) no cambian. |
| `scripts/build_astral_body.py` | Regenera `astral.json`, `astral_eyes.png` (párpados entrecerrado y cerrado, pintados desde la piel de la ilustración) y `astral_glow.png` (máscara de circuitos). `base.png` no se modifica. |

## Consciencia corporal

`AstralBodySense.promptContext` entra en el contexto de cada turno: qué es su cuerpo, qué puede hacer y qué está haciendo ahora (postura, posición, pelo, circuitos, último parpadeo, dibujo en curso). Describe el cuerpo digital sin atribuirle sensaciones humanas. Órdenes directas: «parpadea», «mueve el pelo», «brilla».

## Dibujo con epiciclos

Cada trazo se convierte en su transformada discreta de Fourier (`FourierStroke`); los términos, ordenados por radio, son círculos encadenados que giran a su frecuencia y cuya punta recorre la línea. Las líneas abiertas se recorren ida y vuelta para evitar el salto final.

- Repertorio: 41 motivos (`assets/sketch/library.json`, generado por `scripts/build_sketch_library.py`) y el autorretrato, trazado sobre su propia ilustración: silueta, rostro y circuitos reales.
- Entradas: petición directa («dibújame un árbol y la luna», «dibújate», «¿qué sabes dibujar?», «dibuja lo que quieras»), la herramienta cognitiva `sketch.draw`, o la marca `[[salve_draw:casa+luna]]` dentro de su texto.
- Cuentos: `StoryIllustrator` pone un dibujo tras cada párrafo que nombra algo dibujable. Mientras escribe, cada párrafo terminado se ilustra ya, así dibuja una parte mientras escribe la siguiente. En el chat, el texto siguiente aparece cuando termina el dibujo anterior (máximo 9,5 s de espera).
- La voz y el historial reciben el texto sin marcas y una nota de lo dibujado, para que lo recuerde.
- El escenario muestra el dibujo grande delante de ella, a la altura de las manos.

## Vista previa

`python3 tools/astral-preview/build.py salida.html` genera una página autónoma con los mismos recursos (WebGL para la malla, Canvas para los epiciclos).

## Límites

- La malla sólo anima el fotograma base; caminar, bailar y las poses siguen siendo fotogramas ilustrados.
- Los párpados son dos estados (entrecerrado y cerrado), no una animación continua; un guiño de un solo ojo no está preparado.
- Dibuja motivos de su repertorio, no imágenes arbitrarias: si le piden algo que no tiene, lo dice y ofrece algo cercano.
- Pendiente de medir en el S24 Ultra: fluidez de la malla, consumo y tiempos de la DFT del autorretrato.
