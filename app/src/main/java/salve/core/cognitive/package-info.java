/**
 * Laboratorio cognitivo experimental, desconectado del recorrido de ejecución.
 *
 * <p>{@code CognitiveCore}, {@code LiquidNeuralLayer}, {@code ConceptSpace} y afines implementan
 * mecanismos reales (una red LTC con integración de Euler, aprendizaje hebbiano, espacio de
 * conceptos), pero sus entradas eran vectores aleatorios asignados a frases completas y su estado
 * no influía en ninguna respuesta. Por eso se retiraron del sueño y del ciclo de vida.</p>
 *
 * <p>La continuidad de Salve vive ahora en {@code salve.core.mind}: modelo de sí misma con
 * fuentes, sueño, reflexión y señal de sorpresa. Para reconectar este paquete con sentido:
 * alimentarlo con embeddings reales ({@code EmbeddingsIndex}) en lugar de vectores aleatorios y
 * darle una salida medible, por ejemplo integrar en el tiempo la serie de sorpresa de
 * {@code PredictionLoop} y comprobar si mejora la predicción frente a una media móvil.</p>
 *
 * <p>Excepción: {@code HipocampoSemantico} (con {@code EnciclopediaUniversal}) sigue en uso por la
 * galería visual y la conversación.</p>
 */
package salve.core.cognitive;
