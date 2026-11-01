# Salve — revisión de interpretaciones subjetivas antes de romper dependencias

Fecha de base: 2026-11-01, rama `main` tras PR #325.
Esta revisión examina rutas y clases concretas; **no** supone haber probado
comprensión semántica del modelo real en un teléfono.

## Resultado principal: la aparente duplicación puede ser una función necesaria

No fusionar clasificadores solo porque reciben el mismo texto.
Una misma frase contiene varias capas de información:

| Capa | Componente actual | Qué debe preservar |
| --- | --- | --- |
| Acto conversacional | `ConversationRequestAnalyzer` | Preguntar, opinar, corregir o pedir explicación; señal pragmática **no autorización** |
| Continuidad e intención elíptica | `DialogueFocus` | «Hazlo», opciones mencionadas y límites de contexto; nunca asumir elección inexistente |
| Objetivo del turno | `TurnObjective` / `ReasoningPlanner` | Un marco común para señales preexistentes; no convertirse en segundo LLM |
| Evaluación de conflictos | `SituationalDecisionFrame` | Prioridades *posibles*, citadas como texto del usuario, sin dar por probadas emociones |
| Demanda y estado de inferencia | `CognitiveCycleFrame` | Metadatos baratos de contexto; pueden ser incompletos; nunca un veredicto factual |
| Memoria escrita y causal | `MemoryWritePolicy`, `CausalMemory` | Distinguir afirmación, hipótesis, cita, negación y corrección |
| Consulta de recuerdos | `ConversationMemoryGrounding` | Recuperación con procedencia, sin equiparar coincidencia con verdad |
| Búsqueda pública | `ResearchConversation` | Intención explícita o dato vigente que requiere consulta; no copiar recuerdos |
| Selección y ejecución | `CapabilityPlanner`, `CognitiveToolCatalog`, `CognitiveTurn` | Ranking no concede poderes, permisos siempre en el ejecutor |
| Verificación | `ConversationOutcomeAudit`, `AgentGoalVerifier` | Resultado de herramienta != objetivo demostrado |

En particular, **un gran componente fuertemente conexo en el grafo de imports
NO demuestra funciones duplicadas**. El inventario de PR #325 contiene 540
unidades Java/Kotlin, 43 grupos y 166 relaciones intergrupos observadas.
Las llamadas JNI, la reflexión, la UI declarativa y los efectos runtime no se
capturan en ese inventario.

## Problema reproducible: persistir texto sin una orden de memoria

`IntentRecognizer.recognize` aceptaba `guardar` / `guarda` en cualquier lugar
y también cualquier aparición de `=`. `MotorConversacional` luego llama a
`procesarIntencion` y para `GUARDAR_RECUERDO` acaba en
`memoria.guardarRecuerdo`. Ejemplos ambiguos que antes activaban escritura:

- «¿Por qué debería guardar recuerdos?» (debate, NO orden).
- «Creo que guardar notas podría ser útil» (opinión, NO orden).
- «¿Qué significa x = y?» (pregunta matemática, NO memoria).

### Corrección aplicada en esta fase

Se requiere una petición explícita al principio («guarda», «anota»,
«registra», con prefijos de cortesía) para el efecto. Se mantiene
`aprendí que …` al comienzo como atajo autobiográfico existente.
`Guardar: dato …` se conserva como formato explícito.
Una fórmula suelta `x = y` deja de activar guardado; para almacenar,
«guarda x = y» lo expresa inequívocamente.

## Dos efectos adicionales descubiertos en la revisión de rutas

La misma debilidad léxica también afectaba a:
- **Misiones:** `IntentRecognizer` reconocía «nueva misión» o «añade misión» en cualquier parte; `MotorConversacional.manejarAgregarMision` termina llamando a `MemoriaEmocional.agregarMision` y guarda un recuerdo. Ahora solo se aceptan peticiones explícitas que comiencen por esos verbos y tengan contenido.
- **Sueño:** `IntentRecognizer` reconocía «dormir», «duerme», «modo sueño» y «ciclo de sueño» en cualquier contexto. `MotorConversacional.procesarIntencion` llama entonces a `MemoriaEmocional.cicloDeSueno`, que reorganiza recuerdos, inicia tareas y programa `DreamWorker`. Ahora se exige una orden directa como «duerme», «inicia el ciclo de sueño», «activa el modo sueño» o la orden aislada «modo sueño»; preguntar cómo funciona no inicia nada.

No se cambia el sueño automático programado desde los controladores existentes ni se quitan las misiones que ya están guardadas. Las verificaciones se realizan sobre el reconocedor de comandos: las clases Android que realizan los efectos permanecen intactas.

Esto **no elimina opiniones, hipótesis ni recuerdos**: siguen entrando al
diálogo, historial y recuperación según sus políticas. Sólo evita que una
*referencia léxica* a guardar sea tomada como consentimiento para persistir.
Tampoco modifica herramientas ejecutables, el modelo, la nube, Room, los
permisos de Android ni la política de seguridad.

Límite aceptado: una pregunta cortés «¿puedes guardar esto?» no activa esta
ruta determinista; el usuario puede pedir «guarda esto: ...». Preferimos no
escribir sin una intención clara. Una futura mejora de lenguaje puede proponer
la operación **para confirmarla**, pero no debe confirmarla por sí misma.

## Sospechas adicionales: NO modificadas sin pruebas específicas

1. **Memoria causal**: `CausalMemory.extract` usa patrones lingüísticos como
   «X provoca Y» y puede extraer de un comentario citado, hipotético o inseguro
   una relación que el usuario no presentó como hecho. Evitar promover esas
   relaciones a certeza. Antes de cambiarlo, hay que definir cómo conservarlas
   como hipótesis atribuida y cómo corregirlas/borrarlas, sin perder aprendizaje.
2. **Perfil autobiográfico**: `MemoryWritePolicy.shouldPersist` identifica
   frases como «mi nombre es» en cualquier posición; merece pruebas contra
   ejemplos, citas y mensajes de otra persona.
3. **Actos combinados**: `ConversationRequestAnalyzer` clasifica con reglas
   léxicas, y una misma petición puede contener simultáneamente una orden y
   «creo que». Esa etiqueta debe seguir siendo orientativa, no una autorización
   ni una representación completa del significado.
4. **Diálogo ambiguo**: `DialogueFocus` ya contiene inferencias sujetas a
   contexto sobre aceptación de opciones. Solo debería emitir *señales
   provisionales* cuando existen antecedentes, sin forzar decisiones irrevocables.
5. **UI frente a núcleo**: llamadas de `core` a actividades de
   `presentation` parecen acoplamientos Android reales, pero no equivalen a
   redundancia cognitiva. Separarlas requiere adaptadores y pruebas de navegación
   antes de mover clases.

## Hallazgo arquitectónico adicional: comandos antiguos ANTES de TurnObjective

En `MotorConversacional`, `TurnObjective.understand` aparece cerca de la
línea 1254, pero varias rutas con `inputLower.contains(...)` se ejecutan
**antes** de llegar ahí. Un ciclo unificado de interpretación todavía no gobierna
todas las acciones ni debe reemplazar las rutas específicas sin estudiar sus
efectos:

- `programa` / `escribe código` / `aprende a programar`: puede iniciar
  generación de código al *mencionar* programación, incluso si se quería
  conversar sobre ella.
- `crea una página web` / `publica en internet`: inicia el flujo de
  desarrollo y despliegue. El código debe exigir objetivo explícito y mantener
  aprobación/confirmación de publicación en la frontera real.
- `analiza instagram` / `analiza x`: abre flujo de redes sociales; la
  coincidencia `analiza x` es especialmente amplia.
- `conéctate a la wifi`: abre los ajustes Wi-Fi mediante un `contains`; citar
  esa frase para explicarla no debería abrir los ajustes.
- `analiza la pantalla`: lee datos de Accesibilidad; la intención, el permiso
  y el contexto visual deben tratarse separadamente.

**Estas rutas NO se modifican aquí.** Primero: enumerar los comandos existentes,
sus permisos, confirmaciones, efectos y tests. Luego: sustituir únicamente la
*condición de autorización de acción* por un analizador explícito de comandos
acotados. Conservar lectura, discusión, analogías, emociones, interpretación
del LLM, rutas de cuentos y respuestas subjetivas.

Esto explica por qué «un solo objetivo» en PR #325 significa el núcleo
conversacional normal, **no** que todas las rutas especializadas ya pasen por
`TurnObjective`. No hay que ocultar esa limitación.

## Siguiente refactorización segura (no realizada en esta PR)

1. Crear **pruebas de comportamiento** por dominio antes de cambiar
   `CausalMemory` o `MemoryWritePolicy`: afirmación, creencia, posibilidad,
   cita, pregunta, negación y corrección. Sin transferir hipótesis a hechos.
2. Etiquetar **procedencia y certeza**, sin crear una segunda base ni borrar
   recuerdos históricos. Debe poder convivir «el usuario cree X» con «X no
   verificado»; no convertir `observed` de un recibo en `true`.
3. Conservar la independencia de `DialogueFocus`,
   `SituationalDecisionFrame` y el LLM: el marco común coordina, no sustituye
   a sus perspectivas.
4. Extraer adaptadores de presentación solo cuando se identifiquen llamadas
   concretas y existan tests que comprueben abrir/cerrar/cancelar sin perder voz.
5. Realizar evaluación con el modelo local en Android de diálogos de múltiples
   turnos, cambios de interlocutor, negaciones, permisos y opiniones subjetivas.

**Criterio de seguridad:** una interpretación incierta jamás autoriza por sí
sola escritura privada, publicación, control del teléfono o cambios de código.
La capacidad de comprender matices debe aumentar las *hipótesis consideradas*,
no los *poderes concedidos*.
