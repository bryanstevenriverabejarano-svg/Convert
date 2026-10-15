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

    private SketchRequest(Kind kind, SketchScene scene, String unknown) { this(kind, scene, unknown, false); }
    private SketchRequest(Kind kind, SketchScene scene, String unknown, boolean references) {
        this.kind = kind; this.scene = scene; this.unknown = unknown == null ? "" : unknown; this.references = references;
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
            "\\b(busca|buscar|buscas|busques|mira|consulta)\\b.*\\b(referencia|referencias|imagen|imagenes|fotos|dibujos|internet)\\b"
            + "|\\breferencias?\\b");
    private static final Pattern REFERENCE_SUBJECT = Pattern.compile(
            "\\b(?:referencias?|imagenes|imagen|fotos|dibujos)(?: en internet)? (?:de|del|sobre) (.+)$");
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
            "puedes", "podrias", "por", "ti", "encima", "debajo", "junto", "sobre", "bajo", "cerca", "lado"));

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
        Matcher verb = VERB.matcher(text);
        if (!verb.find()) return new SketchRequest(Kind.NONE, null, null);
        Matcher about = REFERENCE_SUBJECT.matcher(text);
        String subject = about.find() ? about.group(1).trim() : text.substring(verb.end()).trim();
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
