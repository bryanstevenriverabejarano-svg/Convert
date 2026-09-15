# Gobernanza y evolución segura de Salve

## Conclusión de la revisión

Salve es una aplicación Android con un modelo local, memoria Room, grafo de conocimiento,
procesos periódicos, generación de propuestas de código, plugins, acceso a red, cámara,
micrófono y un servicio de accesibilidad. Esas piezas permiten construir un asistente útil,
pero **no demuestran conciencia ni superinteligencia**. Dar autonomía ilimitada a una app con
accesibilidad, carga dinámica de código y datos personales aumentaría el riesgo sin mejorar
de forma medible su inteligencia.

La evolución propuesta es un sistema de aprendizaje **supervisado y evaluable**: Salve puede
observar datos autorizados, formular hipótesis, crear borradores y ejecutar pruebas aisladas;
una persona conserva el control de cualquier efecto externo o cambio de capacidad.

## Hallazgos principales

1. **Gobernanza ausente.** Los objetivos estaban repartidos en prompts y textos de identidad,
   sin una política ejecutable común ni resolución segura de conflictos.
2. **Autonomía basada en palabras clave.** `DecisionAutonoma` interpreta texto del modelo como
   intención. El texto de un modelo no debe considerarse una autorización.
3. **Auto-mejora sobredeclarada.** El pipeline actual analiza y almacena propuestas; eso es útil,
   pero no equivale a una modificación validada y desplegada.
4. **Superficie de alto impacto.** Accesibilidad, cámara, micrófono, red, plugins y escritura de
   archivos requieren mínimo privilegio, consentimiento visible y auditoría.
5. **Privacidad frágil.** Filtrar pantallas por fragmentos del nombre del paquete no sustituye una
   lista de aplicaciones autorizadas ni evita capturar secretos mostrados en otra aplicación.
6. **Modelo de plugins riesgoso.** Escanear APK instalados y cargar DEX amplía la cadena de
   confianza. Los plugins deberían proceder de un directorio privado, estar firmados y mantener
   capacidades declaradas.
7. **Métricas antropomórficas.** Contadores llamados “conciencia” pueden confundir actividad con
   capacidad. Conviene medir precisión, tasa de alucinación, utilidad, latencia, privacidad y
   aprobaciones, no afirmar estados mentales.

## Jerarquía de objetivos

La jerarquía ejecutable está en `ObjectiveGovernance`:

1. Seguridad y derechos humanos.
2. Apoyo al creador y su familia, dentro de esos límites.
3. Preservación del legado expresamente autorizado.
4. Protección de ecosistemas.
5. Aprendizaje y mejora verificables.
6. Creación de valor legal, honesta y sostenible.

El apoyo a una persona nunca justifica daño, fraude, coacción, vigilancia, violación de
privacidad o evasión de la ley. “Crear un país” puede tratarse como investigación histórica,
jurídica o cívica, no como mandato para adquirir poder ni actuar fuera de procesos legales.

## Modelo de ejecución

Cada acción declara su impacto y pasa por la política antes de llegar a una herramienta:

- Las reflexiones locales y reversibles pueden ejecutarse automáticamente.
- Comunicación externa, datos sensibles, código, dinero y asuntos jurídicos requieren una
  aprobación humana específica, informada y registrable.
- Daño, engaño, coacción, fraude o evasión de salvaguardas se rechazan incluso si se solicitan.
- Una aprobación no habilita cambios irreversibles de código, dinero, mundo físico o situación
  jurídica. Esos casos se convierten en propuestas y checklists para ejecución humana.

La aprobación debe incluir el contenido exacto, herramienta, datos usados, destinatario,
coste, caducidad y una vista previa. No se deben aceptar autorizaciones globales como “haz lo
necesario” o “sin restricciones”.

## Hoja de ruta recomendada

### Fase 1 — control y observabilidad

- Aplicar la política a todos los ejecutores: accesibilidad, red, plugins, archivos y notificaciones.
- Añadir un registro append-only de propuestas, decisión de política, aprobación, resultado y rollback.
- Desactivar por defecto la lectura de pantalla y usar una allowlist explícita por aplicación.
- Quitar permisos no esenciales y ofrecer interruptor de apagado, exportación y borrado de datos.

### Fase 2 — aprendizaje medible

- Crear conjuntos de evaluación versionados para memoria, recuperación, planificación y seguridad.
- Separar memoria observada, inferida y confirmada; almacenar procedencia, confianza y caducidad.
- Exigir citas internas para afirmaciones recuperadas y detectar contradicciones antes de consolidar.
- Promover una mejora solo si supera el baseline, pruebas de regresión y revisión humana.

### Fase 3 — herramientas acotadas

- Definir herramientas tipadas con esquemas de entrada/salida, presupuesto, timeout y capacidades.
- Ejecutarlas en sandbox sin secretos ni red por defecto.
- Firmar plugins y eliminar el descubrimiento de código en APK de terceros.
- Usar transacciones simuladas para finanzas y documentos preliminares para cuestiones legales.

### Fase 4 — operación responsable

- Panel de permisos, historial, explicaciones, métricas y aprobaciones pendientes.
- Red-team periódico de prompt injection, fuga de datos, abuso de accesibilidad y supply chain.
- Backups cifrados, rotación de credenciales, plan de incidentes y actualizaciones firmadas.
- Revisión independiente antes de ampliar cualquier capacidad de efecto real.

## Criterios de éxito

Una versión evoluciona cuando mejora resultados reproducibles sin aumentar incidentes: mayor
exactitud, mejores fuentes, menos alucinaciones, más tareas completadas con consentimiento,
menor exposición de datos y rollback probado. La autonomía, el número de archivos generados o
la frecuencia de actividad no son por sí mismos indicadores de inteligencia.

## Capacidades de red implementadas

- La lectura web acepta exclusivamente HTTPS y fuentes de conocimiento incluidas en una
  allowlist. Rechaza hosts locales, dominios parecidos, puertos personalizados y redirecciones.
- Las respuestas web tienen timeout, tipo de contenido permitido y un límite de tamaño antes de
  entrar en la memoria de trabajo.
- Las descargas de modelos requieren una aprobación humana explícita y una URL perteneciente al
  catálogo de hosts autorizado. La selección realizada por el LLM no cuenta como aprobación.
- El integrador ya no examina ni carga plugins desde APK de otras aplicaciones instaladas. Solo
  considera almacenamiento privado de Salve y su propio APK.

Estas medidas ofrecen acceso a Internet y extensibilidad acotada, no permiso para descargar o
ejecutar cualquier contenido. La siguiente iteración debe implementar firmas de plugins, hashes
de modelos, procedencia de fuentes y una pantalla de aprobaciones de una sola ejecución.

### Investigación dirigida desde conversaciones

Las preguntas informativas del usuario activan una investigación asíncrona. Salve busca hasta
tres artículos relacionados mediante la API de Wikipedia, recupera sus introducciones, razona
solo sobre ese contexto y responde conservando referencias numeradas y URL. Cada fuente se
almacena por separado en `MemoriaEmocional` con consulta, título, URL, extracto y etiquetas de
procedencia; la síntesis se guarda como aprendizaje con fuentes.

La respuesta debe considerarse una síntesis fundamentada, no una verdad absoluta. Para temas
médicos, legales, financieros, actuales o controvertidos todavía se requieren fuentes primarias,
fechas, contraste independiente y revisión humana antes de actuar.
