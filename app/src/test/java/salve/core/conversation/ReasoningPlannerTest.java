package salve.core.conversation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ReasoningPlannerTest {
    @Test
    public void casualTurnAvoidsUnnecessaryMemoryAndVerification() {
        ConversationAnalysis analysis = new ConversationAnalysis(ConversationAct.CASUAL, false);

        ReasoningPlan plan = ReasoningPlanner.plan("Hola", analysis, false);

        assertFalse(plan.shouldRetrieveLongTermMemory());
        assertFalse(plan.isVerificationRequired());
        assertFalse(plan.isExternalKnowledgeRequested());
    }

    @Test
    public void userMemoryCueRetrievesLongTermMemory() {
        ConversationAnalysis analysis = new ConversationAnalysis(ConversationAct.QUESTION, false);

        ReasoningPlan plan = ReasoningPlanner.plan("¿Recuerdas mi preferencia?", analysis, false);

        assertTrue(plan.shouldRetrieveLongTermMemory());
        assertTrue(plan.isVerificationRequired());
    }

    @Test
    public void correctionRetrievesContextAndRequiresVerification() {
        ConversationAnalysis analysis = new ConversationAnalysis(ConversationAct.CORRECTION, false);

        ReasoningPlan plan = ReasoningPlanner.plan("Eso no es correcto", analysis, false);

        assertTrue(plan.shouldRetrieveLongTermMemory());
        assertTrue(plan.isVerificationRequired());
    }

    @Test
    public void onlyExplicitSearchRequestsExternalKnowledge() {
        ConversationAnalysis analysis = new ConversationAnalysis(ConversationAct.EXPLANATION_REQUEST, false);

        ReasoningPlan local = ReasoningPlanner.plan("¿Cómo se hace?", analysis, false);
        ReasoningPlan external = ReasoningPlanner.plan("Busca cómo se hace", analysis, true);

        assertFalse(local.isExternalKnowledgeRequested());
        assertTrue(external.isExternalKnowledgeRequested());
    }
    @Test public void chronologyAndProfileQuestionsRetrieveWithoutMagicWord() {
        for (String question : new String[]{"¿Cuál fue tu primer recuerdo?", "¿Dónde vivo?",
                "¿Cuál es mi ubicación?", "¿Cómo se relaciona mi proyecto con ese nodo?"}) {
            assertTrue(question, ReasoningPlanner.plan(question,
                    ConversationRequestAnalyzer.analyze(question, true), false)
                    .shouldRetrieveLongTermMemory());
        }
    }

    @Test public void naturalQuestionWithoutQuestionMarksRetrievesMemory() {
        String question = "donde vivo";
        assertTrue(ReasoningPlanner.plan(question,
                ConversationRequestAnalyzer.analyze(question, true), false)
                .shouldRetrieveLongTermMemory());
    }

}
