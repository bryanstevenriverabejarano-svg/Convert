# Carga de LLM y RAM en Salve

El primer modelo del catálogo es Dolphin 3.0 Llama 3.1 8B Q4_K_M: su archivo tiene 4 920 749 472 bytes (unos 4921 MB). Esto no es su contexto ni toda la memoria que necesita para funcionar. El motor usaba siempre 4096 tokens de contexto; ahora puede trabajar también con 2048.

## Selección y recuperación

1. Antes de descargar un candidato se comprueba la RAM disponible. Sin un motor residente, si ni su configuración mínima cabe según la estimación, se omite su transferencia y se prueba el siguiente candidato. Los archivos existentes no se borran.
2. Si ya hay un motor cargado, esa comprobación preliminar se difiere: no se cargan al mismo tiempo los pesos de dos modelos en la estimación. La activación libera el motor anterior y comprueba de nuevo la RAM antes de cargar el candidato.
3. Para Dolphin se elige 4096 o 2048 tokens según el margen disponible. Se incluyen los pesos, una estimación de memoria de trabajo y al menos 768 MiB de reserva, o el umbral superior indicado por Android.
4. Si falla específicamente la reserva del contexto nativo de 4096, se prueba 2048 una única vez. Otros errores, cancelaciones y entradas demasiado largas no se disfrazan de falta de memoria ni desencadenan ese reintento.
5. El tokenizador vuelve a comprobar el mensaje completo. No se recorta silenciosamente para hacerlo caber. Los generadores de conversación y agente reciben un presupuesto de entrada menor para los turnos siguientes.
6. Si no hay margen, se mantiene la secuencia Dolphin 8B → Dolphin 3B → Gemma. Cada activación sigue exigiendo una respuesta real antes de guardarse como correcta. La vigilancia de presión de RAM durante carga/inferencia se aplica ahora a ambos Dolphin y a GGUF importados.

Las estimaciones no son mediciones ni garantías de funcionamiento. Reducir el contexto no reduce el tamaño de los pesos: no hace caber un 8B de 4,92 GB en cuatro GiB libres. LiteRT/Gemma mantiene su configuración de contexto; no se le aplica el reintento nativo de llama.cpp.

## Diagnóstico desde el móvil

Abre **IA y cámara → Probar modelo local**. El resultado muestra el modelo final, su estado, la RAM libre/total, las estimaciones de inicio, el contexto configurado y el último fallo, si existe. El **Registro de modelos** incluye el mismo diagnóstico. La comprobación de RAM no usa el límite del heap Java ni confunde espacio de almacenamiento con memoria.

Para evaluar el S24 Ultra, conserva el texto de esa prueba. Permitirá distinguir falta de RAM, fallo de archivo/runtime, tiempo agotado y entrada que supera el contexto. Una compilación correcta no demuestra que los pesos completos se hayan cargado y respondido en ese teléfono.

## Validación

- Límites de RAM para 4096/2048, reserva de Android y pesos ya residentes.
- Omisión del 8B antes de transferirlo cuando solo cabe el 3B, conservando el orden de respaldo.
- Reintento nativo acotado, cancelación entre intentos y rechazo explícito de mensajes que no caben.
- Comprobación de sintaxis C++ con la cabecera JNI generada por Java para detectar desajustes de firma.
- Pruebas Android y compilación ARM64 del motor nativo en CI.

No se ha ejecutado una inferencia de los tres modelos completos en un S24 Ultra durante esta revisión. La validación real de RAM, velocidad y calidad debe hacerse en el dispositivo.
