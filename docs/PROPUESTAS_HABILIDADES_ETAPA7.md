# Salve etapa 7 — Propuestas de habilidades con prueba y aprobación

## Qué cambia respecto a la síntesis manual

La etapa 6 permitía aprender transformaciones a partir de ejemplos aportados
manualmente. Esta etapa incorpora un **proponente acotado y conectado al runtime
real**: identifica dos necesidades explícitas que tienen un oráculo independiente,
genera ejemplos y un examen reservado, sintetiza un programa, lo evalúa
contra una estrategia básica en entradas nuevas y pide aprobación para guardarlo.

Las necesidades reconocidas inicialmente son **ordenar impares sin repetir**
y **ordenar pares sin repetir**, ambas dentro del repertorio numérico
ya permitido. No se afirma que Salve descubra necesidades generales o nuevas
primitivas. Solicitudes fuera del contrato se rechazan sin inventar datos.

## Flujo exacto y barreras

1. Petición directa del usuario: `propón habilidad: ordenar impares sin repetir`.
   No se interpreta el texto de páginas, memorias ni propuestas del LLM como
   permiso para instalar habilidades.
2. `InductiveSkillProposal` reconoce la necesidad literal y genera seis
   ejemplos de entrenamiento y seis de holdout. Las salidas proceden de
   un **oráculo Java independiente** con `TreeSet` y la paridad de enteros;
   no del modelo ni de la síntesis DSL.
3. `InductiveSkillLab.preview` enumera a lo sumo 1.110 candidatos de la
   gramática registrada. Usa solo entrenamiento para elegir. Después aplica
   una vez el examen reservado y devuelve un programa **sin escribirlo**.
4. Se aplican a la candidata congelada 96 entradas deterministas inéditas
   generadas con semillas diferentes por familia. La comparación se hace
   contra el oráculo externo y contra una referencia básica `sort` sin
   aprendizaje. Para continuar debe pasar 96/96 y mejorar la referencia en
   al menos diez casos. El benchmark **no elige ni reajusta** programas.
5. Se persiste **solo una propuesta pendiente** en almacenamiento privado,
   con UUID, familia, programa propuesto y fecha de caducidad (24 h).
   No se guardan ejemplos, respuestas, archivos privados ni recuerdos en
   el registro pendiente. Todavía no se instala la habilidad.
6. Solo el comando directo `aprueba habilidad UUID` reconstruye los
   ejemplos, repite la comprobación de invariantes y verifica que el
   programa congelado es idéntico. Entonces llama a `learn`, que guarda
   el programa con escritura atómica. `descarta habilidad UUID` elimina
   el pendiente, sin instalarlo.
7. `pausa el laboratorio` o cancelación abortan la evaluación; el sistema
   no concede permisos de aplicaciones, red, shell ni autoprogramación.

## Comandos disponibles
- `propón habilidad: ordenar impares sin repetir`
- `propón habilidad: ordenar pares sin repetir`
- `propuestas de habilidades`
- `aprueba habilidad <UUID exacto mostrado>`
- `descarta habilidad <UUID exacto mostrado>`
- `habilidades aprendidas`
- `usa habilidad: {"name":"auto_odd_sorted","input":[7,3,7,2,1]}`

## Ingeniería y límites comprobables

`InductiveSkillLab.preview` comparte validaciones y criterio de selección
con la promoción, pero **nunca escribe**. `simulate` interpreta
exclusivamente los programas de operaciones del catálogo, sin efectos
laterales. Los programas persistidos antiguos siguen funcionando.

`InductiveSkillProposalTest` y `AutonomousToolRuntimeTest` cubren propuestas
de pares/impares, rechazo de metas desconocidas, evaluación adicional con
96 entradas, comparación con la referencia, no instalación prematura, UUID
incorrecto, persistencia tras reiniciar, aprobación explícita, descarte,
cancelación, pausa, candidato modificado y fallo del archivo final.

**No es un aprendizaje autónomo de cualquier habilidad ni una prueba ciega
externa.** Los tipos de necesidad y la función-oráculo están programados por
los desarrolladores, y los casos son generados con semillas reproducibles.
La ampliación futura será reconocer necesidades a partir de resultados de
planes y proponer ejemplos sin asumir sus etiquetas, manteniendo un proveedor
independiente de verificación y permiso previo a su promoción.
