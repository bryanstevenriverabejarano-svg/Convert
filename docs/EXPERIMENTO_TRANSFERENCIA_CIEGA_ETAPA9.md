# Salve — Etapa 9: aprendizaje inductivo con reintentos y transferencia reservada

## Hipótesis falsable

**Pregunta:** ¿Puede Salve aprender una transformación no declarada por nombre, a partir de unos pocos ejemplos de entrada/salida, aplicarla correctamente a entradas nuevas de otra distribución y distinguir los fracasos de los éxitos?

Esta etapa implementa el primer protocolo ejecutable y acotado. No prueba AGI,
aprendizaje general entre dominios ni capacidad de inventar nuevas operaciones
primitivas. El lenguaje de hipótesis contiene únicamente las 10 operaciones de
`InductiveSkillLab`, hasta tres pasos (1.110 candidatos). Los desafíos
se eligen dinámicamente entre siete familias de transformación de listas;
los ejemplos y valores se generan al ejecutar, no se entregan como comandos
al modelo.

## Diferenciar entrenamiento de examen

**Tres particiones estrictas:**

1. **Train:** 6 demostraciones iniciales creadas por un **oráculo Java** que no
   llama al intérprete DSL. El sintetizador recibe solo esos pares.
2. **Development:** 6 pares nuevos, distintos de train. `preview()`
   comprueba la candidata congelada. Si falla, se registran diagnósticos de
   estado `NO_CANDIDATE` o `HOLDOUT_FAILED` y se añaden otras 6
   demostraciones **nuevas**, no las respuestas del examen de development,
   para un intento adicional. Cada intento usa un lote nuevo de development.
   Máximo **4 intentos** (24 ejemplos de train). No hay bucle infinito.
3. **Final:** tras una certificación de development se congela el programa.
   Después se generan **96 entradas nuevas** de longitud 12–24 y valores
   más amplios que los de train/development (listas de longitud 4–9).
   Se comparan con la salida del oráculo Java **una única vez**. La variante
   de referencia simplemente devuelve los datos de entrada sin cambiar.
   Para `TRANSFER_VERIFIED` se exigen **96/96**, con al menos 12 aciertos
   más que la referencia.

**Si el examen final falla, el intento queda marcado `TRANSFER_FAILED` y
no se reutiliza el mismo examen para entrenar otra candidata.** Un nuevo
experimento elige otro objetivo/semilla y construye otro examen. Esto evita
la falsa mejora por memorización de la evaluación.

El examen es final respecto al *procedimiento de síntesis*, pero quien puede
leer el repositorio puede ver las familias de oráculos y semillas de las
pruebas deterministas. **No equivale a un benchmark externo secreto**. Un
evaluador fuera de la app puede usar `examine(Oracle, Sampler, seed, cancel)`
para entregar otras tareas sin identificar su transformación al sintetizador.

## Qué cuenta como descubrimiento

El sintetizador **no recibe** el nombre ni el programa del oráculo, únicamente
las parejas entrenamiento/desarrollo. La familia del reto se escoge en el
runtime mediante `SecureRandom`; el oráculo se ejecuta por el controlador
y es independiente de `InductiveSkillLab.simulate`.

Siete familias de oráculos cubren combinaciones de ordenar, eliminar duplicados,
aplicar valor absoluto, cambiar signo, cuadrar, filtrar pares/impares,
invertir y sumar/restar uno. Las familias están **implementadas por el
desarrollador**, aunque no se le comuniquen al sintetizador durante el reto.
La generalización evaluada es **entre distribuciones de listas numéricas**,
no entre modalidades ni campos semánticos arbitrarios.

El código registra cantidad de intentos, fallos de development, aciertos en
examen final y referencia, programa congelado y estado honesto. El informe
incluye `skill_installed=false` y
`final_exam_reused_for_training=false`.
El experimento **no puede** instalar habilidades, ampliar permisos, usar
red, ejecutar shell, controlar WhatsApp, editar código o guardar recuerdos.

## Uso en el teléfono

El comando explícito del usuario es:

`experimento de aprendizaje`

Se ejecuta una sola prueba aleatoria y se muestra el resultado. `pausa el
laboratorio` y cancelar el turno detienen el experimento. No funciona como
tarea silenciosa de vigilancia ni queda activo indefinidamente. Para
incorporar un programa descubierto, deberá existir en el futuro un
proceso de aprobación independiente, similar al de la etapa 7.

## Pruebas

- `BlindSkillTransferExperimentTest` ejecuta familias distintas, entradas
  nuevas más largas y magnitudes diferentes. Incluye **una transformación
  externa no enumerada entre las siete familias**, cuyo oráculo ordena
  operaciones por entrada/salida sin revelar el algoritmo a síntesis.
- Un muestreador de desarrollo con demostraciones iniciales insuficientes
  obliga a **reintentar** usando ejemplos nuevos, hasta cuatro veces.
- Un oráculo adversarial responde con identidad durante desarrollo y cambia
  ante entradas finales más largas. El sistema debe marcar
  `TRANSFER_FAILED` con **un solo examen final y cero reintentos tras ese
  examen**.
- Otro oráculo demanda una transformación imposible de obtener en el
  catálogo. Se espera `DEVELOPMENT_FAILED` después del presupuesto.
- Cancelación y comando de conversación real `AutonomousToolRuntimeTest`:
  sin persistencia ni promociones silenciosas.

La CI Android/Windows/memoria es un paso necesario. Incluso una CI verde no
sustituye probar la inferencia local de Dolphin/Gemma en el Samsung ni una
comparación con retos genuinamente exteriores al repositorio.

## Próximo estudio después de esta etapa

Separar el verificador en un *evaluador externo* con tareas creadas
posteriormente a congelar el programa, de forma que Salve no pueda conocer
ni la familia ni las etiquetas de la prueba. Registrar comparativas con/
sin aprendizaje en dominios distintos, latencia, errores, consumos, falsos
positivos y regresiones; solo promocionar habilidades tras aprobación del
usuario y evidencia repetida.
