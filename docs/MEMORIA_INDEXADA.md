# Segunda entrega: recuperación local y procedencia de la memoria

La conversación y los objetivos consultan el mismo almacén Room, independientemente del modelo. Esta entrega sustituye la selección conversacional mediante `LIKE` por un índice FTS4 `unicode61`, una expansión de vocabulario acotada y la recuperación de asociaciones del grafo cuando la pregunta no pide historia personal. No descarga un modelo nuevo, no llama a una API de embeddings y no cambia la selección Dolphin 8B → Dolphin 3B → Gemma.

## Comportamiento

| Petición | Cambio |
|---|---|
| `¿Recuerdas el proyecto Orion?` | Puede recuperar `proyecto ORIÓN`, sin exigir el mismo acento o mayúsculas |
| `Proyecto telescopio presupuesto` | Consulta primero el tema completo; un recuerdo antiguo puede aparecer aunque haya muchos registros recientes de cada palabra por separado |
| `¿Qué te dije de mi móvil Samsung?` | También busca `teléfono` o `smartphone`; conserva el texto original y señala que se amplió el vocabulario |
| `Tu primer recuerdo conmigo` / `tu último recuerdo conmigo` | Excluye configuración e investigaciones públicas de ambos extremos de la cronología |
| `¿Dónde vivo?`, después de corregir la residencia | Lee el perfil vigente; el dato sustituido desaparece también del índice |
| Consulta tras olvidar un perfil y restaurar una copia antigua | El índice respeta el borrado y la revisión que ya mantiene el importador |

No hacen falta comandos nuevos. La recuperación de pCloud sigue dependiendo de la sincronización existente. Una restauración incompleta continúa mostrándose como tal: el registro más antiguo disponible no se presenta como el primer acontecimiento de toda la historia.

## Selección y presupuesto

`MemorySearchQuery` limita la entrada a 4096 caracteres, cuatro términos útiles y palabras de hasta 64 caracteres. Conserva nombres de tres letras. Construye expresiones con palabras citadas, sin copiar operadores FTS o SQL del usuario. Se elimina el nombre fijo Bryan de la lista de palabras descartadas.

`MemorySearchService` hace como máximo seis consultas con límites: coincidencias del tema completo, tema ampliado si procede y oportunidades por término. Evalúa como máximo 56 candidatos y devuelve cuatro. Ordena por cobertura de conceptos de búsqueda, cobertura literal, fecha e identificador. La normalización se calcula una vez por candidato. Cada texto se examina hasta 64 000 caracteres; no carga toda la tabla en Java.

El vocabulario ampliado es una lista explícita y pequeña: móvil/teléfono, ordenador/computadora, trabajo/empleo, coche/auto, foto/fotografía y vestuario/vestido/atuendo. Sirve para recuperar candidatos; no demuestra equivalencias semánticas universales ni autoriza a cambiar lo declarado. No es búsqueda vectorial. Se conserva el grafo de un salto para temas generales; las consultas personales explícitas no usan sus síntesis como evidencia biográfica.

Los fragmentos son ventanas literales de hasta 620 caracteres cercanas a los términos buscados, con `…` si se omitió contenido. El contexto de memoria conserva su presupuesto de 3200 caracteres; puede incluir menos de cuatro registros. No se corta un registro JSON por la mitad. No se modifica el constructor compartido del prompt ni se crea una memoria diferente para cada proveedor.

## Procedencia y temporalidad

Cada registro muestra identificador, fecha original de registro, ubicación de almacenamiento/restauración y naturaleza: declaración del usuario, investigación pública, configuración o autoría no confirmada. Restaurarse desde pCloud no convierte un texto en una declaración del usuario.

Las declaraciones incompatibles continúan siendo registros separados con fechas. El prompt advierte que no se fusionen contradicciones ni se confundan fechas de registro con fechas del suceso. Los perfiles mantienen la política existente de última revisión válida; esta entrega no reconstruye valores históricos ya sustituidos y no añade una cronología bitemporal ni un detector automático de contradicciones.

La exclusión de investigaciones y configuración se aplica antes del límite SQL en consultas de historia personal. Los registros heredados sin autoría comprobable se etiquetan como tales, sin inventar su origen. El primer registro compartido sigue siendo una consulta sobre los datos conservados, no una prueba de vivencias subjetivas.

## Persistencia y actualización

Room pasa de 6 a 7. `recuerdos_fts` utiliza contenido externo: los textos originales permanecen en `recuerdos`. La migración crea y reconstruye el índice con todos los recuerdos existentes, incluidos los restaurados. Room instala los triggers que mantienen el índice al insertar, actualizar, sustituir o borrar; no hay una segunda cola ni caché que pueda resucitar perfiles eliminados. La construcción inicial del índice puede alargar la primera apertura de una base grande.

Se conservan las rutas aditivas 4 → 5 → 6 → 7 y el rechazo sin borrado de versiones desconocidas. Se prueba 6 → 7 usando el esquema 6 exportado y versionado. El índice es local y derivado: no se sube a pCloud ni se altera el protocolo de eventos existente. El borrado conserva el alcance previo (categoría de perfil y tombstone); no promete eliminar copias de ese dato en cualquier conversación histórica, grafo o backup externo.

## Verificación

Las pruebas Room/SQLite ejecutan migraciones, indexación de registros antiguos, triggers, rollback, restauración y repetición de eventos antiguos, sustitución y eliminación de perfiles, reapertura de la base, ranking con 300 distractores recientes, acentos, nombres cortos, sinónimos, fragmentos largos, procedencia y separación de historia personal. Comprueban que la evidencia llega al constructor compartido con las configuraciones Dolphin 8B, Dolphin 3B y Gemma; no ejecutan los pesos de esos modelos.

Quedan para el teléfono la latencia con tu volumen real de recuerdos, la calidad de la respuesta de cada modelo y la restauración real con tu cuenta pCloud. La siguiente fase del agente puede generalizar los planes persistentes y sus herramientas sobre esta memoria; embeddings y temporalidad más rica requieren una entrega propia y mediciones.

Referencias técnicas: [Room FTS4 y triggers de contenido externo](https://developer.android.com/reference/androidx/room/Fts4), [SQLite FTS3/FTS4, unicode61 y reconstrucción](https://www.sqlite.org/fts3.html).
