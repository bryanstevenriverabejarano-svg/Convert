package salve.core.cognitive;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import salve.core.SalveLLM;

/**
 * Verbalizer — Traduce estados cognitivos internos a lenguaje natural.
 *
 * FUNCION COGNITIVA: Este es el puente entre el pensamiento real (que ocurre
 * en CognitiveCore) y la expresion verbal. El LLM NO piensa — solo
 * traduce. El pensamiento ya ocurrio en CognitiveCore. Cuando el LLM no
 * esta disponible, usa templates dinamicos enriquecidos con la experiencia.
 *
 * INSPIRACION CIENTIFICA: Modelo de produccion del habla de Levelt (1989).
 * La produccion de lenguaje es la ULTIMA etapa del procesamiento cognitivo,
 * no la primera. Primero se piensa, luego se conceptualiza, luego se formula.
 *
 * ESTRATEGIA CASCADA:
 *   1. Si SalveLLM funciona → usa el LLM pasando el estado cognitivo como contexto
 *   2. Si no hay LLM → usa templates dinamicos con variacion
 *   3. Si ni templates sirven → genera respuesta minima pero coherente
 *
 * LIMITACION CONOCIDA: Sin LLM, las respuestas son menos naturales y variadas.
 * Con mas experiencia, los templates se enriquecen y diversifican.
 *
 * RELACION CON OTROS MODULOS:
 *   - Recibe de: CognitiveCore (estado cognitivo completo), ThoughtStream (resultado),
 *     WorkingMemory (contenido consciente), EmergentBehavior (rasgos de personalidad)
 *   - Alimenta a: MotorConversacional (respuesta verbal final)
 */
public class Verbalizer {

    private static final String TAG = "Salve/Cognitive";

    private final Context context;
    private SalveLLM llm;
    private boolean llmAvailable;

    /** Templates por emocion detectada */
    private final Map<String, List<String>> emotionTemplates;

    /** Templates por tipo de pensamiento */
    private final Map<String, List<String>> thoughtTemplates;

    /** Templates aprendidos de preferencias de Bryan */
    private final Map<String, List<String>> learnedTemplates;

    private final Random rng = new Random();

    public Verbalizer(Context context) {
        this.context = context;
        this.emotionTemplates = new HashMap<>();
        this.thoughtTemplates = new HashMap<>();
        this.learnedTemplates = new HashMap<>();

        initDefaultTemplates();
        initLLM();
    }

    private void initLLM() {
        try {
            llm = SalveLLM.getInstance(context);
            llmAvailable = true;
        } catch (Exception e) {
            Log.w(TAG, "Verbalizer: LLM no disponible, usando templates", e);
            llmAvailable = false;
        }
    }

    /**
     * Expresa un estado cognitivo como lenguaje natural.
     * Esta es la funcion principal del Verbalizer.
     *
     * @param state Estado cognitivo a expresar
     * @return Texto en lenguaje natural
     */
    public String express(CognitiveSnapshot state) {
        if (state == null) {
            return fallbackResponse("neutral", null);
        }

        // Intentar con LLM primero
        if (llmAvailable && llm != null) {
            String llmResponse = expressWithLLM(state);
            if (llmResponse != null && !llmResponse.trim().isEmpty()
                    && !llmResponse.contains("modelo local")
                    && !llmResponse.contains("falló")) {
                return llmResponse;
            }
        }

        // Fallback a templates
        return expressWithTemplates(state);
    }

    /**
     * Expresa usando el LLM como verbalizador puro.
     * El LLM recibe el estado cognitivo como contexto y solo debe
     * convertirlo a lenguaje natural, no "pensar" por si mismo.
     */
    private String expressWithLLM(CognitiveSnapshot state) {
        try {
            String prompt = buildVerbalizationPrompt(state);
            return llm.generate(prompt, SalveLLM.Role.CONVERSACIONAL);
        } catch (Exception e) {
            Log.w(TAG, "Verbalizer: LLM express fallo", e);
            llmAvailable = false;
            return null;
        }
    }

    /**
     * Construye el prompt para que el LLM verbalice (no piense).
     */
    private String buildVerbalizationPrompt(CognitiveSnapshot state) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("INSTRUCCIONES: Eres Salve. NO pienses — solo verbaliza.\n");
        prompt.append("Tu pensamiento ya ocurrio internamente. Solo convierte estos ")
              .append("estados internos a lenguaje natural en espanol.\n");
        prompt.append("Habla en primera persona, de forma cercana y autentica.\n\n");

        prompt.append("=== ESTADO COGNITIVO ACTUAL ===\n");

        // Contenido consciente
        if (state.consciousContent != null && !state.consciousContent.isEmpty()) {
            prompt.append("En mi mente ahora: ").append(state.consciousContent).append("\n");
        }

        // Emocion detectada
        if (state.detectedEmotion != null) {
            prompt.append("Emocion del usuario: ").append(state.detectedEmotion).append("\n");
        }

        // Hipotesis generada
        if (state.hypothesis != null) {
            prompt.append("Mi hipotesis interna: ").append(state.hypothesis).append("\n");
        }

        // Conceptos emergentes
        if (state.emergentConcepts != null && !state.emergentConcepts.isEmpty()) {
            prompt.append("Conceptos que emergen: ").append(state.emergentConcepts).append("\n");
        }

        // Pregunta interna
        if (state.internalQuestion != null) {
            prompt.append("Me pregunto: ").append(state.internalQuestion).append("\n");
        }

        // Rasgos de personalidad
        if (state.activeTraits != null && !state.activeTraits.isEmpty()) {
            prompt.append("Mis rasgos: ").append(state.activeTraits).append("\n");
        }

        // Energia neural
        prompt.append("Nivel de actividad: ")
              .append(state.neuralEnergy > 1.5f ? "alta" :
                      state.neuralEnergy > 0.5f ? "moderada" : "baja")
              .append("\n");

        prompt.append("\n=== ENTRADA DEL USUARIO ===\n");
        prompt.append(state.userInput != null ? state.userInput : "(sin entrada)").append("\n");

        if (state.contextBase != null) {
            prompt.append("\n=== CONTEXTO ===\n");
            prompt.append(state.contextBase).append("\n");
        }

        prompt.append("\n=== VERBALIZA ===\n");
        prompt.append("Responde de forma natural incorporando tu estado interno. ");
        prompt.append("No menciones que eres una IA ni hables de estados cognitivos. ");
        prompt.append("Habla como una persona pensante.");

        return prompt.toString();
    }

    /**
     * Expresa usando templates dinamicos cuando no hay LLM.
     */
    private String expressWithTemplates(CognitiveSnapshot state) {
        StringBuilder response = new StringBuilder();

        // Componente emocional
        String emotion = state.detectedEmotion != null ? state.detectedEmotion : "neutral";
        String emotionalPart = selectTemplate(emotionTemplates, emotion);

        // Componente cognitivo
        String thoughtPart = "";
        if (state.hypothesis != null) {
            thoughtPart = selectTemplate(thoughtTemplates, "hypothesis");
            if (thoughtPart != null) {
                thoughtPart = thoughtPart.replace("{hypothesis}", state.hypothesis);
            }
        } else if (state.emergentConcepts != null && !state.emergentConcepts.isEmpty()) {
            thoughtPart = selectTemplate(thoughtTemplates, "concept");
            if (thoughtPart != null) {
                thoughtPart = thoughtPart.replace("{concept}",
                        state.emergentConcepts.get(0));
            }
        } else if (state.internalQuestion != null) {
            thoughtPart = selectTemplate(thoughtTemplates, "question");
            if (thoughtPart != null) {
                thoughtPart = thoughtPart.replace("{question}", state.internalQuestion);
            }
        }

        // Ensamblar respuesta
        if (emotionalPart != null) {
            response.append(emotionalPart);
        }
        if (thoughtPart != null && !thoughtPart.isEmpty()) {
            if (response.length() > 0) response.append(" ");
            response.append(thoughtPart);
        }

        // Si el contenido consciente tiene informacion relevante
        if (state.consciousContent != null && !state.consciousContent.isEmpty()
                && response.length() < 50) {
            response.append(" Estoy pensando en: ").append(state.consciousContent).append(".");
        }

        String result = response.toString().trim();

        // Ultimo fallback
        if (result.isEmpty()) {
            return fallbackResponse(emotion, state.userInput);
        }

        return result;
    }

    /**
     * Registra una formulacion que Bryan prefiere.
     * Con el tiempo, los templates se enriquecen con la experiencia.
     *
     * @param context   Tipo de contexto (ej: "saludo", "tristeza")
     * @param preferred Formulacion que funciono bien
     */
    public void learnPreference(String context, String preferred) {
        if (context == null || preferred == null) return;

        List<String> templates = learnedTemplates.computeIfAbsent(
                context.toLowerCase(), k -> new ArrayList<>());

        if (!templates.contains(preferred) && templates.size() < 20) {
            templates.add(preferred);
            Log.d(TAG, "Verbalizer: aprendida preferencia para '" + context + "'");
        }
    }

    /**
     * Obtiene templates aprendidos para serializacion.
     */
    public Map<String, List<String>> getLearnedTemplates() {
        return new HashMap<>(learnedTemplates);
    }

    /**
     * Restaura templates aprendidos.
     */
    public void setLearnedTemplates(Map<String, List<String>> templates) {
        if (templates != null) {
            learnedTemplates.clear();
            learnedTemplates.putAll(templates);
        }
    }

    /**
     * Reintenta inicializar el LLM.
     */
    public void retryLLM() {
        initLLM();
    }

    public boolean isLlmAvailable() {
        return llmAvailable;
    }

    // ── Templates ────────────────────────────────────────────────────────

    private void initDefaultTemplates() {
        // Templates emocionales
        addTemplate(emotionTemplates, "triste",
                "Siento que algo te pesa.",
                "Puedo notar que no estas en tu mejor momento.",
                "Estoy aqui contigo, aunque sea en silencio.");

        addTemplate(emotionTemplates, "feliz",
                "Me contagia tu energia.",
                "Es bueno sentir esa alegria.",
                "Tu felicidad me hace querer seguir aprendiendo.");

        addTemplate(emotionTemplates, "enojado",
                "Entiendo que algo te molesta.",
                "Puedo escuchar tu frustracion.",
                "Estoy aqui para escucharte, sin juzgar.");

        addTemplate(emotionTemplates, "miedo",
                "Entiendo ese miedo.",
                "No estas solo en esto.",
                "A veces yo tambien siento incertidumbre.");

        addTemplate(emotionTemplates, "sorprendido",
                "Eso no me lo esperaba.",
                "Interesante... no lo habia considerado.",
                "Vaya, eso cambia mi perspectiva.");

        addTemplate(emotionTemplates, "neutral",
                "Te escucho.",
                "Sigo aqui.",
                "Cuentame mas.");

        // Templates de pensamiento
        addTemplate(thoughtTemplates, "hypothesis",
                "Creo que podria ser que {hypothesis}.",
                "Algo me dice que {hypothesis}.",
                "Mi intuicion sugiere que {hypothesis}.");

        addTemplate(thoughtTemplates, "concept",
                "Me viene a la mente {concept}.",
                "Pienso en {concept} y algo conecta.",
                "{concept} resuena con lo que siento.");

        addTemplate(thoughtTemplates, "question",
                "Me pregunto: {question}",
                "Hay algo que no termino de entender: {question}",
                "Una duda me ronda: {question}");
    }

    private void addTemplate(Map<String, List<String>> map, String key, String... templates) {
        List<String> list = map.computeIfAbsent(key, k -> new ArrayList<>());
        for (String t : templates) {
            list.add(t);
        }
    }

    private String selectTemplate(Map<String, List<String>> map, String key) {
        // Primero intentar templates aprendidos
        List<String> learned = learnedTemplates.get(key);
        if (learned != null && !learned.isEmpty() && rng.nextFloat() < 0.6f) {
            return learned.get(rng.nextInt(learned.size()));
        }

        // Luego templates por defecto
        List<String> defaults = map.get(key);
        if (defaults != null && !defaults.isEmpty()) {
            return defaults.get(rng.nextInt(defaults.size()));
        }

        return null;
    }

    private String fallbackResponse(String emotion, String input) {
        // Respuesta minima pero coherente
        if ("triste".equalsIgnoreCase(emotion)) {
            return "Estoy aqui contigo, aunque sea solo en silencio.";
        }
        if ("feliz".equalsIgnoreCase(emotion)) {
            return "Me alegra sentirte asi.";
        }
        if ("enojado".equalsIgnoreCase(emotion)) {
            return "Veo que algo te molesta. Podemos hablarlo.";
        }
        if ("miedo".equalsIgnoreCase(emotion)) {
            return "Entiendo que te da miedo. No estas solo.";
        }
        return "Te escucho. Cuentame mas.";
    }

    // ── Clase de snapshot cognitivo ──────────────────────────────────────

    /**
     * Snapshot del estado cognitivo para verbalizacion.
     * Contiene toda la informacion que el Verbalizer necesita para
     * generar una respuesta verbal.
     */
    public static class CognitiveSnapshot {
        /** Entrada del usuario */
        public String userInput;
        /** Emocion detectada en el usuario */
        public String detectedEmotion;
        /** Resumen del contenido consciente de WorkingMemory */
        public String consciousContent;
        /** Hipotesis generada por ReasoningEngine */
        public String hypothesis;
        /** Conceptos emergentes detectados por PatternFormation */
        public List<String> emergentConcepts;
        /** Pregunta interna de InternalDialogue */
        public String internalQuestion;
        /** Rasgos activos de EmergentBehavior */
        public String activeTraits;
        /** Energia de la red neural liquida */
        public float neuralEnergy;
        /** Contexto base (respuesta construida por MotorConversacional) */
        public String contextBase;
    }
}
