package salve.sketch;

import static org.junit.Assert.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Sus propios vectores (Bézier), formas orgánicas y referencias que no dependen de una sola fuente. */
public class VectorDrawingTest {
    private static float[] circle(int n) {
        float[] p = new float[2 * (n + 1)];
        for (int i = 0; i <= n; i++) {
            double a = 2 * Math.PI * i / n;
            p[2 * i] = 500 + 300 * (float) Math.cos(a); p[2 * i + 1] = 500 + 300 * (float) Math.sin(a);
        }
        return p;
    }

    @Test public void fitsFewCurvesThatFollowTheContour() {
        float[] c = circle(200);
        BezierSketch v = BezierSketch.fit(Collections.singletonList(c), 4f);
        assertTrue("un círculo de 200 puntos cabe en pocas curvas: " + v.segments(), v.segments() >= 2 && v.segments() <= 12);
        for (float[] s : v.sample(12)) for (int i = 0; i < s.length; i += 2) {
            double r = Math.hypot(s[i] - 500, s[i + 1] - 500);
            assertEquals("se queda sobre el contorno", 300, r, 8);
        }
        assertTrue(v.describe().contains("curvas de Bézier"));
    }

    @Test public void variationMovesShapeButKeepsLinesJoined() {
        BezierSketch v = BezierSketch.fit(Collections.singletonList(circle(120)), 4f);
        BezierSketch w = v.vary(7, 40);
        assertEquals(v.segments(), w.segments());
        boolean moved = false;
        for (List<float[]> line : w.curves) for (int i = 1; i < line.size(); i++) {
            float[] a = line.get(i - 1), b = line.get(i);
            assertEquals(a[6], b[0], 1e-3); assertEquals(a[7], b[1], 1e-3);
        }
        float[] before = v.curves.get(0).get(0), after = w.curves.get(0).get(0);
        for (int i = 0; i < 8; i++) if (Math.abs(before[i] - after[i]) > 1) moved = true;
        assertTrue(moved);
    }

    @Test public void organicShapesInHerGrammar() {
        ShapeProgram.Result r = ShapeProgram.parse("blob 40 55 52 48 64 56 66 78 58 92 40 92 32 78\n"
                + "curve 62 88 80 88 86 70 78 58\nline 50 37 52 37");
        assertEquals(3, r.shapes);
        assertTrue(r.isUsable());
        assertTrue(ShapeProgram.prompt("gorila").contains("blob"));
        assertNull(ShapeProgram.shape("curve", new float[]{1, 2, 3}));
    }

    @Test public void referencesFallBackWhenCommonsGivesNothing() throws Exception {
        List<String> asked = java.util.Collections.synchronizedList(new ArrayList<>());
        ImageReferenceSearch search = new ImageReferenceSearch((url, max) -> {
            asked.add(url);
            String body;
            if (url.contains("en.wikipedia.org") && url.contains("pageimages"))
                body = "{\"query\":{\"pages\":[{\"title\":\"Gorilla\",\"thumbnail\":{\"source\":\"https://upload.wikimedia.org/g.jpg\"}}]}}";
            else if (url.contains("api.openverse.org"))
                body = "{\"results\":[{\"title\":\"Gorilla in the zoo\",\"thumbnail\":\"https://api.openverse.org/v1/images/abc/thumb/\"},"
                        + "{\"title\":\"Gorilla sketch\",\"thumbnail\":\"https://other.example/x.jpg\"},"
                        + "{\"title\":\"router\",\"thumbnail\":\"https://192.168.1.1/x.jpg\"}]}";
            else body = "{\"batchcomplete\":true}"; // Commons sin resultados
            return body.getBytes(StandardCharsets.UTF_8);
        });
        List<ImageReferenceSearch.Candidate> found = search.search("gorila", "gorilla", 6, null);
        assertEquals("otro servidor público vale; la red de casa no", 3, found.size());
        assertEquals("https://upload.wikimedia.org/g.jpg", found.get(0).imageUrl);
        assertTrue(found.get(1).imageUrl.startsWith("https://api.openverse.org/"));
        assertEquals("https://other.example/x.jpg", found.get(2).imageUrl);
        assertTrue(search.diagnostic().contains("Wikipedia en «gorilla»: 1"));
        assertTrue(ImageReferenceSearch.allowedImage("https://api.openverse.org/v1/images/abc/thumb/"));
        assertTrue(ImageReferenceSearch.allowedImage("https://other.example/x.jpg"));
        assertFalse(ImageReferenceSearch.allowedImage("https://192.168.1.1/x.jpg"));
        assertEquals(" (<html>Too many requests</html>)", ImageReferenceSearch.snippet("<html>Too many requests</html>"));
    }
}
