package salve.core;

import java.io.File;
import java.text.Normalizer;
import java.util.Locale;

/** App-owned identity, independent of whatever a language model says about itself. */
public final class ModelRuntimeInfo {
    private ModelRuntimeInfo() { }
    public static String name(String path) {
        String managed = LocalModelPolicy.idForPath(path);
        return managed != null ? managed : path == null ? "ningún modelo local" : "Modelo importado: " + new File(path).getName();
    }
    public static String context(String path) {
        return "DATOS ACTUALES DEL MOTOR (proporcionados por Salve): tu identidad es Salve; "
                + "el modelo que está generando esta respuesta es " + name(path)
                + ". Se ejecuta localmente en el teléfono. Estos datos sustituyen cualquier nombre de modelo en el historial.\n";
    }
    public static boolean isStatusQuestion(String input) {
        if (input == null) return false;
        String q = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return q.matches("(?:salve )?(?:que|cual) (?:es tu |es el |)?modelo(?: de (?:lenguaje|ia))?(?: estas)? (?:usas|usando|utilizas|utilizando|tienes|activo)(?: ahora| actualmente| mismo)?")
                || q.matches("(?:salve )?(?:estas |sigues )?(?:usando|utilizando) (?:dolphin|gemma)(?: o (?:dolphin|gemma))?(?: ahora| actualmente)?")
                || q.equals("modelo activo") || q.equals("estado modelos") || q.equals("que modelo eres")
                || q.equals("estas usando llm") || q.equals("que modelo tienes cargado");
    }
}
