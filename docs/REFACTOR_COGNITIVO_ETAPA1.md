# Reforma cognitiva de Salve — etapa 1: separar coordinación y corregir clasificación

## Motivación verificable
`MotorConversacional.java` concentra conversación, voz, dibujo, herramientas,
identidad, aprendizaje y memoria. Su tamaño dificulta cambiar una función sin
romper otra. Esta etapa **no reescribe el motor**: extrae un bloque preciso y
mantiene el mecanismo y el almacenamiento originales.

## Diseño aplicado
- `ConversationSkillCoordinator` es una clase Java independiente de Android
  y del modelo. Su frontera es `Supplier<SkillLab>` +
  `Consumer<SkillLab.Decision>`. Sus métodos reciben eventos observados del
  turno, atribuyen resultados al paso que los produjo y devuelven contexto
  legible para el modelo; no ejecutan herramientas.
- `MotorConversacional` mantiene la entrada del usuario, el dibujo, el
  verificador visual, el motor LLM y el registro de acciones; **delegan**
  `observarExperiencia`, `contarDecision`, `aprenderFuente`,
  `esperarVeredicto`, `estrategiaRespuesta` y `misHabilidades`.
- La respuesta generada registra **únicamente un resultado provisional**
  (`sin_correccion`) que se puede invalidar cuando llega una corrección.
  Una salida no generada no se cuenta como acierto. Un dibujo sin verificación
  visual ni opinión posterior no se vuelve aprendizaje.
- `SkillLab` continúa realizando predicción, observación, experimentación
  y persistencia. No hay otra base de datos, un agente adicional ni un ciclo de
  inferencia nuevo.
- La clase tiene pruebas JVM propias para estrategias, errores, aprendizaje
  verificable, correcciones y consumo único de eventos. El turno Android
  requiere además CI y prueba física para considerarlo validado.

## Corrección de gobernanza
La clasificación de `ObjectiveGovernance` usaba `contains()` sobre
cadenas de caracteres. Eso bloqueaba `probar` porque contiene `robar` y
`armario` porque contiene `arma`. Ahora las señales se comparan con límites
de palabra Unicode, manteniendo acentos normalizados y todas las reglas de
consentimiento, impacto y reversibilidad. Incluimos pruebas positivas,
negativas y con aprobación explícita.

Este filtro léxico **no entiende las intenciones**; una petición que hable
*acerca de* un riesgo puede seguir siendo clasificada conservadoramente como
peligrosa. Acciones del mundo real conservan validación tipada y permisos
específicos en sus propios ejecutores.

## Contrato para etapas posteriores
1. Separar la ruta de **herramientas cognitivas** de `MotorConversacional` en
   un módulo con catálogos tipados y pruebas de permisos.
2. Integrar los recibos de agentes con un **resultado de objetivo verificado**;
   `completed` de una herramienta no significa éxito del objetivo.
3. Elaborar conjuntos de tareas no vistas para probar transferencia de
   habilidades, evitando que las pruebas y el aprendizaje usen los mismos
   ejemplos.
4. Medir memoria, fallos, RAM, latencia y continuidad en un Android real.
5. Mantener documentación y registros de decisiones actualizados cuando se
   fusione cada etapa. La arquitectura no demuestra consciencia subjetiva ni AGI.

Esta etapa se propone como PR, no como APK ya instalada.
