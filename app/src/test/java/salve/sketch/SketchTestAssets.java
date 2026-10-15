package salve.sketch;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

final class SketchTestAssets {
    private static SketchLibrary library;
    private SketchTestAssets() { }

    static String read(String asset) throws Exception {
        Path p = Paths.get("src/main/assets/" + asset);
        if (!Files.exists(p)) p = Paths.get("app/src/main/assets/" + asset);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    static synchronized SketchLibrary library() throws Exception {
        if (library == null) library = SketchLibrary.parse(read("sketch/library.json"));
        return library;
    }
}
