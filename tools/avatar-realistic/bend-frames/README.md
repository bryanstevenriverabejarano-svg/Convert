# Transiciones de postura de Salve

Ilustraciones aprobadas por Bryan el 26-10-2026 (1024×1536, RGBA). En `assets/avatar/realistic/bend.png` (celdas de 448×672):

| Fotograma | Archivo | Pose |
| --- | --- | --- |
| 0 | `T0_rodillas_dobladas.png` | de pie, empezando a doblar las rodillas |
| 1 | `T1_medio_sentada.png` | a medio sentarse |
| 2 | `T1b_agachandose.png` | agachándose, más abajo |
| 3 | `T2_rodilla_bajando.png` | una rodilla bajando |
| 4 | `T2b_rodilla_al_suelo.png` | la rodilla casi en el suelo |

`PoseTransition` las recorre al cambiar de postura, y al revés al levantarse:

- Sentarse: 01 → 0 → 1 → 25
- Agacharse: 01 → 0 → 1 → 2 → 27
- Arrodillarse: 01 → 0 → 3 → 4 → 26
