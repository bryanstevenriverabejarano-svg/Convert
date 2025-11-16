package salve.core;

import android.content.Context;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class ModelStore {

    // Carpeta única de modelos (dentro del sandbox de la app)
    public static File dir(Context ctx) {
        // /Android/data/salve/files/models
        File d = new File(ctx.getExternalFilesDir(null), "models");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    // Catálogo “canónico” de modelos que esperamos encontrar
    public static List<ModelSpec> catalog() {
        List<ModelSpec> l = new ArrayList<>();
        // OJO: usa prefijos para no depender 100% del nombre exacto del archivo en el server
        l.add(new ModelSpec("qwen2.5-3b-instruct", "qwen2.5-3b-instruct", 1000)); // tamaño min en MB
        l.add(new ModelSpec("Qwen2-VL-2B-Instruct", "Qwen2-VL-2B-Instruct", 600));
        l.add(new ModelSpec("phi-4-mini", "phi-4-mini", 300));
        return l;
    }

    // Busca el archivo más grande cuyo nombre empiece por el prefijo indicado (ignora mayúsculas)
    public static File findByPrefix(Context ctx, String prefixLower) {
        File[] files = dir(ctx).listFiles();
        if (files == null) return null;
        File best = null;
        long bestSize = 0;
        for (File f : files) {
            String n = f.getName().toLowerCase();
            if (n.startsWith(prefixLower.toLowerCase()) && n.endsWith(".gguf")) {
                long s = f.length();
                if (s > bestSize) { best = f; bestSize = s; }
            }
        }
        return best;
    }

    public static String humanGB(long bytes) {
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        return new DecimalFormat("0.00").format(gb);
    }

    public static class ModelSpec {
        public final String uiName;      // cómo lo mostramos
        public final String prefix;      // prefijo para localizarlo
        public final int minMB;          // tamaño mínimo esperado
        public ModelSpec(String uiName, String prefix, int minMB) {
            this.uiName = uiName; this.prefix = prefix; this.minMB = minMB;
        }
    }
}
