package salve.core.conversation;

/** Resultado inmutable del análisis pragmático de una entrada. */
public final class ConversationAnalysis {
    private final ConversationAct act;
    private final boolean needsClarification;

    public ConversationAnalysis(ConversationAct act, boolean needsClarification) {
        if (act == null) throw new IllegalArgumentException("act no puede ser null");
        this.act = act;
        this.needsClarification = needsClarification;
    }

    public ConversationAct getAct() {
        return act;
    }

    public boolean needsClarification() {
        return needsClarification;
    }
}
