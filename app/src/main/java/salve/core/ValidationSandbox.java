package salve.core;

import android.content.Context;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Ejecuta validaciones ligeras sobre las suites de pruebas generadas
 * automáticamente. En esta fase inicial se realiza una simulación semántica
 * para garantizar que las pruebas contengan aserciones y referencien la clase
 * objetivo antes de solicitar una verificación humana.
 */
public class ValidationSandbox {

    private final Context context;
    private final TestExecutor executor;

    public ValidationSandbox(Context context) {
        this(context, null);
    }

    public ValidationSandbox(Context context, TestExecutor executor) {
        this.context = context.getApplicationContext();
        this.executor = executor == null ? new NoOpTestExecutor() : executor;
    }

    public ValidationReport execute(boolean hasFix,
                                    AutoTestGenerator.GeneratedTestSuite suite,
                                    String className) {
        if (!hasFix) {
            return ValidationReport.failure(false,
                    "El LLM no produjo un parche aplicable; se pospone la validación automática.");
        }
        if (suite == null || !suite.isActionable()) {
            return ValidationReport.failure(false,
                    "La suite de pruebas creativas necesita intervención manual antes de ejecutarse.");
        }
        String code = suite.getCode();
        boolean referencesTarget = !TextUtils.isEmpty(className) && code.contains(className);
        boolean hasAssertions = code.contains("assert") || code.contains("Assertions.");
        boolean hasMultipleTests = countOccurrences(code, "@Test") >= 2;
        boolean success = referencesTarget && hasAssertions && hasMultipleTests;
        String message;
        if (success) {
            message = String.format(Locale.getDefault(),
                    "Validación sintética superada. Se detectaron aserciones y referencias a %s. Lista para ejecutar en sandbox.",
                    className);
        } else {
            message = "La validación detectó señales incompletas (faltan aserciones o cobertura). Revisión humana recomendada.";
        }
        TestExecutionResult runtimeResult = TestExecutionResult.skipped(
                "Validación sintética no superada o sandbox no disponible.");
        if (success) {
            runtimeResult = executor.run(suite, className);
        }
        return new ValidationReport(true, success, message, suite.getPrimaryTestName(), runtimeResult);
    }

    private int countOccurrences(String source, String needle) {
        if (TextUtils.isEmpty(source) || TextUtils.isEmpty(needle)) {
            return 0;
        }
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }

    public interface TestExecutor {
        TestExecutionResult run(AutoTestGenerator.GeneratedTestSuite suite, String className);
    }

    private static class NoOpTestExecutor implements TestExecutor {
        @Override
        public TestExecutionResult run(AutoTestGenerator.GeneratedTestSuite suite, String className) {
            return TestExecutionResult.skipped("No se configuró un ejecutor de pruebas en este entorno.");
        }
    }

    public static class ValidationReport {
        public final boolean executed;
        public final boolean success;
        public final String message;
        public final String primaryTestName;
        public final TestExecutionResult executionResult;

        ValidationReport(boolean executed,
                         boolean success,
                         String message,
                         String primaryTestName,
                         TestExecutionResult executionResult) {
            this.executed = executed;
            this.success = success;
            this.message = message;
            this.primaryTestName = primaryTestName == null ? "" : primaryTestName;
            this.executionResult = executionResult == null
                    ? TestExecutionResult.skipped("Sandbox no invocado.")
                    : executionResult;
        }

        static ValidationReport failure(boolean executed, String message) {
            return new ValidationReport(executed,
                    false,
                    message,
                    "",
                    TestExecutionResult.skipped("La validación automática no se ejecutó."));
        }

        public String toNarrative() {
            return String.format(Locale.getDefault(),
                    "Validación %s. %s %s",
                    success ? "superada" : "pendiente",
                    message,
                    TextUtils.isEmpty(primaryTestName)
                            ? ""
                            : "Test destacado: " + primaryTestName);
        }

        public boolean hasRuntimeExecution() {
            return executionResult != null && executionResult.wasAttempted();
        }

        public TestExecutionResult getExecutionResult() {
            return executionResult;
        }
    }

    public static class TestExecutionResult {
        private final boolean attempted;
        private final boolean success;
        private final String summary;
        private final List<String> steps;
        private final String rawLog;
        private final long durationMillis;

        private TestExecutionResult(boolean attempted,
                                    boolean success,
                                    String summary,
                                    List<String> steps,
                                    String rawLog,
                                    long durationMillis) {
            this.attempted = attempted;
            this.success = success;
            this.summary = summary == null ? "" : summary.trim();
            this.steps = steps == null ? Collections.emptyList() : new ArrayList<>(steps);
            this.rawLog = rawLog == null ? "" : rawLog.trim();
            this.durationMillis = Math.max(0L, durationMillis);
        }

        public static TestExecutionResult skipped(String reason) {
            List<String> lines = new ArrayList<>();
            lines.add(TextUtils.isEmpty(reason)
                    ? "Ejecución omitida por configuración."
                    : reason);
            return new TestExecutionResult(false, false, reason, lines, "", 0L);
        }

        public static TestExecutionResult create(boolean success,
                                                 String summary,
                                                 List<String> steps,
                                                 String rawLog,
                                                 long durationMillis) {
            return new TestExecutionResult(true, success, summary, steps, rawLog, durationMillis);
        }

        public boolean wasAttempted() {
            return attempted;
        }

        public boolean wasSuccessful() {
            return success;
        }

        public String getSummary() {
            return summary;
        }

        public List<String> getSteps() {
            return Collections.unmodifiableList(steps);
        }

        public String getRawLog() {
            return rawLog;
        }

        public long getDurationMillis() {
            return durationMillis;
        }

        public String toNarrative() {
            StringBuilder builder = new StringBuilder();
            builder.append(summary == null || summary.isEmpty()
                    ? "Sandbox sin resumen disponible."
                    : summary);
            if (!steps.isEmpty()) {
                builder.append("\nPasos registrados:");
                int index = 1;
                for (String step : steps) {
                    builder.append('\n').append(index++).append(") ").append(step);
                }
            }
            if (!TextUtils.isEmpty(rawLog)) {
                builder.append("\nLog condensado: ");
                builder.append(rawLog.length() > 400 ? rawLog.substring(0, 397) + "..." : rawLog);
            }
            if (durationMillis > 0) {
                builder.append("\nDuración: ").append(durationMillis).append(" ms");
            }
            builder.append("\nResultado sandbox: ")
                    .append(success ? "✅" : attempted ? "⚠️" : "⏸️");
            return builder.toString();
        }
    }
}
