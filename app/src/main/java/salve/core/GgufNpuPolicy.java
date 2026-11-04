package salve.core;

import java.util.Locale;

/** Conservative Hexagon NPU admission for GGUF. No remote inference is involved. */
public final class GgufNpuPolicy {
    private GgufNpuPolicy() { }

    /**
     * SM8650 is Snapdragon 8 Gen 3 / Hexagon v75. Q4_0 is the GGUF quantization
     * with broad documented HTP kernel support. Other GGUF files remain on
     * Adreno/CPU until validated on hardware; filename is only a safety gate,
     * never proof that a backend is actually running.
     */
    public static boolean eligible(String socModel, String path) {
        if (socModel == null || path == null) return false;
        String soc = socModel.toUpperCase(Locale.ROOT);
        String name = new java.io.File(path).getName().toUpperCase(Locale.ROOT);
        return (soc.equals("SM8650") || soc.startsWith("SM8650-"))
                && name.endsWith("-Q4_0.GGUF");
    }
}
