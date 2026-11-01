# Salve — habilidad nueva de diferencias consecutivas sin duplicar el PR #307

## Contexto y regla de ingeniería

**Buscar → inspeccionar → reutilizar → conectar → desarrollar solo lo faltante → probar.**

Al terminar la CI del PR #308, el repositorio había cambiado: el PR **#307 se fusionó** incorporando `sum`, `count` y `prefix_sum`, dos nuevas propuestas de agregación y evaluación con los mismos componentes. El PR #309 ya se fusionó después de crear esta rama y añadió `rotate_left` sin repetir `prefix_sum`. Reaplicar el PR #308 duplicaría `running_total` frente a `prefix_sum` y perdería cambios verificados. Por ello este PR se construye **directamente sobre el main que ya incluye #307**.

El nuevo alcance está estrictamente limitado a una operación no presente:
**`adjacent_delta`**, diferencias de elementos consecutivos.

## Cambios mínimos a componentes existentes

1. `InductiveSkillLab`: añade una sola primitiva pura, **`adjacent_delta`**, al final de la gramática ya existente. Ejemplo: `[5,7,4,11]` → `[2,-3,7]`. Con cero o un elemento devuelve `[]`. Con la rotación fusionada en el PR #309, el catálogo conjunto contiene 15 operaciones; combinaciones limitadas a `15 + 15² + 15³ = 3.615`. La constante se calcula desde el propio catálogo para evitar contadores obsoletos.
2. `InductiveSkillProposal`: acepta exclusivamente peticiones originales reconocidas **«diferencias consecutivas»** / «calcular diferencias consecutivas», con el nombre de habilidad `auto_adjacent_delta`. El **oráculo independiente** de referencia no llama al intérprete. Reutiliza exactamente `request`, `preview`, seis casos de entrenamiento, seis de holdout, comparación de 96 casos posteriores a la congelación, `promote` y el mismo catálogo persistente.
3. `AutonomousToolRuntime`: amplía la ayuda y la **lista de tipos de recordatorio ya existente**, sin permitir nuevas herramientas, permisos, shell ni accesos externos. Se conserva aprobación con UUID `aprueba habilidad <ID>`.
4. `AgentSkillGapDetector`: ya itera sobre `InductiveSkillProposal.Kind`, por lo que no requiere modificación funcional. Prueba de regresión asegura sugerencia únicamente con **dos planes reales** y recibos fallidos; nada de texto no confiable.

No se añade ejecutor, clase de LLM, nueva memoria, cola, motor de propuestas, formato de sincronización, canal pCloud o sistema de aprobación.

## Seguridad, compatibilidad y pCloud

- Entrada lista de hasta 24 enteros entre -10.000 y 10.000.
- Cada delta usa aritmética intermedia `long` y **rechaza** cualquier resultado con magnitud superior a 10.000. No hay truncamiento, desbordamiento ni efectos secundarios.
- Programa de máximo tres operaciones, lista blanca cerrada; `python.exec`, red, shell, Java/Kotlin y enlaces pCloud no son operaciones aceptadas.
- Se mantiene el **mismo esquema** del archivo `inductive-skills.json`; versiones anteriores siguen legibles.
- `SkillCloudArchive` del PR #305 valida con el **mismo intérprete** y conserva versiones aprobadas bajo consentimiento. Su fase de cuarentena y aprobación explícita es obligatoria. **No** ejecuta código descargado.
- El síntetizador selecciona con entrenamiento; un fallo en el examen reservado no reinicia la búsqueda ni aprende de sus respuestas. El examen independiente de 96 casos **no prueba generalización universal**.

## Verificaciones

- `InductiveAdjacentDeltaTest`: 96 nuevas secuencias con oracle independiente, aprendizaje y reinicio local, carga segura pCloud, aprobación explícita, rechazo de candidato alterado, desbordamiento, lista vacía, holdout falso, cancelación y rechazo de código arbitrario.
- `AutonomousToolRuntimeTest`: comando original de usuario, propuesta no instalada, autorización exacta y uso tras reinicio.
- `InductiveSkillProposalTest`: todas las familias previas del #307 conservan sus casos y presupuestos; la nueva usa candidato de una operación.
- `AgentSkillGapDetectorTest`: dos objetivos reales fallidos son necesarios; el texto externo no concede capacidad.
- GitHub Actions: Windows, memoria multilingüe, Android debug/preview y sandbox; **no sustituyen** prueba física de pCloud y memoria en Galaxy S24 Ultra.

**No se acredita AGI, entrenamiento de pesos Gemma ni generación autónoma de aplicaciones.**


## Resolución del conflicto con PR #309

Se integró `main` conservando `rotate_left` del PR #309 y
`adjacent_delta` de este PR. Ambos reutilizan `InductiveSkillLab`,
`InductiveSkillProposal` y `AutonomousToolRuntime`, sin motores paralelos.
Los tests de ambas habilidades esperan ahora 3.615 candidatos, y el
recordatorio de sugerencias admite ambos tipos de propuesta.
