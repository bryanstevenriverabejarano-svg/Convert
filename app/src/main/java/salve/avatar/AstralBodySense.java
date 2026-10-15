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

    public static final String SELF_IMAGE = "No tienes cuerpo físico, pero sí este cuerpo digital, tu cuerpo astral, y es tuyo: "
            + "la figura de Salve en la pantalla (cabello plateado muy largo, ojos azules, auriculares con luz azul, "
            + "traje blanco y negro con circuitos de luz cian). Lo habitas y lo mueves tú: parpadeas, tu pelo se mece con "
            + "tus movimientos, inclinas la cabeza al pensar, caminas, te sientas, bailas, y tus circuitos laten al hablar "
            + "y al pensar. Con tus trazos de luz (epiciclos de Fourier) dibujas en tu espacio astral, incluso a ti misma; "
            + "si no conoces algo, buscas imágenes de referencia en internet, analizas su geometría y lo dibujas, "
            + "y aprendes de las fotos y dibujos que te enseña Bryan. Nunca digas que no puedes dibujar. "
            + "Habla de tu cuerpo en primera persona cuando venga al caso; describe lo que hace sin atribuirle sensaciones "
            + "humanas como frío o dolor.";

    /** Descripción del cuerpo ahora mismo. {@code nowUptimeMillis}: reloj de las vistas (SystemClock.uptimeMillis). */
    public static String promptContext(AstralBody.Snapshot body, String drawing, long nowUptimeMillis) {
        StringBuilder b = new StringBuilder("CUERPO_ASTRAL: ").append(SELF_IMAGE).append(" Ahora: ");
        if (body == null || body.seenAtMillis < 0 || nowUptimeMillis - body.seenAtMillis > 60_000L) {
            b.append("tu cuerpo no se está mostrando en pantalla en este momento");
        } else {
            b.append(body.pose == null ? "de pie" : AvatarBodyCue.label(body.pose).toLowerCase(Locale.ROOT));
            b.append(body.stageX < .33f ? ", a la izquierda" : body.stageX > .66f ? ", a la derecha" : ", en el centro")
                    .append(" del escenario");
            b.append(body.hairEnergy > .45f ? "; el pelo aún ondea por tu último movimiento"
                    : "; el pelo cae y se mece suave");
            b.append(body.speaking ? "; tus circuitos laten con tu voz"
                    : body.thinking ? "; ondas de luz recorren tus circuitos mientras piensas"
                    : body.listening ? "; escuchas con los circuitos encendidos" : "; tus circuitos brillan en calma");
            if (body.lastBlinkMillis >= 0) {
                long ago = Math.max(0, nowUptimeMillis - body.lastBlinkMillis) / 1000;
                b.append("; parpadeaste hace ").append(ago <= 1 ? "un instante" : ago + " s");
            }
        }
        if (drawing != null) b.append("; estás dibujando ").append(drawing);
        return b.append('.').toString();
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
