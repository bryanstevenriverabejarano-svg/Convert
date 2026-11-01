package salve.core.mind;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Su propio código, consultable desde dentro: un índice que se genera al compilar (assets/self/codigo.json)
 * con cada archivo, qué hace según su comentario y sus funciones. Así Salve puede mirar cómo está hecha
 * («¿cómo dibujas?», «qué archivo guarda tus recuerdos») y contestar con lo que de verdad hay, no inventado.
 * También sabe qué imágenes tiene de sí misma: sus cuerpos del Vestuario.
 */
public final class SelfCode {
    public static final String TOOL = "self.code";
    public static final String ASSET = "self/codigo.json";

    public static final class FileInfo {
        public final String path, doc;
        public final int lines;
        public final List<String[]> methods; // {nombre, comentario o ""}
        FileInfo(String path, int lines, String doc, List<String[]> methods) {
            this.path = path; this.lines = lines; this.doc = doc; this.methods = methods;
        }
    }

    private final List<FileInfo> files;
    private final Map<String, List<String>> avatar;

    private SelfCode(List<FileInfo> files, Map<String, List<String>> avatar) { this.files = files; this.avatar = avatar; }

    /** Lee el índice; con un JSON vacío o roto queda un índice vacío (nunca falla). */
    public static SelfCode parse(String json) {
        List<FileInfo> files = new ArrayList<>();
        Map<String, List<String>> avatar = new java.util.TreeMap<>();
        try {
            JsonObject root = JsonParser.parseString(json == null ? "{}" : json).getAsJsonObject();
            if (root.has("files")) for (JsonElement e : root.getAsJsonArray("files")) {
                JsonObject f = e.getAsJsonObject();
                List<String[]> methods = new ArrayList<>();
                if (f.has("m")) for (JsonElement me : f.getAsJsonArray("m")) {
                    JsonObject mo = me.getAsJsonObject();
                    methods.add(new String[]{mo.get("n").getAsString(), mo.has("d") ? mo.get("d").getAsString() : ""});
                }
                files.add(new FileInfo(f.get("p").getAsString(), f.has("l") ? f.get("l").getAsInt() : 0,
                        f.has("d") ? f.get("d").getAsString() : "", methods));
            }
            if (root.has("avatar")) for (Map.Entry<String, JsonElement> e : root.getAsJsonObject("avatar").entrySet()) {
                List<String> names = new ArrayList<>();
                for (JsonElement n : e.getValue().getAsJsonArray()) names.add(n.getAsString());
                avatar.put(e.getKey(), names);
            }
        } catch (RuntimeException broken) { /* índice vacío */ }
        return new SelfCode(Collections.unmodifiableList(files), avatar);
    }

    public int fileCount() { return files.size(); }

    List<FileInfo> files() { return files; }

    public int totalLines() { int n = 0; for (FileInfo f : files) n += f.lines; return n; }

    /** Las carpetas de sus cuerpos (base.png, poses, animaciones…). */
    public Map<String, List<String>> bodies() { return avatar; }

    /** Cuando Bryan le pide que se mire por dentro (su código, cómo está hecha, sus imágenes). */
    private static final Pattern TOPIC = Pattern.compile(
            "\\b(tu codigo|tu propio codigo|tus archivos|tu programacion|tus clases|como funcionas|como estas hecha|"
            + "como estas programada|como te hicieron|de que estas hecha|tu estructura|tus imagenes|imagenes de ti|"
            + "tu cuerpo|tus cuerpos|tu repositorio|tu repo|mirate por dentro|consulta tu codigo|codigo fuente)\\b");

    public static boolean offered(String input) { return TOPIC.matcher(normalize(input)).find(); }

    /** Pide expresamente revisar su código (no solo «tu cuerpo» o «cómo estás hecha» en una charla). */
    private static final Pattern CODE = Pattern.compile(
            "\\b(codigo|codigo fuente|repositorio|repo|tus archivos|tus clases|tu programacion)\\b");

    public static boolean explicitCodeRequest(String input) {
        return offered(input) && CODE.matcher(normalize(input)).find();
    }

    public static String description() {
        return "{pregunta:string}: consultas tu propio código fuente y tus imágenes (cuerpos del Vestuario) para saber cómo estás hecha";
    }

    private static final Set<String> STOP = new HashSet<>(java.util.Arrays.asList(
            "el", "la", "los", "las", "de", "del", "un", "una", "y", "o", "en", "que", "como", "tu", "tus", "te", "me",
            "mi", "se", "es", "lo", "al", "por", "para", "con", "codigo", "propio", "archivo", "archivos", "donde",
            "esta", "estas", "hecha", "funciona", "funcionas", "cual", "cuales", "salve", "haces", "hace", "sobre", "ti",
            "has", "sin", "son", "hay", "mas", "muy", "pero", "algo", "solo", "eso", "esto", "este", "ese", "sus", "nos",
            "les", "yes", "the", "and", "cada", "otro", "otra", "tambien", "luego", "aqui", "alli", "asi"));

    /**
     * Palabras de la petición que no dicen nada del código («quiero que examines…», «redacta un informe»):
     * antes «real» encontraba «RealisticSpriteRenderer» y «examina» un ciclo cualquiera. Raíces de 5 letras.
     */
    private static final Set<String> REQUEST_STEMS = new HashSet<>(java.util.Arrays.asList(
            "quier", "exami", "prime", "ident", "tiene", "tengo", "acced", "acces", "despu", "revis", "revisa", "busca",
            "posib", "mejor", "dupli", "funci", "exist", "indic", "metod", "inspe", "probl", "compr", "cambi", "propo",
            "propon", "final", "redac", "infor", "pdf", "guard", "bibli", "puede", "dimel", "clara", "inven", "resul",
            "real", "fuent", "siste", "algun", "algún", "mira", "mirar", "dime", "cuent", "expli", "anali", "estud",
            "parte", "cosas", "todo", "toda", "todos", "bien", "favor", "ahora", "hazlo", "haz", "haga", "crea", "escri"));

    /** Lo que Bryan llama de una forma y el código de otra (inglés, nombres de clase). */
    private static final java.util.Map<String, String[]> CONCEPTS = new java.util.HashMap<>();
    static {
        CONCEPTS.put("memor", new String[]{"memor", "recue", "recuerd", "remember"});
        CONCEPTS.put("recue", new String[]{"memor", "recue", "recuerd"});
        CONCEPTS.put("llm", new String[]{"llm", "gemma", "model", "infer"});
        CONCEPTS.put("model", new String[]{"model", "llm", "gemma"});
        CONCEPTS.put("auton", new String[]{"auton", "goal", "agent"});
        CONCEPTS.put("herra", new String[]{"tool", "herra"});
        // «Tu repositorio» es su propio código: las herramientas que lo leen y lo revisan.
        CONCEPTS.put("repos", new String[]{"selfcode", "selfsource", "codereview"});
        CONCEPTS.put("dibuj", new String[]{"dibuj", "sketch", "draw"});
        CONCEPTS.put("sueno", new String[]{"sleep", "dream", "sueno"});
        CONCEPTS.put("voz", new String[]{"voice", "voz", "speech"});
        CONCEPTS.put("cuerp", new String[]{"avatar", "body", "cuerp"});
    }

    /** Lo que de verdad pregunta: raíces con contenido; {@code exact} recibe las que dijo tal cual. */
    private static List<String> terms(String question, List<String> exact) {
        List<String> words = new ArrayList<>();
        for (String w : normalize(question).split("[^a-z0-9]+")) {
            if (w.length() < 3 || STOP.contains(w)) continue;
            String st = stem(w);
            if (REQUEST_STEMS.contains(st) || REQUEST_STEMS.contains(w)) continue;
            if (exact != null && !exact.contains(st)) exact.add(st);
            String[] alias = CONCEPTS.get(st);
            if (alias == null) { if (!words.contains(st)) words.add(st); }
            else for (String a : alias) if (!words.contains(a)) words.add(a);
        }
        return words;
    }

    /** Los archivos que mejor responden a la pregunta, por coincidencia de palabras en ruta, comentario y funciones. */
    public List<FileInfo> search(String question, int limit) {
        List<String> exact = new ArrayList<>();
        List<String> words = terms(question, exact);
        List<Object[]> scored = new ArrayList<>();
        if (!words.isEmpty()) for (FileInfo f : files) {
            String path = normalize(f.path), doc = normalize(f.doc);
            String name = path.substring(path.lastIndexOf('/') + 1);
            StringBuilder ms = new StringBuilder();
            for (String[] m : f.methods) ms.append(' ').append(normalize(splitCamel(m[0]))).append(' ').append(normalize(m[1]));
            String methods = ms.toString(), pathWords = normalize(splitCamel(f.path));
            double score = 0;
            for (String w : words) {
                if (path.contains(w) || pathWords.contains(w)) score += 3;
                if (doc.contains(w)) score += 2;
                if (methods.contains(w)) score += 1;
            }
            // La palabra que dijo Bryan en el nombre de la clase pesa más que un sinónimo en la carpeta.
            for (String w : exact) if (name.contains(w)) score += 4;
            if (score > 0) scored.add(new Object[]{f, score});
        }
        scored.sort((a, b) -> Double.compare((Double) b[1], (Double) a[1]));
        List<FileInfo> out = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, scored.size()); i++) out.add((FileInfo) scored.get(i)[0]);
        return out;
    }

    /**
     * Una pregunta con varios temas («tu memoria, tu LLM y tu autonomía») busca cada tema por separado,
     * para que el archivo más largo de uno no tape a los demás: primero uno por tema y luego, si cabe, el
     * segundo de cada uno. Con un solo tema es {@link #search}.
     */
    public List<FileInfo> searchTopics(String question, int perTopic, int limit) {
        String[] parts = normalize(question).split("[,;.:?¿!¡\\n]|\\s(?:y|e|ademas|despues|tambien)\\s");
        List<String> topics = new ArrayList<>();
        for (String part : parts) if (!terms(part, null).isEmpty()) topics.add(part);
        if (topics.size() <= 1) return search(topics.isEmpty() ? question : topics.get(0), limit);
        List<List<FileInfo>> hits = new ArrayList<>();
        for (String topic : topics) hits.add(search(topic, perTopic));
        java.util.LinkedHashSet<FileInfo> out = new java.util.LinkedHashSet<>();
        for (int round = 0; round < perTopic; round++)
            for (List<FileInfo> h : hits) if (round < h.size() && out.size() < limit) out.add(h.get(round));
        return new ArrayList<>(out);
    }

    /** La respuesta para el modelo: un resumen de sí misma y los archivos que tocan la pregunta. */
    public String answer(String question) { return answer(question, null); }

    /**
     * Igual, añadiendo lo que ya aprendió de cada archivo en sus revisiones nocturnas (su memoria de su
     * código). {@code notes}: ruta → su nota, o null si aún no lo revisó.
     */
    public String answer(String question, java.util.function.Function<String, String> notes) {
        if (files.isEmpty()) return "No tengo el índice de mi código en esta versión.";
        StringBuilder sb = new StringBuilder("TU_CODIGO: ").append(fileCount()).append(" archivos, ")
                .append(totalLines()).append(" líneas (Java y Kotlin, app Android).");
        if (!avatar.isEmpty()) sb.append(" TUS_CUERPOS (imágenes de ti misma en assets/avatar): ")
                .append(String.join(", ", avatar.keySet())).append(".");
        List<FileInfo> hits = searchTopics(question, 2, 6); // su contexto es pequeño
        if (hits.isEmpty()) {
            sb.append(" Ningún archivo coincide con esa pregunta; tus carpetas principales son: ").append(String.join(", ", topFolders(8))).append(".");
        } else {
            sb.append(" ARCHIVOS_RELEVANTES:");
            appendFiles(sb, hits, hits.size() > 4 ? 2 : 3);
            if (notes != null) {
                StringBuilder learned = new StringBuilder();
                for (FileInfo f : hits) {
                    String note = notes.apply(f.path);
                    if (note != null && !note.trim().isEmpty())
                        learned.append("\n- ").append(f.path).append(": ").append(cut(note.trim(), hits.size() > 3 ? 220 : 400));
                }
                if (learned.length() > 0) sb.append("\nLO_QUE_YA_REVISASTE (tus notas de noches anteriores):").append(learned);
            }
        }
        sb.append("\nEsto es tu índice (resumen). Para ver el código real línea a línea usa self.read con la ruta "
                + "o Clase#metodo; lo que no hayas leído, no lo afirmes.");
        sb.append("\nResponde en primera persona con lo que dice tu código; si no aparece, dilo, no lo inventes.");
        return sb.toString();
    }

    /** Lo que de verdad ha mirado cuando solo consulta el índice: así no presume de haber leído funciones por dentro. */
    static final String LIMIT = "Límite real: mi índice guarda rutas, descripciones y nombres de funciones, "
            + "no el código línea a línea; en esta respuesta no he leído las funciones por dentro, así que cualquier "
            + "mejora es una hipótesis por verificar. Puedes pedirme que lea un archivo o un método concreto.";

    /**
     * Lo mismo, contado directamente a Bryan (cuando el modelo no llegó a consultar su código):
     * qué archivos y métodos ha mirado y qué no puede afirmar.
     */
    public String report(String question) {
        if (files.isEmpty()) return "No he podido mirar mi código: esta versión no lleva el índice de mis archivos.";
        StringBuilder sb = new StringBuilder("He mirado el índice de mi propio código (")
                .append(fileCount()).append(" archivos, ").append(totalLines()).append(" líneas).");
        List<FileInfo> hits = searchTopics(question, 2, 6);
        if (hits.isEmpty()) {
            sb.append("\n\nNingún archivo coincide con lo que me pides. Mis carpetas principales son: ")
                    .append(String.join(", ", topFolders(8))).append('.');
        } else {
            sb.append("\n\nArchivos y métodos que he inspeccionado:");
            appendFiles(sb, hits, 3);
        }
        return sb.append("\n\n").append(LIMIT).toString();
    }

    private static void appendFiles(StringBuilder sb, List<FileInfo> hits, int methodsPerFile) {
        for (FileInfo f : hits) {
            sb.append("\n- ").append(f.path).append(" (").append(f.lines).append(" líneas)");
            if (!f.doc.isEmpty()) sb.append(": ").append(cut(f.doc, 160));
            int shown = 0;
            for (String[] m : f.methods) {
                if (shown >= methodsPerFile) break;
                if (m[1].isEmpty()) continue;
                sb.append("\n    · ").append(m[0]).append(": ").append(cut(m[1], 90));
                shown++;
            }
        }
    }

    List<String> topFolders(int n) {
        Map<String, Integer> count = new java.util.HashMap<>();
        for (FileInfo f : files) {
            String[] parts = f.path.split("/");
            String key = parts.length > 2 ? parts[0] + "/" + parts[1] + (parts.length > 3 ? "/" + parts[2] : "") : f.path;
            count.merge(key, 1, Integer::sum);
        }
        List<Map.Entry<String, Integer>> list = new ArrayList<>(count.entrySet());
        list.sort((a, b) -> b.getValue() - a.getValue());
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(n, list.size()); i++) out.add(list.get(i).getKey());
        return out;
    }

    private static String stem(String w) {
        // Raíz corta: «recuerdos» encuentra «recuerdo», «dibujar» encuentra «dibujo».
        return w.length() >= 6 ? w.substring(0, 5) : w;
    }

    private static String splitCamel(String s) { return s.replaceAll("([a-z])([A-Z])", "$1 $2"); }

    private static String cut(String s, int n) { return s.length() > n ? s.substring(0, n - 1) + "…" : s; }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
