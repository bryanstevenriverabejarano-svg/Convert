package salve.core;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;

// 🔹 IMPORTANTE: import de tu wrapper Kotlin
import salve.core.BasicLocalLlm;

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
        PLANIFICADOR   // para el grafo/planificador
    }

    private static final String TAG = "Salve/LLM";

    // Las mismas prefs que usa MainActivity
    private static final String PREFS_NAME = "salve_prefs";
    private static final String KEY_MODEL_PATH = "llm_model_path";

    // Nombre del config de MLC dentro de la carpeta del modelo
    private static final String MODEL_CONFIG_FILENAME = "mlc-chat-config.json";

    private static SalveLLM instance;

    private final Context appContext;
    private String modelPath;   // ruta absoluta a la carpeta del modelo
    private String modelLib;    // nombre de la librería del modelo
    private boolean engineInitialized = false;
    private boolean modelAvailable    = false;   // ⬅️ indica si tenemos info suficiente del modelo
    private String lastErrorMessage   = null;

    private SalveLLM(Context context) {
        this.appContext = context.getApplicationContext();
        // Intentamos cargar info de modelo y preparar el motor.
        // Pero NUNCA reventamos la app si algo va mal.
        try {
            reloadModelInfoFromPrefs();
            initEngineIfNeeded();
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
     * NUNCA lanza excepciones hacia fuera (aunque la firma tenga 'throws').
     */
    public static synchronized SalveLLM getInstance(Context context) throws Exception {
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
        String path = prefs.getString(KEY_MODEL_PATH, null);

        if (path == null || path.trim().isEmpty()) {
            throw new IllegalStateException(
                    "No se ha definido aún la ruta del modelo en SharedPreferences (" +
                            PREFS_NAME + "/" + KEY_MODEL_PATH + "). " +
                            "Deja que MainActivity detecte modelos primero."
            );
        }

        File dir = new File(path);
        if (!dir.exists() || !dir.isDirectory()) {
            throw new IllegalStateException(
                    "La ruta de modelo guardada no existe o no es carpeta: " + path
            );
        }

        // Manejar subcarpeta interna con el mismo nombre
        File effectiveDir = resolveEffectiveModelDir(dir);

        // Validar existencia de config antes de seguir
        File cfg = new File(effectiveDir, MODEL_CONFIG_FILENAME);
        if (!cfg.exists()) {
            throw new IllegalStateException(
                    "No se encontró " + MODEL_CONFIG_FILENAME + " en " + effectiveDir.getAbsolutePath()
            );
        }

        this.modelPath = effectiveDir.getAbsolutePath();
        this.modelLib  = detectModelLibFromConfig(effectiveDir);

        if (this.modelLib == null || this.modelLib.trim().isEmpty()) {
            throw new IllegalStateException(
                    "No se pudo determinar ninguna librería para el modelo en " + this.modelPath
            );
        }

        if (!isModelLibPresent(effectiveDir, this.modelLib)) {
            throw new IllegalStateException(
                    "No se encontró la librería del modelo (" + this.modelLib + ") en " + effectiveDir.getAbsolutePath()
            );
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

    /**
     * Intenta sacar model_lib del JSON.
     * Si no existe o hay problemas, hace fallback a "libpenguin.so" (nombre fijo).
     * Nunca devuelve null.
     */
    private String detectModelLibFromConfig(File modelDir) {
        File cfg = new File(modelDir, MODEL_CONFIG_FILENAME);
        if (!cfg.exists()) {
            Log.e(TAG,
                    "No se encontró " + MODEL_CONFIG_FILENAME +
                            " en la carpeta del modelo: " + modelDir.getAbsolutePath() +
                            ". Usando libpenguin.so por defecto.");
            // Fallback directo: nombre fijo
            return "libpenguin.so";
        }

        String libFromJson = null;

        try (FileInputStream fis = new FileInputStream(cfg)) {
            byte[] data = new byte[(int) cfg.length()];
            int read = fis.read(data);
            if (read <= 0) {
                Log.e(TAG, "No se pudo leer " + MODEL_CONFIG_FILENAME + ". Usando libpenguin.so.");
            } else {
                String json = new String(data, StandardCharsets.UTF_8);
                JSONObject obj = new JSONObject(json);

                if (obj.has("model_lib")) {
                    libFromJson = obj.optString("model_lib", null);
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error leyendo/parsing " + MODEL_CONFIG_FILENAME + ". Usando libpenguin.so.", e);
        }

        if (libFromJson != null && !libFromJson.trim().isEmpty()) {
            // IMPORTANTE: MLC espera el nombre (libpenguin.so), no una ruta absoluta.
            return libFromJson.trim();
        }

        Log.w(TAG, MODEL_CONFIG_FILENAME +
                " no contiene campo 'model_lib'. Usando libpenguin.so por defecto.");

        // 🔴 Fallback definitivo: siempre damos un nombre válido.
        return "libpenguin.so";
    }

    /**
     * Busca recursivamente el primer fichero .so dentro de dir.
     * (Ahora mismo no se usa, pero lo dejamos por si en el futuro quieres
     * volver a auto-detectar librerías dentro del modelo).
     */
    private File findFirstSoRecursive(File dir) {
        File[] files = dir.listFiles();
        if (files == null) return null;

        for (File f : files) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".so")) {
                return f;
            }
        }
        for (File f : files) {
            if (f.isDirectory()) {
                File found = findFirstSoRecursive(f);
                if (found != null) return found;
            }
        }
        return null;
    }

    /**
     * Inicializa el motor de MLC a través de BasicLocalLlm.
     * Solo se hace una vez por instancia.
     */
    private synchronized void initEngineIfNeeded() throws Exception {
        if (engineInitialized) return;

        if (modelPath == null || modelLib == null) {
            throw new IllegalStateException("initEngineIfNeeded sin modelo válido.");
        }

        Log.d(TAG, "Inicializando BasicLocalLlm con modelPath=" + modelPath +
                " modelLib=" + modelLib);

        // Llama a tu wrapper Kotlin
        BasicLocalLlm.INSTANCE.init(modelPath, modelLib);
        engineInitialized = true;
    }

    /**
     * Punto principal de generación de texto.
     *
     * @param prompt Prompt completo.
     * @param role   Rol deseado.
     * @return Texto generado, o null si algo falla o si no hay modelo local.
     */
    public String generate(String prompt, Role role) {
        if (prompt == null || prompt.trim().isEmpty()) return "";

        if (!modelAvailable) {
            Log.w(TAG,
                    "generate() llamado pero el modelo local NO está disponible. " +
                            "Devolviendo null (se usará fallback).");
            return "No hay modelo local listo todavía. Puedes esperar a la descarga o elegir uno compatible.";
        }

        try {
            initEngineIfNeeded();
        } catch (Exception e) {
            lastErrorMessage = e.getMessage();
            Log.e(TAG, "Error inicializando el LLM local en generate()", e);
            return "No pude preparar el modelo local: " + e.getMessage();
        }

        String decoratedPrompt = decoratePrompt(prompt, role);

        try {
            return BasicLocalLlm.INSTANCE.chatSinglePrompt(decoratedPrompt);
        } catch (Exception e) {
            lastErrorMessage = e.getMessage();
            Log.e(TAG, "Error generando respuesta con BasicLocalLlm", e);
            return "El modelo local falló al responder: " + e.getMessage();
        }
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
        try {
            reloadModelInfoFromPrefs();
            initEngineIfNeeded();
            modelAvailable = true;
        } catch (Exception e) {
            Log.e(TAG, "Error al recargar modelo en forceReloadModel()", e);
            modelPath = null;
            modelLib = null;
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
}
