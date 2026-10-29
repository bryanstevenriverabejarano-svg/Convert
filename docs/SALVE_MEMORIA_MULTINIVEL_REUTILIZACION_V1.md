# Salve — Memoria multinivel mediante reutilización (etapa siguiente al PR #292)

## Auditoría: qué tenía realmente Salve antes de esta etapa

Esta implementación **no** crea otro almacén de recuerdos ni entrena los
pesos de Gemma. El repositorio ya contaba con cinco tipos de información:

| Nivel | Componente existente | Autoridad |
|---|---|---|
| Conversación activa / memoria de trabajo | `ConversationSession`, `MotorConversacional` | Mensajes de sesión; no equivalen a recuerdos persistidos |
| Episodios con fecha y fuente | `RecuerdoEntity` en Room, `ConversationMemoryGrounding` y E5 | Fuente original consultable, no instrucción ejecutable |
| Creencias y preguntas consolidadas | `DreamCycle`, `SelfModelJournal`, `SelfModel.Belief` | Hipótesis inferidas; citan episodios y pueden tener contradicciones |
| Lecciones de errores | `LessonBook` | Orientación operativa, no prueba de un hecho personal |
| Estrategias experimentadas | `SkillLab` y `AutonomousToolLab` | Resultado del laboratorio y decisiones de adoptar/revertir |
| Asociaciones y mapa de la mente | `MindMap`, tablas `KnowledgeNode` y `KnowledgeRelation` | Asociaciones útiles; no permiten reconstruir hechos borrados |

En particular, `DreamCycle` **ya** consolidaba hasta 24 episodios
citando IDs exactos; `SelfModelJournal` **ya** comprobaba y limitaba
fuentes y contradicciones. Duplicar ese trabajo en otra base de datos
crearía divergencias y pérdida de trazabilidad.

## Qué se ha conectado

El nuevo **`MemoryLayerBridge`**, solo de lectura, toma la
`ConversationMemoryGrounding.Result` recién recuperada (fuente primaria)
y selecciona como máximo:
- 2 creencias existentes con confianza suficiente (≥0,55), sin contradicción
  y **con todas las fuentes citadas presentes entre los episodios originales
  del resultado actual**;
- 2 lecciones reforzadas por varias correcciones, etiquetadas
  «procedimiento orientativo» y nunca como biografía;
- 1 estrategia que `SkillLab` haya marcado `adoptado`, presentada como
  estrategia de laboratorio y no como un hecho personal.

El puente no consulta al LLM, no crea recuerdos, no borra ni consolida
creencias, no cambia fechas y no comunica textos privados a servidores.
Se llama desde `MindRuntime.memoryLayersForPrompt` solo si
`IdentityManager.privateAccess()` es verdadero.

### Dónde se usa

- Conversación: después de la recuperación original o del segundo intento
  acotado del PR #292, en los turnos con evidencia recuperada.
- Herramientas: `CognitiveToolExecutor.memory.lookup` incluye el bloque
  de capas **además del** recibo primario de Room. El LLM sigue decidiendo
  qué herramienta usar; el controlador mantiene los permisos.
- No se aplica a ficción, usuario sin verificación, resultados `ERROR`,
  `PARTIAL`, respuestas sin episodios originales ni consultas solo de grafo.

El bloque está limitado a 900 caracteres y declara explícitamente que
las creencias son hipótesis y las lecciones son guías, no hechos verificados.

### Protección de borrados y origen

Aunque exista una creencia persistida en `SelfModelJournal`, el puente
**no la muestra** salvo que TODOS sus IDs `recuerdos:N` sigan apareciendo
como fuentes originales en la lectura Room actual. Una inferencia de la
web pública tampoco se usa para respaldar una creencia biográfica sobre
el usuario. Esta política es deliberadamente conservadora: puede omitir
una creencia genuina que no esté en las primeras cuatro coincidencias,
pero evita reintroducir afirmaciones derivadas de datos ya borrados.

Esto **no implica que `SelfModelJournal` borre físicamente esas creencias**
al olvidar la fuente. La propagación de revocaciones a todas las copias
persistentes, índices y sincronizaciones queda como migración posterior
que exige un inventario integral de referencias y pruebas de borrado.

## Pruebas añadidas

`MemoryLayerBridgeTest`: fuentes actuales y ausentes, contradicciones,
origen web, no reconstrucción desde grafo, niveles procedimentales,
estrategias adoptadas, datos malformados y presupuesto estricto.

## Cómo evaluar la utilidad real

Probar en el Galaxy S24 Ultra con el mismo Gemma y mismos recuerdos:
- Exactitud de «¿qué recuerdas sobre X?», citas que existen y
  resistencia a recuerdos desactualizados y hechos contradichos.
- Influencia de lecciones y estrategias sin que se confundan con hechos.
- Tiempo adicional, bytes de contexto, RAM, y fallos de E5/pCloud.
- Borrado de un recuerdo citado: comprobar que una creencia derivada
  deja de ser mostrada incluso si sigue guardada internamente.

**Estado:** integración del puente en Android pendiente de PR/CI y ensayo
en dispositivo físico. No hay AGI ni reentrenamiento paramétrico.
