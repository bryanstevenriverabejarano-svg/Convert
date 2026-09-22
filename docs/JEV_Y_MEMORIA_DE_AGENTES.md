# Jev, LLM y memoria: investigación y estado real

Investigación del 22 de septiembre de 2026, como complemento del laboratorio verificable. **Jev no está activado en esta revisión:** no hay una clave TypeSafe configurada en el entorno ni se han ejecutado llamadas de inferencia a ese proveedor. Tampoco se ha acreditado sincronización con el servidor de Salve.

## Qué aporta Jev y qué necesita otro componente

Jev recibe estado y preguntas tipadas, y devuelve decisiones estructuradas. Sus primitivas `Choice`, `Score` y `Noul` permiten elegir, puntuar y estimar una respuesta binaria. La documentación recomienda dividir decisiones complejas en preguntas específicas y combinar las respuestas mediante código. Para Salve, su papel posible sería seleccionar una herramienta o detectar la necesidad de aclaración; la conversación y la generación de código seguirían a cargo del LLM. [Introducción oficial](https://docs.typesafe.ai/introduction).

La API publicada usa `POST https://api.typesafe.ai/v1/systemone` con autenticación Bearer y un modelo como `jev-latest`. Esto describe un servicio remoto, no un archivo de pesos que se haya descargado o instalado en el Galaxy. Activarlo requiere credenciales por instalación o un backend propio; no se debe insertar una clave de servicio compartida en el APK. [Inicio rápido oficial](https://docs.typesafe.ai/introduction/quickstart).

`confidence` resume la distribución de probabilidades para Choice y Score; Noul no devuelve ese campo. Los umbrales necesitan evaluarse con datos del caso de uso. Una confianza alta no sustituye comprobar una suma, una ruta o la ejecución efectiva de una herramienta. [Documentación de confianza](https://docs.typesafe.ai/confidence).

El anuncio de TypeSafe publica comparaciones de velocidad y coste y describe limitaciones de esas comparaciones. Su garantía de forma de salida no establece que toda decisión sea correcta. No trasladamos sus cifras a Salve ni usamos «no alucina» como certificación semántica. [Anuncio y metodología del proveedor](https://typesafe.ai/blog/introducing-system-one-models-and-jev).

## Arquitectura propuesta para una prueba real

| Componente | Responsabilidad | Evidencia exigida |
|---|---|---|
| Recuperación local | Seleccionar recuerdos con procedencia y experiencias procedurales pertinentes | Pruebas de relevancia, contradicción, borrado y ausencia de datos innecesarios |
| LLM de Salve | Interpretar la petición, solicitar datos que falten y proponer un plan estructurado | Respuestas reales del modelo con su versión y latencia registradas |
| Jev opcional | Puntuar opciones cerradas, detectar ambigüedad o priorizar candidatos | Comparación con el enrutador local sobre idénticos inputs; errores y coste incluidos |
| Ejecutor | Ejecutar sólo capacidades disponibles, con presupuesto y cancelación | Resultado observado y registro de efectos |
| Verificador | Comprobar el resultado con código, tests o evidencia externa | Criterio independiente del modelo que produjo la respuesta |
| Memoria de experiencias | Conservar resultados y condiciones de uso, incluyendo fallos útiles | Recuperación en otra sesión y revalidación ante nuevos datos |
| Sincronización | Copiar aprendizajes autorizados a la nube configurada | Acuse verificable, idempotencia, reintento y borrado propagado |

No es necesario cargar varios LLM grandes a la vez para separar estas responsabilidades. Se puede usar un mismo modelo por turnos y reservar modelos especializados para tareas donde una comparación demuestre mejora. Esta es una decisión de diseño para el teléfono, no un resultado de rendimiento ya medido.

Antes de promover una combinación de modelos, comparar cuatro condiciones con las mismas peticiones: LLM solo, LLM con memoria seleccionada, LLM con decisor y LLM con ambos. Medir éxito real, errores de interpretación, peticiones de aclaración, latencia p50/p95, consumo y coste. Mantener un conjunto reservado y registrar los fallos. Los tests del laboratorio de cuatro familias no sustituyen esta evaluación semántica.

## Aprender de experiencias sin convertir hipótesis en hechos

ReasoningBank estudia memoria derivada tanto de éxitos como de fallos y su recuperación en tareas posteriores. Es una referencia útil para diseñar experiencias con alcance y procedencia. Sus evaluaciones y resultados pertenecen a sus modelos y benchmarks; no acreditan capacidades de Salve. No hemos importado ese framework ni almacenado razonamientos privados. [Investigación de Google](https://research.google/blog/reasoningbank-enabling-agents-to-learn-from-experience/).

Este PR implementa un paso más pequeño y comprobable: conservar programas y recibos del laboratorio, recuperar metadatos de la familia pertinente y volver a ejecutar los verificadores con datos nuevos. La comparación con/sin memoria usa el mismo Java que Android; mide reutilización de estrategias, no entrenamiento neuronal ni mejora del LLM.

Para recuerdos generales, conservar por separado declaración del usuario, observación, hipótesis y resultado verificado. La memoria procedural del laboratorio no recibe el salario, ubicación, transcripción completa ni el primer recuerdo personal. Sus entradas de regresión numéricas permanecen en el diario local y no se incluyen en el contexto procedural ni en una subida automática de todo el historial.

## Nube: bloqueo encontrado

`CloudSyncManager` conserva un endpoint fijo y una credencial de ejemplo, ahora bloqueada antes de enviar. `CloudLogger` delega en su cola local duradera; los eventos fallidos ya no se purgan automáticamente. Existe una cola Room para ciertos eventos y un ajuste de consentimiento, pero eso no demuestra que el servidor acepte y conserve los aprendizajes. Este PR no marca como enviado ningún dato ni cambia el ajuste silenciosamente.

Para una sincronización real faltan configuración autenticada por instalación, eventos con identificadores idempotentes, acuses validados y política de retención/borrado. Los recibos compartibles deben contener versión del programa, resultado de validación y alcance; los inputs privados requieren una decisión separada de almacenamiento. La autorización de Bryan permite trabajar en esa integración, pero no proporciona una credencial de servidor ni prueba su funcionamiento.

Hasta disponer de esa conexión y un proveedor real para la comparación, el estado correcto es: **herramientas locales verificadas y memoria procedural implementadas; combinación LLM–Jev y sincronización autenticada pendientes de validación**.
