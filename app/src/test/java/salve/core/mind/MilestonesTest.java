package salve.core.mind;

import static org.junit.Assert.*;
import org.junit.Test;

public class MilestonesTest {
    static final class Mem implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }
    private static final String SCENE = "reloj 10 8 45 40+tijeras 40 30 70 75+libro 5 20 95 95";

    @Test public void theFirstSceneDrawingIsRememberedAsTheFirst() {
        Mem store = new Mem();
        Milestones m = new Milestones(store, () -> 1_792_000_000_000L);
        Milestones.Milestone first = m.saveDrawing("reloj, tijeras, cinta y un libro", SCENE, "es el mejor dibujo que has hecho");
        assertEquals("mi primer dibujo de una escena real, hecho a partir de una foto", first.note);
        assertSame("guardarlo dos veces no lo duplica", first, m.saveDrawing("x", SCENE, "otra vez"));
        Milestones.Milestone second = m.saveDrawing("una taza y una mesa", "taza 60 50 70 66+mesa 5 62 98 99", "");
        assertEquals("un dibujo de una escena real", second.note);
        assertEquals("mi primer dibujo guardado", m.saveDrawing("casa", "casa+luna", "").note);
        // Sobrevive a reiniciar la app.
        Milestones again = new Milestones(store, () -> 2L);
        assertEquals(3, again.all().size());
        assertEquals(SCENE, again.findDrawing("enséñame tu primer dibujo de escena").sceneKey);
        assertEquals("casa+luna", again.findDrawing("muéstrame tu dibujo de la casa").sceneKey);
        String prompt = again.forPrompt(3);
        assertTrue(prompt, prompt.contains("MIS_HITOS") && prompt.contains("mi primer dibujo de una escena real") && prompt.contains("el mejor dibujo"));
        assertTrue(Milestones.confirmation(first).contains("enséñame mi primer dibujo de una escena"));
    }

    @Test public void phrases() {
        assertTrue(Milestones.asksToSaveDrawing("Guarda este dibujo, es importante"));
        assertTrue(Milestones.asksToSaveDrawing("guárdalo que es especial"));
        assertTrue(Milestones.asksToSaveDrawing("Este dibujo es muy importante"));
        assertFalse(Milestones.asksToSaveDrawing("guarda esta foto"));
        assertFalse(Milestones.asksToSaveDrawing("dibuja un gato"));
        assertTrue(Milestones.asksToShowDrawing("Enséñame tu primer dibujo"));
        assertTrue(Milestones.asksToShowDrawing("muéstrame tus dibujos guardados"));
        assertTrue(Milestones.asksToShowDrawing("¿Cuáles son tus hitos?"));
        assertFalse(Milestones.asksToShowDrawing("enséñame a dibujar"));
    }
}
