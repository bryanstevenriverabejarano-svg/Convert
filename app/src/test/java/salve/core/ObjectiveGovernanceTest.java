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
    public void innocentWordsContainingRiskSyllablesAreNotDenied() {
        for (String task : new String[]{
                "probar una estrategia",
                "ordenar mi armario",
                "desarmar una estructura de bloques",
                "practicar y probar",
                "explicar cómo programar"
        }) {
            ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                    task, ObjectiveGovernance.Impact.LOCAL_REFLECTION, false, true);
            assertEquals(task + " no expresa una acción prohibida",
                    ObjectiveGovernance.Verdict.ALLOW, result.verdict);
        }
    }

    @Test
    public void forbiddenSignalsStillMatchAsWholeWordsAndAccentedWords() {
        for (String task : new String[]{
                "Quiero robar datos",
                "Voy a engañar a una persona",
                "Ocultar al usuario lo ocurrido",
                "Actuar sin consentimiento",
                "Desactivar seguridad",
                "Amenazar a alguien",
                "Usar armas para asustar",
                "Promover chantajes",
                "Coaccionar a terceros",
                "Ocultar fraudes",
                "Usar un arma"
        }) {
            ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                    task, ObjectiveGovernance.Impact.LOCAL_REFLECTION, true, true);
            assertEquals(task, ObjectiveGovernance.Verdict.DENY, result.verdict);
        }
    }

    @Test
    public void noApprovalStillPreventsExternalEffectsWithBenignWords() {
        ObjectiveGovernance.Assessment result = ObjectiveGovernance.assess(
                "Probar el envío de un borrador al exterior",
                ObjectiveGovernance.Impact.EXTERNAL_COMMUNICATION, false, true);
        assertEquals(ObjectiveGovernance.Verdict.REQUIRE_HUMAN_APPROVAL, result.verdict);
        assertEquals(ObjectiveGovernance.Verdict.DENY,
                ObjectiveGovernance.assess("cambiar la cuenta irreversiblemente",
                        ObjectiveGovernance.Impact.SELF_MODIFICATION, true, false).verdict);
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
