package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Órdenes para la música que ya suena en otra app (Spotify…): «dale play», «pausa la música»,
 * «siguiente canción», «la anterior», «¿qué canción suena?», «léeme mis listas». Solo frases claras
 * de música: «para» a secas no cuenta (con la voz abierta significa «cállate»). Lógica pura.
 */
public final class MusicControlCommand {
    public enum Action { PLAY, PAUSE, NEXT, PREVIOUS, NOW_PLAYING, LIST_PLAYLISTS, GRANT_ACCESS }

    private static final Pattern PLAY = Pattern.compile(
            "^(?:dale (?:al )?play|dale a play|play|pon play|reanuda(?: la musica| la cancion)?|"
                    + "(?:sigue|continua|vuelve a poner) (?:con )?la (?:musica|cancion)|quita la pausa|dale a reproducir)$");
    private static final Pattern PAUSE = Pattern.compile(
            "^(?:pausa|pausala|pon pausa|pon(?:la)? en pausa|(?:pausa|para|deten|detén|quita|apaga|corta) (?:la|esta) (?:musica|cancion)|"
                    + "para spotify|pausa spotify)$");
    private static final Pattern NEXT = Pattern.compile(
            "^(?:siguiente|siguiente (?:cancion|tema)|(?:pasa|salta|cambia)(?: de| la| esta)? (?:cancion|tema)|"
                    + "(?:pon )?(?:la )?siguiente|otra cancion|pasa a la siguiente|skip)$");
    private static final Pattern PREVIOUS = Pattern.compile(
            "^(?:anterior|(?:pon )?la (?:cancion |tema )?anterior|cancion anterior|tema anterior|vuelve a la (?:cancion )?anterior|"
                    + "repite la (?:cancion|anterior))$");
    private static final Pattern NOW_PLAYING = Pattern.compile(
            "^(?:que (?:cancion|tema|musica) (?:es|suena|esta sonando|es esta|es esa)|que suena|que esta sonando|"
                    + "como se llama (?:esta|esa) (?:cancion|musica|tema)|(?:lee|leeme|dime) (?:el titulo|que suena|la cancion|que cancion es)"
                    + "(?: de la cancion| que suena)?|de quien es (?:esta|esa) cancion|quien canta(?: esto| esta cancion)?)$");
    private static final Pattern LIST_PLAYLISTS = Pattern.compile(
            "^(?:(?:revisa|lee|leeme|dime|ensename|muestrame|cuales son|que son|abre) )?(?:(?:cuales|que) )?"
                    + "(?:son )?(?:mis|las mias) (?:listas|playlists)(?: de (?:musica|reproduccion|spotify))?(?: de spotify| en spotify)?$"
                    + "|^que (?:listas|playlists) tengo(?: en spotify)?$");

    /** «Dale acceso a la música»: abrir el ajuste que deja ver y controlar la música. */
    private static final Pattern GRANT_ACCESS = Pattern.compile(
            "^(?:dale|da|activa|concede|dame)(?: el)? (?:acceso|permiso|control) (?:a|de|para) (?:la )?(?:musica|spotify)$");

    private MusicControlCommand() { }

    public static Action parse(String input) {
        String t = normalize(input);
        if (t.isEmpty() || t.split(" ").length > 12) return null;
        if (PLAY.matcher(t).matches()) return Action.PLAY;
        if (PAUSE.matcher(t).matches()) return Action.PAUSE;
        if (NEXT.matcher(t).matches()) return Action.NEXT;
        if (PREVIOUS.matcher(t).matches()) return Action.PREVIOUS;
        if (NOW_PLAYING.matcher(t).matches()) return Action.NOW_PLAYING;
        if (LIST_PLAYLISTS.matcher(t).matches()) return Action.LIST_PLAYLISTS;
        if (GRANT_ACCESS.matcher(t).matches()) return Action.GRANT_ACCESS;
        return null;
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(salve.core.devicecontrol.AppAliases.normalize(value).toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:salve |oye salve |oye )", "").replaceFirst("^(?:puedes |podrias )", "")
                .replaceFirst(" (?:por favor|porfa|ya|ahora|salve)$", "").trim();
    }
}
