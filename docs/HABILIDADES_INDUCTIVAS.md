# Laboratorio inductivo de Salve — primera capacidad de síntesis

## Objetivo y alcance

Esta extensión añade un tipo de aprendizaje **distinto del laboratorio de rutas/mochila**. Un usuario puede proporcionar una habilidad NUEVA mediante ejemplos entrada/salida y Salve sintetiza un pequeño programa declarativo **sin conocer de antemano qué combinación de operaciones corresponde a la habilidad**.

NO es programación arbitraria, entrenamiento de pesos, demostración de AGI ni evidencia de generalización fuera del dominio. El vocabulario básico sí está diseñado por desarrolladores: catorce primitivas puras sobre listas de números enteros. Salve descubre combinaciones de longitud 1–3 (**2.954 candidatos máximo**), seleccionadas exclusivamente sobre entrenamiento; tres pasos permiten operaciones que antes no eran representables. Una capacidad fuera de esta gramática se rechaza con NO_CANDIDATE, no se inventa.

## Frontera de aprendizaje y examen reservado

1. Tres a treinta y dos ejemplos de entrenamiento definen la transformación.
2. El sintetizador enumera composiciones deterministas y observa **solo train**.
3. Compara el catálogo finito de 2.954 candidatos **solo sobre los datos de entrenamiento**, cuenta cuántos encajan y selecciona el programa más corto (desempate por orden fijo). Después congela ese programa.
4. Un verificador independiente del sintetizador aplica el programa fijo a 3–32 casos `holdout`, con entradas distintas de train.
5. Si falla uno, el resultado es HOLDOUT_FAILED y **no se reintenta** usando el examen reservado. Si pasa, el estado pasa a CERTIFIED_ON_PROVIDED_HOLDOUT.
6. Solo después de persistir atómicamente se devuelve éxito. Se guarda el programa, nombre, número de casos train/holdout; no se guardan ejemplos ni información autobiográfica.

**Limitación epistemológica importante:** quien envía el JSON proporciona también los casos holdout. El sintetizador no los usa para buscar, pero este conjunto no es un *test independiente ciego* salvo que un evaluador externo lo genere y mantenga secreto. Para investigar generalización real hacen falta datasets nuevos fuera de la app, publicados después de congelar el programa.

## Uso desde conversación

Los comandos solo se aceptan en el canal directo del usuario (no de una respuesta del LLM o de una página).

- `habilidades aprendidas`
- `aprende habilidad: {"name":"odd_dedup","train":[{"in":[3,3,2],"out":[3]} ... mínimo 3], "holdout":[... mínimo 3]}`
- `usa habilidad: {"name":"odd_dedup","input":[3,2,3,5]}`
- `pausa el laboratorio` y `reanuda el laboratorio`, ya existentes, también bloquean este flujo

Ejemplo completo que combina eliminación de duplicados y filtrado de impares:

```text
aprende habilidad: {"name":"odd_dedup","train":[{"in":[3,-2,3,4],"out":[3]},{"in":[-1,2,2,-3],"out":[-1,-3]},{"in":[8,-4,8,1],"out":[1]}],"holdout":[{"in":[5,-7,5,2],"out":[5,-7]},{"in":[-9,6,6,3],"out":[-9,3]},{"in":[0,7,7,-3],"out":[7,-3]}]}
usa habilidad: {"name":"odd_dedup","input":[11,4,11,-2,9]}
```

Resultado esperado de la aplicación: `[11,9]`.

## Propiedades de ingeniería

- Gramática cerrada, sin Java/Dex generado, sin shell, red, archivos arbitrarios o accesibilidad.
- Solo entradas enteras, hasta 24 elementos, magnitud no superior a 10.000, máximo 16 habilidades.
- Persistencia privada separada del laboratorio clásico: `noBackupFilesDir/autonomy/inductive-skills.json`, a través de `FileToolStateStore` con escritura atómica.
- La carga valida estrictamente nombres, programas y certificado para impedir la ejecución de contenido corrupto.
- Nombres inmutables: no se sustituye una habilidad anterior silenciosamente.
- La pausa, cancelación y errores de almacenamiento impiden certificar.
- Se conserva el laboratorio simbólico anterior y no se modifica `ToolRegistry` para declarar capacidades generales inexistentes.

## Pruebas y siguiente etapa

`InductiveSkillLabTest` preserva los contratos anteriores. `InductiveThreeStageGeneralizationTest` incluye una transformación de **tres operaciones** (ordenar + eliminar repetidos + filtrar impares), verificación de 96 entradas inéditas posteriores a la certificación, revisión negativa de holdout, cancelación, fallo de persistencia y compatibilidad con programas antiguos de dos pasos. `AutonomousToolRuntimeTest` confirma el flujo real desde `aprende habilidad:` hasta `usa habilidad:` tras reiniciar.

Esta versión ejecuta en la batería de pruebas JVM una evaluación adicional de entradas generadas con semilla fija, separadas del entrenamiento y del holdout enviado por el usuario. **No es una prueba ciega externa del modelo** ni demuestra aprendizaje general entre dominios. La siguiente etapa sería conectar el planificador a propuestas de habilidades con ejemplos verificables y checkpoint de aprobación antes de la promoción, comparando con/sin habilidad sobre distribuciones nuevas. Evitar convertir simples instrucciones o coincidencias de texto en una afirmación de aprendizaje.

## Integración posterior al PR #307: rotación de secuencias sin duplicar acumulados

La auditoría de `main` confirmó que el PR #307 ya añadió `sum`, `count` y
`prefix_sum`: **se reutilizan y no se redefinen**. Este cambio incorpora
solo `rotate_left`, una nueva transformación de secuencia que rota una
posición sin perder el orden ni la longitud. Entradas `[]` y `[7]`
se conservan sin cambios; una entrada `[1,2,3]` produce `[2,3,1]`.

La gramática contiene ahora **14 operaciones y 2.954 programas** de
longitud máxima 3, enumerados exclusivamente sobre ejemplos de entrenamiento
y sujetos a cancelación. El nuevo comando directo
`propón habilidad: rotar lista a la izquierda` reutiliza el generador
de ejemplos, oráculo independiente de 96 casos, aprobación explícita por
UUID, persistencia atómica `autonomy/inductive-skills.json` y la
restauración pCloud con cuarentena y autorización del PR #305.

La operación no da acceso a red, shell, archivos de usuario ni
código ejecutable; no entrena los pesos de Gemma. Se mantiene soporte
de habilidades antiguas como `prefix_sum`, junto a límites de 24
enteros, magnitud máxima 10.000 y 16 habilidades persistidas. La
prueba `InductiveRotationSkillTest` comprueba 96 casos nuevos
posteriores a congelar el programa, aprobación, fallo de escritura,
vacíos, longitudes, rechazo de herramientas desconocidas y copia
íntegra validada con `SkillCloudArchive`.

**Nota de coordinación:** el PR #308, abierto en paralelo,
requiere revisión para evitar que `running_total` replique el
`prefix_sum` ya fusionado en #307. Esta implementación no introduce
ningún alias ni segundo acumulador.
