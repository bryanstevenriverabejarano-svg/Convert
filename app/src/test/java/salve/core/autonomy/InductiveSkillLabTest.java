package salve.core.autonomy;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import java.util.function.BooleanSupplier;
import static org.junit.Assert.*;

public class InductiveSkillLabTest {
    static class Memory implements InductiveSkillLab.Store {
        String value;
        boolean fail;
        @Override public String read() { return value; }
        @Override public void write(String json) throws Exception {
            if (fail) throw new java.io.IOException("no space");
            value = json;
        }
    }

    private static JsonObject obj(String text) { return JsonParser.parseString(text).getAsJsonObject(); }
    private static JsonObject request(String holdout) {
        return obj("{\"name\":\"odd_dedup\",\"train\":["
                + "{\"in\":[3,-2,3,4],\"out\":[3,4]},"
                + "{\"in\":[-1,2,2,-3],\"out\":[2]},"
                + "{\"in\":[8,-4,8,1],\"out\":[8,1]}],\"holdout\":" + holdout + "}");
    }
    private static final String HELDOUT = "["
            + "{\"in\":[5,-7,5,2],\"out\":[5,2]},"
            + "{\"in\":[-9,6,6,3],\"out\":[6,3]},"
            + "{\"in\":[0,7,7,-3],\"out\":[7]}]";

    @Test public void learnsNovelCompositionAndAppliesToUnseenInputsAfterRestart() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        JsonObject report = lab.learn(request(HELDOUT), () -> false);
        assertEquals("CERTIFIED_ON_PROVIDED_HOLDOUT", report.get("status").getAsString());
        assertTrue(report.get("certified").getAsBoolean());
        assertEquals("[\"filter_odd\",\"unique\"]", report.get("program").toString());
        assertEquals("[11,9]", lab.apply("odd_dedup", JsonParser.parseString("[11,4,11,-2,9]")).toString());
        assertFalse(memory.value.contains("[-9,6,6,3]")); // no examples in memory
        InductiveSkillLab reload = new InductiveSkillLab(memory);
        assertEquals("[11,9]", reload.apply("odd_dedup", JsonParser.parseString("[11,4,11,-2,9]")).toString());
        assertTrue(reload.catalog().contains("odd_dedup"));
    }

    @Test public void holdoutFailureNeverPromotesAndNeverAdaptsSearchToHoldout() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        JsonObject data = request(HELDOUT);
        data.getAsJsonArray("holdout").get(0).getAsJsonObject()
                .add("out", JsonParser.parseString("[999]"));
        JsonObject report = lab.learn(data, () -> false);
        assertEquals("HOLDOUT_FAILED", report.get("status").getAsString());
        assertFalse(report.get("certified").getAsBoolean());
        assertNull(memory.value);
        assertFalse(lab.catalog().contains("odd_dedup"));
    }

    @Test public void rejectsOverlapAndUntrustedProgram() throws Exception {
        InductiveSkillLab lab = new InductiveSkillLab(new Memory());
        JsonObject data = request(HELDOUT);
        data.getAsJsonArray("holdout").set(0, data.getAsJsonArray("train").get(0));
        try { lab.learn(data, () -> false); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("inéditos")); }
        try { lab.apply("not_registered", JsonParser.parseString("[1]")); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("encontrada")); }
        Memory bad = new Memory();
        bad.value = "{\"schema\":1,\"skills\":{\"evil\":{\"program\":[\"shell\"],\"training\":3,\"holdout\":3}}}";
        try { new InductiveSkillLab(bad); fail(); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("permitida")); }
    }

    @Test public void cancellationAndStorageErrorCannotClaimLearning() throws Exception {
        Memory memory = new Memory();
        InductiveSkillLab lab = new InductiveSkillLab(memory);
        try { lab.learn(request(HELDOUT), () -> true); fail(); }
        catch (IllegalStateException expected) { assertTrue(expected.getMessage().contains("cancelado")); }
        assertNull(memory.value);
        memory.fail = true;
        try { lab.learn(request(HELDOUT), () -> false); fail(); }
        catch (java.io.IOException expected) { assertEquals("no space", expected.getMessage()); }
        assertNull(memory.value);
        assertFalse(lab.catalog().contains("odd_dedup"));
    }

    @Test public void impossibleExamplesAreNotCalledLearning() throws Exception {
        InductiveSkillLab lab = new InductiveSkillLab(new Memory());
        JsonObject data = request(HELDOUT);
        data.getAsJsonArray("train").get(0).getAsJsonObject().add("out", JsonParser.parseString("[1234]"));
        assertEquals("NO_CANDIDATE", lab.learn(data, () -> false).get("status").getAsString());
    }
}
