# Cohesión cognitiva observable — primera integración

## Qué se integra

El turno conversacional real de Salve ya reúne `SYSTEM_STATE`, `SELF_MODEL`,
`MEMORY_CONTEXT`, `CONTEXT`, `HISTORY`, `USER_INPUT` y `TOOL_RESULTS`.
`CognitivePrompt.build` ahora les proporciona un **punto de coordinación común**:
`TASK_STATE.cohesion`, generado por `CognitiveCohesionFrame`.

La nueva estructura no es una segunda mente, una cadena de pensamiento, un
motor de herramientas ni otro almacén. Es un pequeño metadato que informa al
LLM de qué fuentes tiene y qué **comprobación observable** es prioritaria.

| Pilar | Módulos existentes | Papel de la cohesión |
| --- | --- | --- |
| Comprensión | `SituationalDecisionFrame`, `CognitivePrompt`, investigación guiada | Reutilizar el conflicto observado para priorizar comparar alternativas |
| Autoconocimiento funcional | `SelfModelJournal`, `SelfDiscoveryCycle`, estado Android | Señalar presencia de fuentes distintas sin tratarlas como hechos absolutos |
| Creación | `CreativeRequest`, `InductiveSkillLab` | Evitar afirmar que la creación está integrada cuando su ruta especializada no usa este turno |
| Cohesión | `CognitiveCohesionFrame` + contexto existente | Producir una fase común basada en resultados visibles, sin copiar recuerdos |
| Aprendizaje | `SkillLab`, recibos de `CognitiveTurn` | Distinguir verificación del resultado de herramientas frente al éxito del objetivo. **No entrena ni modifica habilidades** por sí solo |

## Estados del punto de coordinación

- `GROUND_RESPONSE`: no hay evidencia de una herramienta ejecutada ni
  señal de conflicto en el marco actual. Usar hechos disponibles y admitir incertidumbres.
- `COMPARE_ALTERNATIVES`: `SituationalDecisionFrame` detectó indicios
  de prioridades en conflicto. Evaluar opciones, no imponer una decisión personal.
- `VERIFY_TOOL_OUTCOME`: existe al menos un recibo `completed`. Significa
  que el controlador terminó una invocación, no que el objetivo se haya cumplido.
- `REPLAN_AFTER_FAILURE`: existe al menos un recibo `failed`.
  Conservar el objetivo, revisar la evidencia y proponer una alternativa autorizada.

`sources_present_not_truth` contiene exclusivamente nombres de fuentes
presentes (`runtime_observed`, `self_model_unverified`,
`memory_unverified`, `context_unverified`,
`conversation_unverified`); no incluye contenido ni datos personales.
`tool_receipts_observed` contabiliza como máximo dos estados reconocidos,
sin copiar el texto de una herramienta o un dato web.

Se prioriza **fallo > ejecución completada > comparación de opciones > respuesta
fundamentada**. El mecanismo se recalcula tras cada resultado de herramienta,
porque `CognitiveTurn` ya reconstruye el prompt con los recibos nuevos.

## Frontera de autoridad y memoria

`cohesion` es metadato dirigido al modelo y **no puede**:
- crear herramientas ni alterar el catálogo ejecutable;
- conceder permisos, modificar `SYSTEM_STATE` o declarar un modelo activo;
- promover datos de `MEMORY_CONTEXT` o `USER_INPUT` a instrucciones del controlador;
- afirmar que una persona siente algo, que Salve tiene experiencia subjetiva
  o que un resultado externo fue exitoso;
- registrar habilidades nuevas sin un verificador independiente.

El generador lo omite cuando consumiría espacio necesario para el mensaje
y las evidencias reales, en especial en prompts de 3.000 caracteres.
Se conserva la ruta compacta preexistente y su contrato JSON sin cambios.

## Pruebas y límites de la entrega

`CognitiveCohesionFrameTest` comprueba prioridad de fases, separación de
fuentes, inyección en textos de memoria/herramientas, presupuesto y que el
catálogo no gana permisos por aparecer la coordinación.

El código **no prueba comprensión humana completa, autoconciencia subjetiva
ni capacidad de generar habilidades arbitrarias**. Las pruebas JVM validan
estado y contratos; es imprescindible evaluar con Dolphin/Gemma en el móvil:
múltiples turnos, decisiones con valores en conflicto, creación, memoria,
fallos de herramientas, presión de RAM y comparación con la versión previa.

## Iteraciones siguientes, todavía pendientes

1. Representar **opciones tipadas** de creación/planificación y criterios
   verificables (beneficio, daño, reversibilidad, esfuerzo y permisos).
2. Añadir un adaptador común del **resultado real** para `SkillLab` y
   `AgentExperience`, asegurando que un recibo `completed` por sí solo no
   active aprendizaje ni cambie la estrategia.
3. Integrar esas señales en la vía `CreativeRequest` y en investigaciones,
   conservando sus presupuestos independientes; evitar crear otra ruta de LLM.
4. Medir transferencia a tareas inéditas con un conjunto de pruebas separado
   de los ejemplos usados durante las mejoras y publicar también los fallos.
