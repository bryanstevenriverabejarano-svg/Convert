package salve.core.visual;

import static org.junit.Assert.*;

import org.junit.Test;

public class SceneGraphTest {
    private static String rels(SceneGraph g) { return g.relations.toString(); }

    @Test public void cupOnATableNextToALaptop() {
        SceneGraph g = SceneGraph.parse("Aquí tienes:\n```json\n{\"objetos\":["
                + "{\"nombre\":\"una Taza\",\"color\":\"blanca\",\"caja\":[62,50,72,66]},"
                + "{\"nombre\":\"mesa\",\"color\":\"marrón\",\"caja\":[5,62,98,100]},"
                + "{\"nombre\":\"portátil\",\"caja\":[25,38,58,66]}]}\n```");
        assertNotNull(g);
        assertEquals(3, g.items.size());
        assertEquals("taza", g.items.get(0).name);
        String r = rels(g);
        assertTrue(r, r.contains("taza encima de mesa"));
        assertTrue(r, r.contains("portátil encima de mesa"));
        assertTrue(r, r.contains("portátil a la izquierda de taza"));
        assertFalse("no se inventa lo contrario", r.contains("mesa encima de"));
    }

    @Test public void thingsInsideAndInFront() {
        SceneGraph g = SceneGraph.parse("{\"objetos\":["
                + "{\"nombre\":\"caja\",\"caja\":[20,20,80,90]},"
                + "{\"nombre\":\"pelota\",\"caja\":[40,60,55,80]},"
                + "{\"nombre\":\"perro\",\"caja\":[60,50,95,99]}]}");
        String r = rels(g);
        assertTrue(r, r.contains("pelota dentro de caja"));
        assertTrue(r, r.contains("perro delante de caja"));
    }

    @Test public void whereAndSizeInWords() {
        SceneGraph g = SceneGraph.parse("{\"objetos\":[{\"nombre\":\"lámpara\",\"caja\":[2,2,15,20]},{\"nombre\":\"sofá\",\"caja\":[10,50,90,98]}]}");
        assertEquals("arriba a la izquierda", g.items.get(0).where());
        assertEquals("pequeño", g.items.get(0).size());
        assertEquals("grande", g.items.get(1).size());
        double[] v = g.items.get(0).vector();
        assertEquals(0.085, v[0], 1e-9);
        assertEquals(0.13, v[2], 1e-9);
    }

    @Test public void toleratesOtherScalesSwappedCornersAndJunk() {
        // 0..1000 (como algunos modelos), esquinas al revés, un objeto sin caja y otro sin nombre.
        SceneGraph g = SceneGraph.parse("[{\"name\":\"silla\",\"box\":[600,900,300,400]},{\"name\":\"nada\"},{\"box\":[1,1,5,5]},"
                + "{\"name\":\"silla\",\"bbox\":[0.1,0.4,0.25,0.9]}]");
        assertNotNull(g);
        assertEquals(2, g.items.size());
        assertEquals(30, g.items.get(0).x1, 1e-9);
        assertEquals(90, g.items.get(0).y2, 1e-9);
        assertEquals("silla 2", g.items.get(1).name);
        assertEquals(java.util.Arrays.asList("silla"), g.names());
        assertNull(SceneGraph.parse("No puedo ver la imagen."));
        assertNull(SceneGraph.parse("{\"objetos\":[]}"));
    }

    @Test public void summaryIsSearchableAndRoundTrips() {
        SceneGraph g = SceneGraph.parse("{\"objetos\":[{\"nombre\":\"taza\",\"color\":\"roja\",\"caja\":[62,50,72,66]},{\"nombre\":\"mesa\",\"caja\":[5,62,98,100]}]}");
        String s = g.summary("dc6c627b-4045-42f9-8113-cfd910e15fc0");
        assertTrue(s, s.startsWith("Escena de la foto dc6c627b-4045-42f9-8113-cfd910e15fc0: hay taza roja (pequeño"));
        assertTrue(s, s.contains("taza: encima de mesa."));
        SceneGraph back = SceneGraph.fromJson(g.json().toString());
        assertEquals(g.relations.toString(), back.relations.toString());
        assertTrue(g.json().toString().contains("\"vector\""));
    }

    @Test public void aSceneFileIsNotMistakenForAPhotoRecord() throws Exception {
        java.io.File dir = java.nio.file.Files.createTempDirectory("escena").toFile();
        VisualMemoryStore store = new VisualMemoryStore(dir);
        String id = "dc6c627b-4045-42f9-8113-cfd910e15fc0";
        assertNull(store.readScene(id));
        store.saveScene(id, "{\"objetos\":[]}");
        assertEquals("{\"objetos\":[]}", store.readScene(id));
        assertTrue(store.list().isEmpty());
    }

    @Test public void drawingTheSceneIsNotDescribingIt() {
        assertTrue(SceneGraph.isDrawRequest("Dibuja la escena de la foto"));
        assertTrue(SceneGraph.isDrawRequest("dibújame lo que hay en la foto"));
        assertTrue(SceneGraph.isDrawRequest("dibuja mi escritorio de memoria"));
        assertFalse(SceneGraph.isDrawRequest("dibuja un gato"));
        assertFalse("dibujar no es describir", SceneGraph.isCommand("Dibuja la escena de la foto"));
        assertTrue(SceneGraph.isCommand("¿Qué objetos hay?"));
        assertEquals(java.util.Arrays.asList("escritorio"), SceneGraph.requestWords("dibuja mi escritorio de memoria"));
        SceneGraph g = SceneGraph.parse("{\"objetos\":[{\"nombre\":\"portátil\",\"caja\":[10,10,50,50]},{\"nombre\":\"taza\",\"caja\":[60,60,70,70]}]}");
        assertEquals(1, g.matches(java.util.Arrays.asList("escritorio"), "Un escritorio con un portátil"));
        assertEquals("portátil", g.prominent(1).get(0).name);
    }
}
