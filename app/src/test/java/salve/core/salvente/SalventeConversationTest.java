package salve.core.salvente;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SalventeConversationTest {
    static SalventeProfile nyra(SalventeProfile.State state) {
        EnumMap<SalventeTrait, Double> t = new EnumMap<>(SalventeTrait.class);
        for (SalventeTrait trait : SalventeTrait.values()) t.put(trait, 0.5);
        t.put(SalventeTrait.CALIDEZ, 0.9); t.put(SalventeTrait.ENERGIA, 0.2);
        return new SalventeProfile("slv-0f8fad5b-d9cb-469f-a165-70867728950e", "Nyra", SalventeProfile.SPECIES,
                "acompañar a mi madre", "Hola", t, EnumSet.of(SalventeCapability.CONVERSACION, SalventeCapability.MEMORIA_PROPIA),
                state, SalventeProfile.Origin.MODELO, 1);
    }

    static final class Store implements SalventeConversation.Memories {
        final List<SalventeMind.Memory> newestFirst = new ArrayList<>();
        public List<SalventeMind.Memory> recent(int max) { return newestFirst.subList(0, Math.min(max, newestFirst.size())); }
        public void remember(String text) { newestFirst.add(0, new SalventeMind.Memory(newestFirst.size() + 100, text)); }
    }

    @Test public void promptUsesOnlyItsOwnIdentityAndMemories() {
        List<SalventeMind.Memory> mem = Collections.singletonList(new SalventeMind.Memory(1, "A su madre le encanta el cocido"));
        String prompt = SalventeMind.prompt(nyra(SalventeProfile.State.ACTIVO), mem, Collections.emptyList(), "Bryan", "¿Qué cocino hoy?", false);
        assertTrue(prompt.startsWith("Eres Nyra"));
        assertFalse(prompt.contains("Eres Salve"));
        assertTrue(prompt.contains("No eres Salve"));
        assertTrue(prompt.contains("cocido"));
        assertTrue(prompt.endsWith("Nyra:"));
        assertTrue(prompt.contains("son sus palabras"));
    }

    @Test public void recallsRecentAndRelatedButNotEverything() {
        List<SalventeMind.Memory> newestFirst = new ArrayList<>();
        for (int i = 0; i < 30; i++) newestFirst.add(new SalventeMind.Memory(1000 - i, "charla corriente número " + i));
        newestFirst.add(new SalventeMind.Memory(5, "Su jardín tiene tomates y albahaca"));
        List<SalventeMind.Memory> chosen = SalventeMind.recall(newestFirst, "¿Cómo van los tomates del jardín?");
        assertEquals(SalventeMind.RECENT + 1, chosen.size());
        assertEquals(5, chosen.get(0).at);   // cronológico: el antiguo relacionado va primero
        assertTrue(SalventeMind.recall(Collections.emptyList(), "hola").isEmpty());
    }

    @Test public void cleansNamePrefixAndInventedContinuation() {
        SalventeProfile p = nyra(SalventeProfile.State.ACTIVO);
        assertEquals("¡Claro! Hoy cocido.", SalventeMind.clean("Nyra: ¡Claro! Hoy cocido.\nBryan: genial\nNyra: …", p, "Bryan"));
        assertEquals("Hola", SalventeMind.clean("  Hola<|eot_id|>", p, "Bryan"));
    }

    @Test public void correctsIdentityDriftOnceAndRemembersTheExchange() throws Exception {
        Store store = new Store(); List<String> prompts = new ArrayList<>();
        SalventeConversation chat = new SalventeConversation(nyra(SalventeProfile.State.ACTIVO), store, prompt -> {
            prompts.add(prompt);
            return prompts.size() == 1 ? "Soy Salve, ¿en qué te ayudo?" : "Soy Nyra. ¿Cocinamos algo rico?";
        }, "Bryan");
        SalventeConversation.Reply r = chat.say("¿Quién eres?");
        assertTrue(r.corrected);
        assertEquals("Soy Nyra. ¿Cocinamos algo rico?", r.text);
        assertTrue(prompts.get(1).contains("IMPORTANTE: tu nombre es Nyra"));
        assertEquals(1, store.newestFirst.size());
        assertTrue(store.newestFirst.get(0).text.contains("¿Quién eres?"));
        assertEquals(2, chat.turns().size());
    }

    @Test public void persistentDriftIsRewrittenToItsOwnName() throws Exception {
        SalventeConversation chat = new SalventeConversation(nyra(SalventeProfile.State.ACTIVO), new Store(), p -> "Soy Salve.", "Bryan");
        assertEquals("Soy Nyra.", chat.say("hola").text);
    }

    @Test public void noModelMeansNoFakeReplyAndNoMemory() {
        Store store = new Store();
        SalventeConversation chat = new SalventeConversation(nyra(SalventeProfile.State.ACTIVO), store, p -> { throw new IllegalStateException("sin modelo"); }, "Bryan");
        assertThrows(IllegalStateException.class, () -> chat.say("hola"));
        assertTrue(store.newestFirst.isEmpty());
        assertTrue(chat.turns().isEmpty());
        SalventeConversation paused = new SalventeConversation(nyra(SalventeProfile.State.PAUSADO), store, p -> "hola", "Bryan");
        assertThrows(IllegalStateException.class, () -> paused.say("hola"));
    }
}
