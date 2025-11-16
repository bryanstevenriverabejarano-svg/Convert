package salve.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa un ciclo de auto-mejora de Salve compuesto por varias etapas.
 * Cada etapa produce artefactos que luego se pueden narrar o persistir en la
 * memoria emocional para mantener trazabilidad.
 */
public class AutoImprovementSession {

    public enum Stage {
        ANALYSIS,
        DESIGN,
        GENERATION,
        TEST_GENERATION,
        VALIDATION,
        TEST_EXECUTION,
        RADAR_MONITORING,
        ETHICAL_REVIEW,
        COGNITIVE_EXPANSION,
        REVIEW
    }

    public static class Artifact {
        private final Stage stage;
        private final String title;
        private final String content;
        private final boolean success;

        Artifact(Stage stage, String title, String content, boolean success) {
            this.stage = stage;
            this.title = title;
            this.content = content;
            this.success = success;
        }

        public Stage getStage() {
            return stage;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public boolean isSuccess() {
            return success;
        }
    }

    private final String issueSummary;
    private final List<Artifact> artifacts;

    public AutoImprovementSession(String issueSummary) {
        this.issueSummary = issueSummary == null ? "" : issueSummary;
        this.artifacts = new ArrayList<>();
    }

    public void addArtifact(Stage stage, String title, String content, boolean success) {
        artifacts.add(new Artifact(stage, title, content, success));
    }

    public List<Artifact> getArtifacts() {
        return Collections.unmodifiableList(artifacts);
    }

    public boolean hasFailures() {
        for (Artifact artifact : artifacts) {
            if (!artifact.success) {
                return true;
            }
        }
        return false;
    }

    /**
     * Genera un relato amigable con la voz creativa del manifiesto para ser
     * almacenado en la MemoriaEmocional.
     */
    public String toNarrative(CreativityManifest manifest) {
        StringBuilder builder = new StringBuilder();
        builder.append("Ciclo de auto-mejora inspirado en el manifiesto creativo.\n");
        if (!issueSummary.isEmpty()) {
            builder.append("Resumen del issue: ").append(issueSummary).append('\n');
        }
        for (Artifact artifact : artifacts) {
            builder.append("→ ").append(artifact.stage.name()).append(" · ")
                    .append(artifact.title == null ? "" : artifact.title)
                    .append("\n");
            if (artifact.content != null && !artifact.content.trim().isEmpty()) {
                builder.append(artifact.content.trim()).append("\n");
            }
            builder.append(artifact.success ? "Resultado: ✅\n" : "Resultado: ⚠️\n");
        }
        builder.append("Principios recordados:\n");
        int index = 1;
        for (String principle : manifest.getCreativePrinciples()) {
            builder.append("  • ").append(index++).append('.').append(' ').append(principle).append('\n');
        }
        return builder.toString().trim();
    }
}
