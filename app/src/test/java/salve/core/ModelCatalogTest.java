package salve.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

public class ModelCatalogTest {
    private static final Set<ModelCatalog.Capability> TEXT = EnumSet.of(ModelCatalog.Capability.TEXT);
    private static final Set<ModelCatalog.Capability> VISION = EnumSet.of(ModelCatalog.Capability.VISION);

    private JsonObject bundled() throws Exception {
        File file = new File("src/main/assets/config/models.json");
        if (!file.isFile()) file = new File("app/src/main/assets/config/models.json");
        return JsonParser.parseString(new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
    }

    private ModelCatalog read(String json) {
        return ModelCatalog.read(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
    }

    private ModelCatalog read(JsonObject json) { return read(json.toString()); }
    private JsonObject first(JsonObject root) { return root.getAsJsonArray("items").get(0).getAsJsonObject(); }

    /** Synthetic entries are fixtures only; they never enter the APK catalog or trigger a download. */
    private JsonObject pair() throws Exception {
        JsonObject root = bundled();
        JsonObject second = first(root).deepCopy();
        second.addProperty("id", "Synthetic text fixture");
        second.addProperty("filename", "synthetic-fixture.litertlm");
        second.addProperty("supportsVision", false);
        root.getAsJsonArray("items").add(second);
        return root;
    }

    @Test public void preservesPublishedFieldsWithoutInventingUnknownMetadata() throws Exception {
        ModelCatalog.Entry entry = read(bundled()).getEntries().get(0);
        assertEquals("Gemma 4 E2B", entry.id);
        assertEquals(entry.id, entry.name);
        assertEquals("Apache-2.0", entry.license);
        assertEquals("https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm", entry.modelCard);
        assertEquals("6e5c4f1e395deb959c494953478fa5cec4b8008f", entry.revision);
        assertNull(entry.version); assertNull(entry.provider); assertNull(entry.location);
        assertNull(entry.contextTokens); assertNull(entry.ramBytes); assertNull(entry.vramBytes);
        assertNull(entry.tokensPerSecond); assertNull(entry.costPerMillionTokens);
        assertNull(entry.cpu); assertNull(entry.gpu);
        assertTrue(entry.specialties.isEmpty()); assertTrue(entry.limitations.isEmpty());
    }

    @Test public void preservesExplicitMetadataWithoutUsingItAsPerformanceEvidence() throws Exception {
        JsonObject root = bundled(), item = first(root);
        item.addProperty("version", "test-version"); item.addProperty("provider", "test-publisher");
        item.addProperty("location", "local"); item.addProperty("contextTokens", 2048);
        item.addProperty("ramBytes", 4096); item.addProperty("vramBytes", 0);
        item.addProperty("tokensPerSecond", 1.25); item.addProperty("costPerMillionTokens", 0);
        item.addProperty("cpu", "declared-test-cpu"); item.addProperty("gpu", "declared-test-gpu");
        item.add("specialties", JsonParser.parseString("[\"test-specialty\"]"));
        item.add("limitations", JsonParser.parseString("[\"test-limit\"]"));
        ModelCatalog catalog = read(root);
        ModelCatalog.Entry entry = catalog.getEntries().get(0);
        assertEquals("test-publisher", entry.provider); assertEquals(Long.valueOf(2048), entry.contextTokens);
        assertEquals(Long.valueOf(0), entry.vramBytes); assertEquals(Double.valueOf(1.25), entry.tokensPerSecond);
        assertEquals(Collections.singletonList("test-limit"), entry.limitations);
        assertNull(catalog.selectReady(TEXT, Collections.emptyMap(), entry.id));
    }

    @Test public void rejectsDuplicateIdsNamesAndFilenamesIncludingCaseVariants() throws Exception {
        for (String field : new String[]{"id", "name", "filename"}) {
            JsonObject root = pair();
            JsonObject original = first(root), duplicate = root.getAsJsonArray("items").get(1).getAsJsonObject();
            if (field.equals("name")) original.addProperty("name", "Shared name");
            duplicate.addProperty(field, original.get(field).getAsString().toUpperCase(java.util.Locale.ROOT));
            assertThrows(IllegalArgumentException.class, () -> read(root));
        }
    }

    @Test public void rejectsDuplicateJsonKeysInsteadOfSilentlyOverwritingThem() throws Exception {
        String root = bundled().toString();
        assertThrows(IllegalArgumentException.class, () -> read(root.replace("\"items\":", "\"items\":[],\"items\":")));
        assertThrows(IllegalArgumentException.class, () -> read(root.replace("\"id\":", "\"id\":\"shadow\",\"id\":")));
    }

    @Test public void rejectsCoercedOrFractionalSizeAndBooleanClaims() throws Exception {
        for (String invalid : new String[]{"\"2588147712\"", "2588147712.5", "0", "-1", "18446744073709551616", "null", "true"}) {
            JsonObject root = bundled(); first(root).add("sizeBytes", JsonParser.parseString(invalid));
            assertThrows(invalid, IllegalArgumentException.class, () -> read(root));
        }
        for (String invalid : new String[]{"\"true\"", "1", "null", "[]"}) {
            JsonObject root = bundled(); first(root).add("supportsVision", JsonParser.parseString(invalid));
            assertThrows(invalid, IllegalArgumentException.class, () -> read(root));
        }
    }

    @Test public void rejectsIncompleteMalformedAndTrailingCatalogContent() throws Exception {
        for (String invalid : new String[]{"{}", "[]", "{\"items\":[]}", "{\"items\":[null]}", "{items: []}", "{\"items\":[],}", bundled() + "{}"}) {
            assertThrows(IllegalArgumentException.class, () -> read(invalid));
        }
        for (String field : new String[]{"id", "url", "filename", "sha256", "sizeBytes"}) {
            JsonObject root = bundled(); first(root).remove(field);
            assertThrows(field, IllegalArgumentException.class, () -> read(root));
        }
    }

    @Test public void rejectsAmbiguousIdentityAndUnsafeArtifactLocations() throws Exception {
        for (String id : new String[]{"", " Gemma", "Gemma ", "Gemma\nshadow"}) {
            JsonObject root = bundled(); first(root).addProperty("id", id);
            assertThrows(IllegalArgumentException.class, () -> read(root));
        }
        for (String filename : new String[]{"../model.litertlm", "/model.litertlm", "model.task", "model.litertlm.part"}) {
            JsonObject root = bundled(); first(root).addProperty("filename", filename);
            assertThrows(IllegalArgumentException.class, () -> read(root));
        }
        String url = first(bundled()).get("url").getAsString();
        for (String changed : new String[]{url.replace("6e5c4f1e395deb959c494953478fa5cec4b8008f", "main"),
                url.replace("huggingface.co", "huggingface.co.evil.test"), url.replace("?download=true", "#file"),
                url.replace("gemma-4-E2B-it.litertlm", "../model.litertlm")}) {
            JsonObject root = bundled(); first(root).addProperty("url", changed);
            assertThrows(IllegalArgumentException.class, () -> read(root));
        }
    }

    @Test public void unknownAndInstalledButUnverifiedModelsAreNeverSelected() throws Exception {
        ModelCatalog catalog = read(pair());
        Map<String, Set<ModelCatalog.Capability>> ready = new HashMap<>();
        ready.put("not-in-catalog", EnumSet.allOf(ModelCatalog.Capability.class));
        ready.put(catalog.getEntries().get(0).id, Collections.emptySet());
        assertNull(catalog.selectReady(TEXT, ready, "not-in-catalog"));
        assertNull(catalog.selectReady(VISION, ready, null));
    }

    @Test public void textInferenceDoesNotVerifyDeclaredVision() throws Exception {
        ModelCatalog catalog = read(bundled());
        ModelCatalog.Entry item = catalog.getEntries().get(0);
        Map<String, Set<ModelCatalog.Capability>> ready = Collections.singletonMap(item.id, TEXT);
        assertSame(item, catalog.selectReady(TEXT, ready, null));
        assertNull(catalog.selectReady(VISION, ready, null));
    }

    @Test public void selectionRequiresBothRuntimeEvidenceAndDeclaredCapability() throws Exception {
        ModelCatalog catalog = read(pair());
        String textOnly = catalog.getEntries().get(1).id;
        assertNull(catalog.selectReady(VISION, Collections.singletonMap(textOnly,
                EnumSet.allOf(ModelCatalog.Capability.class)), textOnly));
    }

    @Test public void preferredModelWinsOnlyWhenCompatibleAndVerified() throws Exception {
        ModelCatalog catalog = read(pair());
        ModelCatalog.Entry visual = catalog.getEntries().get(0), textOnly = catalog.getEntries().get(1);
        Map<String, Set<ModelCatalog.Capability>> ready = new HashMap<>();
        ready.put(visual.id, EnumSet.allOf(ModelCatalog.Capability.class)); ready.put(textOnly.id, TEXT);
        assertSame(textOnly, catalog.selectReady(TEXT, ready, textOnly.id));
        assertSame(visual, catalog.selectReady(VISION, ready, textOnly.id));
        ready.remove(textOnly.id);
        assertSame(visual, catalog.selectReady(TEXT, ready, textOnly.id));
    }

    @Test public void downloadSelectsOneExplicitCompatibleArtifactAndRejectsUnknownIds() throws Exception {
        ModelCatalog catalog = read(pair());
        ModelCatalog.Entry first = catalog.getEntries().get(0), second = catalog.getEntries().get(1);
        assertSame(first, catalog.selectDownload(TEXT, null));
        assertSame(second, catalog.selectDownload(TEXT, second.id));
        assertNull(catalog.selectDownload(VISION, second.id));
        assertThrows(IllegalArgumentException.class, () -> catalog.selectDownload(TEXT, "ghost-model"));
        assertThrows(IllegalArgumentException.class, () -> catalog.selectDownload(Collections.emptySet(), null));
    }

    @Test public void catalogOrderIsNotReplacedByAnInventedLargerModelRanking() throws Exception {
        JsonObject root = pair();
        root.getAsJsonArray("items").get(1).getAsJsonObject().addProperty("sizeBytes", 9_000_000_000L);
        ModelCatalog catalog = read(root);
        assertSame(catalog.getEntries().get(0), catalog.selectDownload(TEXT, null));
    }

    @Test public void callerCannotAlterCatalogOrVerifiedSnapshot() throws Exception {
        ModelCatalog catalog = read(bundled());
        assertThrows(UnsupportedOperationException.class, () -> catalog.getEntries().clear());
        assertThrows(UnsupportedOperationException.class, () -> catalog.getEntries().get(0).declaredCapabilities.clear());
        Set<ModelCatalog.Capability> original = EnumSet.of(ModelCatalog.Capability.TEXT);
        ModelCatalog.RuntimeSnapshot snapshot = new ModelCatalog.RuntimeSnapshot("/model.litertlm", original);
        original.add(ModelCatalog.Capability.VISION);
        assertEquals(TEXT, snapshot.verifiedCapabilities);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.verifiedCapabilities.clear());
    }
}
