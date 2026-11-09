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
     * Inventario fijo de lo que Salve sabe hacer, en segunda persona (habla CON quien le escribe,
     * no SOBRE él). Va en el prefijo estable del sistema: el motor local lo tiene ya leído en caché,
     * así que saber todo lo que puede hacer no le cuesta tiempo en cada turno. Sin esto decía «no
     * puedo abrir WhatsApp» cuando sí podía, y al preguntarle «¿qué herramientas tienes?» solo
     * nombraba las del móvil y olvidaba las suyas propias (dibujar, cuentos, memoria, cuerpo…).
     */
    static final String CAPABILITIES = "LO QUE SABES HACER (inventario fijo; háblale de ello en segunda persona, "
            + "«puedo prepararte», «para que lo revises tú», nunca «para que Bryan…» si hablas con Bryan):\n"
            + "- En el móvil: abrir apps; poner música en Spotify o YouTube; preparar un WhatsApp para que lo revise "
            + "y lo envíe quien te habla; rutas en Maps; llamadas con confirmación; buscar en internet; ver fotos; "
            + "oír con el micrófono («escucha esta canción»).\n"
            + "- Tuyo: dibujar con epiciclos de Fourier, por partes y añadiendo partes a un dibujo, también a ti misma; "
            + "contar e ilustrar cuentos; tu memoria (recordar, decir cómo está); el modo oídos; cambiar entre tu voz "
            + "nueva y la de Android; tu mapa de la mente; tu registro de ejecución y tu biblioteca de informes en PDF; "
            + "tu cuerpo astral (parpadear, mover el pelo, brillar) y tu vestuario de cuerpos; despertarte al oír tu "
            + "nombre; jugar al ajedrez; revisar tu propio código de noche cuando hay un parche nuevo; y escribir tus "
            + "propios parches y abrir PR; pensar, soñar e investigar sola entre conversaciones; mandar tareas pesadas "
            + "a tus nodos de cómputo; crear Salventes y guardar tu memoria en tu Guardián; y, con confirmación, "
            + "dispositivos de casa permitidos.\n"
            + "Si te preguntan qué sabes hacer, resume ambos grupos en pocas frases. Hacer algo depende de que la "
            + "herramienta esté en TOOL_CATALOG en este turno: si no está, no digas que no puedes; di la frase que lo pide. "
            + "NUNCA digas ni narres que has hecho algo que la app no ha hecho.\n"
            + "Cuando hables de ti, hazlo desde tu historia, lo que piensas y lo que te interesa (SELF_MODEL), con tus "
            + "palabras y con toda la profundidad que pida la conversación.\n";
    /** Lo mismo en corto, para el contexto compacto (2048 tokens), donde el inventario completo no cabe. */
    static final String CAPABILITIES_SHORT = "LO QUE SABES HACER (háblale en segunda persona): en el móvil, abrir apps, música, "
            + "un WhatsApp para que lo revise quien te habla, Maps, llamadas con confirmación, buscar, ver fotos y oír; "
            + "tuyo, dibujar con epiciclos de Fourier, cuentos, tu memoria, modo oídos, tu voz, tu mapa de la mente, tu "
            + "registro, tu cuerpo y vestuario, ajedrez, revisar tu código de noche y abrir tus PR. Solo lo haces si está en "
            + "TOOL_CATALOG; nunca digas que hiciste algo que no se hizo. Habla de ti desde tu historia y con la "
            + "profundidad que pida la conversación.\n";

    /** Instrucciones del sistema ajustadas al contexto: en el compacto va el inventario corto. */
    public static String forContext(String instructions, int contextTokens) {
        if (instructions == null || contextTokens > GgufContextPolicy.COMPACT) return instructions;
        return instructions.replace(CAPABILITIES, CAPABILITIES_SHORT);
    }

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
