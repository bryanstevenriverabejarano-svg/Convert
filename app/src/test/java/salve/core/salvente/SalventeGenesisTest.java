package salve.core.salvente;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;
import static org.junit.Assert.*;

public class SalventeGenesisTest {
    private static final String ID = "slv-0f8fad5b-d9cb-469f-a165-70867728950e";

    private static SalventeGenesisEngine engine(SalventeGenesisEngine.Generator generator) {
        return new SalventeGenesisEngine(generator, SalventeGenesisEngine::newId, () -> 1_800_000_000_000L);
    }

    private static EnumMap<SalventeTrait, Double> traits(double base) {
        EnumMap<SalventeTrait, Double> t = new EnumMap<>(SalventeTrait.class);
        for (SalventeTrait trait : SalventeTrait.values()) t.put(trait, base);
        return t;
    }

    private static SalventeProfile profile(String name, EnumMap<SalventeTrait, Double> traits, EnumSet<SalventeCapability> caps) {
        return new SalventeProfile(ID, name, SalventeProfile.SPECIES, "acompañar a mi madre", "Hola", traits, caps,
                SalventeProfile.State.ACTIVO, SalventeProfile.Origin.MODELO, 1);
    }

    private static EnumSet<SalventeCapability> base() { return EnumSet.of(SalventeCapability.CONVERSACION, SalventeCapability.MEMORIA_PROPIA); }

    @Test public void validProfilePassesAndIdIsImmutableFormat() {
        EnumMap<SalventeTrait, Double> t = traits(0.5); t.put(SalventeTrait.CALIDEZ, 0.9); t.put(SalventeTrait.ENERGIA, 0.2);
        assertTrue(SalventeProfileValidator.validate(profile("Nyra", t, base()), Collections.emptyList()).ok());
        assertTrue(SalventeProfileValidator.ID.matcher(SalventeGenesisEngine.newId()).matches());
    }

    @Test public void rejectsImpersonationDuplicatesRangeFlatnessAndIncoherence() {
        EnumMap<SalventeTrait, Double> t = traits(0.5); t.put(SalventeTrait.CALIDEZ, 0.9); t.put(SalventeTrait.ENERGIA, 0.2);
        assertFalse(SalventeProfileValidator.validate(profile("Salve", t, base()), null).ok());
        assertFalse(SalventeProfileValidator.validate(profile("Nueva Salve", t, base()), null).ok());
        assertFalse(SalventeProfileValidator.validate(profile("nyrá", t, base()), Arrays.asList("Nyra")).ok());
        assertFalse(SalventeProfileValidator.validate(profile("N", t, base()), null).ok());
        assertFalse(SalventeProfileValidator.validate(profile("Nyra1", t, base()), null).ok());
        EnumMap<SalventeTrait, Double> out = new EnumMap<>(t); out.put(SalventeTrait.HUMOR, 1.2);
        assertFalse(SalventeProfileValidator.validate(profile("Nyra", out, base()), null).ok());
        assertTrue(SalventeProfileValidator.validate(profile("Nyra", traits(0.5), base()), null).toString().contains("plana"));
        EnumSet<SalventeCapability> wellbeing = base(); wellbeing.add(SalventeCapability.BIENESTAR);
        EnumMap<SalventeTrait, Double> cold = new EnumMap<>(t); cold.put(SalventeTrait.EMPATIA, 0.2);
        assertTrue(SalventeProfileValidator.validate(profile("Nyra", cold, wellbeing), null).toString().contains("Bienestar"));
        EnumSet<SalventeCapability> noMemory = EnumSet.of(SalventeCapability.CONVERSACION);
        assertFalse(SalventeProfileValidator.validate(profile("Nyra", t, noMemory), null).ok());
    }

    @Test public void deterministicFallbackIsStableCoherentAndVariesOnRegenerate() {
        SalventeGenesisEngine e = engine(null);
        SalventeGenesisEngine.Request r = new SalventeGenesisEngine.Request("Ayudar a mi hija con los deberes y contarle cuentos", "");
        SalventeProfile a = e.propose(r, Collections.emptyList(), 0).profile;
        SalventeProfile b = e.propose(r, Collections.emptyList(), 0).profile;
        SalventeProfile c = e.propose(r, Collections.emptyList(), 1).profile;
        assertEquals(SalventeProfile.Origin.DETERMINISTA, a.origin);
        assertEquals(a.name, b.name); assertEquals(a.traits, b.traits);
        assertNotEquals(a.traits, c.traits);
        assertTrue(a.capabilities.contains(SalventeCapability.APOYO_ESTUDIO));
        assertTrue(a.capabilities.contains(SalventeCapability.CUENTOS));
        assertTrue(SalventeProfileValidator.validate(a, null).ok());
        assertFalse(a.greeting.contains("mi hija"));
        assertTrue(a.systemPrompt().contains("No eres Salve"));
    }

    @Test public void fallbackNeverRepeatsTakenNamesAndHonoursRequestedName() {
        SalventeGenesisEngine e = engine(null);
        List<String> taken = new ArrayList<>(Arrays.asList(SalventeGenesisEngine.NAMES));
        SalventeProfile p = e.propose(new SalventeGenesisEngine.Request("acompañar a la abuela", ""), taken, 0).profile;
        assertTrue(p.name.contains(" "));
        assertTrue(SalventeProfileValidator.validate(p, taken).ok());
        SalventeProfile named = e.propose(new SalventeGenesisEngine.Request("acompañar a la abuela", "Kael"), Collections.emptyList(), 0).profile;
        assertEquals("Kael", named.name);
        SalventeProfile impostor = e.propose(new SalventeGenesisEngine.Request("acompañar a la abuela", "Salve"), Collections.emptyList(), 0).profile;
        assertNotEquals("Salve", impostor.name);
    }

    @Test public void usesModelProposalAndRepairsItWithoutInventingAnother() {
        String json = "Claro: {\"nombre\":\"Kael\",\"rasgos\":{\"curiosidad\":0.9,\"Empatía\":0.3,\"calidez\":0.6,\"humor\":80,"
                + "\"creatividad\":0.85,\"rigor\":0.4,\"prudencia\":0.5,\"autonomia\":0.7,\"paciencia\":0.5,\"franqueza\":0.6,"
                + "\"energia\":0.8,\"formalidad\":0.2,\"proteccion\":0.4,\"optimismo\":0.7},"
                + "\"capacidades\":[\"CONVERSACION\",\"BIENESTAR\",\"LLAMADAS\",\"creatividad\"],\"saludo\":\"¡Ey! Soy Kael.\"} fin";
        SalventeGenesisEngine.Draft d = engine(prompt -> json).propose(new SalventeGenesisEngine.Request("inventar juegos", ""), null, 0);
        assertEquals(SalventeProfile.Origin.MODELO, d.profile.origin);
        assertEquals("Kael", d.profile.name);
        assertEquals(0.8, d.profile.trait(SalventeTrait.HUMOR), 1e-9);
        assertTrue(d.profile.capabilities.contains(SalventeCapability.MEMORIA_PROPIA));
        assertTrue(d.profile.capabilities.contains(SalventeCapability.CREATIVIDAD));
        assertFalse(d.profile.capabilities.contains(SalventeCapability.BIENESTAR));
        assertTrue(String.join(" ", d.notes).contains("LLAMADAS"));
        assertTrue(SalventeProfileValidator.validate(d.profile, null).ok());
    }

    @Test public void brokenOrMissingModelFallsBackToDeterministic() {
        assertEquals(SalventeProfile.Origin.DETERMINISTA,
                engine(prompt -> "no sé hacer JSON").propose(new SalventeGenesisEngine.Request("organizar mi negocio", ""), null, 0).profile.origin);
        assertEquals(SalventeProfile.Origin.DETERMINISTA,
                engine(prompt -> { throw new IllegalStateException("sin modelo"); })
                        .propose(new SalventeGenesisEngine.Request("organizar mi negocio", ""), null, 0).profile.origin);
        assertThrows(IllegalArgumentException.class,
                () -> engine(null).propose(new SalventeGenesisEngine.Request(" ", ""), null, 0));
    }

    @Test public void lateModelAnswerAfterCancelDoesNotResurrectTheDraft() throws Exception {
        java.util.concurrent.CountDownLatch thinking = new java.util.concurrent.CountDownLatch(1), release = new java.util.concurrent.CountDownLatch(1);
        SalventeGenesisSession.Store store = new SalventeGenesisSession.Store() {
            public Collection<String> names() { return Collections.emptyList(); }
            public SalventeProfile create(SalventeProfile p) { throw new AssertionError("no debe guardar"); }
        };
        SalventeGenesisSession session = new SalventeGenesisSession(engine(prompt -> { thinking.countDown(); release.await(); return "nada"; }), store);
        java.util.concurrent.atomic.AtomicReference<Object> result = new java.util.concurrent.atomic.AtomicReference<>("pendiente");
        Thread worker = new Thread(() -> result.set(session.propose("acompañar a la abuela", "")));
        worker.start();
        assertTrue(thinking.await(5, java.util.concurrent.TimeUnit.SECONDS));
        session.cancel();                       // no se bloquea aunque el modelo siga pensando
        release.countDown(); worker.join(5000);
        assertNull(result.get());
        assertNull(session.draft());
    }

    @Test public void cancelLeavesNothingAndApproveRevalidatesBeforeSaving() {
        AtomicInteger writes = new AtomicInteger();
        List<String> names = new ArrayList<>();
        SalventeGenesisSession.Store store = new SalventeGenesisSession.Store() {
            public Collection<String> names() { return new ArrayList<>(names); }
            public SalventeProfile create(SalventeProfile p) { writes.incrementAndGet(); names.add(p.name); return p; }
        };
        SalventeGenesisSession session = new SalventeGenesisSession(engine(null), store);
        session.propose("contarle cuentos a mi sobrino", "Lume");
        session.cancel();
        assertNull(session.draft());
        assertEquals(0, writes.get());
        assertThrows(IllegalStateException.class, session::approve);

        session.propose("contarle cuentos a mi sobrino", "Lume");
        assertFalse(session.rename("Salve").ok());
        assertEquals("Lume", session.draft().profile.name);
        assertTrue(session.toggle(SalventeCapability.MEMORIA_PROPIA).ok());
        assertTrue(session.draft().profile.capabilities.contains(SalventeCapability.MEMORIA_PROPIA));
        names.add("Lume");   // otro Salvente con ese nombre apareció mientras se revisaba
        assertThrows(IllegalArgumentException.class, session::approve);
        assertEquals(0, writes.get());
        assertTrue(session.rename("Iria").ok());
        assertEquals("Iria", session.approve().name);
        assertEquals(1, writes.get());
        assertNull(session.draft());
    }
}
