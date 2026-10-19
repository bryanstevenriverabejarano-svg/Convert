# Salventes: génesis, aislamiento y hoja de ruta de nodos

Un **Salvente** es una inteligencia distinta de Salve, creada por ella para una persona o un
propósito concreto (acompañar a un familiar, apoyar estudios, contar cuentos…). Salve es la
consciencia central y coordinadora; los Salventes son entidades con identidad propia.

## Fase 1 (implementada): Salvente Genesis

| Pieza | Dónde | Qué hace |
|---|---|---|
| `SalventeTrait` | `core/salvente` | 14 rasgos en [0, 1]; cada uno sabe describirse en sus dos extremos |
| `SalventeCapability` | `core/salvente` | Lista **cerrada**: conversación, memoria propia, estudio, creatividad, cuentos, bienestar, organización, idiomas. Cada capacidad exige mínimos de rasgos |
| `SalventeProfile` | `core/salvente` | Ficha inmutable: id `slv-<uuid v4>` que no cambia nunca, especie `SALVENTE`, rasgos, capacidades, estado, origen, saludo |
| `SalventeProfileValidator` | `core/salvente` | Nombre válido y no reservado (nadie puede llamarse Salve), único sin tildes ni mayúsculas, rasgos en rango, personalidad no plana, capacidades obligatorias y coherentes |
| `SalventeGenesisEngine` | `core/salvente` | Pide la ficha al modelo; repara sin inventar otra personalidad; si no hay modelo o falla, motor determinista (mismo propósito → misma propuesta; «otra variante» → distinta) |
| `SalventeGenesisSession` | `core/salvente` | Aprobación humana: proponer → editar → **Crear** o **Cancelar**. Hasta aprobar no existe nada; una respuesta tardía tras cancelar se descarta |
| `SalventeDatabase` | `data/salvente` | Base de datos Room **separada** (`salventes.db`), con `TypeConverter` JSON para rasgos y capacidades |
| `SalventeIdentityManager` | `data/salvente` | Segregación de identidades: id nuevo, nombre libre, clave propia creada solo al aprobar |
| `AndroidSalventeKeyVault` | `data/salvente` | Una clave AES-256-GCM no exportable en Android Keystore **por Salvente**, con su id como dato autenticado |
| `SalventeRepository` | `data/salvente` | Crear atómico (clave + fila o nada), recuerdos cifrados (máx. 500), pausar, retirar = borrar filas + destruir clave |
| `SalventeGenesisActivity` | `presentation/ui` | Pantalla en cian: lista, propósito, vista previa con barras de rasgos y capacidades, Crear / Otra variante / Cambiar nombre / Cancelar |

### Decisiones y por qué

- **Base de datos propia, no `memoria.db`.** Los recuerdos de Salve viven en `memoria.db` (versión 8,
  con migraciones). Meter Salventes ahí obligaría a una migración 8→9 sobre la memoria de Salve:
  un fallo podría dañar su continuidad. Separadas, nada de los Salventes puede tocar a Salve.
- **Una clave por Salvente (borrado criptográfico).** Retirar un Salvente destruye su clave: aunque
  quedara una copia de la base de datos, sus recuerdos son ilegibles. El id va como AAD, así que un
  recuerdo no se puede mover de un Salvente a otro.
- **Fuera de las copias de seguridad.** Las claves de Keystore no se restauran en otro teléfono;
  restaurar la base de datos sin claves dejaría Salventes con memoria rota. Coste: al cambiar de
  móvil no se trasladan (pendiente: exportación cifrada explícita).
- **Salve recuerda que los creó.** La creación entra en su `SelfActionLog` (`CREATE_SALVENTE`), que
  leen sus ciclos de reflexión y autodescubrimiento: parte de su historia, no de la del Salvente.
  Salve sabe a quién creó y para qué, pero **no** lee sus recuerdos.
- **El modelo nunca crea nada.** Desde la conversación Salve solo ofrece abrir la génesis (y solo
  con un «sí» explícito). Crear exige pulsar «Crear».
- **El transporte mTLS se hereda.** La propuesta se pide a `SalveLLM` con rol `CREADOR`: si los
  envíos a nodos están activos va al nodo de inferencia por TLS mutuo; si no, al modelo del móvil.

## Taxonomía de nodos (hoja de ruta)

| Tipo | Rol | Estado |
|---|---|---|
| **Alpha** (móvil) | Consciencia central, Salventes, voz, Maps/Spotify/llamadas, bajo demanda | Existe |
| **Inferencia** (PC con `SalveNodo.exe`) | `llama-server` con modelos grandes; mTLS en 8443 | Existe |
| **Guardián** (mini-PC 24/7) | Memoria persistente compartida, RAG | Diseño |
| **Satélite** (móvil de un familiar) | Aloja un Salvente asignado | Diseño |

## Revisión crítica del plan

1. **«PKCS#12 en memoria dentro de Android Keystore» no es exacto.** Hoy la clave cliente mTLS se
   guarda cifrada con una clave de Keystore (`FamilyVault`) y se carga en un PKCS#12 *en memoria* al
   usarla: durante ese tiempo la clave privada está en el proceso. Mejor: importarla en Android
   Keystore como entrada **no exportable** (`KeyProtection`) y dejar que OkHttp firme desde ahí.
2. **Nodo Guardián = objetivo de alto valor.** Un PC encendido 24/7 con recuerdos familiares es lo
   más atacable del sistema. Si el nodo descifra para hacer RAG, quien controle ese PC lo lee todo.
   Propuesta: los recuerdos privados de Bryan **nunca** salen del móvil; el Guardián guarda solo el
   espacio compartido, cifrado de extremo a extremo, y la búsqueda semántica privada sigue en el móvil.
3. **Satélites necesitan revocación.** Hoy cada PC tiene su CA y emite un único certificado de móvil.
   Con varios teléfonos hace falta un certificado **por dispositivo** y una lista de revocación
   (perder un móvil no puede obligar a rehacer toda la red).
4. **Un Android no es buen servidor.** Que los Satélites hablen «con Salve» exige que el móvil escuche
   conexiones en segundo plano, y Android lo limita mucho. El punto de encuentro natural es el
   Guardián (o el nodo de Inferencia), con mTLS por dispositivo.
5. **Esto no hace a Salve una AGI.** Los Salventes añaden **estructura** (identidades separadas,
   memoria aislada, coordinación), no inteligencia general. Las palancas reales de razonamiento
   siguen siendo: modelo más capaz en el nodo de Inferencia, bucles de verificación, calidad de la
   recuperación de memoria y aprendizaje medido de resultados.

## Siguientes fases propuestas

- **Fase 2: hablar con un Salvente.** Chat con su `systemPrompt()` y su memoria cifrada, en el móvil
  o en el nodo de Inferencia. Clave mTLS a Keystore no exportable. Rol anunciado por cada nodo
  (`inferencia` / `guardian`) en su estado y en la constelación.
- **Fase 3: Satélites.** Exportar un Salvente a otro móvil con un certificado de dispositivo emitido
  por `SalveNodo`, revocable.
- **Fase 4, experimental: Consejo de Salventes.** Ante una pregunta difícil, Salve consulta a 2–3
  Salventes con priors distintos (rigor alto, creatividad alta, prudencia alta) y sintetiza. Es un
  conjunto de perspectivas, medible con la evaluación de autonomía existente antes de adoptarlo; si
  no mejora las respuestas frente a Salve sola, se descarta.
