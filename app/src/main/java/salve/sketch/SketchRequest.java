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
    public enum Kind { NONE, DRAW, SELF, FREE, LIST, UNKNOWN }

    public final Kind kind;
    public final SketchScene scene;
    /** Lo que pidió y no está en el repertorio (para decirlo con honestidad). */
    public final String unknown;

    private SketchRequest(Kind kind, SketchScene scene, String unknown) {
        this.kind = kind; this.scene = scene; this.unknown = unknown == null ? "" : unknown;
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
            + "|(quiero|quisiera) (ver )?un dibujo( de| del)?)\\b");
    private static final Pattern NEGATED = Pattern.compile("\\b(no|deja de|para de|nunca) (me )?(dibujes|pintes|dibujar|pintar)\\b");
    private static final Pattern FREE = Pattern.compile("^(algo|lo que (tu )?quieras|lo que te apetezca|lo que (te )?guste|cualquier cosa|un dibujo|algo bonito|algo para mi)?$");
    private static final Set<String> FILLER = new HashSet<>(Arrays.asList(
            "un", "una", "unos", "unas", "el", "la", "los", "las", "lo", "de", "del", "al", "a", "y", "e", "con", "en",
            "mi", "mis", "tu", "tus", "su", "sus", "me", "te", "nos", "por", "favor", "porfa", "porfavor", "salve",
            "ahora", "otra", "otro", "vez", "pequeno", "pequena", "grande", "bonito", "bonita", "simple", "sencillo",
            "que", "se", "vea", "ver", "para", "mi", "dibujo", "dibujar", "muy", "poco", "rapido", "aqui", "hola",
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
        Matcher verb = VERB.matcher(text);
        if (!verb.find()) return new SketchRequest(Kind.NONE, null, null);
        String subject = text.substring(verb.end()).trim();
        List<SketchLibrary.Motif> motifs = new ArrayList<>();
        for (SketchLibrary.Mention m : library.mentions(subject, false)) if (!m.motif.self) motifs.add(m.motif);
        if (!motifs.isEmpty()) return new SketchRequest(Kind.DRAW, SketchScene.compose(motifs), null);
        if (FREE.matcher(subject).matches()) return new SketchRequest(Kind.FREE, null, null);
        StringBuilder rest = new StringBuilder();
        int words = 0;
        for (String w : subject.split(" ")) {
            if (w.isEmpty() || FILLER.contains(w)) continue;
            if (words++ == 4) break;
            rest.append(rest.length() == 0 ? "" : " ").append(w);
        }
        return rest.length() == 0 ? new SketchRequest(Kind.FREE, null, null)
                : new SketchRequest(Kind.UNKNOWN, null, rest.toString());
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
