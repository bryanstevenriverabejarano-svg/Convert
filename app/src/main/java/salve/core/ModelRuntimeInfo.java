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
                + ". Se ejecuta localmente en el teléfono. Estos datos sustituyen cualquier nombre de modelo en el historial.\n"
                + CAPABILITIES;
    }
    /**
     * Lo que la app ya hace por órdenes directas (antes de que pienses). Sin esto el modelo decía
     * «no puedo abrir WhatsApp» cuando Salve sí puede: solo hace falta pedirlo con la frase adecuada.
     */
    static final String CAPABILITIES = "CAPACIDADES DE SALVE EN ESTE MÓVIL: la app ejecuta por sí misma órdenes directas de Bryan: "
            + "abrir apps; poner música en Spotify («pon <canción> en Spotify», «pon mi lista <nombre>», «pon mis me gusta») "
            + "o en YouTube; preparar un WhatsApp a un contacto para que Bryan lo revise y envíe («abre el chat de <contacto> "
            + "y dile <mensaje>»); rutas en Maps; llamadas con confirmación; buscar en internet; ver fotos; y oír con el micrófono («escucha esta canción», «oye este ruido»). Si te piden algo de esto y "
            + "estás respondiendo tú, es que la orden no se reconoció: no digas que no puedes; di la frase que lo hace.\n";
    /** Igual que {@link #context} pero sin atribuir identidad: para voces que no son Salve (Salventes). */
    public static String neutralContext(String path) {
        return "DATOS DEL MOTOR: el modelo que genera esta respuesta es " + name(path)
                + " y se ejecuta localmente en el teléfono. Tu identidad es la que indica el texto siguiente.\n";
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
