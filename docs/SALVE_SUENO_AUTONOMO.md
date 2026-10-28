# Salve · sueño activo y concentración autónoma

Este modo conecta la animación de acostarse con una sesión interna **acotada** de
aprendizaje operativo. No representa conciencia, ejecución continua ni un modelo
que pueda modificar su propio software sin revisión.

## Activación y control

- «Salve, duerme», «vete a dormir» o «acuéstate»: si no existe cama se prepara,
  el avatar camina hasta ella y reproduce su animación de dormir.
- Botones **Acostarse** en la habitación y **Cama** en el avatar flotante: misma
  activación, después de guardar la pose en `salve_avatar`.
- «Despierta», levantarse, caminar o retirar la cama: desactiva el modo y cancela
  sus trabajos pendientes. Un trabajo en curso comprueba la cancelación entre fases.
- El modo opt-in queda persistido tras reiniciar Android, pero **solo trabaja si la
  pose guardada sigue dormida o caminando hacia la cama**.

## Qué hace una sesión

`SleepFocusWorker`, protegido por `BackgroundMindGate`, ejecuta como máximo:

1. `MindRuntime.dream`: consolida nuevas experiencias con identificadores de fuente,
   contradicciones, preguntas y mantenimiento del índice de memoria.
2. `MindRuntime.reflectIfDue`: reflexiona solo si hay cambios y se cumple su
   intervalo propio; el núcleo autobiográfico sigue sujeto a las reglas de aprobación.
3. `MindRuntime.play(..., 1)`: resuelve **un reto local verificable** del laboratorio
   de estrategias; un fallo no se convierte en conocimiento validado.

Las investigaciones de preguntas públicas continúan en el `CuriosityWorker`
existente. Al acostarse se pide un intento de curiosidad diferida, respetando
sus límites, permisos de red y prioridad de la conversación. No hay navegación
sin límites ni publicación automática de cambios al código.

## Ejecución responsable en Android

Un intento único se solicita al acostarse. Si continúa dormida se comprueba
aproximadamente cada **2 horas** mediante WorkManager, únicamente **cargando
y con batería suficiente**. Los intentos no garantizan ejecución a una hora exacta:
Android puede retrasarlos, cancelarlos o no ejecutarlos si falta energía. No hay
`while(true)`, wake-lock permanente, grabación ambiental ni modelo trabajando
24/7. Salve también conserva los otros ciclos periódicos de pensamiento, juego
y curiosidad previos a este cambio.

Si el usuario está hablando, la mente está pausada o el modelo se está cargando,
el ciclo cede prioridad y no fuerza la ejecución. Los errores no imprimen recuerdos,
prompts ni rutas privadas en los registros de Android.

## Verificación

- `SleepFocusPolicyTest`: no trabaja sin una orden de dormir y cama; se detiene
  al despertar/retirar la cama; la pose persistida es la autoridad tras reiniciar.
- `AssistantControlCommandTest`: comandos exactos, sin activar el sueño cuando
  alguien simplemente pregunta por él.
- CI Android: pruebas JVM y construcción de las variantes debug y preview.
- Pendiente comprobar en teléfono real la continuidad de la animación, la latencia
  del modelo y las restricciones de carga/Doze.

## Siguientes etapas para mejorar de verdad

Para pasar de reflexión a **mejora significativa** hacen falta retos inéditos,
criterios externos de corrección y pruebas de regresión. Solo propuestas
comprobables de cambios o nuevas herramientas deben pasar a una bandeja de
revisión; el modelo no debe reescribirse, subir permisos, instalar programas ni
publicar parches de GitHub por una conclusión obtenida durante el sueño.
