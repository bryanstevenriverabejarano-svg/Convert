package salve.sketch;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Marcas de dibujo dentro del texto de Salve: [[salve_draw:arbol]] o [[salve_draw:casa+luna]].
 * La marca indica DÓNDE va el dibujo en el chat. Antes de la voz y del historial se retira; el chat la
 * convierte en un lienzo animado. Una marca nunca ejecuta nada: sólo nombra motivos del repertorio.
 */
public final class SketchProtocol {
    public static final int MAX_PER_MESSAGE = 6;
    /** Cualquier intento de marca (incluso mal formado) se retira de voz e historial. */
    private static final Pattern ANY = Pattern.compile("(?i)\\[{1,2}\\s*salve[_\\s-]*draw\\b[^\\]\\n]{0,160}\\]{0,2}");
    private static final Pattern VALID = Pattern.compile("(?iu)\\[\\[\\s*salve_draw\\s*:\\s*([a-z0-9ñáéíóú +]{2,120})\\]\\]");

    private SketchProtocol() { }

    public static String tag(SketchScene scene) { return scene == null ? "" : "[[salve_draw:" + scene.key + "]]"; }

    public static final class Extraction {
        /** Texto sin marcas (voz, historial). */
        public final String clean;
        /** Texto para el chat: marcas válidas normalizadas a su clave canónica, el resto retirado. */
        public final String display;
        public final List<SketchScene> scenes;
        Extraction(String clean, String display, List<SketchScene> scenes) {
            this.clean = clean; this.display = display; this.scenes = Collections.unmodifiableList(scenes);
        }
        public boolean hasSketches() { return !scenes.isEmpty(); }
        /** Nota breve para el historial: así Salve recuerda qué dibujó. */
        public String historyNote() {
            if (scenes.isEmpty()) return "";
            StringBuilder b = new StringBuilder(" (Dibujé con mis trazos de luz: ");
            for (int i = 0; i < scenes.size(); i++) b.append(i == 0 ? "" : "; ").append(scenes.get(i).title);
            return b.append(".)").toString();
        }
    }

    public static boolean contains(String text) { return text != null && ANY.matcher(text).find(); }

    public static Extraction extract(String text, SketchLibrary library) {
        if (text == null) text = "";
        List<SketchScene> scenes = new ArrayList<>();
        StringBuilder clean = new StringBuilder(), display = new StringBuilder();
        Matcher any = ANY.matcher(text);
        int cursor = 0;
        boolean dropNewline = false;
        while (any.find()) {
            appendClean(clean, text.substring(cursor, any.start()), dropNewline);
            display.append(text, cursor, any.start());
            // Una marca sola en su línea se lleva también su salto: la voz no recibe líneas vacías de más.
            dropNewline = (any.start() == 0 || text.charAt(any.start() - 1) == '\n')
                    && any.end() < text.length() && text.charAt(any.end()) == '\n';
            Matcher valid = VALID.matcher(any.group());
            if (valid.matches() && scenes.size() < MAX_PER_MESSAGE) {
                SketchScene scene = SketchScene.fromKey(library, valid.group(1).trim());
                if (scene != null) {
                    scenes.add(scene);
                    display.append(tag(scene));
                }
            }
            cursor = any.end();
        }
        appendClean(clean, text.substring(cursor), dropNewline);
        display.append(text.substring(cursor));
        return new Extraction(tidy(clean.toString()), display.toString().trim(), scenes);
    }

    private static void appendClean(StringBuilder clean, String chunk, boolean dropNewline) {
        clean.append(dropNewline && chunk.startsWith("\n") ? chunk.substring(1) : chunk);
    }

    /** Quita espacios y líneas vacías que dejan las marcas retiradas. */
    static String tidy(String text) {
        return text.replaceAll("[ \\t]+\\n", "\n").replaceAll("\\n{3,}", "\n\n").replaceAll(" {2,}", " ").trim();
    }

    /** Partes de un texto de chat: texto o clave de dibujo, en orden. */
    public static List<String[]> segments(String display) {
        List<String[]> out = new ArrayList<>();
        if (display == null) return out;
        Matcher m = VALID.matcher(display);
        int cursor = 0;
        while (m.find()) {
            if (m.start() > cursor) out.add(new String[]{"text", display.substring(cursor, m.start())});
            out.add(new String[]{"sketch", m.group(1).trim()});
            cursor = m.end();
        }
        if (cursor < display.length()) out.add(new String[]{"text", display.substring(cursor)});
        return out;
    }
}
