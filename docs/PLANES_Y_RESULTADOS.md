# Planes y resultados en Android

El panel se abre desde **IA y cámara → Planes y resultados**. Comparte el diario Room y el ejecutor del chat: no crea un segundo motor ni una memoria paralela.

- Escribe un objetivo concreto y pulsa **Crear plan**. También se admiten `trabaja en este objetivo: …`, `trabaja en el objetivo: …` y `planifica y ejecuta: …` en el chat. Estos accesos no conceden ejecución remota de código.
- Consulta estado, intentos reales de herramientas, pasos propuestos, respuestas, fuentes y proveedor de cada inferencia. Los pasos propuestos no se presentan como tareas verificadas ni como porcentaje de éxito.
- Cancela un plan pendiente o bloqueado. Continúa uno cancelado o bloqueado manteniendo sus permisos, resultados y presupuesto. Un plan agotado no recibe otro presupuesto por abrir la pantalla.
- **Pausar/reanudar planes y búsquedas** controla las tareas persistentes y su recuperación. No cambia la pausa de las reflexiones personales de GoalAutonomy.
- **Llevar resultado al chat** carga el resultado mediante el control de conversación existente. Se conserva en el contexto reciente y permite preguntas de seguimiento. La respuesta se trata como datos, sin ejecutar instrucciones de avatar incrustadas.

## Recuperación y límites

El identificador del envío y el objetivo pendiente sobreviven a la recreación de la actividad. Repetir el guardado es una transacción idempotente, incluso después de cerrar la base o completar el trabajo. Cambiar el contenido o los permisos bajo el mismo identificador se rechaza. La pantalla consulta Room fuera del hilo principal y deja de actualizarse al salir.

Cada inferencia recibe el objetivo original, un extracto acotado del plan guardado, evidencia y presupuestos restantes. Se conservan los límites de 8 intentos de herramientas y 12 consultas al modelo. Las pruebas usan motores y herramientas controlados; no demuestran la calidad de respuestas de un modelo real en el teléfono. Se necesita un modelo local funcional; Android puede retrasar el trabajo en segundo plano.

## Comprobación en el dispositivo

1. Crea un objetivo sobre un recuerdo existente y gira la pantalla mientras se guarda: debe aparecer un único plan.
2. Comprueba los estados y fuentes. Sin modelo disponible debe aparecer un bloqueo explícito, sin resultado inventado.
3. Cancela, continúa y pausa. Cierra y vuelve a abrir Salve: deben mantenerse el mismo identificador y sus observaciones.
4. Cuando haya respuesta, llévala al chat y pregunta «¿en qué te basas?».

Las pruebas automatizadas cubren estas operaciones de pantalla con un backend controlado, recreación durante el guardado, transacciones Room al reabrir la base y contexto reducido del agente. La instalación y calidad real en el Samsung S24 Ultra quedan pendientes de comprobación física.
