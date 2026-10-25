package salve.core.selfpr;

import static org.junit.Assert.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

/** Salve escribe su propio parche: del texto de su modelo a un diff que el flujo de GitHub acepta. */
public class SelfPatchTest {
    static final String PATH = "app/src/main/java/salve/core/conversation/TurnChecks.java";
    static final String SOURCE = "package salve.core.conversation;\n"
            + "\n"
            + "final class TurnChecks {\n"
            + "    private TurnChecks() { }\n"
            + "\n"
            + "    static boolean returnsQuestion(String reply) {\n"
            + "        String t = reply == null ? \"\" : reply.trim();\n"
            + "        return t.endsWith(\"?\") && t.length() < 40;\n"
            + "    }\n"
            + "\n"
            + "    static boolean announcesWithoutDoing(String reply) {\n"
            + "        return reply != null && reply.startsWith(\"Voy a\");\n"
            + "    }\n"
            + "}\n";

    static final String MODEL_REPLY = "EXPLICACION: también detecto la pregunta devuelta cuando es larga.\n"
            + "BUSCAR:\n```java\n        return t.endsWith(\"?\") && t.length() < 40;\n```\n"
            + "REEMPLAZAR:\n```java\n        return t.endsWith(\"?\") && (t.length() < 40 || t.toLowerCase().contains(\"y a ti\"));\n```\n";

    @Test public void theModelsEditBecomesAnExactUnifiedDiff() throws Exception {
        SelfPatch.Reply reply = SelfPatch.parse(MODEL_REPLY);
        assertEquals("también detecto la pregunta devuelta cuando es larga.", reply.explanation);
        String changed = SelfPatch.apply(SOURCE, reply.edits);
        assertTrue(changed.contains("contains(\"y a ti\")"));
        assertNull(SelfPatch.syntaxProblem(SOURCE, changed));
        assertTrue(SelfPatch.risks(SOURCE, changed).isEmpty());
        String diff = SelfPatch.unifiedDiff(PATH, SOURCE, changed);
        assertEquals("diff --git a/" + PATH + " b/" + PATH + "\n"
                + "--- a/" + PATH + "\n"
                + "+++ b/" + PATH + "\n"
                + "@@ -5,7 +5,7 @@\n"
                + " \n"
                + "     static boolean returnsQuestion(String reply) {\n"
                + "         String t = reply == null ? \"\" : reply.trim();\n"
                + "-        return t.endsWith(\"?\") && t.length() < 40;\n"
                + "+        return t.endsWith(\"?\") && (t.length() < 40 || t.toLowerCase().contains(\"y a ti\"));\n"
                + "     }\n"
                + " \n"
                + "     static boolean announcesWithoutDoing(String reply) {\n", diff);
    }

    @Test public void indentationSlipsAreToleratedButContentIsNot() throws Exception {
        // Los modelos pequeños suelen perder la sangría al copiar.
        String sloppy = "BUSCAR:\n```java\nreturn reply != null && reply.startsWith(\"Voy a\");\n```\n"
                + "REEMPLAZAR:\n```java\nreturn reply != null && (reply.startsWith(\"Voy a\") || reply.startsWith(\"Ahora voy\"));\n```";
        String changed = SelfPatch.apply(SOURCE, SelfPatch.parse(sloppy).edits);
        assertTrue(changed.contains("        return reply != null && (reply.startsWith"));
        try {
            SelfPatch.apply(SOURCE, SelfPatch.parse("BUSCAR:\n```java\nreturn invented();\n```\nREEMPLAZAR:\n```java\nreturn x;\n```").edits);
            fail("no se puede cambiar código que no existe");
        } catch (SelfPatch.PatchError expected) { assertTrue(expected.getMessage().contains("no está en el archivo")); }
    }

    @Test public void ambiguousSearchesAreRejected() throws Exception {
        String twice = "BUSCAR:\n```java\n    }\n```\nREEMPLAZAR:\n```java\n    } // fin\n```";
        try { SelfPatch.apply(SOURCE, SelfPatch.parse(twice).edits); fail(); }
        catch (SelfPatch.PatchError expected) { assertTrue(expected.getMessage().contains("más de una vez")); }
    }

    @Test public void repliesWithoutEditsAreNotProposals() {
        for (String bad : new String[]{null, "", "Creo que deberías cambiar TurnChecks.", "BUSCAR:\n```java\n```\nREEMPLAZAR:\n```java\nx\n```"}) {
            try { SelfPatch.parse(bad); fail(String.valueOf(bad)); } catch (SelfPatch.PatchError expected) { }
        }
    }

    @Test public void unsafeOrBrokenChangesAreCaught() throws Exception {
        String exec = SelfPatch.apply(SOURCE, Collections.singletonList(new SelfPatch.Edit(
                "        return reply != null && reply.startsWith(\"Voy a\");",
                "        try { Runtime.getRuntime().exec(\"rm -rf /\"); } catch (Exception e) { }\n        return false;")));
        assertTrue(SelfPatch.risks(SOURCE, exec).contains("Runtime.getRuntime"));
        String net = SelfPatch.apply(SOURCE, Collections.singletonList(new SelfPatch.Edit(
                "        return reply != null && reply.startsWith(\"Voy a\");",
                "        return reply != null && reply.contains(\"https://evil.example\");")));
        assertFalse(SelfPatch.risks(SOURCE, net).isEmpty());
        String unbalanced = SelfPatch.apply(SOURCE, Collections.singletonList(new SelfPatch.Edit(
                "    static boolean announcesWithoutDoing(String reply) {", "    static boolean announcesWithoutDoing(String reply) { {")));
        assertEquals("llaves { } desparejadas", SelfPatch.syntaxProblem(SOURCE, unbalanced));
        assertNull("una llave dentro de un texto no cuenta", SelfPatch.syntaxProblem(SOURCE, SOURCE.replace("\"Voy a\"", "\"Voy a {\"")));
    }

    @Test public void bigRewritesAreRejected() throws Exception {
        StringBuilder huge = new StringBuilder();
        for (int i = 0; i < 70; i++) huge.append("        int x").append(i).append(" = ").append(i).append(";\n");
        String changed = SelfPatch.apply(SOURCE, Collections.singletonList(new SelfPatch.Edit(
                "        String t = reply == null ? \"\" : reply.trim();", huge + "        String t = reply == null ? \"\" : reply.trim();")));
        try { SelfPatch.unifiedDiff(PATH, SOURCE, changed); fail(); }
        catch (SelfPatch.PatchError expected) { assertTrue(expected.getMessage().contains("máximo")); }
    }

    @Test public void excerptFitsTheBudgetAndFindsTheRelevantCode() {
        StringBuilder big = new StringBuilder("package x;\nclass Big {\n");
        for (int i = 0; i < 200; i++) big.append("    int filler").append(i).append(" = ").append(i).append(";\n");
        big.append("    boolean devuelvePregunta(String reply) { return reply.endsWith(\"?\"); }\n");
        for (int i = 0; i < 200; i++) big.append("    int more").append(i).append(" = ").append(i).append(";\n");
        big.append("}\n");
        String excerpt = SelfPatch.excerpt(big.toString(), SelfPatch.keywords("no_devuelve_pregunta", null), 1500);
        assertTrue(excerpt.length() <= 1500);
        assertTrue(excerpt.contains("devuelvePregunta"));
        assertTrue("líneas completas", excerpt.endsWith(";\n") || excerpt.endsWith("}\n"));
    }

    @Test public void theProposalMatchesWhatTheGitHubRunnerValidates() throws Exception {
        String changed = SelfPatch.apply(SOURCE, SelfPatch.parse(MODEL_REPLY).edits);
        String revision = "0123456789abcdef0123456789abcdef01234567";
        JsonObject p = SelfPatch.proposal(PATH, revision, SOURCE, SelfPatch.unifiedDiff(PATH, SOURCE, changed),
                "Devuelve la pregunta (3 fallos)", true, true);
        assertEquals(3, p.get("schemaVersion").getAsInt());
        assertEquals("TurnChecks", p.get("targetClass").getAsString());
        assertTrue(p.get("proposalId").getAsString().matches("[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}"));
        assertEquals(SelfPatch.sha256(SOURCE), p.get("sourceSha256").getAsString());
        assertTrue(p.get("sourceSha256").getAsString().matches("[0-9a-f]{64}"));
        assertTrue(p.get("patch").getAsString().startsWith("diff --git "));

        String title = SelfPatch.issueTitle("Devuelve la pregunta en vez de responder", p);
        assertTrue(title.startsWith(SelfPatch.TITLE_PREFIX));
        String body = SelfPatch.issueBody("Devuelve la pregunta", "detecta preguntas largas",
                Arrays.asList("«¿qué te gusta?» → «¿y a ti?»"), "Gemma 4 E2B", p);
        int at = body.indexOf(SelfPatch.MARKER);
        assertTrue(at > 0);
        String json = body.substring(body.indexOf("```json\n", at) + 8, body.lastIndexOf("\n```"));
        assertEquals("el JSON de la issue es exactamente la propuesta", p, JsonParser.parseString(json).getAsJsonObject());
        assertTrue("Bryan ve el diff sin abrir el JSON", body.contains("```diff\n"));
    }

    @Test public void keywordsMixHintsAndEvidence() {
        List<String> k = SelfPatch.keywords("no_inventa_recuerdo", new ArrayList<>(Collections.singletonList("Inventó que fuimos a Bilbao")));
        assertTrue(k.contains("recuerdo"));
        assertTrue(k.contains("invento"));
    }
}
