package salve.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Pensar un dibujo por partes: un objeto principal y las partes que lleva, cada una en su sitio y a su
 * tamaño. Una iglesia es una fachada con la puerta abajo en el centro y una cruz encima; una casa lleva
 * ventanas a los lados y una chimenea en el tejado. Cada parte tiene una <b>caja relativa</b> al objeto
 * principal ({x1, y1, x2, y2} en fracciones de su contorno real; puede salirse, p. ej. una cruz encima
 * tiene y negativa) y se encaja dentro de ella conservando su proporción.
 *
 * <p>La caja la decide su modelo (abstraer «dónde va una puerta en una iglesia»); si no responde, estas
 * reglas dan una posición razonable. Todo es geometría pura, sin Android, y se prueba sin el móvil.</p>
 */
public final class PartPlan {
    private PartPlan() { }

    // ───────────── dónde va cada parte ─────────────

    /** Posiciones típicas de partes comunes, relativas al objeto principal. */
    private static final Map<String, float[]> KNOWN = new HashMap<>();
    static {
        float[] door = {.40f, .60f, .60f, 1f};
        for (String k : new String[]{"puerta", "portal", "portico", "entrada", "porton"}) KNOWN.put(k, door);
        float[] window = {.14f, .36f, .32f, .56f};
        for (String k : new String[]{"ventana", "ventanal", "vidriera", "roseton"}) KNOWN.put(k, window);
        KNOWN.put("cruz", new float[]{.44f, -.20f, .56f, 0f});
        float[] tower = {.38f, -.40f, .62f, .05f};
        for (String k : new String[]{"campanario", "torre", "aguja", "cupula"}) KNOWN.put(k, tower);
        KNOWN.put("campana", new float[]{.44f, .08f, .56f, .22f});
        KNOWN.put("chimenea", new float[]{.66f, -.14f, .80f, .14f});
        KNOWN.put("tejado", new float[]{0f, -.32f, 1f, .04f});
        KNOWN.put("techo", new float[]{0f, -.32f, 1f, .04f});
        KNOWN.put("bandera", new float[]{.50f, -.42f, .72f, -.12f});
        KNOWN.put("reloj", new float[]{.42f, .16f, .58f, .32f});
        KNOWN.put("escalera", new float[]{.34f, .92f, .66f, 1.06f});
        KNOWN.put("escalon", new float[]{.34f, .92f, .66f, 1.06f});
        KNOWN.put("ojo", new float[]{.26f, .28f, .42f, .42f});
        KNOWN.put("boca", new float[]{.38f, .60f, .62f, .72f});
        KNOWN.put("nariz", new float[]{.45f, .42f, .55f, .56f});
        KNOWN.put("sombrero", new float[]{.20f, -.30f, .80f, .08f});
        KNOWN.put("corona", new float[]{.25f, -.28f, .75f, .04f});
        KNOWN.put("rueda", new float[]{.10f, .74f, .34f, 1.04f});
        KNOWN.put("vela", new float[]{.38f, -.70f, .62f, .10f});
        KNOWN.put("mastil", new float[]{.47f, -.80f, .53f, .20f});
    }

    /** Partes que van por parejas, una a cada lado («ventanas», «ojos», «ruedas»). */
    private static final java.util.Set<String> PAIRED = new java.util.HashSet<>(
            java.util.Arrays.asList("ventana", "ojo", "rueda", "ventanal", "vidriera"));

    private static final Pattern BOX = Pattern.compile(
            "\\[\\s*(-?\\d{1,3})\\s*[ ,;]\\s*(-?\\d{1,3})\\s*[ ,;]\\s*(-?\\d{1,3})\\s*[ ,;]\\s*(-?\\d{1,3})\\s*\\]");
    private static final Pattern BOX_LINE = Pattern.compile(
            "(?i)caja\\s*[:=]?\\s*\\[?\\s*(-?\\d{1,3})\\s*[ ,;]\\s*(-?\\d{1,3})\\s*[ ,;]\\s*(-?\\d{1,3})\\s*[ ,;]\\s*(-?\\d{1,3})");

    /** La palabra clave de una parte, sin plural ni artículos («las ventanas» → «ventana»). */
    public static String key(String part) {
        String t = SketchLibrary.normalize(part == null ? "" : part).trim();
        t = t.replaceFirst("^(el|la|los|las|un|una|unos|unas|su|sus) ", "");
        String first = t.contains(" ") ? t.substring(0, t.indexOf(' ')) : t;
        if (KNOWN.containsKey(first)) return first;
        if (first.endsWith("es") && KNOWN.containsKey(first.substring(0, first.length() - 2))) return first.substring(0, first.length() - 2);
        if (first.endsWith("s") && KNOWN.containsKey(first.substring(0, first.length() - 1))) return first.substring(0, first.length() - 1);
        return first;
    }

    /** Nombre en inglés de partes comunes, para buscar su icono de líneas. */
    private static final Map<String, String> ENGLISH = new HashMap<>();
    static {
        String[][] pairs = {{"puerta", "door"}, {"portal", "door"}, {"entrada", "door"}, {"ventana", "window"},
                {"cruz", "cross"}, {"campana", "bell"}, {"campanario", "bell"}, {"reloj", "clock"}, {"rueda", "wheel"},
                {"bandera", "flag"}, {"corona", "crown"}, {"sombrero", "hat"}, {"escalera", "stairs"}, {"ojo", "eye"},
                {"estrella", "star"}, {"corazon", "heart"}, {"luna", "moon"}, {"sol", "sun"}, {"nube", "cloud"},
                {"arbol", "tree"}, {"flor", "flower"}, {"hoja", "leaf"}, {"lampara", "lamp"}, {"farol", "lamp"},
                {"vela", "sailboat"}, {"ancla", "anchor"}, {"llave", "key"}, {"candado", "lock"}, {"buzon", "mailbox"}};
        for (String[] p : pairs) ENGLISH.put(p[0], p[1]);
    }

    /** El nombre en inglés de una parte para buscar su icono, o "" si no lo sabe. */
    public static String english(String part) {
        String e = ENGLISH.get(key(part));
        return e == null ? "" : e;
    }

    /** ¿Sabe dónde va esta parte sin preguntar (puerta, cruz, ventana…)? */
    public static boolean knownPlace(String part) { return KNOWN.containsKey(key(part)); }

    /** ¿Va por parejas? «ventanas», «ojos»… (en plural, o siempre si la parte es de las que van a pares). */
    public static boolean paired(String part) {
        String t = SketchLibrary.normalize(part == null ? "" : part).trim();
        String k = key(part);
        return PAIRED.contains(k) && (t.endsWith("s") || t.contains(" dos ") || t.startsWith("dos "));
    }

    /** Una caja escrita por el modelo («[40 60 60 100]» o «CAJA: 40 60 60 100», en %), o null. */
    public static float[] parseBox(String text) {
        if (text == null) return null;
        Matcher m = BOX_LINE.matcher(text);
        if (!m.find()) { m = BOX.matcher(text); if (!m.find()) return null; }
        float[] b = new float[4];
        for (int i = 0; i < 4; i++) b[i] = Integer.parseInt(m.group(i + 1)) / 100f;
        return valid(b) ? ordered(b) : null;
    }

    /** El texto sin la caja (para guardar la forma limpia). */
    public static String withoutBox(String text) {
        return text == null ? "" : BOX.matcher(text).replaceAll("").replaceAll("\\s+", " ").trim();
    }

    /**
     * Dónde va una parte. Primero lo que dijo su modelo (la caja explícita); si no, la posición típica de
     * esa parte; y si no la conoce, lo que digan las palabras («abajo», «arriba a la derecha»…).
     */
    public static float[] boxFor(String part, String positionText, float[] explicit) {
        if (explicit != null && valid(explicit)) return ordered(explicit.clone());
        float[] known = KNOWN.get(key(part));
        if (known != null) return known.clone();
        return fromWords(positionText);
    }

    /** Caja a partir de palabras de posición; centro y tamaño medio si no hay ninguna. */
    public static float[] fromWords(String text) {
        String t = SketchLibrary.normalize(text == null ? "" : text);
        float cx = .5f, cy = .5f, w = .26f, h = .26f;
        if (t.matches(".*\\b(encima|sobre el|sobre la|cima|punta|remate|tejado|techo)\\b.*")) { cy = -.08f; }
        else if (t.matches(".*\\b(arriba|superior|alto|parte de arriba)\\b.*")) cy = .2f;
        else if (t.matches(".*\\b(abajo|inferior|base|suelo|debajo|pie)\\b.*")) cy = .82f;
        if (t.matches(".*\\b(izquierda|izquierdo)\\b.*")) cx = .22f;
        else if (t.matches(".*\\b(derecha|derecho)\\b.*")) cx = .78f;
        if (t.matches(".*\\b(grande|ancha|ancho|enorme)\\b.*")) { w = .44f; h = .44f; }
        else if (t.matches(".*\\b(pequena|pequeno|diminuta|diminuto)\\b.*")) { w = .14f; h = .14f; }
        return new float[]{cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2};
    }

    private static boolean valid(float[] b) {
        if (b == null || b.length != 4) return false;
        for (float v : b) if (!Float.isFinite(v) || v < -1.5f || v > 2.5f) return false;
        return Math.abs(b[2] - b[0]) >= .02f && Math.abs(b[3] - b[1]) >= .02f;
    }

    private static float[] ordered(float[] b) {
        return new float[]{Math.min(b[0], b[2]), Math.min(b[1], b[3]), Math.max(b[0], b[2]), Math.max(b[1], b[3])};
    }

    // ───────────── encajar una parte en el dibujo ─────────────

    /** Contorno real de unos trazos: {minX, minY, maxX, maxY}, o null si no hay puntos. */
    public static float[] bounds(List<float[]> strokes) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        boolean any = false;
        if (strokes != null) for (float[] s : strokes) {
            if (s == null) continue;
            for (int i = 0; i + 1 < s.length; i += 2) {
                any = true;
                minX = Math.min(minX, s[i]); maxX = Math.max(maxX, s[i]);
                minY = Math.min(minY, s[i + 1]); maxY = Math.max(maxY, s[i + 1]);
            }
        }
        return any ? new float[]{minX, minY, maxX, maxY} : null;
    }

    /**
     * Coloca los trazos de una parte dentro de su caja, relativa al contorno real del objeto principal.
     * Conserva la proporción de la parte, la centra en horizontal y, si la caja llega al suelo del objeto
     * (una puerta), la apoya abajo. Devuelve trazos nuevos; no toca los originales.
     */
    public static List<float[]> attach(List<float[]> main, List<float[]> part, float[] box) {
        float[] mb = bounds(main), pb = bounds(part);
        if (mb == null || pb == null || box == null) return Collections.emptyList();
        float mw = Math.max(1, mb[2] - mb[0]), mh = Math.max(1, mb[3] - mb[1]);
        float tx1 = mb[0] + box[0] * mw, ty1 = mb[1] + box[1] * mh, tx2 = mb[0] + box[2] * mw, ty2 = mb[1] + box[3] * mh;
        float tw = Math.max(1, tx2 - tx1), th = Math.max(1, ty2 - ty1);
        float pw = Math.max(1, pb[2] - pb[0]), ph = Math.max(1, pb[3] - pb[1]);
        float scale = Math.min(tw / pw, th / ph);
        float ox = tx1 + (tw - pw * scale) / 2;
        boolean grounded = box[3] >= .97f;
        float oy = grounded ? ty2 - ph * scale : ty1 + (th - ph * scale) / 2;
        List<float[]> out = new ArrayList<>();
        for (float[] s : part) {
            if (s == null || s.length < 4) continue;
            float[] t = new float[s.length];
            for (int i = 0; i + 1 < s.length; i += 2) {
                t[i] = (s[i] - pb[0]) * scale + ox;
                t[i + 1] = (s[i + 1] - pb[1]) * scale + oy;
            }
            out.add(t);
        }
        return out;
    }

    /**
     * Encaja unas líneas dentro de un rectángulo absoluto {x1,y1,x2,y2} (donde estaba la parte que se
     * rehace), conservando su proporción, centradas y apoyadas abajo si {@code grounded}.
     */
    public static List<float[]> fitInto(List<float[]> lines, float[] rect, boolean grounded) {
        float[] pb = bounds(lines);
        if (pb == null || rect == null) return Collections.emptyList();
        float tw = Math.max(1, rect[2] - rect[0]), th = Math.max(1, rect[3] - rect[1]);
        float pw = Math.max(1, pb[2] - pb[0]), ph = Math.max(1, pb[3] - pb[1]);
        float scale = Math.min(tw / pw, th / ph);
        float ox = rect[0] + (tw - pw * scale) / 2, oy = grounded ? rect[3] - ph * scale : rect[1] + (th - ph * scale) / 2;
        List<float[]> out = new ArrayList<>();
        for (float[] s : lines) {
            if (s == null || s.length < 4) continue;
            float[] t = new float[s.length];
            for (int i = 0; i + 1 < s.length; i += 2) { t[i] = (s[i] - pb[0]) * scale + ox; t[i + 1] = (s[i + 1] - pb[1]) * scale + oy; }
            out.add(t);
        }
        return out;
    }

    /**
     * Sustituye los trazos de una parte por otros (ya colocados) y recoloca los índices de las demás
     * partes. Devuelve el dibujo resultante centrado en el lienzo 0..1000.
     */
    public static Combined replacePart(List<float[]> strokes, List<SketchLibrary.Part> parts, int index, List<float[]> replacement) {
        SketchLibrary.Part target = parts.get(index);
        List<float[]> all = new ArrayList<>();
        List<SketchLibrary.Part> moved = new ArrayList<>();
        for (int i = 0; i < parts.size(); i++) {
            SketchLibrary.Part p = parts.get(i);
            int from = all.size();
            if (i == index) for (float[] s : replacement) all.add(s.clone());
            else for (int k = p.from; k < p.to && k < strokes.size(); k++) all.add(strokes.get(k).clone());
            if (all.size() > from) moved.add(new SketchLibrary.Part(p.name, i == index ? target.description : p.description, from, all.size()));
        }
        ShapeProgram.Result normalized = ShapeProgram.normalize(all, all.size(), 0);
        return new Combined(normalized.strokes, normalized.aspect, moved);
    }

    /** La caja reflejada al otro lado (la segunda ventana, el otro ojo). */
    public static float[] mirrored(float[] box) {
        return new float[]{1f - box[2], box[1], 1f - box[0], box[3]};
    }

    /** Un dibujo terminado: trazos en el lienzo 0..1000, su proporción y sus partes con sentido. */
    public static final class Combined {
        public final List<float[]> strokes;
        public final float aspect;
        public final List<SketchLibrary.Part> parts;
        Combined(List<float[]> strokes, float aspect, List<SketchLibrary.Part> parts) {
            this.strokes = strokes; this.aspect = aspect; this.parts = parts;
        }
    }

    /** Una parte ya encajada: su nombre, una frase de qué es, y sus trazos colocados. */
    public static final class Placed {
        public final String name, description;
        public final List<float[]> strokes;
        public Placed(String name, String description, List<float[]> strokes) {
            this.name = name; this.description = description == null ? "" : description; this.strokes = strokes;
        }
    }

    /**
     * Junta el objeto principal (con sus partes, si las tenía) y las partes nuevas en un solo dibujo,
     * centrado en el lienzo 0..1000. Cada parte nueva queda nombrada para trazarla y contarla aparte.
     */
    public static Combined combine(List<float[]> main, List<SketchLibrary.Part> mainParts, String mainName, List<Placed> added) {
        List<float[]> all = new ArrayList<>();
        for (float[] s : main) if (s != null && s.length >= 4) all.add(s.clone());
        List<SketchLibrary.Part> parts = new ArrayList<>();
        if (mainParts != null && !mainParts.isEmpty() && mainParts.get(mainParts.size() - 1).to <= all.size()) parts.addAll(mainParts);
        else parts.add(new SketchLibrary.Part(mainName, "el contorno de " + mainName, 0, all.size()));
        for (Placed p : added) {
            if (p.strokes.isEmpty()) continue;
            int from = all.size();
            for (float[] s : p.strokes) all.add(s.clone());
            parts.add(new SketchLibrary.Part(p.name, p.description, from, all.size()));
        }
        ShapeProgram.Result normalized = ShapeProgram.normalize(all, all.size(), 0);
        return new Combined(normalized.strokes, normalized.aspect, parts);
    }

    // ───────────── formas sencillas que abstrae ella misma ─────────────

    /**
     * Partes tan simples que las abstrae con geometría, sin buscar nada: una cruz son dos palos; una
     * ventana, un marco con su cruceta; una chimenea, un rectángulo. Lienzo 0..1000. null si no la sabe.
     */
    public static List<float[]> primitive(String part) {
        switch (key(part)) {
            case "cruz":
                return list(new float[]{500, 0, 500, 1000}, new float[]{220, 280, 780, 280});
            case "ventana": case "ventanal":
                return list(rect(150, 100, 850, 900), new float[]{500, 100, 500, 900}, new float[]{150, 500, 850, 500});
            case "chimenea":
                return list(rect(300, 0, 700, 1000), new float[]{240, 0, 760, 0});
            case "reloj":
                return list(circle(500, 500, 450, 36), new float[]{500, 500, 500, 230}, new float[]{500, 500, 690, 560});
            case "rueda":
                return list(circle(500, 500, 460, 36), circle(500, 500, 120, 18));
            case "ojo":
                return list(circle(500, 500, 420, 30), circle(500, 520, 160, 16));
            case "boca":
                return list(new float[]{100, 300, 300, 600, 500, 700, 700, 600, 900, 300});
            case "campana":
                return list(new float[]{300, 900, 330, 400, 400, 180, 500, 120, 600, 180, 670, 400, 700, 900, 300, 900});
            case "escalera": case "escalon":
                return list(new float[]{0, 1000, 1000, 1000}, new float[]{100, 660, 900, 660}, new float[]{200, 330, 800, 330});
            case "campanario": case "torre":
                return list(new float[]{250, 1000, 250, 380, 750, 380, 750, 1000}, new float[]{200, 380, 500, 0, 800, 380},
                        rect(400, 520, 600, 720));
            case "tejado": case "techo":
                return list(new float[]{0, 1000, 500, 0, 1000, 1000});
            case "bandera":
                return list(new float[]{100, 1000, 100, 0}, new float[]{100, 0, 900, 200, 100, 420});
            default:
                return null;
        }
    }

    private static List<float[]> list(float[]... strokes) {
        List<float[]> out = new ArrayList<>();
        Collections.addAll(out, strokes);
        return out;
    }

    private static float[] rect(float x1, float y1, float x2, float y2) {
        return new float[]{x1, y1, x2, y1, x2, y2, x1, y2, x1, y1};
    }

    private static float[] circle(float cx, float cy, float r, int n) {
        float[] s = new float[2 * (n + 1)];
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            s[2 * i] = (float) (cx + r * Math.cos(a));
            s[2 * i + 1] = (float) (cy + r * Math.sin(a));
        }
        return s;
    }

    // ───────────── qué pide Bryan ─────────────

    /** «una iglesia con su puerta (y una cruz)» → [iglesia, puerta, cruz]; null si no hay «con …». */
    private static final Pattern WITH = Pattern.compile("^(.+?) con (?:(?:su|sus|un|una|unos|unas|el|la|los|las|dos) )?(.+)$");
    /** Lo que no es una parte sino una forma de dibujarlo («con más detalle», «con otra pose»). */
    private static final Pattern NOT_A_PART = Pattern.compile(
            "^(mas|mucho|otra|otro|pose|detalle|cuidado|calma|colores|color|amor|carino|luz|lineas|trazos|forma|estilo)\\b");

    /**
     * Separa el objeto principal de las partes que debe llevar. Devuelve [principal, parte1, parte2…]
     * (hasta tres partes) o null si la frase no pide partes.
     */
    public static List<String> split(String normalizedSubject) {
        String t = normalizedSubject == null ? "" : normalizedSubject.trim();
        Matcher m = WITH.matcher(t);
        if (!m.matches()) return null;
        String main = clean(m.group(1));
        if (main.isEmpty()) return null;
        List<String> out = new ArrayList<>();
        out.add(main);
        for (String piece : m.group(2).split("\\s*(?:,|\\by\\b|\\bcon\\b)\\s*")) {
            String p = clean(piece);
            if (p.isEmpty() || NOT_A_PART.matcher(p).find() || p.equals(main)) continue;
            out.add(p);
            if (out.size() == 4) break;
        }
        return out.size() >= 2 ? out : null;
    }

    /** Verbos de «añádele una puerta a la iglesia» (sólo de añadir: «dibuja»/«traza» piden un dibujo nuevo). */
    private static final Pattern ADD = Pattern.compile(
            "\\b(anade|anadele|anadeles|anadir|anadirle|agrega|agregale|agregales|agregar|agregarle|agregue|agreguele"
            + "|pon|ponle|ponles|poner|ponerle|incluye|incluyele|incluir|metele|falta|faltaria)\\b");
    /** «… a la iglesia», «… al dibujo»: a qué objeto (sólo con «a/al»; «en el centro» es una posición). */
    private static final Pattern TARGET = Pattern.compile("\\s+(?:a|al)\\s+(?:la |el |los |las |tu |ese |esa |este |esta |mi |su )?(.+)$");
    /** Palabras de posición: dicen dónde va la parte, no qué parte es. */
    private static final Pattern WHERE = Pattern.compile(
            "\\b(en el centro|en medio|en el medio|centro|centrada|centrado|arriba|abajo|encima|debajo|izquierda|derecha"
            + "|a la izquierda|a la derecha|en lo alto|en la cima|en la punta|delante|grande|pequena|pequeno)\\b");
    private static final java.util.Set<String> POSITION_WORDS = new java.util.HashSet<>(java.util.Arrays.asList(
            "centro", "medio", "arriba", "abajo", "encima", "debajo", "izquierda", "derecha", "alto", "cima", "punta",
            "delante", "lado", "frente", "en", "centrada", "centrado"));

    /** Una petición de añadir partes a un dibujo: qué partes, a qué objeto y, si lo dice, dónde. */
    public static final class Add {
        public final List<String> parts;
        public final String target; // «iglesia», o "" si no lo dice («al dibujo»)
        public final String where;  // «arriba a la derecha», o "" si no lo dice
        Add(List<String> parts, String target, String where) { this.parts = parts; this.target = target; this.where = where; }
    }

    /** «Mejora el dibujo y añádele una puerta», «ponle una cruz a la iglesia» → las partes; null si no pide añadir. */
    public static Add add(String normalizedText) {
        String t = normalizedText == null ? "" : normalizedText.trim();
        Matcher verb = ADD.matcher(t);
        if (!verb.find()) return null;
        String rest = t.substring(verb.end()).trim().replaceFirst("^(le |les |tambien |ademas )+", "");
        StringBuilder where = new StringBuilder();
        Matcher w = WHERE.matcher(rest);
        while (w.find()) where.append(where.length() == 0 ? "" : " ").append(w.group(1));
        String target = "";
        Matcher to = TARGET.matcher(rest);
        if (to.find()) {
            String candidate = clean(to.group(1));
            boolean position = true;
            for (String word : candidate.split(" ")) position &= POSITION_WORDS.contains(word);
            boolean drawing = candidate.matches("^(dibujo|tu dibujo|ese dibujo)?$");
            if (!position && !drawing) target = candidate;
            if (!position) rest = rest.substring(0, to.start()).trim();
        }
        List<String> parts = new ArrayList<>();
        for (String piece : WHERE.matcher(rest).replaceAll(" ").split("\\s*(?:,|\\by\\b|\\bcon\\b)\\s*")) {
            String p = clean(piece);
            StringBuilder kept = new StringBuilder();
            for (String word : p.split(" ")) if (!word.isEmpty() && !POSITION_WORDS.contains(word))
                kept.append(kept.length() == 0 ? "" : " ").append(word);
            p = kept.toString();
            if (p.isEmpty() || NOT_A_PART.matcher(p).find()) continue;
            parts.add(p);
            if (parts.size() == 3) break;
        }
        return parts.isEmpty() ? null : new Add(parts, target, where.toString());
    }

    private static final java.util.Set<String> FILLER = new java.util.HashSet<>(java.util.Arrays.asList(
            "un", "una", "unos", "unas", "el", "la", "los", "las", "lo", "su", "sus", "de", "del", "al", "a", "e",
            "le", "les", "tambien", "ademas", "por", "favor", "porfa", "dibujo", "mejor", "mejora", "mejoralo",
            "mejorala", "bien", "bonita", "bonito", "grande", "pequena", "pequeno", "nueva", "nuevo", "que", "se", "vea",
            "dos", "tres", "mas"));

    private static String clean(String s) {
        StringBuilder b = new StringBuilder();
        int words = 0;
        for (String w : SketchLibrary.normalize(s == null ? "" : s).split(" ")) {
            if (w.isEmpty() || FILLER.contains(w)) continue;
            if (words++ == 3) break;
            b.append(b.length() == 0 ? "" : " ").append(w);
        }
        return b.toString();
    }

    // ───────────── lo que ve al mirarse ─────────────

    /** Lo que se pide a su visión: ¿cuáles de estas partes se ven en el dibujo? */
    public static String lookForPartsPrompt(String subject, List<String> parts) {
        return "Mira este dibujo de líneas de «" + VisualConcept.clip(subject, 40) + "». ¿Cuáles de estas partes se ven "
                + "claramente? " + String.join(", ", parts) + ".\nResponde SOLO con dos líneas:\nVEO: las que se ven\nFALTA: las que no se ven";
    }

    /** Las partes que su visión dice que faltan (sólo de la lista pedida). */
    public static List<String> missing(String answer, List<String> asked) {
        List<String> out = new ArrayList<>();
        if (answer == null || asked == null) return out;
        String falta = "";
        for (String line : answer.split("\\r?\\n")) {
            String n = SketchLibrary.normalize(line);
            if (n.startsWith("falta")) { falta = n.substring(5); break; }
        }
        if (falta.matches("^[\\s:=]*(nada|ninguna|ninguno)?\\s*$")) return out;
        for (String part : asked) {
            String k = key(part);
            if (falta.contains(k) && !out.contains(part)) out.add(part);
        }
        return out;
    }

    /** Prompt para que su modelo abstraiga dónde va una parte en un objeto («¿dónde va la puerta de una iglesia?»). */
    public static String whereGoesPrompt(String main, String part) {
        return "Piensas cómo es un objeto para dibujarlo. En un dibujo de «" + VisualConcept.clip(main, 40) + "», ¿dónde va «"
                + VisualConcept.clip(part, 30) + "» y de qué tamaño? Da su caja en porcentaje del objeto: x1 y1 x2 y2, de 0 a 100 "
                + "(y crece hacia abajo; puede ser negativa si va encima, como una cruz en lo alto).\n"
                + "Ejemplo: en «iglesia», «puerta» → CAJA: 40 60 60 100\nResponde SOLO: CAJA: x1 y1 x2 y2";
    }

    static String lower(String s) { return s == null ? "" : s.toLowerCase(Locale.ROOT); }
}
