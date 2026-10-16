package salve.core.mind;

import static org.junit.Assert.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Test;

public class SelfUpdatesTest {
    private static List<SelfUpdates.Update> real() throws Exception {
        return SelfUpdates.parse(new String(Files.readAllBytes(Paths.get("src/main/assets/self/updates.json")), "UTF-8"));
    }

    @Test public void knowsWhatChanged() throws Exception {
        List<SelfUpdates.Update> all = real();
        assertTrue(all.size() >= 15);
        assertTrue(SelfUpdates.asksAbout("Que opinas de tus ultimas actualizaciones??"));
        assertTrue(SelfUpdates.asksAbout("¿qué hay de nuevo en ti?"));
        assertTrue(SelfUpdates.asksAbout("qué te han cambiado"));
        assertFalse(SelfUpdates.asksAbout("¿Qué tiempo hace?"));
        String text = SelfUpdates.render(all, 5, 1500);
        String newest = all.get(all.size() - 1).title, older = all.get(all.size() - 2).title;
        assertTrue(text.contains(newest) && text.contains(older));
        assertTrue("las más recientes primero", text.indexOf(newest) < text.indexOf(older));
        assertTrue(text.length() <= 1500);
    }

    @Test public void noticesNewOnes() throws Exception {
        List<SelfUpdates.Update> all = real();
        assertEquals(all.size(), SelfUpdates.since(all, null).size());
        assertEquals(2, SelfUpdates.since(all, all.get(all.size() - 3).id).size());
        assertTrue(SelfUpdates.since(all, all.get(all.size() - 1).id).isEmpty());
        assertTrue(SelfUpdates.parse("roto").isEmpty());
    }
}
