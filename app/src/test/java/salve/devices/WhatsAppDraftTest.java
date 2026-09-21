package salve.devices;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import java.net.URI;
import java.net.URLDecoder;

public class WhatsAppDraftTest {
    @Test public void preservesExactMessageAndInternationalDestination() throws Exception {
        String message = " ¡Hola, Ana! + café & té?\nLínea 2 🙂 ";
        WhatsAppDraft draft = WhatsAppDraft.create("+34 (612) 345-678", message);
        assertEquals("+34612345678", draft.getPhone());
        assertEquals(message, draft.getMessage());
        URI uri = URI.create(draft.getUrl());
        assertEquals("https", uri.getScheme());
        assertEquals("wa.me", uri.getHost());
        assertEquals("/34612345678", uri.getPath());
        assertEquals(message, URLDecoder.decode(uri.getRawQuery().substring(5), "UTF-8"));
        assertFalse(draft.getUrl().contains("&té"));
    }

    @Test(expected = IllegalArgumentException.class) public void doesNotGuessCountryCode() {
        WhatsAppDraft.create("612345678", "Hola");
    }

    @Test(expected = IllegalArgumentException.class) public void doesNotResolveAmbiguousContactNames() {
        WhatsAppDraft.create("Ana", "Hola");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsInjectedDestinationParameters() {
        WhatsAppDraft.create("+34612345678?text=otro", "Hola");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsBlankMessage() {
        WhatsAppDraft.create("+34612345678", " \n ");
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsOversizeWithoutTruncating() {
        WhatsAppDraft.create("+34612345678", "a".repeat(4001));
    }

    @Test public void acceptsMaximumLengthWithoutAlteringPayload() {
        String message = "a".repeat(4000);
        assertEquals(message, WhatsAppDraft.create("+34612345678", message).getMessage());
        assertTrue(WhatsAppDraft.create("+34612345678", "a&b").getUrl().endsWith("a%26b"));
    }
}
