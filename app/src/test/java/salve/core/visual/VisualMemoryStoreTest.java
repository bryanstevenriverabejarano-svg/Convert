package salve.core.visual;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class VisualMemoryStoreTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();
    private static final byte[] IMAGE = new byte[]{(byte) 0xff, (byte) 0xd8, 1, 2, 3};

    @Test public void restartKeepsPhotoQuestionAnalysisAndConfirmedIdentityTogether() throws Exception {
        File directory = temporary.newFolder();
        VisualMemoryStore store = new VisualMemoryStore(directory);
        VisualMemoryRecord photo = VisualMemoryRecord.create("¿Qué llevo puesto?", "Bryan", "yo", "única persona", true);
        store.save(photo, IMAGE);
        store.save(photo.withAnalysis("Camiseta azul"), null);
        VisualMemoryStore restarted = new VisualMemoryStore(directory);
        VisualMemoryRecord recovered = restarted.recall("¿Recuerdas la foto de Bryan?");
        assertNotNull(recovered);
        assertEquals("¿Qué llevo puesto?", recovered.question);
        assertEquals("Camiseta azul", recovered.analysis);
        assertEquals("yo", recovered.relationship);
        assertTrue(recovered.localOnly);
        assertArrayEquals(IMAGE, Files.readAllBytes(restarted.imageFile(photo.id).toPath()));
    }

    @Test public void modelAnalysisCannotSetOrOverwriteAnIdentity() {
        VisualMemoryRecord unknown = VisualMemoryRecord.create("Mira mi foto", "", "", "", false);
        assertEquals("", unknown.withAnalysis("Parece Bryan").personName);
        VisualMemoryRecord identified = unknown.identify("Diego", "mi hermano", "izquierda");
        assertEquals("Diego", identified.withAnalysis("Creo que es Bryan").personName);
        assertTrue(identified.memoryText().contains("declarada por el usuario"));
    }

    @Test public void failedAnalysisIsNotAVisualDescription() {
        VisualMemoryRecord photo = VisualMemoryRecord.create("Describe", "", "", "", false);
        assertTrue(photo.memoryText().contains("aún no hay un análisis válido"));
        assertFalse(photo.memoryText().contains("He visto"));
    }

    @Test public void outOfOrderCloudEventsCannotUndoCorrections() throws Exception {
        VisualMemoryStore store = new VisualMemoryStore(temporary.newFolder());
        VisualMemoryRecord original = VisualMemoryRecord.create("Foto", "Bryan", "yo", "centro", false);
        store.save(original, IMAGE);
        VisualMemoryRecord corrected = original.withAnalysis("Camiseta azul").identify("Diego", "hermano", "centro");
        store.save(corrected, null);
        assertEquals("Diego", store.save(original, IMAGE).personName);
        assertEquals("Diego", store.read(original.id).personName);
        assertEquals(1, store.list().size());
    }

    @Test public void ambiguousNamedPhotosRequireSelectionAndLatestIsExplicit() throws Exception {
        VisualMemoryStore store = new VisualMemoryStore(temporary.newFolder());
        VisualMemoryRecord first = new VisualMemoryRecord("10000000-0000-0000-0000-000000000001", 1000, 1,
                "Primera", "", "Bryan", "yo", "solo", false);
        VisualMemoryRecord second = new VisualMemoryRecord("10000000-0000-0000-0000-000000000002", 2000, 1,
                "Segunda", "", "Bryan", "yo", "solo", false);
        store.save(first, IMAGE); store.save(second, IMAGE);
        assertNull(store.recall("la foto de Bryan"));
        assertEquals(second.id, store.recall("¿Recuerdas la última foto?").id);
        assertNull(store.recall("la foto de otra persona"));
        assertNull(store.recall("¿Qué hora es?"));
    }

    @Test public void identityRemovalKeepsImageAndObservation() throws Exception {
        VisualMemoryStore store = new VisualMemoryStore(temporary.newFolder());
        VisualMemoryRecord photo = VisualMemoryRecord.create("Foto", "Bryan", "yo", "solo", true).withAnalysis("Camiseta azul");
        store.save(photo, IMAGE);
        VisualMemoryRecord cleared = store.save(photo.identify("", "relación antigua", "centro"), null);
        assertEquals("", cleared.personName); assertEquals("", cleared.relationship); assertEquals("", cleared.position);
        assertEquals("Camiseta azul", cleared.analysis); assertTrue(store.imageFile(photo.id).isFile());
    }

    @Test public void restoringMissingPixelsKeepsNewerIdentityMetadata() throws Exception {
        VisualMemoryStore store = new VisualMemoryStore(temporary.newFolder());
        VisualMemoryRecord original = VisualMemoryRecord.create("Foto", "Bryan", "yo", "solo", false);
        store.save(original, IMAGE);
        store.save(original.identify("Diego", "hermano", "solo"), null);
        assertTrue(store.imageFile(original.id).delete());
        assertEquals("Diego", store.save(original, IMAGE).personName);
        assertArrayEquals(IMAGE, Files.readAllBytes(store.imageFile(original.id).toPath()));
    }

    @Test public void singlePhotoMustNotAnswerAReferenceToAnUnknownPerson() throws Exception {
        VisualMemoryStore store = new VisualMemoryStore(temporary.newFolder());
        store.save(VisualMemoryRecord.create("Foto", "Bryan", "yo", "solo", false), IMAGE);
        assertNull(store.recall("¿Recuerdas la foto de María?"));
        assertNull(store.recall("¿La última foto de María?"));
        assertNotNull(store.recall("¿Recuerdas mi foto?"));
    }

    @Test public void corruptMetadataDoesNotHideOtherPhotos() throws Exception {
        File directory = temporary.newFolder(); VisualMemoryStore store = new VisualMemoryStore(directory);
        VisualMemoryRecord photo = VisualMemoryRecord.create("Foto", "", "", "", false);
        store.save(photo, IMAGE);
        Files.write(new File(directory, "10000000-0000-0000-0000-000000000002.json").toPath(),
                "{broken".getBytes(StandardCharsets.UTF_8));
        assertEquals(1, store.list().size());
    }

    @Test(expected = IllegalArgumentException.class) public void rejectsTraversalPaths() throws Exception {
        new VisualMemoryStore(temporary.newFolder()).imageFile("../../private");
    }

    @Test(expected = java.io.IOException.class) public void refusesOversizePhotoWithoutWritingIt() throws Exception {
        VisualMemoryStore store = new VisualMemoryStore(temporary.newFolder());
        store.save(VisualMemoryRecord.create("", "", "", "", false), new byte[VisualMemoryStore.MAX_IMAGE_BYTES + 1]);
    }
}
