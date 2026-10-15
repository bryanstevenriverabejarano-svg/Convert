package salve.core.autonomy;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;

import java.util.Random;

import org.junit.Test;

public class PlayCycleTest {
    @Test public void inventedChallengesAreValidAndVerifiedByTheRealLab() {
        AutonomousToolLab lab = new AutonomousToolLab();
        Random random = new Random(7);
        for (String family : ToolRegistry.families()) {
            for (int level = 1; level <= PlayCycle.MAX_LEVEL; level++) {
                JsonObject challenge = PlayCycle.invent(family, level, random, "juego-" + family + "-" + level);
                JsonObject report = lab.solve(challenge, () -> false);
                assertTrue(family + " nivel " + level + ": " + report.get("status"), report.get("verified").getAsBoolean());
            }
        }
    }

    @Test public void levelsUpAfterThreeSolvesAndDownAfterAFailure() {
        PlayCycle.State state = new PlayCycle.State();
        JsonObject ok = new JsonObject(); ok.addProperty("verified", true);
        JsonObject bad = new JsonObject(); bad.addProperty("verified", false);
        PlayCycle.record(state, "route", ok, 1);
        PlayCycle.record(state, "route", ok, 2);
        PlayCycle.Outcome third = PlayCycle.record(state, "route", ok, 3);
        assertTrue(third.leveledUp);
        assertEquals(2, state.of("route").level);
        assertNotNull(PlayCycle.reflection("route", third));
        PlayCycle.Outcome fail = PlayCycle.record(state, "route", bad, 4);
        assertTrue(fail.leveledDown);
        assertEquals(1, state.of("route").level);
    }

    @Test public void choosesWhereSheIsLearningOrHasExploredLeast() {
        PlayCycle.State state = new PlayCycle.State();
        for (String family : ToolRegistry.families()) { state.of(family).played = 10; state.of(family).recent.addAll(java.util.Arrays.asList(true, true, true, true)); }
        state.of("schedule").recent.clear();
        state.of("schedule").recent.addAll(java.util.Arrays.asList(false, false, true, true));
        assertEquals("schedule", PlayCycle.choose(state, new Random(1)));
        state.of("dependencies").played = 0;
        state.of("dependencies").recent.clear();
        assertEquals("schedule", PlayCycle.choose(state, new Random(1)));
    }
}
