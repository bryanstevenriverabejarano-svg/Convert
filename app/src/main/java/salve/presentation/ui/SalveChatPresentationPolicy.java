package salve.presentation.ui;

/** Reglas sin estado para no saturar la conversación con acciones introductorias. */
final class SalveChatPresentationPolicy {
    private SalveChatPresentationPolicy() { }

    /** Las sugerencias solo aparecen antes de que empiece el primer intercambio real. */
    static boolean showSuggestions(int completedMessages, boolean isStreamingReply) {
        return completedMessages <= 0 && !isStreamingReply;
    }
}
