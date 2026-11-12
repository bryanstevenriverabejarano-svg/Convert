package salve.core.cognition;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.After;
import org.junit.Test;

/** Entender por significado: ofrece la función, el controlador pone los detalles y nada más. */
public class SelfCommandMeaningTest {
    private static final int DIM = 96;

    /** Cada frase distinta, un vector casi ortogonal a las demás. */
    private static float[] vector(String text) {
        Random r = new Random(text.hashCode());
        float[] v = new float[DIM];
        for (int i = 0; i < DIM; i++) v[i] = (float) r.nextGaussian();
        return v;
    }

    /** Una paráfrasis: el vector del ejemplo con un poco de ruido. */
    private static float[] near(String example, double noise) {
        float[] v = vector(example), n = vector("ruido " + example);
        for (int i = 0; i < DIM; i++) v[i] += (float) (noise * n[i]);
        return v;
    }

    private static SelfCommandMeaning.Index index() throws Exception {
        return SelfCommandMeaning.build(SelfCommandMeaningTest::vector);
    }

    @After public void forget() { SelfCommandMeaning.remember(null, null); }

    @Test public void everyCanonicalPhraseIsUnderstoodByTheExistingRecognizer() {
        // Las de solo lectura no tienen detalles: su frase canónica no se usa para nada.
        for (SelfCommandMeaning.Meaning m : SelfCommandMeaning.MEANINGS)
            if (!SelfCommandIntent.Command.valueOf(m.command).readOnly)
                assertTrue(m.canonical, SelfCommandIntent.detected(m.canonical).contains(SelfCommandIntent.Command.valueOf(m.command)));
    }

    @Test public void aParaphraseIsUnderstoodByMeaning() throws Exception {
        SelfCommandMeaning.Match match = SelfCommandMeaning.understand("déjame ver por dentro lo que piensas",
                near("enséñame cómo piensas por dentro", 0.3), index(), c -> c);
        assertNotNull(match);
        assertEquals("MIND_MAP", match.command);
        assertEquals("abre el mapa de tu mente", match.canonical);
    }

    @Test public void somethingThatIsNotHerFunctionOffersNothing() throws Exception {
        assertNull(SelfCommandMeaning.understand("ábreme Spotify", near("abre Spotify", 0.3), index(), c -> c));
    }

    @Test public void unrelatedTalkOffersNothing() throws Exception {
        assertNull(SelfCommandMeaning.understand("me encanta el café", vector("me encanta el café"), index(), c -> c));
    }

    @Test public void anExplicitNoIsNeverTurnedIntoAnAction() throws Exception {
        assertNull(SelfCommandMeaning.understand("no actives tus oídos",
                vector("activa el modo oídos"), index(), c -> c));
    }

    @Test public void onAndOffTooCloseIsAmbiguousAndOffersNothing() throws Exception {
        float[] on = vector("activa el modo oídos"), off = vector("desactiva el modo oídos");
        float[] both = new float[DIM];
        for (int i = 0; i < DIM; i++) both[i] = on[i] + off[i];
        assertNull(SelfCommandMeaning.understand("lo de los oídos", both, index(), c -> c));
    }

    @Test public void withoutCalibrationNothingIsUnderstood() throws Exception {
        assertNull(SelfCommandMeaning.understand("quiero ver tu mente", vector("quiero ver tu mente"), index(), c -> -1));
    }

    @Test public void theModelMayChooseItButTheDetailsComeFromTheController() throws Exception {
        String input = "oye, usa tu oído para notar cómo te hablo";
        assertFalse(SelfCommandIntent.detected(input).contains(SelfCommandIntent.Command.EAR_MODE));
        try {
            SelfCommandIntent.verified(input, "EAR_MODE");
            fail("Sin entenderla, una acción no pedida se rechaza");
        } catch (IllegalArgumentException expected) { }

        SelfCommandMeaning.Match match = SelfCommandMeaning.understand(input,
                near("usa tu oído para entender cómo te hablo", 0.3), index(), c -> c);
        assertNotNull(match);
        SelfCommandMeaning.remember(input, match);
        assertEquals(SelfCommandIntent.Command.EAR_MODE, SelfCommandIntent.verified(input, "EAR_MODE"));
        assertEquals("activa el modo oídos", SelfCommandIntent.wordsFor(input, SelfCommandIntent.Command.EAR_MODE));
        assertTrue(SelfCommandIntent.available(input).contains(SelfCommandIntent.Command.EAR_MODE));
        // Otra acción distinta sigue sin estar pedida.
        try {
            SelfCommandIntent.verified(input, "CHESS_START");
            fail();
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void whatWasUnderstoodBelongsOnlyToThatTurn() throws Exception {
        String input = "quiero ver tu mente";
        SelfCommandMeaning.remember(input, SelfCommandMeaning.understand(input, vector(input), index(), c -> c));
        assertNotNull(SelfCommandMeaning.forTurn(input));
        assertNull(SelfCommandMeaning.forTurn("abre el chat"));
        assertFalse(SelfCommandIntent.understood("abre el chat").contains(SelfCommandIntent.Command.MIND_MAP));
    }

    @Test public void exactPhrasesKeepUsingTheUserWords() {
        String input = "activa el modo oídos";
        assertEquals(input, SelfCommandIntent.wordsFor(input, SelfCommandIntent.Command.EAR_MODE));
    }
}
