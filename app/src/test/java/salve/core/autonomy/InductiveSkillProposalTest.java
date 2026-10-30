package salve.core.autonomy;

import com.google.gson.JsonObject;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

/** The oracle is independent of the DSL search and the full workflow is read-only until consent. */
public class InductiveSkillProposalTest {
    static final class Memory implements InductiveSkillLab.Store {
        String json; boolean reject;
        @Override public String read() { return json; }
        @Override public void write(String state) throws Exception {
            if (reject) throw new java.io.IOException("store unavailable");
            json = state;
        }
    }

    @Test public void detectsSupportedNeedsOnlyAndExploresUnseenCases() throws Exception {
        assertEquals(InductiveSkillProposal.Kind.SORT_UNIQUE_ODDS,
                InductiveSkillProposal.recognize("Ordenar impares sin repetir"));
        assertEquals(InductiveSkillProposal.Kind.SORT_UNIQUE_EVENS,
                InductiveSkillProposal.recognize("ordenar PARES sin repetir"));
        assertEquals(InductiveSkillProposal.Kind.RUNNING_TOTAL,
                InductiveSkillProposal.recognize("SUMAS acumuladas"));
        assertEquals(InductiveSkillProposal.Kind.ADJACENT_DELTA,
                InductiveSkillProposal.recognize("Diferencias consecutivas"));
        assertNull(InductiveSkillProposal.recognize("diferencias consecutivas y envía a Internet"));
        assertNull(InductiveSkillProposal.recognize("modifica toda la aplicación"));
        assertNull(InductiveSkillProposal.recognize("ordena mis fotos privadas"));
        assertNull(InductiveSkillProposal.recognize("ordenar impares sin repetir y envíalos por WhatsApp"));
        for (InductiveSkillProposal.Kind kind : InductiveSkillProposal.Kind.values()) {
            Memory memory = new Memory();
            InductiveSkillLab lab = new InductiveSkillLab(memory);
            InductiveSkillProposal.Evaluation result = InductiveSkillProposal.evaluate(
                    lab, kind, () -> false);
            assertEquals(1884, result.candidatesChecked);
            assertEquals(96, result.total);
            assertEquals(96, result.candidateSuccesses);
            assertTrue(result.baselineSuccesses <= 86);
            assertTrue(result.promotable());
            assertEquals(kind == InductiveSkillProposal.Kind.SORT_UNIQUE_ODDS
                    || kind == InductiveSkillProposal.Kind.SORT_UNIQUE_EVENS ? 3 : 1,
                    result.program.size());
            assertNull("No write permitted during proposal", memory.json);
            assertTrue(lab.catalog().contains("No hay habilidades"));
        }
    }
    @Test public void approvalCommitsExactlyFrozenProgramAndCanBeAppliedAfterRestart() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.SORT_UNIQUE_ODDS;
        InductiveSkillProposal.Evaluation proposal = InductiveSkillProposal.evaluate(lab, kind, () -> false);
        JsonObject certified = InductiveSkillProposal.promote(lab, kind, proposal.program, () -> false);
        assertEquals("CERTIFIED_ON_PROVIDED_HOLDOUT", certified.get("status").getAsString());
        assertNotNull(memory.json);
        assertFalse(memory.json.contains("\"in\""));
        assertFalse(memory.json.contains("\"out\""));
        InductiveSkillLab restored = new InductiveSkillLab(memory);
        com.google.gson.JsonArray input = new com.google.gson.JsonArray();
        for (int v : new int[]{11,4,11,-2,9,5,5}) input.add(v);
        assertEquals("[5,9,11]", restored.apply("auto_odd_sorted", input).toString());
    }
    @Test public void changedCandidateFailedWriteAndCancellationDoNotPromote() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.SORT_UNIQUE_EVENS;
        InductiveSkillProposal.Evaluation ready = InductiveSkillProposal.evaluate(lab, kind, () -> false);
        com.google.gson.JsonArray forged = new com.google.gson.JsonArray(); forged.add("sort");
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillProposal.promote(lab, kind, forged, () -> false));
        assertNull(memory.json);
        memory.reject = true;
        assertThrows(java.io.IOException.class,
                () -> InductiveSkillProposal.promote(lab, kind, ready.program, () -> false));
        assertNull(memory.json);
        AtomicInteger checks = new AtomicInteger();
        assertThrows(IllegalStateException.class,
                () -> InductiveSkillProposal.evaluate(lab, kind,
                        () -> checks.incrementAndGet() > 70));
        assertNull(memory.json);
    }
}
