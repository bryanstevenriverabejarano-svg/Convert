package salve.core;

import org.junit.Test;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class LLMCoderTest {
    @Test public void localCodeNeverCallsCloudAndMissingSourceNeverInvokesModel() {
        AtomicInteger localCalls = new AtomicInteger();
        LLMCoder coder = new LLMCoder(() -> true,
                prompt -> { fail("No cloud calls in local mode"); return null; },
                prompt -> { localCalls.incrementAndGet(); return ModelResult.success("```java\nclass X {}\n```", 1); });
        assertEquals("class X {}", coder.generateCode("Crea X", "Java"));
        assertTrue(coder.generateFix("Revisa X", "X").contains("falta el código fuente exacto"));
        assertEquals(1, localCalls.get());
    }

    @Test public void failedLocalInferenceDoesNotBecomeCodeOrUseCloud() {
        LLMCoder coder = new LLMCoder(() -> true,
                prompt -> { fail("No cloud fallback"); return null; },
                prompt -> ModelResult.failure(ModelResult.Status.ERROR, "Native error", 1));
        String result = coder.generateCode("Crea X", "Java");
        assertTrue(result.startsWith("// No se pudo"));
        assertFalse(result.contains("Native error"));
    }

    @Test public void providerChoiceIsReadForEachRequest() {
        AtomicBoolean localMode = new AtomicBoolean(false);
        LLMCoder coder = new LLMCoder(localMode::get,
                prompt -> ModelResult.success("cloud", 1),
                prompt -> ModelResult.success("local", 1));
        assertEquals("cloud", coder.generateCode("Crea X", "Java"));
        localMode.set(true);
        assertEquals("local", coder.generateCode("Crea X", "Java"));
    }

    @Test public void unreadablePreferenceDoesNotTransmitPrompt() {
        LLMCoder coder = new LLMCoder(() -> { throw new IllegalStateException("Preference unavailable"); },
                prompt -> { fail("Unknown policy cannot authorize cloud"); return null; },
                prompt -> { fail("No routing without policy"); return null; });
        assertTrue(coder.generateFix("Revisa X", "X").startsWith("// No se pudo"));
    }

    @Test public void blankOutputAndCloudCancellationAreNotActionableCode() {
        LLMCoder blank = new LLMCoder(() -> true, null,
                prompt -> ModelResult.success("```java\n\n```", 1));
        assertTrue(blank.generateCode("Crea X", "Java").startsWith("// No se pudo"));
        LLMCoder cancelled = new LLMCoder(() -> false,
                prompt -> ModelResult.failure(ModelResult.Status.CANCELLED, "Stopped", 1),
                prompt -> { fail("Cancellation stops generation"); return null; });
        assertTrue(cancelled.generateFix("Revisa X", "X", snapshot()).startsWith("// No se pudo"));
    }

    @Test public void fixIncludesExactSourceIdentityAndOnlyReturnsTargetDiff() {
        AtomicReference<String> prompt = new AtomicReference<>();
        LLMCoder coder = new LLMCoder(() -> true,
                value -> { fail("No cloud in local mode"); return null; },
                value -> { prompt.set(value); return ModelResult.success(diff("X"), 1); });
        AutoImprovementSourceSnapshot snapshot = snapshot();
        assertEquals(diff("X"), coder.generateFix("Reduce los parámetros de f", "X", snapshot));
        assertTrue(prompt.get().contains(snapshot.source("X").code));
        assertTrue(prompt.get().contains(snapshot.source("X").sha256));
        assertTrue(prompt.get().contains(snapshot.revision));
    }

    @Test public void missingContextAndExcessiveDiagnosisNeverCallProvider() {
        LLMCoder coder = new LLMCoder(() -> true, null,
                prompt -> { fail("No generation without bounded context"); return null; });
        assertTrue(coder.generateFix("Revisa", "Missing", snapshot()).startsWith("// No se pudo"));
        assertTrue(coder.generateFix("x".repeat(1001), "X", snapshot()).startsWith("// No se pudo"));
    }

    @Test public void malformedOrDifferentTargetOutputIsNotAnActionableFix() {
        for (String output : Arrays.asList("class X {}", diff("Other"), diff("X") + diff("Other"), "diff --git a/x b/x")) {
            LLMCoder coder = new LLMCoder(() -> true, null, prompt -> ModelResult.success(output, 1));
            assertTrue(coder.generateFix("Revisa", "X", snapshot()).startsWith("// No se pudo"));
        }
    }

    @Test public void returnedDiffCanActuallyBeAppliedToItsExactSource() throws Exception {
        java.nio.file.Path root = java.nio.file.Files.createTempDirectory("salve-diff-check");
        try {
            assertEquals(0, new ProcessBuilder("git", "init", "--quiet").directory(root.toFile()).start().waitFor());
            java.nio.file.Path file = root.resolve("app/src/main/java/salve/core/X.java");
            java.nio.file.Files.createDirectories(file.getParent());
            java.nio.file.Files.write(file, snapshot().source("X").code.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            LLMCoder coder = new LLMCoder(() -> true, null, prompt -> ModelResult.success(diff("X").trim(), 1));
            java.nio.file.Path patch = root.resolve("change.patch");
            java.nio.file.Files.write(patch, coder.generateFix("Haz la clase final", "X", snapshot()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            Process applied = new ProcessBuilder("git", "apply", patch.toString()).directory(root.toFile()).redirectErrorStream(true).start();
            String output = new String(applied.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            assertEquals(output, 0, applied.waitFor());
            assertEquals("package salve.core;\nfinal class X {}\n", new String(java.nio.file.Files.readAllBytes(file), java.nio.charset.StandardCharsets.UTF_8));
        } finally {
            try (java.util.stream.Stream<java.nio.file.Path> paths = java.nio.file.Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    private static String diff(String name) {
        String path = "app/src/main/java/salve/core/" + name + ".java";
        return "diff --git a/" + path + " b/" + path + "\n--- a/" + path + "\n+++ b/" + path
                + "\n@@ -1,2 +1,2 @@\n package salve.core;\n-class X {}\n+final class X {}\n";
    }

    private static AutoImprovementSourceSnapshot snapshot() {
        String source = "package salve.core;\nclass X {}\n";
        Map<String, Object> file = new LinkedHashMap<>();
        file.put("path", "app/src/main/java/salve/core/X.java");
        file.put("source", source); file.put("sha256", AutoImprovementSourceSnapshot.hash(source));
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("schemaVersion", 1); root.put("revision", "a".repeat(40)); root.put("files", Arrays.asList(file));
        return AutoImprovementSourceSnapshot.parse(new Gson().toJson(root));
    }
}
