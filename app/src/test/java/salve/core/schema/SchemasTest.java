package salve.core.schema;

import static org.junit.Assert.*;

import java.util.List;
import org.junit.Test;
import salve.core.mind.SelfModelJournal;
import salve.core.visual.SceneGraph;

public class SchemasTest {
    static final class Mem implements SelfModelJournal.Store {
        String json;
        @Override public String read() { return json; }
        @Override public void write(String j) { json = j; }
    }

    @Test public void theCatalogIsFixedAndItsInferencesAreAlwaysTheSame() {
        assertEquals(16, ImageSchemas.Schema.values().length);
        List<String> a = ImageSchemas.Schema.SOPORTE.infer("la economía", "la confianza");
        assertEquals("la economía se mantiene gracias a la confianza", a.get(0));
        assertEquals("si falta la confianza, la economía cae", a.get(1));
        assertEquals(a, ImageSchemas.Schema.SOPORTE.infer("la economía", "la confianza"));
        assertTrue(ImageSchemas.Schema.FRONTERA.infer(null, null).get(0).contains("«un lado»"));
        assertEquals(ImageSchemas.Schema.PARTE_TODO, ImageSchemas.byName("Parte y todo"));
        assertEquals(ImageSchemas.Schema.CAMBIO_ESTADO, ImageSchemas.byName("cambio_estado"));
        assertNull(ImageSchemas.byName("gravedad cuántica"));
    }

    @Test public void metaphorsGiveAwayTheirSchemas() {
        assertEquals(ImageSchemas.Schema.SOPORTE, ImageSchemas.detect("¿Por qué la economía se cae?").get(0));
        assertTrue(ImageSchemas.detect("poner fronteras emocionales a alguien").contains(ImageSchemas.Schema.FRONTERA));
        assertTrue(ImageSchemas.detect("estoy en un callejón sin salida").contains(ImageSchemas.Schema.CAMINO));
        assertTrue(ImageSchemas.detect("hola qué tal").isEmpty());
        assertTrue(ImageSchemas.catalog().contains("soporte (sostenido, soporte)"));
    }

    @Test public void aPhotoShowsItsSchemasAndWhatFollowsFromThem() {
        // El caso real de Bryan: reloj y tijeras encima del libro, el libro encima de la manta.
        SceneGraph g = SceneGraph.parse("{\"objetos\":[{\"nombre\":\"reloj\",\"caja\":[15,15,40,40]},{\"nombre\":\"tijeras\",\"caja\":[50,35,75,70]},"
                + "{\"nombre\":\"libro\",\"caja\":[10,30,85,85]},{\"nombre\":\"manta\",\"caja\":[0,0,100,100]}]}");
        String d = SceneSchemas.describe(g);
        assertTrue(d, d.contains("soporte (libro sostiene a reloj)") || d.contains("soporte (libro sostiene a tijeras)"));
        List<String> ded = SceneSchemas.deductions(g);
        assertTrue(ded.toString(), ded.stream().anyMatch(s -> s.startsWith("si quitas libro, caerían") && s.contains("reloj") && s.contains("tijeras")));
        // Nada está «dentro» de la manta ni del libro: no son recipientes.
        assertFalse(d, d.contains("contiene"));
        assertTrue(ded.toString(), ded.stream().anyMatch(s -> s.startsWith("si quitas manta, caerían") && s.contains("libro")));
    }

    @Test public void chainsAreFollowed() {
        SceneGraph g = SceneGraph.parse("{\"objetos\":[{\"nombre\":\"taza\",\"caja\":[45,30,55,42]},{\"nombre\":\"libro\",\"caja\":[35,42,65,50]},"
                + "{\"nombre\":\"mesa\",\"caja\":[10,48,90,95]}]}");
        assertTrue(SceneSchemas.deductions(g).toString(), SceneSchemas.deductions(g).contains("si quitas mesa, caerían libro y taza"));
    }

    @Test public void theModelOnlyChoosesFromTheCatalogAndItIsValidated() {
        String model = "```json\n{\"palabras\":[\"crisis\",\"economy\",\"economía\"],\"esquemas\":[{\"esquema\":\"Soporte\",\"sostenido\":\"la economía\","
                + "\"soporte\":\"la confianza\"},{\"esquema\":\"gravedad cuántica\",\"x\":\"y\"},{\"esquema\":\"equilibrio\",\"un lado\":\"lo que se gasta\","
                + "\"otro_lado\":\"lo que se produce\"},{\"esquema\":\"soporte\"}]}\n```";
        ConceptStore.Concept c = ConceptStore.parse("economía", model, "test");
        assertNotNull(c);
        assertEquals(2, c.schemas.size());
        assertEquals("soporte", c.schemas.get(0).schema);
        assertEquals("lo que se gasta", c.schemas.get(1).a);
        assertEquals(java.util.Arrays.asList("crisis", "economy"), c.words);
        String r = ConceptStore.reasoning(c);
        assertTrue(r, r.contains("si falta la confianza, la economía cae"));
        assertTrue(Abstraction.explain(c), Abstraction.explain(c).contains("Como algo que se mantiene gracias a lo que lo sostiene: la economía se mantiene gracias a la confianza."));
        assertNull(ConceptStore.parse("x", "{\"esquemas\":[{\"esquema\":\"inventado\"}]}", "t"));
        assertNull(ConceptStore.parse("x", "no sé", "t"));
    }

    @Test public void conceptsSurviveRestartAndAnyWordFindsThem() {
        Mem store = new Mem();
        ConceptStore s = new ConceptStore(store, () -> 1L);
        s.queue("libertad"); s.queue("libertad"); s.queue("economía");
        assertEquals(java.util.Arrays.asList("libertad", "economia"), s.pending(5));
        s.put(ConceptStore.parse("economía", "{\"palabras\":[\"economy\"],\"esquemas\":[{\"esquema\":\"soporte\",\"sostenido\":\"economía\",\"soporte\":\"confianza\"}]}", "t"));
        ConceptStore again = new ConceptStore(store, () -> 2L);
        assertNotNull(again.find("Economy"));
        assertNotNull(again.find("ECONOMÍA"));
        assertEquals(java.util.Arrays.asList("libertad"), again.pending(5));
        assertTrue(Abstraction.list(again.all(), again.pending(5)).contains("economía (soporte)"));
    }

    @Test public void phrases() {
        assertTrue(Abstraction.offered("¿Por qué decimos que una economía se puede caer?"));
        assertTrue(Abstraction.offered("¿Qué significa poner fronteras emocionales?"));
        assertFalse(Abstraction.offered("hola"));
        assertEquals("libertad", Abstraction.decomposeRequest("Descompón el concepto de libertad"));
        assertEquals("amistad", Abstraction.decomposeRequest("abstrae la amistad"));
        assertNull(Abstraction.decomposeRequest("analiza la escena"));
        assertTrue(Abstraction.asksForList("¿Qué conceptos has abstraído?"));
        assertTrue(Abstraction.toolDescription().length() < 160);
    }
}
