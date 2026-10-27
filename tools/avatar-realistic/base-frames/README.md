# Bases de Salve (27-10-2026)

| Archivo | Carpeta | Vestuario |
| --- | --- | --- |
| `base_pecho_cubierto.png` | `assets/avatar/realistic/base.png` | Salve actual |
| `base_pecho_descubierto.png` | `assets/avatar/realistic_abierto/base.png` | Salve · escote abierto |
| (la 01 de las 32 ilustraciones) | `assets/avatar/realistic_2610/base.png` | Salve del 26 de octubre |

Las dos variantes usan `"extends": "realistic"` en su manifiesto: su base y su capa viva son propias y
el resto de hojas (andar, girarse, bailar, posturas, dormir, despertar) son las de la actual.
De frente, cada versión se dibuja con su propia base.

La capa viva de las nuevas (`python3 scripts/build_astral_body.py realistic|realistic_abierto`) usa
párpados y bocas H1–H3 pintados sobre la propia cara, provisionales hasta tener ilustraciones con los
ojos cerrados y la boca abierta de esta misma imagen.
