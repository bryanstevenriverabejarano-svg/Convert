# Salve v12 — Núcleo de Sinergia Adaptativa

## Implementación

El núcleo no es energía física ni consciencia. Es un **estado computacional persistente** con cuatro valores normalizados entre 0 y 1: energía de trabajo, curiosidad, incertidumbre y prioridad de aprendizaje. El núcleo puro `SynergyCore` evoluciona con eventos comprobables: respuesta entregada, anomalía de calidad, hallazgo de curiosidad, investigación sin evidencia y aprendizaje efectivamente verificado. Que una respuesta haya sido entregada **no significa que sea verdadera**: únicamente modifica la reserva de trabajo, nunca el contador de aprendizaje verificado.

`SynergyRuntime` almacena el estado en las preferencias privadas de la aplicación y lee indicadores reales de Android sin permisos nuevos: batería, carga, ahorro de energía, aviso de escasez de memoria y estado térmico grave. Ante sensores no disponibles, los ciclos costosos se cierran por defecto. El estado no hace inferencias por su cuenta y no mantiene el LLM encendido continuamente.

## Efectos funcionales

- `MotorConversacional` registra el coste de una conversación y las revisiones de calidad y proporciona una línea de estado al modelo cuando el presupuesto de prompt supera 6000 caracteres. Los contextos comprimidos preservan historial y memoria.
- `CuriosityWorker` puede investigar cuando hay batería y recursos, sin conversación activa ni pausa. `MindRuntime.exploreCuriosity` distingue un hallazgo realmente guardado de una búsqueda sin evidencia.
- `ThinkWorker` y `DreamWorker` no ejecutan trabajos costosos con memoria escasa, sobrecalentamiento, ahorro de batería ni presupuesto bajo; mantienen sus controles de usuario y WorkManager.
- `SynergyRequests` permite preguntar «estado de tu sinergia» o «tu energía cognitiva» para inspeccionar indicadores y estado del teléfono.

La energía se recupera progresivamente entre ciclos y cargando; la curiosidad también se repone con el tiempo. No se usa una barra decorativa para fingir metabolismo biológico.

## Evolución del código

`SynergyEvolution.candidate` analiza los fallos reales de un examen (no texto libre de Internet) y, si se repiten, propone **un módulo concreto de una lista aprobada** y una hipótesis de reparación. Se adjunta a «propuestas de mejora». No produce código ejecutable ni modifica archivos instalados: el paso de fuente requiere un parche explícito validado, aislamiento, pruebas A/B, revisión de diferencias, firma y aprobación para actualizar el APK.

El repositorio ya tiene `scripts/auto_improvement_runner.py`: recibe propuestas de parches completas, las comprueba y prueba en sandbox. El candidato del móvil es **un plan de experimento**, no un archivo listo para el runner. Esto evita que una inferencia o una fuente web se convierta en permiso para modificar el programa.

## Limitaciones y pruebas

El regulador es heurístico, no aprendizaje de parámetros del modelo ni prueba de AGI. `SynergyCoreTest` y `SynergyEvolutionTest` prueban variación, límites, prioridad absoluta de hardware, persistencia conceptual de sus estados y ausencia de activación automática de parches. La efectividad deberá medirse en el Galaxy S24 Ultra (latencia, RAM, batería, porcentaje de respuestas útiles y casos nuevos). Los modelos locales y las funciones de seguridad/identidad no se modifican.

## Próximo experimento

Comparar v11 y v12 en un protocolo A/B controlado con las mismas preguntas y condiciones de batería, y medir las activaciones de curiosidad/autoexamen. Sólo cuando la ganancia esté demostrada debe ampliarse el repertorio de estrategias adaptables.
