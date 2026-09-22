# Salve — Informe de evolución verificable

Fecha: 22 de septiembre de 2026. Requisitos: [instrucción aportada por Bryan](REQUISITOS_EVOLUCION_AUTONOMA.md). Este informe separa implementación, pruebas de contrato y capacidades demostradas en ejecución.

## 1. Arquitectura inicial y punto de continuación

Checkpoint estable anterior: `43f51d4af3f3053ef384b6f0326c9513be50bcb7`, integración del PR 82. Se conservó la referencia local `salve-stable-pr82-43f51d4`. La rama de trabajo es `feature/salve-autonomous-evolution`.

Al recibir el nuevo archivo de requisitos, el trabajo ya había implementado el laboratorio y certificado una primera ejecución de 416 retos. Una compilación Android de ese estado intermedio había pasado 726 pruebas JVM. Estaban en curso la comparación con/sin memoria y una corrección de retención de recibos. No se reinició el proyecto ni se interpretó la renovación del entorno de trabajo como un fallo de Salve.

La arquitectura existente ya tenía conversación y recuperación de memoria, inferencia local, voz, acciones Android con permisos, objetivos persistentes, un lector HTTPS y un runner externo de propuestas. No tenía un navegador DOM, una flota de modelos activos, ni pruebas de infraestructura Namecheap operativa. El [diagnóstico detallado](LABORATORIO_AUTONOMO_VERIFICABLE.md) identifica componentes y límites.

## 2. Arquitectura final de esta fase

La conversación se conecta a un laboratorio con registro de capacidades, intérprete declarativo, kernels reales, verificador independiente, comprobación de regresiones y diario versionado. La memoria procedural retorna metadatos pertinentes al contexto. La investigación pública usa resultados tipados y fuentes identificables. El catálogo de modelos pasa a ser una fuente única con estado de inferencia separado de sus declaraciones. El runner aplica también las suites generadas válidas antes del snapshot de Docker.

## 3. Capacidades añadidas y mapa de lo que falta

| Capacidad | Estado real | Evidencia y límite |
|---|---|---|
| Planificar y adaptar cálculos | Implementado, evaluado | Búsqueda finita entre doce estrategias; no es planificación general |
| Crear herramientas | Implementado, acotado | Genera programas ejecutables `salve-tools/1`; no inventa primitivas nuevas |
| Comprobar resultados | Implementado, evaluado | Testigos y óptimos certificados en cuatro familias |
| Recuperar experiencias | Implementado, evaluado | Diario procedural, reinicio, pertinencia y retención por familia |
| Usar conocimiento personal | Existente, parcial | Continúa la recuperación conversacional; esta suite no evalúa su calidad con LLM real |
| Investigar en Internet | Cliente y coordinación implementados | Tests de contrato pasan; acceso público real no validado por fallo DNS |
| Navegar con DOM, formularios y pestañas | Ausente | No se presenta el lector HTTP como navegador interactivo |
| Computer use | Existente, parcial | Accesibilidad Android; no probado aquí con apps o dispositivos reales |
| Terminal/Python para código propuesto | Infraestructura externa requerida | Runner Docker; sin ejecución de código arbitrario en anfitrión |
| Coordinar agentes de modelos | Parcial | Roles y etapas secuenciales, normalmente sobre el mismo modelo local |
| Seleccionar modelos | Catálogo y selección implementados | Un modelo descargable real; sin benchmark de varios modelos instalados |
| Aprendizaje neuronal continuo | No implementado en este grupo | Conservar una estrategia no modifica los pesos del LLM |
| Jev | Investigado, sin activar | Falta credencial y comparación mediante inferencia real |
| Nube Namecheap | Cliente protegido, infraestructura desconocida | Envíos bloqueados con token de ejemplo; sin backend versionado ni acceso administrativo acreditado |
| Automejora general | Parcial | Parches y tests trazables; aislamiento real y promoción general pendientes |

## 4. Herramientas añadidas

`ToolRegistry` describe `route`, `knapsack`, `schedule` y `dependencies`, sus entradas/salidas, estrategias, permisos, riesgos y límites. El contrato ofrecido al modelo consulta este registro. `catálogo de herramientas` permite consultar las capacidades locales. La promoción de programas sólo ocurre después de validar resultado y regresiones.

El registro todavía no unifica todas las herramientas históricas de Salve. Las operaciones de investigación siguen conectadas mediante los flujos conversacionales existentes; no se anuncian como llamadas JSON disponibles si no tienen ese adaptador.

## 5. Modelos incorporados

**Ningún modelo nuevo descargado ni activado.** Se conserva el modelo ya fijado por SHA/revisión en `assets/models.json`. `ModelCatalog` valida duplicados, formato, tipos y metadatos; descarga una sola entrada elegida. RAM, VRAM, velocidad o costes ausentes permanecen desconocidos.

`SalveLLM` registra capacidades comprobadas durante la sesión por separado para texto y visión. Encontrar un archivo o completar una inferencia de texto no acredita visión. El registro puede consultarse desde la interfaz. Roles de planificación y síntesis no significan que se hayan cargado varios modelos independientes.

## 6. Cambios en memoria

El diario conserva programas, versiones y regresiones; la conversación recupera sólo metadatos de la familia pertinente. Se protege al menos un recibo vigente por familia frente al uso intensivo de otras herramientas. No se incorporan los retos a recuerdos personales ni se suben como conversaciones. Un programa guardado se vuelve a validar con cada input nuevo.

La separación completa entre memoria temporal, episódica, conocimiento, validada, procedural y crítica sigue siendo una arquitectura por completar. En este grupo se materializa la memoria procedural y se evita contaminar las otras categorías.

## 7. Cambios en navegación e investigación

`WikipediaResearchClient` conserva lectura HTTPS acotada, búsquedas de descubrimiento y varias fuentes; añade errores por fuente, cancelación, tiempo total de lectura y validación DNS en el propio transporte OkHttp. Se usa conexión directa: los proxies del sistema no se utilizan en este lector.

`PublicResearchCoordinator` mantiene fuentes deduplicadas con IDs estables, hasta tres búsquedas y cuatro llamadas al modelo. El adaptador de producción utiliza `SalveLLM.generateResult`; no transforma un error textual en síntesis correcta. La consulta web ordinaria entrega extractos al prompt conversacional para evitar sintetizar dos veces.

Se eliminaron las afirmaciones de comprensión perfecta y el guardado automático de conclusiones como verdades autobiográficas. Comprobar que una cita apunta a una fuente recibida **no verifica semánticamente cada afirmación**.

## 8. Cambios en planificación

El laboratorio consulta primero el programa guardado, ejecuta un candidato, evalúa el resultado y selecciona una alternativa ante feedback negativo. La investigación permite al modelo decidir si hace una lectura adicional o termina, con formato estricto, límites y detección de consultas repetidas. El ciclo de objetivos existente no pasa a ejecutar arbitrariamente todas las capacidades de fondo.

## 9. Cambios en aprendizaje

Existe adaptación de estrategia y reutilización posterior. La comparación controla únicamente la retención del diario, manteniendo iguales código, corpus y criterios. El laboratorio no produce por sí mismo un diagnóstico causal completo ni una hipótesis científica: los registros marcan esos campos como no establecidos cuando no existen.

## 10. Cambios en infraestructura cloud

No se creó ni modificó infraestructura remota. En la recuperación final se bloquean envíos con configuración de ejemplo; `CloudLogger` usa la cola duradera común y se elimina la purga automática de eventos fallidos. Los eventos agotados permanecen locales, pero reanudarlos y verificar acuses remotos sigue pendiente. Se inspeccionaron `CloudSyncManager`, `CloudLogger`, cola Room y configuración. No hay PHP/backend versionado; el token es una cadena de ejemplo. No se conocen CPU, RAM, GPU, cuotas o servicios de Namecheap. La consulta pública al endpoint no proporcionó acceso verificable; no se enviaron credenciales de ejemplo ni datos privados.

Para continuar hacen falta acceso administrativo acotado o inventario verificable, autenticación por instalación, acuses de envío, reintento/idempotencia y borrado propagado. La autorización de trabajo no sustituye esos recursos.

## 11. Resultados conocidos y comparación antes/después

La versión estable anterior no contenía este módulo: su resultado es **no disponible**, no un 0/104 inventado. La comparación experimental usa el mismo módulo con y sin memoria.

| Medida | Sin memoria | Con memoria |
|---|---:|---:|
| Ejecuciones correctas | 416/416 | 416/416 |
| Intentos de candidatos | 596 | 424 |
| Candidatos rechazados | 180 | 8 |
| Operaciones contadas | 617.869 | 1.519.591 |
| Comprobaciones de regresión | 0 | 1.227 |
| Programas reutilizados | 0 | 407 |

La memoria redujo los intentos un **28,9 %**, pero consumió **2,46 veces** las operaciones por las verificaciones conservadas. No se presenta como mejora universal de eficiencia. La tasa de éxito fue idéntica. Latencias descriptivas de una ejecución JVM: p95 2,12 ms sin memoria y 4,69 ms con memoria; no miden el teléfono ni inferencia.

Evidencia: [resumen](evidence/autonomous-tools/summary.json), [comparación completa](evidence/autonomous-tools/ablation.json), [104 registros enriquecidos](evidence/autonomy-generalization/warmup-cases) y JSONL reproducibles en esas carpetas.

## 12. Fallos y regresiones encontrados

Se detectaron tres errores del nuevo diario: regresiones corruptas aceptadas al reiniciar, desbordamiento de contadores y pérdida de todos los recibos de una familia por recorte FIFO. La revisión del sistema anterior encontró suites generadas omitidas por el runner, declaraciones de investigación sin respaldo y catálogo permisivo/desconectado del estado real del modelo.

## 13. Correcciones verificables

Cada error del diario tiene una reproducción permanente. Restaurar valida los inputs; los contadores usan suma exacta; la retención protege evidencia vigente. El runner incorpora suites válidas antes de calcular el árbol evaluado, rechaza colisiones/symlinks y registra su SHA. Sus tests de Git y contrato no equivalen a ejecutar Docker. Las propuestas antiguas sin suite siguen identificadas explícitamente.

## 14. Pruebas adicionales

Se prueban entradas inválidas, cancelación, pausa/reanudación, escritura fallida, rollback, reinicio, pertinencia y privacidad del contexto, contratos del registro y oráculos adversariales. La investigación tiene pruebas con proveedores de fixture identificados como tales. El catálogo tiene pruebas de formato, disponibilidad y selección sin descargas de red.

El intento de lectura HTTPS real contra TypeSafe y Android falló con `UnknownHostException`. Se conserva como [NOT_VALIDATED](evidence/public-web-smoke.json); no se sustituye por una lectura simulada exitosa. La compilación Android final y sus recuentos se registran en [la validación de esta fase](evidence/autonomous-evolution-validation.json).

## 15. Pruebas nuevas tras congelar el código

**128/128 inputs nuevos correctos**, 32 por familia. La semilla se generó después de fijar hashes de fuentes: `258126683136933666013022950214099361488`. No coinciden con los conocidos, ni por simple reordenación. El evaluador externo comprueba soluciones y continuidad del estado; Salve no recibe respuestas esperadas.

Los 128 reutilizaron programas aprendidos, sin candidatos rechazados, y ejecutaron 384 comprobaciones de regresión. Esto demuestra generalización finita dentro de cuatro dominios conocidos. No demuestra programación general, investigación semántica, inteligencia superior ni aprendizaje neuronal.

Evidencia: [resumen de generalización](evidence/autonomy-generalization/summary.json) y [128 informes individuales](evidence/autonomy-generalization/holdout-cases). Los registros incluyen estado, capacidades, plan, modelos usados —ninguno en esta evaluación—, memoria, acciones, resultados, feedback, cambios y límites.

## 16. Recursos utilizados

La evaluación JVM usa heap máximo de 256 MiB. Los candidatos tienen presupuesto de operaciones y los runners timeout por proceso. Android se compila con SDK 36, Gradle 9.7.1 y JDK 21. No se descargaron pesos nuevos ni se midieron consumo energético, batería o latencia del S24 Ultra. Los metadatos de modelos no inventan esos resultados.

## 17. Limitaciones actuales

No hay inferencia real de un LLM de Salve en estas evaluaciones, ni acceso TypeSafe, navegador DOM, terminal Android general o servidor cloud autenticado. No se ejecutó la prueba Docker de código arbitrario. El código conserva permisos y límites existentes. No se ha probado este APK en el teléfono. La renovación del entorno eliminó la clave debug anterior; una compilación nueva no debe presumirse actualizable sobre el APK anterior con la misma firma.

## 18. Próximos cuellos de botella

Prioridad: probar el modelo instalado con tareas de lenguaje natural y memoria; verificar lectura web desde el teléfono; integrar un navegador estructurado; habilitar el sandbox externo y probar un parche real con su suite; configurar la nube autenticada. Sólo después comparar un decisor Jev u otros modelos mediante el mismo conjunto y métricas de calidad/coste. El [estudio de Jev](JEV_Y_MEMORIA_DE_AGENTES.md) detalla su papel y requisitos.

## 19. Qué evidencia nueva existe

Hay ejecución real de herramientas declarativas, rechazo de candidatos incorrectos, conservación y recuperación de programas, adaptación frente a cambios y transferencia a 128 inputs nuevos. Se conocen tanto la reducción de intentos como su coste adicional. Las afirmaciones amplias que exceden esa evidencia quedan fuera del resultado de esta fase.

## 20. Repositorio y recuperación

Checkpoint del laboratorio y sus evaluaciones: `8af05de`, sobre la base estable del PR 82. Las integraciones se añaden después de ese checkpoint en la misma rama y se entregan en un nuevo PR. `main` no se modifica ni se fusiona automáticamente. Los hashes de código y artefactos acompañan cada evaluación; el commit del PR identifica la revisión entregada.

El rollback del diario recupera programas anteriores; Git permite recuperar el código. Ninguno sustituye backups de datos personales o una estrategia operativa de recuperación cloud. Los fallos registrados no se borran para mejorar una métrica.

## 21. Recuperación y verificación final del 22 de septiembre

Se encontró el checkpoint `8af05de` y las integraciones sin commit; no existía rama remota ni PR de esta rama. Se guardaron las integraciones como `d37a45d`. El código del laboratorio, los evaluadores y el corpus coinciden por SHA-256 con la evaluación original. Los resultados anteriores se conservan; las ejecuciones nuevas están en [reverification-20260922](evidence/reverification-20260922).

| Ronda | Correctos | Intentos | Candidatos rechazados | Reutilizaciones | Regresiones comprobadas |
|---|---:|---:|---:|---:|---:|
| 1 | 104/104 | 112 | 8 | 95 | 291 |
| 2 | 104/104 | 104 | 0 | 104 | 312 |
| 3 | 104/104 | 104 | 0 | 104 | 312 |
| 4 | 104/104 | 104 | 0 | 104 | 312 |

Las cuatro rondas ejecutan el mismo solver con memoria que evoluciona y entradas del corpus previsto; **no son cuatro versiones de código ni cuatro generaciones de LLM**. La primera ronda presenta cinco adaptaciones frente a programas anteriores rechazados. Las siguientes reutilizan estrategias. No hay una medida de conversación general, memoria autobiográfica o planificación abierta entre rondas: esas capacidades no fueron evaluadas por este corpus.

La repetición sin memoria obtiene 416/416, con 596 intentos y 180 candidatos rechazados. Con memoria: 424 intentos, ocho candidatos rechazados, 407 reutilizaciones y 1.227 regresiones. Se conserva el coste adicional (1.519.591 frente a 617.869 operaciones). La semilla nueva del holdout es `23865494274557189258270834515272387938`: 128/128 tras 416 calentamientos; generalización dentro de las mismas cuatro familias.

Verificación Android final: **760 tests JVM, cero fallos, errores u omisiones**, ejecutados con `:app:testDebugUnitTest --rerun`; APK ARM64 compilado y firma v2 validada. Python: **107 tests, 106 correctos y uno omitido**, correspondiente al smoke Docker; no existe Docker en este entorno. No se cambió el criterio de éxito para conseguir estos resultados.

El log interrumpido conserva el fallo de compilación `Files.readString`: su sustitución compatible con `readAllBytes` ya estaba en el árbol local al recuperar el trabajo y se verificó, sin repetir la corrección. Los fallos del diario, suites omitidas y contratos del catálogo descritos arriba tienen cobertura de regresión. El hallazgo nuevo fue el envío con credencial de ejemplo y la eliminación tras agotar reintentos: ahora se bloquea el transporte no configurado, se unifica el logger con la cola y se retienen los eventos. Tres tests nuevos comprueban el rechazo de configuración ausente, insegura o de ejemplo; no simulan almacenamiento remoto real.

### Estado de los ocho requisitos

1. Auditoría y evaluación verificable: completadas para el alcance local documentado.
2. Generación/adaptación y recuperación: completadas para el DSL y las cuatro familias; programación general y nuevas primitivas siguen pendientes.
3. 104 casos × cuatro rondas: completados y reproducidos, con resultados individuales originales y nuevos conservados.
4. Comparación: completada para memoria procedural, planificación finita, herramientas, adaptación y recuperación; comportamiento conversacional y aprendizaje neuronal no medidos.
5. Regresiones: suites JVM/Python ejecutadas, APK compilado; Docker y dispositivo físico no validados.
6. Memoria y agentes: integración local terminada; ningún LLM nuevo activado. Jev permanece sin integrar/activar y requiere credencial de proveedor y comparación real.
7. Persistencia: diario procedural local verificado; **nube no completada**. Faltan backend accesible, autenticación por instalación y protocolo de acuse, idempotencia, retención y borrado. No se afirma que existan aprendizajes subidos.
8. Documentación: informe, límites, fallos y evidencia actualizados. Se solicita revisión mediante PR, sin fusionar `main`.

### Reproducción

Usar el SDK/JDK configurados y dependencias Gradle disponibles:

```sh
python3 -m unittest discover -s scripts -p 'test_*.py'
bash gradlew --no-daemon --max-workers=2 --init-script scripts/android-arm64.init.gradle :app:testDebugUnitTest --rerun :app:assembleDebug
python3 scripts/run_autonomous_tool_evaluation.py --output build/reverification/autonomous-tools
python3 scripts/evaluate_autonomy_generalization.py --output build/reverification/generalization --seed 23865494274557189258270834515272387938
```

Los logs de compilación y resultados XML comprimidos están junto a `verification.json`, que conserva hashes de fuentes y APK. La inferencia real, la lectura web en producción, Jev y Namecheap son bloqueos explícitos: esta entrega no cierra la totalidad del objetivo de autonomía general.
