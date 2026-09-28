# Salve: aprendizaje experimental contrastado con investigación

Fecha de revisión: 2026-09-28. Base examinada: `a3a5640cb1e1b9454456ff301cf8a1b6306fef9b`, después del PR #93.

## Conclusión de diseño

No hay una disyuntiva entre programación fiable y arquitectura experimental. El aprendizaje necesita un mecanismo estable para medir resultados, conservar procedencia, cancelar, comparar y revertir. Este cambio añade **selección de estrategias a partir de experiencia verificada** al laboratorio que Salve ya ejecuta, y corrige un sesgo concreto en la recuperación de recuerdos. No sustituye el modelo fundacional, no entrena sus pesos, no reproduce todos los sistemas citados y no acredita superinteligencia o conciencia.

La comparación bibliográfica se basa en las fichas y resúmenes originales de los autores, contrastados con los puntos de integración del repositorio. No es una reproducción experimental de sus artículos. Sus resultados publicados pertenecen a sus modelos, presupuestos y conjuntos de evaluación, no a Salve.

## Qué existía y dónde estaba la diferencia

`AutonomousToolLab` ya generaba programas declarativos de un catálogo acotado, ejecutaba verificadores, conservaba tres regresiones por familia y permitía revertir versiones. La selección empezaba por la última estrategia guardada y después seguía el catálogo; no comparaba alternativas por coste en experiencias emparejadas.

`ConversationMemoryGrounding` ya conservaba referencias, perfiles, fechas y un grafo de un salto. Sin embargo, detenía la búsqueda en cuanto encontraba cuatro recuerdos: la primera palabra podía impedir consultar las siguientes. El PR #93 ya incorporó fotos persistentes, identidades declaradas y pCloud. Este cambio conserva esas funciones; no las presenta como nuevas.

`AutoImprovementManager` ya prepara propuestas de parches y el ejecutor externo las prueba en Docker sin red. Su diagnóstico principal inspecciona firmas de métodos; eso no equivale a la búsqueda de mejoras generales evaluadas en AIDE². El bucle de `BucleCognitivoAutonomo` emplea umbrales y textos prefijados: sus nombres y estados no demuestran una conciencia ni un currículo aprendido.

## Contraste de los trabajos

| Trabajo original | Idea que importa | Aplicación en este PR / diferencia restante |
|---|---|---|
| [AIDE²](https://arxiv.org/abs/2609.26457) | Modificar una estrategia y conservarla por evaluaciones, no por su propia descripción. | Se comparan estrategias ejecutables con resultados verificados. No se implementa la reescritura recursiva de su agente investigador ni sus evaluaciones ocultas. El runner de parches existente permanece separado. |
| [EnSIMem](https://arxiv.org/abs/2609.27279) | Recuperar evidencia de entidades/propiedades conservando turnos y tiempo. | Se elimina el sesgo de la primera consulta y se mantienen IDs/fechas originales. El clasificador lexical NO es su índice entidad-propiedad ni resuelve identidades. |
| [ACLArena](https://arxiv.org/abs/2609.23989) | Aprendizaje continuo, replay y expertos LoRA enrutados. | Se conserva replay verificado y se separan contextos de selección. No hay entrenamiento RL, LoRA ni cambios de pesos. Es una analogía de evaluación, no una implementación de ACLArena. |
| [SEMV](https://arxiv.org/abs/2609.27175) | Consolidar experiencia sólo después de verificar; preservar posibilidad de revisión. | Las observaciones y el programa se guardan en una única transacción; los fallos conservados bloquean preferir un candidato. No se añade el sistema argumentativo multimodal ni una gestión completa de contradicciones autobiográficas. |
| [PaMER](https://arxiv.org/abs/2609.27286) | Decidir qué memoria se necesita antes de actuar. | Recuperación acotada por la consulta completa como base contrastable. No se leen estados ocultos del LLM ni se implementa el predictor de compresión/recuerdo de PaMER. |
| [Self-Organizing Agent Teams](https://arxiv.org/abs/2609.22682) | Aprender organización y colaboración, no sólo añadir agentes. | Pendiente: evaluar estructuras de equipo con presupuesto igualado. Reordenar algoritmos en un catálogo NO equivale a aprendizaje de colaboración entre LLMs. |
| [AEWM](https://arxiv.org/abs/2609.28416) | Revisar estados de tarea contaminados por supuestos o planes obsoletos. | El laboratorio mantiene resultado, verificación y persistencia separados. No se reemplaza el estado conversacional ni se entrena Action Judge/State Revision. |
| [AutoViewMem](https://arxiv.org/abs/2609.21940) | Evitar interferencia entre recuerdos mediante vistas estructuradas. | Se amplía la oportunidad de recuperar candidatos antes de seleccionar cuatro. No se descubren vistas automáticamente ni se cambian esquemas de escritura. |
| [VibeMemBench](https://arxiv.org/abs/2609.23570) | Medir utilidad ejecutable de memoria frente a un control sin ella. | Nueva ablación aprendizaje activado/desactivado, mismo flujo de problemas; incluye coste de exploración. La suite existente conserva su comparación con/sin diario. No equivale a tareas reales de programación del benchmark. |
| [Emergent Collusion](https://arxiv.org/abs/2609.24967) | Historial y recompensas pueden inducir validación incorrecta entre agentes. | El selector no puede editar ni sustituir el verificador. No se afirma que esto elimine todos los riesgos de coordinación; no se reproduce el experimento multiagente. |
| [RSIAgent](https://arxiv.org/abs/2609.15364) | Explorar, actuar, verificar y reutilizar experiencia sin cambiar pesos. | Una sonda contrafactual acotada compara otra estrategia con el mismo problema y replay. No explora aplicaciones ni descubre herramientas arbitrarias o relaciones causales generales. |
| [SelfMem](https://arxiv.org/abs/2607.03726) | Optimizar estrategias de memoria con feedback. | Se aprende una política finita de selección de herramientas. La política de memoria conversacional sigue siendo una base lexical fija, no SelfMem. |
| [ALMA](https://arxiv.org/abs/2602.07755) | Buscar diseños de memoria expresados como código. | Pendiente: candidatos de arquitectura en el runner aislado y una evaluación externa congelada. No se permite que una observación reescriba el esquema o los evaluadores. |
| [When Continual Learning Moves to Memory](https://arxiv.org/abs/2604.27003) | La interferencia reaparece en recuperación; más memoria no garantiza menos olvido. | Diario acotado, perfiles de contexto y bloqueo ante regresiones observadas. La ventana finita puede olvidar evidencia antigua; no garantiza ausencia de olvido catastrófico. |
| [CTM-AI](https://arxiv.org/abs/2605.04097) | Selección/integración de información entre procesadores como arquitectura cognitiva. | Orientación para un futuro espacio compartido evaluable. Este PR no implementa CTM-AI ni usa autodescripciones como evidencia de conciencia. |

## Circuito implementado

`reto → estrategia → ejecución real → verificador exacto + replay → sonda alternativa → comparación emparejada → memoria de resultados → preferencia futura → nueva verificación`

### Selección adaptativa de herramientas

`VerifiedStrategyPolicy`, integrada por `ToolLearningSupport` en `AutonomousToolLab`, conserva hasta 96 observaciones. Guarda contexto, huellas SHA-256 del problema y del par problema/replay, nombre de estrategia, éxito y operaciones instrumentadas. No guarda textos del chat, fotos ni credenciales. Las huellas identifican entradas; no son anonimización criptográfica de datos fáciles de adivinar.

Para adelantar una alternativa en el catálogo exige al menos tres problemas distintos comparables, el mismo snapshot de replay en cada pareja, cero fallos conservados de esa alternativa en el contexto, ninguna regresión de coste por problema y al menos 10% menos operaciones agregadas. Son umbrales de ingeniería, no significación estadística ni garantía de generalización. El coste contado incluye los verificadores; no mide toda la CPU, energía, red o coste del modelo.

Una ejecución puede probar como máximo una alternativa adicional, con presupuesto de 100.000 operaciones. Se priorizan alternativas menos observadas, no se repite una pareja estrategia/problema mientras siga registrada y se deja de explorar una estrategia tras tres fallos conservados en ese contexto. Las sondas no cambian el resultado ya obtenido ni se promocionan inmediatamente. Cualquier estrategia elegida en una consulta posterior vuelve a pasar el verificador y las regresiones.

Los registros nuevos se confirman junto al programa; fallo de disco o cancelación no confirma aprendizaje. Revertir una herramienta elimina sus observaciones de selección para no reactivar silenciosamente la preferencia revertida. El registro permanece local en el archivo privado del laboratorio; **no se incorpora una sincronización pCloud de esta política**. El resto de la memoria y su sincronización no se modifican.

El modo experimental está activado por defecto en diarios que aún no tienen la opción. Puede consumir más operaciones sin conseguir una mejora; el informe de ablación debe mostrar ese resultado cuando ocurra. Desactivarlo detiene tanto el reordenamiento como las sondas, sin borrar la evidencia previa. Conserva las herramientas ya aprendidas: por eso un experimento causal debe partir de dos estados equivalentes, no alternar la opción en una sola sesión ya entrenada.

Comandos escritos exactos en Salve:

```
estado aprendizaje experimental
desactiva aprendizaje experimental
activa aprendizaje experimental
```

Se mantienen pausa/reanudación y reversión del laboratorio. No se añaden permisos del móvil, ejecución de shell, descarga de código, acceso a cuentas ni acciones externas.

### Recuperación de memoria

Se consultan hasta cuatro términos y ocho candidatos por término, máximo 32 registros únicos. Se ordenan por cobertura de términos completos de la consulta, con normalización de acentos y mayúsculas; la fecha sólo desempata. Se entregan como máximo cuatro recuerdos, conservando el presupuesto de contexto, procedencia y separación entre datos e instrucciones. Es una heurística lexical evaluable, no recuperación semántica aprendida. Dos registros contradictorios no se fusionan en una supuesta verdad; la selección tampoco garantiza recuperar todas las contradicciones existentes.

## Validación y reproducibilidad

Ejecutado localmente con JDK: `bash scripts/test-research-kernel.sh` → **638 aserciones aprobadas**. Incluye 200 escenarios generados con tres invariantes cada uno; no son 638 tareas independientes ni evaluaciones de un LLM.

La suite JUnit añade pruebas del laboratorio real: coste de sondas, persistencia, desactivación, cancelación durante la exploración, error de almacenamiento, compatibilidad del diario, huellas canónicas y ablación de 24 problemas de rutas. Añade pruebas de recuperación a través del DAO y de los comandos del runtime. La salida `SALVE_LEARNING_ABLATION` informa coste base, coste experimental total, coste exploratorio y reordenamientos **sin exigir ni fingir que gane la variante nueva**. No usa respuestas precalculadas como feedback del selector.

La compilación Android y la suite completa corresponden al workflow del PR. Este documento no da por aprobada una ejecución que todavía no haya terminado. La prueba local no compiló Android ni utilizó Gson/JUnit; la integración completa requiere CI. No se ejecutaron en el teléfono inferencia real, cámara, restauración pCloud ni medidas de batería. Los 104 desafíos existentes son problemas acotados, no pruebas de superinteligencia.

## Siguiente frontera experimental, no implementada aquí

1. Congelar conjuntos de tareas nuevos de conversación, memoria temporal, programación y uso de herramientas; medir éxito, errores de memoria, coste completo y retención frente a versiones sin cada componente.
2. Probar diseños de memoria y estructuras de agentes como candidatos dentro del runner aislado; mantener evaluadores independientes y datos de prueba reservados. No cambiar a la vez modelo, prompt, memoria y presupuesto sin ablaciones.
3. Sólo después de obtener evidencia de transferencia, explorar LoRA/replay, workspace cognitivo y revisión aprendida del estado. Usar versiones y reversión. No convertir resultados locales en una afirmación de inteligencia general o experiencia subjetiva.
