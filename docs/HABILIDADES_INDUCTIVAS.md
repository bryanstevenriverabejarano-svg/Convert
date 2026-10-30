# Laboratorio inductivo de Salve — primera capacidad de síntesis

## Objetivo y alcance

Esta extensión añade un tipo de aprendizaje **distinto del laboratorio de rutas/mochila**. Un usuario puede proporcionar una habilidad NUEVA mediante ejemplos entrada/salida y Salve sintetiza un pequeño programa declarativo **sin conocer de antemano qué combinación de operaciones corresponde a la habilidad**.

NO es programación arbitraria, entrenamiento de pesos, demostración de AGI ni evidencia de generalización fuera del dominio. El vocabulario básico sí está diseñado por desarrolladores: doce primitivas puras sobre listas de números enteros. Salve descubre combinaciones de longitud 1–3 (**1.884 candidatos máximo**), seleccionadas exclusivamente sobre entrenamiento; tres pasos permiten operaciones que antes no eran representables. Una capacidad fuera de esta gramática se rechaza con NO_CANDIDATE, no se inventa.

## Frontera de aprendizaje y examen reservado

1. Tres a treinta y dos ejemplos de entrenamiento definen la transformación.
2. El sintetizador enumera composiciones deterministas y observa **solo train**.
3. Compara el catálogo finito de 1.884 candidatos **solo sobre los datos de entrenamiento**, cuenta cuántos encajan y selecciona el programa más corto (desempate por orden fijo). Después congela ese programa.
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


## Extensión — secuencias numéricas verificadas (2026-10-30)

El mismo laboratorio (sin motor nuevo) incluye ahora dos primitivas acotadas:

- `running_total` produce las **sumas acumuladas** en el mismo orden: `[2,3,-1] → [2,5,4]`.
- `adjacent_delta` produce las **diferencias consecutivas**: `[2,5,4] → [3,-1]`. Una lista con cero o un elemento produce una lista vacía.

La gramática queda en 12 primitivas, composiciones de 1 a 3 etapas
(**1.884** candidatas exactas). Se mantiene la cota original de 24 enteros,
magnitud 10.000, validación de desbordamiento **en cada paso**, 16 habilidades,
mismas reglas de entrada/salida, pausa, cancelación y persistencia atómica.

Los comandos directos añaden `propón habilidad: sumas acumuladas` y
`propón habilidad: diferencias consecutivas`. Se reutilizan íntegramente
`InductiveSkillProposal` y el flujo `aprueba habilidad <ID>`: 6 ejemplos
de entrenamiento, 6 reservados, un examen posterior de 96 casos nuevos,
no se aprueba una propuesta con 10 o menos ventajas sobre línea base ni
se ejecuta ninguna habilidad antes de aprobación explícita. El examen no
entrena el programa.

Los archivos existentes `inductive-skills.json` versión 1 siguen legibles
y el archivo con estas operaciones se valida mediante el mismo decoder
antes de copiarse a la cuarentena de pCloud. No habilita código Python,
red, control de apps, JavaScript ni instrucciones remotas.
