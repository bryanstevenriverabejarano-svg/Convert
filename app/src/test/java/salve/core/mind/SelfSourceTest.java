package salve.core.mind;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;

public class SelfSourceTest {
    static final String INDEX = "{\"version\":1,\"files\":["
            + "{\"p\":\"salve/core/SalveLLM.java\",\"l\":12,\"d\":\"\",\"m\":[]},"
            + "{\"p\":\"salve/core/memory/Store.java\",\"l\":3,\"d\":\"\",\"m\":[]},"
            + "{\"p\":\"salve/data/Store.java\",\"l\":3,\"d\":\"\",\"m\":[]}]}";
    static final String LLM = "package salve.core;\n\n/** Motor. */\npublic final class SalveLLM {\n"
            + "    private int x;\n\n    /** Genera texto. */\n    public ModelResult generateResult(String p) {\n"
            + "        return run(p);\n    }\n}\n";

    private static final Map<String, String> FILES = new HashMap<>();
    static { FILES.put("salve/core/SalveLLM.java", LLM); }
    private static String read(String file, int from) {
        return SelfSource.read(SelfCode.parse(INDEX), file, from, FILES::get);
    }

    @Test public void readsRealNumberedLinesByClassName() {
        String r = read("SalveLLM", 1);
        assertTrue(r.startsWith("TU_CODIGO_REAL salve/core/SalveLLM.java"));
        assertTrue(r.contains("    4| public final class SalveLLM {"));
        assertTrue(r.contains("Líneas 1–12 de 12."));
        assertFalse(r.contains("Para seguir"));
        assertTrue(read("app/src/main/java/salve/core/SalveLLM.java", 1).contains("SalveLLM"));
    }

    @Test public void jumpsToAMethodWithItsComment() {
        String r = read("SalveLLM#generateResult", 1);
        assertTrue(r.contains("    6| "));
        assertTrue(r.contains("    8|     public ModelResult generateResult(String p) {"));
        assertFalse(r.contains("    4| "));
        assertTrue(read("SalveLLM#noExiste", 1).startsWith("NO_ENCONTRADO"));
    }

    @Test public void longFilesAreReadInPiecesThatSayWhereToContinue() {
        StringBuilder big = new StringBuilder();
        for (int i = 1; i <= 500; i++) big.append("        int linea").append(i).append(" = ").append(i).append(";\n");
        Map<String, String> files = new HashMap<>();
        files.put("salve/core/SalveLLM.java", big.toString());
        String r = SelfSource.read(SelfCode.parse(INDEX), "SalveLLM", 1, files::get);
        assertTrue(r.length() < SelfSource.MAX_CHARS + 200);
        assertTrue(r.contains("Para seguir: desde="));
        int next = Integer.parseInt(r.replaceAll("(?s).*desde=(\\d+).*", "$1"));
        String second = SelfSource.read(SelfCode.parse(INDEX), "SalveLLM", next, files::get);
        assertTrue(second.contains(String.format("%5d| ", next)));
        assertTrue(SelfSource.read(SelfCode.parse(INDEX), "SalveLLM", 9999, files::get).startsWith("FIN"));
    }

    @Test public void neverInventsWhatItCannotRead() {
        assertTrue(read("NoExiste", 1).startsWith("NO_ENCONTRADO"));
        String twice = read("Store", 1);
        assertTrue(twice.startsWith("VARIOS_ARCHIVOS"));
        assertTrue(twice.contains("salve/core/memory/Store.java") && twice.contains("salve/data/Store.java"));
        assertTrue(read("salve/data/Store.java", 1).startsWith("SIN_FUENTE"));
    }
}
