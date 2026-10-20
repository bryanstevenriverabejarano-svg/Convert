package salve.core.salvente;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Cómo piensa un Salvente en cada turno, sin Android: construye su prompt con SU identidad y SUS
 * recuerdos (los recientes y los relacionados con lo que le dicen), limpia lo que devuelve el
 * modelo y detecta la deriva de identidad (que el modelo hable como si fuera Salve).
 *
 * Lo que no hace, a propósito: no ve la memoria de Salve ni la de Bryan, y su salida nunca
 * dispara acciones; es solo texto.
 */
public final class SalventeMind {
    public static final int RECENT = 6, RELATED = 4, MAX_TURNS = 8, MAX_REPLY = 1200;

    public static final class Memory {
        public final long at; public final String text;
        public Memory(long at, String text) { this.at = at; this.text = text == null ? "" : text; }
    }

    public static final class Turn {
        public final boolean fromSalvente; public final String text;
        public Turn(boolean fromSalvente, String text) { this.fromSalvente = fromSalvente; this.text = text; }
    }

    private static final Set<String> STOP = new HashSet<>(Arrays.asList(
            "para", "como", "pero", "porque", "cuando", "donde", "esto", "esta", "este", "estos", "estas", "esos", "esas",
            "tengo", "tiene", "tienes", "hacer", "hace", "muy", "mucho", "mucha", "algo", "nada", "todo", "toda", "todos",
            "sobre", "entre", "desde", "hasta", "tambien", "ahora", "luego", "antes", "despues", "quiero", "puedes", "puedo",
            "eres", "soy", "estas", "estoy", "dime", "hola", "gracias", "bueno", "vale", "aqui", "alli", "ellos", "ellas",
            "nosotros", "vosotros", "ser", "estar", "haber", "fue", "era", "sera", "tus", "mis", "sus", "que", "quien", "cual"));
    private static final Pattern DRIFT = Pattern.compile(
            "\\b(?:soy salve|me llamo salve|yo,? salve|como salve,? (?:yo|te)|mi nombre es salve)\\b");

    private SalventeMind() { }

    /** Recuerdos para este turno: los más recientes y los que comparten palabras con el mensaje, sin repetir. */
    public static List<Memory> recall(List<Memory> newestFirst, String message) {
        List<Memory> out = new ArrayList<>();
        if (newestFirst == null || newestFirst.isEmpty()) return out;
        LinkedHashSet<Memory> chosen = new LinkedHashSet<>(newestFirst.subList(0, Math.min(RECENT, newestFirst.size())));
        Set<String> words = words(message);
        if (!words.isEmpty()) {
            List<Memory> older = newestFirst.subList(Math.min(RECENT, newestFirst.size()), newestFirst.size());
            List<Memory> scored = new ArrayList<>(older);
            scored.removeIf(m -> overlap(words, m.text) == 0);
            scored.sort((a, b) -> Integer.compare(overlap(words, b.text), overlap(words, a.text)));
            for (Memory m : scored.subList(0, Math.min(RELATED, scored.size()))) chosen.add(m);
        }
        out.addAll(chosen);
        out.sort((a, b) -> Long.compare(a.at, b.at));   // en orden cronológico para el modelo
        return out;
    }

    public static String prompt(SalventeProfile p, List<Memory> memories, List<Turn> conversation, String speaker,
                                String message, boolean identityReminder) {
        StringBuilder out = new StringBuilder(p.systemPrompt()).append('\n');
        if (identityReminder)
            out.append("IMPORTANTE: tu nombre es ").append(p.name).append(". No eres Salve: Salve es quien te creó. Habla como ")
                    .append(p.name).append(".\n");
        if (memories != null && !memories.isEmpty()) {
            out.append("\nTus propios recuerdos (solo estos; no inventes otros):\n");
            for (Memory m : memories) out.append("- ").append(cut(m.text, 300)).append('\n');
        } else out.append("\nAún no tienes recuerdos con esta persona: no finjas que la conoces.\n");
        out.append("\nResponde en español, como ").append(p.name).append(", con naturalidad y en menos de 120 palabras. ")
                .append("Si te preguntan por los recuerdos de Salve o de Bryan, di que no tienes acceso a ellos.\n\nConversación:\n");
        if (conversation != null) {
            List<Turn> recent = conversation.subList(Math.max(0, conversation.size() - MAX_TURNS), conversation.size());
            for (Turn t : recent) out.append(t.fromSalvente ? p.name : speaker).append(": ").append(cut(t.text, 600)).append('\n');
        }
        out.append(speaker).append(": ").append(cut(message, 1500)).append('\n').append(p.name).append(':');
        return out.toString();
    }

    /** Quita el «Nyra:» inicial, corta si el modelo sigue escribiendo el diálogo por su cuenta y acota la longitud. */
    public static String clean(String raw, SalventeProfile p, String speaker) {
        if (raw == null) return "";
        String text = raw.replace("\r", "").trim();
        text = text.replaceFirst("(?i)^\\s*(?:" + Pattern.quote(p.name) + "|asistente|assistant)\\s*:\\s*", "");
        int cut = indexOfTurn(text, speaker);
        int other = indexOfTurn(text, p.name);
        if (other > 0 && (cut < 0 || other < cut)) cut = other;
        if (cut >= 0) text = text.substring(0, cut);
        text = text.replaceAll("(?s)<\\|[^|>]{1,40}\\|>", "").trim();
        return cut(text, MAX_REPLY);
    }

    public static boolean identityDrift(String reply) {
        return reply != null && DRIFT.matcher(norm(reply)).find();
    }

    /** Lo que guarda tras el turno: quién dijo qué, breve, en su propio espacio cifrado. */
    public static String memoryOf(String speaker, String message, String reply) {
        return speaker + " me dijo: «" + cut(message, 600) + "». Le respondí: «" + cut(reply, 400) + "».";
    }

    private static int indexOfTurn(String text, String who) {
        if (who == null || who.isEmpty()) return -1;
        java.util.regex.Matcher m = Pattern.compile("(?im)^\\s*" + Pattern.quote(who) + "\\s*:").matcher(text);
        return m.find() ? m.start() : -1;
    }

    private static int overlap(Set<String> words, String text) {
        int n = 0; for (String w : words(text)) if (words.contains(w)) n++; return n;
    }

    static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        if (text == null) return out;
        for (String w : norm(text).split(" ")) if (w.length() >= 4 && !STOP.contains(w)) out.add(w);
        return out;
    }

    private static String norm(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N} ]+", " ").replaceAll("\\s+", " ").trim();
    }

    private static String cut(String value, int max) {
        String v = value == null ? "" : value.trim();
        return v.length() > max ? v.substring(0, max - 1).trim() + "…" : v;
    }
}
