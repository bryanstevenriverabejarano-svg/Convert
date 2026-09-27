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

## Conexión desde Android

En la ficha **Salve Android** de https://docs.pcloud.com/my_apps/ añade exactamente esta Redirect URI y guarda los cambios:

`http://localhost:8765/callback`

Instala la versión de Salve que incluya esta pantalla. En la app abre **IA y cámara → Nube pCloud → Conectar** y autoriza en el navegador del mismo móvil. El Client ID público ya está incorporado en la app; el Client secret no se usa. El navegador vuelve a un receptor local temporal de Salve. El callback verifica un estado aleatorio y el host oficial antes de guardar el token cifrado en Android Keystore. El receptor se cierra al completar o tras tres minutos. Luego se activa el worker. En usos posteriores se conserva la autorización local y no se abre el navegador mientras siga conectada.

El enlace con el backend es:

```java
CloudSyncManager.configurePCloud(context, accessToken, apiHostname);
CloudSyncManager.setEnabled(context, true);
```

El Client ID es público y está en el código; el Client secret y el token nunca se incluyen en Git. La carpeta `/Salve` y sus subcarpetas se crean con la primera subida, no al registrar la aplicación en pCloud. Sólo los eventos encolados mientras la sincronización está activada se envían; no se vuelcan automáticamente recuerdos anteriores.

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
