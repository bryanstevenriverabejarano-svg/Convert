package salve.core.selfpr;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Un cambio suyo no es una mejora hasta que el examen lo dice. */
public class SelfVerificationTest {
    private static JsonObject change(long t, String issue, int failures) {
        return SelfLineage.entry(t, "bryan", "leer mejor los sensores", SelfChangeScope.ROOT + "sensors/DeviceSenses.java",
                "0123456789abcdef0123456789abcdef01234567", issue, failures);
    }

    @Test public void aChangeIsPendingOnlyAfterTheAppWasUpdatedAndUntilItsVerdict() {
        List<JsonObject> lineage = Arrays.asList(change(1_000, "https://github.com/o/r/issues/1", 3), change(5_000, "https://github.com/o/r/issues/2", 2));
        List<JsonObject> none = Collections.emptyList();
        assertNull("la app aún no se ha actualizado", SelfVerification.pending(lineage, none, 4_000));
        assertNull("Android no dijo cuándo se instaló", SelfVerification.pending(lineage, none, 0));
        JsonObject waiting = SelfVerification.pending(lineage, none, 9_000);
        assertEquals("solo el último cambio", "https://github.com/o/r/issues/2", waiting.get("issue").getAsString());
        JsonObject verdict = SelfVerification.verdict(waiting, 1, 10_000);
        assertNull("con veredicto ya no está pendiente", SelfVerification.pending(lineage, Collections.singletonList(verdict), 9_000));
        assertNull(SelfVerification.pending(new ArrayList<>(), none, 9_000));
        assertNull(SelfVerification.pending(null, none, 9_000));
        assertTrue(SelfVerification.pendingNotice(waiting).contains("cambio mío sin comprobar (DeviceSenses.java)"));
        assertEquals("", SelfVerification.pendingNotice(null));
    }

    @Test public void theVerdictComparesTheExamBeforeAndAfter() {
        JsonObject better = SelfVerification.verdict(change(5_000, "https://github.com/o/r/issues/2", 2), 1, 10_000);
        assertEquals("mejor", better.get("resultado").getAsString());
        assertTrue(SelfVerification.sentence(better).contains("el examen mejoró, de 2 a 1 casos fallidos. Lo doy por bueno."));

        JsonObject worse = SelfVerification.verdict(change(5_000, "https://github.com/o/r/issues/2", 2), 4, 10_000);
        assertEquals("peor", worse.get("resultado").getAsString());
        String text = SelfVerification.sentence(worse);
        assertTrue(text, text.contains("el examen empeoró, de 2 a 4 casos fallidos"));
        assertTrue(text, text.contains("recomiendo revertir su commit (lo encuentras en https://github.com/o/r/issues/2)"));
        assertTrue("no afirma una causa que no puede probar", text.contains("es un indicio, no una prueba"));

        JsonObject same = SelfVerification.verdict(change(5_000, "https://github.com/o/r/issues/2", 2), 2, 10_000);
        assertEquals("igual", same.get("resultado").getAsString());
        assertTrue(SelfVerification.sentence(same).contains("tampoco demuestra que ayudara"));
    }

    @Test public void withoutBothMeasurementsThereIsNoVerdict() {
        assertNull("no había examen al proponerlo", SelfVerification.verdict(change(5_000, "https://github.com/o/r/issues/2", -1), 1, 10_000));
        assertNull("el examen de ahora no se pudo contar", SelfVerification.verdict(change(5_000, "https://github.com/o/r/issues/2", 2), -1, 10_000));
        assertNull(SelfVerification.verdict(null, 1, 10_000));
        assertEquals("", SelfVerification.sentence(null));
    }

    @Test public void examsOfDifferentSizeAreNotCompared() {
        JsonObject change = change(5_000, "https://github.com/o/r/issues/2", 2);
        change.addProperty("casos_examen", 38);
        assertEquals("mejor", SelfVerification.verdict(change, 1, 38, 10_000).get("resultado").getAsString());
        JsonObject grown = SelfVerification.verdict(change, 4, 44, 10_000);
        assertEquals("un examen con más casos falla más sin que el cambio tenga culpa", "no_comparable", grown.get("resultado").getAsString());
        assertTrue(SelfVerification.sentence(grown).contains("no lo puedo dar"));
        // Sin saber el tamaño de alguno de los dos, se compara como antes.
        assertEquals("peor", SelfVerification.verdict(change, 4, -1, 10_000).get("resultado").getAsString());
        assertEquals(3, SelfLineage.totalCases(com.google.gson.JsonParser.parseString(
                "{\"detail\":{\"cases\":[{},{},{}]}}").getAsJsonObject()));
        assertEquals(-1, SelfLineage.totalCases(null));
    }

    @Test public void aNewAdapterIsAChangeLikeAnyOtherAndGetsItsOwnVerdict() {
        JsonObject adapter = SelfLineage.entry(5_000, SelfVerification.ADAPTER, "adaptador entrenado el 2026-11-12", "salve-lora.gguf",
                "abcdef0123456789", SelfVerification.ADAPTER + ":abcdef012345", 6);
        adapter.addProperty("casos_examen", 44);
        List<JsonObject> lineage = Collections.singletonList(adapter);
        // Ya está cargado: no espera a una actualización de la app.
        JsonObject waiting = SelfVerification.pending(lineage, Collections.emptyList(), 0);
        assertNotNull(waiting);
        assertTrue(SelfVerification.pendingNotice(waiting).contains("adaptador nuevo cargado sin comprobar"));
        JsonObject worse = SelfVerification.verdict(waiting, 9, 44, 10_000);
        assertEquals("peor", worse.get("resultado").getAsString());
        String text = SelfVerification.sentence(worse);
        assertTrue(text, text.contains("Recomiendo quitar el adaptador"));
        assertFalse("no habla de commits: no es un parche", text.contains("commit"));
        assertTrue(SelfVerification.sentence(SelfVerification.verdict(waiting, 3, 44, 10_000)).contains("el examen mejoró, de 6 a 3"));
        assertNull(SelfVerification.pending(lineage, Collections.singletonList(worse), 0));
    }
}
