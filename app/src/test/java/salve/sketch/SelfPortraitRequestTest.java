package salve.sketch;

import static org.junit.Assert.*;

import org.junit.Test;

public class SelfPortraitRequestTest {
    @Test public void drawMeAndYouTogetherIsBoth() {
        SelfPortraitRequest r = SelfPortraitRequest.of("Dibujame a mí y s ti juntos", "salve");
        assertTrue(r.salve); assertTrue(r.bryan); assertTrue(r.together());
        assertEquals("Bryan y yo", r.title());
    }

    @Test public void drawYourselfIsSalveOnly() {
        SelfPortraitRequest r = SelfPortraitRequest.of("dibújate a ti misma", "ti misma");
        assertTrue(r.salve); assertFalse(r.bryan);
    }

    @Test public void drawMeIsBryanOnly() {
        SelfPortraitRequest r = SelfPortraitRequest.of("dibújame a mí", "mi");
        assertFalse(r.salve); assertTrue(r.bryan);
    }

    @Test public void ordinaryDrawingsAreNotPortraits() {
        assertFalse(SelfPortraitRequest.of("dibújame un gato", "gato").any());
        assertFalse(SelfPortraitRequest.of("dibuja a mi madre", "madre").any());
        assertFalse(SelfPortraitRequest.of("dibuja dos gatos juntos", "gatos").any());
    }
}
