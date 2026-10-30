package salve.core.autonomy;

import static org.junit.Assert.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Random;
import org.junit.Test;
import salve.data.sync.SkillCloudArchive;

/**
 * Independent sequence oracles and post-freeze inputs; no self-assessment
 * from the LLM and no remote code installation.
 */
public class InductiveSequenceSkillsTest {
    static final class Memory implements InductiveSkillLab.Store {
        String saved;
        boolean fail;
        @Override public String read() { return saved; }
        @Override public void write(String json) throws Exception {
            if (fail) throw new java.io.IOException("storage unavailable");
            saved = json;
        }
    }
    private static JsonArray array(int... values) {
        JsonArray out = new JsonArray();
        for (int v : values) out.add(v);
        return out;
    }
    private static JsonArray independent(InductiveSkillProposal.Kind kind, int[] input) {
        JsonArray out = new JsonArray();
        if (kind == InductiveSkillProposal.Kind.ROTATE_LEFT) {
            for (int i = 1; i < input.length; i++) out.add(input[i]);
            if (input.length > 0) out.add(input[0]);
        } else {
            long accumulated = 0;
            for (int n : input) { accumulated += n; out.add(accumulated); }
        }
        return out;
    }

    @Test public void proposesAndInstallsOnlyAfterExplicitPromotionAndSurvivesCloudValidation()
            throws Exception {
        for (InductiveSkillProposal.Kind kind : new InductiveSkillProposal.Kind[]{
                InductiveSkillProposal.Kind.ROTATE_LEFT, InductiveSkillProposal.Kind.PREFIX_SUM}) {
            Memory store = new Memory();
            InductiveSkillLab lab = new InductiveSkillLab(store);
            InductiveSkillProposal.Evaluation proposal =
                    InductiveSkillProposal.evaluate(lab, kind, () -> false);
            assertEquals(1884, proposal.candidatesChecked);
            assertEquals(96, proposal.candidateSuccesses);
            assertTrue(proposal.promotable());
            assertEquals(1, proposal.program.size());
            assertNull("Preview must not persist", store.saved);
            JsonObject installed = InductiveSkillProposal.promote(lab, kind,
                    proposal.program.deepCopy(), () -> false);
            assertTrue(installed.get("certified").getAsBoolean());
            assertNotNull(store.saved);
            assertFalse("Examples and oracle labels must not be stored", store.saved.contains("\"in\""));
            assertTrue(store.saved.length() < 1024);
            String archive = SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE,
                    store.saved, 12345L);
            assertEquals(store.saved, SkillCloudArchive.unpack(archive,
                    SkillCloudArchive.ROOT + "/inductive-12345-"
                            + SkillCloudArchive.sha256(store.saved) + ".json").payload);
            InductiveSkillLab restarted = new InductiveSkillLab(store);
            Random novel = new Random(0x506F1E + kind.ordinal());
            for (int trial = 0; trial < 96; trial++) {
                int[] sample = new int[3 + novel.nextInt(22)]; // up to 24
                for (int i = 0; i < sample.length; i++)
                    sample[i] = novel.nextInt(2001) - 1000;
                // Avoid artificial overflow in this transfer suite.
                if (kind == InductiveSkillProposal.Kind.PREFIX_SUM)
                    for (int i = 0; i < sample.length; i++) sample[i] /= 10;
                JsonArray actual = restarted.apply(InductiveSkillProposal.skillName(kind), array(sample));
                assertEquals("Unseen case " + trial, independent(kind, sample), actual);
            }
        }
    }

    @Test public void prefixSumRejectsOverflowWithoutExecutingUncheckedNumbers() {
        JsonArray op = JsonParser.parseString("[\"prefix_sum\"]").getAsJsonArray();
        assertEquals("[2,5,4]", InductiveSkillLab.simulate(op, array(2,3,-1)).toString());
        assertEquals("[1,3,6]", InductiveSkillLab.simulate(op, array(1,2,3)).toString());
        assertEquals("[]", InductiveSkillLab.simulate(op, array()).toString());
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(op, array(9000,2000)));
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(op, array(-9000,-2000)));
    }

    @Test public void rotationsOfEmptyAndSingletonSequencesAreBoundedAndNoOp() {
        JsonArray op = JsonParser.parseString("[\"rotate_left\"]").getAsJsonArray();
        assertEquals("[]", InductiveSkillLab.simulate(op, array()).toString());
        assertEquals("[7]", InductiveSkillLab.simulate(op, array(7)).toString());
        assertEquals("[2,3,1]", InductiveSkillLab.simulate(op, array(1,2,3)).toString());
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(op, array(10001)));
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(
                        JsonParser.parseString("[\"rotate_left\",\"shell\"]").getAsJsonArray(),
                        array(1)));
    }

    @Test public void tamperedFrozenProgramBadGoldAndFailedWriteNeverPromote() throws Exception {
        Memory store = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(store);
        InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.PREFIX_SUM;
        InductiveSkillProposal.Evaluation ready =
                InductiveSkillProposal.evaluate(lab, kind, () -> false);
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillProposal.promote(lab, kind,
                        JsonParser.parseString("[\"sort\"]").getAsJsonArray(), () -> false));
        assertNull(store.saved);
        store.fail = true;
        assertThrows(java.io.IOException.class,
                () -> InductiveSkillProposal.promote(lab, kind,
                        ready.program.deepCopy(), () -> false));
        assertNull(store.saved);
        assertThrows(IllegalStateException.class,
                () -> InductiveSkillProposal.evaluate(lab, kind, () -> true));
        assertNull(store.saved);
    }

    @Test public void legacySavedSequenceProgramAndOldSkillsCanLoadTogether() throws Exception {
        Memory store = new Memory();
        store.saved = "{\"schema\":1,\"skills\":{"
                + "\"legacy_odd\":{\"program\":[\"unique\",\"filter_odd\"],\"training\":3,\"holdout\":3},"
                + "\"sequence_sum\":{\"program\":[\"prefix_sum\"],\"training\":3,\"holdout\":3}}}";
        InductiveSkillLab lab = new InductiveSkillLab(store);
        assertEquals("[5,-3]", lab.apply("legacy_odd", array(5,5,2,-3,-3)).toString());
        assertEquals("[5,7,4]", lab.apply("sequence_sum", array(5,2,-3)).toString());
    }
}
