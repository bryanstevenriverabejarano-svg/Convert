package salve.core.reports;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Herramienta de chat «report.save»: cuando Bryan le pide un informe en PDF, Salve guarda lo que ha
 * redactado en su biblioteca de informes (Registro de ejecución → Informes). Solo decide el texto; el
 * archivo lo escribe {@link AutonomousReportWriter#saveText}, la misma biblioteca que usan el sueño y
 * la curiosidad. Sin dependencias de Android para poder probarlo en la JVM.
 */
public final class SelfReport {
    public static final String TOOL = "report.save";
    static final int MAX_TITLE = 120, MAX_BODY = 12000;

    private SelfReport() { }

    /** Bryan pide expresamente un informe o un PDF (no basta con mencionar la palabra «informe» al pasar). */
    private static final Pattern ASKED = Pattern.compile(
            "\\b(pdf|(redact|escrib|hac|haz|crea|gener|guard|prepar|elabor)\\w*\\s+(\\w+\\s+){0,3}(informe|reporte))\\b");

    public static boolean requested(String input) { return ASKED.matcher(normalize(input)).find(); }

    public static String description() {
        return "{titulo:string,contenido:string}: guardas TU informe en PDF en tu biblioteca de informes; "
                + "solo con lo que has comprobado en este turno, sin inventar";
    }

    /** El texto del PDF: deja claro que son propuestas sin aplicar y de dónde salen. */
    public static String compose(String title, String content) {
        String t = title == null ? "" : title.trim();
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) throw new IllegalArgumentException("El informe está vacío");
        if (t.isEmpty()) t = "Informe de Salve";
        if (t.length() > MAX_TITLE) t = t.substring(0, MAX_TITLE);
        if (c.length() > MAX_BODY) c = c.substring(0, MAX_BODY) + "…";
        return "INFORME DE SALVE\n" + t
                + "\n\nEstado: propuestas sin implementar ni verificar. Basado solo en lo que Salve consultó en esta conversación."
                + "\n\n" + c;
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
    }
}
