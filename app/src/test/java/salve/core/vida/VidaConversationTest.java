package salve.core.vida;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class VidaConversationTest {
    // Miércoles 18 de noviembre de 2026, 10:00 en Madrid.
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-18T09:00:00Z"), ZoneId.of("Europe/Madrid"));
    private final VidaConversation conversation = new VidaConversation(clock);
    private VidaLog log = new VidaLog();

    private String say(String input) {
        assertTrue("Debe capturarse: " + input, VidaConversation.handles(input, q -> VidaConversation.knows(log, q)));
        VidaConversation.Response r = conversation.handle(input, log);
        if (r.clearAll) log = new VidaLog();
        else if (r.next != null) log = r.next;
        return r.reply;
    }

    @Test public void recordsSetByVoiceWithAccentsAndCommas() {
        String reply = say("Curl predicador, 15 kilos, 8 reps");
        assertEquals(1, log.sets.size());
        VidaLog.GymSet s = log.sets.get(0);
        assertEquals("Curl predicador", s.exercise);
        assertEquals(15.0, s.kg, 0.0);
        assertEquals(Integer.valueOf(8), s.reps);
        assertEquals("2026-11-18", s.date);
        assertTrue(reply.contains("serie 1"));
        say("curl predicador 17,5 kg x 7 repeticiones");
        assertEquals(1, log.sets.get(1).setIndex);
        assertEquals(17.5, log.sets.get(1).kg, 0.0);
    }

    @Test public void sleepAcrossMidnightWithWords() {
        String reply = say("Me acosté a la una y me levanté a las ocho");
        assertEquals(1, log.sleep.size());
        assertEquals(7 * 60, log.sleep.get(0).minutes);
        assertTrue(reply, reply.contains("7 h"));
        say("me acoste a las 11 y media y me levante a las 7");
        assertEquals("solo un registro por noche", 1, log.sleep.size());
        assertEquals(7 * 60 + 30, log.sleep.get(0).minutes);
    }

    @Test public void mealsAndQuestionsAreDistinct() {
        say("He comido lentejas con arroz");
        assertEquals("comida", log.meals.get(0).kind);
        assertEquals("lentejas con arroz", log.meals.get(0).text);
        assertFalse(VidaConversation.handles("He cenado fuera, ¿qué opinas?", q -> false));
    }

    @Test public void scheduleBlocksAndToday() {
        say("Añade al horario gym los lunes, miércoles y viernes a las 19:00 durante 60 minutos");
        assertEquals(3, log.blocks.size());
        String today = say("¿Qué tengo hoy?");
        assertTrue(today, today.contains("miercoles"));
        assertTrue(today, today.contains("19:00 Gym"));
        say("añade al horario gym los miercoles a las 20");
        assertEquals("reemplaza el bloque del mismo día", 3, log.blocks.size());
        say("quita gym del horario");
        assertTrue(log.blocks.isEmpty());
    }

    @Test public void historyOnlyCapturedForKnownExercises() {
        assertFalse(VidaConversation.handles("cómo voy con el trabajo", q -> VidaConversation.knows(log, q)));
        say("peso muerto, 100 kg, 5 reps");
        assertFalse("«eso» no es peso muerto", VidaConversation.handles("como voy con eso", q -> VidaConversation.knows(log, q)));
        String h = say("historial de peso muerto");
        assertTrue(h, h.contains("Mejor serie: 100 kg × 5"));
    }

    @Test public void progressionAfterThreeEqualSessions() {
        List<VidaLog.GymSet> sets = new ArrayList<>();
        for (int d = 1; d <= 3; d++)
            sets.add(new VidaLog.GymSet("2026-11-0" + d, "", "Curl predicador", 0, "normal", 15.0, 8, null, null, "hevy", null));
        assertTrue(VidaConversation.progression(sets).contains("16,25 kg"));
    }

    @Test public void weeklySummaryAndUndo() {
        say("press banca, 60 kg, 8 reps");
        say("he cenado pollo");
        say("deshaz la ultima anotacion");
        assertTrue(log.meals.isEmpty());
        String week = say("resumen semanal");
        assertTrue(week, week.contains("1 días"));
        assertTrue(week, week.contains("480 kg"));
        say("borra mis datos de vida");
        assertTrue(log.isEmpty());
    }

    @Test public void ordinaryChatIsNotCaptured() {
        assertFalse(VidaConversation.handles("cuéntame un cuento", q -> false));
        assertFalse(VidaConversation.handles("mi sueldo es 1800 euros al mes", q -> false));
        assertFalse(VidaConversation.handles("hola salve", q -> false));
    }

    @Test public void serializationRoundTripKeepsTabsAndNewlines() {
        say("he comido arroz\tcon\\pollo");
        say("curl, 10 kg, 10 reps");
        VidaLog back = VidaLog.parse(log.serialize());
        assertEquals(log.serialize(), back.serialize());
    }
}
