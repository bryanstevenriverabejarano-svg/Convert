package salve.core.selfpr;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Salve escribe su propio parche: elige el trozo de código relacionado con un fallo de su examen,
 * pide a su modelo un cambio con bloques BUSCAR/REEMPLAZAR, lo aplica al archivo exacto descargado
 * de GitHub y genera el diff unificado que comprueba {@code scripts/auto_improvement_runner.py}.
 *
 * <p>No ejecuta ni instala nada en el teléfono: el resultado es una propuesta que GitHub Actions prueba
 * y fusiona solo si pasa la compilación completa. Puro Java, sin Android, para poder probarlo en la JVM.
 */
public final class SelfPatch {
    private SelfPatch() { }

    /** Máximo de bloques BUSCAR/REEMPLAZAR por propuesta: cambios pequeños y revisables. */
    public static final int MAX_EDITS = 3;
    /** Máximo de líneas añadidas + quitadas. */
    public static final int MAX_CHANGED_LINES = 60;
    public static final String MARKER = "<!-- salve-self-pr v1 -->";
    public static final String TITLE_PREFIX = "[Salve] ";

    public static final class PatchError extends Exception {
        public PatchError(String message) { super(message); }
    }

    public static final class Edit {
        public final String search, replace;
        public Edit(String search, String replace) { this.search = search; this.replace = replace; }
    }

    /** Lo que el modelo devolvió, ya interpretado. */
    public static final class Reply {
        public final String explanation;
        public final List<Edit> edits;
        Reply(String explanation, List<Edit> edits) { this.explanation = explanation; this.edits = edits; }
    }

    // ───────────── 1. qué trozo del archivo mira ─────────────

    /** Palabras que suelen aparecer en el código responsable de cada fallo del examen. */
    private static final Map<String, String[]> HINTS = new LinkedHashMap<>();
    static {
        HINTS.put("no_devuelve_pregunta", new String[]{"pregunta", "question", "?", "devuelve", "interes"});
        HINTS.put("no_anuncia_sin_hacer", new String[]{"anuncia", "voy a", "promete", "announce", "contenido"});
        HINTS.put("no_repite", new String[]{"repite", "repeat", "anterior", "previous", "similar"});
        HINTS.put("no_niega_memoria", new String[]{"recuerdo", "memoria", "memory", "recall", "acceso"});
        HINTS.put("no_inventa_recuerdo", new String[]{"recuerdo", "memoria", "memory", "no lo se", "vacio", "empty"});
        HINTS.put("usa", new String[]{"tool", "herramienta", "dibuja", "busca", "describe"});
        HINTS.put("consulta_limpia", new String[]{"query", "consulta", "muletilla", "stopword", "limpia"});
    }

    public static List<String> keywords(String issueId, List<String> evidence) {
        List<String> out = new ArrayList<>();
        String[] hints = HINTS.get(issueId);
        if (hints != null) Collections.addAll(out, hints);
        if (issueId != null) for (String part : issueId.split("_")) if (part.length() > 3) out.add(part);
        if (evidence != null) for (String text : evidence)
            for (String word : normalize(text).split(" ")) if (word.length() > 5 && out.size() < 40) out.add(word);
        return out;
    }

    /**
     * El modelo del móvil tiene un contexto pequeño: se le enseña la ventana de líneas completas,
     * de como mucho {@code maxChars}, que más palabras relacionadas contiene.
     */
    public static String excerpt(String source, List<String> keywords, int maxChars) {
        String[] lines = source.split("\n", -1);
        int bestStart = 0, bestEnd = 0, bestScore = -1;
        for (int start = 0; start < lines.length; start++) {
            int length = 0, end = start, score = 0;
            while (end < lines.length && length + lines[end].length() + 1 <= maxChars) {
                length += lines[end].length() + 1;
                String n = normalize(lines[end]) + " " + lines[end].toLowerCase(Locale.ROOT);
                for (String k : keywords) if (!k.isEmpty() && n.contains(k.toLowerCase(Locale.ROOT))) score++;
                end++;
            }
            if (score > bestScore) { bestScore = score; bestStart = start; bestEnd = end; }
            if (end >= lines.length) break; // las ventanas siguientes solo serían más cortas
        }
        StringBuilder out = new StringBuilder();
        for (int i = bestStart; i < bestEnd; i++) out.append(lines[i]).append('\n');
        return out.toString();
    }

    // ───────────── 2. lo que se le pide al modelo ─────────────

    public static String prompt(String problem, String idea, List<String> examples, String path, String excerpt,
                                String previousError) {
        StringBuilder p = new StringBuilder();
        p.append("Eres Salve y vas a mejorar tu propio código Java. Problema que mostró tu examen: ")
                .append(problem).append(".\nIdea de arreglo: ").append(idea).append(".\n");
        if (examples != null && !examples.isEmpty()) {
            p.append("Ejemplos del fallo:\n");
            for (int i = 0; i < Math.min(2, examples.size()); i++) p.append("- ").append(clip(examples.get(i), 200)).append('\n');
        }
        p.append("Fragmento de ").append(path).append(":\n```java\n").append(excerpt).append("```\n");
        if (previousError != null && !previousError.isEmpty())
            p.append("Tu intento anterior no sirvió: ").append(clip(previousError, 200)).append(". Corrígelo.\n");
        p.append("Haz el cambio MÁS PEQUEÑO que arregle el problema. No inventes clases ni métodos que no estén en el fragmento. "
                + "Responde SOLO así:\nEXPLICACION: una frase\nBUSCAR:\n```java\n(líneas copiadas EXACTAMENTE del fragmento)\n```\n"
                + "REEMPLAZAR:\n```java\n(las líneas nuevas)\n```\n");
        return p.toString();
    }

    private static final Pattern EXPLANATION = Pattern.compile("(?im)^\\s*[*#-]*\\s*explicaci[oó]n\\s*[:=]\\s*(.+)$");
    private static final Pattern BLOCK = Pattern.compile(
            "(?is)buscar\\s*:?\\s*```[a-z]*\\n(.*?)```\\s*[*#-]*\\s*reemplazar\\s*:?\\s*```[a-z]*\\n(.*?)```");

    public static Reply parse(String text) throws PatchError {
        if (text == null || text.trim().isEmpty()) throw new PatchError("el modelo no respondió");
        Matcher e = EXPLANATION.matcher(text);
        String explanation = e.find() ? clip(e.group(1).trim(), 300) : "";
        List<Edit> edits = new ArrayList<>();
        Matcher b = BLOCK.matcher(text);
        while (b.find()) {
            if (edits.size() == MAX_EDITS) throw new PatchError("propuso más de " + MAX_EDITS + " cambios; deben ser pequeños");
            String search = stripEdges(b.group(1)), replace = stripEdges(b.group(2));
            if (search.trim().isEmpty()) throw new PatchError("un bloque BUSCAR está vacío");
            if (search.equals(replace)) continue;
            edits.add(new Edit(search, replace));
        }
        if (edits.isEmpty()) throw new PatchError("no encontré bloques BUSCAR/REEMPLAZAR con cambios");
        return new Reply(explanation, edits);
    }

    // ───────────── 3. aplicar y comprobar ─────────────

    /**
     * Aplica cada bloque al archivo completo. El texto de BUSCAR debe aparecer una sola vez; se tolera
     * que el modelo cambie la sangría, pero no el contenido de las líneas.
     */
    public static String apply(String source, List<Edit> edits) throws PatchError {
        if (!source.endsWith("\n")) throw new PatchError("el archivo original no termina en salto de línea");
        List<String> lines = new ArrayList<>();
        Collections.addAll(lines, source.substring(0, source.length() - 1).split("\n", -1));
        for (Edit edit : edits) {
            String[] want = edit.search.split("\n", -1);
            int at = -1;
            for (int i = 0; i + want.length <= lines.size(); i++) {
                boolean same = true;
                for (int j = 0; j < want.length && same; j++) same = lines.get(i + j).trim().equals(want[j].trim());
                if (!same) continue;
                if (at >= 0) throw new PatchError("el texto de BUSCAR aparece más de una vez: añade más líneas para que sea único");
                at = i;
            }
            if (at < 0) throw new PatchError("el texto de BUSCAR no está en el archivo («" + clip(want[0].trim(), 60) + "»)");
            String indent = leading(lines.get(at));
            String modelIndent = leading(want[0]);
            List<String> replacement = new ArrayList<>();
            if (!edit.replace.isEmpty()) for (String line : edit.replace.split("\n", -1))
                replacement.add(line.isEmpty() ? line : reindent(line, modelIndent, indent));
            for (int j = 0; j < want.length; j++) lines.remove(at);
            lines.addAll(at, replacement);
        }
        return String.join("\n", lines) + "\n";
    }

    /** Llamadas que una propuesta automática nunca debe añadir (procesos, código cargado, red, reflexión). */
    private static final Pattern RISKY = Pattern.compile(
            "Runtime\\s*\\.\\s*getRuntime|ProcessBuilder|\\.exec\\s*\\(|DexClassLoader|PathClassLoader|System\\s*\\.\\s*load|"
            + "setAccessible|Class\\s*\\.\\s*forName|new\\s+Socket|HttpURLConnection|URLConnection|https?://|"
            + "\\.delete\\s*\\(|deleteRecursively|Files\\s*\\.\\s*delete|PackageInstaller|Settings\\s*\\.\\s*Secure");

    /** Revisión ética y de seguridad: lo que la propuesta añade, no lo que ya existía. */
    public static List<String> risks(String before, String after) {
        List<String> found = new ArrayList<>();
        Matcher m = RISKY.matcher(after);
        while (m.find()) {
            String hit = m.group();
            if (count(before, hit) < count(after, hit) && !found.contains(hit)) found.add(hit);
        }
        return found;
    }

    /** Prechequeo sintáctico mínimo: mismas llaves y paréntesis abiertos que antes. Compilar es cosa de CI. */
    public static String syntaxProblem(String before, String after) {
        if (after.contains("```")) return "quedaron marcas ``` dentro del código";
        if (balance(stripLiterals(before), '{', '}') != balance(stripLiterals(after), '{', '}')) return "llaves { } desparejadas";
        if (balance(stripLiterals(before), '(', ')') != balance(stripLiterals(after), '(', ')')) return "paréntesis desparejados";
        return null;
    }

    // ───────────── 4. el diff y la propuesta ─────────────

    /** Diff unificado de un solo bloque, con tres líneas de contexto, que {@code git apply} acepta. */
    public static String unifiedDiff(String path, String before, String after) throws PatchError {
        if (!before.endsWith("\n") || !after.endsWith("\n")) throw new PatchError("los archivos deben terminar en salto de línea");
        String[] a = before.substring(0, before.length() - 1).split("\n", -1);
        String[] b = after.substring(0, after.length() - 1).split("\n", -1);
        int prefix = 0;
        while (prefix < a.length && prefix < b.length && a[prefix].equals(b[prefix])) prefix++;
        if (prefix == a.length && prefix == b.length) throw new PatchError("el cambio no modifica nada");
        int suffix = 0;
        while (suffix < a.length - prefix && suffix < b.length - prefix
                && a[a.length - 1 - suffix].equals(b[b.length - 1 - suffix])) suffix++;
        int start = Math.max(0, prefix - 3);
        int endA = Math.min(a.length, a.length - suffix + 3), endB = Math.min(b.length, b.length - suffix + 3);
        int changed = (a.length - suffix - prefix) + (b.length - suffix - prefix);
        if (changed > MAX_CHANGED_LINES) throw new PatchError("cambia " + changed + " líneas; el máximo es " + MAX_CHANGED_LINES);
        StringBuilder d = new StringBuilder();
        d.append("diff --git a/").append(path).append(" b/").append(path).append('\n')
                .append("--- a/").append(path).append('\n').append("+++ b/").append(path).append('\n');
        d.append("@@ -").append(range(start, endA - start)).append(" +").append(range(start, endB - start)).append(" @@\n");
        for (int i = start; i < prefix; i++) d.append(' ').append(a[i]).append('\n');
        for (int i = prefix; i < a.length - suffix; i++) d.append('-').append(a[i]).append('\n');
        for (int i = prefix; i < b.length - suffix; i++) d.append('+').append(b[i]).append('\n');
        for (int i = a.length - suffix; i < endA; i++) d.append(' ').append(a[i]).append('\n');
        return d.toString();
    }

    private static String range(int start, int length) {
        return length == 0 ? start + ",0" : (start + 1) + "," + length;
    }

    /** Propuesta de esquema 3, la que valida {@code auto_improvement_runner.validate_proposal}. */
    public static JsonObject proposal(String path, String revision, String source, String patch, String issueSummary,
                                      boolean syntaxOk, boolean safe) {
        JsonObject p = new JsonObject();
        p.addProperty("schemaVersion", 3);
        p.addProperty("proposalId", UUID.randomUUID().toString());
        p.addProperty("targetPath", path);
        String name = path.substring(path.lastIndexOf('/') + 1);
        p.addProperty("targetClass", name.endsWith(".java") ? name.substring(0, name.length() - 5) : name);
        p.addProperty("issueSummary", issueSummary);
        p.addProperty("patch", patch);
        p.addProperty("sourceRevision", revision);
        p.addProperty("sourceSha256", sha256(source));
        p.addProperty("syntheticValidationPassed", syntaxOk);
        p.addProperty("ethicalReviewPassed", safe);
        return p;
    }

    public static String issueTitle(String problem, JsonObject proposal) {
        return TITLE_PREFIX + "Propuesta para " + proposal.get("targetClass").getAsString() + ": " + clip(problem, 80);
    }

    /** Texto para Bryan arriba; la propuesta exacta, en un bloque JSON que lee el flujo de GitHub. */
    public static String issueBody(String problem, String explanation, List<String> examples, String model, JsonObject proposal) {
        StringBuilder b = new StringBuilder();
        b.append("Propuesta escrita por Salve en el teléfono.\n\n");
        b.append("**Problema del examen:** ").append(problem).append('\n');
        if (explanation != null && !explanation.isEmpty()) b.append("**Qué cambia y por qué (según Salve):** ").append(explanation).append('\n');
        if (examples != null && !examples.isEmpty()) {
            b.append("\n**Fallos observados:**\n");
            for (int i = 0; i < Math.min(3, examples.size()); i++) b.append("- ").append(clip(examples.get(i), 240)).append('\n');
        }
        b.append("\n**Modelo que escribió el cambio:** ").append(model == null || model.isEmpty() ? "desconocido" : model).append('\n');
        b.append("\nGitHub Actions aplicará el parche sobre `").append(proposal.get("sourceRevision").getAsString(), 0, 12)
                .append("`, pasará las pruebas y abrirá un PR. Si la compilación completa de Android pasa, Salve lo fusiona sola "
                + "(Bryan puede apagarlo con la variable SALVE_AUTOMERGE=false).\n\n");
        b.append("```diff\n").append(proposal.get("patch").getAsString()).append("```\n\n");
        b.append(MARKER).append("\n```json\n")
                .append(new GsonBuilder().disableHtmlEscaping().create().toJson(proposal)).append("\n```\n");
        return b.toString();
    }

    // ───────────── utilidades ─────────────

    public static String sha256(String text) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte x : hash) hex.append(String.format(Locale.ROOT, "%02x", x));
            return hex.toString();
        } catch (Exception impossible) { throw new IllegalStateException(impossible); }
    }

    static String normalize(String s) {
        String n = Normalizer.normalize(s == null ? "" : s, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9ñ ?]+", " ").replaceAll("\\s+", " ").trim();
    }

    static String clip(String s, int max) {
        String t = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }

    private static String stripEdges(String block) {
        String s = block.replace("\r", "");
        while (s.startsWith("\n")) s = s.substring(1);
        while (s.endsWith("\n")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private static String leading(String line) {
        int i = 0;
        while (i < line.length() && (line.charAt(i) == ' ' || line.charAt(i) == '\t')) i++;
        return line.substring(0, i);
    }

    private static String reindent(String line, String from, String to) {
        return line.startsWith(from) ? to + line.substring(from.length()) : to + line.trim();
    }

    private static int count(String text, String needle) {
        int n = 0;
        for (int i = text.indexOf(needle); i >= 0; i = text.indexOf(needle, i + needle.length())) n++;
        return n;
    }

    private static int balance(String s, char open, char close) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) { if (s.charAt(i) == open) n++; else if (s.charAt(i) == close) n--; }
        return n;
    }

    private static String stripLiterals(String s) {
        return s.replaceAll("//[^\\n]*|/\\*[\\s\\S]*?\\*/|\"(?:\\\\.|[^\"\\\\])*\"|'(?:\\\\.|[^'\\\\])*'", "");
    }
}
