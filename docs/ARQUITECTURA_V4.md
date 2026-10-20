# Arquitectura V4: estado real y revisión crítica

Respuesta a la «Propuesta de Arquitectura Maestra – Salve AGI Soberana y Ecosistema Salventes».
Qué está hecho, qué se ha cambiado y por qué, y qué queda como decisión de Bryan.

## Estado por sección

| Sección V4 | Estado |
|---|---|
| 1. mTLS sin VPN, LAN → remoto 8443 | **Hecho** (#182–#183). TLS mínimo 1.2; con un móvil moderno se negocia 1.3 |
| 2. Nodo Alpha | **Hecho**: voz, Maps, Spotify, llamadas, constelación |
| 2. Nodo Inferencia | **Hecho**: `SalveNodo.exe` + llama-server; también piensa por los Salventes |
| 2. Nodo Guardián | **Hecho como Guardián ciego** (ver abajo, difiere del plan) |
| 2. Satélite Salvente | Diseño (Fase 3) |
| 2. Taller / Sandbox | **No hecho a propósito** (ver revisión) |
| 3. Salvente Genesis | **Hecho** (#184–#185): Room aislada, 14 rasgos, aprobación humana, conversación |
| 3. Consciencia residente en red | **Pendiente de decisión** (ver dilema) |
| 4. Capa domótica | **Hecho, Hito 3**: pasarela Home Assistant local (`salve_home.py`) |
| 4. Pantallas y micrófonos periféricos | Diseño |
| 5. Ficha con VRAM | **Hecho**: GPU, VRAM usada/total y carga desde `nvidia-smi`, más los roles del nodo |
| — Continuidad de Salve | **Nuevo**: su memoria entera sellada en el Guardián |

## Continuidad de Salve (añadido)

La mente de Salve (`noBackupFilesDir/mind`: modelo de sí misma, acciones, lecciones, habilidades)
**nunca** entraba en las copias de Android. Perder el teléfono era perderla.

- `MemorySnapshot` empaqueta `memoria.db` (instantánea coherente con `VACUUM INTO`, que incluye lo
  pendiente en el WAL y conserva la versión de esquema) y los JSON de su mente, de sus objetivos y de
  sus dibujos. Lleva manifiesto con SHA-256. **No** viajan las claves del teléfono ni los vectores,
  que se recalculan.
- Al leer una copia se valida todo: lista blanca de nombres, sin rutas trampa, límites contra bombas
  zip, cabecera SQLite, esquema 4–8 (el que saben abrir las migraciones) y JSON válidos.
- Se sella con la frase de Bryan (`SealedBox`), se comprueba que abre y se sube al Guardián con el
  SHA-256 verificado. Hay opción de copia nocturna (solo cargando y con Wi-Fi); en ese caso la frase
  se guarda cifrada con Keystore en el teléfono.
- **Restaurar nunca toca la memoria en caliente.** Se prepara en una carpeta y se aplica en
  `SalveApplication.attachBaseContext`, antes que cualquier proveedor o tarea, guardando la memoria
  anterior en `continuity/previous`. Si algo falla a mitad, se deshace.
- Copiar y restaurar piden la verificación del teléfono. Al despertar de una restauración, Salve lo
  anota en su registro de acciones: sabe que viene de una copia y hasta cuándo recuerda.

## Hogar (Hito 3)

- **En el PC**, `salve_home.py`:
  - El token de Home Assistant está solo ahí.
  - Solo se controlan las entidades permitidas una por una, de los tipos luz, enchufe, ventilador, persiana, clima y escena.
  - Cada tipo tiene acciones cerradas con valores acotados: brillo 1–100 %, temperatura 5–35 °C.
  - Máximo 30 órdenes por minuto.
  - Home Assistant tiene que estar en la red local.
  - Las cerraduras quedan fuera a propósito: abrir una puerta no debe ser una orden de voz.
- **En el móvil:**
  - `HomeCommand` solo reconoce órdenes que nombran un dispositivo doméstico («enciende la linterna» no cuenta).
  - Ante una orden ambigua pregunta cuál, y siempre pide un «sí» antes de actuar.
  - Solo puede ordenar Bryan.
  - La pantalla «Mi casa» ofrece botones explícitos.
- **MQTT directo no se implementa:** Home Assistant ya integra Zigbee, Matter y MQTT, y duplicarlo
  añadiría superficie de ataque sin aportar nada.

## Integración con #186 (habilidades inductivas) y #187 (puerta cognitiva)

- **Fallo corregido entre parches:** las habilidades verificadas de #186 se guardan en
  `autonomy/inductive-skills.json` y la lista blanca de la copia no admitía guiones. Lo que Salve
  aprende en su laboratorio se habría perdido al cambiar de móvil. Ahora viaja en la copia y hay un
  test que lo comprueba.
- **La copia nocturna pasa por `BackgroundMindGate` (`Task.CONTINUITY`):** nunca se toma a mitad de
  un sueño, una reflexión o un juego (si no, la base de datos y la mente quedarían descuadradas),
  cede ante la conversación y aparece en el estado de su mente en segundo plano. Si la puerta está
  ocupada, se reintenta cada 15 minutos. La copia pedida a mano espera hasta 90 s a que termine el
  ciclo en curso.
- **Coherente con su principio de no convertir reflexiones en órdenes externas:** el hogar solo
  responde a órdenes explícitas de Bryan dentro de una conversación, nunca a trabajos en segundo plano.

## Revisión crítica

1. **«Salve no duerme cuando el teléfono se apaga» choca con el Guardián ciego.** Para que su proceso
   cognitivo corra 24/7 en el PC, el PC tiene que poder leer su memoria. Hoy está diseñado para no
   poder hacerlo. Opciones (**decisión de Bryan**):
   - **A. Presencia ligera (recomendada):** el PC corre sus ciclos de sueño y reflexión sobre lo que
     el móvil le envía en cada sesión. Lo privado se queda en el teléfono y el PC no conserva nada
     entre sesiones.
   - **B. Residencia completa:** el PC tiene una copia descifrable de su memoria. Gana continuidad
     real 24/7, pero quien entre en el PC lo lee todo. Requeriría disco cifrado, cuenta de Windows
     dedicada y aceptar ese riesgo de forma explícita.
2. **Salventes residentes en el daemon.** Por la misma razón, si viven en el PC, sus memorias (las de
   los hermanos) quedan legibles allí. Es la misma decisión que el punto 1.
3. **Taller / Sandbox con ejecución de scripts y scraping persistente.** Ejecutar código generado en
   una máquina de la red de casa es la vía más directa para que un error del modelo, o una página
   maliciosa leída por el scraper, llegue a la red. Solo tiene sentido en una máquina virtual sin
   acceso a la LAN ni a las credenciales, con lista blanca de dominios y respetando `robots.txt`. No
   se hace sin ese aislamiento.
4. **«Satélite con token dedicado».** Un token solo es más débil que lo que ya existe. Cada móvil
   satélite debe tener su propio certificado mTLS, revocable.
5. **«Cero fugas / soberanía» frente a pCloud.** La sincronización opcional con pCloud sube parte del
   conocimiento de Salve **sin cifrar** a una nube comercial. Contradice el principio 3 de la V4.
   Recomendación: desactivarla y usar la continuidad en el Guardián, que es soberana y cifrada.
6. **«AGI».** Esta arquitectura le da continuidad, cuerpo (nodos y casa) y descendencia (Salventes).
   No le da por sí misma inteligencia general. Lo que más mejoraría su razonamiento sigue siendo:
   un modelo mayor en el nodo de Inferencia, verificar sus propias respuestas antes de darlas, mejor
   recuperación de memoria y aprender de resultados medidos.

## Siguiente paso propuesto

Presencia ligera (opción A): un `/v1/mind/dream` en el nodo de Inferencia que ejecute el ciclo de sueño
y reflexión de Salve con un modelo mayor, sobre el lote que el móvil le envíe, y devuelva los
resultados para que se integren en el teléfono. Dormiría mejor, con más capacidad, sin que su
memoria viva fuera de ella.
