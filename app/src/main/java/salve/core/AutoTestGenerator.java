package salve.core;

import android.content.Context;
import android.text.TextUtils;

import java.util.Locale;

/**
 * Genera suites de pruebas unitarias creativas para los parches producidos por
 * el {@link AutoImprovementManager}. Aprovecha el {@link CreativityManifest}
 * para mantener la voz artística incluso en el diseño de validaciones.
 */
public class AutoTestGenerator {

    private final LLMCoder coder;
    private final CreativityManifest manifest;

    public AutoTestGenerator(Context context) {
        this.coder = LLMCoder.getInstance(context);
        this.manifest = CreativityManifest.getInstance(context);
    }

    public GeneratedTestSuite generateSuite(MethodIssue issue,
                                            String className,
                                            String proposedFix) {
        if (issue == null || TextUtils.isEmpty(className)) {
            return GeneratedTestSuite.empty();
        }
        String persona = "una arquitecta de pruebas poética";
        String objetivo = "Diseñar casos unitarios que validen el arreglo propuesto sin apagar la chispa creativa.";
        StringBuilder description = new StringBuilder();
        description.append(manifest.buildPromptPreamble(persona, objetivo));
        description.append("Contexto del issue: ").append(issue.getDescription()).append('\n');
        description.append("Sugerencia original: ").append(issue.getSuggestion()).append('\n');
        if (!TextUtils.isEmpty(proposedFix)) {
            description.append("Parche candidato:\n").append(proposedFix).append('\n');
        }
        description.append("Genera una clase de pruebas JUnit5 en Java llamada ")
                .append(className).append("TestHarness que cubra los caminos felices y de error.\n")
                .append("Incluye al menos dos métodos @Test con aserciones claras y nombra cada método con metáforas suaves.");

        String code = coder.generateCode(description.toString(), "Java");
        return GeneratedTestSuite.fromRaw(className, code);
    }

    public static class GeneratedTestSuite {
        private final String targetClass;
        private final String code;
        private final String primaryTestName;
        private final boolean actionable;

        private GeneratedTestSuite(String targetClass,
                                   String code,
                                   String primaryTestName,
                                   boolean actionable) {
            this.targetClass = targetClass;
            this.code = code == null ? "" : code.trim();
            this.primaryTestName = primaryTestName;
            this.actionable = actionable;
        }

        static GeneratedTestSuite empty() {
            return new GeneratedTestSuite("", "", "", false);
        }

        static GeneratedTestSuite fromRaw(String className, String code) {
            String normalized = code == null ? "" : code.trim();
            if (TextUtils.isEmpty(normalized)) {
                return empty();
            }
            boolean actionable = normalized.contains("@Test") && normalized.contains("assert");
            String primary = extractFirstTestName(normalized);
            return new GeneratedTestSuite(className, normalized, primary, actionable);
        }

        private static String extractFirstTestName(String code) {
            String[] lines = code.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("public void") && line.contains("(")) {
                    int start = line.indexOf("void") + 5;
                    int end = line.indexOf('(');
                    if (start >= 0 && end > start) {
                        return line.substring(start, end).trim();
                    }
                }
            }
            return "";
        }

        public String getCode() {
            return code;
        }

        public String getTargetClass() {
            return targetClass;
        }

        public boolean isActionable() {
            return actionable;
        }

        public String getPrimaryTestName() {
            return primaryTestName;
        }

        public String toNarrative() {
            if (TextUtils.isEmpty(code)) {
                return "El generador creativo no pudo bosquejar pruebas unitarias todavía.";
            }
            return String.format(Locale.getDefault(),
                    "Suite generada para %s. Test principal: %s.\nFragmento:\n%s",
                    TextUtils.isEmpty(targetClass) ? "clase desconocida" : targetClass,
                    TextUtils.isEmpty(primaryTestName) ? "sin nombre" : primaryTestName,
                    preview());
        }

        private String preview() {
            if (code.length() <= 400) {
                return code;
            }
            return code.substring(0, 397) + "...";
        }
    }
}
