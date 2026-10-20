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
        List<String> asked = java.util.Collections.synchronizedList(new ArrayList<>());
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> {
            asked.add(url);
            String body = url.contains("es.wikipedia") ? ES : commons(url.contains("drawing"));
            return body.getBytes(StandardCharsets.UTF_8);
        });
        List<ImageReferenceSearch.Candidate> found = search.search("un cochinillo", 4);
        // Las fuentes se consultan a la vez: importa qué se pidió, no en qué orden.
        assertTrue(asked.stream().anyMatch(u -> u.contains("es.wikipedia.org") && u.contains("cochinillo")));
        assertTrue(asked.stream().anyMatch(u -> u.contains("suckling+pig") && u.contains("filetype%3Adrawing")));
        assertEquals("mapas y orígenes ajenos se descartan", "Pig icon", found.get(0).label());
        assertTrue(found.get(0).drawing);
        for (ImageReferenceSearch.Candidate c : found) {
            assertTrue(c.imageUrl, ImageReferenceSearch.allowedImage(c.imageUrl));
            assertFalse("nunca usa la dirección de otro servidor", c.imageUrl.contains("example.com"));
        }
        try { search.download(new ImageReferenceSearch.Candidate("x", "https://example.com/a.png", "", false), 10); fail(); }
        catch (java.io.IOException expected) { }
    }

    @Test public void filesWithoutThumbnailAreNotLost() {
        // Lo que pasó con «dibuja un gorila»: Commons devolvió archivos, pero sin miniatura en la respuesta.
        String json = "{\"continue\":{\"iistart\":\"2019-01-19T12:46:12Z\",\"continue\":\"||\"},\"query\":{\"pages\":["
                + "{\"pageid\":1,\"title\":\"File:Gorilla silhouette.svg\",\"index\":2},"
                + "{\"pageid\":2,\"title\":\"File:Gorilla line art.svg\",\"index\":1,\"imageinfo\":[{\"mime\":\"image/svg+xml\","
                + "\"thumberror\":\"Error creating thumbnail\"}]},"
                + "{\"pageid\":3,\"title\":\"File:Gorilla (cropped).jpg\",\"index\":3,\"imageinfo\":[{\"mime\":\"image/jpeg\","
                + "\"thumburl\":\"https://upload.wikimedia.org/g.jpg\"}]},"
                + "{\"pageid\":4,\"title\":\"File:Gorilla range map.svg\",\"index\":4},"
                + "{\"pageid\":5,\"title\":\"File:Gorilla.pdf\",\"index\":5},"
                + "{\"pageid\":6,\"title\":\"File:Gorilla bad.svg\",\"index\":6,\"imageinfo\":\"rara\"}]}}";
        StringBuilder why = new StringBuilder();
        List<ImageReferenceSearch.Candidate> found = ImageReferenceSearch.parseCommons(json, why);
        assertEquals(4, found.size());
        assertEquals("en el orden de la búsqueda", "Gorilla line art", found.get(0).label());
        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/Gorilla_line_art.svg?width=480", found.get(0).imageUrl);
        assertTrue(found.get(0).drawing);
        assertEquals("Gorilla silhouette", found.get(1).label());
        assertEquals("https://upload.wikimedia.org/g.jpg", found.get(2).imageUrl);
        assertFalse(found.get(2).drawing);
        assertEquals("una página rara no tira el lote", "Gorilla bad", found.get(3).label());
        for (ImageReferenceSearch.Candidate c : found) {
            assertTrue(c.imageUrl, ImageReferenceSearch.allowedImage(c.imageUrl));
            assertEquals("Wikimedia Commons", c.source);
        }
        assertTrue(why.toString(), why.toString().contains("pedidas por nombre"));
        assertTrue(why.toString(), why.toString().contains("mapa/logo/texto"));
        assertTrue(why.toString(), why.toString().contains("formato"));
        assertFalse("nombres con ruta no", ImageReferenceSearch.allowedImage(
                "https://commons.wikimedia.org/wiki/Special:FilePath/../w/api.php"));
    }

    @Test public void thumbnailsFromOtherHostsAreAskedByName() {
        // «origen ajeno×6» en el móvil: la miniatura de la API no era de upload.wikimedia.org.
        String json = "{\"query\":{\"pages\":["
                + "{\"title\":\"File:Gorilla icon.svg\",\"index\":1,\"imageinfo\":[{\"mime\":\"image/svg+xml\","
                + "\"thumburl\":\"https://elsewhere.example/thumb/g.png\"}]},"
                + "{\"title\":\"File:Gorilla sketch.png\",\"index\":2,\"imageinfo\":[{\"mime\":\"image/png\","
                + "\"thumburl\":\"//upload.wikimedia.org/thumb/s.png\"}]}]}}";
        List<ImageReferenceSearch.Candidate> found = ImageReferenceSearch.parseCommons(json, null);
        assertEquals(2, found.size());
        assertEquals("https://commons.wikimedia.org/wiki/Special:FilePath/Gorilla_icon.svg?width=480", found.get(0).imageUrl);
        assertEquals("https://upload.wikimedia.org/thumb/s.png", found.get(1).imageUrl);
        assertEquals("https://upload.wikimedia.org/a.png", ImageReferenceSearch.wikimediaThumb("http://upload.wikimedia.org/a.png"));
        assertNull(ImageReferenceSearch.wikimediaThumb("https://upload.wikimedia.org.evil.example/a.png"));
        assertNull(ImageReferenceSearch.wikimediaThumb("https://upload.wikimedia.org/../x"));
    }

    @Test public void theRequestIsNotPartOfTheObject() {
        assertEquals("gorilla", ImageReferenceSearch.withoutRequestWords("improved gorilla"));
        assertEquals("gorila", ImageReferenceSearch.withoutRequestWords("gorila mejorado"));
        assertEquals("gorilla", ImageReferenceSearch.withoutRequestWords("a better detailed gorilla"));
        assertEquals("muñeco de nieve", ImageReferenceSearch.withoutRequestWords("muñeco de nieve"));
        assertEquals("si no queda nada, la original", "mejor", ImageReferenceSearch.withoutRequestWords("mejor"));
    }

    @Test public void googleThumbnailsAreUsedWithoutDownloading() throws Exception {
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> { throw new AssertionError("no descarga nada: " + url); });
        byte[] seen = {1, 2, 3, 4, 5};
        ImageReferenceSearch.Candidate c = ImageReferenceSearch.Candidate.fromScreen("Gorila dibujo para colorear", seen, "", 2);
        assertTrue(c.fromScreen());
        assertEquals("Google Imágenes", c.source);
        assertTrue("el texto dice que es un dibujo", c.drawing);
        assertEquals(2, c.screenIndex);
        assertTrue("huella estable, no la imagen", c.imageUrl.matches("google-imagenes:[0-9a-f]{24}"));
        assertEquals(c.imageUrl, ImageReferenceSearch.Candidate.fromScreen("otro texto", seen.clone(), "", 0).imageUrl);
        assertArrayEquals(seen, search.download(c, 100));
        try { search.download(c, 2); fail(); } catch (java.io.IOException tooBig) { }
        ImageReferenceSearch.Candidate photo = ImageReferenceSearch.Candidate.fromScreen("Gorila de montaña", seen, "", 0);
        assertFalse(photo.drawing);
        assertTrue("lo vio como dibujo", photo.seenAs(true).drawing);
        assertTrue(photo.seenAs(true).fromScreen());
        try { ImageReferenceSearch.Candidate.fromScreen("x", new byte[0], "", 0); fail(); } catch (IllegalArgumentException expected) { }
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

    @Test public void aSlowSourceDoesNotBlockTheOthers() throws Exception {
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> {
            if (url.contains("openverse")) {
                try { Thread.sleep(10_000); } catch (InterruptedException stop) { throw new java.io.IOException("timeout"); }
            }
            String body = url.contains("es.wikipedia") ? ES : commons(url.contains("drawing"));
            return body.getBytes(StandardCharsets.UTF_8);
        }).budget(1500);
        long t0 = System.currentTimeMillis();
        List<ImageReferenceSearch.Candidate> found = search.search("un cochinillo", 4);
        assertTrue("no espera a la fuente lenta", System.currentTimeMillis() - t0 < 4000);
        assertFalse(found.isEmpty());
    }

    @Test public void reportsWhenEverySourceFails() {
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> { throw new java.io.IOException("timeout"); }).budget(1000);
        try { search.search("gorila", 4); fail(); }
        catch (java.io.IOException expected) { assertTrue(search.diagnostic().contains("openverse")); }
    }
}
