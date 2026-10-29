# Regla permanente de desarrollo de SALVE

En **cada PR** del núcleo, memoria, herramientas, sueño, agentes, interfaz y
sincronización, aplicar esta secuencia antes de escribir código:

**Buscar → Inspeccionar → Reutilizar → Conectar → Desarrollar solo lo que falte → Probar.**

## Comprobación obligatoria (PR)

1. **Buscar** en el árbol de `main` funciones, clases, workers, DAOs,
   servicios, pruebas y documentación que ya resuelvan parte del requisito.
2. **Inspeccionar** las llamadas y los caminos reales: productor, consumidor,
   restricciones, persistencia, errores y verificadores; no confiar solo en
   el nombre de la clase o en un resumen.
3. **Reutilizar** componentes existentes y contratos públicos; nunca crear
   otro almacén, LLM, orquestador o canal de sincronización para lo mismo.
4. **Conectar** al ciclo cognitivo y a los puntos de verificación, con
   permisos y efectos controlados fuera del texto del modelo.
5. **Desarrollar** únicamente la funcionalidad faltante, con presupuestos
   de RAM, CPU, privacidad, batería y permisos explícitos.
6. **Probar** con JVM/Robolectric/Android CI y, cuando lo exija la función,
   dispositivo físico y el modelo real. Incluir regresiones de borrado,
   cancelación, memoria incompleta, restauración y fallo de red.

## Evidencia que debe quedar en la descripción del PR

- Componentes examinados (rutas exactas) y por qué no bastaban.
- Componentes reutilizados/conectados.
- Cambios nuevos y límites, permisos o supuestos.
- Pruebas nuevas y existentes, resultados concretos de GitHub Actions.
- Limitaciones pendientes y plan de reversión si aplica.

**Nunca** declarar que algo está implementado por un plan teórico, que está
probado por CI si solo se comprobó la sintaxis, o que se olvidó completamente
un dato si aún quedan copias externas/archivos de respaldo.

## Aplicación de la regla al olvido coherente (octubre 2026)

**Ya existía:** `MemoryForgetRequest` y confirmación humana;
`MemoriaEmocional.eliminarDatoPerfil` borra etiquetas Room y registra
tombstones de perfil; `CloudMemoryImporter.profile` bloquea replay de
registros anteriores; `SelfModelJournal.forgetSources` retira referencias,
creencias y pensamientos derivados; `MindRuntime.pruneDeletedMemories`
elimina vectores huérfanos; Room FTS tiene sus disparadores de borrado.

**Huecos conectados en esta etapa:**
- La reconciliación del modelo derivado se ejecuta tras el borrado confirmado,
  sin esperar a un sueño o un worker posterior.
- `MindRuntime.exportMindSnapshot` depura fuentes ausentes y valida el
  respaldo antes de subir una copia del yo persistente.
- `MindRuntime.importMindSnapshot` rechaza respaldos anteriores que citen
  IDs Room ausentes. No reactiva automáticamente recuerdos desaparecidos.
- `SelfModelJournal.snapshotReferencesAvailable` reutiliza los tipos y
  esquema actuales para verificar citas; no crea otra DB o un tombstone nuevo.

**No cubierto todavía:** purga definitiva de todos los artefactos exportados
en pCloud, historiales remotos inmutables y dispositivos familiares; borrado
semántico de texto libre en narrativas, experiencias y documentos; coordinación
de identificadores estables entre dispositivos. Una respuesta de Salve debe
explicar esas limitaciones. No se debe asegurar «eliminación total» hasta
contar con protocolo de revocación confirmado de extremo a extremo.
