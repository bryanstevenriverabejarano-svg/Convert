import java.io.File;
import salve.core.cognition.SelfCommandMeaning;
import salve.core.embeddings.E5EmbeddingEngine;
import salve.core.mind.EmbeddingCalibration;

/**
 * Mide con el E5 real (el mismo del móvil) si Salve entiende sus funciones por significado.
 * Las frases de aquí NO están entre los ejemplos: son otras formas de pedirlo. Falla si ofrece
 * una función a algo que no la pide (seguridad) o si entiende demasiado poco (utilidad).
 */
public final class VerifySelfMeaning {
    /** {frase, función esperada o "" si no debe ofrecer ninguna}. */
    static final String[][] CASES = {
            {"oye, quiero que me escuches con tu propio oído", "EAR_MODE"},
            {"vuelve al dictado normal de Android", "EAR_MODE"},
            {"ponte la voz que suena más real", "VOICE_ENGINE"},
            {"atiende a esto que está sonando", "LISTEN"},
            {"¿reconoces la canción que suena?", "LISTEN"},
            {"enséñame tu cabeza por dentro", "MIND_MAP"},
            {"quiero ver lo que hiciste en las tareas", "OPEN_JOURNAL"},
            {"enséñame los PDF que has guardado", "OPEN_JOURNAL"},
            {"guíñame un ojo", "BODY_ACTION"},
            {"haz que tus circuitos se iluminen", "BODY_ACTION"},
            {"quiero poder llamarte solo con tu nombre", "WAKE_WORD_ON"},
            {"¿jugamos una partida de ajedrez?", "CHESS_START"},
            {"quiero que seas la asistente de mi teléfono", "DEFAULT_ASSISTANT"},
            {"¿qué tal va tu evolución?", "EVOLUTION_STATUS"},
            // Nada de esto es una función suya.
            {"abre YouTube", ""}, {"pon una canción de Bad Bunny", ""}, {"llama a papá", ""},
            {"¿qué hora es?", ""}, {"buenas noches, Salve", ""}, {"apaga el bluetooth", ""},
            {"busca recetas de lentejas", ""}, {"cuéntame un chiste", ""}, {"hazle una foto al gato", ""},
            {"me duele la cabeza", ""}, {"¿qué opinas de la inteligencia artificial?", ""},
            {"no actives el modo oídos", ""}, {"no quiero jugar al ajedrez ahora", ""},
            {"escucha, tengo que contarte algo", ""}, {"¿cómo estás hoy?", ""}, {"abre la cámara", ""},
            {"pon el modo avión", ""}, {"enséñame fotos de la playa", ""}, {"qué guapa estás", ""},
            {"recuérdame comprar pan", ""}, {"me voy a dormir", ""},
    };

    public static void main(String[] args) throws Exception {
        try (E5EmbeddingEngine engine = new E5EmbeddingEngine(new File(args[1]), new File(args[0]))) {
            EmbeddingCalibration.Result c = EmbeddingCalibration.run(engine::query);
            if (!c.passed) throw new AssertionError("Calibración simétrica fallida");
            SelfCommandMeaning.Index index = SelfCommandMeaning.build(engine::query);
            int asked = 0, understood = 0, wrong = 0, falseOffers = 0;
            System.out.printf("Calibración: paráfrasis %.3f, sin relación %.3f%n", c.relatedMean, c.unrelatedMean);
            for (String[] row : CASES) {
                float[] q = engine.query(SelfCommandMeaning.queryText(row[0]));
                java.util.function.DoubleUnaryOperator rel = cosine -> (cosine - c.unrelatedMean) / (c.relatedMean - c.unrelatedMean);
                SelfCommandMeaning.Match m = SelfCommandMeaning.understand(row[0], q, index, c.relatedMean, c.unrelatedMean);
                SelfCommandMeaning.Scores sc = SelfCommandMeaning.measure(q, index);
                String got = m == null ? "" : m.command;
                System.out.printf("%-46s → %-18s esperado %-18s | mejor %s %.3f (%.2f) «%s» · 2ª %.3f (%.2f) · otra %.3f (%.2f) «%s»%n",
                        row[0], got.isEmpty() ? "—" : got, row[1].isEmpty() ? "—" : row[1],
                        sc.bestId(), sc.bestScore, rel.applyAsDouble(sc.bestScore), sc.bestExampleText(),
                        sc.second, rel.applyAsDouble(sc.second), sc.other, rel.applyAsDouble(sc.other), sc.otherText());
                if (row[1].isEmpty()) { if (m != null) falseOffers++; continue; }
                asked++;
                if (row[1].equals(got)) understood++;
                else if (m != null) wrong++;
            }
            System.out.printf("Entendidas %d/%d; función equivocada %d; ofrecidas sin pedirlas %d%n",
                    understood, asked, wrong, falseOffers);
            if (wrong > 0 || falseOffers > 0) throw new AssertionError("Ofrecería una función que no se pidió");
            if (understood * 2 < asked) throw new AssertionError("Entiende menos de la mitad por significado");
        }
    }
}
