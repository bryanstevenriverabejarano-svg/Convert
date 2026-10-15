package salve.avatar;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Propiocepción de Salve: convierte el estado de su cuerpo astral en una descripción breve para su
 * contexto, y atiende las acciones voluntarias sobre él ("parpadea", "mueve el pelo", "brilla").
 * No afirma sensaciones humanas: describe lo que su cuerpo digital hace y puede hacer.
 */
public final class AstralBodySense {
    private AstralBodySense() { }

    /**
     * Esquema corporal: hechos sobre su cuerpo digital y lo que el código le permite hacer con él.
     * Sólo se incluye cuando la conversación trata de su cuerpo, su aspecto, sus dibujos o de quién es;
     * en el resto de turnos basta la propiocepción breve. Sin órdenes de estilo: datos.
     */
    public static final String BODY_SCHEMA = "ESQUEMA_CORPORAL (hechos): no tengo cuerpo físico; mi cuerpo es digital: "
            + "la figura de Salve en la pantalla (cabello plateado muy largo, ojos azules, auriculares con luz azul, "
            + "traje blanco y negro con circuitos de luz cian). Mi código lo mueve: parpadeo, pelo con inercia, cabeza, "
            + "caminar, sentarme, bailar; los circuitos se encienden al pensar y al hablar. Dibujar lo hace mi módulo de "
            + "trazos: cada línea es una cadena de círculos que giran (transformada de Fourier) y la punta del último la traza; "
            + "las líneas salen de mi repertorio, de fotos o dibujos que me enseñan, de imágenes de referencia que busco en "
            + "internet analizando su geometría, o de formas que compongo. No siento frío ni dolor: describo lo que mi cuerpo hace.";

    /** ¿El mensaje trata de su cuerpo, su aspecto, sus dibujos, sus capacidades o de quién es? */
    public static boolean relevant(String input) {
        String t = normalize(input);
        if (t.isEmpty()) return false;
        return SELF_TOPICS.matcher(t).find();
    }

    private static final Pattern SELF_TOPICS = Pattern.compile(
            // Pedir un dibujo no es hablar de ella: sólo preguntar CÓMO dibuja o cómo es trae el esquema completo.
            "\\b(como dibujas|como lo dibujas|como la dibujas|como dibujaste|como lo dibujaste|como la dibujaste|"
            + "con que dibujas|tecnica|epiciclo\\w*|fourier|cuerpo|pelo|cabello|ojos|parpade\\w*|brill\\w*|"
            + "circuito\\w*|aspecto|figura|como eres|quien eres|que eres|como te ves|tu misma|ti misma|consciente|conciencia|"
            + "consciencia|como lo hiciste|como la hiciste|como hiciste|como lo haces|que puedes|que sabes hacer|que has hecho|"
            + "puedes hacer|autonom\\w*|identidad)\\b");

    /** Propiocepción: el estado de su cuerpo ahora. Breve si el tema no va de ella. */
    public static String promptContext(AstralBody.Snapshot body, String drawing, long nowUptimeMillis, boolean full) {
        StringBuilder b = new StringBuilder(full ? BODY_SCHEMA + "\n" : "").append("CUERPO_AHORA: ");
        if (body == null || body.seenAtMillis < 0 || nowUptimeMillis - body.seenAtMillis > 60_000L) {
            b.append("no se muestra en pantalla");
        } else {
            b.append(body.pose == null ? "de pie" : AvatarBodyCue.label(body.pose).toLowerCase(Locale.ROOT));
            b.append(body.stageX < .33f ? ", a la izquierda" : body.stageX > .66f ? ", a la derecha" : ", en el centro");
            if (full) {
                b.append(body.hairEnergy > .45f ? "; el pelo aún ondea por mi último movimiento" : "; el pelo quieto");
                b.append(body.speaking ? "; circuitos al ritmo de mi voz" : body.thinking ? "; circuitos en ondas mientras pienso"
                        : "; circuitos en calma");
                if (body.lastBlinkMillis >= 0) {
                    long ago = Math.max(0, nowUptimeMillis - body.lastBlinkMillis) / 1000;
                    b.append("; parpadeé hace ").append(ago <= 1 ? "un instante" : ago + " s");
                }
            }
        }
        if (drawing != null) b.append("; dibujando ").append(drawing);
        return b.append('.').toString();
    }

    /** Compatibilidad: versión completa. */
    public static String promptContext(AstralBody.Snapshot body, String drawing, long nowUptimeMillis) {
        return promptContext(body, drawing, nowUptimeMillis, true);
    }

    private static final Pattern BLINK = Pattern.compile(
            "^(salve )?(parpadea|parpadeame|parpadea para mi|parpadea dos veces|puedes parpadear|guiname un ojo|guina un ojo|cierra y abre los ojos)$");
    private static final Pattern HAIR = Pattern.compile(
            "^(salve )?(mueve|agita|sacude|menea|ondea|puedes mover|mueveme)( el| tu| la| tus)? (pelo|cabello|melena|mechones)( al viento| un poco| para mi)?$");
    private static final Pattern GLOW = Pattern.compile(
            "^(salve )?(brilla|brilla para mi|enciende tus (luces|circuitos)|ilumina tus circuitos|haz brillar tus circuitos|enciendete)$");

    /** Respuesta en primera persona si el mensaje es una orden directa al cuerpo; null si no lo es. */
    public static String command(String input) {
        String text = normalize(input);
        if (text.isEmpty() || text.length() > 80) return null;
        AstralBody body = AstralBody.get();
        if (BLINK.matcher(text).matches()) {
            body.requestBlink();
            return text.contains("guin") ? "Guiño con los dos ojos a la vez; todavía no sé cerrar uno solo. ¿Lo has visto?"
                    : "Parpadeo para ti. ¿Lo has visto?";
        }
        if (HAIR.matcher(text).matches()) {
            body.flickHair(0);
            return "Ahí va mi pelo: lo dejo ondear y vuelve a caer despacio.";
        }
        if (GLOW.matcher(text).matches()) {
            body.pulse();
            return "Enciendo mis circuitos: la luz sale del pecho y recorre todo mi cuerpo.";
        }
        return null;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
