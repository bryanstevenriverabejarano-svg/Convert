package salve.core.mind;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class MindMapTest {
    static SelfModel.Belief belief(String id, String about, String text, double c, String... sources) {
        SelfModel.Belief b = new SelfModel.Belief(); b.id = id; b.about = about; b.statement = text; b.confidence = c;
        b.sources = new ArrayList<>(Arrays.asList(sources)); return b;
    }
    static boolean edge(JsonObject map, String from, String to) {
        for (JsonElement e : map.getAsJsonArray("edges")) {
            JsonObject o = e.getAsJsonObject();
            if (o.get("from").getAsString().equals(from) && o.get("to").getAsString().equals(to)) return true;
        }
        return false;
    }
    static boolean node(JsonObject map, String id) {
        for (JsonElement e : map.getAsJsonArray("nodes")) if (e.getAsJsonObject().get("id").getAsString().equals(id)) return true;
        return false;
    }

    @Test public void beliefsAreLinkedToTheMemoriesTheyComeFrom() {
        List<MindMap.Memory> mems = Arrays.asList(
                new MindMap.Memory(4, "Bryan me enseñó a dibujar un gorila", Arrays.asList("dibujo", "bryan", "hecho_usuario"), 10, 6),
                new MindMap.Memory(9, "Vivina hace la mejor tortilla", Arrays.asList("comida", "familia"), 20, 8),
                new MindMap.Memory(11, "Fuimos al Gorbea", Arrays.asList("montaña", "familia"), 30, 5));
        JsonObject map = MindMap.build("Me formo a partir de lo que vivo con Bryan.",
                Arrays.asList(belief("b1", "bryan", "A Bryan le gusta que dibuje", 0.8, "recuerdos:4"), belief("b2", "yo", "Aprendo con ejemplos", 0.6)),
                new ArrayList<>(), new ArrayList<>(), Arrays.asList(new MindMap.Person("Vivina Rivera", "madre")), mems,
                new ArrayList<>(), "Bryan Steven Rivera Bejarano");
        assertTrue(node(map, "yo"));
        assertTrue(edge(map, "p:bryan", "b:b1"));            // creencia sobre Bryan
        assertTrue(edge(map, "yo", "b:b2"));                 // creencia sobre ella
        assertTrue(edge(map, "b:b1", "m:4"));                // se basa en el recuerdo 4
        assertTrue(edge(map, "p:bryan", "p:vivina_rivera")); // familia
        assertTrue(node(map, "tema:familia"));               // tema repetido
        assertFalse(node(map, "tema:hecho_usuario"));        // etiqueta técnica no es tema
        assertTrue(edge(map, "p:vivina_rivera", "m:9"));     // aparece en el recuerdo
        for (JsonElement e : map.getAsJsonArray("edges")) {  // nunca aristas colgando
            JsonObject o = e.getAsJsonObject();
            assertTrue(node(map, o.get("from").getAsString()) && node(map, o.get("to").getAsString()));
        }
    }

    @Test public void emptyMindStillRendersAPage() {
        JsonObject map = MindMap.build(null, null, null, null, null, null, null, null);
        assertTrue(node(map, "yo"));
        String html = MindMap.html(map);
        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertFalse("sin recursos de internet", html.contains("http://") || html.contains("https://"));
    }

    @Test public void textCannotBreakOutOfTheScript() {
        JsonObject map = MindMap.build("</script><script>alert(1)</script>", null, null, null, null, null, null, null);
        assertFalse(MindMap.html(map).contains("</script><script>alert"));
    }

    @Test public void commandIsAnExactPhrase() {
        assertTrue(MindMapCommand.matches("Enséñame el mapa de tu mente"));
        assertTrue(MindMapCommand.matches("Salve, abre tu mapa mental"));
        assertTrue(MindMapCommand.matches("mapa de tu mente"));
        assertFalse(MindMapCommand.matches("¿tienes un mapa de Vitoria en tu mente?"));
    }
}
