package salve.core.conversation;

/** Tipo de intervención conversacional, independiente de las acciones del sistema. */
public enum ConversationAct {
    QUESTION,
    COMMAND,
    OPINION,
    EXPLANATION_REQUEST,
    CORRECTION,
    CASUAL,
    STATEMENT
}
