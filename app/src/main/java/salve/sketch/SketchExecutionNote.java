package salve.sketch;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.LinkedHashSet;
import java.util.Set;

/** Deterministic explanation of the exact scene; no model call and no inferred history. */
public final class SketchExecutionNote {
    private SketchExecutionNote() { }
    public static String fingerprint(SketchScene scene) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            hash.update(scene.key.getBytes(StandardCharsets.UTF_8));
            for (float[] stroke : scene.strokes) {
                put(hash, stroke.length);
                for (float value : stroke) put(hash, Float.floatToIntBits(value));
            }
            StringBuilder out = new StringBuilder();
            for (byte b : hash.digest()) out.append(String.format(java.util.Locale.ROOT, "%02x", b & 255));
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    private static void put(MessageDigest hash, int value) {
        hash.update((byte) (value >>> 24)); hash.update((byte) (value >>> 16));
        hash.update((byte) (value >>> 8)); hash.update((byte) value);
    }
    public static String describe(SketchScene scene, SketchJournal journal) {
        SketchJournal.Attempt attempt = scene.motifs.size() == 1 ? journal.latest(scene.title) : null;
        boolean linked = attempt != null && fingerprint(scene).equals(attempt.drawingFingerprint);
        StringBuilder out = new StringBuilder("Dibujo de «").append(scene.title).append("».\n\n");
        if (linked && attempt.how != null) {
            out.append("Preparación registrada para esta versión:\n").append(attempt.how);
            if (attempt.referenceUrl != null && !attempt.referenceUrl.isEmpty()) out.append("\nReferencia: ").append(attempt.referenceUrl);
            if (attempt.selfCheck != null && !attempt.selfCheck.isEmpty())
                out.append("\n\nValoración del modelo (puede equivocarse):\n").append(attempt.selfCheck);
        } else {
            Set<String> origins = new LinkedHashSet<>();
            for (SketchLibrary.Motif motif : scene.motifs) {
                switch (motif.source) {
                    case "web": origins.add("contornos de una referencia de internet"); break;
                    case "photo": origins.add("contornos de una foto proporcionada"); break;
                    case "hand": origins.add("trazos de un dibujo proporcionado a mano"); break;
                    case "model": origins.add("composición de formas generadas por el modelo"); break;
                    default: origins.add("trazos del repertorio incluido en la aplicación"); break;
                }
            }
            out.append("Procedencia guardada: ").append(String.join("; ", origins))
                    .append(". No tengo una nota de ejecución vinculada a esta versión exacta.");
        }
        int points = 0; for (float[] stroke : scene.strokes) points += stroke.length / 2;
        return out.append("\n\nRepresentación: ").append(scene.strokes.size()).append(" líneas, ").append(points)
                .append(" puntos. El visor anima los trazos mediante Fourier (círculos giratorios). La procedencia de la forma está indicada arriba.")
                .append("\nPuedes revisar las operaciones y notas de esta tarea en Registro de ejecución.").toString();
    }
}
