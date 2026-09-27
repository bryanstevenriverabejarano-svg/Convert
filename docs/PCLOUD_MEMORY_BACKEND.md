# Backend de memoria pCloud

## Qué cambia

Salve usa pCloud como almacenamiento remoto autenticado sin guardar el token OAuth en el repositorio ni en texto plano.

Flujo:

1. El final del OAuth entrega `access_token` y el host de API de pCloud.
2. `CloudSyncManager.configurePCloud(context, token, host)` cifra el token mediante Android Keystore.
3. La acción visible del usuario habilita la sincronización con `CloudSyncManager.setEnabled(context, true)`.
4. Los eventos se conservan en Room y se suben a `/Salve/events/`.
5. Una subida sólo se considera correcta después de pedir a pCloud el checksum del archivo remoto y compararlo con el local.
6. Tras éxito, la fila local se conserva con `tries=-1`; deja de ser parte de la outbox pero sigue disponible para el diario/grafo.
7. Grafo, índice y visor se guardan en `/Salve/graphs/` y `/Salve/memory/`.
8. `restoreCoreMemory` recupera esos artefactos y restaura el diario remoto sin duplicados.

## Seguridad

- Sólo se aceptan `api.pcloud.com` y `eapi.pcloud.com`.
- El bearer token está cifrado con una clave AES-GCM de Android Keystore.
- El token nunca se registra ni se incluye en Git.
- Las rutas remotas rechazan traversal (`..`).
- La sincronización continúa requiriendo consentimiento local explícito.

## Integración OAuth pendiente del front

Este PR implementa el backend receptor del OAuth. El punto que ya completa el flujo es:

```java
CloudSyncManager.configurePCloud(context, accessToken, apiHostname);
CloudSyncManager.setEnabled(context, true);
```

No se incluye `client_id`, token ni redirect URI en el código. Si el OAuth ya fue realizado fuera de esta revisión, el token/hostname deben entregarse a este punto desde el callback autorizado de la app.

## Prueba end-to-end en teléfono

1. Configurar la credencial OAuth en la app.
2. Crear un recuerdo/evento con red desactivada.
3. Confirmar que permanece en `sync_events` con `tries >= 0`.
4. Recuperar red y ejecutar `SyncWorker`.
5. Confirmar archivo bajo `/Salve/events/` en pCloud.
6. Confirmar que la fila local sigue existiendo con `tries=-1`.
7. Borrar sólo los artefactos locales de prueba.
8. Ejecutar `restoreCoreMemory`.
9. Confirmar que índice/grafo y eventos vuelven y no se duplican al ejecutar restauración de nuevo.

La prueba remota necesita el token OAuth del dispositivo; CI sólo valida contratos, compilación y lógica local.
