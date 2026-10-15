package salve.sketch;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class ImageReferenceSearchTest {
    private static final String ES = "{\"query\":{\"pages\":[{\"title\":\"Cochinillo\",\"langlinks\":[{\"lang\":\"en\",\"title\":\"Suckling pig\"}]}]}}";
    private static String commons(boolean svg) {
        return "{\"query\":{\"pages\":["
                + "{\"title\":\"File:World map.svg\",\"index\":1,\"imageinfo\":[{\"thumburl\":\"https://upload.wikimedia.org/a/map.png\",\"mime\":\"image/svg+xml\"}]},"
                + "{\"title\":\"File:Pig icon.svg\",\"index\":2,\"imageinfo\":[{\"thumburl\":\"https://upload.wikimedia.org/a/pig.png\",\"mime\":\""
                + (svg ? "image/svg+xml" : "image/jpeg") + "\",\"descriptionurl\":\"https://commons.wikimedia.org/wiki/File:Pig_icon.svg\"}]},"
                + "{\"title\":\"File:Evil.svg\",\"index\":3,\"imageinfo\":[{\"thumburl\":\"https://example.com/x.png\",\"mime\":\"image/svg+xml\"}]}]}}";
    }

    @Test public void searchesInEnglishAndSpanishPreferringDrawings() throws Exception {
        List<String> asked = new ArrayList<>();
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> {
            asked.add(url);
            String body = url.contains("es.wikipedia") ? ES : commons(url.contains("drawing"));
            return body.getBytes(StandardCharsets.UTF_8);
        });
        List<ImageReferenceSearch.Candidate> found = search.search("un cochinillo", 4);
        assertTrue(asked.get(0).contains("es.wikipedia.org") && asked.get(0).contains("cochinillo"));
        assertTrue(asked.get(1).contains("suckling+pig") && asked.get(1).contains("filetype%3Adrawing"));
        assertEquals("mapas y orígenes ajenos se descartan", "Pig icon", found.get(0).label());
        assertTrue(found.get(0).drawing);
        for (ImageReferenceSearch.Candidate c : found) assertTrue(c.imageUrl.startsWith("https://upload.wikimedia.org/"));
        try { search.download(new ImageReferenceSearch.Candidate("x", "https://example.com/a.png", "", false), 10); fail(); }
        catch (java.io.IOException expected) { }
    }

    @Test public void malformedAnswersAreHarmless() {
        assertNull(ImageReferenceSearch.parseEnglishTitle("<html>"));
        assertTrue(ImageReferenceSearch.parseCommons("{\"batchcomplete\":true}").isEmpty());
    }

    @Test public void requestsWithReferencesAndPronouns() throws Exception {
        SketchLibrary lib = SketchTestAssets.library();
        SketchRequest r = SketchRequest.parse("Dibujalo busca referencias en internet y las hanalizas y lo haces", lib);
        assertEquals(SketchRequest.Kind.AGAIN, r.kind);
        assertTrue(r.references);
        r = SketchRequest.parse("dibuja un cochinillo", lib);
        assertEquals(SketchRequest.Kind.UNKNOWN, r.kind);
        assertEquals("cochinillo", r.unknown);
        r = SketchRequest.parse("busca referencias de un perro y dibújalo", lib);
        assertEquals(SketchRequest.Kind.UNKNOWN, r.kind);
        assertTrue(r.references);
        assertTrue(r.unknown.startsWith("perro"));
        assertEquals(SketchRequest.Kind.AGAIN, SketchRequest.parse("dibújalo", lib).kind);
        assertEquals(SketchRequest.Kind.DRAW, SketchRequest.parse("dibuja un perro", lib).kind);
    }
}
