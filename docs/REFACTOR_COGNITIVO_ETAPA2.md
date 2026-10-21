# Reforma cognitiva de Salve — etapa 2: catálogo separado y recibos verificables

## Antes y después
`MotorConversacional.cognitiveTools` mezclaba la decisión de cuáles herramientas
exponer, validación de campos JSON, ejecución Android y resultados. La nueva ruta
conserva `CognitiveTurn` y las herramientas reales, pero separa responsabilidades:

```text
USER_INPUT + estado verificado del dispositivo
  -> CognitiveToolCatalog.build (allowlist + esquema por herramienta)
  -> CognitivePrompt.prioritize / limitTools (lo no ofertado NO se ejecuta)
  -> CognitiveTurn (valida contrato del modelo, presupuesto, no repetición)
  -> MotorConversacional.executeCognitiveTool (implementaciones Android existentes)
  -> CognitiveTurn.Outcome.receipts() (copia defensiva del registro real)
  -> CognitiveToolAudit (resumen de resultados sin argumentos ni recuerdos)
  -> ExecutionTrace ("RECIBOS DEL TURNO COGNITIVO")
```

## Límites de autoridad
- El modelo **no registra herramientas**; el catálogo procede del objetivo
  actual, del estado Android y de controles de acceso del sistema.
- En modo foto descriptiva no hay herramientas ejecutables.
- `TAP` y `ESCRIBIR` necesitan el servicio activo y una intención relevante;
  su ejecución exige la interfaz de confirmación que ya existía.
- `DEPLOY_WEB` solo aparece con solicitud explícita; su publicación sigue
  pasando por aprobación independiente.
- Las herramientas de vestuario siguen pasando por
  `AvatarDesignRequest.parse`, incluidos nombres e identificadores existentes.
- **Cambio deliberado de privacidad:** `memory.lookup` y `cloud.sync` exigen
  `IdentityManager.privateAccess()`, no simplemente que la sesión se presuma
  del creador. La nube además requiere intención explícita de sincronizar,
  pCloud configurado y no estar pausado por el usuario. Se revalida el acceso
  al ejecutar, no solo al crear el catálogo.
- La búsqueda web conserva la comprobación existente
  `NetworkPrivacy.safeForNetwork`: memoria y datos privados no se copian a
  la consulta externa.

## Qué significa un recibo
`CognitiveTurn.Outcome.receipts()` devuelve los resultados del controlador,
sin transcribir razonamientos del modelo. Una aprobación pendiente se etiqueta
`handoff`, **no** `completed`. El resumen `CognitiveToolAudit` contiene solo
nombre de herramienta, estado y contadores: omite respuestas de páginas, datos
personales y parámetros. Conserva `goal_verified=false` en todos los casos,
porque la ejecución de una herramienta nunca basta para certificar el objetivo.

Este registro se conecta realmente a `ExecutionTrace`. No incorpora todavía
una nueva habilidad a `SkillLab`: para aprender de forma justificada habría que
relacionar el recibo con un **verificador independiente del objetivo**, no
contar las llamadas a herramientas como aciertos.

## Pruebas y validación
- Pruebas de permisos del catálogo: invitado/creador, fotografía, intención
  explícita, pantalla, despliegue, vestuario, argumentos extra y no ejecución
  al construir el catálogo.
- Pruebas de recibos: `completed` frente a `failed` y `handoff`,
  aislamiento de datos privados, copia defensiva y ausencia de éxito supuesto.
- Las suites JVM y Android de CI, así como pruebas físicas Dolphin/Gemma,
  determinan si se puede fusionar con confianza. Código publicado no significa
  instalación probada en el Samsung.

## Etapa 3
1. Añadir un **contrato de verificador del objetivo** independiente del LLM
   que asigne `VERIFIED`, `FAILED`, `PARTIAL` o `UNKNOWN`, con recibos y
   condiciones preestablecidas.
2. Solo los resultados independientes deberían nutrir un nuevo ciclo de
   aprendizaje; registrar señales predictivas y comprobar generalización
   con tareas nunca observadas durante la selección de estrategias.
3. Proseguir separando el dibujo y la generación de respuestas de
   `MotorConversacional`, con pruebas de regresión de UX/voz.
