package salve.core.cognition;

import static org.junit.Assert.*;

import java.util.Random;
import org.junit.After;
import org.junit.Test;

/** Entender por significado: ofrece la función, el controlador pone los detalles y nada más. */
public class ActionMeaningTest {
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

    private static ActionMeaning.Index index() throws Exception {
        return ActionMeaning.build(ActionMeaningTest::vector);
    }

    @After public void forget() { ActionMeaning.remember(null, null); }

    @Test public void everyCanonicalPhraseIsUnderstoodByTheExistingRecognizer() {
        for (ActionMeaning.Meaning m : ActionMeaning.MEANINGS) {
            switch (m.command) {
                case "device.setting":
                    assertTrue(m.canonical, salve.core.devicecontrol.SystemSetting.offeredExact(m.canonical)); break;
                case "device.control":
                    assertNotNull(m.canonical, DeviceControlIntent.parseExact(m.canonical)); break;
                case "device.media_control":
                    assertTrue(m.canonical, MediaActionIntent.offeredExact(m.canonical)); break;
                default:
                    // Música nueva, apps y rutas: el destino sale de la frase real, no de la canónica.
                    if (m.command.indexOf('.') >= 0) break;
                    // Las de solo lectura no tienen detalles: su frase canónica no se usa para nada.
                    SelfCommandIntent.Command c = SelfCommandIntent.Command.valueOf(m.command);
                    if (!c.readOnly) assertTrue(m.canonical, SelfCommandIntent.detected(m.canonical).contains(c));
            }
        }
    }

    private static ActionMeaning.Match understood(String id, String canonical) {
        for (ActionMeaning.Meaning m : ActionMeaning.MEANINGS)
            if (m.command.equals(id) && m.canonical.equals(canonical)) return new ActionMeaning.Match(m, m.examples[0], 0.95);
        throw new AssertionError(id + " " + canonical);
    }

    @Test public void aSettingUnderstoodByMeaningTakesItsDetailsFromTheCanonicalPhrase() {
        String input = "córtame el wifi, anda";
        assertNull(salve.core.devicecontrol.SystemSetting.parse(input));
        ActionMeaning.remember(input, understood("device.setting", "desactiva el wifi"));
        assertEquals("WIFI_OFF", salve.core.devicecontrol.SystemSetting.parse(input).action.name());
        assertEquals("WIFI_OFF", salve.core.devicecontrol.SystemSetting.verified(input, "WIFI_OFF").action.name());
        try {
            salve.core.devicecontrol.SystemSetting.verified(input, "WIFI_ON");
            fail("El modelo no puede convertir apagar en encender");
        } catch (IllegalArgumentException expected) { }
        // Lo entendido es para el wifi: no se cuela en el mando de la linterna ni en la música.
        assertNull(DeviceControlIntent.parse(input));
        assertFalse(MediaActionIntent.offered(input));
        // Y no es una frase exacta: el controlador no lo exige ni lo ejecuta por su cuenta.
        assertFalse(salve.core.devicecontrol.SystemSetting.offeredExact(input));
    }

    @Test public void torchAndMusicByMeaning() {
        String dark = "no veo nada aquí";
        ActionMeaning.remember(dark, understood("device.control", "enciende la linterna"));
        assertEquals(DeviceControlIntent.Action.TORCH_ON, DeviceControlIntent.verified(dark, "TORCH_ON").action);

        String skip = "uf, esta no me gusta";
        ActionMeaning.remember(skip, understood("device.media_control", "siguiente canción"));
        assertEquals("NEXT", MediaActionIntent.verifiedAction(skip, "NEXT").name());
        assertFalse(MediaActionIntent.offeredExact(skip));
    }

    @Test public void newMusicByMeaningStillNeedsTheTargetInHisWords() {
        String input = "me apetece oír a Queen";
        ActionMeaning.remember(input, understood("device.play_music", "pon música"));
        assertTrue(DeviceActionIntent.offered(DeviceActionIntent.Kind.PLAY_MUSIC, input));
        assertEquals("Queen", DeviceActionIntent.checkedTarget(DeviceActionIntent.Kind.PLAY_MUSIC, input, "Queen"));
        try {
            DeviceActionIntent.checkedTarget(DeviceActionIntent.Kind.PLAY_MUSIC, input, "Metallica");
            fail("El modelo no puede inventar el destino");
        } catch (IllegalArgumentException expected) { }
        assertFalse(DeviceActionIntent.offered(DeviceActionIntent.Kind.OPEN_APP, input));
        assertFalse(DeviceActionIntent.offeredExact(DeviceActionIntent.Kind.PLAY_MUSIC, input));
    }

    @Test public void callingIsNeverUnderstoodByMeaning() {
        assertNull(DeviceActionIntent.tool(DeviceActionIntent.Kind.CALL));
        for (ActionMeaning.Meaning m : ActionMeaning.MEANINGS) assertNotEquals("device.call", m.command);
    }

    @Test public void aToolUnderstoodIsNotOneOfHerOwnFunctions() {
        String input = "córtame el wifi, anda";
        ActionMeaning.remember(input, understood("device.setting", "desactiva el wifi"));
        assertTrue(SelfCommandIntent.understood(input).isEmpty());
    }

    @Test public void aParaphraseIsUnderstoodByMeaning() throws Exception {
        ActionMeaning.Match match = ActionMeaning.understand("déjame ver por dentro lo que piensas",
                near("enséñame cómo piensas por dentro", 0.3), index(), 1.0, 0.0);
        assertNotNull(match);
        assertEquals("MIND_MAP", match.command);
        assertEquals("abre el mapa de tu mente", match.canonical);
    }

    @Test public void somethingThatIsNotHerFunctionOffersNothing() throws Exception {
        assertNull(ActionMeaning.understand("ábreme Spotify", near("abre Spotify", 0.3), index(), 1.0, 0.0));
    }

    @Test public void unrelatedTalkOffersNothing() throws Exception {
        assertNull(ActionMeaning.understand("me encanta el café", vector("me encanta el café"), index(), 1.0, 0.0));
    }

    @Test public void anExplicitNoIsNeverTurnedIntoAnAction() throws Exception {
        assertNull(ActionMeaning.understand("no actives tus oídos",
                vector("activa el modo oídos"), index(), 1.0, 0.0));
    }

    @Test public void onAndOffTooCloseIsAmbiguousAndOffersNothing() throws Exception {
        float[] on = vector("activa el modo oídos"), off = vector("desactiva el modo oídos");
        float[] both = new float[DIM];
        for (int i = 0; i < DIM; i++) both[i] = on[i] + off[i];
        assertNull(ActionMeaning.understand("lo de los oídos", both, index(), 1.0, 0.0));
    }

    @Test public void withoutCalibrationNothingIsUnderstood() throws Exception {
        assertNull(ActionMeaning.understand("quiero ver tu mente", vector("quiero ver tu mente"), index(), 0.8, 0.8));
    }

    @Test public void theModelMayChooseItButTheDetailsComeFromTheController() throws Exception {
        String input = "oye, usa tu oído para notar cómo te hablo";
        assertFalse(SelfCommandIntent.detected(input).contains(SelfCommandIntent.Command.EAR_MODE));
        try {
            SelfCommandIntent.verified(input, "EAR_MODE");
            fail("Sin entenderla, una acción no pedida se rechaza");
        } catch (IllegalArgumentException expected) { }

        ActionMeaning.Match match = ActionMeaning.understand(input,
                near("usa tu oído para entender cómo te hablo", 0.3), index(), 1.0, 0.0);
        assertNotNull(match);
        ActionMeaning.remember(input, match);
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
        ActionMeaning.remember(input, ActionMeaning.understand(input, vector(input), index(), 1.0, 0.0));
        assertNotNull(ActionMeaning.forTurn(input));
        assertNull(ActionMeaning.forTurn("abre el chat"));
        assertFalse(SelfCommandIntent.understood("abre el chat").contains(SelfCommandIntent.Command.MIND_MAP));
    }

    @Test public void exactPhrasesKeepUsingTheUserWords() {
        String input = "activa el modo oídos";
        assertEquals(input, SelfCommandIntent.wordsFor(input, SelfCommandIntent.Command.EAR_MODE));
    }

    @Test public void aRefusalIsNotARequestButTurningSomethingOffIs() {
        assertTrue(ActionMeaning.refused("no quiero jugar al ajedrez ahora"));
        assertTrue(ActionMeaning.refused("No me apetece que parpadees"));
        assertFalse(ActionMeaning.refused("ya no quiero que te despiertes al oír tu nombre"));
    }

    @Test public void herNameIsNotPartOfTheMeaning() {
        assertEquals("buenas noches", ActionMeaning.queryText("buenas noches, Salve"));
        assertEquals("enséñame tu mente", ActionMeaning.queryText("Oye Salve, enséñame tu mente"));
        assertEquals("salvemos el día", ActionMeaning.queryText("salvemos el día"));
    }
}
