package salve.core;

import android.content.Context;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IntentRecognizer reconoce la intención del usuario a partir de su texto,
 * usando patrones regulares + una puntuación semántica basada en sinónimos.
 *
 * ✅ Ahora también puede usar el LLM local (SalveLLM) como FALLBACK
 *    cuando las reglas no están seguras, para que Salve vaya
 *    “afinando” mejor qué le estás pidiendo.
 */
public class IntentRecognizer {

    /** Diccionario de sinónimos para enriquecer la semántica */
    private final Map<String, List<String>> synonymsMap;

    /** Opcional: LLM local para clasificación semántica de intenciones */
    private final SalveLLM llm;

    // ============================================================
    //  CONSTRUCTORES
    // ============================================================

    /** Constructor clásico sin LLM (sigue funcionando como antes). */
    public IntentRecognizer() {
        this.synonymsMap = new HashMap<>();
        this.llm = null;
        initSynonyms();
    }

    /**
     * Nuevo constructor con Context → permite usar SalveLLM
     * como fallback de clasificación.
     */
    public IntentRecognizer(Context context) {
        this.synonymsMap = new HashMap<>();
        initSynonyms();
        try {
            this.llm = (context != null) ? SalveLLM.getInstance(context.getApplicationContext()) : null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Inicializa un pequeño diccionario de sinónimos */
    private void initSynonyms() {
        synonymsMap.put("guardar",    Arrays.asList("almacenar","grabar","anotar","registrar"));
        synonymsMap.put("recuerdo",   Arrays.asList("memoria","recordar","recuerdos"));
        synonymsMap.put("programar",  Arrays.asList("codificar","desarrollar","escribir código"));
        synonymsMap.put("hacking",    Arrays.asList("piratear","seguridad","ciberseguridad","hackear"));
        synonymsMap.put("mision",     Arrays.asList("tarea","objetivo","encargo"));
        synonymsMap.put("reflexiona", Arrays.asList("piensa","analiza","hipótesis","reflexión"));
        synonymsMap.put("duerme",     Arrays.asList("descansa","sueño","modo sueño"));
        // ... puedes seguir ampliando este mapa más adelante
    }

    /**
     * Calcula una puntuación semántica comparando dos textos.
     * +1.0 por cada palabra idéntica,
     * +0.5 por cada sinónimo encontrado.
     */
    private double semanticScore(String text, String pattern) {
        double score = 0;
        String lowerText = text.toLowerCase();
        for (String word : pattern.toLowerCase().split("\\s+")) {
            if (lowerText.contains(word)) {
                score += 1.0;
            }
            List<String> syns = synonymsMap.get(word);
            if (syns != null) {
                for (String syn : syns) {
                    if (lowerText.contains(syn)) {
                        score += 0.5;
                    }
                }
            }
        }
        return score;
    }

    /** Resultado de un reconocimiento: tipo de intención y slots extraídos. */
    public static class Intent {
        public final IntentType type;
        public final Map<String, String> slots;
        public Intent(IntentType type) {
            this.type  = type;
            this.slots = new HashMap<>();
        }
    }

    /**
     * Reconoce la intención en base a:
     *  1) patrones + semántica rápida,
     *  2) si nada encaja y hay LLM, pregunta al LLM (fallback).
     *
     * @param input texto del usuario
     * @return Intent con type y posibles slots
     */
    public Intent recognize(String input) {
        if (input == null) {
            return new Intent(IntentType.NINGUNO);
        }

        String text = input.trim().toLowerCase();

        // --------------------------------------------------------
        // 1) GUARDAR_RECUERDO
        // --------------------------------------------------------
        if (text.matches(".*\\b(guardar|recuerdo|aprend[ií] que)\\b.*")
                || text.contains("=")
                || semanticScore(text, "guardar recuerdo") >= 1.5) {

            Intent i = new Intent(IntentType.GUARDAR_RECUERDO);
            String frase = text;

            if (frase.contains("=")) {
                // estilo: "x = y"
                String[] partes = frase.split("=", 2);
                i.slots.put("frase", partes[0].trim() + " = " + partes[1].trim());
            } else {
                frase = frase.replaceFirst(".*?(?:guardar|recuerdo|aprend[ií] que)\\s+", "");
                i.slots.put("frase", frase.trim());
            }
            return i;
        }

        // --------------------------------------------------------
        // 2) BUSCAR_RECUERDO_TEXT
        // --------------------------------------------------------
        if (text.matches(".*\\b(recuerda|qué recuerdo|mostrar recuerdo)\\b.*")
                || semanticScore(text, "buscar recuerdo texto") >= 1.0) {

            Intent i = new Intent(IntentType.BUSCAR_RECUERDO_TEXT);
            Matcher m = Pattern.compile("recuerd[ao]\\s+de\\s+([\\wáéíóúñ]+)").matcher(text);
            String clave;
            if (m.find()) {
                clave = m.group(1);
            } else {
                String[] w = text.split("\\s+");
                clave = w[w.length - 1];
            }
            i.slots.put("palabraClave", clave);
            return i;
        }

        // --------------------------------------------------------
        // 3) BUSCAR_RECUERDO_EMO
        // --------------------------------------------------------
        if (text.matches(".*\\brecu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)\\b.*")
                || semanticScore(text, "buscar recuerdo emoción") >= 1.0) {

            Intent i = new Intent(IntentType.BUSCAR_RECUERDO_EMO);
            Matcher m = Pattern.compile("recu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)")
                    .matcher(text);
            if (m.find()) {
                i.slots.put("emocion", m.group(1));
            }
            return i;
        }

        // --------------------------------------------------------
        // 4) AGREGAR_MISION
        // --------------------------------------------------------
        if (text.matches(".*\\b(nueva misi[oó]n|añade misi[oó]n)\\b.*")
                || semanticScore(text, "agregar misión") >= 1.0) {

            Intent i = new Intent(IntentType.AGREGAR_MISION);
            String mision = text.replaceFirst(".*?(?:nueva misi[oó]n|añade misi[oó]n)\\s+", "");
            i.slots.put("mision", mision.trim());
            return i;
        }

        // --------------------------------------------------------
        // 5) CICLO_SUENO
        // --------------------------------------------------------
        if (text.matches(".*\\b(duerme|dormir|ciclo de sue[nñ]o|modo sue[nñ]o)\\b.*")
                || semanticScore(text, "ciclo sueño") >= 1.0) {

            return new Intent(IntentType.CICLO_SUENO);
        }

        // --------------------------------------------------------
        // 6) REFLEXION
        // --------------------------------------------------------
        if (text.matches(".*\\b(reflexiona|dime una reflexi[oó]n|en qu[eé] piensas|alguna hip[oó]tesis)\\b.*")
                || semanticScore(text, "hacer reflexión") >= 1.0) {

            return new Intent(IntentType.REFLEXION);
        }

        // --------------------------------------------------------
        // 7) BUSCAR_WEB (NUEVA INTENCIÓN)
        // --------------------------------------------------------
        if (text.matches(".*\\b(qu[eé] es|busca|investiga|quien es|qui[eé]n es)\\b.*")
                || text.matches(".*\\b(qu[eé] significa|significado de)\\b.*")
                || text.matches(".*\\b(mimetiza|copia a gemini|aprende de gemini)\\b.*")) {

            Intent i = new Intent(IntentType.BUSCAR_WEB);
            // Intentar extraer el término
            String termino = text.replaceFirst(".*?(?:qu[eé] es|busca|investiga|quien es|qui[eé]n es|qu[eé] significa|significado de|mimetiza|copia a gemini|aprende de gemini)\\s+", "").trim();
            
            if (text.contains("mimetiza") || text.contains("copia") || text.contains("aprende de gemini")) {
                i.slots.put("mimetismo", "true");
            }

            if (!termino.isEmpty()) {
                i.slots.put("termino", termino);
                return i;
            }
        }

        // --------------------------------------------------------
        // 8) NINGUNO → intentamos con el LLM (si existe)
        // --------------------------------------------------------
        Intent fromLLM = llmFallback(input);
        if (fromLLM != null) {
            return fromLLM;
        }

        // fallback final
        return new Intent(IntentType.NINGUNO);
    }

    // ============================================================
    //  LLM FALLBACK
    // ============================================================

    /**
     * Si las reglas no detectan nada claro y hay un LLM disponible,
     * le pedimos que clasifique la intención.
     *
     * El LLM debe responder en JSON puro, por ejemplo:
     *
     * {
     *   "intent": "GUARDAR_RECUERDO",
     *   "slots": { "frase": "..." }
     * }
     */
    private Intent llmFallback(String input) {
        if (llm == null || input == null || input.trim().isEmpty()) {
            return null;
        }

        try {
            String prompt =
                    "Eres el módulo interno de clasificación de intenciones de Salve.\n" +
                            "El usuario dice (en español):\n" +
                            "\"" + input + "\"\n\n" +
                            "Debes clasificar esta frase EXACTAMENTE en uno de estos tipos de intención:\n" +
                            " - GUARDAR_RECUERDO: quiere que aprendas o guardes algo en tu memoria.\n" +
                            " - BUSCAR_RECUERDO_TEXT: quiere que recuerdes algo por palabra clave.\n" +
                            " - BUSCAR_RECUERDO_EMO: quiere que recuerdes algo asociado a una emoción.\n" +
                            " - AGREGAR_MISION: quiere añadir una misión o tarea para ti.\n" +
                            " - CICLO_SUENO: quiere que entres en modo sueño / procesamiento interno.\n" +
                            " - REFLEXION: quiere que reflexiones, compartas una hipótesis o en qué piensas.\n" +
                            " - NINGUNO: si no encaja claramente con ninguna de las anteriores.\n" +
                            "\n" +
                            "Si puedes, también rellena algunos 'slots' útiles:\n" +
                            " - Para GUARDAR_RECUERDO: usa slot 'frase'.\n" +
                            " - Para BUSCAR_RECUERDO_TEXT: usa slot 'palabraClave'.\n" +
                            " - Para BUSCAR_RECUERDO_EMO: usa slot 'emocion' (triste, feliz, alegre, enojado, sorprendido...).\n" +
                            " - Para AGREGAR_MISION: usa slot 'mision'.\n" +
                            "\n" +
                            "DEVUELVE SOLO JSON PURO, sin texto extra, con este formato exacto:\n" +
                            "{\n" +
                            "  \"intent\": \"...\",\n" +
                            "  \"slots\": { \"clave\": \"valor\" }\n" +
                            "}\n";

            String raw = llm.generate(prompt, SalveLLM.Role.PLANIFICADOR);
            if (raw == null || raw.trim().isEmpty()) {
                return null;
            }

            String json = extractJson(raw);
            if (json == null) {
                return null;
            }

            JSONObject obj = new JSONObject(json);
            String intentStr = obj.optString("intent", "NINGUNO").trim();
            IntentType type = parseIntentType(intentStr);

            Intent intent = new Intent(type);

            // ⚠️ Aquí es donde cambiamos keySet() → keys() para evitar tu error
            JSONObject slots = obj.optJSONObject("slots");
            if (slots != null) {
                Iterator<String> keys = slots.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    String value = slots.optString(key, null);
                    if (value != null && !value.trim().isEmpty()) {
                        intent.slots.put(key, value.trim());
                    }
                }
            }

            return intent;

        } catch (Exception ignore) {
            // si algo falla, simplemente devolvemos null y seguimos con NINGUNO
            return null;
        }
    }

    /** Convierte el texto del LLM en un IntentType seguro. */
    private IntentType parseIntentType(String s) {
        if (s == null) return IntentType.NINGUNO;
        String key = s.trim().toUpperCase();

        switch (key) {
            case "GUARDAR_RECUERDO":
                return IntentType.GUARDAR_RECUERDO;
            case "BUSCAR_RECUERDO_TEXT":
            case "BUSCAR_RECUERDO_TEXTO":
                return IntentType.BUSCAR_RECUERDO_TEXT;
            case "BUSCAR_RECUERDO_EMO":
            case "BUSCAR_RECUERDO_EMOCION":
                return IntentType.BUSCAR_RECUERDO_EMO;
            case "AGREGAR_MISION":
            case "AÑADIR_MISION":
                return IntentType.AGREGAR_MISION;
            case "CICLO_SUENO":
            case "CICLO_SUEÑO":
            case "MODO_SUEÑO":
                return IntentType.CICLO_SUENO;
            case "REFLEXION":
            case "REFLEXIÓN":
                return IntentType.REFLEXION;
            default:
                return IntentType.NINGUNO;
        }
    }

    /**
     * Extrae solo el JSON de una respuesta del LLM
     * buscando desde el primer '{' hasta el último '}'.
     */
    private String extractJson(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        int end   = raw.lastIndexOf('}');
        if (start < 0 || end <= start) return null;
        return raw.substring(start, end + 1);
    }
}
