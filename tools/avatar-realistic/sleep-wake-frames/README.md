# Salve duerme y se despierta

Ilustraciones aprobadas por Bryan el 26-10-2026. `scripts/build_salve_sleep_wake.py` les quita la neblina
oscura del fondo y monta dos hojas en `assets/avatar/realistic/`:

**sleep.png** (celdas horizontales de 672×448, cabeza a la izquierda, sobre la almohada)

| Fotograma | Archivo | Uso |
| --- | --- | --- |
| 0 | `D1_dormida_de_lado.png` | duerme de lado (reflejada) |
| 1 | `D2_dormida_acurrucada.png` | se da la vuelta: acurrucada |
| 2 | `D3_dormida_hecha_un_ovillo.png` | hecha un ovillo |
| 3 | `D4_abre_los_ojos.png` | abre los ojos al despertar |

Mientras duerme cambia de postura cada 40 s (0 → 1 → 2) con un fundido y respira despacio.

**wake.png** (celdas de 448×672, de rodillas)

| Fotograma | Archivo | Uso |
| --- | --- | --- |
| 0 | `E1_se_frota_el_ojo_bostezando.png` | despertar, bostezo |
| 1 | `E2_se_estira.png` | despertar, «estírate» |
| 2 | `E3_se_estira_del_todo.png` | despertar, «estírate» |
| 3 | `E4_bosteza_tapandose.png` | despertar, «bosteza» |
| 4 | `E5_se_frota_el_ojo_despierta.png` | despertar |
| 5 | `E6_mano_en_la_mejilla.png` | despertar; de rodillas escuchando |
| 6 | `E7_guino_y_saludo.png` | despertar, «guíñame un ojo» |

Al despertar: D4 en la cama → E1 → E2 → E3 → E4 → E5 → E6 → E7 → se levanta (bend 4, 3, 0). ≈ 8,7 s.
Si se le pide otra cosa a medias, lo deja y obedece.
