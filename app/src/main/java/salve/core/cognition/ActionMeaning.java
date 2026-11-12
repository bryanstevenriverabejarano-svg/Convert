package salve.core.cognition;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Entender por significado qué acción le piden, aunque Bryan no use la frase exacta: sus funciones
 * propias (self.command), sus cuentos y los mandos del móvil (ajustes, linterna y volumen, música,
 * abrir apps, rutas).
 *
 * <p>Cada acción tiene frases de ejemplo y una frase canónica que su reconocedor de siempre ya
 * entiende («activa el modo oídos», «apaga el wifi»). La petición se compara por significado (E5, el mismo modelo
 * de su memoria) con los ejemplos; si se parece de verdad a una y no a otra cosa, esa función se le
 * ofrece a su LLM, que decide si la usa. Los detalles (encender o apagar, qué pestaña, qué gesto)
 * salen de la frase canónica emparejada por el controlador, nunca de los argumentos del modelo.</p>
 *
 * <p>Las frases de {@link #OTHER} son cosas que NO son acciones suyas (charla, preguntas, buscar en
 * internet, WhatsApp, llamadas):
 * si la petición se parece más a eso, no se ofrece nada. Una negación explícita tampoco: los
 * embeddings apenas distinguen «activa» de «no actives».</p>
 *
 * <p>Lógica pura y sin dependencias (las funciones propias van por su id de
 * {@code SelfCommandIntent.Command}; las herramientas del móvil, por el nombre de la herramienta):
 * el modelo de embeddings y su calibración se inyectan, y la CI la mide con el E5 real.</p>
 */
public final class ActionMeaning {
    /*
     * Umbrales en unidades de la calibración de ESTE modelo (0 = media de frases sin relación,
     * 1 = media de paráfrasis), sin recortar. Medidos con el E5 real en la CI
     * (scripts/embeddings/VerifyActionMeaning.java): las peticiones reales quedan en 1,1–2,3 y ganan
     * a la segunda opción por ≥0,45; lo que no es suyo nunca cumple las tres a la vez.
     */
    static final double MIN_RELATED = 0.9;
    /** Ventaja mínima sobre «otra cosa» y sobre la segunda función. */
    static final double MIN_LEAD = 0.4;

    public interface Embedder { float[] embed(String text) throws Exception; }

    // Una prohibición explícita nunca se ofrece al modelo (también la usa SelfCommandIntent).
    static final Pattern NEGATED = Pattern.compile(
            "\\b(?:no|nunca|jamas)\\s+(?:(?:me|nos|te|le|les)\\s+)?"
            + "(?:abras|abre|actives|activa|enciendas|pongas|cambies|uses|escuches|"
            + "parpadees|muevas|brilles|juguemos|juegues|hagas|mandes)\\b");
    // «No quiero jugar ahora», «no me apetece»: un rechazo. «Ya no quiero QUE te despiertes…» sí es una
    // petición (apagar algo), por eso se excluye lo que sigue con «que».
    private static final Pattern REFUSED = Pattern.compile(
            "\\b(?:no|nunca|jamas)\\s+(?:(?:quiero|hace falta|me hace falta)\\b(?!\\s+que\\b)|me apetece\\b|necesito\\b)");

    public static final class Meaning {
        /** Id de {@code SelfCommandIntent.Command}, o nombre de herramienta del móvil (lleva punto). */
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
        public Match(Meaning meaning, String example, double score) {
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
                WAKE_WORD_ON = "WAKE_WORD_ON",
                STORY_SHELF = "STORY_SHELF",
                // Herramientas del móvil: el mismo nombre que ve el modelo en su catálogo.
                SETTING = "device.setting",
                CONTROL = "device.control",
                MEDIA = "device.media_control",
                PLAY_MUSIC = "device.play_music",
                SEARCH_MUSIC = "device.search_music",
                OPEN_APP = "device.open_app",
                NAVIGATE = "device.navigate";
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
                "que te actives cuando diga tu nombre", "quiero llamarte sin tocar el móvil"));
        m.add(new Meaning(Ids.WAKE_WORD_OFF, "desactiva la palabra de activación",
                "desactiva la palabra de activación", "deja de esperar a que diga tu nombre",
                "ya no quiero que te despiertes al oír tu nombre", "quita lo de activarte por tu nombre"));
        m.add(new Meaning(Ids.DEFAULT_ASSISTANT, "hazte mi asistente predeterminado",
                "hazte mi asistente predeterminado", "quiero que salgas al mantener el botón lateral",
                "sustituye al asistente de Google", "quiero que seas la asistente del móvil"));
        m.add(new Meaning(Ids.CHESS_START, "quiero jugar al ajedrez",
                "quiero jugar al ajedrez", "echamos una partida de ajedrez", "te reto al ajedrez",
                "saca el tablero que movemos peones"));
        m.add(new Meaning(Ids.STORY_SHELF, "repertorio de cuentos",
                "repertorio de cuentos", "¿qué cuentos tienes guardados?", "enséñame los cuentos que me has contado",
                "quiero elegir uno de tus cuentos"));

        // ── Ajustes del móvil (Shizuku): el ajuste y si se enciende o se apaga salen de la canónica.
        m.add(new Meaning(Ids.SETTING, "activa el wifi",
                "activa el wifi", "conéctate a la red wifi", "pon internet por wifi", "enciende el wifi del móvil"));
        m.add(new Meaning(Ids.SETTING, "desactiva el wifi",
                "desactiva el wifi", "desconéctate del wifi", "corta el wifi", "apaga el wifi del móvil"));
        m.add(new Meaning(Ids.SETTING, "activa el bluetooth",
                "activa el bluetooth", "enciende el bluetooth para conectar los cascos",
                "quiero conectar los auriculares inalámbricos"));
        m.add(new Meaning(Ids.SETTING, "desactiva el bluetooth",
                "desactiva el bluetooth", "apaga el bluetooth que gasta batería", "desconecta el bluetooth"));
        m.add(new Meaning(Ids.SETTING, "activa los datos móviles",
                "activa los datos móviles", "enciende los datos que no tengo wifi", "pon los datos del móvil"));
        m.add(new Meaning(Ids.SETTING, "desactiva los datos móviles",
                "desactiva los datos móviles", "apaga los datos que se me acaban los megas", "quita los datos del móvil"));
        m.add(new Meaning(Ids.SETTING, "activa el modo avión",
                "activa el modo avión", "pon el móvil en modo vuelo", "voy a despegar, pon el modo avión"));
        m.add(new Meaning(Ids.SETTING, "desactiva el modo avión",
                "desactiva el modo avión", "quita el modo vuelo", "ya he aterrizado, quita el modo avión"));
        m.add(new Meaning(Ids.SETTING, "activa el modo no molestar",
                "activa el modo no molestar", "que no me lleguen notificaciones", "silencia todas las notificaciones un rato"));
        m.add(new Meaning(Ids.SETTING, "desactiva el modo no molestar",
                "desactiva el modo no molestar", "quita el no molestar", "vuelve a dejar que me lleguen las notificaciones"));

        // ── Mandos físicos: linterna, volumen y vibración.
        m.add(new Meaning(Ids.CONTROL, "enciende la linterna",
                "enciende la linterna", "no veo nada, dame luz", "pon la luz del móvil", "alumbra que está oscuro"));
        m.add(new Meaning(Ids.CONTROL, "apaga la linterna",
                "apaga la linterna", "quita la luz del móvil", "ya no necesito la luz"));
        m.add(new Meaning(Ids.CONTROL, "sube el volumen",
                "sube el volumen", "no te oigo bien, habla más fuerte", "más alto, que no se escucha", "pon el sonido más fuerte"));
        m.add(new Meaning(Ids.CONTROL, "baja el volumen",
                "baja el volumen", "estás hablando muy alto", "más bajito, por favor", "pon el sonido más flojo"));
        m.add(new Meaning(Ids.CONTROL, "silencia el móvil",
                "silencia el móvil", "quita todo el sonido", "pon el móvil en silencio"));
        m.add(new Meaning(Ids.CONTROL, "vibra",
                "vibra", "haz vibrar el teléfono", "que tiemble el móvil un momento"));

        // ── Música que ya suena (sesión multimedia).
        m.add(new Meaning(Ids.MEDIA, "reanuda la música",
                "reanuda la música", "sigue con la canción que estaba sonando", "vuelve a poner la música donde estaba"));
        m.add(new Meaning(Ids.MEDIA, "pausa",
                "pausa", "para la música un momento", "detén la canción", "calla la música"));
        m.add(new Meaning(Ids.MEDIA, "siguiente canción",
                "siguiente canción", "esta no me gusta, pasa a otra", "salta este tema", "cambia de canción"));
        m.add(new Meaning(Ids.MEDIA, "canción anterior",
                "canción anterior", "vuelve a la que sonaba antes", "pon otra vez el tema de antes"));
        m.add(new Meaning(Ids.MEDIA, "qué canción suena",
                "qué canción suena", "¿cómo se llama lo que tengo puesto en Spotify?", "dime el título de la canción que tengo puesta"));
        // ── Música nueva, apps y rutas: el destino lo pone el modelo y el controlador comprueba que
        //    aparece en la frase de Bryan; aquí solo se entiende la clase de acción.
        m.add(new Meaning(Ids.PLAY_MUSIC, "pon música",
                "ponme algo de Rosalía", "quiero escuchar a Queen", "me apetece oír música de los ochenta",
                "pon el último disco de Bad Bunny"));
        m.add(new Meaning(Ids.SEARCH_MUSIC, "busca música",
                "busca canciones de Shakira en Spotify", "encuéntrame el álbum de Estopa", "búscame música para estudiar"));
        m.add(new Meaning(Ids.OPEN_APP, "abre la app",
                "abre Instagram", "quiero entrar en YouTube", "llévame a la aplicación del banco", "enséñame el calendario del móvil"));
        m.add(new Meaning(Ids.NAVIGATE, "llévame",
                "llévame al trabajo", "¿cómo llego a la estación?", "dame la ruta hasta Bilbao", "guíame hasta el hospital"));

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
            "llama a mi madre", "escríbele a Ana por WhatsApp", "busca en internet las noticias de hoy",
            "investiga sobre los agujeros negros", "cuéntame un cuento nuevo de dragones", "hola, ¿qué tal estás?",
            "buenas noches", "buenos días", "hasta mañana", "te quiero mucho", "¿qué tiempo hace mañana?",
            "dibuja un gato", "hazme una foto", "¿qué ves en esta foto?", "recuérdame una cosa mañana",
            "¿qué sabes sobre mí?", "explícame cómo funciona un motor", "estoy cansado", "¿qué opinas de esto?"
    };

    /** Vectores de los ejemplos, calculados una vez por proceso. */
    public static final class Index {
        final float[][][] meanings;
        final float[][] other;
        Index(float[][][] meanings, float[][] other) { this.meanings = meanings; this.other = other; }
    }

    private ActionMeaning() { }

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

    /** Lo que midió, para el registro y la CI: la mejor función, la segunda y «otra cosa». */
    public static final class Scores {
        public final int best, bestExample, otherIndex;
        public final double bestScore, second, other;
        Scores(int best, int bestExample, double bestScore, double second, double other, int otherIndex) {
            this.best = best; this.bestExample = bestExample; this.bestScore = bestScore;
            this.second = second; this.other = other; this.otherIndex = otherIndex;
        }
        public String bestId() { return best < 0 ? "" : MEANINGS.get(best).command; }
        public String bestExampleText() { return best < 0 ? "" : MEANINGS.get(best).examples[bestExample]; }
        public String otherText() { return otherIndex < 0 ? "" : OTHER[otherIndex]; }
    }

    /** El texto que se vectoriza: sin su nombre al principio o al final («buenas noches, Salve»). */
    public static String queryText(String input) {
        if (input == null) return "";
        return input.trim().replaceFirst("(?i)^(?:(?:oye|hola|hey|vale)[,\\s]+)?salve[,.:;!\\s]+", "")
                .replaceFirst("(?i)[,\\s]+salve[.!?¡¿\\s]*$", "").trim();
    }

    public static Scores measure(float[] query, Index index) {
        int best = -1, bestExample = -1, otherIndex = -1;
        double bestScore = -2, second = -2, other = -2;
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
        for (int i = 0; i < index.other.length; i++) {
            double c = cosine(query, index.other[i]);
            if (c > other) { other = c; otherIndex = i; }
        }
        return new Scores(best, bestExample, bestScore, second, other, otherIndex);
    }

    /** Una prohibición o un rechazo explícito: nunca se convierte en acción. */
    static boolean refused(String input) {
        String s = normalize(input);
        return NEGATED.matcher(s).find() || REFUSED.matcher(s).find();
    }

    /**
     * @param relatedMean   media del coseno entre paráfrasis de la calibración simétrica de este móvil
     * @param unrelatedMean media del coseno entre frases sin relación de esa misma calibración
     * @return la función entendida, o null si no se parece con claridad a ninguna.
     */
    public static Match understand(String input, float[] query, Index index, double relatedMean, double unrelatedMean) {
        if (input == null || query == null || index == null) return null;
        double span = relatedMean - unrelatedMean;
        if (!(span > 1e-6)) return null;
        String s = normalize(input);
        if (s.isEmpty() || s.length() > 300 || refused(input)) return null;
        Scores m = measure(query, index);
        if (m.best < 0) return null;
        if ((m.bestScore - unrelatedMean) / span < MIN_RELATED) return null;
        if ((m.bestScore - m.other) / span < MIN_LEAD) return null;
        if ((m.bestScore - m.second) / span < MIN_LEAD) return null;
        Meaning meaning = MEANINGS.get(m.best);
        return new Match(meaning, meaning.examples[m.bestExample], m.bestScore);
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

    /**
     * La frase canónica de lo entendido en este turno si es para esa herramienta, o null. Los
     * reconocedores del móvil la usan cuando la frase de Bryan no es la exacta: así el ajuste, el mando
     * o la acción de música salen del controlador, no del modelo.
     */
    public static String canonicalFor(String input, String tool) {
        Match match = forTurn(input);
        return match != null && match.command.equals(tool) ? match.canonical : null;
    }

    /** Lo entendido para exactamente esta frase, o null. */
    public static Match forTurn(String input) {
        String remembered;
        Match match;
        synchronized (ActionMeaning.class) { remembered = turnInput; match = turnMatch; }
        return input != null && input.equals(remembered) ? match : null;
    }
}
