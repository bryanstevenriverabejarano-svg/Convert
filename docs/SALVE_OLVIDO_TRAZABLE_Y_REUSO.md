# Salve — regla permanente de desarrollo y olvido trazable (29-10-2026)

## Norma para todos los PR siguientes

**Buscar → inspeccionar → reutilizar → conectar → desarrollar solo lo que falte → probar.**

Se ha incorporado una plantilla obligatoria en `.github/pull_request_template.md`.
El autor de cada PR debe listar módulos ya existentes, límites de su
responsabilidad, ruta efectiva del usuario a la función, qué código nuevo
era imprescindible y qué pruebas verifican que no se produjo una regresión.
La plantilla guía la revisión; **no sustituye la revisión humana ni impide
automáticamente una fusión**.

## Auditoría del borrado: código que YA existía

- `MemoryForgetRequest` + `PendingMemoryDeletion`: identifican la categoría
  exacta y requieren confirmación explícita «confirmar olvido».
- `MemoriaEmocional.eliminarDatoPerfil`: borra el perfil en Room y conserva
  el tombstone `memory_sync_state` y un evento en el outbox.
- `CloudMemoryImporter.profile`: aplica un `profile_delete` solo cuando
  supera la versión existente; un valor remoto antiguo no sobrescribe un
  borrado más reciente.
- `SelfModelJournal.forgetSources`: retira/ajusta creencias, pensamientos,
  preguntas y narrativas pendientes que citan fuentes eliminadas.
- `MindRuntime.pruneDeletedMemories`, `forgetMemoryVector`: controlan
  vectores E5 obsoletos y referencias derivadas.
- `MemoryLayerBridge`: no presenta como confirmada una hipótesis si
  sus episodios fuente no aparecen en el conjunto autorizado.

**No** se ha creado una segunda memoria, un segundo sincronizador,
ni otro mecanismo de borrado de perfil.

## Riesgos corregidos

### 1. Un olvido confirmado no debe esperar al próximo sueño

`RecuerdoDao.idsPorEtiqueta` captura IDs exactos bajo la misma transacción
que acepta el borrado. Después del commit,
`MindRuntime.revokeMemorySources` usa el mecanismo de
`SelfModelJournal.forgetSources` existente, comprueba que no quedan
referencias y elimina los vectores correspondientes. Si la limpieza
derivada falla, se registra advertencia y se conserva una vía de
reconciliación posterior. Se mantiene el timeout original de la transacción
Room sin añadir E5/archivo vectorial al trabajo dentro de ella.

### 2. Los borrados aceptados desde pCloud también retiran las referencias

`CloudMemoryImporter.profileDeletionIds` obtiene los IDs que *podrían*
ser afectados, y `CloudSyncManager.restoreMemoryBatch` los revoca
únicamente si el tombstone fue aceptado y su transacción Room se confirmó.
Un evento remoto antiguo o malformado no retira memoria vigente.
Se conservan los recibos de recuperación existentes.

### 3. Una restauración parcial no equivale a borrado

Un ID ausente de Room durante una restauración incompleta **no prueba**
que el usuario lo haya eliminado. El escaneo amplio de huérfanos se
aplaza si la historia remota aún se está recuperando; los borrados
confirmados se resuelven por IDs exactos.

### 4. Una copia de la mente no puede saltarse la verificación de fuentes

`SelfModelJournal.snapshotReferencesAvailable` reutiliza la estructura de
fuentes existente. Antes de exportar o restaurar una copia de
`SelfModelJournal`, `MindRuntime` verifica que las citas
`recuerdos:N` existen en Room; una copia antigua que menciona IDs
borrados se rechaza sin instalarse. La API de `restoreMind` se conserva,
con una condición adicional de integridad. Una copia bloqueada puede
reintentarse después de completar la restauración de sus episodios.

## Pruebas

- `SelfModelJournalTest`: bloqueo de copia antigua con fuente eliminada,
  aceptación de copia limpia y citas web independientes.
- `CloudMemoryImporterTest`: revocación solo de perfiles exactos.
- `IndexedMemoryIntegrationTest`: IDs seleccionados antes de tombstone,
  no afectar categorías adyacentes, no resucitar datos tras reinicio y
  mantener FTS.
- GitHub Actions Android debug/preview y suites de regresión existentes.

## Limitaciones pendientes

Este PR **no** garantiza el borrado irreversible de todas las copias
históricas existentes en dispositivos ajenos, respaldos externos u otros
proveedores. El tombstone y la validación impiden reaplicar estados
antiguos por las rutas previstas, pero una auditoría de todas las
representaciones, fotos, exportaciones manuales, grafos cacheados e
identificadores estables entre dispositivos sigue siendo necesaria.
La existencia de un ID local no demuestra por sí sola igualdad de
contenido si un respaldo se importó desde otra base con IDs diferentes.
La prueba final de borrado debe hacerse en el Galaxy S24 Ultra con una
copia de prueba, nunca con recuerdos personales irreemplazables.
