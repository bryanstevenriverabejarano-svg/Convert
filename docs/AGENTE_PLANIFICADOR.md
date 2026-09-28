# Salve: planes persistentes, herramientas y eventos

Este cambio conecta el modelo local con un bucle acotado de planificación, herramienta, observación, evaluación y corrección. Los flujos de investigación y memoria anteriores siguen disponibles. Implementar y compilar este código no instala el APK, descarga los pesos ni configura un servidor privado en el teléfono del usuario.

## Uso

| Instrucción | Comportamiento |
|---|---|
| `resuelve y recuerda: consulta https://example.org/datos y resume los cambios` | Guarda un objetivo; el modelo propone pasos, lee herramientas y revisa una respuesta con evidencia. |
| `resuelve con código: calcula los primeros 30 números primos` | Autoriza Python remoto sólo para ese plan; requiere el puente configurado. |
| `mis planes` / `resultado de mi último plan` | Estado, pasos, límites, fuentes y modelo observado en cada inferencia. |
| `cancela plan ID` / `reanuda plan ID` | Cancela o recupera una tarea bloqueada/cancelada conservando permisos y diario. |
| `vigila cada 6 horas: novedades sobre un tema público` | Autoriza un objetivo recurrente, con intervalo mínimo. |
| `vigila al abrir Salve: un objetivo público` | Activa al abrir la interfaz, como máximo una vez cada seis horas. |
| `vigila mientras carga: un objetivo público` | Comprueba la carga en el trabajo periódico; intervalo mínimo de seis horas. |
| `mis vigilancias` / `cancela vigilancia ID` | Revisa o elimina activaciones futuras; los planes creados tienen cancelación propia. |
| `pausa tu autonomía` / `reanuda tu autonomía` | Pausa común persistente; invalida ejecutores antiguos y conserva progreso. |
| `estado del agente` / `configura herramientas` | Capacidades y métricas observadas / pantalla privada de conexión. |

WorkManager aplica batería, Doze y límites del sistema: una hora es el intervalo de comprobación, no una garantía de ejecución exacta. La conversación tiene prioridad entre inferencias; una llamada nativa ya en curso no se interrumpe mágicamente. Los eventos necesitan una autorización explícita guardada; el modelo no puede crear vigilancias a partir de una página o recuerdo.

## Bucle y almacenamiento

1. `AgentCommand` convierte únicamente el turno del usuario en una petición tipada.
2. `RoomAgentStore` guarda el objetivo y sus capacidades. `AgentWorker` recibe sólo un ID.
3. `AgentEngine` obtiene una concesión de ejecución, pide un paso JSON al modelo local y valida el contrato.
4. `AgentPolicy` contrasta la propuesta con las herramientas registradas y los permisos originales. Una denegación es una observación visible.
5. Guarda el ID de llamada antes de usar la herramienta; después guarda recibo y estadísticas en una transacción. Un reinicio conserva el mismo ID.
6. Una respuesta necesita identificadores de observaciones útiles. El revisor puede pedir hasta dos correcciones. La revisión es otra inferencia local; no una prueba independiente de verdad.
7. La respuesta final y su evento de sincronización opcional se archivan atómicamente. `sintesis_agente` conserva procedencia y queda fuera de consultas personales y del perfil del usuario.

Room 8 añade `agent_runs`, `agent_subscriptions` y `agent_tool_stats`, sin borrar memoria ni reconstruir el historial. Las migraciones conservan el índice FTS y las tareas antiguas. La memoria corta sigue en la sesión; la intermedia en los diarios y objetivos; la larga en recuerdos con fecha/procedencia, perfil confirmado y grafo existente. pCloud es réplica/restauración, no una segunda autoridad sobre la identidad.

El diario guarda decisiones breves, llamadas y resultados, sin cadenas privadas de razonamiento. `providers` registra el proveedor devuelto por cada inferencia, incluso si el router cambia entre Dolphin y Gemma. El contexto se limita según el modelo activo; el diario completo permanece en Room y se proporcionan extractos al LLM.

## Límites verificables

- 20 planes pendientes, 5 vigilancias, intervalo de 1 a 720 horas; sin avalancha de ejecuciones perdidas.
- 8 herramientas, 12 inferencias y 8 intentos de ejecución por plan. La reanudación explícita restablece intentos, conservando presupuestos de pasos/modelo y permisos.
- Concesión de 15 minutos; pausa, cancelación o toma por otro worker invalidan resultados tardíos.
- `SUCCEEDED`: el bucle terminó con evidencia no parcial y pasó la revisión del modelo sobre sus extractos. No equivale a veracidad garantizada. `PARTIAL`: evidencia/revisión incompleta o presupuesto agotado. `BLOCKED`: modelo/capacidad/reintentos no disponibles. El detalle se conserva.
- Las estadísticas son éxitos, errores y latencia de recibos efectivamente guardados. Orientan decisiones posteriores; no cambian pesos, permisos ni permiten autopublicar parches.
- `web.search` usa exactamente el objetivo autorizado. Las URLs deben proceder del objetivo o de enlaces públicos observados; los recuerdos privados no autorizan destinos. Los planes con código remoto no consultan memoria personal.
- La lectura y las APIs públicas usan HTTPS GET. Navegador/Python requieren el [servicio opcional](../scripts/agent_sandbox/README.md). Las APIs con efectos externos o credenciales de otras cuentas requieren otra integración explícita.

## Estado del plan global

| Área | Código disponible | Validación o despliegue pendiente |
|---|---|---|
| Dolphin 8B → 3B → Gemma y modelo visible | Router local anterior, traza por inferencia y contexto adaptado aquí | Descarga, RAM, latencia y alternancia real en el S24 Ultra del usuario. |
| Memoria compartida entre modelos | Room/FTS, procedencia, perfil, revisiones y réplica pCloud anteriores; síntesis separadas aquí | Restauración con la cuenta privada y contraste del primer recuerdo en el teléfono. |
| Planificar, actuar, observar, corregir | Bucle tipado con diario y recuperación aquí | Calidad real de planificación con los pesos del móvil; las pruebas controladas no la demuestran. |
| Iniciativa por eventos | Vigilancias explícitas, apertura, carga y temporizador aquí | Comportamiento de Doze/batería en dispositivo real. |
| Navegación y APIs públicas | Lector local y seguimiento de enlaces; puente opcional con búsqueda general | Desplegar dominio HTTPS/servicio y, si se desea, aportar una clave propia de búsqueda. |
| Código aislado y HTML dinámico | Contenedores sin red y renderizado de JS incrustado; pruebas reales en CI | Instalar/configurar servicio propio; no hay navegador general autenticado ni sesiones arbitrarias. |
| Mejora de decisiones | Contadores observados, corrección acotada, laboratorio y flujo de propuestas previos | Aprendizaje general, ajuste de pesos y autopublicación de cambios no implementados por este bucle. |
| Multimodalidad y sensores | Flujos existentes de foto/voz/sensores; planes no suplantan identificación | Cámara, voz continua y capacidades del modelo real se comprueban en el dispositivo. |

No se declara terminado todo el programa de evolución: quedan la puesta en servicio privada, la aceptación física y capacidades avanzadas como recuperación vectorial híbrida y adaptación general validada. Los resultados del laboratorio de cuatro familias de algoritmos no se extrapolan a navegación, memoria autobiográfica o inteligencia general.

## Pruebas

`AgentContractTest`: contratos, permisos, comandos, URLs y diarios corruptos. `RoomAgentStoreTest`: ejecución con modelo controlado, revisión/corrección, presupuesto de contexto, reinicio, cancelación, concesiones, atomicidad del outbox, estadísticas, eventos y límites. `MemoryMigrationTest`: versiones históricas 4/5/6/7 a 8 y conservación del índice. El modelo controlado permite probar el motor sin inventar una medición de Dolphin.

CI compila el APK ARM64, ejecuta la suite JVM y los controles existentes de firma/alineación nativa. Un trabajo separado ejecuta Python y Chromium reales dentro de Docker; sus logs muestran disponibilidad efectiva en el runner. Esto no configura automáticamente el servicio en el móvil.

Referencia de planificación Android: [WorkManager y ejecución diferida](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work).
