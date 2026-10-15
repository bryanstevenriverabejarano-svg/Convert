package salve.core.web;

import java.text.Normalizer;
import java.util.Locale;

/** Órdenes sobre la mini ventana donde Salve enseña lo que busca y lo que suena. */
public final class LiveSearchCommand {
    public enum Action { SHOW, HIDE, CLOSE }

    private LiveSearchCommand() { }

    public static Action parse(String input) {
        String t = input == null ? "" : Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^salve ", "").replaceFirst(" por favor$", "");
        if (t.matches("(?:no (?:me )?(?:muestres|ensenes)|oculta(?:me)?|deja de (?:mostrar|ensenar)(?:me)?) (?:tus|las) busquedas"))
            return Action.HIDE;
        if (t.matches("(?:muestra(?:me)?|ensena(?:me)?|quiero ver) (?:tus|las) busquedas(?: en (?:tiempo real|directo|vivo))?"
                + "|(?:activa|abre) (?:la )?mini ?ventana|ensename lo que buscas"))
            return Action.SHOW;
        if (t.matches("(?:cierra|quita) (?:la )?mini ?ventana|(?:para|quita|apaga|deten|detén) (?:la )?musica|para el video"))
            return Action.CLOSE;
        return null;
    }
}
