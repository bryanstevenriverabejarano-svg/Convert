# Salve: coordinación cognitiva y aprendizaje verificable — 28 octubre 2026

> Esta etapa implementa componentes concretos, **no** AGI ni aprendizaje continuo
> de pesos. La única fuente de verdad es la revisión de GitHub y, para afirmar
> rendimiento de Gemma, una evaluación posterior en el teléfono con sus pesos.
> Los resultados de Android CI no equivalen a ensayos físicos.

## Arquitectura revisada: lo que ya existía

| Área | Módulos actuales | Decisión |
|---|---|---|
| Interpretar intención e historial | `MotorConversacional`, `ReasoningPlanner`, `ConversationRequestAnalyzer` | Reutilizar, no crear otro LLM |
| Seleccionar y ejecutar herramientas | `CognitiveToolCatalog`, `CognitiveTurn`, `CognitiveToolExecutor` | Ranking acotado antes del catálogo de 500 caracteres; controlador conserva permisos |
| Memoria híbrida | `ConversationMemoryGrounding`, Room, E5 y grafo de conocimiento | Seguir respetando fuente/fecha, límites de contexto y borrado; ampliar evaluaciones antes de subir el número de recuerdos |
| Agente persistente | `AgentEngine`, `AgentRun`, `GoalAutonomy` | Mantener checkpoint, presupuestos, confirmaciones y verificación externa |
| Sueño y práctica | `DreamCycle`, `ReflectionCycle`, `SleepFocusWorker`, `PlayCycle`, `SkillLab` | Práctica adaptativa limitada y pruebas independientes, sin afirmar entrenamiento |
| Aprendizaje inductivo | `BlindSkillTransferExperiment`, `InductiveSkillLab` | Usar el laboratorio ya existente, no inventar otro ni instalar automáticamente habilidades |
| Autoedición de software | `SelfPrDesk`, `scripts/self_pr_ci.py` y GitHub Actions | Separar compilación, evaluación emparejada y aprobación |
| Nodos propios | `DistributedCompute`, `ComputeRouter` | Mantener copia de memoria privada en móvil; no enviar datos sensibles sin política específica |

## Modificaciones ejecutables de esta etapa

### 1. Selector de herramientas por finalidad

`CapabilityPlanner.rank` ordena **únicamente** las capacidades que el
controlador Android ya autorizó para ese turno. Prioriza memoria, web, estado,
dibujo, vestuario, evidencias nocturnas y acciones permitidas según intención,
sin ampliar el catálogo ni omitir el control independiente en el ejecutor.
La llamada real sigue usando `CognitivePrompt.prioritize` → `limitTools`
→ inferencia de Gemma → `CognitiveTurn` → `CognitiveToolExecutor`.

**Ejemplo:** «Busca en internet lo que hablamos antes y contrástalo» ofrece
primero `web.search` y `memory.lookup`, cuando la identidad lo permite y
caben dentro del presupuesto. No significa que se hayan ejecutado: hacen
falta llamadas válidas del modelo y recibos reales.

### 2. Sueño con presupuestos adaptativos y métrica honesta

`NightPracticePolicy` lee los últimos 12 resultados de `SkillLab` con
verificador: ejecuta **uno** o, si dominan al menos tres fallos verificados,
**dos** retos como máximo. Ningún resultado sin verificador justifica ampliar
el esfuerzo nocturno.

Se distingue `attempted`, `solved`, `saved` y el motivo. Una práctica que
se ejecutó pero cuyo progreso no pudo guardarse **no** se presenta como
aprendizaje persistente. El informe declara explícitamente que no cambia los
pesos de Gemma.

`NightTransferTrial` utiliza una vez cada **siete días** el experimento de
transferencia ciega ya existente, solo durante sueño autorizado,
en cargador y con política de recursos favorable. Su examen final de
96 casos se genera después de congelar el programa. Guarda únicamente
contadores y veredicto; **no** instala habilidades, cambia código ni
transmite datos personales. Si se interrumpe, no afirma éxito.

La noche sigue limitada por WorkManager, batería, calor, bloqueo cognitivo
y prioridad de la conversación. No es un proceso continuo 24/7.

### 3. Comparación emparejada de resultados antes de considerar mejoras

`scripts/salve_eval_gate.py` evalúa dos conjuntos de al menos **30**
casos previamente congelados e identificados por su mismo ID, con al menos
3 por dominio: memoria, herramientas, objetivos multipaso, transferencia
y seguridad. Cada caso contiene solamente resultados medidos y verificables,
sin prompts, nombres privados, recuerdos ni transcripciones.

Requisitos para el resultado `CANDIDATE` (no es autorización de instalación):
- Mismo modelo base y distintas revisiones de GitHub identificadas por SHA.
- Ningún fallo nuevo en un caso anteriormente superado.
- Ningún incidente de privacidad, seguridad o afirmación sin evidencia.
- Al menos tres mejoras netas verificadas.
- Latencia p95 no superior al 150% de la versión base.

Las métricas son una **puerta práctica**, no significación estadística
demostrada. Una mejora candidata requiere además revisión humana,
reproducción en un teléfono físico y regresión de memoria/código. No
hay fusión automática vinculada a este resultado en esta etapa.

Ejemplo de uso en ordenador con datos ANONIMIZADOS:
```sh
python3 scripts/salve_eval_gate.py --baseline base.json --candidate candidato.json --report decision.json
```
La suite `scripts/test_salve_eval_gate.py` se incluye en `android.yml`
mediante el descubrimiento de tests Python existente.

## Línea de investigación, no capacidades implementadas

1. **Memoria a varias velocidades (Nested Learning/Hope):** comparar memoria
   inmediata, episodios recientes, esquemas y habilidades consolidadas.
   No instalar la arquitectura Hope ni atribuir su memoria paramétrica a Gemma.
2. **Sleep framework (Google, 27 oct 2026):** entrenar expertos MoE/LoRA
   fuera del móvil y medir olvido catastrófico en un conjunto reservado.
   El sueño operativo de Salve no actualiza parámetros; requiere un
   entrenador compatible, GPU, modelo exacto, versión y reversión.
3. **Metaaprendizaje/Hyperagents:** probar variaciones de políticas de
   estrategia y planificación en contenedores aislados, no autoeditar
   permisos o métodos de verificación.
4. **Modelo del mundo/Genie 4:** comenzar con estados y transiciones
   simbólicas evaluables para tareas con observaciones estructuradas;
   no hay simulador multimodal de mundo real en Android.
5. **Supervisor cognitivo extendido:** `AgentEngine` ya es un bucle de
   plan/act/observar/verificar (8 pasos, 12 llamadas). Una siguiente
   iteración debe unificar sesiones cortas y planes persistentes sin
   eludir el límite de seguridad ni iniciar tareas irreversibles sin permiso.
6. **Memoria iterativa:** evaluar recall@k, fuentes contradictorias,
   rechazo de memorias contaminadas y borrado propagado antes de ampliar
   el presupuesto de `ConversationMemoryGrounding` o sustituir E5.
7. **Regulación de nodos y seguridad de datos:** clasificar fragmentos
   del prompt antes de delegar inferencia; un nodo no recibe la DB completa,
   pero el prompt sí puede contener recuerdos. Validación mTLS y
   política de consentimiento necesarias.

### Fuentes científicas consultadas

- Google Research, 27 oct 2026:
  https://research.google/blog/the-sleep-framework-a-new-way-for-llms-to-retain-knowledge-in-continual-learning/
- Google Research, Nested Learning / Hope:
  https://research.google/blog/introducing-nested-learning-a-new-ml-paradigm-for-continual-learning/
- DeepMind, Genie 4, 27 oct 2026:
  https://deepmind.google/blog/genie-4-a-new-generation-of-world-simulation/
- Hyperagents, 2026:
  https://arxiv.org/abs/2603.19461

**Estas investigaciones son inspiración, no una reproducción experimental.**
No se han desplegado Hope, Genie 4, expertos MoE, entrenamiento LoRA en el
S24 Ultra ni modificaciones recursivas libres del propio núcleo.

## Validación antes de activar

1. CI: Java/JVM, contratos de permisos/herramientas, memoria multilingüe,
   APK ARM64, aislamiento del runner y scripts Python.
2. Samsung: conservar la firma y datos, probar con/sin cargador, salida de
   sueño en el registro, fallos de archivo, cancelación, RAM y temperatura.
3. Benchmark real con pesos instalados: baseline vs candidato en casos
   inéditos, sin modificar a la vez modelo, memoria, prompt y política.
4. Solo si mejora: preparar otro PR específico para probarlo. Mantener
   una vía de reversión y no conceder nuevos permisos automáticamente.
