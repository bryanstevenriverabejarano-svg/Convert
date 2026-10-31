package salve.core.autonomy;

import static org.junit.Assert.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Random;
import org.junit.Test;
import salve.data.sync.SkillCloudArchive;

/** New independent, sealed-sequence evidence for the original verified skill lab. */
public class InductiveRotationSkillTest {
    private static final class Memory implements InductiveSkillLab.Store {
        String json;
        boolean reject;
        @Override public String read() { return json; }
        @Override public void write(String value) throws Exception {
            if (reject) throw new java.io.IOException("Write blocked");
            json = value;
        }
    }

    private static JsonArray vector(int... values) {
        JsonArray out = new JsonArray();
        for (int n : values) out.add(n);
        return out;
    }

    @Test public void frozenRotationTransfersTo96UnseenInputsAndValidatedPCloudCopy() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.ROTATE_LEFT;
        InductiveSkillProposal.Evaluation proposal =
                InductiveSkillProposal.evaluate(lab, kind, () -> false);
        assertEquals(InductiveSkillLab.MAX_CANDIDATES, proposal.candidatesChecked);
        assertEquals(3615, proposal.candidatesChecked);
        assertTrue(proposal.promotable());
        assertEquals(96, proposal.candidateSuccesses);
        assertEquals(1, proposal.program.size());
        assertEquals("rotate_left", proposal.program.get(0).getAsString());
        assertNull("Preview is never installation", memory.json);

        JsonArray forged = vector(1); // not an executable skill program
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillProposal.promote(lab, kind, forged, () -> false));
        assertNull(memory.json);
        JsonObject result = InductiveSkillProposal.promote(lab, kind,
                proposal.program.deepCopy(), () -> false);
        assertTrue(result.get("certified").getAsBoolean());
        assertNotNull(memory.json);
        JsonObject saved = com.google.gson.JsonParser.parseString(memory.json).getAsJsonObject();
        assertFalse(saved.has("train"));
        JsonObject programOnly = saved.getAsJsonObject("skills").getAsJsonObject("auto_rotate_left");
        assertEquals(3, programOnly.size()); // program + train/holdout counts, no input/output examples
        assertTrue(programOnly.has("program"));
        assertTrue(programOnly.has("training"));
        assertTrue(programOnly.has("holdout"));

        InductiveSkillLab restored = new InductiveSkillLab(memory);
        Random novel = new Random(0x5A115E);
        for (int i = 0; i < 96; i++) {
            int len = novel.nextInt(25);
            int[] numbers = new int[len];
            JsonArray expected = new JsonArray();
            for (int k = 0; k < len; k++) numbers[k] = novel.nextInt(20001) - 10000;
            for (int k = 1; k < len; k++) expected.add(numbers[k]);
            if (len > 0) expected.add(numbers[0]);
            assertEquals("Independent new input " + i, expected,
                    restored.apply("auto_rotate_left", vector(numbers)));
        }
        String packed = SkillCloudArchive.pack(SkillCloudArchive.INDUCTIVE, memory.json, 1234567L);
        SkillCloudArchive.Snapshot copy = SkillCloudArchive.unpack(
                packed, SkillCloudArchive.unpack(packed, null).remotePath());
        assertEquals(memory.json, copy.payload);
        assertNotNull(new InductiveSkillLab(new InductiveSkillLab.Store() {
            public String read() { return copy.payload; }
            public void write(String ignored) { throw new AssertionError("Read-only copy"); }
        }).catalog());
    }

    @Test public void rotateEmptySingletonAndLegacyProgramsWithoutScripts() throws Exception {
        JsonArray rotate = new JsonArray(); rotate.add("rotate_left");
        assertEquals("[]", InductiveSkillLab.simulate(rotate, vector()).toString());
        assertEquals("[7]", InductiveSkillLab.simulate(rotate, vector(7)).toString());
        assertEquals("[2,3,1]", InductiveSkillLab.simulate(rotate, vector(1,2,3)).toString());
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(rotate, vector(10001)));
        JsonArray unsafe = rotate.deepCopy(); unsafe.add("shell");
        assertThrows(IllegalArgumentException.class,
                () -> InductiveSkillLab.simulate(unsafe, vector(1,2)));
        Memory previous = new Memory();
        previous.json = new StringBuilder()
                .append("{").append("\"schema\":1,\"skills\":{")
                .append("\"old\":{\"program\":[\"prefix_sum\"],\"training\":3,\"holdout\":3}")
                .append("}}").toString();
        assertEquals("[1,3,6]", new InductiveSkillLab(previous)
                .apply("old", vector(1,2,3)).toString());
    }

    @Test public void cancellationAndFailedPersistenceDoNotPromote() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        InductiveSkillProposal.Kind kind = InductiveSkillProposal.Kind.ROTATE_LEFT;
        assertThrows(IllegalStateException.class,
                () -> InductiveSkillProposal.evaluate(lab, kind, () -> true));
        assertNull(memory.json);
        InductiveSkillProposal.Evaluation candidate =
                InductiveSkillProposal.evaluate(lab, kind, () -> false);
        memory.reject = true;
        assertThrows(java.io.IOException.class,
                () -> InductiveSkillProposal.promote(lab, kind, candidate.program, () -> false));
        assertNull(memory.json);
    }
}
