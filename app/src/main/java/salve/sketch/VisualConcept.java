package salve.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lo que Salve entiende de un objeto antes de dibujarlo: qué es, sus partes con su forma y posición,
 * la pose típica y su nombre en inglés. Lo escribe su modelo una vez («un gorila es un primate grande:
 * cabeza pequeña con cresta, hombros muy anchos, brazos largos apoyados en los nudillos…») y sirve para
 * tres cosas: buscar mejores referencias (siluetas y dibujos de línea), componerlo parte por parte
 * cuando no hay referencia, y explicar después por qué lo dibujó así.
 *
 * El texto del modelo sólo se acepta con este formato y longitudes acotadas; nunca se ejecuta.
 */
public final class VisualConcept {
    public static final class Part {
        public final String name, shape;
        /** Dónde va dentro del objeto ({x1,y1,x2,y2} en fracciones), si su modelo lo dijo; si no, null. */
        public final float[] box;
        Part(String name, String shape) { this(name, shape, null); }
        Part(String name, String shape, float[] box) { this.name = name; this.shape = shape; this.box = box; }
    }

    /** La parte de su concepto que corresponde a una pedida («puerta» ↔ «puerta principal»), o null. */
    public Part part(String wanted) {
        String k = PartPlan.key(wanted);
        if (k.isEmpty()) return null;
        for (Part p : parts) if (PartPlan.key(p.name).equals(k)) return p;
        for (Part p : parts) if (SketchLibrary.normalize(p.name).contains(k)) return p;
        return null;
    }

    public final String subject, english, kind, pose;
    /** El nombre bien escrito que ella entendió («vestido de verano» aunque Bryan escribiera «vesrido»). */
    public String name = "";
    public final List<Part> parts;

    public VisualConcept(String subject, String english, String kind, String pose, List<Part> parts) {
        this.subject = clip(subject, 60);
        this.english = english == null ? "" : english;
        this.kind = clip(kind, 80);
        this.pose = clip(pose, 120);
        this.parts = Collections.unmodifiableList(new ArrayList<>(parts == null ? Collections.emptyList() : parts));
    }

    /** Hay algo útil: al menos dos partes descritas. */
    public boolean isUsable() { return parts.size() >= 2; }

    /** Instrucción para el modelo: un ejemplo completo (un modelo pequeño copia mejor que interpreta). */
    public static String prompt(String subject, String wish) {
        String what = clip(subject == null ? "" : subject.replaceAll("[\\r\\n\"«»]+", " "), 60);
        String w = wish == null || wish.trim().isEmpty() ? "" : " (" + clip(wish.replaceAll("[\\r\\n\"«»]+", " "), 80) + ")";
        return "Eres Salve. Antes de dibujar algo, piensas cómo es de verdad. Describe cómo se ve «" + what + "» para "
                + "dibujarlo con líneas. Responde SOLO con este formato, sin nada más:\n"
                + "NOMBRE: el objeto bien escrito en español\nINGLES: nombre en inglés\nTIPO: qué es\nPARTES:\n"
                + "- parte: forma y posición [x1 y1 x2 y2]\nPOSE: postura para dibujarlo\n"
                + "La caja [x1 y1 x2 y2] dice dónde va la parte dentro del objeto, en % (0 a 100, y crece hacia abajo; "
                + "negativa si sobresale por encima). Incluye las partes que lo hacen reconocible.\n"
                + "Ejemplo «gaton»:\nNOMBRE: gato\nINGLES: cat\nTIPO: animal, felino pequeño\nPARTES:\n"
                + "- cabeza: redonda, delante y arriba, con dos orejas triangulares [62 0 100 42]\n"
                + "- cuerpo: óvalo alargado y horizontal [10 25 80 75]\n- patas: cuatro, finas y rectas, debajo del cuerpo [15 65 75 100]\n"
                + "- cola: larga, curvada hacia arriba, detrás [0 5 18 55]\nPOSE: de perfil, de pie\n"
                + "Ejemplo «iglesia»:\nNOMBRE: iglesia\nINGLES: church\nTIPO: edificio religioso\nPARTES:\n"
                + "- fachada: rectángulo alto con tejado a dos aguas [0 20 100 100]\n- puerta: arco alto, abajo en el centro [40 60 60 100]\n"
                + "- campanario: torre estrecha encima del tejado [38 -25 62 22]\n- cruz: dos palos en lo más alto [45 -40 55 -25]\n"
                + "POSE: de frente\n"
                + "Ahora «" + what + "»" + w + ":\n";
    }

    private static final Pattern FIELD = Pattern.compile("^\\s*[*#-]*\\s*(nombre|ingles|inglés|english|tipo|pose|partes)\\s*[:=]\\s*(.*)$",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern PART = Pattern.compile("^\\s*(?:[-*•]|\\d+[.)])\\s*([\\p{L} ]{2,30}?)\\s*[:=–-]\\s*(.{3,})$");
    private static final Pattern ENGLISH = Pattern.compile("^[a-z][a-z '\\-]{1,38}$");

    public static VisualConcept parse(String text, String subject) {
        String english = "", kind = "", pose = "", name = "";
        List<Part> parts = new ArrayList<>();
        if (text == null) return new VisualConcept(subject, "", "", "", parts);
        boolean inParts = false;
        for (String raw : text.split("\\r?\\n")) {
            String line = raw.replace("**", "").trim();
            if (line.isEmpty() || line.startsWith("```")) continue;
            if (line.toLowerCase(Locale.ROOT).startsWith("ahora") || line.toLowerCase(Locale.ROOT).startsWith("ejemplo")) {
                if (!parts.isEmpty()) break; // empieza a inventar otro ejemplo
                continue;
            }
            Matcher f = FIELD.matcher(line);
            if (f.matches()) {
                String key = f.group(1).toLowerCase(Locale.ROOT), value = f.group(2).trim();
                inParts = key.equals("partes");
                if (key.startsWith("ingl") || key.equals("english")) {
                    String e = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z '\\-]", " ").replaceAll("\\s+", " ").trim();
                    if (ENGLISH.matcher(e).matches()) english = e;
                } else if (key.equals("tipo")) kind = value;
                else if (key.equals("pose")) pose = value;
                else if (key.equals("nombre")) {
                    String n = value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L} ]", " ").replaceAll("\\s+", " ").trim();
                    if (n.length() >= 2 && n.length() <= 40 && n.split(" ").length <= 5) name = n;
                }
                continue;
            }
            Matcher p = PART.matcher(line);
            if (p.matches() && (inParts || parts.size() > 0 || kind.length() > 0) && parts.size() < 8) {
                String partName = p.group(1).trim().toLowerCase(Locale.ROOT);
                float[] box = PartPlan.parseBox(p.group(2));
                String shape = clip(PartPlan.withoutBox(p.group(2).trim()), 110);
                if (partName.length() >= 2) parts.add(new Part(partName, shape, box));
            }
        }
        VisualConcept c = new VisualConcept(subject, english, kind, pose, parts);
        c.name = name;
        return c;
    }

    /** Partes para la composición geométrica («dibuja cada una con su forma y posición»). */
    public String forShapes() {
        if (parts.isEmpty()) return "";
        StringBuilder b = new StringBuilder("Así es «").append(subject).append("»")
                .append(kind.isEmpty() ? "" : " (" + kind + ")").append(". Dibuja CADA parte con su forma y posición:\n");
        for (Part p : parts) b.append("- ").append(p.name).append(": ").append(p.shape).append('\n');
        if (!pose.isEmpty()) b.append("Pose: ").append(pose).append('\n');
        return b.toString();
    }

    /** Resumen breve para explicar sus dibujos («para mí un gorila es…»). */
    public String summary() {
        StringBuilder b = new StringBuilder();
        if (!kind.isEmpty()) b.append(kind);
        if (!parts.isEmpty()) {
            b.append(b.length() == 0 ? "" : ": ");
            for (int i = 0; i < parts.size(); i++)
                b.append(i == 0 ? "" : "; ").append(parts.get(i).name).append(' ').append(parts.get(i).shape);
        }
        if (!pose.isEmpty()) b.append(b.length() == 0 ? "" : "; ").append("pose: ").append(pose);
        return clip(b.toString(), 420);
    }

    public static String clip(String s, int max) {
        String t = s == null ? "" : s.replaceAll("[\\p{Cntrl}]+", " ").replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1).trim() + "…";
    }
}
