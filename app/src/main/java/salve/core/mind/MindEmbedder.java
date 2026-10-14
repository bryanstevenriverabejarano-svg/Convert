package salve.core.mind;

/** Vector de significado normalizado (L2) para un texto. */
public interface MindEmbedder {
    float[] embed(String text) throws Exception;
}
