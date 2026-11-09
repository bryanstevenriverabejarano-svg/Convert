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
| Controles multimedia | device.media_control / MusicControlCommand / MusicController | Esta PR: modelo elige acción; controlador impide invertir play, pausa, siguiente, anterior, información de título |
| Llamadas | device.call / DeviceControlActivity | Esta PR: revisión antes de llamar |
| Sentidos del teléfono | device.sense / DeviceSenses / SistemaSensorial.percibir | 2026-11-09: ráfaga de solo lectura de todos los sensores y del estado del dispositivo; ver [relieve 3D y sentidos](RELIEVE_3D_Y_SENTIDOS.md) |
| Mandos físicos | device.control / DeviceControlIntent / SistemaSensorial.accionar | 2026-11-09: linterna, volumen y vibración; la acción y el valor salen de la frase del usuario |
| Funciones propias de Salve | self.command / SelfCommandIntent / MotorConversacional.runSelfCommand | PR siguiente: memoria, núcleos, modelo, descarga, PR propios, propuestas de mejora, modo oídos, voz, escuchar, mapa de la mente, registro/biblioteca, cuerpo astral, palabra de activación, asistente predeterminado, ajedrez |

## Funciones propias bajo el LLM (self.command)
Antes, unas veinte órdenes literales («¿cómo está tu memoria?», «activa el modo oídos», «abre el mapa de tu mente», «parpadea»…) respondían ANTES de que el modelo viera el turno. Ahora, con modelo cargado y Bryan verificado:
- El reconocedor de siempre solo **ofrece** `self.command` con los ids que pidió el usuario en este turno (`SelfCommandIntent.detected`).
- El LLM decide si la usa o si basta con conversar.
- El ejecutor vuelve a comprobar el id contra la frase real (`SelfCommandIntent.verified`); los detalles (encender/apagar, qué voz, qué pestaña, qué gesto) se sacan de lo que dijo el usuario, nunca de los argumentos del modelo.
- Las funciones de lectura (memoria, núcleos, modelo, descarga, estado de PR, propuestas) devuelven su dato real al modelo como recibo y él lo explica con sus palabras. Las que tienen efecto (abrir pantallas, cambiar modo, mover el cuerpo, abrir un PR) son entrega (handoff), no objetivo verificado.
- Una negación explícita («no abras…») no se ofrece al modelo y vuelve al parser antiguo.
- Una sola implementación (`runSelfCommand`) sirve al LLM y al respaldo sin modelo.
- No migradas a propósito: cambio de modelo (descarga/recarga el motor que está usando el propio turno), olvido/confirmaciones, protocolo de seguridad, historias y partidas en curso, gestos del avatar ligados a la sesión.

## Siguientes grupos que necesitan integración y pruebas
1. Multimedia avanzada: ampliar la interpretación natural y verificar el estado real de la reproducción a través de MediaSession cuando esté disponible. Las acciones principales ya están incorporadas a esta PR.
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
SelfCommandIntentTest comprueba que las funciones propias se ofrecen solo a Bryan verificado, que el modelo no puede elegir una que no se pidió, que «abre el mapa de tu mente» no se trata como app instalada y que la mensajería sigue en whatsapp.draft. DeviceActionIntentTest y MediaActionIntentTest comprueban selección, destinatarios y destinos basados en la orden real, acceso privado, rechazo de argumentos no declarados y handoff sin falso éxito.
