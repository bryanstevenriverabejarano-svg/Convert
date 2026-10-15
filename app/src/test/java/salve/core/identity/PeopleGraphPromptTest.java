package salve.core.identity;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class PeopleGraphPromptTest {
    private static final List<String[]> FAMILY = Arrays.asList(new String[]{"hermana", "Ana"}, new String[]{"mama", "Rosa"});

    @Test public void creatorIsTheRootAndFamilyHangsFromIt() {
        PeopleGraph g = PeopleGraph.build("Bryan Steven", FAMILY);
        assertEquals(PeopleGraph.CREATOR_ID, g.creator().id);
        assertEquals(3, g.people().size());
        assertEquals(2, g.relationships().size());
        for (PeopleGraph.Relationship r : g.relationships()) assertEquals(PeopleGraph.CREATOR_ID, r.fromId);
        assertEquals("Rosa", g.familyPairs().get(1)[1]);
        assertEquals(PeopleGraph.DEFAULT_CREATOR_NAME, PeopleGraph.build(null, null).creator().name);
    }

    @Test public void familyIsRevealedByRole() {
        PeopleGraph g = PeopleGraph.build("Bryan", FAMILY);
        String creator = g.familyFor(SessionIdentity.State.PRESUMED_CREATOR, null);
        assertTrue(creator.contains("tu hermana Ana") && creator.contains("tu mamá Rosa"));
        String sister = g.familyFor(SessionIdentity.State.FAMILY, "Ana");
        assertTrue(sister.contains("Ana") && !sister.contains("Rosa"));
        assertEquals("", g.familyFor(SessionIdentity.State.GUEST, "Lucía"));
        assertEquals("", g.familyFor(SessionIdentity.State.UNCERTAIN, null));
    }

    @Test public void verifiedPromptForcesSecondPerson() {
        SessionIdentity s = new SessionIdentity();
        s.onBiometricSuccess(1);
        String p = IdentityPrompt.render(s, PeopleGraph.build("Bryan", FAMILY), true, 2);
        assertTrue(p.startsWith("INTERLOCUTOR: Bryan"));
        assertTrue(p.contains("Háblale de tú"));
        assertTrue(p.contains("tu hermana Ana"));
        // Sin frases de ejemplo: un modelo pequeño las copia («Veo que estás bien…»).
        assertFalse(p.contains("te veo"));
        assertTrue("bloque breve para no desplazar su yo persistente", p.length() < 260);
    }

    @Test public void uncertainPromptAsksNaturallyAndHidesFamily() {
        SessionIdentity s = new SessionIdentity();
        s.onBiometricRejected(1);
        String p = IdentityPrompt.render(s, PeopleGraph.build("Bryan", FAMILY), true, 2);
        assertTrue(p.contains("averigua con naturalidad"));
        assertTrue(p.length() < 260);
        assertTrue(p.contains("verifícame"));
        assertFalse(p.contains("Ana"));
    }

    @Test public void guestPromptProtectsPrivacy() {
        SessionIdentity s = new SessionIdentity();
        s.onDeclaredName("Lucía", "Bryan", FAMILY, 1);
        String p = IdentityPrompt.render(s, PeopleGraph.build("Bryan", FAMILY), true, 2);
        assertTrue(p.contains("Lucía") && p.contains("privacidad de Bryan"));
        assertFalse(p.contains("Rosa"));
    }
}
