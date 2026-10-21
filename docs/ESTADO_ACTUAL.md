# Estado verificado de Salve — 21 de octubre de 2026

> **Fuente de verdad y alcance.** Esta página distingue el árbol `main`, las propuestas abiertas y los ensayos históricos. Se revisaron las rutas citadas del repositorio, pero no se ejecutó Salve en un dispositivo. **Código presente no significa función probada en el Samsung ni AGI.** La documentación describe interfaces y límites; el teléfono y la ejecución CI verifican resultados reales.

## Versión de referencia

- Rama: `main`. La revisión de este documento no cambia la APK, los motores, permisos ni datos de Salve.
- PR [#199 — Registro de ejecución](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/199): **fusionada** al comprobar el estado actual. La guía `docs/execution-journal.md` está en `main`.
- PR [#200 — Deliberación situacional](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/200): **fusionada**. `SituationalDecisionFrame` y `docs/DELIBERACION_SITUACIONAL.md` forman parte de `main`.
- PR [#201 — Sincronizar documentación](https://github.com/bryanstevenriverabejarano-svg/Convert/pull/201): **fusionada**. Esta página fue creada por esa propuesta; se actualizan aquí sus referencias históricas.
- El marco adicional de **cohesión** `CognitiveCohesionFrame` está propuesto en la rama de esta revisión y **aún no está fusionado**. Consulta [cohesión cognitiva](COHESION_COGNITIVA.md) en la rama de propuesta; no anunciarlo como función liberada hasta la fusión.

## Mapa de capacidades en `main`

| Área | Base técnica en el árbol | Estado que puede afirmarse |
| --- | --- | --- |
| Conversación y deliberación | `MotorConversacional`, `CognitivePrompt`, `CognitiveTurn`, `SituationalDecisionFrame`, `ConversationSession` | Ruta de turno y marco de conflictos en `main`; las alternativas semánticas y la coherencia en el modelo requieren evaluación de inferencia real |
| Modelos locales | `LocalModelPolicy.kt`, `ModelRuntimeInfo.java`, `docs/dolphin-android.md` | Escalera automática **Dolphin 3.0 Llama 3.1 8B → Dolphin 3.0 Llama 3.2 3B → Gemma 4 E2B**; Gemma 4 E4B está identificada como alternativa **opcional fuera** de esa escalera. Depende de RAM, pesos verificados y carga real; no garantizar 8B en cualquier móvil |
| Catálogo de descargas | `app/src/main/assets/config/models.json`, `LocalModelPolicy.kt` | El JSON de assets contiene la entrada fijada de Gemma 4 E2B; no es una lista completa de todos los GGUF gestionados. No confundir catálogo parcial con motor activo |
| Memoria | `ConversationMemoryGrounding`, `salve.core.mind`, Room | Recuperación léxica e índice semántico opcional; procedencia y límites de recuperación. Una búsqueda de recuerdo no demuestra que lo recordado sea verdadero |
| Memoria multilingüe H2 | `EmbeddingArtifacts.java`, `docs/H2_MEMORIA_MULTILINGUE.md` | Contrato **multilingual-e5-small INT8 + SentencePiece**, descarga de dos artefactos fijados y validación; pruebas de escritorio documentadas. **La carga ARM64 real en el teléfono sigue pendiente de validación** |
| Investigación web | `salve.core.research.GuidedPublicResearch`, `scripts/RESEARCH_INTENT.md` | Plan de objetivo, consultas, análisis individual de páginas y citas de evidencia; límites de 3 consultas, 6 páginas y 11 llamadas al modelo. Acceso, cobertura y exactitud dependen de fuentes y proveedor |
| Agente de tareas | `salve.core.agent.AgentEngine`, `AgentRun` | Bucle acotado plan → herramienta → observación → revisión con recibos persistentes. No implica controlar indiscriminadamente todas las apps ni actuar sin autorización |
| Pensamiento experimental | `salve.core.cognitive.package-info` | `CognitiveCore`/red líquida son **laboratorio desconectado de la ruta de ejecución**. No atribuir la respuesta del chat a esa red experimental |
| Cómputo distribuido | `docs/COMPUTO_DISTRIBUIDO.md` | Nodos **propios** autorizados para inferencia por tareas, con respaldo local. Requieren despliegue, credenciales y pruebas propias; no fragmentan un mismo LLM entre varios dispositivos |
| Avatar | `docs/SALVE_AVATAR_2D.md` | Base 2D por sprites/canvas; no malla 3D activa ni rigging libre. Live2D es integración opcional, no el renderizador principal verificado |
| APK móvil | `README.md`, workflow `Android tests and APK` | La variante `preview` usa identificador propio. Un artefacto de CI no garantiza instalación real en el teléfono ni actualización de una APK firmada con otra clave |

## Dónde encontrar documentación fiable

- [Modelo local y respaldo](dolphin-android.md) — lectura prioritaria frente a la sección heredada de MLC.
- [Memoria H2 en español](H2_MEMORIA_MULTILINGUE.md) — arquitectura, artefactos, calibración y validaciones pendientes en Android.
- [Mente persistente](MENTE_PERSISTENTE.md) — continuidad, fuentes, sueño y límites medidos; consultar también H2 para el estado **más reciente** de embeddings.
- [Investigación guiada](../scripts/RESEARCH_INTENT.md) — interpretación, fuentes, límites y verificación.
- [Computación distribuida](COMPUTO_DISTRIBUIDO.md), [Avatar 2D](SALVE_AVATAR_2D.md), [Continuidad](CONTINUIDAD_Y_APRENDIZAJE.md).
- [Núcleo cognitivo y contrato](NUCLEO_COGNITIVO_V1.md), [Gobernanza](../GOBERNANZA_Y_EVOLUCION.md).
- [Auditoría de septiembre](../ANALISIS_ARQUITECTURA.md) — **histórica**, no inventario de capacidades actuales.

## Contradicciones que corrige esta actualización

1. El `README` mezclaba la prioridad Dolphin 3B, el flujo 8B actual y una guía antigua que declaraba Qwen/MLC «predeterminado». Una instrucción de empaquetado histórico no describe el runtime actualmente escogido.
2. `MENTE_PERSISTENTE.md` decía que la memoria por significado seguía desactivada hasta desarrollar un modelo multilingüe. H2 ya implementa los artefactos y el contrato E5; la activación depende ahora de que **se descarguen/verifiquen** los archivos y pase la calibración en el dispositivo.
3. `ANALISIS_ARQUITECTURA.md` comienza con hallazgos de septiembre y contiene estados «antes de este cambio». No se debe usar como diagnóstico vigente en octubre.
4. Las PR #199, #200 y #201 **ya están fusionadas**; sus textos anteriores de estado pendiente eran una foto temporal. Las PR futuras deben actualizarse antes de anunciar capacidades como disponibles.

## Regla para documentación futura

Para cada cambio, registrar: `rama/commit`, rutas modificadas, interfaz que lo invoca, resultado observado (JVM/CI/Android físico), limitaciones y permiso requerido. Estados permitidos: **implementado en main**, **propuesto en PR**, **experimental desconectado**, **validado en host**, **pendiente en móvil**. Evitar términos como «piensa realmente», «conciencia» o «AGI» como resultados sin métricas específicas.

## Qué queda sin verificar en esta auditoría

No se hizo inventario exhaustivo de todas las clases ni de todos los Markdown; no se compilaron los APK ni se ejecutaron inferencias reales en el S24 Ultra. El estado de una PR puede cambiar después de esta fecha. Esta es una **auditoría focalizada** de la portada y de los flujos que afectaban a la comprensión del proyecto.
