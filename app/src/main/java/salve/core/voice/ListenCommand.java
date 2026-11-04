package salve.core.voice;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * «Escucha esto», «escucha esta canción y dime cuál es», «oye este ruido», «¿qué es ese sonido?»:
 * Salve graba unos segundos con el micrófono y se los da a su modelo (los Gemma 4 sin censura oyen).
 * Lógica pura para pruebas JVM.
 */
public final class ListenCommand {
    public static final int DEFAULT_SECONDS = 8, MAX_SECONDS = 20;

    private static final Pattern LISTEN = Pattern.compile(
            "^(?:escucha|escuchame|oye|oyeme|presta atencion a)\\s+"
                    + "(esto|esta cancion|esta musica|la musica|la cancion|lo que suena|este sonido|ese sonido|este ruido|ese ruido|"
                    + "el ruido|este audio|esta voz|mi voz|como hablo|como canto)"
                    + "(?:\\s+(?:durante|por|unos)\\s+(\\d{1,2}|unos|dos|tres|cinco|diez|quince|veinte)\\s+segundos?)?"
                    + "(?:\\s+y\\s+(.+))?$");
    private static final Pattern WHAT_SOUND = Pattern.compile(
            "^(?:que|cual) es (?:este|ese|el) (?:ruido|sonido)(?: que (?:se oye|suena|oigo))?$");
    private static final Pattern RECOGNIZE = Pattern.compile(
            "^(?:reconoce|identifica|adivina)\\s+(?:esta|la|que)\\s+cancion(?: es)?(?: que suena)?$");

    public final int seconds;
    public final boolean music;
    /** Lo que se le pide al modelo sobre el audio. */
    public final String question;

    private ListenCommand(int seconds, boolean music, String question) {
        this.seconds = seconds; this.music = music; this.question = question;
    }

    public static ListenCommand parse(String input) {
        String text = normalize(input);
        if (text.isEmpty() || text.split(" ").length > 25) return null;
        Matcher m = LISTEN.matcher(text);
        if (m.matches()) {
            String what = m.group(1);
            boolean music = what.contains("cancion") || what.contains("musica") || what.equals("como canto");
            String asked = m.group(3) == null ? null : m.group(3).replaceFirst("^(?:dime|di|cuentame|explicame)\\s+", "").trim();
            return new ListenCommand(seconds(m.group(2)), music, asked == null || asked.isEmpty() ? defaultQuestion(music) : asked);
        }
        if (WHAT_SOUND.matcher(text).matches()) return new ListenCommand(DEFAULT_SECONDS, false, defaultQuestion(false));
        if (RECOGNIZE.matcher(text).matches()) return new ListenCommand(12, true, defaultQuestion(true));
        return null;
    }

    /** «¿Qué canción suena?» cuando ninguna app dice qué está sonando (radio, la tele, un bar…). */
    public static ListenCommand forSongNobodyReports() { return new ListenCommand(12, true, defaultQuestion(true)); }

    static String defaultQuestion(boolean music) {
        return music
                ? "Escucha el audio. Si es una canción, di cuál crees que es y de quién; si no estás segura, dilo y describe el estilo, "
                    + "los instrumentos, la voz y el ambiente."
                : "Escucha el audio y cuenta lo que oyes: si hay voces, qué dicen y con qué tono; si hay música o sonidos, qué son.";
    }

    private static int seconds(String value) {
        if (value == null || value.equals("unos")) return DEFAULT_SECONDS;
        int n;
        switch (value) {
            case "dos": n = 2; break;
            case "tres": n = 3; break;
            case "cinco": n = 5; break;
            case "diez": n = 10; break;
            case "quince": n = 15; break;
            case "veinte": n = 20; break;
            default: n = Integer.parseInt(value);
        }
        return Math.max(2, Math.min(MAX_SECONDS, n));
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim()
                .replaceFirst("^(?:salve |oye salve )", "").replaceFirst("^(?:puedes |podrias )", "")
                .replaceFirst(" (?:por favor|porfa)$", "").trim();
    }
}
