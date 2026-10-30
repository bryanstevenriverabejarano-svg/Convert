# Salve — extensión segura de habilidades simbólicas y comprobación de transferencia (PR posterior al #305)

## Regla permanente: buscar → inspeccionar → reutilizar → conectar → desarrollar solo lo que falta → probar

Inspeccionados y reutilizados: `InductiveSkillLab`, `InductiveSkillProposal`, `BlindSkillTransferExperiment`, `AgentSkillGapDetector`, `AutonomousToolRuntime`, `SkillCloudArchive`, `CloudSyncManager` y las pruebas existentes. Ya había un intérprete limitado a listas de enteros con síntesis de hasta tres operaciones, 6 ejemplos train/holdout separados, propuestas con **96 casos independientes** y aprobación explícita. El PR #305 ya archivaba catálogos en pCloud con validación y cuarentena.

**No se crea un segundo motor, modelo, sistema de memoria, nube, sandbox, planificador, lenguaje de programación arbitrario ni ejecutor remoto.**

## Cambio mínimo en el intérprete original

Se agregan **tres primitivas puras, auditables y deterministas** a las diez existentes:

- `sum`: reduce hasta 24 enteros a una lista de un elemento, su suma; lista vacía → `[0]`.
- `count`: devuelve el número de elementos actuales como lista `[n]`, incluso sobre listas filtradas, con duplicados o vacías.
- `prefix_sum`: devuelve los acumulados sucesivos, conservando su orden; lista vacía → `[]`.

Con las primitivas anteriores pueden aprenderse composiciones genuinas: `abs → sum`, `filter_even → unique → count`, `filter_odd → prefix_sum`. El tamaño de entrada sigue limitado a 24 elementos, cada valor/suma parcial a ±10 000, y cada programa a **3 pasos**. Los desbordamientos y operadores extraños **se rechazan**; no se abren red, archivos, shell, reflexión, Android u otras acciones.

La búsqueda de candidatas es exhaustiva y finita en el catálogo auditado: pasa de **1 110 a 2 379** combinaciones (13 + 13² + 13³). Los operadores anteriores conservan su orden de prioridad. Aumentar el número de candidatos puede afectar CPU y latencia: se mantienen los controles de cancelación existentes y se debe medir en teléfono.

## Propuestas y verificación independiente reutilizadas

`InductiveSkillProposal` amplía las dos necesidades previamente reconocidas con dos **peticiones exactas del dueño**:

- `propón habilidad: sumar valores absolutos` → `auto_abs_sum` (`abs → sum`).
- `propón habilidad: contar pares distintos` → `auto_distinct_even_count` (`filter_even/unique/count`, orden apropiado).

La generación de ejemplos y las etiquetas esperadas siguen bajo **oráculos independientes del intérprete**. El modelo no puede decidir por sí mismo que el examen es correcto. El proceso existente exige: 3+ demostraciones, tres o más holdouts reservados, un programa congelado y **96 nuevas entradas deterministas** no usadas durante la síntesis; mejora de al menos 10 resultados respecto a ordenar simplemente los valores; **aprobación explícita con ID de propuesta** y revalidación antes de escribir. La selección `AgentSkillGapDetector` sigue exigiendo **dos planes reales diferentes**, recientes, fallidos y de objetivos originales exactamente reconocidos; no interpreta textos de páginas ni opiniones del LLM como permiso para ampliar la aplicación.

El catálogo persiste en el **mismo** `autonomy/inductive-skills.json` (esquema JSON 1 y límites existentes), de modo que `SkillCloudArchive` del PR #305 admite las operaciones nuevas en sus mismos decoders y almacén pCloud, pero **no instala ni ejecuta** lo descargado sin verificación del dueño y pausa. Los catálogos antiguos siguen siendo válidos.

## Pruebas

- `InductiveSkillAggregationTest`: sumas, conteos, acumulados, vacíos, composición de tres pasos, saturación/overflow, entradas mayores de 24, operadores shell/http rechazados; aprendizaje real de `prefix_sum` con train/holdout separados, 96 entradas inéditas, persistencia y relectura; compatibilidad con `SkillCloudArchive` sin ejecutar código externo.
- `InductiveSkillProposalTest`: las 4 familias ahora utilizan el mismo benchmark de 96 entradas, que se ejecuta con un oráculo ajeno a la gramática del intérprete; no se instala nada durante `preview`; rechazo de cambios en la candidata congelada; promoción explícita y comprobación tras reinicio.
- `AgentSkillGapDetectorTest`: evidencia de dos planes independientes, no desde solicitudes arbitrarias de acceso a datos ni permisos de código.
- `InductiveThreeStageGeneralizationTest`: mismo programa de tres pasos previo, ahora con presupuesto finito actualizado y 96 ejemplos inéditos.
- La CI existente debe pasar Android debug/preview, Windows, memoria multilingüe y aislamiento de herramientas; CI **no** demuestra menor consumo ni autonomía general en Galaxy S24 Ultra.

## Límites e hipótesis

**Aprender habilidades aquí significa síntesis simbólica finita y evaluación sobre datos nuevos**, no reentrenamiento de Gemma, creación libre de APK, escritura ejecutable en pCloud ni AGI. Los ejemplos independientes son generados por código para pruebas; no validan datos privados ni el control del dispositivo. Antes de ampliar a otros dominios (archivos, imágenes o programas más complejos) se requerirán contratos independientes, evaluación real y aprobación del dueño. No se afirma que la sincronización remota se haya ejecutado en una cuenta pCloud física.
