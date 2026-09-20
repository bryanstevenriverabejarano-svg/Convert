package salve.core;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Bandeja de salida entre la app y un ejecutor externo de PR.
 *
 * La app nunca conserva credenciales de GitHub ni modifica su APK. Produce un
 * artefacto estructurado que un proceso aislado puede recoger, volver a probar
 * y publicar en una rama independiente.
 */
public final class AutoImprovementProposalStore {

    private static final String OUTBOX_DIRECTORY = "auto-improvement/outbox";

    private final File outbox;
    private final Gson gson;

    public AutoImprovementProposalStore(Context context) {
        this(new File(context.getApplicationContext().getFilesDir(), OUTBOX_DIRECTORY));
    }

    AutoImprovementProposalStore(File outbox) {
        this.outbox = outbox;
        this.gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();
    }

    public File enqueue(Proposal proposal) throws IOException {
        if (proposal == null || !proposal.hasActionablePatch()) {
            throw new IllegalArgumentException("La propuesta debe incluir un parche accionable");
        }
        if (!outbox.exists() && !outbox.mkdirs()) {
            throw new IOException("No se pudo crear la bandeja de propuestas");
        }

        String id = System.currentTimeMillis() + "-" + UUID.randomUUID();
        File temporary = new File(outbox, id + ".json.tmp");
        File destination = new File(outbox, id + ".json");
        try (OutputStreamWriter writer = new OutputStreamWriter(
                new FileOutputStream(temporary), StandardCharsets.UTF_8)) {
            gson.toJson(proposal, writer);
        }
        if (!temporary.renameTo(destination)) {
            temporary.delete();
            throw new IOException("No se pudo publicar atómicamente la propuesta");
        }
        return destination;
    }

    public static final class Proposal {
        public final int schemaVersion = 1;
        public final long createdAtEpochMillis;
        public final String targetClass;
        public final String issueSummary;
        public final String patch;
        public final String generatedTests;
        public final boolean syntheticValidationPassed;
        public final boolean sandboxAttempted;
        public final boolean sandboxPassed;
        public final boolean ethicalReviewPassed;

        public Proposal(long createdAtEpochMillis,
                        String targetClass,
                        String issueSummary,
                        String patch,
                        String generatedTests,
                        boolean syntheticValidationPassed,
                        boolean sandboxAttempted,
                        boolean sandboxPassed,
                        boolean ethicalReviewPassed) {
            this.createdAtEpochMillis = createdAtEpochMillis;
            this.targetClass = safe(targetClass);
            this.issueSummary = safe(issueSummary);
            this.patch = safe(patch);
            this.generatedTests = safe(generatedTests);
            this.syntheticValidationPassed = syntheticValidationPassed;
            this.sandboxAttempted = sandboxAttempted;
            this.sandboxPassed = sandboxPassed;
            this.ethicalReviewPassed = ethicalReviewPassed;
        }

        public boolean hasActionablePatch() {
            return !patch.trim().isEmpty() && !patch.trim().startsWith("// No se pudo");
        }

        public boolean isReadyForPullRequest() {
            return hasActionablePatch()
                    && syntheticValidationPassed
                    && sandboxAttempted
                    && sandboxPassed
                    && ethicalReviewPassed;
        }

        private static String safe(String value) {
            return value == null ? "" : value.trim();
        }
    }
}
