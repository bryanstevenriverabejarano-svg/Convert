package salve.core;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * El catálogo de descargas opcionales tiene que poder leerse entero: si una sola entrada es inválida,
 * fallan TODAS las descargas (le pasó el 4/11/2026 con los mmproj marcados como «con visión»).
 */
public class OptionalModelCatalogTest {
    private static File optional() {
        File file = new File("src/main/assets/config/models_optional.json");
        return file.isFile() ? file : new File("app/src/main/assets/config/models_optional.json");
    }

    @Test public void everyOptionalModelCanBeDownloaded() throws Exception {
        try (InputStream in = new FileInputStream(optional())) {
            ModelCatalog catalog = ModelCatalog.read(in);
            Set<String> ids = new HashSet<>();
            for (ModelCatalog.Entry entry : catalog.getEntries()) ids.add(entry.id);
            assertTrue(ids.contains(LocalModelPolicy.UNCENSORED));
            assertTrue(ids.contains(LocalModelPolicy.UNCENSORED_E2B));
            assertTrue(ids.contains(LocalModelPolicy.UNCENSORED_VISION));
            assertTrue(ids.contains(LocalModelPolicy.UNCENSORED_E2B_VISION));
            for (ModelCatalog.Entry entry : catalog.getEntries()) {
                if (LocalModelPolicy.isUncensored(entry.id))
                    assertEquals(LocalModelPolicy.uncensoredFile(entry.id), entry.filename);
            }
        }
    }
}
