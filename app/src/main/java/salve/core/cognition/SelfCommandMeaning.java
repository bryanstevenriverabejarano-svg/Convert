package salve.core.cognition;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.DoubleUnaryOperator;
import java.util.regex.Pattern;

/**
 * Entender por significado qué función propia le piden, aunque Bryan no use la frase exacta.
 *
 * <p>Cada función tiene frases de ejemplo y una frase canónica que su reconocedor de siempre ya
 * entiende («activa el modo oídos»). La petición se compara por significado (E5, el mismo modelo
 * de su memoria) con los ejemplos; si se parece de verdad a una y no a otra cosa, esa función se le
 * ofrece a su LLM, que decide si la usa. Los detalles (encender o apagar, qué pestaña, qué gesto)
 * salen de la frase canónica emparejada por el controlador, nunca de los argumentos del modelo.</p>
 *
 * <p>Las frases de {@link #OTHER} son cosas que NO son sus funciones (abrir apps, música, charla):
 * si la petición se parece más a eso, no se ofrece nada. Una negación explícita tampoco: los
 * embeddings apenas distinguen «activa» de «no actives».</p>
 *
 * <p>Lógica pura y sin dependencias (las funciones van por su id de {@code SelfCommandIntent.Command}):
 * el modelo de embeddings y su calibración se inyectan, y la CI la mide con el E5 real.</p>
 */
public final class SelfCommandMeaning {
    /** Normalizado con la calibración del propio modelo: 0 = sin relación, 1 = paráfrasis típica. */
    static final double MIN_RELATED = 0.75;
    /** Ventaja mínima sobre «otra cosa» y sobre la segunda función (normalizada). */
    static final double MIN_LEAD = 0.15;

    public interface Embedder { float[] embed(String text) throws Exception; }

    // Una prohibición explícita nunca se ofrece al modelo (también la usa SelfCommandIntent).
    static final Pattern NEGATED = Pattern.compile(
            "\\b(?:no|nunca|jamas)\\s+(?:(?:me|nos|te|le|les)\\s+)?"
            + "(?:abras|abre|actives|activa|enciendas|pongas|cambies|uses|escuches|"
            + "parpadees|muevas|brilles|juguemos|juegues|hagas|mandes)\\b");

    public static final class Meaning {
        /** Id de {@code SelfCommandIntent.Command}. */
        public final String command;
        /** Frase que su reconocedor de siempre entiende; de ella salen los detalles. */
        public final String canonical;
        final String[] examples;
        Meaning(String command, String canonical, String... examples) {
            this.command = command; this.canonical = canonical; this.examples = examples;
        }
    }

    /** Qué se entendió, con su puntuación para el registro. */
    public static final class Match {
        public final String command;
        public final String canonical;
        public final String example;
        public final double score;
        Match(Meaning meaning, String example, double score) {
            this.command = meaning.command; this.canonical = meaning.canonical; this.example = example; this.score = score;
        }
    }

    /** Ids de las funciones (los mismos nombres que SelfCommandIntent.Command). */
    static final class Ids {
        static final String
                BODY_ACTION = "BODY_ACTION",
                CAUSAL_MEMORY = "CAUSAL_MEMORY",
                CHESS_START = "CHESS_START",
                DEFAULT_ASSISTANT = "DEFAULT_ASSISTANT",
                EAR_MODE = "EAR_MODE",
                EVOLUTION_STATUS = "EVOLUTION_STATUS",
                IMPROVEMENT_PROPOSALS = "IMPROVEMENT_PROPOSALS",
                LISTEN = "LISTEN",
                MEMORY_HEALTH = "MEMORY_HEALTH",
                MIND_MAP = "MIND_MAP",
                MODEL_STATUS = "MODEL_STATUS",
                OPEN_JOURNAL = "OPEN_JOURNAL",
                SELF_PR_STATUS = "SELF_PR_STATUS",
                VOICE_ENGINE = "VOICE_ENGINE",
                WAKE_WORD_OFF = "WAKE_WORD_OFF",
                WAKE_WORD_ON = "WAKE_WORD_ON";
        private Ids() { }
    }

    static final List<Meaning> MEANINGS;
    static {
        List<Meaning> m = new ArrayList<>();
        m.add(new Meaning(Ids.EAR_MODE, "activa el modo oídos",
                "activa el modo oídos", "quiero que me oigas con tus propios oídos",
                "escúchame tú directamente en vez del dictado", "usa tu oído para entender cómo te hablo",
                "quiero que notes mi tono de voz cuando te hablo"));
        m.add(new Meaning(Ids.EAR_MODE, "desactiva el modo oídos",
                "desactiva el modo oídos", "deja de usar tus oídos y vuelve al dictado",
                "quita lo de escucharme con tu modelo", "vuelve a usar el reconocimiento de voz de Android"));
        m.add(new Meaning(Ids.VOICE_ENGINE, "usa tu voz nueva",
                "usa tu voz nueva", "habla con la voz más natural que tienes",
                "pon la voz que suena más humana", "cámbiate a la voz bonita"));
        m.add(new Meaning(Ids.VOICE_ENGINE, "usa la voz de Android",
                "usa la voz de Android", "vuelve a hablar con la voz de antes",
                "pon la voz robótica del sistema", "quita la voz nueva"));
        m.add(new Meaning(Ids.LISTEN, "escucha esto",
                "escucha esto", "pon atención a lo que suena ahora", "atiende a este sonido y dime qué es",
                "a ver qué te parece este ruido"));
        m.add(new Meaning(Ids.LISTEN, "escucha esta canción",
                "escucha esta canción", "¿sabes qué canción está sonando?", "adivina qué música suena",
                "dime de quién es la canción que se oye"));
        m.add(new Meaning(Ids.MIND_MAP, "abre el mapa de tu mente",
                "abre el mapa de tu mente", "enséñame cómo piensas por dentro", "quiero ver tu mente",
                "muéstrame las conexiones de tus ideas", "déjame ver cómo están organizados tus pensamientos"));
        m.add(new Meaning(Ids.OPEN_JOURNAL, "abre el registro de ejecución",
                "abre el registro de ejecución", "enséñame lo que has ido haciendo en tus tareas",
                "quiero ver tu registro", "muéstrame tus notas de lo que ejecutaste"));
        m.add(new Meaning(Ids.OPEN_JOURNAL, "abre la biblioteca de informes",
                "abre la biblioteca de informes", "enséñame tus informes en PDF",
                "quiero leer tus investigaciones guardadas", "muéstrame los documentos que has escrito"));
        m.add(new Meaning(Ids.BODY_ACTION, "parpadea",
                "parpadea", "abre y cierra los ojos para mí", "hazme un guiño", "mueve los párpados"));
        m.add(new Meaning(Ids.BODY_ACTION, "mueve el pelo",
                "mueve el pelo", "agita tu melena", "que se te mueva el cabello", "sacude la cabeza y el pelo"));
        m.add(new Meaning(Ids.BODY_ACTION, "brilla",
                "brilla", "ilumínate", "haz que se enciendan tus luces", "que brillen tus circuitos"));
        m.add(new Meaning(Ids.WAKE_WORD_ON, "activa la palabra de activación",
                "activa la palabra de activación", "quiero despertarte diciendo tu nombre",
                "que te actives cuando diga Salve", "quiero llamarte sin tocar el móvil"));
        m.add(new Meaning(Ids.WAKE_WORD_OFF, "desactiva la palabra de activación",
                "desactiva la palabra de activación", "deja de esperar a que diga tu nombre",
                "ya no quiero que te despiertes al oír Salve", "quita lo de activarte por tu nombre"));
        m.add(new Meaning(Ids.DEFAULT_ASSISTANT, "hazte mi asistente predeterminado",
                "hazte mi asistente predeterminado", "quiero que salgas al mantener el botón lateral",
                "sustituye al asistente de Google", "quiero que seas la asistente del móvil"));
        m.add(new Meaning(Ids.CHESS_START, "quiero jugar al ajedrez",
                "quiero jugar al ajedrez", "echamos una partida de ajedrez", "te reto al ajedrez",
                "saca el tablero que movemos peones"));
        // Las de solo lectura ya se ofrecen siempre; entenderlas solo ordena el catálogo.
        m.add(new Meaning(Ids.MEMORY_HEALTH, "cómo está tu memoria",
                "cómo está tu memoria", "¿recuerdas bien las cosas?", "¿cuántos recuerdos tienes guardados?"));
        m.add(new Meaning(Ids.MODEL_STATUS, "qué modelo usas",
                "qué modelo usas", "¿con qué cerebro estás pensando ahora?", "¿cómo va tu modelo de lenguaje?"));
        m.add(new Meaning(Ids.SELF_PR_STATUS, "cómo van tus PR",
                "cómo van tus PR", "¿se aprobaron los cambios que hiciste a tu código?", "¿qué pasó con tus parches en GitHub?"));
        m.add(new Meaning(Ids.IMPROVEMENT_PROPOSALS, "qué mejorarías de ti misma",
                "qué mejorarías de ti misma", "¿en qué fallas todavía?", "¿qué te gustaría arreglar de ti?"));
        m.add(new Meaning(Ids.EVOLUTION_STATUS, "en qué fase de tu evolución estás",
                "en qué fase de tu evolución estás", "¿cómo vas evolucionando?", "¿qué estás estudiando para mejorar?"));
        m.add(new Meaning(Ids.CAUSAL_MEMORY, "qué causas has aprendido",
                "qué causas has aprendido", "¿qué sabes de por qué me pasan las cosas?", "¿qué relaciones de causa y efecto conoces de mí?"));
        MEANINGS = Collections.unmodifiableList(m);
    }

    /** Peticiones que NO son sus funciones: si se parece más a esto, no se ofrece ninguna. */
    static final String[] OTHER = {
            "abre Spotify", "abre WhatsApp", "pon música de reggaeton", "llama a mi madre",
            "llévame a casa", "busca en internet las noticias de hoy", "cuéntame un cuento",
            "hola, ¿qué tal estás?", "hazme una foto", "sube el volumen", "enciende la linterna",
            "activa el wifi", "dibuja un gato", "¿qué tiempo hace mañana?", "te quiero mucho",
            "escríbele a Ana por WhatsApp", "investiga sobre los agujeros negros", "¿qué sabes sobre mí?"
    };

    /** Vectores de los ejemplos, calculados una vez por proceso. */
    public static final class Index {
        final float[][][] meanings;
        final float[][] other;
        Index(float[][][] meanings, float[][] other) { this.meanings = meanings; this.other = other; }
    }

    private SelfCommandMeaning() { }

    public static Index build(Embedder embedder) throws Exception {
        float[][][] vectors = new float[MEANINGS.size()][][];
        for (int i = 0; i < MEANINGS.size(); i++) {
            String[] examples = MEANINGS.get(i).examples;
            vectors[i] = new float[examples.length][];
            for (int j = 0; j < examples.length; j++) vectors[i][j] = embedder.embed(examples[j]);
        }
        float[][] other = new float[OTHER.length][];
        for (int i = 0; i < OTHER.length; i++) other[i] = embedder.embed(OTHER[i]);
        return new Index(vectors, other);
    }

    /**
     * @param relatedness coseno → 0..1 con la calibración de ESTE modelo en este móvil
     *                    (1 − sorpresa); negativo si no hay calibración válida.
     * @return la función entendida, o null si no se parece con claridad a ninguna.
     */
    public static Match understand(String input, float[] query, Index index, DoubleUnaryOperator relatedness) {
        if (input == null || query == null || index == null || relatedness == null) return null;
        String s = normalize(input);
        if (s.isEmpty() || s.length() > 300 || NEGATED.matcher(s).find()) return null;
        int best = -1, bestExample = -1;
        double bestScore = -2, second = -2;
        for (int i = 0; i < index.meanings.length; i++) {
            double score = -2; int example = -1;
            for (int j = 0; j < index.meanings[i].length; j++) {
                double c = cosine(query, index.meanings[i][j]);
                if (c > score) { score = c; example = j; }
            }
            if (score > bestScore) {
                // La segunda que cuenta es otra función (o la misma con otro detalle: encender vs apagar).
                second = bestScore; bestScore = score; best = i; bestExample = example;
            } else if (score > second) second = score;
        }
        double other = -2;
        for (float[] v : index.other) other = Math.max(other, cosine(query, v));
        if (best < 0) return null;
        double top = relatedness.applyAsDouble(bestScore);
        if (top < MIN_RELATED) return null;
        if (top - relatedness.applyAsDouble(other) < MIN_LEAD) return null;
        if (top - relatedness.applyAsDouble(second) < MIN_LEAD) return null;
        Meaning meaning = MEANINGS.get(best);
        return new Match(meaning, meaning.examples[bestExample], bestScore);
    }

    static String normalize(String raw) {
        return Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }

    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) return -2;
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        return na == 0 || nb == 0 ? -2 : dot / Math.sqrt(na * nb);
    }

    // ── Lo entendido en el turno actual ─────────────────────────────────────────
    // El controlador lo calcula una vez con la frase de Bryan; el catálogo y el ejecutor lo
    // consultan con esa misma frase. Otra frase (otro turno) no hereda nada.

    private static volatile String turnInput;
    private static volatile Match turnMatch;

    public static synchronized void remember(String input, Match match) {
        turnInput = input; turnMatch = match;
    }

    /** Lo entendido para exactamente esta frase, o null. */
    public static Match forTurn(String input) {
        String remembered;
        Match match;
        synchronized (SelfCommandMeaning.class) { remembered = turnInput; match = turnMatch; }
        return input != null && input.equals(remembered) ? match : null;
    }
}
