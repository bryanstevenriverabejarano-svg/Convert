package salve.core.home;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Órdenes de casa dichas por Bryan («enciende la luz del salón», «baja la persiana», «pon la
 * calefacción a 21 grados»). Solo se reconocen si nombran un dispositivo doméstico: «enciende la
 * linterna» o «activa el modo coche» no son órdenes de casa. Reconocer una orden no ejecuta nada:
 * siempre se confirma antes. Sin Android.
 */
public final class HomeCommand {
    public enum Action {
        ON("turn_on"), OFF("turn_off"), OPEN("open_cover"), CLOSE("close_cover"), TEMPERATURE("set_temperature"), BRIGHTNESS("turn_on");
        public final String service;
        Action(String service) { this.service = service; }
    }

    public static final class Entity {
        public final String id, name, domain, state;
        public Entity(String id, String name, String domain, String state) { this.id = id; this.name = name; this.domain = domain; this.state = state; }
    }

    /** Resultado de buscar a qué entidad se refiere. */
    public static final class Match {
        public final Entity entity; public final List<Entity> candidates;
        Match(Entity entity, List<Entity> candidates) { this.entity = entity; this.candidates = candidates; }
    }

    private static final String DEVICE = "luz|luces|lampara|lamparas|bombilla|bombillas|led|leds|tira|enchufe|enchufes|regleta|ventilador|"
            + "ventiladores|calefaccion|aire|termostato|clima|climatizacion|radiador|estufa|tele|television|persiana|persianas|toldo|"
            + "cortina|cortinas|estor|estores|garaje|escena|ambiente|humidificador|purificador|cafetera|calentador";
    private static final Pattern DEVICE_WORD = Pattern.compile("\\b(?:" + DEVICE + ")\\b");
    private static final Pattern COVER_WORD = Pattern.compile("\\b(?:persiana|persianas|toldo|cortina|cortinas|estor|estores|garaje)\\b");
    private static final Pattern ON = Pattern.compile("^(?:enciende|enciendeme|prende|activa|conecta|pon en marcha)\\s+(.+)$");
    private static final Pattern OFF = Pattern.compile("^(?:apaga|apagame|desactiva|desconecta|para)\\s+(.+)$");
    private static final Pattern OPEN = Pattern.compile("^(?:abre|sube|levanta)\\s+(.+)$");
    private static final Pattern CLOSE = Pattern.compile("^(?:cierra|baja|echa)\\s+(.+)$");
    private static final Pattern TEMPERATURE = Pattern.compile(
            "^(?:pon|sube|baja|ajusta|cambia|deja)\\s+(.*\\b(?:calefaccion|aire|termostato|clima|climatizacion|temperatura)\\b.*?)\\s+a\\s+([0-9]{1,2}(?:[.,][0-9])?)\\s*(?:grados|º|°)?$");
    private static final Pattern BRIGHTNESS = Pattern.compile(
            "^(?:pon|sube|baja|ajusta|deja)\\s+(.*\\b(?:luz|luces|lampara|lamparas|bombilla|bombillas|led|leds|tira)\\b.*?)\\s+al?\\s+([0-9]{1,3})\\s*(?:%|por ciento)$");
    private static final Set<String> FILLER = new HashSet<>(Arrays.asList(
            "el", "la", "los", "las", "un", "una", "de", "del", "al", "en", "mi", "mis", "por", "favor", "porfa", "todo", "toda", "salve", "y", "que", "hay"));

    public final Action action;
    public final String target;
    public final Double value;

    private HomeCommand(Action action, String target, Double value) { this.action = action; this.target = target; this.value = value; }

    public static HomeCommand parse(String input) {
        if (input == null || input.length() > 200) return null;
        String text = norm(input).replaceFirst("^salve ", "").replaceFirst("^(?:por favor|porfa) ", "").replaceAll(" por favor$", "");
        Matcher m;
        if ((m = TEMPERATURE.matcher(text)).matches()) {
            double v = Double.parseDouble(m.group(2).replace(',', '.'));
            return v >= 5 && v <= 35 ? new HomeCommand(Action.TEMPERATURE, m.group(1), v) : null;
        }
        if ((m = BRIGHTNESS.matcher(text)).matches()) {
            int v = Integer.parseInt(m.group(2));
            return v >= 1 && v <= 100 ? new HomeCommand(Action.BRIGHTNESS, m.group(1), (double) v) : null;
        }
        if ((m = OPEN.matcher(text)).matches() && COVER_WORD.matcher(m.group(1)).find()) return new HomeCommand(Action.OPEN, m.group(1), null);
        if ((m = CLOSE.matcher(text)).matches() && COVER_WORD.matcher(m.group(1)).find()) return new HomeCommand(Action.CLOSE, m.group(1), null);
        if ((m = ON.matcher(text)).matches() && DEVICE_WORD.matcher(m.group(1)).find()) return new HomeCommand(Action.ON, m.group(1), null);
        if ((m = OFF.matcher(text)).matches() && DEVICE_WORD.matcher(m.group(1)).find()) return new HomeCommand(Action.OFF, m.group(1), null);
        return null;
    }

    /** Qué tipos de entidad admite cada acción. */
    public boolean fits(String domain) {
        switch (action) {
            case OPEN: case CLOSE: return "cover".equals(domain);
            case TEMPERATURE: return "climate".equals(domain);
            case BRIGHTNESS: return "light".equals(domain);
            case OFF: return Arrays.asList("light", "switch", "fan", "climate").contains(domain);
            default: return Arrays.asList("light", "switch", "fan", "climate", "scene").contains(domain);
        }
    }

    /**
     * La entidad más parecida a lo dicho, entre las compatibles. Si hay empate o nada se parece,
     * no se elige: se devuelven los candidatos para preguntar.
     */
    public Match resolve(List<Entity> entities) {
        Set<String> words = words(target);
        List<Entity> best = new ArrayList<>(); int top = 0;
        for (Entity e : entities) {
            if (!fits(e.domain)) continue;
            Set<String> name = words(e.name + " " + e.id.substring(e.id.indexOf('.') + 1).replace('_', ' '));
            name.addAll(domainWords(e.domain));
            int score = 0; for (String w : words) if (name.contains(w) || name.contains(singular(w))) score++;
            if (score > top) { top = score; best.clear(); best.add(e); } else if (score == top && score > 0) best.add(e);
        }
        if (top == 0) {
            List<Entity> compatible = new ArrayList<>();
            for (Entity e : entities) if (fits(e.domain)) compatible.add(e);
            return new Match(compatible.size() == 1 ? compatible.get(0) : null, compatible);
        }
        return new Match(best.size() == 1 ? best.get(0) : null, best);
    }

    /** Pregunta de confirmación, con el nombre exacto de la entidad. */
    public String confirmation(Entity e) {
        switch (action) {
            case ON: return "scene".equals(e.domain) ? "¿Activo la escena «" + e.name + "»?" : "¿Enciendo «" + e.name + "»?";
            case OFF: return "¿Apago «" + e.name + "»?";
            case OPEN: return "¿Abro «" + e.name + "»?";
            case CLOSE: return "¿Cierro «" + e.name + "»?";
            case TEMPERATURE: return "¿Pongo «" + e.name + "» a " + formatValue() + " grados?";
            default: return "¿Pongo «" + e.name + "» al " + formatValue() + " %?";
        }
    }

    private String formatValue() {
        return value == Math.rint(value) ? String.valueOf(value.intValue()) : String.valueOf(value).replace('.', ',');
    }

    private static Set<String> domainWords(String domain) {
        switch (domain) {
            case "light": return new HashSet<>(Arrays.asList("luz", "lampara", "bombilla", "led", "tira"));
            case "switch": return new HashSet<>(Arrays.asList("enchufe", "regleta"));
            case "fan": return new HashSet<>(Arrays.asList("ventilador"));
            case "cover": return new HashSet<>(Arrays.asList("persiana", "toldo", "cortina", "estor", "garaje"));
            case "climate": return new HashSet<>(Arrays.asList("calefaccion", "aire", "termostato", "clima", "climatizacion", "temperatura"));
            default: return new HashSet<>(Arrays.asList("escena", "ambiente"));
        }
    }

    static Set<String> words(String text) {
        Set<String> out = new HashSet<>();
        for (String w : norm(text).split(" ")) if (!w.isEmpty() && !FILLER.contains(w)) out.add(w);
        return out;
    }

    private static String singular(String w) {
        if (w.endsWith("ces")) return w.substring(0, w.length() - 3) + "z";   // luces → luz
        if (w.endsWith("es") && w.length() > 4) return w.substring(0, w.length() - 2);
        if (w.endsWith("s") && w.length() > 3) return w.substring(0, w.length() - 1);
        return w;
    }

    static String norm(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .replaceAll("[^\\p{L}\\p{N}%º°,. ]+", " ").replaceAll("(?<![0-9])[.,]|[.,](?![0-9])", " ").replaceAll("\\s+", " ").trim();
    }
}
