package salve.core.mind;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Su código real, línea a línea (herramienta «self.read»). Cada APK lleva sus propios archivos fuente
 * (assets/self/src/&lt;ruta&gt;.txt), así que lo que lee es exactamente la versión instalada, sin red.
 * Su contexto es pequeño: lee por trozos numerados y sabe desde dónde seguir. {@link SelfCode} le dice
 * qué archivo mirar; esto le enseña lo que hay dentro. Lógica pura: el cargador lo pone Android.
 */
public final class SelfSource {
    public static final String TOOL = "self.read";
    public static final String ASSET_DIR = "self/src/";
    static final int MAX_CHARS = 3500;

    private SelfSource() { }

    public static String description() {
        return "{archivo:string,desde:entero}: lees tu código real línea a línea por trozos; archivo=ruta, "
                + "Clase o Clase#metodo; desde=línea (1 al empezar)";
    }

    /** Ruta del asset con el texto del archivo. */
    public static String assetPath(String path) { return ASSET_DIR + path + ".txt"; }

    /**
     * @param requested ruta, nombre de clase («SalveLLM») o «Clase#metodo»
     * @param from      primera línea (1 = principio); con «#metodo» se ignora y empieza en el método
     * @param loader    ruta del índice → texto del archivo, o null si no está
     */
    public static String read(SelfCode index, String requested, int from, Function<String, String> loader) {
        String wanted = requested == null ? "" : requested.trim();
        String method = null;
        int hash = wanted.indexOf('#');
        if (hash >= 0) { method = wanted.substring(hash + 1).replaceAll("[()\\s]", ""); wanted = wanted.substring(0, hash).trim(); }
        if (wanted.isEmpty()) throw new IllegalArgumentException("Falta el archivo");
        List<String> matches = resolve(index, wanted);
        if (matches.isEmpty())
            return "NO_ENCONTRADO: ningún archivo de tu código se llama «" + wanted
                    + "». Busca primero con self.code y no inventes su contenido.";
        if (matches.size() > 1) {
            StringBuilder sb = new StringBuilder("VARIOS_ARCHIVOS: «" + wanted + "» puede ser:");
            for (int i = 0; i < Math.min(6, matches.size()); i++) sb.append("\n- ").append(matches.get(i));
            return sb.append("\nVuelve a leer con la ruta exacta.").toString();
        }
        String path = matches.get(0);
        String text = loader.apply(path);
        if (text == null)
            return "SIN_FUENTE: esta versión no lleva el texto de " + path + "; solo su resumen en el índice. No lo inventes.";
        String[] lines = text.replace("\r\n", "\n").split("\n", -1);
        int total = lines.length;
        int start = Math.max(1, from);
        if (method != null && !method.isEmpty()) {
            int found = findMethod(lines, method);
            if (found < 0) return "NO_ENCONTRADO: " + path + " no tiene ningún método «" + method + "». No lo inventes.";
            start = Math.max(1, found - 2); // con su comentario justo encima
        }
        if (start > total) return "FIN: " + path + " tiene " + total + " líneas; ya no hay más.";
        StringBuilder sb = new StringBuilder("TU_CODIGO_REAL ").append(path).append('\n');
        int end = start - 1;
        for (int i = start; i <= total; i++) {
            String line = String.format(Locale.ROOT, "%5d| %s\n", i, lines[i - 1]);
            if (sb.length() + line.length() > MAX_CHARS && end >= start) break;
            sb.append(line);
            end = i;
        }
        sb.append("Líneas ").append(start).append('–').append(end).append(" de ").append(total).append('.');
        if (end < total) sb.append(" Para seguir: desde=").append(end + 1).append('.');
        return sb.append(" Cita solo lo que aparece aquí.").toString();
    }

    /** Ruta exacta, ruta sin extensión o nombre de clase; varias si el nombre se repite. */
    static List<String> resolve(SelfCode index, String wanted) {
        String w = wanted.replace('\\', '/').replaceFirst("^/+", "").replaceFirst("(?i)^app/src/main/java/", "");
        String lower = w.toLowerCase(Locale.ROOT);
        List<String> exact = new ArrayList<>(), byName = new ArrayList<>();
        for (SelfCode.FileInfo f : index.files()) {
            String p = f.path.toLowerCase(Locale.ROOT);
            String noExt = p.replaceFirst("\\.(java|kt)$", "");
            String name = noExt.substring(noExt.lastIndexOf('/') + 1);
            if (p.equals(lower) || noExt.equals(lower) || p.endsWith("/" + lower) || noExt.endsWith("/" + lower)) exact.add(f.path);
            else if (name.equals(lower.replaceFirst("\\.(java|kt)$", ""))) byName.add(f.path);
        }
        return exact.isEmpty() ? byName : exact;
    }

    /** Línea (1…) donde se declara el método, saltando llamadas y comentarios. */
    static int findMethod(String[] lines, String method) {
        Pattern declaration = Pattern.compile(
                "^\\s*(?:@\\w+\\s+)*(?:(?:public|protected|private|static|final|synchronized|abstract|override|suspend|internal|open)\\s+)*"
                + "(?:fun\\s+(?:<[^>]+>\\s+)?|(?:<[^>]+>\\s+)?[\\w.<>\\[\\]?, ]+\\s+)" + Pattern.quote(method) + "\\s*\\(");
        for (int i = 0; i < lines.length; i++) {
            String t = lines[i].trim();
            if (t.startsWith("//") || t.startsWith("*") || t.startsWith("return ") || t.startsWith("new ")) continue;
            if (declaration.matcher(lines[i]).find()) return i + 1;
        }
        return -1;
    }
}
