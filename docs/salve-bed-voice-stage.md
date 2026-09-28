# Cama ilustrada y conversación de voz en el escenario

La cama usa `app/src/main/assets/avatar/furniture/salve_bed.png` (1962 × 801 RGBA, 1.43 MB). Se creó con la herramienta integrada de generación de imágenes. El PNG original se conserva sin modificar: una sola textura compartida entre pantalla principal, habitación y ventana flotante. El renderer vuelve a pintar la zona de edredón delante de Salve para cubrirla al acostarse; el núcleo y las 20 vistas siguen intactos.

La escena de descanso se eleva hacia el centro del teléfono. La representación de Salve sigue siendo la malla frontal 2D existente.

## Voz

`LiveVoiceSession` sustituye al diálogo modal. El estado y las acciones Interrumpir/Terminar ocupan espacio debajo del avatar, sin oscurecerlo ni superponer una ventana. Al iniciar una conversación válida, Salve se despierta para que sean visibles sus gestos de voz. Los turnos usan el mismo motor, métricas, límite de diez minutos y protección contra eco anteriores.

Tocar el estado abre los detalles completos en el panel de conversación. Los errores de reconocimiento (incluidos los datos de idioma ausentes que aparecen en la captura) conservan su explicación y una acción de reintento; el micrófono permanece cerrado tras el error. Este cambio de interfaz no instala los paquetes de reconocimiento de Android ni cambia silenciosamente a un proveedor remoto.

Volver cierra primero los paneles y después la sesión. Terminar, salir de la actividad y empezar una entrada escrita/dictada cierran la captura y cancelan los turnos pendientes. Los mensajes reconocidos se añaden al chat compartido; la respuesta del motor conserva su publicación habitual sin duplicarla.

## Revisión en teléfono

1. Pedir «cama», después «acuéstate» y «guarda la cama»; comprobar personaje, almohada y edredón.
2. Abrir voz desde el menú: ver a Salve completa mientras escucha y habla, sin ventana blanca.
3. Interrumpir una respuesta; comprobar que termina la reproducción antes de abrir el micrófono.
4. Ver detalles del estado, volver al personaje y terminar la sesión. Probar también Inicio, giro del teléfono y enviar texto durante voz.
5. Con datos de idioma ausentes, comprobar mensaje completo, reintento y micrófono cerrado.

## Prompt final del recurso

Use case: stylized-concept. Asset type: production game furniture sprite for an Android anime companion. Create ONE empty futuristic single bed for Salve, a silver-haired anime android whose body is glossy pearl-white ceramic with obsidian black inserts and luminous electric-blue seams. Match high-quality anime game illustration with delicate contours, smooth detailed shaded materials and restrained realistic highlights, NOT a flat cartoon icon. Furniture only, no character. Landscape image, pure transparent background with clean alpha, entire object fully visible with little empty margin. Exact composition: long horizontal bed in a near-orthographic SIDE VIEW, head/pillow on LEFT, feet on RIGHT; slight view down onto the mattress, not deep isometric perspective. The mattress top is horizontal. Elegant sculpted pearl-white chassis, thin black metallic undershell, small blue light strips and angular diamond/ear-like motifs inspired by a cybernetic android. A comfortable pale silver pillow on the left, softly folded silver-white duvet with understated blue piping covering the right TWO THIRDS of the mattress and draping down the near-facing side. Rich soft fabric folds so the bed feels welcoming rather than a medical pod. Low futuristic tapered supports. No tall footboard, no enclosing capsule, no canopy. Keep headboard low so a character can be composited lying horizontally with head at the left; long clear horizontal sleeping surface. Transparent background, NO room, NO floor plane, NO text, NO watermark, NO people, NO separate loose objects. One cohesive finished sprite, not a collage or sprite sheet.
