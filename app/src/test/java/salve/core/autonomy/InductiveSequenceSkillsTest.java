package salve.core.autonomy;

import static org.junit.Assert.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Random;
import org.junit.Test;
import salve.data.sync.SkillCloudArchive;

/** Independent reference checks on genuinely new input sequences, never used in synthesis. */
public class InductiveSequenceSkillsTest {
    static final class Memory implements InductiveSkillLab.Store {
        String json;
        public String read() { return json; }
        public void write(String state) { json = state; }
    }

    private static JsonArray input(int... values) {
        JsonArray out = new JsonArray();
        for (int n : values) out.add(n);
        return out;
    }

    private static JsonArray independentOracle(InductiveSkillProposal.Kind kind, int[] values) {
        JsonArray expected = new JsonArray();
        if (kind == InductiveSkillProposal.Kind.RUNNING_TOTAL) {
            // Independent straight-line reference; not the DSL implementation.
            int running = 0;
            for (int value : values) {
                running = Math.addExact(running, value);
                expected.add(running);
            }
        } else if (kind == InductiveSkillProposal.Kind.ADJACENT_DELTA) {
            for (int i = 1; i < values.length; i++)
                expected.add(Math.subtractExact(values[i], values[i - 1]));
        } else {
            throw new IllegalArgumentException("Expected a new sequence family");
        }
        return expected;
    }

    @Test public void twoNewFamiliesRequireConsentTransferAndSurviveLocalAndCloudReload() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        for (InductiveSkillProposal.Kind kind : new InductiveSkillProposal.Kind[]{
                InductiveSkillProposal.Kind.RUNNING_TOTAL,
                InductiveSkillProposal.Kind.ADJACENT_DELTA}) {
            InductiveSkillProposal.Evaluation proposal =
                    InductiveSkillProposal.evaluate(lab, kind, () -> false);
            assertTrue(proposal.promotable());
            assertEquals(96, proposal.candidateSuccesses);
            assertEquals(1884, proposal.candidatesChecked);
            assertEquals(1, proposal.program.size());
            String expectedOp = kind == InductiveSkillProposal.Kind.RUNNING_TOTAL
                    ? "running_total" : "adjacent_delta";
            assertEquals(expectedOp, proposal.program.get(0).getAsString());
            assertNull("Preview must not install anything on disk", memory.json);

            JsonObject approved = InductiveSkillProposal.promote(lab, kind,
                    proposal.program.deepCopy(), () -> false);
            assertTrue(approved.get("certified").getAsBoolean());
            assertNotNull(memory.json);
            assertFalse("Never save train/holdout inputs", memory.json.contains("\"in\""));

            InductiveSkillLab afterRestart = new InductiveSkillLab(memory);
            Random novel = new Random(133_713L + kind.ordinal());
            for (int caseNo = 0; caseNo < 96; caseNo++) {
                int[] unseen = new int[1 + novel.nextInt(22)];
                for (int i = 0; i < unseen.length; i++)
                    unseen[i] = novel.nextInt(201) - 100;
                assertEquals("Independent case " + caseNo + " " + kind,
                        independentOracle(kind, unseen),
                        afterRestart.apply(InductiveSkillProposal.skillName(kind), input(unseen)));
            }

            // The pCloud transport reuses the existing registry decoder, no arbitrary code.
            String packed = SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE,
                    memory.json, 1_234_567L + kind.ordinal());
            SkillCloudArchive.Snapshot verified =
                    SkillCloudArchive.unpack(packed, SkillCloudArchive.unpack(packed, null).remotePath());
            assertEquals(memory.json, verified.payload);
            InductiveSkillLab fromCloud = new InductiveSkillLab(new InductiveSkillLab.Store() {
                public String read() { return verified.payload; }
                public void write(String ignored) { throw new AssertionError("Cloud read-only"); }
            });
            assertEquals(independentOracle(kind, new int[]{3, -4, 7, 2}),
                    fromCloud.apply(InductiveSkillProposal.skillName(kind),
                            input(3, -4, 7, 2)));
        }
    }

    @Test public void negativeHoldoutAndChangedCandidateCannotPromote() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        JsonObject request = InductiveSkillProposal.request(InductiveSkillProposal.Kind.ADJACENT_DELTA);
        request.getAsJsonArray("holdout").get(0).getAsJsonObject().add("out", input(777));
        JsonObject failed = lab.learn(request, () -> false);
        assertEquals("HOLDOUT_FAILED", failed.get("status").getAsString());
        assertNull(memory.json);
        InductiveSkillProposal.Evaluation candidate =
                InductiveSkillProposal.evaluate(lab, InductiveSkillProposal.Kind.RUNNING_TOTAL, () -> false);
        JsonArray forged = new JsonArray(); forged.add("sort");
        assertThrows(IllegalArgumentException.class, () -> InductiveSkillProposal.promote(
                lab, InductiveSkillProposal.Kind.RUNNING_TOTAL, forged, () -> false));
        assertNull(memory.json);
        assertTrue(candidate.promotable());
    }

    @Test public void rejectsOverflowsAndUnknownCodeWhileKeepingLegacyOperations() {
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(JsonParser.parseString("[\"running_total\"]"),
                        input(10000, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(JsonParser.parseString("[\"adjacent_delta\"]"),
                        input(10000, -10000)));
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(JsonParser.parseString("[\"javascript\"]"),
                        input(1, 2)));
        assertEquals(input(1, 3, 6),
                InductiveSkillLab.simulate(JsonParser.parseString("[\"running_total\"]"),
                        input(1, 2, 3)));
        assertEquals(input(2, -5, 4),
                InductiveSkillLab.simulate(JsonParser.parseString("[\"adjacent_delta\"]"),
                        input(1, 3, -2, 2)));
        assertEquals(input(-3, 1),
                InductiveSkillLab.simulate(JsonParser.parseString("[\"unique\",\"sort\"]"),
                        input(1, -3, -3, 1)));
    }

    @Test public void cancelStopsWithoutPersistingNewFamily() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        final int[] checks = {0};
        assertThrows(IllegalStateException.class,
                () -> InductiveSkillProposal.evaluate(lab, InductiveSkillProposal.Kind.RUNNING_TOTAL,
                        () -> ++checks[0] > 15));
        assertNull(memory.json);
    }
}
