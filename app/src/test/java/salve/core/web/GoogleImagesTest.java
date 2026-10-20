package salve.core.web;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.Test;

public class GoogleImagesTest {
    private static String dataUri(int size) {
        byte[] fake = new byte[size];
        for (int i = 0; i < size; i++) fake[i] = (byte) i;
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(fake);
    }

    /** WebView devuelve el resultado de evaluateJavascript como texto JSON entre comillas. */
    private static String evaluated(String json) {
        return "\"" + json.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @Test public void searchesDrawingsWithSafeSearch() {
        String url = GoogleImages.url("gorila");
        assertTrue(url.startsWith("https://www.google.com/search?"));
        assertTrue("pestaña de imágenes", url.contains("udm=2"));
        assertTrue(url.contains("safe=active"));
        assertTrue(url.contains("q=gorila+dibujo"));
        assertTrue("no repite «dibujo»", GoogleImages.url("silueta de gorila").endsWith("q=silueta+de+gorila"));
        try { GoogleImages.url("  "); fail(); } catch (IllegalArgumentException expected) { }
    }

    @Test public void readsThumbnailsTheWindowSees() {
        String json = "{\"vw\":360,\"vh\":640,\"items\":["
                + "{\"alt\":\"Gorila dibujo de líneas\",\"data\":\"" + dataUri(900) + "\",\"x\":10,\"y\":100,\"w\":160,\"h\":160,"
                + "\"href\":\"https://example.org/gorila\"},"
                + "{\"alt\":\"Gorila silueta\",\"data\":\"\",\"x\":180,\"y\":100,\"w\":160,\"h\":160,\"href\":\"javascript:alert(1)\"},"
                + "{\"alt\":\"vacía\",\"data\":\"data:image/gif;base64,R0lGODlhAQABAAAAACw=\",\"x\":10,\"y\":900,\"w\":160,\"h\":160}]}";
        GoogleImages.Page page = GoogleImages.parse(evaluated(json));
        assertEquals(360, page.viewportW, 0);
        assertEquals(3, page.spots.size());
        GoogleImages.Spot first = page.spots.get(0);
        assertEquals("Gorila dibujo de líneas", first.alt);
        assertNotNull("miniatura incrustada", first.bytes);
        assertEquals(900, first.bytes.length);
        assertTrue(first.visible(page.viewportW, page.viewportH));
        assertNull("sin imagen incrustada: se recorta de la pantalla", page.spots.get(1).bytes);
        assertEquals("solo enlaces https", "", page.spots.get(1).href);
        assertNull("gif de relleno no vale", page.spots.get(2).bytes);
        assertFalse("fuera de la pantalla", page.spots.get(2).visible(page.viewportW, page.viewportH));
    }

    @Test public void oddAnswersNeverThrow() {
        assertTrue(GoogleImages.parse(null).spots.isEmpty());
        assertTrue(GoogleImages.parse("null").spots.isEmpty());
        assertTrue(GoogleImages.parse("<html>").spots.isEmpty());
        assertTrue(GoogleImages.parse(evaluated("{\"vw\":0,\"vh\":0,\"items\":[],\"error\":\"x\"}")).spots.isEmpty());
        assertNull(GoogleImages.decode("data:image/jpeg;base64,%%%"));
        assertNull("demasiado pequeña", GoogleImages.decode(dataUri(50)));
        assertNull("demasiado grande", GoogleImages.decode(dataUri(GoogleImages.MAX_THUMB_BYTES + 1)));
    }

    @Test public void knowsWhenGoogleAsksForAHuman() {
        assertEquals(GoogleImages.Blocker.NONE, GoogleImages.blocker("https://www.google.com/search?udm=2&q=gorila"));
        assertEquals(GoogleImages.Blocker.VERIFICATION, GoogleImages.blocker("https://www.google.com/sorry/index?continue=x"));
        assertEquals(GoogleImages.Blocker.CONSENT, GoogleImages.blocker("https://consent.google.com/ml?continue=x"));
        assertEquals(GoogleImages.Blocker.OTHER, GoogleImages.blocker("https://example.org/"));
    }

    @Test public void marksOnlyANumberedThumbnail() {
        String js = GoogleImages.markJs(3, true);
        assertTrue(js.contains("data-salve-idx=\"3\""));
        assertTrue(GoogleImages.markJs(-5, false).contains("data-salve-idx=\"0\""));
        assertTrue(GoogleImages.COLLECT_JS.startsWith("(function(){"));
        assertFalse("no ejecuta nada de la página", GoogleImages.COLLECT_JS.contains("eval("));
        assertEquals("ascii", GoogleImages.COLLECT_JS, new String(GoogleImages.COLLECT_JS.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII));
    }
}
