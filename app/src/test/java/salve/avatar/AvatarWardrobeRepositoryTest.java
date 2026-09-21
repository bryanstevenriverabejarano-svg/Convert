package salve.avatar;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import static org.junit.Assert.*;

public final class AvatarWardrobeRepositoryTest {
    @Rule public TemporaryFolder temp = new TemporaryFolder();
    private AvatarDesignRequest design(String name, boolean wear) {
        return AvatarDesignRequest.create(name, "pajamas", AvatarDesignSpec.Palette.LAVENDER, AvatarDesignSpec.Pattern.STARS, wear);
    }
    @Test public void createWearAndRestartRecoverTheExactRecipe() throws Exception {
        File file = new File(temp.getRoot(), "wardrobe.json");
        AvatarWardrobeRepository store = new AvatarWardrobeRepository(file);
        AvatarDesignSpec created = store.create(design("Noche", true));
        AvatarWardrobeRepository restarted = new AvatarWardrobeRepository(file);
        assertEquals(created.id, restarted.selected().id);
        assertEquals("pajamas", restarted.selected().template);
        assertEquals(AvatarDesignSpec.Palette.LAVENDER, restarted.selected().palette);
        assertEquals(AvatarDesignSpec.Pattern.STARS, restarted.selected().pattern);
        assertEquals(2, restarted.list().size());
    }
    @Test public void creatingWithoutWearingPreservesSelection() throws Exception {
        AvatarWardrobeRepository store = new AvatarWardrobeRepository(new File(temp.getRoot(), "wardrobe.json"));
        AvatarDesignSpec first = store.create(design("Uno", true)); store.create(design("Dos", false));
        assertEquals(first.id, store.selected().id);
        store.select("original"); assertEquals("original", store.selected().id);
        store.select(first.id); assertEquals(first.id, store.selected().id);
    }
    @Test public void deletingSelectedDesignReturnsToOriginalAcrossRestart() throws Exception {
        File file = new File(temp.getRoot(), "wardrobe.json"); AvatarWardrobeRepository store = new AvatarWardrobeRepository(file);
        AvatarDesignSpec removed = store.create(design("Borrar", true)); store.delete(removed.id);
        AvatarWardrobeRepository restarted = new AvatarWardrobeRepository(file);
        assertEquals("original", restarted.selected().id); assertEquals(1, restarted.list().size());
    }
    @Test public void originalAndUnknownSelectionCannotBeDeletedOrSubstituted() throws Exception {
        AvatarWardrobeRepository store = new AvatarWardrobeRepository(new File(temp.getRoot(), "wardrobe.json"));
        try { store.delete("original"); fail(); } catch (IllegalArgumentException expected) { }
        try { store.select("00000000-0000-0000-0000-000000000000"); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals("original", store.selected().id);
    }
    @Test public void duplicateNamesDoNotOverwriteExistingWork() throws Exception {
        AvatarWardrobeRepository store = new AvatarWardrobeRepository(new File(temp.getRoot(), "wardrobe.json"));
        AvatarDesignSpec first = store.create(design("Café", true));
        try { store.create(design(" CAFE\u0301 ", true)); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals(first.id, store.selected().id); assertEquals(2, store.list().size());
    }
    @Test public void quotaIsEnforcedAndDeletionFreesCapacity() throws Exception {
        AvatarWardrobeRepository store = new AvatarWardrobeRepository(new File(temp.getRoot(), "wardrobe.json"));
        AvatarDesignSpec first = null;
        for (int i = 0; i < AvatarWardrobeRepository.MAX_DESIGNS; i++) {
            AvatarDesignSpec created = store.create(design("Diseño " + i, false)); if (first == null) first = created;
        }
        try { store.create(design("Extra", true)); fail(); } catch (IllegalArgumentException expected) { }
        assertEquals("original", store.selected().id);
        store.delete(first.id); store.create(design("Extra", true));
        assertEquals(AvatarWardrobeRepository.MAX_DESIGNS + 1, store.list().size());
    }
    @Test public void failedDiskWriteDoesNotAnnounceOrPublishANewDesign() throws Exception {
        File parent = new File(temp.getRoot(), "blocked"); File file = new File(parent, "wardrobe.json");
        AvatarWardrobeRepository store = new AvatarWardrobeRepository(file);
        Files.write(parent.toPath(), new byte[]{1});
        try { store.create(design("No guardado", true)); fail(); } catch (IOException expected) { }
        assertEquals(1, store.list().size()); assertEquals("original", store.selected().id);
    }
    @Test public void corruptPersistedFileIsPreservedForRecovery() throws Exception {
        File file = new File(temp.getRoot(), "wardrobe.json");
        String damaged = "{\"version\":1,\"selected\":\"original\",\"designs\":[],\"extra\":true}";
        Files.write(file.toPath(), damaged.getBytes(StandardCharsets.UTF_8));
        try { new AvatarWardrobeRepository(file); fail(); } catch (IOException expected) { }
        assertEquals(damaged, new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }
}
