package salve.core.conversation;

import java.text.Normalizer;
import java.time.Clock;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/** A current device observation, independent of the model and of any declared location. */
public final class DeviceClockContext {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT);
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/uuuu", Locale.ROOT);
    private static final Pattern TIME_QUESTION = Pattern.compile(
            "(?:que hora es(?: ahora)?|que hora tienes|dime la hora(?: actual)?|hora actual"
                    + "|que hora marca (?:el movil|el telefono)|que hora es en (?:el movil|el telefono))");
    private static final Pattern DATE_QUESTION = Pattern.compile(
            "(?:que fecha es(?: hoy)?|que dia es hoy|que dia es|a que dia estamos"
                    + "|en que fecha estamos|dime la fecha(?: de hoy)?|fecha actual|cual es la fecha de hoy)");

    private final Clock clock;
    private final Supplier<ZoneId> deviceZone;

    public DeviceClockContext() {
        this(Clock.systemUTC(), ZoneId::systemDefault);
    }

    /** Re-evaluates the zone on each observation so a device timezone change is respected. */
    public DeviceClockContext(Clock clock, Supplier<ZoneId> deviceZone) {
        this.clock = Objects.requireNonNull(clock, "clock");
        this.deviceZone = Objects.requireNonNull(deviceZone, "deviceZone");
    }

    public String promptContext() {
        ZonedDateTime now = now();
        return "RELOJ DEL DISPOSITIVO: " + DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now)
                + "; zona configurada=" + now.getZone().getId() + ". "
                + "Fuente: reloj y configuración horaria del teléfono, no una consulta de internet. "
                + "Es la hora del dispositivo; no demuestra la ubicación del usuario ni la hora de otra ciudad. "
                + "No deduzcas una zona horaria de una ubicación declarada sin una conversión verificada.";
    }

    /** Null leaves contextual, compound, event and other-location questions to the conversation. */
    public String directReply(String input) {
        String question = normalize(input);
        boolean time = TIME_QUESTION.matcher(question).matches();
        boolean date = DATE_QUESTION.matcher(question).matches();
        if (!time && !date) return null;
        ZonedDateTime now = now();
        String source = " (zona " + now.getZone().getId() + ", " + offsetLabel(now) + ").";
        return time ? "Según el reloj del teléfono, son las " + TIME.format(now) + source
                : "Según el calendario del teléfono, hoy es " + DATE.format(now) + source;
    }

    private ZonedDateTime now() {
        return clock.instant().atZone(Objects.requireNonNull(deviceZone.get(), "deviceZone value"));
    }

    private static String offsetLabel(ZonedDateTime now) {
        String offset = now.getOffset().getId();
        return offset.equals("Z") ? "UTC+00:00" : "UTC" + offset;
    }

    private static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!.,]", " ")
                .replaceAll("\\s+", " ").trim()
                .replaceFirst("^por favor ", "")
                .replaceFirst(" por favor$", "");
    }
}
