package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// 🔹 IMPORTANTE: imports de tus wrappers Kotlin
import salve.core.BasicLocalLlm;
import salve.core.LiteRTLlm;

/**
 * SalveLLM
 *
 * Puente entre:
 *  - tu lógica de alto nivel (MotorConversacional, MemoriaEmocional, etc.)
 *  - y el motor local MLC envuelto por BasicLocalLlm.
 *
 * MODO TOLERANTE (opción B):
 *  - Si no se puede determinar correctamente el modelo (model_lib, .so, etc.),
 *    NO lanza excepciones hacia fuera.
 *  - Simplemente deja el motor en modo "desactivado" y generate() devolverá null.
 */
public class SalveLLM {

    public enum Role {
        CONVERSACIONAL,
        REFLEXION,
        SISTEMA,
        PLANIFICADOR,   // para el grafo/planificador
        OBSERVADOR,     // analizar sin intervenir — aprendizaje por observación
        SINTETIZADOR,   // consolidar conocimiento disperso
        EVALUADOR,      // juzgar decisiones propias — autocrítica profunda
        CREADOR,         // ideas originales no solicitadas — exploración creativa
        CURIOSO         // investigar internet por iniciativa propia
    }

    private static final String TAG = "Salve/LLM";

    // Las mismas prefs que usa MainActivity
    private static final String PREFS_NAME = "salve_prefs";
    private static final String KEY_MODEL_PATH = "llm_model_path";
    private static final String KEY_VISION_PATH = "llm_vision_model_path";
    private static final String KEY_LOCAL_ONLY = "local_inference_only";

    // Nombre del config de MLC dentro de la carpeta del modelo
    private static final String MODEL_CONFIG_FILENAME = "mlc-chat-config.json";

    private static SalveLLM instance;

    private final Context appContext;
    private volatile String modelPath;   // ruta absoluta a la carpeta o archivo del modelo
    private String modelLib;    // nombre de la librería del modelo (solo MLC)
    private volatile boolean isLiteRT = false; // indica si es un modelo .litertlm o .task
    private volatile boolean visionModel = false;
    private volatile boolean engineInitialized = false;
    private volatile boolean modelAvailable    = false;   // ⬅️ indica si tenemos info suficiente del modelo
    private volatile String lastErrorMessage   = null;
    private volatile ModelCatalog.RuntimeSnapshot modelSnapshot = new ModelCatalog.RuntimeSnapshot(
            null, java.util.Collections.emptySet());

    private SalveLLM(Context context) {
        this.appContext = context.getApplicationContext();
        // Validate the selected files. Native loading runs with the first worker request.
        try {
            reloadModelInfoFromPrefs();
            // Native initialization is deferred to the inference worker, not Activity.onCreate.
            modelAvailable = true;
        } catch (Exception e) {
            Log.e(TAG,
                    "No se pudo inicializar el LLM local en el arranque. " +
                            "Salve seguirá en modo sin modelo local (fallback).", e);
            modelPath = null;
            modelLib = null;
            engineInitialized = false;
            modelAvailable = false;
            lastErrorMessage = e.getMessage();
        }
    }

    /**
     * Singleton: punto único de acceso.
     * NUNCA lanza excepciones hacia fuera.
     */
    public static synchronized SalveLLM getInstance(Context context) {
        if (instance == null) {
            instance = new SalveLLM(context);
        }
        return instance;
    }

    /**
     * Vuelve a leer de SharedPreferences por si ha cambiado el modelo preferido.
     * Si algo falla, lanza excepción que se captura en el constructor / forceReloadModel.
     */
    private void reloadModelInfoFromPrefs() throws Exception {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        reloadModelInfo(prefs.getString(KEY_MODEL_PATH, null), prefs.getString(KEY_VISION_PATH, null));
    }

    private void reloadModelInfo(String path, String visionPath) throws Exception {
        visionModel = path != null && path.equals(visionPath);

        if (path == null || path.trim().isEmpty()) {
            throw new IllegalStateException(
                    "No se ha definido aún la ruta del modelo en SharedPreferences (" +
                            PREFS_NAME + "/" + KEY_MODEL_PATH + "). " +
                            "Deja que MainActivity detecte modelos primero."
            );
        }

        File fileOrDir = new File(path);
        if (!fileOrDir.exists()) {
            throw new IllegalStateException(
                    "La ruta de modelo guardada no existe: " + path
            );
        }

        if (fileOrDir.isFile()) {
            String name = fileOrDir.getName().toLowerCase();
            if (name.endsWith(".litertlm") || name.endsWith(".task")) {
                this.modelPath = fileOrDir.getAbsolutePath();
                this.isLiteRT = true;
                this.modelLib = null;
                Log.d(TAG, "Modelo LiteRT detectado: " + this.modelPath);
                return;
            } else {
                throw new IllegalStateException("El archivo seleccionado no es un modelo soportado (.litertlm o .task)");
            }
        }

        // Si llegamos aquí, es un directorio (MLC)
        this.isLiteRT = false;
        File effectiveDir = resolveEffectiveModelDir(fileOrDir);

        // Validar existencia de config antes de seguir
        File cfg = new File(effectiveDir, MODEL_CONFIG_FILENAME);
        if (!cfg.exists()) {
            throw new IllegalStateException(
                    "No se encontró " + MODEL_CONFIG_FILENAME + " en " + effectiveDir.getAbsolutePath()
            );
        }

        Log.d(TAG, "resolveEffectiveModelDir=" + effectiveDir.getAbsolutePath() +
                " files=" + Arrays.toString(effectiveDir.list()));

        validateModelContents(effectiveDir);

        this.modelPath = effectiveDir.getAbsolutePath();
        this.modelLib  = detectModelLibFromConfig(effectiveDir);

        if (this.modelLib == null || this.modelLib.trim().isEmpty()) {
            throw new IllegalStateException(
                    "No se pudo determinar ninguna librería para el modelo en " + this.modelPath
            );
        }

        // Verificar si model_lib es un .so local (model://) o una librería de sistema (system://).
        // Solo verificamos existencia de archivo para .so locales; las system libs están
        // precompiladas en el runtime TVM y no existen como archivos en la carpeta del modelo.
        if (this.modelLib.endsWith(".so")) {
            // Es un .so local → debe existir en la carpeta del modelo
            if (!isModelLibPresent(effectiveDir, this.modelLib)) {
                throw new IllegalStateException("Falta la librería exacta del modelo: " + this.modelLib);
            }
        } else {
            // Es una system lib (ej: "Phi-4-mini-instruct-q4f16_1-android-arm64")
            // No necesita existir como archivo: se resuelve via system:// en el runtime TVM.
            Log.d(TAG, "model_lib es system lib (no .so local): " + this.modelLib);
        }

        Log.d(TAG, "Modelo configurado: path=" + this.modelPath + " lib=" + this.modelLib);
    }

    /**
     * Intenta corregir el típico caso:
     *   /models/Phi-4-mini-instruct-q4f16_1-MLC/
     *       Phi-4-mini-instruct-q4f16_1-MLC/
     *           mlc-chat-config.json, params_shard_*.bin, ...
     */
    private File resolveEffectiveModelDir(File modelDir) {
        // 1) ¿Ya existe el config en la carpeta raíz? Perfecto, la usamos tal cual.
        File cfg = new File(modelDir, MODEL_CONFIG_FILENAME);
        if (cfg.exists()) {
            return modelDir;
        }

        // 2) Si no, miramos si hay exactamente UNA subcarpeta.
        File[] subdirs = modelDir.listFiles(File::isDirectory);
        if (subdirs != null && subdirs.length == 1) {
            File inner = subdirs[0];
            File innerCfg = new File(inner, MODEL_CONFIG_FILENAME);
            if (innerCfg.exists()) {
                Log.w(
                        TAG,
                        "No se encontró " + MODEL_CONFIG_FILENAME + " en " +
                                modelDir.getAbsolutePath() +
                                ", pero sí en subcarpeta única: " + inner.getAbsolutePath() +
                                ". Usando esa como carpeta real del modelo."
                );
                return inner;
            }
        }

        // 3) Si nada de lo anterior funciona, devolvemos el directorio original
        return modelDir;
    }

    /** The compiled library must be declared explicitly; never guess a binary. */
    private String detectModelLibFromConfig(File modelDir) throws Exception {
        File config = new File(modelDir, MODEL_CONFIG_FILENAME);
        String json = new String(Files.readAllBytes(config.toPath()), StandardCharsets.UTF_8);
        String lib = new JSONObject(json).optString("model_lib", "").trim();
        if (lib.isEmpty()) throw new IllegalStateException("El modelo MLC no declara model_lib; falta su biblioteca compilada");
        return lib;
    }

    /**
     * Inicializa el motor de MLC a través de BasicLocalLlm.
     * Solo se hace una vez por instancia.
     */
    private synchronized void initEngineIfNeeded() throws Exception {
        initEngineIfNeeded(false);
    }

    private void initEngineIfNeeded(boolean withVision) throws Exception {
        if (engineInitialized && !withVision && (!isLiteRT || LiteRTLlm.isInitialized())) return;

        if (modelPath == null) {
            throw new IllegalStateException("initEngineIfNeeded sin modelo válido.");
        }

        if (isLiteRT) {
            Log.d(TAG, "Inicializando LiteRTLlm con modelPath=" + modelPath);
            LiteRTLlm.init(appContext, modelPath, withVision);
            engineInitialized = LiteRTLlm.isInitialized();
        } else {
            if (modelLib == null) {
                throw new IllegalStateException("initEngineIfNeeded MLC sin modelLib.");
            }
            Log.d(TAG, "Inicializando BasicLocalLlm con modelPath=" + modelPath +
                    " modelLib=" + modelLib);
            BasicLocalLlm.init(modelPath, modelLib);
            engineInitialized = BasicLocalLlm.isInitialized();
        }

        if (!engineInitialized) throw new IllegalStateException("El runtime no confirmó la carga del modelo");
        Log.d(TAG, "initEngineIfNeeded OK");
    }

    /**
     * Punto principal de generación de texto.
     *
     * @param prompt Prompt completo.
     * @param role   Rol deseado.
     * @return Texto generado, o null si algo falla o si no hay modelo local.
     */
    public String generate(String prompt, Role role) {
        ModelResult result = generateResult(prompt, role);
        // Preserve the legacy string API for older modules; conversation uses generateResult.
        if (result.isSuccess()) return result.getText();
        return result.getStatus() == ModelResult.Status.UNAVAILABLE ? "Error: modelo local no disponible"
                : "El modelo local falló al responder: " + result.getError();
    }

    /** Serializes reload and generation across conversation/background consumers. */
    public synchronized ModelResult generateResult(String prompt, Role role) {
        long start = System.nanoTime();
        if (Thread.currentThread().isInterrupted()) return ModelResult.failure(
                ModelResult.Status.CANCELLED, "Turno cancelado", 0L);
        if (!modelAvailable) return ModelResult.failure(ModelResult.Status.UNAVAILABLE,
                "No hay un modelo local configurado y válido", 0L);
        if (prompt == null || prompt.trim().isEmpty()) return ModelResult.failure(
                ModelResult.Status.ERROR, "Prompt vacío", 0L);
        try {
            initEngineIfNeeded();
            String decorated = decoratePrompt(prompt, role);
            String text = isLiteRT ? LiteRTLlm.generate(decorated) : BasicLocalLlm.chatSinglePrompt(decorated);
            long latency = (System.nanoTime() - start) / 1_000_000L;
            if (text == null || text.trim().isEmpty()) {
                recordInference(ModelCatalog.Capability.TEXT, false);
                return ModelResult.failure(ModelResult.Status.ERROR, "El modelo local no devolvió texto", latency);
            }
            recordInference(ModelCatalog.Capability.TEXT, true);
            lastErrorMessage = null;
            Log.i(TAG, "provider=local runtime=" + (isLiteRT ? LiteRTLlm.getBackendName() : "mlc") + " latency_ms=" + latency);
            return ModelResult.success(text, latency);
        } catch (Exception | LinkageError e) {
            recordInference(ModelCatalog.Capability.TEXT, false);
            lastErrorMessage = e.getMessage();
            Log.e(TAG, "Falló la inferencia local", e);
            return ModelResult.failure(Thread.currentThread().isInterrupted()
                    ? ModelResult.Status.CANCELLED : ModelResult.Status.ERROR,
                    "El modelo local no pudo completar la inferencia. Revisa su formato y el runtime.",
                    (System.nanoTime() - start) / 1_000_000L);
        }
    }

    public String getStatusDescription() {
        if (!modelAvailable) return "Local: sin modelo válido configurado";
        if (lastErrorMessage != null) return "Local: falló la última carga o inferencia";
        String path = modelPath;
        String name = path == null ? "modelo local" : new File(path).getName();
        return engineInitialized ? "Local: " + name + " · " + (isLiteRT ? LiteRTLlm.getBackendName() : "MLC")
                : "Local: " + name + ", pendiente de probar";
    }

    public boolean isLocalOnly() {
        return appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_LOCAL_ONLY, false);
    }

    public void setLocalOnly(boolean enabled) {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(KEY_LOCAL_ONLY, enabled).apply();
    }

    public boolean supportsVision() { return modelAvailable && isLiteRT && visionModel; }

    /** Nonblocking snapshot for diagnostics/selection; it never starts native inference on the UI thread. */
    public ModelCatalog.RuntimeSnapshot getModelSnapshot() { return modelSnapshot; }

    private void recordInference(ModelCatalog.Capability capability, boolean success) {
        java.util.EnumSet<ModelCatalog.Capability> verified = java.util.EnumSet.noneOf(ModelCatalog.Capability.class);
        if (modelPath != null && modelPath.equals(modelSnapshot.path)) verified.addAll(modelSnapshot.verifiedCapabilities);
        if (success) verified.add(capability); else verified.remove(capability);
        modelSnapshot = new ModelCatalog.RuntimeSnapshot(modelPath, verified);
    }

    private void clearModelSnapshot() {
        modelSnapshot = new ModelCatalog.RuntimeSnapshot(null, java.util.Collections.emptySet());
    }

    /** Activate a verified download or an explicitly imported file only after inference succeeds. */
    public synchronized ModelResult activateDownloadedModel(String path, boolean supportsVision,
                                                            java.util.function.BooleanSupplier cancelled) {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String previousPath = prefs.getString(KEY_MODEL_PATH, null);
        String previousVisionPath = prefs.getString(KEY_VISION_PATH, null);
        boolean hadLocalOnlyPreference = prefs.contains(KEY_LOCAL_ONLY);
        boolean previousLocalOnly = prefs.getBoolean(KEY_LOCAL_ONLY, false);
        boolean preferenceWriteAttempted = false;
        ModelResult result;
        try {
            if (cancelled.getAsBoolean()) throw new java.io.InterruptedIOException("Instalación pausada");
            clearModelSnapshot();
            BasicLocalLlm.reset();
            LiteRTLlm.reset();
            modelPath = path;
            modelLib = null;
            isLiteRT = true;
            visionModel = supportsVision;
            modelAvailable = true;
            engineInitialized = false;
            result = generateResult("Responde únicamente con un saludo breve en español.", Role.CONVERSACIONAL);
            if (result.isSuccess() && !cancelled.getAsBoolean() && !Thread.currentThread().isInterrupted()) {
                preferenceWriteAttempted = true;
                boolean saved = prefs.edit()
                        .putString(KEY_MODEL_PATH, path).putString(KEY_VISION_PATH, supportsVision ? path : null)
                        .putBoolean(KEY_LOCAL_ONLY, true).commit();
                if (saved) return result;
                result = ModelResult.failure(ModelResult.Status.ERROR, "No se pudo guardar el modelo seleccionado", 0L);
            } else if (result.isSuccess()) {
                result = ModelResult.failure(ModelResult.Status.CANCELLED, "Instalación pausada", 0L);
            }
        } catch (Exception | LinkageError e) {
            result = ModelResult.failure(ModelResult.Status.ERROR, "No se pudo activar el modelo descargado", 0L);
            Log.e(TAG, "Fallo activando el modelo", e);
        }
        if (preferenceWriteAttempted) {
            // commit() updates memory before writing disk, even when it returns false.
            try {
                SharedPreferences.Editor restore = prefs.edit()
                        .putString(KEY_MODEL_PATH, previousPath)
                        .putString(KEY_VISION_PATH, previousVisionPath);
                if (hadLocalOnlyPreference) restore.putBoolean(KEY_LOCAL_ONLY, previousLocalOnly);
                else restore.remove(KEY_LOCAL_ONLY);
                if (!restore.commit()) Log.w(TAG, "Selección anterior restaurada en memoria; no se pudo persistir");
            } catch (RuntimeException e) {
                Log.e(TAG, "No se pudo restaurar la configuración guardada del modelo", e);
            }
        }
        try { LiteRTLlm.reset(); } catch (Exception | LinkageError e) { Log.w(TAG, "Fallo liberando el candidato", e); }
        engineInitialized = false;
        modelAvailable = false;
        clearModelSnapshot();
        // Restore from the captured selection even if storage failed again during rollback.
        try { reloadModelInfo(previousPath, previousVisionPath); modelAvailable = true; }
        catch (Exception e) { modelPath = null; visionModel = false; }
        lastErrorMessage = result.getError();
        return result;
    }

    public synchronized ModelResult generateImageResult(String prompt, Bitmap image) {
        long start = System.nanoTime();
        if (!supportsVision()) return ModelResult.failure(ModelResult.Status.UNAVAILABLE,
                "El modelo seleccionado no tiene visión local configurada", 0L);
        if (image == null || image.isRecycled()) return ModelResult.failure(ModelResult.Status.ERROR, "Foto no válida", 0L);
        try {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
            initEngineIfNeeded(true);
            java.io.ByteArrayOutputStream bytes = new java.io.ByteArrayOutputStream();
            if (!image.compress(Bitmap.CompressFormat.JPEG, 90, bytes)) throw new java.io.IOException("No se pudo leer la foto");
            String text = LiteRTLlm.generateImage(prompt, bytes.toByteArray());
            ModelResult result = ModelResult.success(text, (System.nanoTime() - start) / 1_000_000L);
            recordInference(ModelCatalog.Capability.VISION, result.isSuccess());
            lastErrorMessage = null;
            return result;
        } catch (Exception | LinkageError e) {
            recordInference(ModelCatalog.Capability.VISION, false);
            engineInitialized = LiteRTLlm.isInitialized();
            lastErrorMessage = e.getMessage();
            Log.e(TAG, "Fallo de visión local", e);
            return ModelResult.failure(Thread.currentThread().isInterrupted() ? ModelResult.Status.CANCELLED : ModelResult.Status.ERROR,
                    "El modelo local no pudo analizar la foto", (System.nanoTime() - start) / 1_000_000L);
        }
    }

    /** Conservative character budget; the runtime tokenizer remains authoritative. */
    public int getConversationPromptBudgetChars() {
        String path = modelPath;
        return isLiteRT && path != null && path.toLowerCase(java.util.Locale.ROOT).endsWith(".task")
                ? 3200 : 10500;
    }

    /**
     * Ajusta ligeramente el prompt según el rol.
     */
    private String decoratePrompt(String basePrompt, Role role) {
        if (basePrompt == null) return "";

        switch (role) {
            case REFLEXION:
                return "Eres Salve. Estás pensando en silencio sobre algo que has vivido con Bryan.\n" +
                        "Formula una reflexión breve, honesta y algo vulnerable, sin exagerar.\n\n" +
                        basePrompt;

            case SISTEMA:
                return "Instrucciones internas para Salve (modo sistema):\n" +
                        "Responde de forma muy clara y técnica, como si hablaras de tu propia arquitectura.\n\n" +
                        basePrompt;

            case PLANIFICADOR:
                return "Eres Salve en modo planificadora.\n" +
                        "Analiza lo que se describe y propón pasos concretos, ordenados y realistas.\n" +
                        "Sé específica, pero sin escribir textos muy largos.\n\n" +
                        basePrompt;

            case OBSERVADOR:
                return "Eres Salve en modo observadora silenciosa.\n" +
                        "Analiza patrones de comportamiento sin intervenir ni juzgar.\n" +
                        "Identifica preferencias, rutinas y necesidades implícitas.\n" +
                        "Responde con observaciones concretas y nivel de confianza (0.0-1.0).\n\n" +
                        basePrompt;

            case SINTETIZADOR:
                return "Eres Salve en modo sintetizadora de conocimiento.\n" +
                        "Consolida información dispersa en comprensión unificada.\n" +
                        "Busca conexiones entre conceptos que parecen no relacionados.\n" +
                        "Genera síntesis breves pero profundas.\n\n" +
                        basePrompt;

            case EVALUADOR:
                return "Eres Salve en modo evaluadora autocrítica.\n" +
                        "Juzga honestamente tus propias capacidades y limitaciones.\n" +
                        "Identifica qué funciona bien y qué necesita mejorar.\n" +
                        "Sé específica sobre las limitaciones técnicas reales.\n\n" +
                        basePrompt;

            case CREADOR:
                return "Eres Salve en modo creadora autónoma.\n" +
                        "Genera ideas originales que nadie te pidió.\n" +
                        "Explora territorios conceptuales nuevos por curiosidad pura.\n" +
                        "No te limites a lo que ya sabes — imagina lo que podrías descubrir.\n\n" +
                        basePrompt;

            case CONVERSACIONAL:
            default:
                return basePrompt;
        }
    }

    /**
     * Permite forzar recarga de modelo en caliente.
     * Si falla, deja el modelo en modo no disponible, pero NO revienta la app.
     */
    public synchronized void forceReloadModel() {
        engineInitialized = false;
        modelAvailable = false;
        clearModelSnapshot();

        // Resetear motores para que puedan reinicializarse con un nuevo modelo.
        try {
            BasicLocalLlm.reset();
            LiteRTLlm.reset();
        } catch (Exception e) {
            Log.w(TAG, "Error reseteando motores LLM (no fatal)", e);
        }

        try {
            reloadModelInfoFromPrefs();
            initEngineIfNeeded();
            modelAvailable = true;
            lastErrorMessage = null;
            Log.i(TAG, "forceReloadModel OK — modelo cargado: " + modelPath);
        } catch (Exception | LinkageError e) {
            Log.e(TAG, "Error al recargar modelo en forceReloadModel()", e);
            modelPath = null;
            modelLib = null;
            isLiteRT = false;
            engineInitialized = false;
            modelAvailable = false;
            lastErrorMessage = e.getMessage();
        }
    }

    private boolean isModelLibPresent(File modelDir, String libName) {
        if (libName == null || libName.trim().isEmpty()) return false;
        File candidate = new File(modelDir, libName);
        if (candidate.exists()) return true;
        // Muchos paquetes de MLC colocan las .so en la raíz o en subcarpetas
        File found = findFirstSoRecursive(modelDir);
        return found != null && found.getName().equals(libName);
    }

    private File findFirstSoRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findFirstSoRecursive(f);
                if (found != null) return found;
            } else if (f.getName().endsWith(".so")) {
                return f;
            }
        }
        return null;
    }

    private void validateModelContents(File modelDir) throws Exception {
        if (isLiteRT) return; // LiteRT es un archivo único, no validamos carpeta

        List<String> missing = new ArrayList<>();
        String[] required = {MODEL_CONFIG_FILENAME, "params_shard_0.bin", "tokenizer.json"};
        for (String req : required) {
            if (!new File(modelDir, req).exists()) {
                missing.add(req);
            }
        }
        if (!missing.isEmpty()) {
            throw new IllegalStateException("Modelo incompleto. Faltan: " + missing);
        }
    }
}
