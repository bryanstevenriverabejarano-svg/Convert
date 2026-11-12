package salve.core.reports;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * La biblioteca de informes de Salve: cada PDF lleva al lado una ficha (.json) con título, tipo y un
 * resumen, para verlos como una lista ordenada en vez de nombres de archivo. Los PDF antiguos sin
 * ficha siguen apareciendo, con la fecha sacada de su nombre. Solo java.io: se prueba en la JVM.
 */
public final class ReportLibrary {
    public enum Category {
        CODE("Mi código"), RESEARCH("Investigación"), SLEEP("Sueño"), CHAT("Pedido en el chat"),
        JOURNAL("Registro"), OTHER("Otros");
        public final String label;
        Category(String label) { this.label = label; }
    }

    public static final class Entry {
        public final File pdf;
        public final String title, preview;
        public final Category category;
        public final long createdAt;
        /** La mejora concreta que el informe propone para su código («MEJORA PARA MI CÓDIGO»), o "". */
        public final String improvement;
        Entry(File pdf, String title, Category category, String preview, long createdAt) {
            this(pdf, title, category, preview, createdAt, "");
        }
        Entry(File pdf, String title, Category category, String preview, long createdAt, String improvement) {
            this.pdf = pdf; this.title = title; this.category = category; this.preview = preview; this.createdAt = createdAt;
            this.improvement = improvement == null ? "" : improvement;
        }
    }

    private ReportLibrary() { }

    private static final Pattern STAMP = Pattern.compile("informe-(\\d{10,})\\.pdf");

    /** Título, tipo y resumen a partir del texto con el que se generó el PDF. */
    public static Entry describe(File pdf, String body, long createdAt) {
        String text = body == null ? "" : body.replace("\r\n", "\n").trim();
        String[] lines = text.split("\n");
        String first = lines.length > 0 ? lines[0].trim() : "";
        String second = lines.length > 1 ? lines[1].trim() : "";
        String title;
        Category category;
        if (first.startsWith("INFORME AUTÓNOMO DE SALVE")) {
            String cat = second.replaceFirst("^Categoría:\\s*", "");
            String n = normalize(cat);
            category = n.contains("investig") ? Category.RESEARCH : n.contains("sueno") ? Category.SLEEP : Category.OTHER;
            title = cat.isEmpty() ? "Informe autónomo" : cat;
        } else if (first.startsWith("INFORME DE SALVE:")) {
            category = Category.JOURNAL;
            title = "Tarea: " + first.substring("INFORME DE SALVE:".length()).trim();
        } else if (first.startsWith("INFORME DE SALVE")) {
            title = second.isEmpty() ? "Informe de Salve" : second;
            String n = normalize(title);
            category = n.contains("codigo") || n.contains("parche") ? Category.CODE : Category.CHAT;
        } else {
            title = first.isEmpty() ? "Informe" : cut(first, 80);
            category = Category.OTHER;
        }
        return new Entry(pdf, cut(title, 90), category, preview(lines), createdAt,
                salve.core.selfpr.EvolutionSignals.improvementOf(text));
    }

    /** Lo primero con contenido después de la cabecera y del aviso de «Estado». */
    static String preview(String[] lines) {
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < lines.length && sb.length() < 220; i++) {
            String l = lines[i].trim();
            if (l.isEmpty() || l.startsWith("Estado:") || l.startsWith("Fuente:") || l.equals(l.toUpperCase(Locale.ROOT)) && l.length() < 60)
                continue;
            if (sb.length() > 0) sb.append(' ');
            sb.append(l);
        }
        return cut(sb.toString(), 200);
    }

    public static File metaFile(File pdf) {
        return new File(pdf.getParentFile(), pdf.getName().replaceFirst("\\.pdf$", ".json"));
    }

    /** Guarda la ficha al lado del PDF. Si falla, el PDF sigue en la biblioteca con un título genérico. */
    public static void writeMeta(Entry entry) {
        try {
            JsonObject o = new JsonObject();
            o.addProperty("title", entry.title);
            o.addProperty("category", entry.category.name());
            o.addProperty("preview", entry.preview);
            o.addProperty("created", entry.createdAt);
            if (!entry.improvement.isEmpty()) o.addProperty("improvement", entry.improvement);
            Files.write(metaFile(entry.pdf).toPath(), o.toString().getBytes(StandardCharsets.UTF_8));
        } catch (Exception ignored) { }
    }

    public static Entry read(File pdf) {
        long created = pdf.lastModified();
        Matcher m = STAMP.matcher(pdf.getName());
        if (m.matches()) try { created = Long.parseLong(m.group(1)); } catch (NumberFormatException ignored) { }
        File meta = metaFile(pdf);
        if (meta.isFile()) {
            try {
                JsonObject o = JsonParser.parseString(new String(Files.readAllBytes(meta.toPath()), StandardCharsets.UTF_8)).getAsJsonObject();
                Category c = Category.OTHER;
                try { c = Category.valueOf(o.get("category").getAsString()); } catch (RuntimeException ignored) { }
                return new Entry(pdf, o.get("title").getAsString(), c,
                        o.has("preview") ? o.get("preview").getAsString() : "",
                        o.has("created") ? o.get("created").getAsLong() : created,
                        o.has("improvement") ? o.get("improvement").getAsString() : "");
            } catch (Exception broken) { /* ficha rota: como un PDF antiguo */ }
        }
        return new Entry(pdf, "Informe", Category.OTHER, "", created);
    }

    /** Todos los informes, el más reciente primero; {@code only} = null para todos. */
    public static List<Entry> list(File directory, Category only) {
        List<Entry> out = new ArrayList<>();
        File[] files = directory == null ? null : directory.listFiles((d, n) -> n.startsWith("informe-") && n.endsWith(".pdf"));
        if (files != null) for (File f : files) {
            Entry e = read(f);
            if (only == null || e.category == only) out.add(e);
        }
        out.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        return out;
    }

    /** Borra el PDF y su ficha. */
    public static boolean delete(Entry entry) {
        boolean ok = !entry.pdf.exists() || entry.pdf.delete();
        File meta = metaFile(entry.pdf);
        if (meta.exists()) meta.delete();
        return ok;
    }

    // ───────── pedirlo hablando: «abre tu biblioteca», «registro de ejecución ábrelo» ─────────

    public static final String TAB_LIBRARY = "biblioteca", TAB_EXECUTION = "ejecucion";
    private static final Pattern OPEN_LIBRARY = Pattern.compile(
            "^(?:salve )?(?:(?:abre|abreme|abrir|ver|muestra|muestrame|ensename|ensenanos|quiero ver|ir a) )?"
            + "(?:la |el |tu |tus |mi |mis |los |las )?(?:biblioteca(?: de informes| de pdf)?|informes(?: pdf)?|pdfs?(?: de (?:mejoras|informes))?)"
            + "(?: (?:abrela|abrelos|abrelo|abre(?:la|los|lo)?|por favor|porfa))*$");
    private static final Pattern OPEN_JOURNAL = Pattern.compile(
            "^(?:salve )?(?:(?:abre|abreme|abrir|ver|muestra|muestrame|ensename) )?(?:el |tu |las )?"
            + "(?:registro de ejecucion|notas de esta tarea|notas de la tarea)"
            + "(?: (?:abrelo|abrela|abre(?:lo|la)?|por favor|porfa))*$");

    /** {@link #TAB_LIBRARY}, {@link #TAB_EXECUTION} o null si no es una orden de abrir. */
    public static String openRequest(String input) {
        String s = normalize(input).replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
        if (OPEN_LIBRARY.matcher(s).matches()) return TAB_LIBRARY;
        if (OPEN_JOURNAL.matcher(s).matches()) return TAB_EXECUTION;
        return null;
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    static String cut(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "…" : s; }
}
