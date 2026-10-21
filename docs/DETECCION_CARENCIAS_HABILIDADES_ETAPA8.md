# Salve — etapa 8: detectar carencias observadas y sugerir habilidades

## Objetivo y alcance

Hasta la etapa 7, Salve necesitaba un mensaje como
`propón habilidad: ordenar impares sin repetir` para investigar esa capacidad.
Ahora, **después de terminar un plan real**, el controlador puede reconocer una
carencia limitada y preparar una propuesta **sin instalarla**.

El diseño prioriza evidencia de ejecución sobre afirmaciones del modelo.
No deduce capacidades generales a partir de diálogos, textos recuperados o
autoevaluaciones. Por ahora solo enlaza dos familias del laboratorio local,
ya definidas por una función-oráculo verificable: ordenar pares o impares
sin repetir. No es un detector general de habilidades ni una AGI.

## Activación

`AgentRuntime.run` consulta el historial reciente de Room después de guardar
el resultado terminal de un plan, siempre que la autonomía siga activa y no
exista una conversación prioritaria. Invoca
`AutonomousToolRuntime.observeFailedPlanHistory`, sin crear otro agente.

`AgentSkillGapDetector.detect` exige:

- Dos **IDs de planes distintos** realizados durante los últimos siete días.
- Objetivos **originales del usuario** que correspondan de forma exacta a una
  de las dos transformaciones admitidas. Se reconocen variantes controladas
  como `ordena los números pares sin repetir`.
- Estados terminales `BLOCKED` o `PARTIAL`, nunca `SUCCEEDED`.
- Al menos un **recibo real de herramienta** `ERROR` o `BLOCKED` por plan.
- Ningún éxito `VERIFIED` y ningún permiso de ejecución de código remoto.
- Se ignora el texto del resultado, las afirmaciones del LLM y cualquier
  documento externo: **no son autorizaciones ni evidencia del fallo**.

Un fallo aislado, una respuesta revisada favorablemente o una tarea
semánticamente distinta no generan propuestas.

## Preparación automática sin promoción

Una vez detectada la carencia, y únicamente en segundo plano dentro del
trabajador del agente:

1. Comprueba que la habilidad correspondiente **no esté ya registrada**.
2. Comprueba que no haya otra propuesta pendiente válida.
3. Respeta un **periodo de siete días por familia** después de una propuesta,
   incluso si el usuario la descarta.
4. Ejecuta la evaluación de la etapa 7: síntesis por entrenamiento, examen
   reservado y **96 casos nuevos** contra un oráculo independiente y una
   línea base simple. Ningún caso del benchmark cambia el candidato.
5. Solo si supera los umbrales, escribe una propuesta pendiente con UUID,
   familia, programa congelado y caducidad de 24 horas. No guarda datos del
   usuario, ejemplos ni texto de modelos.
6. Al confirmar la persistencia de esa propuesta, registra un pequeño
   marcador privado de supresión por siete días, sin objetivos ni recuerdos.

Esta ejecución puede interrumpirse por pausa/cancelación. Si falla el
almacenamiento de la propuesta, no comienza un periodo de silencio.

## Experiencia de uso

Al consultar `estado del agente` o `mis planes`, Salve muestra la propuesta
pendiente como una **sugerencia, NO una habilidad instalada**. También sirve:

- `propuestas de habilidades`: muestra el ID.
- `aprueba habilidad <UUID>`: vuelve a verificar la candidata y la promueve
  usando el contrato de la etapa 7; requiere el comando exacto en el canal
  directo del usuario.
- `descarta habilidad <UUID>`: retira la propuesta sin instalar nada.

El observador automático **no** puede llamar a estos comandos ni conceder
permisos Android, nube, shell, WhatsApp o autoprogramación. No envía
notificaciones y no ejecuta nuevas tareas personales para buscar carencias.

## Tests de regresión

- `AgentSkillGapDetectorTest`: dos fallos reales, IDs distintos, objetivos
  con variantes exactas, planes antiguos/futuros, permisos de código,
  respuestas del modelo inventadas, una sola lectura fallida, coincidencias
  falsas en recibos y casos fuera de catálogo.
- `AutonomousToolRuntimeTest`: planificación simulada pero persistencia real
  de archivos, sugerencia sin instalación, aprobación directa con ID correcto,
  reinicio, habilidad ya registrada, cooldown después de descarte, pausa,
  cancelación y ausencia de archivos con fallos insuficientes.
- El flujo de propuesta de la etapa 7 y todos los verificadores anteriores
  siguen vigentes. El nuevo detector no cambia el diario ni el motor LLM.

## Siguiente experimento

Para salir del catálogo de necesidades predefinidas se necesita un mecanismo
que **derive objetivos de aprendizaje de postcondiciones explícitas del usuario**,
con etiquetas verificadas independientemente. Un LLM puede proponer hipótesis,
pero nunca inventar las etiquetas, ampliar permisos o aprobar la promoción.
Medir los falsos positivos en Android real antes de habilitar sugerencias
más frecuentes.
