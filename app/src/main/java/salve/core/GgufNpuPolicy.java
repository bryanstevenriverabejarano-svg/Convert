package salve.core;

import java.util.Locale;
import java.util.regex.Pattern;

/** Conservative Hexagon NPU admission for GGUF. No remote inference is involved. */
public final class GgufNpuPolicy {
    /**
     * Quantizations with HTP kernels in the pinned llama.cpp (HTP_TYPE_Q4_0/Q8_0/Q4_K/Q6_K in
     * ggml-hexagon.cpp). K-quant variants with suffixes (Q4_K_M, Q4_K_S, HauhauCS Q4_K_P…) are
     * mostly Q4_K/Q6_K tensors (Salve's own downloads add a commit suffix: «-Q4_K_M-45b6a33.gguf»); anything the HTP cannot run falls back through warm-up to OpenCL/CPU.
     */
    private static final Pattern SUPPORTED = Pattern.compile("(?:^|[-_.])(?:Q4_0|Q8_0|Q4_K(?:_[A-Z])?|Q6_K(?:_[A-Z])?)(?:-[0-9A-F]{6,40})?\\.GGUF$");

    private GgufNpuPolicy() { }

    /**
     * SM8650 is Snapdragon 8 Gen 3 / Hexagon v75. The filename is only a safety gate, never proof
     * that a backend is actually running: the native warm-up confirms it.
     */
    public static boolean eligible(String socModel, String path) {
        if (socModel == null || path == null) return false;
        String soc = socModel.toUpperCase(Locale.ROOT);
        String name = new java.io.File(path).getName().toUpperCase(Locale.ROOT);
        return (soc.equals("SM8650") || soc.startsWith("SM8650-")) && SUPPORTED.matcher(name).find();
    }
}
