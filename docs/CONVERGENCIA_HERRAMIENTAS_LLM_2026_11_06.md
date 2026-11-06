# Salve: convergencia de herramientas bajo el LLM (2026-11-06)

## Objetivo
El usuario formula un objetivo libre. Salve interpreta el objetivo y decide qué adaptador existente utilizar, en qué orden, y si debe responder sin herramientas. El modelo nunca crea permisos ni ejecuta directamente intents arbitrarios.

Diferenciar:
- Ofrecida: autorización del controlador para que el modelo proponga una herramienta en este turno.
- Solicitada: el modelo propone la acción y sus argumentos.
- Entregada (handoff): flujo de Android o interfaz de revisión; puede fallar o cancelarse.
- Completada: el ejecutor interno devuelve un resultado.
- Verificada: una señal independiente corrobora el objetivo final.

## Componentes que ya existen y no deben duplicarse
TurnObjective y ReasoningPlanner entienden el objetivo; CognitiveToolCatalog autoriza; CapabilityPlanner prioriza; CognitiveTurn limita inferencias, operaciones y repeticiones; CognitiveToolExecutor delega; ConversationOutcomeAudit y ExecutionTrace distinguen evidencia y resultados. PhoneActionsDesk, PhoneActions, DeviceControlActivity, MusicController, AgentRuntime, GoalAutonomyRuntime y ResearchTaskRuntime ejecutan funciones concretas.

## Estado de integración con el LLM
| Grupo | Adaptadores | Estado |
| --- | --- | --- |
| Memoria, web, dibujo, estado, PDF, nube, código, conceptos, avatar, retos | CognitiveToolCatalog / CognitiveToolExecutor | Disponibles al modelo cuando se cumplen las condiciones actuales |
| WhatsApp | whatsapp.draft / DeviceControlActivity | PR #367: redacción libre, revisión humana para envío |
| Aplicaciones | device.open_app / PhoneActions | Esta PR: apertura solicitada, verificación externa pendiente |
| Rutas | device.navigate / PhoneActions | Esta PR: ruta solicitada, destino verificado en la orden |
| Música | device.play_music / device.search_music / PhoneActions | Esta PR: no confundir apertura con reproducción |
| Llamadas | device.call / DeviceControlActivity | Esta PR: revisión antes de llamar |

## Siguientes grupos que necesitan integración y pruebas
1. Multimedia completa: MusicControlCommand / MusicController (play, pausa, pistas, qué suena). Verificar reproducción con MediaSession cuando sea posible.
2. Accesibilidad y rutinas: SalveAccessibilityService. Planes de varios pasos con observación del foco/paquete y cancelación si cambia la pantalla.
3. Sensores, cámaras, fotos, galería: sesiones efímeras, permisos, privacidad y resultados de modelos de visión.
4. Objetivos y agentes persistentes: conectar AgentRuntime, ResearchTaskRuntime y GoalAutonomyRuntime al mismo contrato de resultados, sin segundo planificador.
5. Ajustes del teléfono, hogar, Bluetooth, pCloud, autodesarrollo: autorización por tipo de efecto, con registro y verificación.
6. Coche y Android Auto: límites de uso en conducción, audio y restricciones de plataforma.

Estas rutas NO se consideran migradas hasta tener código, pruebas de regresión y verificación en el teléfono.

## Contrato de aceptación común
1. El modelo elige una herramienta y puede redactar libremente; Android conserva autorización y comprueba destinatarios/destinos de acciones externas.
2. Autorización, identidad, argumentos y contexto se verifican en el ejecutor antes de cada efecto.
3. Acciones de riesgo (enviar, llamar, comprar, borrar, publicar) mantienen revisión proporcionada; nunca confirmaciones duplicadas de una misma etapa.
4. Un intent aceptado o un texto generado nunca equivale por sí solo a objetivo cumplido.
5. No se crea un segundo motor paralelo; usar los ejecutores existentes y migrar gradualmente rutas literales.
6. Conservar el flujo antiguo como respaldo durante la migración, sin declarar cobertura total antes de comprobarla.
7. Probar órdenes libres, erratas, variantes por voz, tareas múltiples, salidas inesperadas y cancelación. CI no equivale a validación física del Galaxy S24 Ultra.

## Pruebas de esta fase
DeviceActionIntentTest comprueba selección, destinatarios y destinos basados en la orden real, acceso privado, rechazo de argumentos no declarados y handoff sin falso éxito.
