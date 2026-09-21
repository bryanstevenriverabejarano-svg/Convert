# Presupuesto personal dentro de la conversación

La intención corregida es recordar sueldo y gastos, actualizar esos datos y calcular un reparto mensual hablando con Salve. La calculadora de negocio de la PR anterior conserva su función opcional; no hace falta abrirla para este presupuesto personal.

## Ejemplo ficticio reproducible

1. «Mi sueldo es 1800 EUR netos al mes».
2. «Pago 650 EUR de alquiler al mes».
3. «Gasto 200 EUR en comida al mes».
4. «Quiero ahorrar el 20%».
5. «¿Cuánto puedo gastar?».

El cálculo usa ingresos de 1800, gastos declarados de 850 y objetivo de ahorro de 360. La diferencia después de gastos y reserva es 590 EUR. **Mientras no se hayan confirmado todos los gastos es provisional; no es autorización para gastar 590 ni el saldo bancario disponible.**

«Esos son todos mis gastos» marca la lista completa según el usuario. «Pago 700 EUR de alquiler al mes» sustituye el alquiler anterior, sin sumar otra copia. «Qué gastos tengo» muestra hasta diez nombres y el total. «Quiero ahorrar el 10%» modifica el objetivo. «Olvida mi sueldo», «borra el gasto alquiler» y «borra mi presupuesto» eliminan datos concretos o el conjunto personal.

Un sueldo incompleto, por ejemplo «cobro 1800», no se guarda aún: se pregunta divisa, frecuencia y si es neto. Se puede responder «EUR netos al mes». Después de establecer moneda, un gasto puede heredarla; si falta frecuencia se pregunta antes de guardarlo. Cambiar de tema cancela la aclaración pendiente. La entrada admite coma decimal y rechaza agrupaciones ambiguas como `1.800` o `1,800`; escribir `1800` evita confundir miles con decimales.

## Método de ahorro

Se usa el 50/30/20 como comparación: 50% de ingreso neto para necesidades, 30% para deseos y 20% para ahorro. Para 1800, las referencias son 900, 540 y 360. No se alteran los gastos reales para encajarlos en esos porcentajes y el objetivo de ahorro puede cambiarse. [Guía educativa del CFPB](https://files.consumerfinance.gov/f/documents/cfpb_building_block_activities_learning-about-budgets_guide.pdf).

El ahorro factible se limita al menor entre la meta y el excedente no negativo. Si los gastos superan los ingresos se muestra el déficit y no se propone una disponibilidad ficticia. El cálculo mensual no modela fechas de cobro, vencimientos, saldo inicial ni movimientos bancarios; no es un gestor de caja diaria.

## Arquitectura e integración

- `PersonalBudget`: estado inmutable, moneda única, sueldo neto mensual opcional, gastos recurrentes por nombre y categoría, objetivo y confirmación de completitud. `BigDecimal` evita errores binarios en dinero. Actualizaciones sustituyen importes; añadir o eliminar gastos vuelve a dejar la lista incompleta.
- `PersonalBudgetConversation`: reconoce declaraciones y preguntas españolas acotadas, pide aclaraciones, propone cambios y genera respuestas basadas en los cálculos. No llama al LLM. Los ejemplos, preguntas, negaciones, datos de terceros y de empresa no modifican el presupuesto personal.
- `PersonalBudgetStore` y `PersonalBudgetService`: leen, interpretan y guardan en serie. Escritura mediante archivo temporal y sustitución atómica; la confirmación llega después del guardado. Un archivo corrupto se conserva, no se reemplaza silenciosamente por un presupuesto vacío. El borrado explícito también elimina el temporal pendiente.
- `FinanceConversationPolicy`: distingue la ruta personal de las consultas generales. Añade contexto financiero breve al prompt real de ambos proveedores, sin insertar el presupuesto personal.
- `MainActivity` y `MotorConversacional`: capturan la ruta privada antes de la sincronización y del historial general, incluyendo entradas incompletas reconocidas. El texto de respuesta llega a pantalla aunque no se añada al historial del LLM.

Se eligió extracción determinista para esta primera versión porque permite probar exactamente qué se guarda. La alternativa de extracción con LLM admite más formas de expresarse, pero exige validar un esquema y gestionar incertidumbre antes de modificar datos. No se ha añadido esa alternativa ni se promete comprensión arbitraria de cualquier frase.

## Persona y empresa

El presupuesto guardado es **personal**. Preguntas generales sobre administración, margen, liquidez, costes, caja y capital de trabajo siguen llegando al modelo configurado. El contexto distingue beneficio de efectivo, exige moneda/periodo y supuestos y pide país y ejercicio para normativa fiscal. No guarda facturación de empresa como sueldo ni mezcla ambos patrimonios.

Estas instrucciones no equivalen a entrenar un especialista financiero ni a comprobar todos sus consejos. No hay contabilidad empresarial persistente, conexión bancaria, conciliación, presentación de impuestos ni consulta automática de cotizaciones nueva en este cambio. Para información cambiante se pide una fuente oficial realmente consultada y reconocer la falta de verificación.

## Datos y voz

El archivo se guarda en `Context.getNoBackupFilesDir()/finance/personal-budget.json`. La ruta de presupuesto reconocida no añade mensajes a la memoria general, historial del modelo, eventos de nube ni subida del grafo. Las respuestas sólo se sintetizan si Android informa que la voz seleccionada funciona sin red; de lo contrario quedan visibles en pantalla.

Esto no garantiza que todo texto financiero posible sea detectado: el clasificador tiene cobertura acotada. Tampoco cambia el reconocimiento de voz Android, que puede usar un servicio remoto antes de que Salve reciba la transcripción. No se afirma que el reconocimiento sea local ni que el archivo sea cifrado; usa el almacenamiento privado de la aplicación, excluido del respaldo automático. El borrado no elimina datos que se hayan compartido anteriormente por otras rutas.

## Alcance inicial y validación

La entrada conversacional reconoce EUR, USD y COP y exige importes mensuales recurrentes. No convierte monedas ni ingresos anuales o semanales; los gastos puntuales necesitan un libro de movimientos separado. Las categorías reconocidas se indican al guardar; si no se puede clasificar un gasto, se pregunta. No se inventan los gastos que faltan.

Las pruebas cubren persistencia entre instancias, actualización sin duplicados, pérdida de escritura, JSON corrupto, borrado, ingresos ausentes/cero, déficit, objetivos, importes ambiguos, hipótesis, aclaraciones y aislamiento de empresa/persona. Los tests de lenguaje verifican la cobertura definida; no miden calidad de un LLM real. La revisión de la integración Android es de código, sin prueba instrumentada ni física en S24 ni APK nuevo.

Validación de este grupo: **70 pruebas JVM superadas** entre presupuesto, diálogo, persistencia, clasificación y regresiones de la calculadora empresarial. Las clases financieras se compilaron para Java 11; se comprobó la sintaxis de los archivos Java modificados. No se ejecutó compilación Gradle integral ni pruebas Android instrumentadas.
