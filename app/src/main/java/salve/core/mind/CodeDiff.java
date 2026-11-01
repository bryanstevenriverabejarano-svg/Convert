package salve.core.mind;

import java.util.ArrayList;
import java.util.List;

/**
 * Diferencia línea a línea entre la versión de un archivo que Salve ya revisó y la del parche nuevo
 * (algoritmo de Myers). Devuelve trozos con un poco de contexto, en el formato de siempre: «-» lo que
 * se quitó, «+» lo que entró, « » lo que sigue igual. Lógica pura, sin Android.
 */
public final class CodeDiff {
    private CodeDiff() { }

    /** Más ediciones que esto en un archivo = reescritura: no merece la pena alinear línea a línea. */
    static final int MAX_EDITS = 2000;

    public static final class Hunk {
        public final int oldStart, newStart; // 1…
        public final List<String> lines;     // «-texto», «+texto», « texto»
        Hunk(int oldStart, int newStart, List<String> lines) {
            this.oldStart = oldStart; this.newStart = newStart; this.lines = lines;
        }
        public int added() { int n = 0; for (String l : lines) if (l.startsWith("+")) n++; return n; }
        public int removed() { int n = 0; for (String l : lines) if (l.startsWith("-")) n++; return n; }
    }

    public static String[] lines(String text) {
        if (text == null || text.isEmpty()) return new String[0];
        String t = text.replace("\r\n", "\n");
        if (t.endsWith("\n")) t = t.substring(0, t.length() - 1);
        return t.split("\n", -1);
    }

    /** Trozos cambiados con {@code context} líneas iguales alrededor; vacío si no cambió nada. */
    public static List<Hunk> diff(String before, String after, int context) {
        String[] a = lines(before), b = lines(after);
        // Lo común al principio y al final no entra en el algoritmo (los parches suelen ser locales).
        int prefix = 0;
        while (prefix < a.length && prefix < b.length && a[prefix].equals(b[prefix])) prefix++;
        int suffix = 0;
        while (suffix < a.length - prefix && suffix < b.length - prefix
                && a[a.length - 1 - suffix].equals(b[b.length - 1 - suffix])) suffix++;
        List<char[]> ops = new ArrayList<>(); // {op} por línea: '=', '-', '+'
        List<String> text = new ArrayList<>();
        for (int i = 0; i < prefix; i++) { ops.add(new char[]{'='}); text.add(a[i]); }
        myers(a, prefix, a.length - suffix, b, prefix, b.length - suffix, ops, text);
        for (int i = a.length - suffix; i < a.length; i++) { ops.add(new char[]{'='}); text.add(a[i]); }
        return hunks(ops, text, context);
    }

    private static void myers(String[] a, int a0, int a1, String[] b, int b0, int b1,
                              List<char[]> ops, List<String> text) {
        int n = a1 - a0, m = b1 - b0, max = n + m;
        if (n == 0) { for (int j = b0; j < b1; j++) { ops.add(new char[]{'+'}); text.add(b[j]); } return; }
        if (m == 0) { for (int i = a0; i < a1; i++) { ops.add(new char[]{'-'}); text.add(a[i]); } return; }
        int offset = max;
        int[] v = new int[2 * max + 2];
        List<int[]> trace = new ArrayList<>(); // por paso, solo la franja [-d, d]: memoria O(D²)
        int found = -1;
        int limit = Math.min(max, MAX_EDITS);
        for (int d = 0; d <= limit && found < 0; d++) {
            trace.add(java.util.Arrays.copyOfRange(v, offset - d, offset + d + 1));
            for (int k = -d; k <= d; k += 2) {
                int x = (k == -d || (k != d && v[offset + k - 1] < v[offset + k + 1]))
                        ? v[offset + k + 1] : v[offset + k - 1] + 1;
                int y = x - k;
                while (x < n && y < m && a[a0 + x].equals(b[b0 + y])) { x++; y++; }
                v[offset + k] = x;
                if (x >= n && y >= m) { found = d; break; }
            }
        }
        if (found < 0) { // reescritura casi total: se cuenta como sustituida entera
            for (int i = a0; i < a1; i++) { ops.add(new char[]{'-'}); text.add(a[i]); }
            for (int j = b0; j < b1; j++) { ops.add(new char[]{'+'}); text.add(b[j]); }
            return;
        }
        // Reconstruye el camino hacia atrás.
        List<char[]> rOps = new ArrayList<>();
        List<String> rText = new ArrayList<>();
        int x = n, y = m;
        for (int d = found; d > 0; d--) {
            int[] pv = trace.get(d); // v al empezar el paso d, indexado por k + d
            int k = x - y;
            int prevK = (k == -d || (k != d && pv[k - 1 + d] < pv[k + 1 + d])) ? k + 1 : k - 1;
            int prevX = pv[prevK + d], prevY = prevX - prevK;
            while (x > prevX && y > prevY) { x--; y--; rOps.add(new char[]{'='}); rText.add(a[a0 + x]); }
            if (x == prevX) { y--; rOps.add(new char[]{'+'}); rText.add(b[b0 + y]); }
            else { x--; rOps.add(new char[]{'-'}); rText.add(a[a0 + x]); }
        }
        while (x > 0 && y > 0) { x--; y--; rOps.add(new char[]{'='}); rText.add(a[a0 + x]); }
        for (int i = rOps.size() - 1; i >= 0; i--) { ops.add(rOps.get(i)); text.add(rText.get(i)); }
    }

    private static List<Hunk> hunks(List<char[]> ops, List<String> text, int context) {
        List<Hunk> out = new ArrayList<>();
        int size = ops.size();
        boolean[] keep = new boolean[size];
        for (int i = 0; i < size; i++) if (ops.get(i)[0] != '=')
            for (int j = Math.max(0, i - context); j <= Math.min(size - 1, i + context); j++) keep[j] = true;
        int oldLine = 1, newLine = 1;
        List<String> current = null;
        int hOld = 0, hNew = 0;
        for (int i = 0; i < size; i++) {
            char op = ops.get(i)[0];
            if (keep[i]) {
                if (current == null) { current = new ArrayList<>(); hOld = oldLine; hNew = newLine; }
                current.add((op == '=' ? " " : String.valueOf(op)) + text.get(i));
            } else if (current != null) { out.add(new Hunk(hOld, hNew, current)); current = null; }
            if (op != '+') oldLine++;
            if (op != '-') newLine++;
        }
        if (current != null) out.add(new Hunk(hOld, hNew, current));
        return out;
    }

    /** Los trozos como texto para el modelo, cortado a {@code maxChars}. */
    public static String render(List<Hunk> hunks, int maxChars) {
        StringBuilder sb = new StringBuilder();
        for (Hunk h : hunks) {
            String head = "@@ antes línea " + h.oldStart + ", ahora línea " + h.newStart + "\n";
            if (sb.length() + head.length() > maxChars) { sb.append("…(más cambios)\n"); break; }
            sb.append(head);
            for (String l : h.lines) {
                if (sb.length() + l.length() + 1 > maxChars) return sb.append("…(más cambios)\n").toString();
                sb.append(l).append('\n');
            }
        }
        return sb.toString();
    }
}
