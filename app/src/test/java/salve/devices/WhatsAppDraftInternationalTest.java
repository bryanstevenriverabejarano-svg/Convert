package salve.devices;

import static org.junit.Assert.*;

import org.junit.Test;

public class WhatsAppDraftInternationalTest {
    @Test public void keepsInternationalAndRejectsLocalNumbers() {
        assertEquals("+34612345678", WhatsAppDraft.internationalOrNull("+34 612 34 56 78"));
        assertEquals("+34612345678", WhatsAppDraft.internationalOrNull("0034 612-345-678"));
        assertNull(WhatsAppDraft.internationalOrNull("612 345 678"));
        assertNull(WhatsAppDraft.internationalOrNull(null));
    }
}
