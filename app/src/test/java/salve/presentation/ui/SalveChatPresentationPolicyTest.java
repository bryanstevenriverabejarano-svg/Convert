package salve.presentation.ui;

import org.junit.Test;
import static org.junit.Assert.*;

public class SalveChatPresentationPolicyTest {
    @Test public void showsSuggestionsOnlyBeforeConversation() {
        assertTrue(SalveChatPresentationPolicy.showSuggestions(0, false));
        assertFalse(SalveChatPresentationPolicy.showSuggestions(1, false));
        assertFalse(SalveChatPresentationPolicy.showSuggestions(2, false));
        assertFalse(SalveChatPresentationPolicy.showSuggestions(20, false));
    }

    @Test public void hidesSuggestionsDuringStreamingAndNeverReturnsThemByAccident() {
        assertFalse(SalveChatPresentationPolicy.showSuggestions(0, true));
        assertFalse(SalveChatPresentationPolicy.showSuggestions(1, true));
        assertFalse(SalveChatPresentationPolicy.showSuggestions(20, true));
    }
}
