# Laboratorio inductivo de Salve — primera capacidad de síntesis

## Objetivo y alcance

Esta extensión añade un tipo de aprendizaje **distinto del laboratorio de rutas/mochila**. Un usuario puede proporcionar una habilidad NUEVA mediante ejemplos entrada/salida y Salve sintetiza un pequeño programa declarativo **sin conocer de antemano qué combinación de operaciones corresponde a la habilidad**.

NO es programación arbitraria, entrenamiento de pesos, demostración de AGI ni evidencia de generalización fuera del dominio. El vocabulario básico sí está diseñado por desarrolladores: diez primitivas puras sobre listas de números enteros. Salve descubre combinaciones de longitud 1–2 (110 candidatos máximo). Una capacidad fuera de esta gramática se rechaza con NO_CANDIDATE, no se inventa.

## Frontera de aprendizaje y examen reservado

1. Tres a treinta y dos ejemplos de entrenamiento definen la transformación.
2. El sintetizador enumera composiciones deterministas y observa **solo train**.
3. Elige la primera que coincide con **todos** los casos de entrenamiento; congela el programa.
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

`InductiveSkillLabTest` cubre síntesis de composición, entradas desconocidas, persistencia, examen fallido, separación de ejemplos, rechazo de operaciones no permitidas, cancelación y fallo de almacenamiento.

La siguiente etapa sería añadir un generador de retos independiente en CI con *holdouts* desconocidos para el modelo y tests de generalización entre tareas; después integrar el planificador para detectar una necesidad, solicitar ejemplos verificables, proponer una habilidad y pedir aprobación antes de su promoción. Evitar convertir simples instrucciones o coincidencias de texto en una afirmación de aprendizaje.
