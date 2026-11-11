# Caché del motor local: orden del prompt (2026-11-08)

## Qué pasaba
El motor GGUF (llama.cpp) ya reutiliza lo que coincide con el principio del prompt anterior
(`salve_llama.cpp`, `common_prefix`). Pero el prompt cognitivo empezaba por `SYSTEM_STATE`
(hora, RAM, fase), que cambia en cada inferencia, seguido de `TASK_STATE` y del catálogo. La
caché se corta en el primer carácter distinto, así que en cada inferencia se releía casi todo.
Además, al llegar un recibo de herramienta se recalculaban los recortes de memoria e historial.

## Cambio
- `CognitivePrompt.cacheFriendly`: en el motor GGUF, la línea `SYSTEM_STATE` va al final (misma línea, otra posición).
- Orden de datos por estabilidad: SELF_MODEL, HISTORY, MEMORY_CONTEXT, CONTEXT, USER_INPUT, TOOL_CATALOG, TASK_STATE, TOOL_RESULTS.
- Presupuesto estable por turno: memoria, modelo propio e historial se recortan con lo que no cambia entre inferencias (reglas, catálogo del turno, márgenes fijos para estado y tarea). Los recibos usan una reserva pequeña (máx. 900 caracteres) más lo que sobre.
- Las reglas (COMPACT/MINIMAL) se eligen con el catálogo completo del turno, no con el visible, para que no cambien en la última inferencia.
- Medición: cada inferencia y cada turno dejan en el registro de ejecución «RENDIMIENTO DEL MODELO» y «TIEMPO DEL TURNO» (tokens leídos, ya en caché, segundos).

## Medido en JVM (prompt de 7.000 caracteres, perfil de E4B en GGUF)
| Paso | Antes | Ahora |
|---|---|---|
| 1ª → 2ª inferencia (tras recibo) | 26 % reutilizado | 76 % |
| 2ª → reparación de formato | 26 % | 78 % |
| Turno siguiente (charla) | 26 % | 39 % |

Coste: en la primera inferencia de un turno con herramientas, la memoria baja ~25 % (944 → 709
caracteres) y el recibo llega algo más corto (1.014 → 634). En la segunda pasada la memoria tiene
más sitio que antes (538 → 709). Ajustable con `RECEIPTS_RESERVE_MAX` y `TASK_METADATA_ALLOWANCE`.

## Pendiente
Validar en el S24 Ultra con el registro de ejecución: los segundos reales dependen de la
velocidad de lectura de la gráfica/procesador, que aquí no se puede medir.

## Experimento descartado: historial anclado (2026-11-11)
Medido en JVM con una charla de 40 turnos: cuando el historial ya no cabe, la ventana se desliza en cada
turno y la caché se corta al empezar `HISTORY`. Reutilizado entre turnos: 42 % con 7.000 caracteres y
31–36 % con 12.000 (antes de llenarse llega al 73 %).

Se probó a empezar la ventana en un mensaje «ancla» (fijo durante varios turnos). Resultado: con 12.000
caracteres sube a 51–56 %; con 7.000 y respuestas largas (el caso real de Salve, 4–5 mensajes en ventana)
no sube nada, y siempre cuesta 1–2 mensajes de historial. **No se integra**: en el contexto que usa Bryan no
gana velocidad y pierde contexto. Queda como referencia por si el contexto crece.

Sí se integra: las descripciones de `device.sense`, `device.control` y `device.setting` eran largas (la de
sentidos, ~470 caracteres) y con poco presupuesto dejaban fuera a `memory.lookup` y `web.search`. Acortadas.

