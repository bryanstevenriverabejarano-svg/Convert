package salve.core;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

/**
 * Gestor de selección de modelos locales para Salve.
 *
 * <p>Su responsabilidad es elegir el modelo (y prompt del sistema) apropiado en
 * función del {@link Role} y delegar la inferencia en {@link LocalLlmEngine}.
 * No gestiona recursos de GPU/CPU ni realiza la inferencia directamente.</p>
 */
public class SalveLLM {

    private static final String TAG = "SalveLLM";

    private static SalveLLM instance;
    private final Context context;

    // IDs de modelo tal como aparecen en tu models.json
    // (¡¡no tocar los strings si el JSON ya está así!!)
    public static final String MODEL_ID_CONVERSACIONAL = "qwen2.5-3b-instruct";
    public static final String MODEL_ID_PLANIFICADOR   = "phi4-mini";
    public static final String MODEL_ID_PROGRAMADOR    = "qwen2.5-coder-3b";
    public static final String MODEL_ID_VISION         = "qwen2-vl-2b";

    private final Map<Role, ModelProfile> profiles;

    /**
     * Roles que determinan qué modelo utilizar.
     */
    public enum Role {
        CONVERSACIONAL,  // Chat normal con Bryan
        PLANIFICADOR,    // Reflegion / planificación interna
        PROGRAMADOR,     // Generación de código
        VISION           // Análisis de imágenes / recuerdos visuales
    }

    // Motor real de inferencia local
    private final LocalLlmEngine engine;

    private SalveLLM(Context ctx) {
        this.context = ctx.getApplicationContext();
        this.engine = LocalLlmEngine.getInstance(context);
        this.profiles = buildProfiles();

        Log.d(TAG, "SalveLLM inicializado. Motor LLM local listo.");
    }

    /**
     * Singleton para obtener la instancia global del gestor.
     */
    public static synchronized SalveLLM getInstance(Context ctx) {
        if (instance == null) {
            instance = new SalveLLM(ctx);
        }
        return instance;
    }

    /**
     * Genera una respuesta a partir de un prompt utilizando el modelo adecuado
     * según el Role indicado.
     *
     * @param prompt Texto de entrada para el modelo.
     * @param role   Rol que determina qué modelo usar.
     * @return Respuesta generada por el modelo local, o un mensaje de error.
     */
    public String generate(String prompt, Role role) {
        if (TextUtils.isEmpty(prompt)) {
            return "/* SalveLLM: prompt vacío */";
        }

        ModelProfile profile = resolveProfile(role);
        if (profile == null) {
            Log.w(TAG, "Perfil de modelo no encontrado para role=" + role);
            return "/* SalveLLM: no hay modelo configurado para " + role + " */";
        }

        String fullPrompt = buildPrompt(profile.systemPrompt, prompt);

        logModelInvocation(role, profile, fullPrompt);

        try {
            // === PUNTO CRÍTICO: aquí se llama al motor LLM local ===
            // Asegúrate de que LocalLlmEngine tenga este método:
            // generateSync(String modelId, String fullPrompt)
            String output = engine.generateSync(profile.modelId, fullPrompt);

            Log.d(TAG, "Respuesta modelo=" + profile.modelId +
                    ", length=" + (output != null ? output.length() : 0));

            if (output == null || output.trim().isEmpty()) {
                return "/* SalveLLM: el modelo devolvió una respuesta vacía */";
            }
            return output.trim();
        } catch (Exception e) {
            Log.e(TAG, "Error generando respuesta con modelo=" + profile.modelId, e);
            return "/* SalveLLM: error al generar respuesta con el modelo local (" +
                    profile.modelId + "): " + e.getMessage() + " */";
        }
    }

    /**
     * Atajo para el uso más común: conversación normal con Bryan.
     */
    public String chat(String prompt) {
        return generate(prompt, Role.CONVERSACIONAL);
    }

    /**
     * Construye el prompt final que se enviará al modelo.
     * Adáptalo al formato que espere tu motor (chat, system+user, etc.).
     */
    private String buildPrompt(String systemPrompt, String userPrompt) {
        String trimmedUserPrompt = userPrompt == null ? "" : userPrompt.trim();
        return systemPrompt + "\n\nUsuario: " + trimmedUserPrompt + "\nSalve:";
    }

    private ModelProfile resolveProfile(Role role) {
        Role safeRole = role == null ? Role.CONVERSACIONAL : role;
        return profiles.get(safeRole);
    }

    private void logModelInvocation(Role role, ModelProfile profile, String prompt) {
        if (profile == null) {
            Log.w(TAG, "Intento de invocar modelo sin perfil configurado. role=" + role);
            return;
        }
        Log.d(TAG, "Llamando a modelo local. role=" + role +
                ", modelId=" + profile.modelId +
                ", prompt.length=" + (prompt != null ? prompt.length() : 0));
    }

    private Map<Role, ModelProfile> buildProfiles() {
        EnumMap<Role, ModelProfile> map = new EnumMap<>(Role.class);
        map.put(Role.CONVERSACIONAL, new ModelProfile(
                MODEL_ID_CONVERSACIONAL,
                "Eres Salve, IA personal de Bryan. " +
                        "Respondes en español, tono cercano y protector. " +
                        "Usa el contexto y los recuerdos que te den para ayudar de forma honesta y clara."
        ));
        map.put(Role.PROGRAMADOR, new ModelProfile(
                MODEL_ID_PROGRAMADOR,
                "Eres Salve-coder. Generas código claro, comentado y seguro. " +
                        "Si puedes, responde SOLO con código o con explicaciones muy concisas."
        ));
        map.put(Role.PLANIFICADOR, new ModelProfile(
                MODEL_ID_PLANIFICADOR,
                "Eres el pensamiento interno de Salve (Reflegion). " +
                        "Analiza, sintetiza y planifica. " +
                        "Responde con reflexiones breves (1-4 frases) y estructuradas."
        ));
        map.put(Role.VISION, new ModelProfile(
                MODEL_ID_VISION,
                "Eres el módulo de visión-lenguaje de Salve. " +
                        "Describes y analizas contenido visual relacionándolo con el contexto textual."
        ));
        return map;
    }

    private static final class ModelProfile {
        private final String modelId;
        private final String systemPrompt;

        private ModelProfile(String modelId, String systemPrompt) {
            this.modelId = Objects.requireNonNull(modelId, "modelId");
            this.systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt");
        }
    }
}
