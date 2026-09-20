package salve.core.conversation;

/**
 * Plan observable de un turno. Contiene decisiones de control, no cadenas de
 * pensamiento ni texto privado generado por un modelo.
 */
public final class ReasoningPlan {
    private final boolean retrieveLongTermMemory;
    private final boolean externalKnowledgeRequested;
    private final boolean verificationRequired;
    private final String responseDirective;

    ReasoningPlan(boolean retrieveLongTermMemory,
                  boolean externalKnowledgeRequested,
                  boolean verificationRequired,
                  String responseDirective) {
        this.retrieveLongTermMemory = retrieveLongTermMemory;
        this.externalKnowledgeRequested = externalKnowledgeRequested;
        this.verificationRequired = verificationRequired;
        this.responseDirective = responseDirective;
    }

    public boolean shouldRetrieveLongTermMemory() {
        return retrieveLongTermMemory;
    }

    public boolean isExternalKnowledgeRequested() {
        return externalKnowledgeRequested;
    }

    public boolean isVerificationRequired() {
        return verificationRequired;
    }

    public String toPromptContext() {
        return "MEMORIA_LARGO_PLAZO=" + retrieveLongTermMemory
                + " | CONOCIMIENTO_EXTERNO=" + externalKnowledgeRequested
                + " | VERIFICACION=" + verificationRequired
                + " | DIRECTRIZ=" + responseDirective;
    }
}
