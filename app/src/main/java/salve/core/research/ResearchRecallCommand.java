package salve.core.research;

import java.text.Normalizer;
import java.util.Locale;

/** Local recall is distinct from a request to refresh the web. */
public final class ResearchRecallCommand {
    private ResearchRecallCommand() { }
    public static boolean matches(String input) {
        String text = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT)
                .replaceAll("[¿?¡!.,:;]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "").replaceFirst(" por favor$", "");
        return text.matches("(?:(?:muestra(?:me)?|recupera|recuerda|cual fue|resultado de) )?"
                + "(?:(?:la|tu|mi) )?ultima (?:busqueda|investigacion)")
                || text.matches("que (?:buscaste|investigaste|encontraste) (?:la ultima vez|antes)");
    }
}
