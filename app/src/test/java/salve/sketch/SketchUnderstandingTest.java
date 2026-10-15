package salve.sketch;

import static org.junit.Assert.*;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Salve entiende lo que dibuja: lo piensa antes, lo mejora sin copiar y explica por qué salió así. */
public class SketchUnderstandingTest {
    private static SketchRequest parse(String s) throws Exception { return SketchRequest.parse(s, SketchTestAssets.library()); }

    private static final String GORILLA = "Claro.\nINGLES: Gorilla\nTIPO: animal, primate grande\nPARTES:\n"
            + "- cabeza: pequeña, con cresta, arriba y delante\n- hombros: muy anchos\n"
            + "- brazos: largos, apoyados en los nudillos\n- piernas: cortas y gruesas\nPOSE: a cuatro patas, de perfil\n"
            + "Ahora «gato»:\n- cola: larga";

    @Test public void thinksAboutTheObjectBeforeDrawing() {
        VisualConcept c = VisualConcept.parse(GORILLA, "gorila");
        assertTrue(c.isUsable());
        assertEquals("gorilla", c.english);
        assertEquals(4, c.parts.size());
        assertEquals("brazos", c.parts.get(2).name);
        assertTrue(c.pose.contains("cuatro patas"));
        assertTrue(c.summary().contains("primate grande"));
        String shapes = ShapeProgram.prompt("gorila", c, "con más detalle");
        assertTrue(shapes.indexOf("- hombros: muy anchos") < shapes.lastIndexOf("Ahora «gorila»"));
        assertTrue(shapes.contains("Bryan quiere: con más detalle"));
        assertFalse(VisualConcept.parse("No sé qué es eso.", "x").isUsable());
        assertEquals("", VisualConcept.parse("INGLES: <script>\nTIPO: a\n- a: b\n- c: d", "x").english.replaceAll("[a-z ]", ""));
    }

    @Test public void threeShapesAreTheMinimum() {
        assertFalse("un cuadrado y un círculo no son un gorila",
                ShapeProgram.parse("rect 10 10 80 80\ncircle 50 50 30").isUsable());
    }

    @Test public void improvingNeverReusesTheSavedVersion() throws Exception {
        // Las peticiones reales de Bryan.
        SketchRequest r = parse("Dibuja un muñeco de nieve. Umm busca refwrencias que el que te enseñe a dibujar estaba muy feo.");
        assertEquals(SketchRequest.Kind.UNKNOWN, r.kind);
        assertTrue(r.improve);
        assertTrue(r.references);
        assertEquals("muneco nieve", r.unknown);
        r = parse("Vale  dibuja el muñeco de nieve pero mejor");
        assertTrue(r.improve);
        assertEquals("muneco nieve", r.unknown);
        r = parse("Sí dibuja un gato con más detalle con una pose diferente");
        assertEquals(SketchRequest.Kind.UNKNOWN, r.kind);
        assertTrue(r.improve);
        assertEquals("gato", r.unknown);
        assertTrue(r.wish.contains("con mas detalle"));
        assertTrue(r.wish.contains("pose diferente"));
        r = parse("mejóralo");
        assertEquals(SketchRequest.Kind.AGAIN, r.kind);
        assertTrue(r.improve);
        assertEquals(SketchRequest.Kind.DRAW, parse("dibuja un gato").kind);
        assertFalse(parse("dibuja un gato").improve);
    }

    @Test public void namesLoseThePetitionGluedToThem() {
        assertEquals("muñeco de nieve", SketchRequest.cleanName("Muñeco de nieve mejoralo.."));
        assertTrue(SketchRequest.asksToImprove("Muñeco de nieve mejóralo.."));
        assertEquals("muñeco de nieve", SketchRequest.cleanName("muñeco de nieve"));
        assertFalse(SketchRequest.asksToImprove("muñeco de nieve"));
        assertEquals("mejor", SketchRequest.cleanName("mejor"));
    }

    @Test public void recognisesQuestionsAboutHerDrawings() {
        assertEquals(SketchTalk.Kind.WHY, SketchTalk.parse("¿Por qué lo dibujaste así?"));
        assertEquals(SketchTalk.Kind.WHY, SketchTalk.parse("por qué se ve tan abstracto"));
        assertEquals(SketchTalk.Kind.WHY, SketchTalk.parse("¿en qué te basaste?"));
        assertEquals(SketchTalk.Kind.CRITIQUE, SketchTalk.parse("No se parece mucho a un gorila. Sabes que es un gorila??"));
        assertEquals(SketchTalk.Kind.CRITIQUE, SketchTalk.parse("está muy feo"));
        assertEquals(SketchTalk.Kind.LOOK, SketchTalk.parse("mira tu dibujo, ¿qué ves?"));
        assertEquals(SketchTalk.Kind.NONE, SketchTalk.parse("¿qué tiempo hace mañana?"));
    }

    @Test public void readsWhatSheSeesInHerOwnDrawing() {
        SketchTalk.Look no = SketchTalk.parseLook("VEO: un cuadrado con un círculo y una diagonal\nPARECE_OBJETIVO: no");
        assertEquals(SketchTalk.Verdict.NO, no.verdict);
        assertFalse(no.acceptable());
        assertTrue(no.describe("gorila").contains("no se reconoce como gorila"));
        assertEquals(SketchTalk.Verdict.PARTLY, SketchTalk.parseLook("veo: un animal\nparece_objetivo: un poco").verdict);
        assertEquals(SketchTalk.Verdict.YES, SketchTalk.parseLook("VEO: un gorila\nPARECE_OBJETIVO: sí").verdict);
        assertTrue(SketchTalk.parseLook("bla").acceptable());
    }

    @Test public void journalExplainsWhyAndRemembersReferences() throws Exception {
        File dir = Files.createTempDirectory("journal").toFile();
        SketchJournal j = new SketchJournal(dir);
        j.saveConcept(VisualConcept.parse(GORILLA, "gorila"));
        SketchJournal.Attempt a = new SketchJournal.Attempt();
        a.source = "model";
        a.how = "no tenía una referencia útil (no hay conexión a internet), así que lo imaginé yo con 3 formas.";
        a.selfCheck = "veo un cuadrado; no se reconoce como gorila.";
        j.record("un gorila", a);
        SketchJournal.Attempt b = new SketchJournal.Attempt();
        b.source = "web"; b.how = "busqué referencias."; b.reference = "Gorilla silhouette"; b.referenceUrl = "https://upload.wikimedia.org/g.png";
        j.record("gorila", b);
        j.feedback("gorila", "No se parece mucho a un gorila");
        // Persiste: otra instancia lo lee del archivo.
        SketchJournal again = new SketchJournal(dir);
        String why = again.explain("gorila");
        assertTrue(why.contains("Gorilla silhouette"));
        assertTrue(why.contains("primate grande"));
        assertTrue(why.contains("compuesto por mí con formas geométricas"));
        assertTrue(why.contains("No se parece mucho a un gorila"));
        assertEquals(Collections.singletonList("https://upload.wikimedia.org/g.png"), again.usedReferences("gorila"));
        assertEquals("gorilla", again.concept("gorila").english);
        assertEquals("", again.explain("dragón azul"));
    }

    @Test public void referencesKeepTheOutlineAndAreNotRepeated() throws Exception {
        List<float[]> strokes = new ArrayList<>();
        strokes.add(new float[]{0, 0, 900, 0, 900, 900, 0, 900, 0, 0}); // contorno
        for (int i = 0; i < 120; i++) strokes.add(new float[]{i, i, i + 3, i + 2}); // pelo menudo
        ImageTracer.Result noisy = ImageTracer.trace(blank(), 16, 16, 100);
        ImageTracer.Result many = new ImageTracer.Result(strokes, 1, new ImageTracer.Geometry(100, 40, 40, 20, 21, 1, 121, .5f, 1));
        ImageTracer.Result focused = ImageReferenceSearch.focus(many, 45);
        assertEquals(45, focused.strokes.size());
        assertSame("el contorno largo se queda", strokes.get(0), focused.strokes.get(0));
        ImageReferenceSearch.Candidate silhouette = new ImageReferenceSearch.Candidate("File:Gorilla silhouette.svg", "u", "", true);
        ImageReferenceSearch.Candidate skull = new ImageReferenceSearch.Candidate("File:Gorilla skull.svg", "u", "", true);
        assertTrue("una foto con 121 trazos ya no se descarta del todo", ImageReferenceSearch.score(silhouette, many) > 0);
        assertTrue(ImageReferenceSearch.score(silhouette, focused) > ImageReferenceSearch.score(skull, focused));
        assertNotNull(noisy);

        List<String> asked = new ArrayList<>();
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> {
            asked.add(url);
            String body = url.contains("es.wikipedia") ? "{}" : "{\"query\":{\"pages\":["
                    + "{\"title\":\"File:A.svg\",\"index\":1,\"imageinfo\":[{\"thumburl\":\"https://upload.wikimedia.org/a.png\",\"mime\":\"image/svg+xml\"}]},"
                    + "{\"title\":\"File:B.svg\",\"index\":2,\"imageinfo\":[{\"thumburl\":\"https://upload.wikimedia.org/b.png\",\"mime\":\"image/svg+xml\"}]}]}}";
            return body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        });
        List<ImageReferenceSearch.Candidate> found = search.search("gorila", "gorilla", 4, Arrays.asList("https://upload.wikimedia.org/a.png"));
        assertTrue("usa el nombre en inglés que ella pensó", asked.get(1).contains("gorilla+silhouette"));
        assertEquals(1, found.size());
        assertEquals("https://upload.wikimedia.org/b.png", found.get(0).imageUrl);
    }

    @Test public void echoedHistoryNotesNeverReachTheChat() throws Exception {
        SketchProtocol.Extraction e = SketchProtocol.extract(
                "Es un gorila. (Dibujé con mis trazos de luz: gorila con detalle.) [dibujo mostrado: gorila]", SketchTestAssets.library());
        assertEquals("Es un gorila.", e.clean.trim());
        assertFalse(e.display.contains("Dibujé"));
    }

    private static int[] blank() { int[] p = new int[256]; Arrays.fill(p, 0xFFFFFFFF); return p; }
}
