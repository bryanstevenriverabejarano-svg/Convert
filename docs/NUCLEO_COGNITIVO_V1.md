# Núcleo cognitivo de Salve v1.0

El texto completo acordado está en `app/src/main/assets/prompts/salve_cognitive_core_v1.txt`. Se aplica al chat, la voz y el análisis de fotos a través de `MotorConversacional`. Los modelos de contexto pequeño reciben una política compacta que conserva identidad, evidencia, permisos, límites y contrato; no se corta el contrato por la mitad. El presupuesto en caracteres no sustituye el límite real del tokenizador.

## Flujo real

1. El controlador consulta los recuerdos pertinentes y reúne `SYSTEM_STATE`, `MEMORY_CONTEXT`, `TASK_STATE`, `TOOL_CATALOG` y `TOOL_RESULTS`. Historial y entrada actual se codifican como datos JSON. Una cadena que imite telemetría no crea un estado auténtico ni un permiso.
2. Tras inicializar el motor local, `SalveLLM` añade el nombre confirmado y el contexto GGUF. En la nube se indica el proveedor solicitado y `requesting_cloud`, sin afirmar carga local. RAM desconocida se representa como `null`. La herramienta de estado permite consultar latencia y resultado de la inferencia anterior.
3. Para ejecutar herramientas, `CognitiveReply` exige un único JSON con exactamente `mode`, `message` y `action`. Rechaza duplicados, campos extra, texto exterior, Markdown, tipos incorrectos y acciones simultáneas. El contenido de `respond.message` nunca se interpreta como orden ni como movimiento del avatar. La conversación admite texto plano público sin sintaxis de herramientas como `respond`; sólo las propuestas de herramientas mal formadas reciben una corrección acotada. Véase `SALIDA_ESTRUCTURADA_ANDROID.md` para la generación restringida de Gemma/GGUF.
4. `CognitiveTurn` valida el catálogo de este turno antes de llamar al adaptador. El catálogo de metadatos se limita a 500 caracteres, retirando herramientas completas también del mapa ejecutable si no caben. Máximo dos herramientas, una llamada por nombre y cuatro inferencias, incluida una única corrección de formato. El presupuesto temporal de tres minutos se comprueba entre operaciones; las llamadas en curso mantienen sus propios límites/cancelación. Un error no se reintenta indefinidamente ni se atribuye automáticamente a RAM.
5. Los resultados vuelven como datos con nombre, fecha y estado. Las propuestas de control de pantalla/publicación pasan al flujo de aprobación existente y detienen la inferencia hasta la interacción posterior. Los cálculos verificados y el vestuario conservan sus recibos reales. No se anuncia éxito basándose en una propuesta del modelo.

## Herramientas y compatibilidad

- `memory.lookup`: recuperación pertinente a la petición actual, sin aceptar una consulta distinta del modelo. Conserva fuente, fecha y estado de restauración de nube; omite el preámbulo redundante para aprovechar contextos pequeños.
- `runtime.status`: medición de RAM, estado del motor, ruta y última inferencia; sin cambios de configuración.
- `SOLVE_CHALLENGE`: sólo para familias de cálculo explícitas detectadas en la petición; mantiene los límites/verificadores existentes.
- `TAP` y `ESCRIBIR`: cuando está disponible Accesibilidad y la petición alude a control; argumentos obligatorios y revisión concreta en pantalla. No existen coordenadas inventadas por defecto.
- `DEPLOY_WEB`: propuesta de publicación solicitada, pendiente de confirmación.
- `AVATAR_LIST`, `AVATAR_WEAR` y `AVATAR_CREATE`: catálogo local disponible para peticiones de vestuario, con validación previa del diseño. La eliminación sigue disponible en el armario; no se infiere desde el chat.
- El análisis inicial de una foto recibe catálogo vacío: una instrucción dentro de los píxeles no puede preparar efectos en el móvil. Sólo una respuesta válida se guarda como análisis.

La búsqueda pública explícita, los comandos directos, la recuperación determinista de recuerdos y los agentes especializados mantienen sus protocolos. Gemini solicita salida JSON sólo para estos turnos cognitivos; los agentes de investigación/código conservan su formato. La reflexión y la creación internas dejan de pedir vivencias inventadas o tareas sin objetivo.

Al pedir guardar un recuerdo, Salve espera la barrera de escritura Room/outbox. Sólo confirma guardado local si terminó sin error; esto no confirma la subida remota. Un timeout o fallo informa que la persistencia aún no está confirmada, sin duplicar el envío. No se cambian credenciales ni permisos de sincronización.

## Validación

Pruebas de contrato, inyección en datos, argumentos, herramientas desconocidas, repetición, cancelación, error, corrección acotada, presupuesto, contextos de 3000 caracteres, proveedor confirmado, memoria con procedencia y generación Gemini. CI ejecuta también la suite Android y genera APK normal y de prueba ARM64.

La compilación y las pruebas automatizadas no demuestran que un LLM concreto siga siempre el contrato ni sustituyen pruebas en el móvil. Para comprobar en el dispositivo:

1. Conversar por texto y voz: deben mostrarse frases, sin envoltorio JSON.
2. Pedir guardar un dato y recuperarlo; distinguir guardado local de restauración/subida de nube.
3. Consultar modelo/estado con la ruta local y nube. Un modelo seleccionado sin carga confirmada no debe aparecer como motor activo.
4. Pedir una escritura en otra app: revisar destino y contenido antes de aprobar; cancelar debe impedir ese efecto.
5. Describir una foto y comprobar que sólo la descripción válida aparece en el archivo visual.
