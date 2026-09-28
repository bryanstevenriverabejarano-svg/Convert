package salve.core.visual;

import org.junit.Test;
import static org.junit.Assert.*;

public class VisualIdentityContextTest {
    @Test public void selfProfileRequiresExplicitNameRelationshipAndPosition() {
        assertTrue(VisualMemoryRecord.create("Foto", "Bryan", "yo", "única persona", true).identifiesUser());
        assertFalse(VisualMemoryRecord.create("Soy yo", "", "yo", "única persona", true).identifiesUser());
        assertFalse(VisualMemoryRecord.create("Foto", "Diego", "hermano", "izquierda", true).identifiesUser());
        assertFalse(VisualMemoryRecord.create("Foto", "Bryan", "yo", "", true).identifiesUser());
    }

    @Test public void textOnlyModelReceivesDeclaredIdentityWithoutClaimingPixelAccess() {
        VisualMemoryRecord photo = VisualMemoryRecord.create("Foto", "Bryan", "yo", "centro", true);
        assertTrue(photo.promptContext(false).contains("NO RECIBES PIXELES"));
        assertTrue(photo.promptContext(false).contains("Bryan"));
        assertFalse(photo.promptContext(false).contains("puedes volver a analizarla"));
        assertTrue(photo.promptContext(true).contains("puedes volver a analizarla"));
    }
}
