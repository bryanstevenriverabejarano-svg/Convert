package salve.core.mind;

import java.util.Locale;

/**
 * Comprueba en el propio dispositivo si un modelo de embeddings distingue significado en español.
 *
 * <p>Un modelo entrenado solo en inglés (como gte-small) da similitudes casi iguales a frases
 * relacionadas y no relacionadas en español. Activar la memoria por significado o la señal de
 * sorpresa con él inyectaría ruido con apariencia de recuerdo. Esta prueba fija decide si se
 * activan y calibra los umbrales para el modelo concreto.</p>
 */
public final class EmbeddingCalibration {
    static final String[] DOCS = {
            "Las ventas de agosto me quitan el sueño",
            "Mi hermana Ana cumple años el 3 de marzo",
            "Prefiero que me respondas de forma breve",
            "Los lunes reviso las facturas de la empresa",
            "Mañana tengo cita con el dentista",
            "Quiero aprender a tocar la guitarra",
            "El coche hace un ruido raro al frenar",
            "Mi hijo empieza el colegio en septiembre",
    };
    /** QUERIES[i] parafrasea DOCS[i] con poco vocabulario compartido. */
    static final String[] QUERIES = {
            "¿Qué me preocupaba del negocio?",
            "¿Cuándo celebramos lo de Ana?",
            "Me gustan las contestaciones cortas",
            "¿Qué hago al empezar la semana con la contabilidad?",
            "Tengo que ir a que me miren los dientes",
            "Me apetece estudiar música con un instrumento de cuerdas",
            "Hay algo mal en los frenos del vehículo",
            "El niño vuelve a clase después del verano",
    };
    static final double MIN_ACCURACY = 0.75;
    static final double MIN_MARGIN = 0.02;

    public static final class Result {
        public final boolean passed;
        public final double accuracy;
        public final double meanMargin;
        /** Media del coseno entre paráfrasis. */
        public final double relatedMean;
        /** Media del coseno entre frases no relacionadas. */
        public final double unrelatedMean;
        /** Umbral de recuperación: punto medio entre la paráfrasis y el mejor distractor. */
        public final double threshold;
        public final String error;

        Result(boolean passed, double accuracy, double meanMargin, double relatedMean, double unrelatedMean,
               double threshold, String error) {
            this.passed = passed; this.accuracy = accuracy; this.meanMargin = meanMargin;
            this.relatedMean = relatedMean; this.unrelatedMean = unrelatedMean; this.threshold = threshold;
            this.error = error;
        }

        /** Sorpresa 0..1 normalizada con las medias de este modelo (0 = paráfrasis, 1 = sin relación). */
        public double surprise(double cosine) {
            double span = relatedMean - unrelatedMean;
            if (!passed || span <= 1e-6) return -1;
            return Math.max(0, Math.min(1, (relatedMean - cosine) / span));
        }

        public String describe() {
            if (error != null) return "Memoria por significado: no disponible (" + error + ").";
            return String.format(Locale.ROOT,
                    "Memoria por significado: %s (acierto %.0f%%, margen %.3f, paráfrasis %.3f, sin relación %.3f).",
                    passed ? "activa" : "desactivada porque el modelo de embeddings no distingue bien el español",
                    accuracy * 100, meanMargin, relatedMean, unrelatedMean);
        }
    }

    private EmbeddingCalibration() {}

    public static Result failed(String error) { return new Result(false, 0, 0, 0, 0, 1, error); }

    public static Result run(MindEmbedder embedder) {
        if (embedder == null) return failed("sin modelo");
        try {
            float[][] docs = new float[DOCS.length][];
            float[][] queries = new float[QUERIES.length][];
            for (int i = 0; i < DOCS.length; i++) docs[i] = embedder.embed(DOCS[i]);
            for (int i = 0; i < QUERIES.length; i++) queries[i] = embedder.embed(QUERIES[i]);
            int correct = 0;
            double margins = 0, related = 0, unrelated = 0, bestWrong = 0;
            int unrelatedCount = 0;
            for (int q = 0; q < queries.length; q++) {
                double right = cosine(queries[q], docs[q]);
                double wrong = -2;
                for (int d = 0; d < docs.length; d++) {
                    if (d == q) continue;
                    double c = cosine(queries[q], docs[d]);
                    wrong = Math.max(wrong, c);
                    unrelated += c;
                    unrelatedCount++;
                }
                if (right > wrong) correct++;
                margins += right - wrong;
                related += right;
                bestWrong += wrong;
            }
            int n = queries.length;
            double accuracy = correct / (double) n;
            double margin = margins / n;
            double relatedMean = related / n;
            double threshold = (relatedMean + bestWrong / n) / 2;
            boolean passed = accuracy >= MIN_ACCURACY && margin >= MIN_MARGIN;
            return new Result(passed, accuracy, margin, relatedMean, unrelated / unrelatedCount, threshold, null);
        } catch (Exception failure) {
            return failed(failure.getClass().getSimpleName());
        }
    }

    static double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length != b.length || a.length == 0) throw new IllegalArgumentException("Vectores incompatibles.");
        double dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.length; i++) { dot += a[i] * b[i]; na += a[i] * a[i]; nb += b[i] * b[i]; }
        return na == 0 || nb == 0 ? 0 : dot / Math.sqrt(na * nb);
    }
}
