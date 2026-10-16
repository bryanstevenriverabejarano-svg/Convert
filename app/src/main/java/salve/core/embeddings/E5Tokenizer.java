package salve.core.embeddings;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** XLM-R ids from upstream SentencePiece, with canonical Hugging Face XLM-R special-token handling. */
public final class E5Tokenizer implements AutoCloseable {
    static { System.loadLibrary("salve_sentencepiece"); }
    public static final int MAX_TOKENS = 256;
    public static final int PAD_ID = 1;
    private long handle;

    public E5Tokenizer(File model) {
        handle = nativeOpen(model.getAbsolutePath());
        if (handle == 0) throw new IllegalStateException("No se pudo cargar el tokenizador E5");
    }
    /** No padding: the attention mask follows sequence positions, including literal special tokens. */
    public synchronized int[] encode(String text, int maxTokens) {
        if (handle == 0) throw new IllegalStateException("Tokenizador cerrado");
        Objects.requireNonNull(text, "text");
        return nativeEncode(handle, text.getBytes(StandardCharsets.UTF_8), maxTokens);
    }
    public int[] query(String text) { return encode("query: " + bounded(text), MAX_TOKENS); }
    public int[] passage(String text) { return encode("passage: " + bounded(text), MAX_TOKENS); }
    private static String bounded(String text) {
        Objects.requireNonNull(text, "text");
        int end = Math.min(text.length(), 32768);
        if (end < text.length() && Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end);
    }
    @Override public synchronized void close() {
        if (handle != 0) { nativeClose(handle); handle = 0; }
    }
    private static native long nativeOpen(String path);
    private static native int[] nativeEncode(long handle, byte[] utf8, int maxTokens);
    private static native void nativeClose(long handle);
}
