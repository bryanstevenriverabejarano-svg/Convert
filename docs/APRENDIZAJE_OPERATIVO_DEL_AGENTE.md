# Aprendizaje operativo del agente

Salve usa ahora los resultados de ejecuciones anteriores para orientar sus siguientes planes. Es memoria operativa finita y verificable: no entrena los pesos del modelo, no prueba inteligencia general y no convierte una respuesta obtenida en un hecho verdadero.

## Experiencia entre planes

Al crear un plan se conserva una instantánea de hasta tres planes terminados pertinentes entre los cincuenta más recientes. La pertinencia usa coincidencia de términos del objetivo, excluye palabras genéricas y prioriza coincidencia y fecha. Solo se usan ejecuciones de los siete días anteriores, con la misma configuración de puente. Se excluyen planes cancelados, pendientes y los que tenían permiso de código remoto.

La instantánea contiene IDs de planes, nombres conocidos de herramientas y cantidades de respuestas completas, parciales, errores y bloqueos. No contiene objetivos antiguos, argumentos, resultados, recuerdos privados ni URLs. Un plan con permiso de código remoto tampoco recibe esta experiencia. Los permisos y las reglas de acceso a memoria/red siguen siendo los mismos.

El contexto que recibe el modelo incorpora un extracto JSON válido, limitado por su capacidad de contexto. Puede omitir herramientas o toda la experiencia si no cabe. El diario completo conserva la instantánea para inspección; no se vuelve a calcular al reintentar un mismo envío. El panel muestra la experiencia **disponible**, sin afirmar que determinó la decisión del modelo.

## Evitar bucles de herramientas

Cada recibo lleva una huella SHA-256 del nombre de la herramienta y su argumento exacto. La calcula el ejecutor local, no la herramienta ni el modelo. No se normalizan mayúsculas, rutas o consultas que podrían cambiar su significado.

- Si la misma llamada ya produjo una respuesta completa o parcial, se indica al modelo que reutilice su ID de evidencia o elija otra acción. No se ejecuta ni registra un éxito adicional.
- Si hubo un error, se permite un reintento. Tras dos errores iguales se pide otro enfoque.
- Si la llamada ya fue bloqueada, repetirla no amplía permisos ni la ejecuta otra vez.
- Los presupuestos de 8 intentos de herramientas y 12 consultas al modelo permanecen. Un modelo que insiste se detiene con resultado parcial.

Las huellas y el contador de repeticiones evitadas sobreviven al reinicio. **Continuar este plan**, tras cancelación o bloqueo, permite reintentar llamadas fallidas si el usuario corrigió la conectividad o configuración. Conserva la evidencia útil, los permisos y los presupuestos originales. No vuelve a ejecutar llamadas útiles ya guardadas.

Los diarios antiguos, sin experiencia o huellas, siguen siendo legibles. No se requiere migración destructiva ni cambio de esquema Room.

## Verificación

Pruebas unitarias: selección por pertinencia y fecha, aislamiento de código remoto, ausencia de contenido privado en el contexto, límites y validación JSON, identidad exacta de solicitudes y compatibilidad con diarios previos.

Pruebas Room: experiencia guardada/reabierta y suministrada a la siguiente inferencia, llamadas útiles reutilizadas sin ejecución ni estadísticas duplicadas, errores con un único reintento, reinicio con llamada pendiente repetida, y reanudación explícita que conserva permisos. La pantalla comprueba la presentación de experiencia y repeticiones evitadas.

Estas pruebas usan modelos controlados para medir el comportamiento del ejecutor. Queda pendiente medir en el S24 Ultra si un modelo real aprovecha el contexto para resolver mejor objetivos nuevos.
