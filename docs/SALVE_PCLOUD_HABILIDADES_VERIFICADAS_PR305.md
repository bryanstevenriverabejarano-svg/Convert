# Salve — biblioteca de habilidades simbólicas en pCloud (2026-10-30)

## Hallazgo, reutilización y alcance

**Buscar → Inspeccionar → Reutilizar → Conectar → Desarrollar lo que falta → Probar.**

Antes de este cambio, `CloudSyncManager.syncKnowledge` ya sincronizaba `mind/skills.json` (estrategias de `SkillLab`) y otros artefactos de aprendizaje con pCloud. Sin embargo, no transportaba los dos registros **locales** de habilidades que el laboratorio autónomo usa para ejecutar operaciones simbólicas:

- `getNoBackupFilesDir()/autonomy/tools.json` → estado y versiones de `AutonomousToolLab`, cuatro familias verificables.
- `getNoBackupFilesDir()/autonomy/inductive-skills.json` → programas acotados de listas numéricas de `InductiveSkillLab`.

No se cambian los LLM, los kernels, sus pruebas de certificación, `ToolLearningSupport`, ni los permisos existentes. No se crea otro laboratorio, programación remota ni escritura dinámica de código de la APK.

## Integración del mismo transporte pCloud

`SyncWorker` ya llama a `CloudSyncManager.syncKnowledge` cuando hay conexión, autorización OAuth y consentimiento de sincronización explícito. Se reutiliza ese punto, junto a `PCloudProvider.listFilesChecked`, `uploadJson` (checksum remoto comprobado) y `downloadBounded`.

La nueva ruta remota es **`/Salve/autonomy/verified/`**. Cada catálogo válido y no vacío se guarda con nombre inmutable, basado en **tipo, hora y SHA-256** del JSON. No se pisa otro archivo de pCloud ni una versión creada desde otro móvil. Las cargas de un digest ya presente se omiten. No se suben los archivos de propuestas pendientes, tokens, código Java/Kotlin, scripts de sistema ni credenciales.

`SkillCloudArchive` empaqueta solo datos JSON con contrato cerrado de cinco campos: esquema, familia, fecha, digest y contenido. La carga se vuelve a validar con **los propios constructores actuales** de `AutonomousToolLab` e `InductiveSkillLab`, en almacenes de **solo lectura**, no con un verificador nuevo que pueda divergir. Se limitan los tamaños a 512 KiB por catálogo y 1 MiB por sobre.

**Confidencialidad:** los catálogos pueden incluir entradas y ejemplos de retos anteriores. El contenido queda en la **cuenta pCloud que el usuario habilitó**; el checksum detecta corrupción accidental, pero SHA-256 **no es autenticación** contra un usuario/servidor malicioso. No se afirma cifrado extremo a extremo de estos archivos. No sincronizar si los ejemplos contienen información que no debe almacenarse remotamente.

## Recuperación y autorización

Los archivos externos nunca se descargan directamente sobre `autonomy/tools.json` o `autonomy/inductive-skills.json`. Solo si el catálogo local **no existe**, el worker considera hasta tres versiones recientes dentro de una lista acotada, verifica tipo, digest, ruta, tamaño y el decoder original, y deja la copia en:

`getNoBackupFilesDir()/autonomy/cloud-inbox/<nombre>.json`

**Cuarentena.** No hay auto-promoción ni ejecución de código desde el almacenamiento remoto. Nunca se sustituye un archivo local existente o una copia ya pendiente de revisión.

Los comandos de usuario ya enrutados a `AutonomousToolRuntime` se amplían con:
- `estado habilidades pcloud`: cuenta únicamente copias pendientes; exige `IdentityManager.privateAccess()`.
- `restaura habilidades pcloud`: requiere dueño verificado, sincronización pCloud consentida y laboratorio previamente **pausado**; relee y revalida el sobre y el catálogo mediante decoders existentes; **solo instala cuando no hay catálogo local**. El laboratorio permanece pausado hasta que el usuario lo reanude de forma explícita. Si el catálogo local existe, ni se borra ni se mezcla automáticamente.

El comando `habilidades aprendidas` y el estado del laboratorio muestran aviso de copias pendientes; no realizan importaciones silenciosas.

### No confundir con programación remota

pCloud es **almacenamiento de versiones**. Sus archivos no ejecutan programas en sus servidores. Las habilidades recuperadas son recetas de las operaciones puras y permitidas de Salve. `code.python` del agente es un entorno **aislado y aparte** que exige autorización específica; esta integración no permite descargar scripts arbitrarios ni modificar el código Android en producción.

## Regresiones automatizadas

- `SkillCloudArchiveTest`: decoders originales, digest ligado a ruta, nombres permitidos, registro desconocido, código fuera de la lista blanca, corrupción, presupuestos, versiones, cuarentena y veto a sobrescribir locales.
- `AutonomousToolRuntimeTest`: sin aprobación, no importa; sin pausa, no importa; con pausa y aprobación, restaura únicamente ausentes; no reanuda ejecutores ni pisa el catálogo.

**Limitaciones:** CI no puede autenticar la pCloud particular del teléfono ni comprobar una restauración real desde varios dispositivos. También queda para validación física la duración de subida/bajada, copias con múltiples versiones, consumo de batería y selección humana de conflictos entre dos catálogos válidos. El PR no prueba nuevas capacidades AGI ni reentrena Gemma.
