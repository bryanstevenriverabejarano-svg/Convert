# Salve — Etapa 5: verificación del producto final y evaluación con casos inéditos

## Diferencia respecto a la etapa 4

Antes solo se certificaba que se hicieron dos lecturas. Eso no prueba que
las conclusiones fueran correctas. Esta etapa añade un caso **estrecho, pero
con resultado final verificable**: cálculo aritmético de enteros.

Ejemplo de objetivo literal: `Calcula -491 * 37`.

Se incorpora una ruta totalmente **local en Android**:

1. `AgentArithmeticTask.parse(goal)` reconoce únicamente expresiones explícitas
   de suma, resta o multiplicación con dos enteros de valor absoluto <=1.000.000.
   No se ejecutan expresiones arbitrarias, bibliotecas remotas o código generado.
2. `AgentPolicy` permite `math.compute` solo si el argumento de la herramienta
   es **idéntico al objetivo original** y el plan no usa permisos de código.
3. `AndroidAgentTools` utiliza aritmética Java comprobada (long + Math exacto)
   y entrega un recibo `RESULTADO_ARITMETICO=...`, sin enviar nada a la red.
4. `AgentArithmeticVerifier` vuelve a calcular independientemente mediante
   `BigInteger`, valida la huella vinculada a la llamada, el estado de la
   herramienta, la cita de su recibo y **el número de la respuesta final**.
   El valor numérico no puede ir acompañado de texto arbitrario.
5. `AgentEngine.review` ya no da `SUCCEEDED` a esos planes si el modelo vota
   «pass» pero la respuesta falla la comprobación externa: se guarda como
   `PARTIAL` y `goalVerification=NOT_VERIFIED`.
6. `RoomAgentStore` guarda el fallo en el diario para diagnóstico, **sin
   promover una respuesta numérica errónea a recuerdo o evento de nube**.
7. `AgentRuntime` identifica la respuesta errónea como borrador rechazado.
8. `AgentExperience` solo cuenta objetivos verificados tras recalcular el
   oráculo. Que una llamada sea exitosa no equivale a resultado correcto.

No se altera el contrato de lectura pública ni la verificación de dos URL.
Los objetivos semánticos abiertos permanecen `UNKNOWN`.

## Evaluación de generalización limitada

- `AgentArithmeticTaskTest`: números con signo y magnitudes nuevas, cobertura
  de suma, resta y multiplicación, rechazo de entrada ambigua, permisos, y
  respuestas que incluyen afirmaciones extra.
- `AgentArithmeticVerifierTest`: pruebas de respuesta incorrecta, huella de
  herramienta cambiada, cita ausente, recibo fabricado y validación del
  resultado. Incluye **96 casos generados con semilla fija** no usados para
  desarrollar las reglas del verificador; se genera además una respuesta
  numérica cambiada en cada caso, que debe ser rechazada.
- `RoomAgentStoreTest`: plan de cálculo completo, resultado correcto, cierre,
  Room y reapertura; plan con respuesta equivocada y revisión positiva del
  modelo que **no** puede obtener `SUCCEEDED`, ni contaminar recuerdos.
- `AgentExperienceTest`: experiencia transferida solo si se mantiene correcto
  el producto final y su recibo.
- Compilación Android/Windows y contrato multilingüe: necesarios antes de
  fusionar esta PR.

## Qué demuestra y qué no

Demuestra que se puede aplicar una **evaluación externa del resultado final**
a una familia acotada de problemas, y que los casos erróneos se detectan
independientemente del lenguaje del modelo. No prueba que el modelo local
haya aprendido a sumar, que haya descubierto un algoritmo nuevo ni que Salve
generalice fuera de suma, resta y multiplicación.

Para evaluar transferencia real de habilidades aún hacen falta problemas de
dominios nuevos, separación estricta entre datos de aprendizaje y pruebas,
criterios de éxito definidos antes de observar las salidas, varias semillas,
medición de falsos positivos, y comparación con una línea base.

El siguiente experimento razonable es reutilizar estos verificadores como
*oráculos de evaluación* de estrategias, manteniendo permisos, memoria e
interfaces desacopladas.
