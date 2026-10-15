package salve.core.conversation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/** Streaming: el cuento aparece palabra a palabra y la voz lee frases completas mientras se escribe. */
public class LiveDraftTest {
    private static LiveDraft creative() {
        return new LiveDraft(raw -> CreativeRequest.cutTranscriptLeak(CreativeRequest.stripPreamble(raw), "Alex"),
                CreativeRequest.MAX_CHARS);
    }

    private static List<String> feed(LiveDraft draft, String text, int step) {
        List<String> spoken = new ArrayList<>();
        for (int i = 0; i < text.length(); i += step) {
            draft.append(text.substring(i, Math.min(text.length(), i + step)));
            String next = draft.nextSpeech(false);
            if (next != null) spoken.add(next);
        }
        spoken.addAll(draft.drainSpeech());
        return spoken;
    }

    @Test public void preambleIsNeverSpokenAndEverySentenceIsSpokenOnce() {
        String story = "Había una vez un faro que soñaba con viajar. Cada noche miraba los barcos alejarse.\n\n"
                + "Una tormenta apagó su luz y los marineros se perdieron en la niebla. Entonces el faro decidió cantar. "
                + "Su canción cruzó el mar entero y todos encontraron el camino de vuelta. Desde entonces canta cada noche.";
        LiveDraft draft = creative();
        List<String> spoken = feed(draft, "Claro que sí. Aquí tienes uno corto:\n\n" + story, 3);
        String all = String.join(" ", spoken);
        assertFalse(all.contains("Aquí tienes"));
        assertFalse(all.contains("Claro que sí"));
        assertEquals(story.replaceAll("\\s+", " "), all.replaceAll("\\s+", " "));
        assertEquals(story, draft.visible());
        assertTrue("se habla mientras se escribe, no todo al final", spoken.size() >= 2);
    }

    @Test public void holdsTheOpeningUntilItCanBeCleaned() {
        LiveDraft draft = creative();
        draft.append("Claro que sí. Aquí tienes uno corto: ");
        assertNull(draft.nextSpeech(false));
    }

    @Test public void dialogueLeakIsCutFromScreenAndVoice() {
        LiveDraft draft = creative();
        String story = "El zorro aprendió a esperar la primavera junto al río helado, y aquella espera le enseñó a mirar "
                + "con calma cómo el hielo se volvía agua y el agua, por fin, canción. Fin.";
        List<String> spoken = feed(draft, story + "\nAlex: ¡Qué bonito! Cuéntame otro.", 5);
        assertFalse(String.join(" ", spoken).contains("Cuéntame otro"));
        assertFalse(draft.visible().contains("Alex:"));
    }

    @Test public void partsAreCleanedSeparatelyAndJoinedWithParagraphs() {
        LiveDraft draft = creative();
        draft.append("Primera parte de la historia, con un bosque que guarda un secreto antiguo y una niña que lo escucha cada noche sin miedo alguno.");
        draft.nextPart();
        draft.append("Aquí tienes la segunda parte:\n\nLa niña siguió el susurro hasta un claro iluminado por luciérnagas que formaban letras en el aire.");
        String visible = draft.visible();
        assertFalse(visible.contains("Aquí tienes"));
        assertTrue(visible.contains("sin miedo alguno.\n\nLa niña siguió"));
    }

    @Test public void longTailIsSplitForTheSynthesizer() {
        LiveDraft draft = new LiveDraft(null, 20000);
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 120; i++) text.append("Frase número ").append(i).append(" del cuento largo. ");
        draft.append(text.toString());
        for (String chunk : draft.drainSpeech()) assertTrue(chunk.length() <= LiveDraft.MAX_SPEECH_CHARS);
    }
}
