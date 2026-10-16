# H2: memoria semántica multilingüe

## Decisión y alcance

Se sustituye el contrato `gte-small`/WordPiece por **multilingual-e5-small INT8**, con
prefijos distintos para consultas (`query: `) y recuerdos (`passage: `), media de los
tokens atendidos y normalización L2. Son vectores de 384 componentes. El contexto del
codificador es de 256 tokens; no es el contexto de Gemma ni su límite de respuesta.

La normalización Unicode y segmentación Unigram las realiza **Google SentencePiece
0.2.1**, expuesto a Java mediante JNI. Implementarlas de cero en Java introduciría
riesgos innecesarios de divergencia. El adaptador implementa los IDs especiales de
XLM-R, la conversión fairseq y el truncado conservando BOS/EOS. La referencia es
`transformers==4.57.6`, `XLMRobertaTokenizer(use_fast=False)`, con el archivo original.
No se afirma identidad con el tokenizador *Fast* en todos los extremos Unicode.

Esto mejora la recuperación de candidatos para la memoria de Salve. No demuestra
AGI, comprensión infalible de negaciones ni identidad correcta de cada persona.
La búsqueda híbrida existente conserva los textos, procedencia y advertencias de
coincidencia semántica; un coseno alto no convierte un recuerdo en un hecho probado.

## Artefactos descargados y versiones

Modelo oficial: [intfloat/multilingual-e5-small](https://huggingface.co/intfloat/multilingual-e5-small).
Revisión inmutable: `614241f622f53c4eeff9890bdc4f31cfecc418b3`.

| Archivo remoto | Bytes | SHA-256 |
|---|---:|---|
| `onnx/model_qint8_avx512_vnni.onnx` | 118.346.824 | `dd476dd0c2514e9b9be83aeb3853fac0763e0bdf4a71645407587d77c48a2d88` |
| `sentencepiece.bpe.model` | 5.069.051 | `cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865` |

Total: **123.415.875 bytes (123,42 MB / 117,70 MiB)**. Es el tamaño de los dos archivos,
no una estimación de un ZIP ni de su consumo de RAM. El nombre `avx512_vnni` identifica
la exportación cuantizada; el contenido es un grafo ONNX con operadores portables,
no un ejecutable AVX. Su ejecución real en ARM64 sigue siendo una prueba de dispositivo.

SentencePiece se fija al commit `31646a467d2051eb904e0b45de3a73e91fe1c1e3` del
[repositorio oficial](https://github.com/google/sentencepiece/tree/v0.2.1), mediante
submódulo. No se descargan fuentes durante la ejecución de la app. Solo el adaptador
nativo y su licencia se incluyen en el APK; el modelo y vocabulario se descargan aparte.
ONNX Runtime Android y el verificador de escritorio usan la versión `1.24.2` de
[Microsoft](https://github.com/microsoft/onnxruntime).

## Comparación realizada

Los archivos `docs/evidence/embeddings/*-host.json` conservan los resultados y los
errores de una evaluación sintética de 32 pares consulta/recuerdo, congelados antes
de ejecutarla. Hay paráfrasis, erratas, personas, fechas, negaciones, idiomas y
contradicciones. Cada consulta busca entre los 32 recuerdos.

| Candidato descargado y ejecutado | Modelo ONNX | Primero correcto | Correcto entre tres |
|---|---:|---:|---:|
| E5 small INT8 oficial, elegido | 118,35 MB | 26/32 | 29/32 |
| E5 small FP32 oficial | 470,27 MB | 26/32 | 30/32 |
| E5 small INT8, exportación Xenova | 118,31 MB | 26/32 | 30/32 |
| paraphrase-multilingual-MiniLM-L12-v2 INT8 ARM64 | 118,41 MB | 25/32 | 31/32 |

En una segunda máquina (GitHub Actions Linux x86_64), E5 oficial obtuvo **26/32 y
27/32**, tanto en Python como en el índice Java persistido. Su informe también se
conserva (`e5-github-x86.json`). La coincidencia Java/Python se mantuvo en `2,98e-8`.
La variación observada refuerza que las cifras host no predicen exactamente las del
móvil. La prueba automática compara el índice con la referencia en la misma máquina:
coseno con error máximo `0,003` por la cuantización del índice y pérdida de cobertura
de como máximo un caso, manteniendo al menos 25/32 primeras posiciones en la referencia.
El primer intento de CI falló porque exigía 28/32 entre tres en cualquier CPU;
no se ha ocultado ese resultado ni cambiado el corpus para hacerlo pasar.

Referencias: [MiniLM](https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2/tree/e8f8c211226b894fcb81acc59f3b34ba3efd5f42),
[Xenova E5](https://huggingface.co/Xenova/multilingual-e5-small/tree/761b726dd34fb83930e26aab4e9ac3899aa1fa78).
También se revisó [EmbeddingGemma 300M](https://huggingface.co/google/embeddinggemma-300m);
no se ejecutó ni se incluye en esta comparación: su repositorio consultado exigía
aceptar acceso y no aportaba el mismo camino ONNX listo para este proyecto.

E5 se elige por su orientación a recuperación multilingüe y el artefacto oficial ya
compatible con ONNX. La diferencia de una consulta frente a MiniLM no demuestra que
sea universalmente mejor; MiniLM recuperó más respuestas entre las tres primeras.
El comparador léxico simple obtuvo 12/32, pero es Jaccard de palabras, **no** la
búsqueda Room/FTS completa de Salve. No se presenta como una mejora medida frente a
la aplicación anterior de extremo a extremo.

La [FAQ oficial de E5](https://huggingface.co/intfloat/multilingual-e5-small#faq)
explica que importa el orden relativo de las puntuaciones. El umbral absoluto de
la calibración antigua (`0,84497` con E5) retenía solo 15/32 fuentes correctas de
este conjunto. La calibración sigue comprobando que el motor distingue español,
pero ese umbral deja de descartar candidatos. Se recupera un número acotado por
orden de similitud y se fusiona con la búsqueda léxica; el contexto exige comprobar
persona, fecha y negación y admitir que ningún candidato responde a la pregunta.
Este cambio favorece cobertura y puede incluir candidatos irrelevantes: no se
interpreta como una probabilidad calibrada de verdad.

Los seis errores de E5 INT8 permanecen publicados. Confunde, entre otros, a Lucía
amiga con Lucía hermana y no encuentra el paraguas desde una pregunta en inglés.
FP32 también falla varios de estos casos: aumentar RAM o quitar la cuantización no
resuelve por sí solo el problema. Hará falta un conjunto mayor de recuerdos reales
anonimizados y evaluación de la respuesta final antes de ajustar umbrales o entrenar.

## Descarga, migración y uso

- `IA y cámara → Memoria en español` permite preparar/reanudar o pausar la descarga.
  WorkManager la solicita una vez automáticamente, con red sin coste (normalmente
  Wi-Fi) y almacenamiento disponible. Cancelarla conserva los parciales y requiere
  reanudación explícita. El progreso se muestra en la notificación.
- Cada archivo se verifica por tamaño y SHA-256, admite reanudación y se publica de
  forma atómica. La pareja solo queda lista cuando ambos están verificados. Se
  vuelven a comprobar los hashes antes de la primera carga de cada proceso.
- Los pesos están en `noBackupFilesDir`: no inflan el APK ni la copia Android. Si se
  restaura una instalación sin los pesos, se pueden preparar otra vez desde el menú.
- El índice derivado incluye la identidad del modelo, tokenizador, prefijos y
  pooling en su nombre. Los antiguos vectores GTE no se reutilizan aunque tengan
  la misma dimensión. Los recuerdos originales de Room permanecen intactos.
- La reconstrucción es progresiva: 32 recuerdos tras preparar y en cada lote de
  indexación de conversación, hasta 400 en cada ciclo de sueño. No se promete que
  todo un archivo grande esté indexado al terminar la descarga.
- La calibración de recuperación usa query/passage; la señal de sorpresa se calibra
  aparte con query/query. Si el modelo no pasa la prueba, faltan archivos o no hay
  RAM, sigue disponible la búsqueda léxica. La descarga nunca se inicia desde ONNX.

## Presupuesto de memoria y prueba en el S24 Ultra

Una sola sesión compartida, carga diferida fuera de la UI, secuencias de longitud
real (sin ejecutar 256 posiciones de relleno), dos hilos CPU y sin arena CPU ni
patrones de memoria retenidos. La sesión se cierra tras 90 segundos sin actividad
y ante avisos de memoria de Android.

Antes de abrirla se exigen 384 MiB de margen más `max(256 MiB, threshold de Android)`;
con la sesión residente se preserva esa reserva. `lowMemory` o una medición desconocida
impiden ejecutar. Es una política de admisión, **no una medición de consumo del S24**.
La captura aportada (2,66 GiB disponibles) supera este margen para embeddings por
separado; no prueba que Gemma y todos los servicios simultáneos vayan a caber.

La primera prueba host INT8 E5 mostró pico de proceso de 351.952 KiB frente a
257.740 KiB antes de abrir ONNX. Incluye Python, bibliotecas y tokenizador; la resta
de dos máximos tampoco es RSS exacto del modelo. No se traslada como medición Android.

En el teléfono, validar descarga, pausa/reanudación, modo avión, presión de memoria,
recuperación de recuerdos antiguos y coexistencia con Gemma. Registrar `adb shell
dumpsys meminfo com.salve.app` antes/después de recuperar y después de 90 s de reposo.
La prueba optativa `MindContinuityRealTest#embeddingModelReportsSpanishCalibration`
requiere preparar primero los archivos y activar `runRealMind=true`. La CI compila
su APK instrumentado, pero no sustituye ejecutar esa prueba en el S24.

## Verificación reproducible

```sh
git submodule update --init third_party/sentencepiece
python3 -m venv build/e5-venv
build/e5-venv/bin/pip install -r scripts/embeddings/requirements.txt
PYTHON="$PWD/build/e5-venv/bin/python" bash scripts/embeddings/verify.sh
```

Requiere Linux, JDK 17 o superior completo, CMake 3.22 o superior, compilador C++ y
curl. Descarga los artefactos fijados a `build/embedding-validation`, fuera de git.
Verifica los 248 casos de IDs con Hugging Face y después con las clases Java/JNI de
producción: Unicode compuesto/descompuesto, espacios especiales, emoji, otros
alfabetos, tokens especiales y truncado. Comprueba además BOS/EOS con presupuesto 2.
Genera 64 vectores con Python y compara el motor Java real, incluidos prefijos,
atención y pooling. Error absoluto máximo observado: `2.9802322387695312e-8`;
tolerancia automática `2e-4` para diferencias de backend.
El verificador también guarda y vuelve a abrir el índice INT8 real de Salve:
mantiene 26/32 en primera posición y 29/32 entre tres candidatos. Las dos
calibraciones independientes (recuperación y similitud) pasan 8/8.

El workflow `Multilingual memory contract` repite esas comprobaciones en Linux
x86_64 y ARM64 nativos; el resultado ARM64 de servidor tampoco mide el teléfono. La CI Android
ejecuta los tests de contrato (integridad, máscara/pooling, RAM y prefijos), construye
ARM64, verifica que no haya pesos en assets y comprueba alineación nativa de 16 KiB.
El resultado de cada ejecución de CI, no este documento, indica si la rama compila.
