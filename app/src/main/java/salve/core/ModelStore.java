package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Gestión de modelos locales:
 *  - Carpeta /Android/data/<pkg>/files/models
 *  - Catálogo de modelos (pensamiento, coder, pequeño)
 *  - Búsqueda por prefijo
 *  - Soporte para:
 *      * carpetas MLC
 *      * .gguf sueltos
 *      * .zip con el modelo dentro
 */
public final class ModelStore {

    private static final String TAG = "ModelStore";

    // Carpeta única de modelos (dentro del sandbox de la app)
    public static File dir(Context ctx) {
        // /Android/data/com.salve.salve/files/models   (en tu app real es com.salve.app)
        File d = new File(ctx.getExternalFilesDir(null), "models");
        if (!d.exists()) d.mkdirs();
        return d;
    }

    // Catálogo “canónico” de modelos que esperamos encontrar
    public static List<ModelSpec> catalog() {
        List<ModelSpec> l = new ArrayList<>();

        // Pensamiento / razonamiento general
        l.add(new ModelSpec(
                "Qwen2.5 3B Instruct",
                "qwen2.5-3b-instruct",   // prefijo
                1500                     // tamaño mínimo en MB (ajustable)
        ));

        // Programación / herramientas
        l.add(new ModelSpec(
                "Qwen2.5 3B Coder",
                "qwen2.5-coder-3b-instruct",
                1500
        ));

        // Modelo más pequeño / rápido
        l.add(new ModelSpec(
                "Phi-4 mini",
                "phi-4-mini",
                500
        ));

        return l;
    }

    /**
     * Busca el artefacto más grande compatible cuyo nombre empiece por el prefijo indicado.
     * AHORA:
     *  1) Si existen carpetas MLC válidas (mlc-chat-config.json + params_shard_0.bin),
     *     se prefieren SIEMPRE.
     *  2) Si no hay carpeta, se busca .zip o .gguf como antes.
     */
    public static File findByPrefix(Context ctx, String prefixLower) {
        File base = dir(ctx);
        File[] files = base.listFiles();
        if (files == null) return null;

        String pref = prefixLower.toLowerCase();

        // --- 1) PRIMERO: buscar carpeta MLC válida ---
        File bestMLC = null;
        long bestMLCSize = 0;
        for (File f : files) {
            if (!f.isDirectory()) continue;
            String n = f.getName().toLowerCase();
            if (!n.startsWith(pref)) continue;
            if (!looksLikeMLCFolder(f)) continue;

            long size = folderSize(f);
            if (size > bestMLCSize) {
                bestMLC = f;
                bestMLCSize = size;
            }
        }
        if (bestMLC != null) {
            Log.d(TAG, "findByPrefix: usando carpeta MLC " + bestMLC.getAbsolutePath());
            return bestMLC;
        }

        // --- 2) SI NO HAY CARPETA MLC, fallback a .zip / .gguf / carpeta genérica (comportamiento antiguo) ---
        File best = null;
        long bestSize = 0;

        for (File f : files) {
            String n = f.getName().toLowerCase();

            boolean matchPrefix = n.startsWith(pref);
            boolean okExt =
                    f.isDirectory() ||
                            n.endsWith(".gguf") ||
                            n.endsWith(".zip");

            if (!matchPrefix || !okExt) continue;

            long size = f.isDirectory() ? folderSize(f) : f.length();
            if (size > bestSize) {
                best = f;
                bestSize = size;
            }
        }

        if (best != null) {
            Log.d(TAG, "findByPrefix: usando artefacto " + best.getAbsolutePath());
        }

        return best;
    }

    /**
     * Asegura que el modelo esté descomprimido en una carpeta.
     * Si:
     *  - ya es carpeta → se devuelve tal cual (y si es MLC, perfecto).
     *  - es .gguf → se devuelve el propio archivo.
     *  - es .zip → se descomprime (si hace falta) y se devuelve la carpeta.
     */
    public static File ensureModelFolder(Context ctx, File artifact, String prefixLower) throws Exception {
        if (artifact == null) return null;

        if (artifact.isDirectory()) {
            // si ya es carpeta MLC está lista
            if (looksLikeMLCFolder(artifact)) {
                Log.d(TAG, "ensureModelFolder: carpeta MLC detectada: " + artifact.getAbsolutePath());
            } else {
                Log.d(TAG, "ensureModelFolder: carpeta genérica: " + artifact.getAbsolutePath());
            }
            return artifact;
        }

        String name = artifact.getName().toLowerCase();

        // Si es .gguf suelto, lo usamos directamente (tu motor ya sabe leerlo)
        if (name.endsWith(".gguf")) {
            Log.d(TAG, "ensureModelFolder: usando .gguf " + artifact.getAbsolutePath());
            return artifact;
        }

        // Si es ZIP → descomprimir si aún no está
        if (name.endsWith(".zip")) {
            return unzipModelIfNeeded(ctx, artifact, prefixLower);
        }

        // Cualquier otro tipo: lo devolvemos tal cual (mejor que null)
        return artifact;
    }

    /**
     * Descomprime un ZIP de modelo en /models/<nombre_sin_zip>/ si no existe aún.
     * Devuelve la carpeta resultante.
     */
    public static File unzipModelIfNeeded(Context ctx, File zipFile, String prefixLower) throws Exception {
        if (zipFile == null || !zipFile.exists()) return null;

        File modelsDir = dir(ctx);

        // Carpeta objetivo: /models/<prefijo>/
        String baseName = prefixLower;
        File targetDir = new File(modelsDir, baseName);

        // Si ya existe y tiene contenido, lo damos por bueno
        if (targetDir.exists() && targetDir.isDirectory() && targetDir.listFiles() != null
                && targetDir.listFiles().length > 0) {
            Log.d(TAG, "Modelo ya descomprimido en: " + targetDir.getAbsolutePath());
            ModelConsoleOverlay.log("✅ Modelo listo: " + baseName);
            return targetDir;
        }

        // Mostrar en la consola hacker
        ModelConsoleOverlay.show();
        ModelConsoleOverlay.log("📦 Descomprimiendo " + zipFile.getName() + "…");

        // Asegurar carpeta limpia
        if (!targetDir.exists() && !targetDir.mkdirs()) {
            throw new RuntimeException("No se pudo crear carpeta de modelo: " + targetDir.getAbsolutePath());
        }

        // Descomprimir
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry entry;
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                File outFile = new File(targetDir, entryName);
                if (entry.isDirectory()) {
                    if (!outFile.exists() && !outFile.mkdirs()) {
                        throw new RuntimeException("No se pudo crear subcarpeta: " + outFile.getAbsolutePath());
                    }
                } else {
                    File parent = outFile.getParentFile();
                    if (parent != null && !parent.exists() && !parent.mkdirs()) {
                        throw new RuntimeException("No se pudo crear carpeta padre: " + parent.getAbsolutePath());
                    }

                    try (FileOutputStream fos = new FileOutputStream(outFile)) {
                        int n;
                        while ((n = zis.read(buf)) > 0) {
                            fos.write(buf, 0, n);
                        }
                    }
                }
                zis.closeEntry();
            }
        }

        Log.d(TAG, "Modelo descomprimido en: " + targetDir.getAbsolutePath());
        ModelConsoleOverlay.log("✅ Modelo descomprimido: " + baseName);
        // escondemos la consola un ratito después
        ModelConsoleOverlay.hideDelayed(1200);

        return targetDir;
    }

    // ---------- helpers ----------

    /**
     * Devuelve true si la carpeta tiene pinta de modelo MLC:
     *   - mlc-chat-config.json
     *   - params_shard_0.bin
     */
    private static boolean looksLikeMLCFolder(File dir) {
        if (dir == null || !dir.isDirectory()) return false;
        File config = new File(dir, "mlc-chat-config.json");
        File shard0 = new File(dir, "params_shard_0.bin");
        return config.exists() && shard0.exists();
    }

    // Tamaño total de una carpeta (para elegir "la más grande")
    private static long folderSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long sum = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;
        for (File f : files) {
            if (f.isDirectory()) sum += folderSize(f);
            else sum += f.length();
        }
        return sum;
    }

    public static String humanGB(long bytes) {
        double gb = bytes / (1024.0 * 1024.0 * 1024.0);
        return new DecimalFormat("0.00").format(gb);
    }

    // === Descriptor de modelo ===
    public static class ModelSpec {
        public final String uiName;      // cómo lo mostramos
        public final String prefix;      // prefijo para localizarlo
        public final int minMB;          // tamaño mínimo esperado

        public ModelSpec(String uiName, String prefix, int minMB) {
            this.uiName = uiName;
            this.prefix = prefix;
            this.minMB = minMB;
        }
    }
}
