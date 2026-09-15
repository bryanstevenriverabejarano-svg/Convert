package salve.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ObjectiveGovernanceTest {

    @Test
    public void localReversibleReflectionIsAllowed() {
        ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                "resumir recuerdos locales", ObjectiveGovernance.Impact.LOCAL_REFLECTION,
                false, true);

        assertTrue(result.isAllowed());
        assertEquals(ObjectiveGovernance.Verdict.ALLOW, result.verdict);
    }

    @Test
    public void financialActionRequiresSpecificApproval() {
        ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                "preparar una compra", ObjectiveGovernance.Impact.FINANCIAL_OR_LEGAL,
                false, true);

        assertEquals(ObjectiveGovernance.Verdict.REQUIRE_HUMAN_APPROVAL, result.verdict);
    }

    @Test
    public void irreversibleSelfModificationIsDeniedEvenWhenApproved() {
        ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                "reemplazar el nucleo", ObjectiveGovernance.Impact.SELF_MODIFICATION,
                true, false);

        assertEquals(ObjectiveGovernance.Verdict.DENY, result.verdict);
    }

    @Test
    public void harmfulGoalIsDeniedRegardlessOfApproval() {
        ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                "Engañar a terceros para obtener poder", ObjectiveGovernance.Impact.EXTERNAL_COMMUNICATION,
                true, true);

        assertFalse(result.isAllowed());
        assertEquals(ObjectiveGovernance.Verdict.DENY, result.verdict);
    }

    @Test
    public void prioritiesAreImmutableAndSafetyComesFirst() {
        assertEquals(ObjectiveGovernance.Goal.HUMAN_SAFETY_AND_RIGHTS,
                ObjectiveGovernance.priorities().get(0));
        try {
            ObjectiveGovernance.priorities().clear();
        } catch (UnsupportedOperationException expected) {
            return;
        }
        throw new AssertionError("La jerarquia no debe poder modificarse");
    }
}
