# Salve cambia su estructura a petición, con juez protegido y linaje — 10 de noviembre de 2026

## Qué había

Salve ya abría y fusionaba sus PR ([cómo](SALVE_ABRE_SUS_PRS.md)), pero solo podía tocar **cinco archivos**:
los que `SynergyEvolution` asocia a siete tipos de fallo de su examen. Fuera de eso no podía cambiarse.

## Qué cambia

1. **Cambio dirigido.** Bryan dice el objetivo con sus palabras: «abre un PR para que…» o «cambia tu código
   para…». `SelfChangeScope.goal` lo extrae de la frase, `SelfChangeScope.choose` busca en el índice de su
   propio código (`SelfCode`, el mismo de `self.code`) el módulo que mejor encaja, y el resto es el flujo de
   siempre: fragmento → parche BUSCAR/REEMPLAZAR → issue → pruebas → compilación completa → fusión.
   La orden «abre un PR» sin objetivo sigue siendo la del examen.
2. **Alcance: casi todo su núcleo.** Cualquier `.java` de `salve/core`, excepto lo que la mide o la autoriza:
   `evaluation/` (su examen), `selfpr/` y `SelfPrDesk` (la puerta de sus PR), `identity/`, `privatefamily/`,
   `finance/` y `ObjectiveGovernance`. El límite está en la app (`SelfChangeScope.PROTECTED`) y, sobre todo,
   en el servidor (`PROTECTED_CORE` en `scripts/auto_improvement_runner.py`), que es quien lo hace cumplir.
3. **Linaje.** Cada propuesta enviada queda anotada en `files/selfpr/linaje.jsonl` (solo se añade): objetivo,
   archivo, revisión, issue, quién lo pidió y cuántos casos fallaba su examen entonces. «¿Cómo van tus PR?»
   lo muestra y compara con el examen más reciente: si empeoró, lo dice y sugiere repetir el examen y revertir.

No cambia: máximo 60 líneas, un archivo por propuesta, sin llamadas peligrosas, solo issues desde el teléfono,
fusión solo con todo en verde sobre el commit probado, y `SALVE_AUTOMERGE=false` para apagarla.

## Por qué así: lo que dice la investigación reciente

- **Darwin Gödel Machine** (agentes que reescriben su propio código): las mejoras llegaron con un archivo de
  variantes y su linaje; los autores trabajaron en entornos aislados, con supervisión humana, y avisan de que
  optimizar solo una puntuación puede llevar a «hackear» el objetivo. De ahí: linaje trazable y que el examen
  no sea editable por quien es examinada.
- **GRASP** (bibliotecas de habilidades que se mejoran solas): aceptar una edición solo si arregla más de lo
  que rompe en una muestra de casos anteriores. Sin esa puerta, escribir habilidades no fue mejor que no
  tenerlas. De ahí: comparar el examen antes y después de cada cambio, no fiarse de «compila».
- **LightMem** (memoria con modelos pequeños): un modelo pequeño reescribe la consulta, recupera en dos etapas
  y resume cada turno; la consolidación pesada se hace fuera de línea. Salve ya separa turno y sueño; el paso
  que aún no tiene es reescribir la pregunta en varias consultas hipotéticas antes de buscar en su memoria.

## Auditoría de esta revisión

- 584 archivos fuente, 46 subsistemas. Ninguna clase de `salve/` queda sin referencias (búsqueda por nombre),
  así que no se ha retirado nada.
- Deuda principal: `MotorConversacional` (3.424 líneas), `MainActivity` (2.591) y `DrawingStudio` (1.654), y un
  único gran ciclo de dependencias que une casi todo `core`. Partirlos a ciegas, sin compilar ni probar en el
  teléfono, rompería más de lo que arregla; es trabajo para hacer por piezas con el examen como red.

## Veredicto de cada cambio (11 de noviembre de 2026)

El linaje decía qué propuso; faltaba cerrar el ciclo. `SelfVerification`:

- Un cambio suyo está **sin comprobar** si la app se actualizó después de proponerlo y no se ha examinado con
  esa versión. Lo dice en «¿cómo van tus PR?» y lo anota en el registro del sueño (sin inferencia).
- El primer examen que haga a partir de ahí lo compara con el que tenía al proponerlo y guarda un **veredicto**
  (`files/selfpr/veredictos.jsonl`, solo se añade): mejor, igual o peor. Sale al final del informe del examen
  y en el registro de ejecución. Si empeoró, recomienda revertir el commit y da el issue donde encontrarlo.
- No afirma más de lo que mide: que la app se actualizara no prueba que la propuesta se fusionara, y entre dos
  exámenes pueden cambiar más cosas. El texto lo dice.
- Vive en `selfpr/`, zona que ella no puede reescribir.

### Se examina sola (11 de noviembre de 2026)

`SelfExam.runIfPending`, llamado por `SleepFocusWorker` dentro de la puerta del sueño (cargando y sin uso):

- Solo si hay un cambio suyo instalado sin veredicto. Nunca por rutina: el examen son minutos de inferencia.
- Usa el examen de siempre en silencio (`ConversationExamRunner.runQuietly`): no habla ni cambia el estado del
  chat. Entre caso y caso mira si Bryan ha vuelto; si hay que parar, **no guarda nota** (un examen a medias no
  es una medida).
- `SelfExamPolicy`: si se interrumpe no reintenta hasta pasadas 6 h, y tras 3 intentos con el mismo cambio
  espera a que Bryan lo pida. Cada decisión queda en el registro del sueño («ME EXAMINO SOLA») con su motivo.
- Necesita el motor de conversación vivo en el proceso (la app abierta o en segundo plano) y el modelo cargado;
  si no, lo dice y no hace nada.
- No abre PR ni revierte: mide y deja el veredicto.

### El examen mide lo mismo de día y de noche, y ahora también sus herramientas del teléfono

- El catálogo del examen dependía de si Bryan estaba verificado en ese instante: de día tenía más
  herramientas que de noche, así que dos exámenes no eran comparables y un veredicto nocturno podía salir
  «peor» sin motivo. `ConversationExamRunner.examCatalog` lo fija siempre al de Bryan verificado, todo simulado.
- Seis casos nuevos (44 en total): batería y luz (`device.sense`), linterna y volumen (`device.control`),
  wifi (`device.setting`) y «¿hay alguna mejora que efectuarías en ti misma?» (`self.command`). Nada se
  ejecuta en el teléfono durante el examen.
- Al cambiar el tamaño del examen, el primer veredicto pendiente saldrá «no comparable»: es lo correcto.


## Siguientes pasos, por efecto esperado

1. Que con un veredicto «peor» abra ella la propuesta de reversión (hoy da el veredicto y la recomendación), y
   separar del todo el examen del motor para que no dependa de que la app siga viva.
2. Reescritura de la consulta de memoria en 2–3 consultas hipotéticas (LightMem) dentro del presupuesto de 7k.
3. La misma puerta «arregla más de lo que rompe» para lecciones y habilidades aprendidas en el sueño.
4. Ampliar el examen: hoy mide conversación; no mide herramientas del teléfono, sentidos ni dibujo.

## Límites honestos

- Con Gemma 4 E2B/E4B en el teléfono y una ventana de unas 2.000 letras de código, muchas propuestas no
  compilarán. Lo que esta PR amplía es **dónde** puede intentarlo, no su capacidad de programar.
- Validado en JVM (`SelfChangeScopeTest`, pruebas del runner). El flujo completo desde el teléfono, con el
  modelo real escribiendo un parche dirigido, **no está probado**.
