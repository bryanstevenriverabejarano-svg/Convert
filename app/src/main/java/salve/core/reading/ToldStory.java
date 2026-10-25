package salve.core.reading;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.conversation.ChatMessage;

/**
 * Los cuentos que cuenta ella misma en el chat. Antes, «guarda ese cuento en tu repertorio» no
 * guardaba nada (solo existía la estantería de documentos leídos) y el modelo decía «lo he
 * guardado» sin haberlo hecho. Aquí se reconoce la petición y se encuentra el cuento que contó.
 */
public final class ToldStory {
    private ToldStory() { }

    /** Un cuento contado más corto que esto es una frase, no un cuento. */
    static final int MIN_CHARS = 350;

    private static final Pattern SAVE = Pattern.compile(
            "^(?:salve )?(?:guarda|guardate|guardame|apunta|anade|anademe|mete)(?:lo)? (?:ese|este|el|tu|el ultimo|ese ultimo) "
            + "(?:cuento|relato|historia)(?: que me (?:has|acabas de) contar|contado| que me has contado| que me acabas de contar)?"
            + "(?: (?:en|a) (?:tu|mi|la) (?:repertorio|estanteria|biblioteca|coleccion|memoria)(?: de cuentos)?)?"
            + "(?:,? (?:con el (?:nombre|titulo) (?:de )?|que se llama |titulado |como )(.{2,60}))?$");
    private static final Pattern TITLE_IN_ORIGINAL = Pattern.compile(
            "(?iu)(?:con el (?:nombre|t[ií]tulo) (?:de )?|que se llama |titulado |como )[«\"“]?([^«»\"“”]{2,60}?)[»\"”]?[.!]*$");
    private static final Pattern TOLD_TITLE = Pattern.compile(
            "(?iu)\\b(?:cu[eé]nt\\p{L}*me|cu[eé]ntame|l[eé]eme|inv[eé]ntate|inventa|escr[ií]beme|narra)\\b.*?\\b(?:cuento|relato|historia)"
            + "\\s+(?:de\\s+(?:la\\s+|el\\s+|los\\s+|las\\s+)?|del\\s+|sobre\\s+(?:la\\s+|el\\s+|un\\s+|una\\s+)?|que se llama\\s+)([^?.!¿¡]{2,60})");

    /** null si no pide guardar un cuento; "" si lo pide sin título; si no, el título que le dio. */
    public static String saveRequest(String input) {
        String t = DocumentReading.normalize(input == null ? "" : input).replaceAll("[.!]+$", "").trim();
        Matcher m = SAVE.matcher(t);
        if (!m.matches()) return null;
        if (m.group(1) == null) return "";
        Matcher o = TITLE_IN_ORIGINAL.matcher(input.trim());
        return capital(o.find() ? o.group(1).trim() : m.group(1).trim());
    }

    /** El último cuento que contó: {texto limpio, título por la petición de Bryan (o "")}; null si no hay. */
    public static String[] last(List<ChatMessage> history) {
        if (history == null) return null;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            if (m.getRole() != ChatMessage.Role.ASSISTANT) continue;
            String text = clean(m.getContent());
            if (text.length() < MIN_CHARS) continue;
            String title = "";
            for (int j = i - 1; j >= 0 && j >= i - 4 && title.isEmpty(); j--)
                if (history.get(j).getRole() == ChatMessage.Role.USER) title = titleFromRequest(history.get(j).getContent());
            return new String[]{text, title};
        }
        return null;
    }

    /** «Cuéntame el cuento de faro del susurro» → «Faro del susurro». */
    public static String titleFromRequest(String request) {
        if (request == null) return "";
        Matcher m = TOLD_TITLE.matcher(request.trim());
        return m.find() ? capital(m.group(1).replaceAll("\\s+", " ").trim()) : "";
    }

    /** Sin las marcas de dibujo (se vuelven a dibujar al leerlo) y sin espacios de sobra. */
    public static String clean(String text) {
        if (text == null) return "";
        return text.replaceAll("\\[\\[salve_draw:[^\\]]*\\]\\]", "").replaceAll("[ \\t]+", " ")
                .replaceAll(" *\\n *", "\n").replaceAll("\\n{3,}", "\n\n").trim();
    }

    /** Título por las primeras palabras, si Bryan no le dio ninguno. */
    public static String fallbackTitle(String text) {
        String first = clean(text).split("[.!?\\n]", 2)[0].trim();
        String[] w = first.split("\\s+");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < Math.min(6, w.length); i++) b.append(i == 0 ? "" : " ").append(w[i]);
        return b.length() == 0 ? "Mi cuento" : capital(b.toString().replaceAll("[,;:]+$", ""));
    }

    /** Resumen corto para la estantería: las dos primeras frases. */
    public static String summary(String text) {
        Matcher m = Pattern.compile("[^.!?…]+[.!?…]+").matcher(clean(text).replace('\n', ' '));
        StringBuilder b = new StringBuilder();
        for (int n = 0; n < 2 && m.find(); n++) b.append(n == 0 ? "" : " ").append(m.group().trim());
        String s = b.length() == 0 ? clean(text) : b.toString();
        return s.length() > 300 ? s.substring(0, 300).trim() + "…" : s;
    }

    static String capital(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
