# Salve — Etapa 4: objetivos verificables de varios pasos

## Problema original
La PR #207 separa la revisión del propio modelo del cumplimiento verificable de
un objetivo, pero el criterio inicial `PUBLIC_READ` admite solo **una página**.
Un plan que necesita dos acciones podría declarar completado el objetivo
después de una sola operación. Este cambio añade un criterio conservador de
objetivo compuesto sin inferir intenciones a partir de contenido de páginas.

## Flujo y contrato
Para una orden explícita como:

`Lee https://example.org/alpha-unseen y https://example.net/beta-unseen`

`AgentMultiStepVerifier` extrae del **texto original autorizado por el usuario**
ambas URL y exige lo siguiente:

1. Dos HTTPS públicas, distintas, permitidas por `AgentPolicy`, sin crear
   nuevas autorizaciones.
2. Dos invocaciones a `web.read`, cada una con su huella de llamada
   `AgentExperience.fingerprint("web.read", url)` calculada por el controlador.
3. Cada recibo incluye la URL exacta en `sources`, un contenido no vacío y
   `status=SUCCESS`.
4. Los dos identificadores de recibo son distintos y aparecen en
   `AgentRun.evidence` (citas de la respuesta).
5. El modelo ha entregado texto de respuesta no vacío. Su revisión no decide
   si el objetivo es verdadero.

Cuando ambas lecturas se completan, `goalVerification=VERIFIED` con criterio
`PUBLIC_READ_SET`. Una lectura válida entre dos, o una lectura parcial,
produce `PARTIAL`; ausencia de lecturas válidas, `NOT_VERIFIED`.

**Límite semántico esencial:** verificar dos lecturas **NO** verifica un
resumen, una comparación, una conclusión ni la verdad de las páginas. Ordenes
como «compara», «deduce» o «investiga» permanecen `UNKNOWN` hasta contar con
criterios independientes para sus contenidos.

## Persistencia y autonomía
- `AgentGoalVerifier` detecta este contrato antes del antiguo
  `PUBLIC_READ`.
- `AgentEngine.finish` guarda los recibos verificados en orden de la petición
  en `verificationEvidenceIds` y conserva `verificationEvidence` como
  primer recibo para lectores antiguos.
- `AgentRun.decode` valida ids, duplicados, coherencia con evidencias y
  que se requieran **dos** para `PUBLIC_READ_SET`. Diarios anteriores sin
  la lista se siguen leyendo; no hay migración destructiva.
- `AgentRuntime` muestra ambos recibos al consultar los planes.
- `AgentExperience` reutiliza el verificador independiente existente para
  contar como una meta satisfecha el objetivo compuesto completo. No amplía
  permisos, ni entrena el modelo, ni copia información privada.

## Evaluación
`AgentMultiStepVerifierTest` cubre:
- URLs no usadas en el desarrollo del primer verificador, orden invertido de
  ejecución, estado parcial, fallos, citas ausentes, fuentes y huellas falsas,
  destinos privados, duplicados y afirmaciones del modelo sin evidencia;
- diario persistente, exactitud de ids y rechazo de metadatos incoherentes.

`RoomAgentStoreTest` cubre dos llamadas reales al contrato de ejecutor,
respuesta referenciada, revisión, comprobación independiente y reapertura de
la base Room.

`AgentExperienceTest` comprueba que solo el objetivo realmente verificado
aporta una señal a planes posteriores; un solo recibo degradado lo elimina.

CI Android, Windows y contrato multilingüe deben pasar antes de fusionar.
Una compilación exitosa no sustituye una prueba en el Samsung.

## Siguiente paso
Hacer un entorno de tareas **con resultados estructurados independientes**,
por ejemplo resolver pequeñas consultas usando pruebas generadas fuera del
modelo, con ejemplos reservados para generalización. No debe promoverse el
razonamiento privado del modelo a «prueba» ni confundirla con consciencia o AGI.
