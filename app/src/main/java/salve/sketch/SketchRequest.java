package salve.sketch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reconoce cuándo Bryan pide un dibujo ("dibújame un árbol y la luna", "dibújate", "¿qué sabes dibujar?"). */
public final class SketchRequest {
    public enum Kind { NONE, DRAW, SELF, FREE, LIST, UNKNOWN, AGAIN }

    public final Kind kind;
    public final SketchScene scene;
    /** Pidió buscar referencias en internet (aunque ya lo tenga en su repertorio). */
    public final boolean references;
    /** Lo que pidió y no está en el repertorio (para decirlo con honestidad). */
    public final String unknown;
    /**
     * Pidió que lo mejore («dibújalo mejor», «con más detalle», «el que te enseñé estaba feo»): no repite
     * la versión que ya tiene, la rehace ella con otras referencias o pensando sus partes.
     */
    public final boolean improve;
    /** Lo que quiere distinto («con más detalle, pose diferente»); vacío si nada. */
    public final String wish;
    /**
     * Pedido sin verbo de dibujar («mejóralo, busca referencias…»): sólo cuenta si se estaba hablando
     * de un dibujo hace poco.
     */
    public boolean implicit;

    private SketchRequest(Kind kind, SketchScene scene, String unknown) { this(kind, scene, unknown, false); }
    private SketchRequest(Kind kind, SketchScene scene, String unknown, boolean references) {
        this(kind, scene, unknown, references, false, "");
    }
    private SketchRequest(Kind kind, SketchScene scene, String unknown, boolean references, boolean improve, String wish) {
        this.kind = kind; this.scene = scene; this.unknown = unknown == null ? "" : unknown; this.references = references;
        this.improve = improve; this.wish = wish == null ? "" : wish;
    }

    private static final Pattern SELF = Pattern.compile(
            "\\b(dibujate|pintate|trazate|autorretrato|autoretrato|(puedes|podrias|sabes|quiero que|vuelve a) (dibujarte|pintarte)"
            + "|(dibuja|pinta|traza)( a)? ti misma|(dibuja|pinta|traza|dibujar|pintar) (tu|tu propio) (cuerpo|figura|cuerpo astral|retrato)"
            + "|hazte un (dibujo|retrato))\\b");
    private static final Pattern LIST = Pattern.compile(
            "\\bque (cosas |dibujos )?(puedes|sabes) (dibujar|pintar|trazar)\\b|\\bque dibujos (sabes|puedes)\\b");
    private static final Pattern VERB = Pattern.compile(
            "\\b(dibuja|dibujame|dibujanos|dibujale|pinta|pintame|pintanos|traza|trazame|ilustra|ilustrame|bosqueja"
            + "|(haz|hazme|haznos|puedes hacer|puedes hacerme|podrias hacer|podrias hacerme) (un|otro) dibujo( de| del)?"
            + "|(puedes|podrias|sabes|quiero que|me gustaria que|vuelve a) (dibujar|pintar|trazar|dibujarme|pintarme)"
            + "|(quiero|quisiera) (ver )?un dibujo( de| del)?"
            + "|dibujalo|dibujala|dibujalos|dibujalas|dibujamelo|dibujamela|pintalo|pintala|trazalo|trazala)\\b");
    /** «Busca referencias (en internet) de un cochinillo», «busca imágenes de…». */
    private static final Pattern REFERENCES = Pattern.compile(
            "\\b(busca|buscar|buscas|busques|mira|consulta)\\b.*\\b(ref\\w{0,5}ncias?|imagen|imagenes|fotos|dibujos|internet)\\b"
            + "|\\bref\\w{0,5}ncias?\\b"); // tolera erratas: «refwrencias», «referncias»
    private static final Pattern REFERENCE_SUBJECT = Pattern.compile(
            "\\b(?:ref\\w{0,5}ncias?|imagenes|imagen|fotos|dibujos)(?: en internet)? (?:de|del|sobre) (.+)$");
    /** Quiere una versión mejor, no la misma. */
    private static final Pattern IMPROVE = Pattern.compile(
            "\\b(mejor|mejora|mejoralo|mejorala|mejorarlo|mejorarla|mejoralos|perfeccionalo|perfecciona"
            + "|mas (bonito|bonita|detalle|detallado|detallada|realista|parecido|parecida|reconocible)|con (mucho )?(mas )?detalle"
            + "|(otra|una|con una|con otra) pose|pose (diferente|distinta|nueva)|de otra (forma|manera)|otra version|version (nueva|tuya|propia)"
            + "|(estaba|esta|era|es|quedo|salio) (muy |un poco |bastante )?(feo|fea|mal|raro|rara)|no se parec\\w*|que se parezca)\\b");
    private static final Pattern STRONG_IMPROVE = Pattern.compile(
            "\\b(mejoralo|mejorala|mejorarlo|mejorarla|perfeccionalo|con mas detalle|mas detalle|dale mas detalle)\\b");
    private static final Pattern REF_WORD = Pattern.compile("\\bref\\w{0,5}ncias?\\b");
    /** «… que sea un vestido de verano». */
    private static final Pattern THAT_IS = Pattern.compile("\\bque sea (?:un|una|el|la|unos|unas) (.+)$");
    /** «Mejóralo» a secas: se refiere al último dibujo. */
    private static final Pattern IMPROVE_ONLY = Pattern.compile(
            "^(salve )?(mejoralo|mejorala|mejora (el|tu|ese|este) dibujo|hazlo mejor|hazla mejor|dibujalo mejor|dibujala mejor"
            + "|intentalo (otra vez|de nuevo)( pero)?( mejor)?|otra vez pero mejor|puedes mejorarlo|puedes mejorarla)( por favor| porfa)?$");
    private static final Pattern WISH = Pattern.compile(
            "\\b(con (mucho )?mas detalle|mas detallad[oa]|(una |otra )?pose (diferente|distinta|nueva)|otra pose|mas realista"
            + "|mas bonit[oa]|mas reconocible|de perfil|de frente|sentad[oa]|de pie|corriendo|volando|saltando|durmiendo|comiendo"
            + "|a cuatro patas|con los brazos (abiertos|arriba)|de lado)\\b");
    private static final Pattern PRONOUN_ONLY = Pattern.compile(
            "^(dibujalo|dibujala|dibujalos|dibujalas|dibujamelo|dibujamela|pintalo|pintala|trazalo|trazala|hazlo)$");
    private static final Pattern NEGATED = Pattern.compile("\\b(no|deja de|para de|nunca) (me )?(dibujes|pintes|dibujar|pintar)\\b");
    private static final Pattern FREE = Pattern.compile("^(algo|lo que (tu )?quieras|lo que te apetezca|lo que (te )?guste|cualquier cosa|un dibujo|algo bonito|algo para mi)?$");
    private static final Set<String> FILLER = new HashSet<>(Arrays.asList(
            "un", "una", "unos", "unas", "el", "la", "los", "las", "lo", "de", "del", "al", "a", "y", "e", "con", "en",
            "mi", "mis", "tu", "tus", "su", "sus", "me", "te", "nos", "por", "favor", "porfa", "porfavor", "salve",
            "ahora", "otra", "otro", "vez", "pequeno", "pequena", "grande", "bonito", "bonita", "simple", "sencillo",
            "que", "se", "vea", "ver", "para", "mi", "dibujo", "dibujar", "muy", "poco", "rapido", "aqui", "hola",
            "busca", "buscar", "buscas", "referencia", "referencias", "imagen", "imagenes", "fotos", "internet",
            "analiza", "analizalas", "analizala", "analizas", "hanalizas", "hazlo", "haces", "lo", "las", "les", "y",
            "puedes", "podrias", "por", "ti", "encima", "debajo", "junto", "sobre", "bajo", "cerca", "lado",
            "mejor", "mejora", "mejoralo", "mejorala", "mejorarlo", "mejorarla", "detalle", "detallado", "detallada", "pose",
            "diferente", "distinta", "nueva", "nuevo", "forma", "manera", "version", "pero", "mas", "realista", "parecido",
            "parecida", "feo", "fea", "raro", "rara", "mal", "estaba", "esta", "era", "ese", "esa", "este", "umm", "um", "vale",
            "ok", "bueno", "nada", "si", "dale", "venga", "quiero", "hazlo", "haslo", "pues", "joder", "ensene", "ensenaste", "hice", "hiciste", "anterior", "antes", "perfeccionalo", "reconocible"));

    public static SketchRequest parse(String input, SketchLibrary library) {
        String text = SketchLibrary.normalize(input);
        if (library == null || text.isEmpty() || text.length() > 400 || NEGATED.matcher(text).find())
            return new SketchRequest(Kind.NONE, null, null);
        if (LIST.matcher(text).find()) return new SketchRequest(Kind.LIST, null, null);
        if (SELF.matcher(text).find()) {
            SketchLibrary.Motif self = library.byId("salve");
            return self == null ? new SketchRequest(Kind.NONE, null, null)
                    : new SketchRequest(Kind.SELF, SketchScene.compose(java.util.Collections.singletonList(self)), null);
        }
        boolean references = REFERENCES.matcher(text).find();
        boolean improve = IMPROVE.matcher(text).find();
        String wish = wishes(text);
        if (IMPROVE_ONLY.matcher(text).matches()) return new SketchRequest(Kind.AGAIN, null, null, true, true, wish);
        Matcher verb = VERB.matcher(text);
        if (!verb.find()) {
            // «Mejóralo, busca referencias que sea un vestido de verano»: es un dibujo aunque no diga «dibuja».
            // Sólo vale justo después de hablar de un dibujo (lo comprueba quien llama con {@link #implicit}).
            boolean strong = STRONG_IMPROVE.matcher(text).find();
            if (!strong && !REF_WORD.matcher(text).find()) return new SketchRequest(Kind.NONE, null, null);
            Matcher sea = THAT_IS.matcher(text);
            Matcher about = REFERENCE_SUBJECT.matcher(text);
            String wanted = meaningful(sea.find() ? sea.group(1) : about.find() ? about.group(1) : "");
            SketchRequest r = wanted.isEmpty() ? new SketchRequest(Kind.AGAIN, null, null, references, improve || strong, wish)
                    : new SketchRequest(Kind.UNKNOWN, null, wanted, true, improve || strong, wish);
            r.implicit = true;
            return r;
        }
        if (improve) {
            // Rehacerlo mejor: nunca la versión guardada; ella vuelve a buscar o a pensar sus partes.
            String wanted = improvedSubject(input, library);
            return wanted.isEmpty() ? new SketchRequest(Kind.AGAIN, null, null, true, true, wish)
                    : new SketchRequest(Kind.UNKNOWN, null, wanted, true, true, wish);
        }
        Matcher about = REFERENCE_SUBJECT.matcher(text);
        String subject = about.find() ? about.group(1).trim() : text.substring(verb.end()).trim();
        // «Un vestido de verano blanco con volantes, dibújalo»: el objeto va antes del verbo.
        if (meaningful(subject).isEmpty() && verb.start() > 0) subject = text.substring(0, verb.start()).trim();
        if (references) {
            // Con referencias, busca en internet aunque el motivo ya exista en su repertorio.
            String wanted = meaningful(subject);
            if (wanted.isEmpty() && about.find(0)) wanted = meaningful(about.group(1));
            return wanted.isEmpty() ? new SketchRequest(Kind.AGAIN, null, null, true)
                    : new SketchRequest(Kind.UNKNOWN, null, wanted, true);
        }
        if (PRONOUN_ONLY.matcher(verb.group(1)).matches() && meaningful(subject).isEmpty())
            return new SketchRequest(Kind.AGAIN, null, null);
        List<SketchLibrary.Motif> motifs = new ArrayList<>();
        for (SketchLibrary.Mention m : library.mentions(subject, false)) if (!m.motif.self) motifs.add(m.motif);
        if (!motifs.isEmpty()) return new SketchRequest(Kind.DRAW, SketchScene.compose(motifs), null);
        if (FREE.matcher(subject).matches()) return new SketchRequest(Kind.FREE, null, null);
        String rest = meaningful(subject);
        return rest.isEmpty() ? new SketchRequest(Kind.FREE, null, null)
                : new SketchRequest(Kind.UNKNOWN, null, rest);
    }

    // ───────────── seguimiento de una conversación de dibujo ─────────────

    public enum FollowUp { NONE, GO, SHOW, IMPROVE }

    /** «Hazlo otra vez» sobre lo último que se estaba dibujando. */
    public static SketchRequest again(boolean improve, String wish) {
        return new SketchRequest(Kind.AGAIN, null, null, false, improve, wish);
    }

    private static final Pattern GO = Pattern.compile(
            "^(si|vale|ok|okey|venga|dale|claro|perfecto|adelante|bueno|pues)?( (si|vale|venga|dale|claro|adelante))*"
            + "( (hazlo|haslo|hazla|dale|adelante|empieza|por favor|porfa|ya))*$");
    private static final Pattern SHOW = Pattern.compile(
            "\\b(no (me )?(lo )?(estas )?(mostrando|ensenando|veo)|donde esta|quiero ver|ver) (el |tu |ese )?dibujo\\b"
            + "|^\\?+$|^(y )?(el )?dibujo\\??$");
    private static final Pattern MORE = Pattern.compile(
            "\\b(mas detalle|mas detallado|detallalo|mejoralo|mejorala|hazlo mejor|otra vez|de nuevo|intentalo)\\b");
    private static final String[] SHOW_WORDS = {"muestrame", "muestramelo", "muestramela", "ensename", "ensenamelo", "ensenamela"};

    /**
     * Respuestas cortas que, justo después de hablar de un dibujo, siguen pidiéndolo: «vale, hazlo»,
     * «sí, dale más detalle», «muéstrame el dibujo», «?». Tolera erratas («muestrsme»).
     */
    public static FollowUp followUp(String input) {
        String t = SketchLibrary.normalize(input);
        if (input != null && input.trim().matches("\\?+")) return FollowUp.SHOW;
        if (t.isEmpty() || t.length() > 160) return FollowUp.NONE;
        if (MORE.matcher(t).find()) return FollowUp.IMPROVE;
        if (SHOW.matcher(t).find()) return FollowUp.SHOW;
        for (String w : t.split(" ")) for (String target : SHOW_WORDS)
            if (Math.abs(w.length() - target.length()) <= 2 && w.length() >= 7 && distance(w, target) <= 2) return FollowUp.SHOW;
        if (t.length() <= 30 && GO.matcher(t).matches()) return FollowUp.GO;
        return FollowUp.NONE;
    }

    static int distance(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++)
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            int[] tmp = prev; prev = cur; cur = tmp;
        }
        return prev[b.length()];
    }

    /** Lo que quiere distinto, en sus palabras («con mas detalle, pose diferente»). */
    static String wishes(String normalized) {
        Matcher m = WISH.matcher(normalized);
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        while (m.find() && found.size() < 4) found.add(m.group(1));
        return String.join(", ", found);
    }

    /**
     * Qué quiere que mejore: la frase con el verbo de dibujar (sin «busca referencias, el que te enseñé
     * estaba feo»), el nombre de un motivo conocido si lo menciona, o sus palabras con contenido.
     */
    static String improvedSubject(String raw, SketchLibrary library) {
        for (String sentence : raw.split("[.!?;\\n]+")) {
            String t = SketchLibrary.normalize(sentence);
            Matcher verb = VERB.matcher(t);
            if (!verb.find()) continue;
            String after = t.substring(verb.end()).trim();
            if (meaningful(cut(after)).isEmpty() && verb.start() > 0) after = t.substring(0, verb.start()).trim();
            for (SketchLibrary.Mention m : library.mentions(after, false))
                if (!m.motif.self) return LearnedSketches.stripArticle(cleanName(m.motif.name));
            Matcher about = REFERENCE_SUBJECT.matcher(after);
            String wanted = meaningful(about.find() ? about.group(1) : cut(after));
            if (!wanted.isEmpty()) return wanted;
        }
        return "";
    }

    /** Corta en lo que ya no es el objeto: «un gorila con más detalle» → «un gorila». */
    private static String cut(String after) {
        Matcher m = Pattern.compile("\\b(con|pero|que|en|de forma|de otra|mas|y busca|busca)\\b").matcher(after);
        return m.find() && m.start() > 2 ? after.substring(0, m.start()).trim() : after;
    }

    private static final Pattern TRAILING_ASK = Pattern.compile(
            "(?:[\\s.,;:!¡]+(?:mej[oó]ralo|mej[oó]rala|mejorarlo|mejorarla|perfecci[oó]nalo|por favor|porfa|plis|mejor))+[\\s.,;:!¡]*$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    /**
     * Nombre de un dibujo sin la petición pegada: «Muñeco de nieve mejóralo..» → «muñeco de nieve».
     * @return el nombre limpio en minúsculas (o el original si quedaría vacío)
     */
    public static String cleanName(String name) {
        if (name == null) return "";
        String n = name.trim().toLowerCase(java.util.Locale.ROOT);
        String cleaned = TRAILING_ASK.matcher(n).replaceAll("").replaceAll("[.,;:!?¡¿]+", " ").replaceAll("\\s+", " ").trim();
        return cleaned.length() < 2 ? n : cleaned;
    }

    /** ¿El nombre traía una petición de mejora («… mejóralo»)? */
    public static boolean asksToImprove(String name) {
        return name != null && TRAILING_ASK.matcher(name.trim().toLowerCase(java.util.Locale.ROOT)).find()
                && SketchLibrary.normalize(name).matches(".*\\b(mejoralo|mejorala|mejorarlo|mejorarla|perfeccionalo|mejor)( por favor| porfa| plis)?$");
    }

    /** Hasta cuatro palabras con contenido (sin artículos ni muletillas). */
    static String meaningful(String subject) {
        StringBuilder rest = new StringBuilder();
        int words = 0;
        for (String w : subject.split(" ")) {
            if (w.isEmpty() || FILLER.contains(w)) continue;
            if (words++ == 4) break;
            rest.append(rest.length() == 0 ? "" : " ").append(w);
        }
        return rest.toString();
    }

    /** Cuando la deja elegir: un motivo propio, con preferencia por el cielo de la hora. */
    public static SketchScene choose(SketchLibrary library, int hourOfDay, Random random) {
        List<SketchLibrary.Motif> pool = new ArrayList<>();
        for (SketchLibrary.Motif m : library.motifs()) if (!m.self) pool.add(m);
        if (pool.isEmpty()) return null;
        List<SketchLibrary.Motif> scene = new ArrayList<>();
        SketchLibrary.Motif sky = library.byId(hourOfDay >= 20 || hourOfDay < 7 ? "luna" : "sol");
        SketchLibrary.Motif main = pool.get(random.nextInt(pool.size()));
        if (sky != null && main.place == SketchLibrary.Place.GROUND && random.nextBoolean()) scene.add(sky);
        scene.add(main);
        return SketchScene.compose(scene);
    }

    /** Lista hablada del repertorio ("un árbol, una casa…"). */
    public static String repertoire(SketchLibrary library, int max) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (SketchLibrary.Motif m : library.motifs()) {
            if (m.self) continue;
            if (n++ == max) { b.append("…"); break; }
            b.append(b.length() == 0 ? "" : ", ").append(m.name);
        }
        return b.toString();
    }
}
