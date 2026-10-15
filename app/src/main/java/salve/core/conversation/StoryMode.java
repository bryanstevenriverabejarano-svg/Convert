package salve.core.conversation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Historias interactivas: Salve inventa y narra, Bryan decide.
 *
 * <p>15/10/2026: jugando a «tú el explorador, yo la guía», Salve repetía el mensaje de Bryan al
 * empezar, volvía a preguntar en vez de avanzar aunque él dijera «lo demás decídelo tú» y, cuando
 * su personaje sintió «una presencia que añoro», respondió como terapeuta. Mientras hay una
 * historia en marcha, todo lo que escribe Bryan se lee dentro de la ficción y Salve tiene la
 * iniciativa del narrador.</p>
 */
public final class StoryMode {
    /** Una historia sin tocar durante este tiempo deja de estar activa (se puede continuar). */
    public static final long IDLE_MS = 12L * 60 * 60 * 1000;
    static final int MAX_BEATS = 8;

    private static final Pattern START = Pattern.compile(".*\\b(juguemos|jugamos a|vamos a jugar|quiero jugar|"
            + "inventa(?:te|r)? (?:un|una|otro|otra) (?:juego|historia|aventura|partida)|juego de rol|partida de rol|rol de|"
            + "historia interactiva|cuentame una historia|empecemos (?:la|una) (?:historia|aventura|partida)|"
            + "yo (?:soy|sere) (?:el|la) \\w+ y tu (?:eres |seras )?(?:el|la) \\w+)\\b.*");
    private static final Pattern RESUME = Pattern.compile(".*\\b(continuemos|sigamos|seguimos|retomemos|retomamos|continua) "
            + "(?:con )?(?:la |nuestra )?(?:historia|aventura|partida|juego)\\b.*");
    private static final Pattern END = Pattern.compile(".*\\b(fin del juego|fin de la historia|salir del juego|salgamos del juego|"
            + "terminamos el juego|terminemos el juego|paramos el juego|paremos el juego|dejemos de jugar|deja de jugar|dejar de jugar|"
            + "volvamos a hablar normal|hablemos en serio|fuera del juego del todo)\\b.*");
    private static final Pattern DELEGATE = Pattern.compile(".*\\b(decidelo tu|lo demas (?:lo )?(?:decides|decide|eliges) tu|"
            + "lo demas decidelo|elige tu|tu decides|sorprendeme|lo que tu quieras|como tu quieras)\\b.*");

    /** Estado persistente de la historia (se guarda como JSON). */
    public static final class State {
        public boolean active;
        public long startedAt, lastAt;
        public String premise = "";
        public List<String> beats = new ArrayList<>();
    }

    private StoryMode() { }

    public static boolean startsStory(String input) {
        String t = normalize(input);
        return START.matcher(t).matches() && !t.contains("ajedrez"); // el ajedrez tiene su propio tablero
    }
    public static boolean resumesStory(String input) { return RESUME.matcher(normalize(input)).matches(); }
    public static boolean endsStory(String input) { return END.matcher(normalize(input)).matches(); }
    public static boolean delegates(String input) { return DELEGATE.matcher(normalize(input)).matches(); }

    /** ¿Este mensaje se juega dentro de la historia? Actualiza el estado (inicio, reanudación, caducidad). */
    public static boolean enter(State state, String input, long now) {
        if (state == null) return false;
        if (state.active && now - state.lastAt > IDLE_MS) state.active = false;
        if (!state.active && startsStory(input)) {
            state.active = true; state.startedAt = now; state.premise = bounded(input, 300); state.beats = new ArrayList<>();
        } else if (!state.active && resumesStory(input) && state.beats != null && !state.beats.isEmpty()) {
            state.active = true;
        } else if (state.active && state.beats.size() < 2 && input != null && input.length() < 300) {
            // Lo que concreta al principio (género, papeles) forma parte de la premisa.
            state.premise = bounded(state.premise + " / " + input.trim(), 420);
        }
        if (state.active) state.lastAt = now;
        return state.active;
    }

    /** Instrucciones del turno: rol de narradora, iniciativa y lo ocurrido hasta ahora. */
    public static String promptBlock(State state, String input) {
        StringBuilder out = new StringBuilder(" | MODO_HISTORIA (activo): eres la narradora y directora de juego de una historia "
                + "interactiva que inventas con Bryan; él es el protagonista y decide sus actos. Reglas: "
                + "1) Todo lo que Bryan escribe es lo que hace, dice o siente su personaje dentro de la historia: nunca lo tomes "
                + "como una confesión personal ni respondas como terapeuta. "
                + "2) No repitas ni parafrasees su mensaje al empezar: avanza directamente con la acción. "
                + "3) Narra en segunda persona con escenas concretas: lugar, sensaciones, personajes y consecuencias de lo que hizo. "
                + "4) Ten iniciativa: introduce giros, personajes y peligros con nombre propio. "
                + "5) Cierra la escena con un dilema o desafío concreto (dos o tres caminos, o algo que exige actuar), "
                + "no con preguntas sobre sentimientos. 6) Sé coherente con lo ocurrido. 7) Entre 80 y 170 palabras. "
                + "Si escribe entre paréntesis, responde como Salve y vuelve a la historia.");
        if (delegates(input))
            out.append(" AHORA: Bryan te deja decidir; decide tú lo que falte y continúa la escena sin volver a preguntarle por ello.");
        if (state.premise != null && !state.premise.isEmpty()) out.append(" PREMISA: ").append(state.premise).append('.');
        if (state.beats != null && !state.beats.isEmpty())
            out.append(" HISTORIA_HASTA_AHORA: ").append(String.join(" → ", state.beats)).append('.');
        return out.toString();
    }

    /** Guarda un resumen breve de la escena narrada para mantener la coherencia. */
    public static void addBeat(State state, String reply) {
        if (state == null || reply == null || reply.trim().isEmpty()) return;
        String first = reply.trim().split("(?<=[.!?…])\\s+")[0];
        if (first.length() < 40 && reply.trim().length() > first.length())
            first = reply.trim().substring(0, Math.min(reply.trim().length(), 160));
        if (state.beats == null) state.beats = new ArrayList<>();
        state.beats.add(bounded(first, 160));
        while (state.beats.size() > MAX_BEATS) state.beats.remove(0);
    }

    /**
     * Quita la frase inicial que solo repite el mensaje de Bryan («Un pantano mágico.», «La
     * búsqueda de un conocimiento prohibido.»). Nunca deja la respuesta vacía.
     */
    public static String stripEcho(String reply, String input) {
        if (reply == null || input == null) return reply;
        String trimmed = reply.trim();
        String[] parts = trimmed.split("(?<=[.!?…])\\s+", 2);
        if (parts.length < 2) return reply;
        Set<String> said = words(input), first = words(parts[0]);
        if (first.isEmpty() || said.isEmpty()) return reply;
        Set<String> common = new HashSet<>(first);
        common.retainAll(said);
        boolean echo = common.size() * 10 >= first.size() * 7 && first.size() <= Math.max(said.size() + 3, 4);
        boolean filler = normalize(parts[0]).matches("(entiendo|ya veo|vale|de acuerdo|perfecto)");
        return (echo || filler) && parts[1].trim().length() >= 12 ? parts[1].trim() : reply;
    }

    private static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        for (String w : normalize(text).split(" ")) if (w.length() >= 3) out.add(w);
        return out;
    }

    private static String bounded(String value, int max) {
        String v = value == null ? "" : value.replaceAll("\\s+", " ").trim();
        return v.length() <= max ? v : v.substring(0, max - 1) + "…";
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9ñ ]", " ").replaceAll("\\s+", " ").trim();
    }
}
