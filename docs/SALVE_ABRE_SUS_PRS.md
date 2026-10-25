# Salve abre (y fusiona) sus propios PR

Desde el 25-10-2026 Salve puede proponer cambios en su propio código desde el teléfono. Si la compilación completa pasa, los fusiona sola.

## Cómo se usa

1. **Token (una vez).** En GitHub: *Settings → Developer settings → Fine-grained tokens → Generate new token*.
   - *Repository access:* solo `Convert`.
   - *Permissions:* **Issues: Read and write**, ningún otro.
   - En Salve se pega en *Ajustes → IA y cámara → PRs de Salve (GitHub)*. Se guarda cifrado con AndroidKeyStore, queda fuera de las copias de seguridad y no se envía al modelo.
2. **Permiso de Actions (una vez).** En el repositorio: *Settings → Actions → General → Workflow permissions →* marcar **Allow GitHub Actions to create and approve pull requests**. Sin esto, el flujo sube la rama y comenta el enlace para abrir el PR a mano.
3. **Uso.** Primero «hazte el examen»; después «abre un PR». Para ver cómo van: «¿cómo van tus PR?».

## Qué pasa por dentro

| Paso | Dónde | Qué hace |
| --- | --- | --- |
| 1 | Teléfono (`SelfPrDesk`) | Toma el fallo más repetido del último examen (≥ 2 casos) y el módulo que `SynergyEvolution` asocia a ese fallo. |
| 2 | Teléfono (`GitHubSelfPr`) | Descarga ese archivo exacto de `main` (API de GitHub, solo lectura) y su revisión. |
| 3 | Teléfono (`SelfPatch`) | Enseña al modelo local la ventana de código más relacionada (cabe en su contexto). El modelo responde con bloques BUSCAR/REEMPLAZAR. Salve los aplica al archivo completo y comprueba llaves, paréntesis y llamadas peligrosas, con un máximo de 60 líneas. Después genera el diff unificado; si algo falla hace un segundo intento. |
| 4 | Teléfono | Abre una issue `[Salve] Propuesta…` con el diff legible y la propuesta JSON (esquema 3 de `auto_improvement_runner`, con `sourceRevision` y `sourceSha256`). |
| 5 | Actions, job `probar` (solo lectura, sin secretos) | Solo actúa con issues del dueño. Valida la propuesta, vuelve a comprobar los límites, exige la misma huella de la fuente, aplica el parche y ejecuta `:app:testDebugUnitTest`. |
| 6 | Actions, job `publicar` (escritura, sin ejecutar el código propuesto) | Vuelve a aplicar el parche sobre la misma revisión, exige el **mismo árbol probado**, sube `salve/auto-…` y abre el PR. |
| 7 | Actions, `merge` | Lanza `android.yml` (pruebas, APK, firmas y alineación) sobre esa rama. Si termina en verde sobre **exactamente ese commit** y el PR no ha cambiado, lo fusiona (squash), pide el APK de `main` y borra la rama. |
| 8 | Issue | Comenta el resultado (`merged`, `passed`, `failed` o `rejected`) y la cierra. Salve lo lee con «¿cómo van tus PR?». |

## Cómo apagar la fusión automática

Crea la variable de repositorio `SALVE_AUTOMERGE` con valor `false` en *Settings → Secrets and variables → Actions → Variables*. Los PR seguirán abriéndose con la compilación completa lanzada, pero esperarán tu revisión. Para deshacer un cambio ya fusionado basta con revertir su commit.

## Límites honestos

- El modelo del teléfono (Gemma 4 E2B) es pequeño y ve unas 2.000 letras de código. Es probable que muchas propuestas no compilen o no pasen las pruebas; el flujo lo dirá en la issue.
- «Pasa las pruebas» no significa que mejore a Salve: las pruebas JVM no miden la calidad de la conversación. Lo que lo mide es repetir el examen en el teléfono tras actualizar.
- Solo puede tocar los módulos de la lista de `SynergyEvolution`, siempre dentro de `app/src/main/java/salve/core`.
- No puede añadir procesos, carga de código, red, reflexión ni borrado de archivos, y las propuestas que se fusionan solas no pueden añadir suites de prueba generadas.
