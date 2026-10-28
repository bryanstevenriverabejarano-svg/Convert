# Salve — Un único ciclo conversacional para interpretar, decidir y verificar

## Auditoría del flujo real de `main` (28-10-2026)

El motor ya tenía piezas funcionales. No construimos un LLM de reemplazo ni afirmamos AGI:

```text
Turno (voz/texto) → prioridad: parar/cancelar/permisos y órdenes explícitas
 → MotorConversacional (intención, historial, reglas de privacidad)
 → recuperación de memoria con evidencia y contexto autorizado
 → CognitiveToolCatalog (herramientas realmente disponibles + permisos)
 → CognitivePrompt (HISTORY, MEMORY_CONTEXT, TASK_STATE, TOOL_CATALOG)
 → Gemma 4 / modelo disponible: respuesta o acción JSON
 → CognitiveTurn (máximo 2 herramientas, 4 inferencias, sin repetir efectos)
 → CognitiveToolExecutor (autoriza y ejecuta; devuelve recibo)
 → resultado observado / presentación / seguimiento / SkillLab
```

Tres fallos de integración reales:

1. `SleepImprovementCycle.asksStatus` desviaba preguntas de sueño antes
   de la inferencia y dejaba al modelo fuera de la selección de fuentes.
2. `CognitivePrompt.limitTools` impone **500 caracteres** para el catálogo:
   una función añadida sin prioridad podría quedar fuera de la selección
   aunque exista en Java.
3. `ExecutionJournal` (registro de tareas) y `SleepImprovementRuntime`
   (informe nocturno verificado) eran dos almacenes paralelos, sin una
   herramienta semántica común de solo lectura.

## Cambio: evidencia primero, modelo para elegir el ámbito

- `ConversationEvidenceIntent` reconoce peticiones claras de registro,
  sueño, resultados y seguimientos elípticos según los turnos recientes.
  **Es una pista de pertinencia, no otro modelo**. No considera consultas de
  ubicación/GPS como el registro de ejecución y no intercepta ficción.
- El catálogo ofrece la única herramienta nueva `evidence.inspect` solo
  cuando el usuario pregunta por pruebas locales y está verificado.
  Su parámetro explícito es `scope: sleep | execution | both`.
- `CognitivePrompt.prioritize` conserva esta herramienta dentro de los
  500 caracteres. `CognitiveTurn` permite que el LLM elija la fuente;
  `CognitiveToolExecutor` revalida la identidad y la petición, lee ambos
  almacenes sin modificar nada y devuelve una respuesta textual real.
- Los comandos críticos de parar, confirmar, borrar, identidad y
  manipulación de Android **no** dependen de que el LLM acierte.
  «Abre el Registro de ejecución» puede seguir abriendo directamente la
  pantalla: una navegación explícita no necesita inferencia.
- Si Gemma responde sin llamar a la herramienta relevante, el
  controlador reemplaza **solo esa consulta específica** por la lectura
  local verificada. Esta última defensa impide «he aprendido X» sin
  un registro; la respuesta normal sigue llegando del modelo para otros temas.
- Las pruebas de `ConversationExamRunner` inyectan herramientas simuladas;
  el fallback real **no** se ejecuta en esas pruebas.
- Nada en este cambio supone que `WorkManager` haya ejecutado un sueño;
  el informe puede faltar porque Android requiere carga, tiempo o no
  concedió el turno. La animación por sí sola no equivale a aprendizaje.
  Tampoco modifica los pesos del modelo ni ejecuta PR de código sin orden.

## Comprobación en teléfono

1. Fusionar PR tras CI y actualizar la APK con firma compatible.
2. Consultar «¿Qué aprendiste mientras dormiste?», «¿Y el registro?»,
   «Muéstrame tus notas de ejecución», «¿Puedes mostrarme ambas cosas?»
   dentro de un contexto donde se habló de sueño.
3. Confirmar que la respuesta explica `ESPERANDO_CARGA` o
   `sin informe` cuando corresponde; que nunca inventa un ciclo;
   y que el registro muestra los eventos reales del Worker.
4. Probar con identidad no verificada: sin herramientas privadas ni
   divulgación de información interna.
5. Comparar rapidez y precisión con Gemma real. Esta PR compila y prueba
   contratos JVM/Android, no sustituye la validación física en el S24 Ultra.

**Siguiente iteración** (no incluida): ampliar el catálogo semántico a todas
las funciones existentes con metadatos de permisos, efectos y comprobadores
uniformes; instrumentar cobertura, latencia y conflictos de comandos en
un conjunto amplio de peticiones reales. No convertir respuestas de LLM
en efectos Android de forma implícita.
