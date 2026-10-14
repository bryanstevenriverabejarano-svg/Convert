package salve.core.mind;

import static org.junit.Assert.*;
import static salve.core.mind.MindTestSupport.list;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

import salve.core.cognition.CognitivePrompt;

/**
 * Contrato de continuidad de extremo a extremo con un modelo guionizado.
 *
 * <p>Comprueba la tubería, no la inteligencia: que lo consolidado un día llega al prompt de una
 * conversación una semana después con sus fuentes, que la reflexión se puede retomar, que una
 * contradicción se refleja y que olvidar un recuerdo borra lo que dependía de él. La calidad con
 * el modelo real se mide en {@code MindContinuityRealTest} (prueba instrumentada opcional).</p>
 */
public class MindContinuityTest {
    private final MindTestSupport.MemoryStore store = new MindTestSupport.MemoryStore();
    private final MindTestSupport.Clock clock = new MindTestSupport.Clock();

    @Test public void aWeekLaterSalveStillKnowsWhatItLearnedAndWhy() {
        SelfModelJournal journal = MindTestSupport.journal(store, clock);
        List<Episode> day1 = new ArrayList<>();
        day1.add(new Episode("recuerdos:21", "[declaracion_usuario] Cada lunes cuadro las facturas de la empresa", clock.get()));
        day1.add(new Episode("recuerdos:22", "[declaracion_usuario] Me agobia el cierre del trimestre", clock.get()));

        MindTestSupport.ScriptedGenerator model = new MindTestSupport.ScriptedGenerator()
                .then("{\"beliefs\":[{\"about\":\"bryan\",\"statement\":\"Bryan cuadra las facturas cada lunes\",\"confidence\":0.7,"
                        + "\"sources\":[\"recuerdos:21\"]}],\"questions\":[{\"text\":\"¿Qué parte del cierre trimestral le agobia más?\","
                        + "\"salience\":0.7,\"sources\":[\"recuerdos:22\"]}]}")
                .then("{\"thought\":\"Si el agobio llega al cierre, quizá ayude preparar un resumen cada lunes.\","
                        + "\"sources\":[\"recuerdos:21\",\"recuerdos:22\"]}");
        new DreamCycle(journal, model).run(day1, () -> false);
        clock.advanceHours(2);
        new ReflectionCycle(journal, model).run(day1, () -> false);

        // Reinicio de la app una semana después: el modelo se relee del almacén.
        clock.advanceHours(24 * 7);
        SelfModelJournal later = MindTestSupport.journal(store, clock);
        String self = later.render(1400);
        JsonObject data = promptData(self, "¿Qué me recomiendas para el lunes?");
        String seen = data.get("SELF_MODEL").getAsString();
        assertTrue(seen.contains("Bryan cuadra las facturas cada lunes"));
        assertTrue("La creencia conserva su procedencia", seen.contains("recuerdos:21"));
        assertTrue("El pensamiento propio se puede retomar", seen.contains("preparar un resumen cada lunes"));
        assertTrue(seen.contains("¿Qué parte del cierre trimestral le agobia más?"));
        assertTrue(seen.contains("hace 7 días"));
    }

    @Test public void contradictionAndForgettingPropagate() {
        SelfModelJournal journal = MindTestSupport.journal(store, clock);
        journal.apply(new MindPatch("sueño").belief("bryan", "Bryan prefiere explicaciones largas", 0.5, list("recuerdos:30")));
        String id = journal.beliefs().get(0).id;
        MindTestSupport.ScriptedGenerator model = new MindTestSupport.ScriptedGenerator()
                .then("{\"contradictions\":[{\"beliefId\":\"" + id + "\",\"sources\":[\"recuerdos:31\"]}]}");
        List<Episode> episodes = Collections.singletonList(
                new Episode("recuerdos:31", "[declaracion_usuario] Sé más directa, por favor", clock.get()));
        new DreamCycle(journal, model).run(episodes, () -> false);
        assertTrue(journal.render(1400).contains("CONTRADICHA"));

        journal.forgetSources(new HashSet<>(list("recuerdos:30")));
        assertFalse(journal.render(1400).contains("explicaciones largas"));
    }

    private static JsonObject promptData(String self, String input) {
        String prompt = CognitivePrompt.build("", input, new JsonObject(), self, "", "", Collections.emptyList(),
                Collections.emptyMap(), new JsonArray(), false, 2, 9000);
        return salve.core.autonomy.AutonomousToolCommand.parseObject(prompt.substring(prompt.lastIndexOf('\n') + 1));
    }
}
