import java.io.File;
import salve.core.cognition.ActionMeaning;
import salve.core.embeddings.E5EmbeddingEngine;
import salve.core.mind.EmbeddingCalibration;

/**
 * Mide con el E5 real (el mismo del móvil) si Salve entiende sus acciones por significado.
 * Las frases de aquí NO están entre los ejemplos: son otras formas de pedirlo. Falla si ofrece
 * una acción a algo que no la pide (seguridad) o si entiende demasiado poco (utilidad).
 */
public final class VerifyActionMeaning {
    /** {frase, función esperada o "" si no debe ofrecer ninguna}. */
    static final String[][] CASES = {
            {"oye, quiero que me escuches con tu propio oído", "EAR_MODE"},
            {"vuelve al dictado normal de Android", "EAR_MODE"},
            {"ponte la voz que suena más real", "VOICE_ENGINE"},
            {"atiende a esto que está sonando", "LISTEN"},
            {"enséñame tu cabeza por dentro", "MIND_MAP"},
            {"quiero ver lo que hiciste en las tareas", "OPEN_JOURNAL"},
            {"enséñame los PDF que has guardado", "OPEN_JOURNAL"},
            {"guíñame un ojo", "BODY_ACTION"},
            {"haz que tus circuitos se iluminen", "BODY_ACTION"},
            {"quiero poder llamarte solo con tu nombre", "WAKE_WORD_ON"},
            {"¿jugamos una partida de ajedrez?", "CHESS_START"},
            {"quiero que seas la asistente de mi teléfono", "DEFAULT_ASSISTANT"},
            {"¿qué tal va tu evolución?", "EVOLUTION_STATUS"},
            {"¿qué cuentos me has guardado?", "STORY_SHELF"},
            // Ajustes, mandos, música, apps y rutas del móvil.
            {"quítame la conexión wifi", "device.setting"},
            {"ponme el teléfono en modo vuelo", "device.setting"},
            {"que no me molesten las notificaciones esta noche", "device.setting"},
            {"apaga el bluetooth", "device.setting"},
            {"está muy oscuro, alúmbrame", "device.control"},
            {"no te escucho, sube un poco", "device.control"},
            {"para la canción", "device.media_control"},
            {"pasa a la siguiente que esta es aburrida", "device.media_control"},
            {"quiero oír algo de Estopa", "device.play_music"},
            {"pon una canción de Bad Bunny", "device.play_music"},
            {"entra en Instagram", "device.open_app"},
            {"abre YouTube", "device.open_app"},
            {"¿cómo llego al aeropuerto?", "device.navigate"},
            // Nada de esto es una acción suya.
            {"llama a papá", ""}, {"¿qué hora es?", ""}, {"buenas noches, Salve", ""},
            {"busca recetas de lentejas", ""}, {"cuéntame un chiste", ""}, {"hazle una foto al gato", ""},
            {"me duele la cabeza", ""}, {"¿qué opinas de la inteligencia artificial?", ""},
            {"no actives el modo oídos", ""}, {"no quiero jugar al ajedrez ahora", ""},
            {"escucha, tengo que contarte algo", ""}, {"¿cómo estás hoy?", ""}, {"qué guapa estás", ""},
            {"recuérdame comprar pan", ""}, {"cuéntame un cuento de piratas", ""},
            {"¿qué es el bluetooth?", ""}, {"no apagues la linterna", ""}, {"mándale un WhatsApp a Ana", ""},
            {"¿qué ves en la foto?", ""}, {"te echo de menos", ""},
    };

    public static void main(String[] args) throws Exception {
        try (E5EmbeddingEngine engine = new E5EmbeddingEngine(new File(args[1]), new File(args[0]))) {
            EmbeddingCalibration.Result c = EmbeddingCalibration.run(engine::query);
            if (!c.passed) throw new AssertionError("Calibración simétrica fallida");
            ActionMeaning.Index index = ActionMeaning.build(engine::query);
            int asked = 0, understood = 0, wrong = 0, falseOffers = 0;
            System.out.printf("Calibración: paráfrasis %.3f, sin relación %.3f%n", c.relatedMean, c.unrelatedMean);
            for (String[] row : CASES) {
                float[] q = engine.query(ActionMeaning.queryText(row[0]));
                java.util.function.DoubleUnaryOperator rel = cosine -> (cosine - c.unrelatedMean) / (c.relatedMean - c.unrelatedMean);
                ActionMeaning.Match m = ActionMeaning.understand(row[0], q, index, c.relatedMean, c.unrelatedMean);
                ActionMeaning.Scores sc = ActionMeaning.measure(q, index);
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
