package salve.core;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * IntentRecognizer reconoce la intención del usuario a partir de su texto,
 * usando patrones explícitos y extracción acotada de parámetros.
 * Las intenciones operativas son deterministas: una conversación ambigua nunca
 * se convierte en una acción por decisión de otro modelo.
 */
public class IntentRecognizer {

    /**
     * Writing private memory is a side effect, not a topic of conversation.
     * The previous anywhere-match on "guardar" or "=" could persist a question
     * or hypothesis. Preserve explicit imperative commands and the existing
     * first-person "aprendí que" shortcut, but do not infer authorization from
     * an arbitrary mention of saving or a bare equation.
     */
    private static final Pattern EXPLICIT_SAVE = Pattern.compile(
            "^(?:(?:salve|oye|por favor)\\s*[,;:]?\\s+)*"
            + "(?:guarda|almacena|graba|anota|registra)(?:\\s*:\\s*|\\s+)(\\S[\\s\\S]*)$");
    private static final Pattern COLON_SAVE = Pattern.compile(
            "^(?:guardar|anotar|registrar)\\s*:\\s*(\\S[\\s\\S]*)$");
    private static final Pattern LEARNED_THAT = Pattern.compile(
            "^aprend[ií] que\\s+(\\S[\\s\\S]*)$");


    // ============================================================
    //  CONSTRUCTORES
    // ============================================================

    public IntentRecognizer() {
    }

    /** Conserva la firma usada por Android; el contexto no inicia ningún modelo. */
    public IntentRecognizer(Context context) {
        this();
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
     * Reconoce intenciones operativas mediante patrones y semántica acotada.
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
        Matcher save = EXPLICIT_SAVE.matcher(text);
        if (!save.matches()) save = COLON_SAVE.matcher(text);
        if (!save.matches()) save = LEARNED_THAT.matcher(text);
        if (save.matches()) {
            // Only the explicit payload is stored, never incidental wording about a command.
            String phrase = save.group(1).trim();
            if (!phrase.isEmpty()) {
                Intent i = new Intent(IntentType.GUARDAR_RECUERDO);
                i.slots.put("frase", phrase);
                return i;
            }
        }

        // --------------------------------------------------------
        // 2) BUSCAR_RECUERDO_TEXT
        // --------------------------------------------------------
        if (text.matches(".*\\b(recuerda|recordar|qu[eé] recuerdo|muestra recuerdo|mostrar recuerdo)\\b.*")) {

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
        if (text.matches(".*\\brecu[eé]rdame lo (triste|feliz|alegre|enojado|sorprendido)\\b.*")) {

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
        if (text.matches(".*\\b(nueva misi[oó]n|añade misi[oó]n|agrega misi[oó]n)\\b.*")) {

            Intent i = new Intent(IntentType.AGREGAR_MISION);
            String mision = text.replaceFirst(".*?(?:nueva misi[oó]n|añade misi[oó]n|agrega misi[oó]n)\\s+", "");
            i.slots.put("mision", mision.trim());
            return i;
        }

        // --------------------------------------------------------
        // 5) CICLO_SUENO
        // --------------------------------------------------------
        if (text.matches(".*\\b(duerme|dormir|ciclo de sue[nñ]o|modo sue[nñ]o)\\b.*")) {

            return new Intent(IntentType.CICLO_SUENO);
        }

        // --------------------------------------------------------
        // 6) REFLEXION
        // --------------------------------------------------------
        if (text.matches(".*\\b(reflexiona|dime una reflexi[oó]n|en qu[eé] piensas|alguna hip[oó]tesis)\\b.*")) {

            return new Intent(IntentType.REFLEXION);
        }

        // --------------------------------------------------------
        // 7) BUSCAR_WEB (NUEVA INTENCIÓN)
        // --------------------------------------------------------
        String topic = salve.core.research.ResearchConversation.requestTopic(input);
        // Preserve existing explicit research aliases.
        if (topic == null && text.matches(".*\\b(mimetiza|copia a gemini|aprende de gemini)\\b.*")) {
            topic = text.replaceFirst(".*?(?:mimetiza|copia a gemini|aprende de gemini)\\s+", "").trim();
        }
        if (topic != null && !topic.isEmpty()) {
            Intent i = new Intent(IntentType.BUSCAR_WEB);
            i.slots.put("termino", topic);
            return i;
        }

        // Una entrada sin acción explícita continúa como conversación normal.
        return new Intent(IntentType.NINGUNO);
    }
}

