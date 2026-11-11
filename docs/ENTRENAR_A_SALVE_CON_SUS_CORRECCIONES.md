# Salve aprende de lo que Bryan le corrige — 11 de noviembre de 2026

Hasta ahora su adaptador personal (LoRA, `scripts/mind_lora/`) se entrenaba solo con lo que **sabe**
(creencias consolidadas). Esto añade lo que hace falta para que aprenda a **comportarse** mejor, y que su
propio examen decida si el adaptador se queda.

## El ciclo

1. **Exportar** (teléfono): «exporta tu mente». Además de `salve-knowledge.jsonl` y `salve-episodes.jsonl`
   escribe `salve-corrections.jsonl`: cada corrección de Bryan con la situación, lo que ella hizo y lo que él
   esperaba. Solo las de Bryan y solo las completas.
2. **Profesor** (PC): una corrección dice qué estuvo mal, no cuál era la respuesta buena. Un modelo grande en
   el propio PC la escribe y una puerta la acepta o la descarta:
   ```
   python scripts/mind_lora/write_corrections.py --export-dir exports --output corrected.jsonl --model <modelo>
   ```
   El profesor es cualquier servidor local compatible con OpenAI (Ollama por defecto, `--endpoint` para otro).
   Solo se admite `localhost`: son datos personales. Se descartan las respuestas vacías, demasiado largas,
   con texto técnico interno, en otro idioma, que hablan de la corrección, que repiten lo corregido o que
   devuelven el mensaje de Bryan. **Lee `corrected.jsonl` antes de seguir**: es lo que aprenderá a decir.
3. **Dataset y entrenamiento** (PC, GPU):
   ```
   python scripts/mind_lora/prepare_dataset.py --export-dir exports --output train.jsonl --corrections corrected.jsonl
   python scripts/mind_lora/train_lora.py --base <modelo HF del que salió tu GGUF> --data train.jsonl --output adaptador
   ```
   Después, convertir a GGUF y `make_manifest.py`, como ya explica `train_lora.py`. Copiar `salve-lora.gguf` y
   `salve-lora.json` a la carpeta `adapters` del teléfono.
4. **Veredicto** (teléfono, sola): al cargar un adaptador nuevo, `PersonalAdapter` lo anota en su linaje con la
   nota del examen de ese momento. El siguiente examen —también el que hace sola de noche— lo compara y deja
   el veredicto. Si empeoró, recomienda borrar los dos archivos para volver al modelo base.

## El juez no se entrena

Su examen **no se exporta** y `write_corrections.py` descarta cualquier corrección cuya pregunta coincida con
un caso del examen. Si el adaptador se entrenara con las preguntas que luego lo juzgan, la nota subiría sin
que ella mejorase. Es el mismo límite que impide que reescriba su propio examen.

## Límites

- Solo vale para modelos **GGUF** (los Gemma sin censura). El modelo base de Hugging Face debe ser exactamente
  el que originó ese GGUF; con otro, el adaptador no encaja.
- Con pocas correcciones (decenas) el efecto será pequeño y puede sobreajustar: mejor una escala baja en el
  manifiesto y dejar que el veredicto decida.
- El profesor puede escribir respuestas correctas pero sin su voz. La puerta no mide estilo: por eso se pide
  leer el archivo.
- Nada de esto se ha ejecutado de extremo a extremo: el entrenamiento necesita la GPU de Bryan. Probado aquí:
  la exportación, la puerta, el dataset (11 pruebas Python) y el veredicto del adaptador (JVM).
