package salve.core.mind;

import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Descripción de un adaptador LoRA personal ({@code salve-lora.json} junto a {@code salve-lora.gguf}).
 *
 * <pre>{"base":"Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf","scale":0.8,"sha256":"…","trainedOn":"2026-10-20"}</pre>
 *
 * <p>Un adaptador solo es válido para el modelo base con el que se entrenó; se exige el nombre del
 * archivo base y el SHA-256 del adaptador para no aplicar uno corrupto o de otro modelo.</p>
 */
public final class AdapterManifest {
    public final String base;
    public final float scale;
    public final String sha256;
    public final String trainedOn;

    private AdapterManifest(String base, float scale, String sha256, String trainedOn) {
        this.base = base; this.scale = scale; this.sha256 = sha256; this.trainedOn = trainedOn;
    }

    public static AdapterManifest parse(String json) {
        JsonObject o = MindJson.firstObject(json, 4096);
        String base = MindJson.text(o, "base", 255, true);
        String sha = MindJson.text(o, "sha256", 64, true).toLowerCase(Locale.ROOT);
        if (!sha.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("sha256 inválido.");
        double scale = MindJson.number(o, "scale", 1.0, 0.0, 2.0);
        return new AdapterManifest(base, (float) scale, sha, MindJson.text(o, "trainedOn", 40, false));
    }

    /** null si el adaptador puede aplicarse a este modelo; si no, el motivo. */
    public String rejectionFor(String baseModelPath, File adapter) {
        if (baseModelPath == null) return "No hay modelo base cargado.";
        if (!new File(baseModelPath).getName().equals(base))
            return "El adaptador se entrenó para " + base + ", no para " + new File(baseModelPath).getName() + ".";
        if (adapter == null || !adapter.isFile()) return "Falta el archivo del adaptador.";
        try {
            if (!sha256(adapter).equals(sha256)) return "El SHA-256 del adaptador no coincide.";
        } catch (Exception e) {
            return "No se pudo leer el adaptador.";
        }
        return null;
    }

    static String sha256(File file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (InputStream in = new FileInputStream(file)) {
            byte[] buffer = new byte[1 << 16];
            int n;
            while ((n = in.read(buffer)) > 0) digest.update(buffer, 0, n);
        }
        StringBuilder out = new StringBuilder();
        for (byte b : digest.digest()) out.append(String.format(Locale.ROOT, "%02x", b));
        return out.toString();
    }
}
