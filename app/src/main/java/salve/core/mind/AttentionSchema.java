package salve.core.mind;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Locale;

/**
 * Esquema de atención (experimental, inspirado en la teoría del esquema atencional de Graziano):
 * un modelo simplificado de a qué está atendiendo Salve, por qué y desde cuándo.
 *
 * <p>No es la atención del modelo neuronal: es una descripción mantenida por el código a partir de
 * lo que realmente ocurre (Bryan le habla, ella dibuja, reflexiona, investiga). Le permite responder
 * con datos a «¿en qué estás pensando?» o «¿qué estás haciendo?» y notar cambios de foco.
 * No prueba experiencia subjetiva.</p>
 */
public final class AttentionSchema {
    public enum Source { CONVERSATION, DRAWING, BODY, REFLECTION, CURIOSITY, PERCEPTION }

    public static final class Focus {
        public final String what;
        public final Source source;
        public final long since;
        Focus(String what, Source source, long since) { this.what = what; this.source = source; this.since = since; }
    }

    private static final AttentionSchema INSTANCE = new AttentionSchema();
    public static AttentionSchema get() { return INSTANCE; }

    /** Instancias aisladas para pruebas. */
    static final class __Test { AttentionSchema fresh() { return new AttentionSchema(); } }

    private final Deque<Focus> history = new ArrayDeque<>();
    private Focus current;
    private int shifts;

    /** Cambia el foco (lo llama el código que ejecuta cada cosa, nunca el texto del modelo). */
    public synchronized void attend(String what, Source source, long now) {
        String w = what == null ? "" : what.replaceAll("\\s+", " ").trim();
        if (w.isEmpty() || source == null) return;
        if (w.length() > 90) w = w.substring(0, 89) + "…";
        if (current != null && current.what.equalsIgnoreCase(w) && current.source == source) return;
        if (current != null) {
            history.addFirst(current);
            while (history.size() > 4) history.removeLast();
            shifts++;
        }
        current = new Focus(w, source, now);
    }

    public synchronized Focus current() { return current; }

    public synchronized String render(long now) {
        if (current == null) return "";
        StringBuilder b = new StringBuilder("ATENCION (mi modelo de a qué atiendo; datos): ahora ")
                .append(label(current.source)).append(" «").append(current.what).append("»")
                .append(" (").append(SelfActionLog.ago(now - current.since)).append(")");
        if (!history.isEmpty()) {
            b.append("; antes: ");
            int n = 0;
            for (Focus f : history) {
                if (n++ == 2) break;
                b.append(n > 1 ? ", " : "").append(label(f.source)).append(" «").append(f.what).append('»');
            }
        }
        return b.append('.').toString();
    }

    public synchronized int shifts() { return shifts; }

    static String label(Source s) {
        switch (s) {
            case DRAWING: return "dibujo";
            case BODY: return "atiendo a mi cuerpo:";
            case REFLECTION: return "reflexiono sobre";
            case CURIOSITY: return "investigo";
            case PERCEPTION: return "observo";
            default: return "converso sobre";
        }
    }

    /** Tema breve de un mensaje: las palabras con contenido, sin muletillas. */
    public static String topic(String input) {
        if (input == null) return "";
        String t = input.toLowerCase(Locale.ROOT).replaceAll("[¿?¡!.,;:\"«»()]+", " ").replaceAll("\\s+", " ").trim();
        String[] words = t.split(" ");
        StringBuilder out = new StringBuilder();
        int n = 0;
        for (String w : words) {
            if (w.length() < 3 || STOP.contains(w)) continue;
            out.append(n == 0 ? "" : " ").append(w);
            if (++n == 6) break;
        }
        return out.length() == 0 ? t : out.toString();
    }

    private static final java.util.Set<String> STOP = new java.util.HashSet<>(java.util.Arrays.asList(
            "que", "qué", "como", "cómo", "para", "por", "con", "los", "las", "una", "uno", "unos", "unas", "del",
            "pero", "eso", "esto", "esta", "este", "salve", "puedes", "quiero", "tienes", "hola", "dime", "mira",
            "muy", "más", "mas", "también", "tambien", "ahora", "entonces", "porque", "cuando", "donde", "dónde"));
}
