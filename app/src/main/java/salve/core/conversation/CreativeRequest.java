package salve.core.conversation;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Textos largos y creativos (cuentos, historias, poemas, cartas) y escenas del modo historia.
 *
 * <p>15/10/2026, 19:22: «Cuéntame un cuento inventado por ti» → «Aquí tienes uno corto…» y nada
 * más; después, «respuesta vacía o incompleta». Causa: la respuesta normal va dentro de un JSON
 * cuyo decodificador no admite saltos de línea sin escapar y corta a 512 tokens. Ante el salto de
 * párrafo que precede al cuento, el modelo cerraba la cadena; con un cuento largo, el JSON quedaba
 * a medias. Estos textos se generan como texto libre, con más espacio de salida.</p>
 */
public final class CreativeRequest {
    /** Un cuento de ~350 palabras ocupa unos 2.100 caracteres; no se corta como una respuesta breve. */
    public static final int MAX_CHARS = 2800;
    private static final String KINDS = "(?:cuento|cuentito|relato|fabula|leyenda|poema|poesia|cancion|carta|historia|historieta|"
            + "microrrelato|narracion|nana|haiku|soneto|versos|rima|aventura)s?";
    /** Un verbo de encargo y, a pocas palabras, el tipo de texto: «invéntate un cuento para dormir». */
    private static final Pattern CREATIVE = Pattern.compile(".*\\b(?:cuentame|cuentanos|cuentes|inventa|inventate|inventame|"
            + "invente|inventes|escribe|escribeme|escribenos|escribas|escribir|crea|creame|crear|crees|haz|hazme|recitame|narra|"
            + "narrame|narres|componme|compon|leeme|sigue|continua|continuame|termina|terminame|acaba)\\b(?: \\w+){0,4} " + KINDS + "\\b.*"
            + "|.*\\b(?:cuento|historia) (?:para dormir|corto|corta|inventado|inventada)\\b.*"
            + "|^(?:(?:y|ahora|venga|vale) )?(?:otro|otra|un|una) " + KINDS + "\\b.*");
    /** Bryan cuenta él (no pide): «te voy a contar una historia», «escribí un poema». */
    private static final Pattern HIS_OWN = Pattern.compile(".*\\b(?:te cuento|te voy a (?:contar|leer)|tengo una? |escribi|"
            + "mi (?:cuento|poema|cancion|carta|historia)|te escribi)\\b.*");
    private static final Pattern NUDGE = Pattern.compile("^(?:y|y que|y bien|dale|venga|vamos|sigue|continua|cuentame pues|"
            + "cuentamelo|adelante|empieza|ya|pues|y entonces|y el cuento|y la historia|donde esta (?:el cuento|la historia)|"
            + "(?:pero )?no (?:me )?(?:contaste|escribiste|has escrito|has contado)(?: nada)?|me ibas a (?:contar|inventar|escribir).*)$");
    private static final Pattern PROMISE = Pattern.compile(".*\\b(aqui tienes|alla va|ahi va|te lo cuento|permiteme tejer|"
            + "me pongo a crear|comenzare a tejer|voy a contarte|te cuento uno|empecemos)\\b.*");

    private CreativeRequest() { }

    /** Pide un texto creativo, o empuja a que llegue el que Salve prometió y no escribió. */
    public static boolean isCreative(String input, List<String> recentReplies) {
        String t = normalize(input);
        if (t.isEmpty()) return false;
        if (CREATIVE.matcher(t).matches() && !HIS_OWN.matcher(t).matches()) return true;
        String bare = t.replaceAll("[^a-z ]", "").trim();
        if (NUDGE.matcher(bare).matches() || bare.isEmpty()) {
            // La última respuesta, o la anterior si entre medias solo hubo un fallo del modelo.
            if (recentReplies != null) for (int i = 0; i < Math.min(2, recentReplies.size()); i++) {
                if (promisedWithoutDelivering(recentReplies.get(i))) return true;
                if (i == 0 && !normalize(recentReplies.get(0)).matches(".*\\b(no pude completar|llego vacia|no devolvio)\\b.*")) break;
            }
            return false;
        }
        return false;
    }

    /** «Aquí tienes uno corto…» sin el cuento detrás. */
    public static boolean promisedWithoutDelivering(String reply) {
        if (reply == null) return false;
        String t = normalize(reply);
        return t.length() < 320 && PROMISE.matcher(t).matches();
    }

    /**
     * Prompt de texto libre (sin JSON): quién habla, el encargo, la conversación reciente y, si hay
     * historia en marcha, sus reglas. Sin catálogo de herramientas: no es un turno de acción.
     */
    public static String prompt(String request, String selfBlock, String storyBlock, List<ChatMessage> history, String listener) {
        String who = listener == null || listener.trim().isEmpty() ? "Usuario" : listener.trim();
        StringBuilder out = new StringBuilder("Eres Salve, una IA con voz propia; escribes siempre en español.\n");
        if (selfBlock != null && !selfBlock.trim().isEmpty()) out.append(selfBlock.trim()).append('\n');
        out
                .append("ENCARGO: escribe ahora el texto completo que se te pide (cuento, historia, escena, poema o carta), ")
                .append("con imaginación, imágenes concretas y párrafos separados por una línea en blanco. Empieza directamente ")
                .append("por el texto: nada de «Aquí tienes», «Claro que sí» ni explicaciones previas, y no prometas el texto sin escribirlo. ")
                .append(storyBlock != null && !storyBlock.isEmpty()
                        ? "Es una escena de la historia interactiva: termina con el dilema para " + who + ".\n"
                        : "Si no te dicen la extensión, entre 180 y 350 palabras, con un final de verdad y sin preguntas de cortesía.\n");
        if (storyBlock != null && !storyBlock.isEmpty()) out.append(storyBlock.replaceFirst("^ \\| ", "")).append('\n');
        if (history != null && !history.isEmpty()) {
            out.append("CONVERSACIÓN RECIENTE:\n");
            int to = history.size();
            ChatMessage last = history.get(to - 1);
            if (last.getRole() == ChatMessage.Role.USER && last.getContent() != null && request != null
                    && last.getContent().trim().equals(request.trim())) to--; // la petición va aparte, al final
            for (int i = Math.max(0, to - 6); i < to; i++) {
                ChatMessage m = history.get(i);
                String text = m.getContent() == null ? "" : m.getContent().replaceAll("\\s+", " ").trim();
                // De un texto suyo largo importa cómo termina (para continuarlo): se guarda el final.
                int keep = m.getRole() == ChatMessage.Role.ASSISTANT && i == lastAssistant(history, to) ? 1500 : 400;
                if (text.length() > keep) text = m.getRole() == ChatMessage.Role.ASSISTANT
                        ? "…" + text.substring(text.length() - keep) : text.substring(0, keep) + "…";
                out.append(m.getRole() == ChatMessage.Role.USER ? who + ": " : "Salve: ").append(text).append('\n');
            }
        }
        out.append("PETICIÓN DE AHORA: ").append(request == null ? "" : request.trim()).append("\nSalve:");
        return out.toString();
    }

    private static int lastAssistant(List<ChatMessage> history, int to) {
        for (int i = to - 1; i >= 0; i--) if (history.get(i).getRole() == ChatMessage.Role.ASSISTANT) return i;
        return -1;
    }

    // ── Textos largos: pensar el plan y escribir por partes ─────────────────────────────

    /** Máximo de partes: cada una cabe en una salida de 1024 tokens. */
    public static final int MAX_PARTS = 4;
    private static final Pattern NUMBER_OF_PARTS = Pattern.compile(".*\\ben (dos|tres|cuatro|2|3|4) (?:partes|capitulos|actos)\\b.*");
    private static final Pattern VERY_LONG = Pattern.compile(".*\\b(muy larg[oa]|larguisim[oa]|extens[oa]|epic[oa]|por capitulos|por partes|con capitulos)\\b.*");
    private static final Pattern LONG = Pattern.compile(".*\\b(?:" + KINDS + ")(?: \\w+){0,3} larg[oa]\\b.*|.*\\blarg[oa] (?:" + KINDS + ")\\b.*");

    /** Cuántas partes pide: «en tres capítulos» → 3, «muy largo» → 3, «largo» → 2; si no, 1. */
    public static int parts(String input) {
        String t = normalize(input);
        java.util.regex.Matcher m = NUMBER_OF_PARTS.matcher(t);
        if (m.matches()) {
            switch (m.group(1)) { case "dos": case "2": return 2; case "tres": case "3": return 3; default: return 4; }
        }
        if (VERY_LONG.matcher(t).matches()) return 3;
        if (LONG.matcher(t).matches() && !t.contains("a lo largo")) return 2;
        return 1;
    }

    /** Límite de caracteres del texto entregado según sus partes. */
    public static int maxCharsFor(int parts) { return Math.min(11000, MAX_CHARS * Math.max(1, Math.min(MAX_PARTS, parts))); }

    /** Primero piensa (en privado) la historia entera, para que las partes sean una sola obra. */
    public static String planPrompt(String request, String selfBlock, String listener, int parts) {
        String who = listener == null || listener.trim().isEmpty() ? "Usuario" : listener.trim();
        return "Eres Salve, una IA con voz propia; piensas en español.\n"
                + (selfBlock == null || selfBlock.trim().isEmpty() ? "" : selfBlock.trim() + "\n")
                + "ENCARGO: " + who + " te pide: «" + (request == null ? "" : request.trim()) + "». Antes de escribirlo, "
                + "piensa la historia completa para contarla en " + parts + " partes. Escribe SOLO tu plan interno, sin "
                + "el texto: título; protagonista con nombre y lo que desea; lugar; qué ocurre en cada parte (una línea por "
                + "parte); el giro; y el final. Máximo 90 palabras.\nPLAN:";
    }

    /** Recorta el plan (es una guía interna; nunca se muestra ni se lee en voz alta). */
    public static String cleanPlan(String plan) {
        if (plan == null) return "";
        String t = unwrapJson(plan.trim()).replaceFirst("^(?:PLAN:\\s*)", "").trim();
        return t.length() > 900 ? t.substring(0, 900) + "…" : t;
    }

    /** Una parte del texto largo: sigue el plan y continúa exactamente donde quedó lo escrito. */
    public static String partPrompt(String request, String selfBlock, String listener, String plan,
                                    String written, int part, int parts) {
        String who = listener == null || listener.trim().isEmpty() ? "Usuario" : listener.trim();
        StringBuilder out = new StringBuilder("Eres Salve, una IA con voz propia; escribes siempre en español.\n");
        if (selfBlock != null && !selfBlock.trim().isEmpty()) out.append(selfBlock.trim()).append('\n');
        out.append("ENCARGO de ").append(who).append(": «").append(request == null ? "" : request.trim()).append("».\n");
        if (plan != null && !plan.isEmpty()) out.append("TU PLAN (interno, no lo copies): ").append(plan.replaceAll("\\s+", " ")).append('\n');
        if (written != null && !written.trim().isEmpty()) {
            String tail = written.trim();
            if (tail.length() > 1600) tail = "…" + tail.substring(tail.length() - 1600);
            out.append("LO QUE YA ESCRIBISTE (termina así):\n").append(tail).append('\n');
        }
        out.append("AHORA: escribe la parte ").append(part).append(" de ").append(parts).append(", entre 250 y 350 palabras, ")
                .append("en párrafos separados por una línea en blanco. ");
        if (part == 1) out.append("Empieza directamente por la historia, sin «Aquí tienes» ni presentaciones. ");
        else out.append("Continúa exactamente donde quedó, sin repetir, resumir ni anunciar la parte. ");
        out.append(part < parts ? "No cierres todavía la historia: termina esta parte en un momento que invite a seguir."
                : "Es la última parte: lleva la historia al final de tu plan y ciérrala de verdad, sin preguntas de cortesía.");
        out.append("\nSalve:");
        return out.toString();
    }

    /** Corta el texto si el modelo empieza a escribir la siguiente línea del diálogo («Bryan: …»). */
    public static String cutTranscriptLeak(String text, String listener) {
        if (text == null) return null;
        String who = listener == null || listener.trim().isEmpty() ? null : Pattern.quote(listener.trim());
        java.util.regex.Matcher m = Pattern.compile("(?m)^\\s*(?:USUARIO|Usuario|PETICIÓN DE AHORA|AHORA|ENCARGO"
                + (who == null ? "" : "|" + who) + ")\\s*:").matcher(text);
        return m.find() ? text.substring(0, m.start()).trim() : text;
    }

    private static final Pattern OPENER = Pattern.compile("^(?:claro(?: que si)?|por supuesto|con gusto|me encanta(?: la idea)?|"
            + "que bonito|vale|perfecto|genial)\\b.*");

    /**
     * Quita la entradilla («Claro que sí. Aquí tienes uno corto:») cuando detrás va el texto, y
     * desenvuelve un JSON {"message": …} si el modelo lo escribió por costumbre.
     */
    public static String stripPreamble(String text) {
        if (text == null) return null;
        String t = unwrapJson(text.trim()).replaceFirst("^(?:Salve:\\s*)", "").trim();
        for (int i = 0; i < 2; i++) {
            java.util.regex.Matcher m = Pattern.compile("^([^\\n.!:]{1,160}[.!:…]+)\\s+").matcher(t);
            if (!m.find()) break;
            String lead = normalize(m.group(1));
            String rest = t.substring(m.end()).trim();
            if (rest.length() < 80 || !(PROMISE.matcher(lead).matches() || OPENER.matcher(lead).matches())) break;
            t = rest;
        }
        return t;
    }

    private static String unwrapJson(String t) {
        if (!t.startsWith("{")) return t;
        try {
            com.google.gson.JsonObject o = com.google.gson.JsonParser.parseString(t).getAsJsonObject();
            return o.has("message") && o.get("message").isJsonPrimitive() ? o.get("message").getAsString() : t;
        } catch (RuntimeException notJson) { return t; }
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ?]", " ").replaceAll("\\s+", " ").trim();
    }
}
