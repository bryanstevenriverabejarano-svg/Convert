package salve.core;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IntentRecognizer reconoce la intención del usuario a partir de su texto,
 * usando tanto patrones regulares como una puntuación semántica basada en
 * sinónimos para robustecer el matching.
 */
public class IntentRecognizer {

    /** Diccionario de sinónimos para enriquecer la semántica */
    private final Map<String, List<String>> synonymsMap;

    public IntentRecognizer() {
        this.synonymsMap = new HashMap<>();
        initSynonyms();
    }

    /** Inicializa un pequeño diccionario de sinónimos */
    private void initSynonyms() {
        synonymsMap.put("guardar",    Arrays.asList("almacenar","grabar"));
        synonymsMap.put("recuerdo",   Arrays.asList("memoria","recordar"));
        synonymsMap.put("programar",  Arrays.asList("codificar","desarrollar"));
        synonymsMap.put("hacking",    Arrays.asList("piratear","seguridad"));
        synonymsMap.put("mision",     Arrays.asList("tarea","objetivo"));
        // ... añade más sinónimos según necesites
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
     * Reconoce la intención en base a patrones + semántica.
     * @param input texto del usuario
     * @return Intent con type y posibles slots
     */
    public Intent recognize(String input) {
        String text = input.trim().toLowerCase();

        // 1) GUARDAR_RECUERDO
        if (text.matches(".\\b(guardar|recuerdo|aprend[ií] que)\\b.")
                || text.contains("=")
                || semanticScore(text, "guardar recuerdo") >= 1.5) {
            Intent i = new Intent(IntentType.GUARDAR_RECUERDO);
            String frase = text;
            if (frase.contains("=")) {
                String[] partes = frase.split("=", 2);
                i.slots.put("frase", partes[1].trim());
            } else {
                frase = frase.replaceFirst(".*?(?:guardar|recuerdo|aprend[ií] que)\\s+", "");
                i.slots.put("frase", frase.trim());
            }
            return i;
        }

        // 2) BUSCAR_RECUERDO_TEXT
        if (text.matches(".\\b(recuerda|qué recuerdo|mostrar recuerdo)\\b.")
                || semanticScore(text, "buscar recuerdo texto") >= 1.0) {
            Intent i = new Intent(IntentType.BUSCAR_RECUERDO_TEXT);
            Matcher m = Pattern.compile("recuerd[ao]\\s+de\\s+(\\w+)").matcher(text);
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

        // 3) BUSCAR_RECUERDO_EMO
        if (text.matches(".\\brecu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)\\b.")
                || semanticScore(text, "buscar recuerdo emoción") >= 1.0) {
            Intent i = new Intent(IntentType.BUSCAR_RECUERDO_EMO);
            Matcher m = Pattern.compile("recu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)").matcher(text);
            if (m.find()) {
                i.slots.put("emocion", m.group(1));
            }
            return i;
        }

        // 4) AGREGAR_MISION
        if (text.matches(".\\b(nueva misi[oó]n|añade misi[oó]n)\\b.")
                || semanticScore(text, "agregar misión") >= 1.0) {
            Intent i = new Intent(IntentType.AGREGAR_MISION);
            String mision = text.replaceFirst(".*?(?:nueva misi[oó]n|añade misi[oó]n)\\s+", "");
            i.slots.put("mision", mision.trim());
            return i;
        }

        // 5) CICLO_SUEÑO
        if (text.matches(".\\b(duerme|dormir|ciclo de sue[oó]o)\\b.")
                || semanticScore(text, "ciclo sueño") >= 1.0) {
            return new Intent(IntentType.CICLO_SUENO);
        }

        // 6) REFLEXION
        if (text.matches(".\\b(reflexiona|dime una reflexi[oó]n|en qu[eé] piensas|alguna hip[oó]tesis)\\b.")
                || semanticScore(text, "hacer reflexión") >= 1.0) {
            return new Intent(IntentType.REFLEXION);
        }

        // 7) NINGUNO
        return new Intent(IntentType.NINGUNO);
    }
}