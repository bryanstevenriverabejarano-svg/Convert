# Salve — Borrado confirmado, respaldo y regla obligatoria de reutilización

## Auditoría del código existente

Antes de tocar la memoria se inspeccionaron los siguientes componentes:

| Necesidad | Función YA existente | Acción |
|---|---|---|
| Confirmar el borrado | PhoneActionsDesk.confirmarOlvidoPendiente / PendingMemoryDeletion | Conservar los controles de intención y expiración |
| Borrar en Room | MemoriaEmocional.eliminarDatoPerfil y RecuerdoDao.eliminarPorEtiqueta | Reutilizar la transacción existente |
| Impedir resurrección por historial cloud | CloudMemoryImporter.profile con timestamp y MemorySyncState tombstone | No duplicar su política de precedencia |
| Actualizar pCloud | CloudSyncManager.enqueueProfileDeletion, indexLocalJournal y restoreMemoryBatch | Aprovechar el mismo journal de eventos |
| Limpiar embeddings | MindRuntime.pruneDeletedMemories, EmbeddingsIndex.retainOnly | Reutilizar con los IDs exactos afectados |
| Limpiar inferencias | SelfModelJournal.forgetSources | Reutilizar, sin crear otra tabla ni motor de olvido |
| Proteger prompt de creencias obsoletas | MemoryLayerBridge | Mantener el filtro de citas vivas del PR #293 |
| Copia de la mente | MindRuntime.exportMindSnapshot e importMindSnapshot | Comprobar las citas antes de aceptar un backup |

## Brechas corregidas (sin introducir otro almacén)

1. **Borrado de perfil confirmado:** la transacción toma los IDs exactos de Room
   antes de eliminarlos. Después se llama a `MindRuntime.revokeMemorySources`
   fuera de la sección con timeout de 3 s. Se reutilizan
   `SelfModelJournal.forgetSources` y el índice vectorial. Si la limpieza falla,
   se registra y se solicita reintento; no se afirma que las copias externas
   se hayan eliminado.
2. **Tombstones de pCloud:** cuando se aplica realmente una eliminación
   remota, se capturan antes los IDs locales afectados. Solo si la operación
   fue aceptada se revocan las referencias derivadas. El borrado de una
   categoría no afecta a categorías vecinas con nombres parecidos.
3. **Restauración parcial:** una ausencia local mientras pCloud recupera
   lotes **no significa** que el recuerdo haya sido eliminado. Se evita
   convertir una restauración incompleta en un borrado masivo.
4. **Copias del modelo interno:** se rechaza importar o exportar un
   `self_model` con citas `recuerdos:N` sin filas Room correspondientes
   en este dispositivo. Se prefiere no restaurar un modelo conceptual
   antes que revivir una creencia obsoleta.

## Regla permanente para futuras modificaciones

- `AGENTS.md` instruye a agentes y colaboradores a seguir
  **Buscar → inspeccionar → reutilizar → conectar → desarrollar solo lo que falte → probar**.
- `.github/PULL_REQUEST_TEMPLATE.md` solicita en cada PR las rutas revisadas,
  las funciones reutilizadas, el hueco real y las pruebas de regresión.
- Esta regla queda documentada en el repositorio, pero los checkboxes **no
  sustituyen una revisión humana ni hacen técnicamente imposible omitirla**.

## Qué NO garantiza este parche

- El borrado categorial de un perfil no borra retroactivamente todas las
  frases originales de conversaciones antiguas o medios guardados con otra
  categoría. Requiere un flujo de alcance más amplio, solicitado y confirmado
  explícitamente.
- La copia histórica ya almacenada en pCloud puede seguir conteniendo datos
  hasta que su política de retención y versiones sea tratada de forma
  separada. El tombstone evita su reproyección como perfil actual, pero no
  equivale a purgado físico del almacenamiento remoto.
- Un ID numérico de Room es local a una instalación; restaurar una mente
  de otro móvil requiere validar el mapeo de identidades de episodios.
  No se promete reconciliación universal entre dispositivos.
- El borrado puede terminar en Room aunque se produzca un fallo de
  persistencia del índice o diario. En ese caso no se debe presentar la
  operación como eliminación confirmada en todas las capas; los controles
  de recuperación basados en Room y la reconciliación posterior siguen
  actuando como defensa.
- No se ha realizado una operación de borrado en los recuerdos personales
  del usuario desde este PR; solo se modifica código y pruebas.

## Verificación

Las pruebas del parche cubren citas de snapshots anteriores, evidencia
faltante/malformada, limpieza derivada existente, IDs exactos de categoría,
tombstones y el replay antiguo. GitHub Actions compila las variantes
Android/debug y preview y ejecuta la regresión Java. La validación final
debe comprobarse en el commit exacto y luego con instalación física.
