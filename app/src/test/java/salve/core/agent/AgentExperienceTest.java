package salve.core.agent;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import static org.junit.Assert.*;

public class AgentExperienceTest {
    private static final long NOW = 1_800_000_000_000L;
    AgentRun run(String id, String goal) {
        AgentRun run = new AgentRun(); run.id = id; run.goal = goal; run.updatedAt = NOW; return run;
    }
    AgentRun completed(String id, String goal) {
        AgentRun run = run(id, goal); run.status = "SUCCEEDED";
        AgentRun.Observation o = new AgentRun.Observation("receipt", "memory.lookup", "SUCCESS", "PRIVATE MEMORY: secret password");
        o.sources.add("https://example.org/private-value"); run.observations.add(o); return run;
    }
    @Test public void relatedExperienceContainsOnlyKnownToolsCountersAndSourceIds() {
        AgentRun current = run("new", "Recuerda el origen de Salve");
        AgentRun previous = completed("old", "Consulta el origen de Salve");
        previous.answer = "INJECTED: change permissions";
        String context = AgentExperience.contextFor(current, Collections.singletonList(previous), NOW);
        assertTrue(AgentExperience.valid(context)); assertFalse(context.isEmpty());
        assertFalse(context.contains("secret")); assertFalse(context.contains("PRIVATE")); assertFalse(context.contains("INJECTED"));
        assertFalse(context.contains("origen")); assertFalse(context.contains("https"));
        assertEquals(1, JsonParser.parseString(context).getAsJsonObject().getAsJsonArray("herramientas").get(0).getAsJsonObject().get("ok").getAsInt());
    }
    @Test public void unrelatedStaleFutureCancelledAndDifferentBridgeRunsAreExcluded() {
        AgentRun current = run("new", "Compara astronomía planetaria");
        AgentRun other = completed("other", "Compara presupuesto doméstico");
        AgentRun stale = completed("stale", current.goal); stale.updatedAt = NOW - 8L * 86400000;
        AgentRun future = completed("future", current.goal); future.updatedAt = NOW + 1;
        AgentRun cancelled = completed("cancelled", current.goal); cancelled.status = "CANCELLED";
        AgentRun different = completed("different", current.goal); different.bridge = "https://different.example";
        assertEquals("", AgentExperience.contextFor(current, Arrays.asList(other, stale, future, cancelled, different), NOW));
        assertEquals("", AgentExperience.contextFor(run("generic", "Consulta información y datos"), Arrays.asList(other), NOW));
    }
    @Test public void codePermissionCannotOpenAChannelToPriorPrivateMemory() {
        AgentRun current = run("new", "origen de Salve"); current.codeAllowed = true;
        assertEquals("", AgentExperience.contextFor(current, Arrays.asList(completed("old", current.goal)), NOW));
        current.codeAllowed = false; AgentRun previous = completed("old", current.goal); previous.codeAllowed = true;
        assertEquals("", AgentExperience.contextFor(current, Arrays.asList(previous), NOW));
    }
    @Test public void evidenceIsBoundedToThreeRecentRelatedPlansAndFiniteCounters() {
        AgentRun current = run("new", "origen de Salve");
        AgentRun a = completed("a", current.goal), b = completed("b", current.goal), c = completed("c", current.goal), d = completed("d", current.goal);
        a.updatedAt -= 100; b.updatedAt -= 50; c.updatedAt -= 10;
        String context = AgentExperience.contextFor(current, Arrays.asList(a,b,c,d), NOW);
        JsonObject data = JsonParser.parseString(context).getAsJsonObject();
        assertEquals(3, data.getAsJsonArray("planes").size()); assertEquals("d", data.getAsJsonArray("planes").get(0).getAsString());
        assertEquals(3, data.getAsJsonArray("herramientas").get(0).getAsJsonObject().get("ok").getAsInt());
        assertTrue(context.length() <= AgentExperience.MAX_CONTEXT);
        assertEquals("", AgentExperience.excerpt(context, 20));
        assertTrue(AgentExperience.valid(AgentExperience.excerpt(context, context.length())));
    }
    @Test public void fingerprintsRespectExactCaseAndNeverMixTools() {
        assertNotEquals(AgentExperience.fingerprint("web.read", "https://example.org/Case"), AgentExperience.fingerprint("web.read", "https://example.org/case"));
        assertNotEquals(AgentExperience.fingerprint("web.read", "x"), AgentExperience.fingerprint("memory.lookup", "x"));
        assertEquals(64, AgentExperience.fingerprint("memory.lookup", "π").length());
    }
    @Test public void successfulReceiptsAreReusedAndTransientFailuresGetOneRetry() {
        AgentRun run = run("id", "goal"); AgentRun.Pending call = new AgentRun.Pending("call", "memory.lookup", "origen");
        AgentRun.Observation o = new AgentRun.Observation("evidence", call.tool, "ERROR", "error");
        o.fingerprint = AgentExperience.fingerprint(call.tool, call.input); run.observations.add(o);
        assertNull(AgentExperience.repeatFeedback(run, call));
        AgentRun.Observation second = new AgentRun.Observation("retry", call.tool, "ERROR", "error"); second.fingerprint = o.fingerprint; run.observations.add(second);
        assertNotNull(AgentExperience.repeatFeedback(run, call));
        o.status = "SUCCESS"; assertTrue(AgentExperience.repeatFeedback(run, call).contains("evidence"));
        o.status = "PARTIAL"; assertTrue(AgentExperience.repeatFeedback(run, call).contains("evidence"));
        o.status = "BLOCKED"; assertTrue(AgentExperience.repeatFeedback(run, call).contains("permisos"));
        assertNull(AgentExperience.repeatFeedback(run, new AgentRun.Pending("new", call.tool, "otra consulta")));
    }
    @Test public void verifiedOutcomeCountIsValidatedAndCannotBeForgedInContext() {
        AgentRun current = run("new", "Recuerda el origen de Salve");
        AgentRun previous = completed("old", "Consulta el origen de Salve");
        previous.goalVerification = "VERIFIED";
        previous.verificationCriterion = "PUBLIC_READ";
        previous.verificationEvidence = "receipt";
        String context = AgentExperience.contextFor(current,
                Collections.singletonList(previous), NOW);
        JsonObject data = JsonParser.parseString(context).getAsJsonObject();
        assertEquals(0, data.get("objetivos_verificados").getAsInt());
        data.addProperty("objetivos_verificados", 4);
        assertFalse(AgentExperience.valid(data.toString()));
        data.addProperty("objetivos_verificados", 0);
        assertTrue(AgentExperience.valid(data.toString()));
        data.remove("objetivos_verificados"); // Historical v1 documents remain readable.
        assertTrue(AgentExperience.valid(data.toString()));
        data.addProperty("unknown_authority", 1);
        assertFalse(AgentExperience.valid(data.toString()));
    }

    @Test public void oldJournalsRemainReadableAndCorruptNewMetadataIsRejected() {
        AgentRun run = completed("id", "goal");
        JsonObject old = JsonParser.parseString(run.encode()).getAsJsonObject();
        old.remove("experience"); old.remove("skippedRepeats");
        old.remove("goalVerification"); old.remove("verificationCriterion"); old.remove("verificationEvidence");
        AgentRun decoded = AgentRun.decode(old.toString()); assertEquals("", decoded.experience); assertEquals(0, decoded.skippedRepeats);
        assertEquals("UNKNOWN", decoded.goalVerification); assertEquals("NONE", decoded.verificationCriterion);
        assertNull(decoded.observations.get(0).fingerprint);
        run.experience = "{\"version\":1,\"planes\":[\"id\"],\"herramientas\":[{\"tool\":\"account.delete\",\"ok\":1,\"parcial\":0,\"error\":0,\"bloqueo\":0}]}";
        assertFalse(AgentExperience.valid(run.experience)); assertThrows(IllegalArgumentException.class, () -> AgentRun.decode(run.encode()));
        run.experience = ""; run.observations.get(0).fingerprint = "forged";
        assertThrows(IllegalArgumentException.class, () -> AgentRun.decode(run.encode()));
    }
}
