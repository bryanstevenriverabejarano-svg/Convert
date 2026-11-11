package salve.core.sensors;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Los sentidos del teléfono como una sola percepción para Salve.
 *
 * {@link SensorSnapshot} cubría cinco lecturas dentro de una sesión pedida a mano. Esto amplía el
 * alcance a todos los sensores físicos habituales (entorno, movimiento, orientación) y al estado del
 * propio dispositivo (energía y sistema), y lo ofrece al modelo como herramienta de solo lectura:
 * una ráfaga breve que mide, se interpreta con unidades y se descarta. No guarda historial, no infiere
 * emociones y no afirma nada que el teléfono no haya medido en ese instante.
 *
 * Sin Android: el servicio {@code SistemaSensorial} entrega los valores; aquí solo se interpretan.
 */
public final class DeviceSenses {
    public static final String TOOL = "device.sense";

    public enum Group {
        TODO("todo"), ENTORNO("luz, proximidad, temperatura, humedad, presión, campo magnético"),
        MOVIMIENTO("aceleración, giro, pasos"), ORIENTACION("postura y rumbo de brújula"),
        ENERGIA("batería y carga"), SISTEMA("RAM, almacenamiento, red, volumen, brillo, calor"),
        INVENTARIO("lista de sensores que tiene este teléfono");

        public final String covers;
        Group(String covers) { this.covers = covers; }
    }

    /** Cómo se reduce una muestra a un número. */
    enum Shape { SCALAR, MAGNITUDE, POSTURE }

    /** Sensores físicos reconocidos; {@code type} es la constante {@code android.hardware.Sensor.TYPE_*}. */
    public enum Kind {
        LIGHT(5, Group.ENTORNO, "Luz", "lux", Shape.SCALAR),
        PROXIMITY(8, Group.ENTORNO, "Proximidad", "cm", Shape.SCALAR),
        AMBIENT_TEMPERATURE(13, Group.ENTORNO, "Temperatura ambiente", "°C", Shape.SCALAR),
        HUMIDITY(12, Group.ENTORNO, "Humedad relativa", "%", Shape.SCALAR),
        PRESSURE(6, Group.ENTORNO, "Presión atmosférica", "hPa", Shape.SCALAR),
        MAGNETIC_FIELD(2, Group.ENTORNO, "Campo magnético", "µT", Shape.MAGNITUDE),
        ACCELEROMETER(1, Group.MOVIMIENTO, "Aceleración total (con gravedad)", "m/s²", Shape.MAGNITUDE),
        LINEAR_ACCELERATION(10, Group.MOVIMIENTO, "Aceleración propia (sin gravedad)", "m/s²", Shape.MAGNITUDE),
        GYROSCOPE(4, Group.MOVIMIENTO, "Giro", "rad/s", Shape.MAGNITUDE),
        STEP_COUNTER(19, Group.MOVIMIENTO, "Pasos desde que se encendió el teléfono", "pasos", Shape.SCALAR),
        GRAVITY(9, Group.ORIENTACION, "Postura del teléfono", "", Shape.POSTURE);

        public final int type;
        public final Group group;
        final String label, unit;
        final Shape shape;

        Kind(int type, Group group, String label, String unit, Shape shape) {
            this.type = type; this.group = group; this.label = label; this.unit = unit; this.shape = shape;
        }

        public static Kind of(int androidType) {
            for (Kind kind : values()) if (kind.type == androidType) return kind;
            return null;
        }
    }

    private DeviceSenses() { }

    // ------------------------------------------------------------------ cuándo se ofrece

    private static final Pattern ASKS = Pattern.compile("\\b(?:"
            + "sensor(?:es)?|que (?:percibes|detectas|mides)|tus sentidos|tu entorno|a tu alrededor"
            + "|bateria|cargando|enchufad[oa]"
            + "|(?:cuanta|hay|que|poca|mucha) luz|esta oscuro|a oscuras|iluminacion"
            + "|brujula|rumbo|donde esta el norte|hacia donde (?:apunt\\w+|mir\\w+|estoy mirando)"
            + "|altitud|a que altura|presion|barometro|humedad"
            + "|temperatura (?:del|de mi|de tu|de la|aqui|ambiente)|se esta calentando|esta caliente el"
            + "|me estoy moviendo|estoy quieto|estoy en movimiento|estoy caminando|cuantos pasos"
            + "|postura|boca arriba|boca abajo|en vertical|en horizontal|apaisado"
            + "|como esta (?:el|mi|tu) (?:movil|telefono|dispositivo|cuerpo fisico)"
            + "|estado del (?:movil|telefono|dispositivo)"
            + "|almacenamiento|espacio (?:libre|disponible)|ram"
            + "|(?:tengo|hay|tienes) (?:wifi|conexion|internet|datos|cobertura)|estoy conectado|a que red|que red"
            + "|(?:a que|que|cual es el|como esta el) (?:volumen|brillo)"
            + "|campo magnetico|giroscopio|acelerometro|magnetometro"
            + ")\\b");

    /** ¿El turno pregunta por algo que el teléfono puede medir? Solo decide si se ofrece; no mide nada. */
    public static boolean offered(String input) {
        String s = normalize(input);
        return !s.isEmpty() && s.length() <= 300 && ASKS.matcher(s).find();
    }

    public static String description() {
        // Corta a propósito: cada carácter del catálogo se lee en cada inferencia y quita sitio a otras herramientas.
        return "{grupo:'TODO'|'ENTORNO'|'MOVIMIENTO'|'ORIENTACION'|'ENERGIA'|'SISTEMA'|'INVENTARIO'}: mide AHORA con los sensores "
                + "y el estado del teléfono (solo lectura). Cuenta solo lo medido; «no disponible» no se inventa.";
    }

    /** El modelo propone el grupo; si no es válido se deduce de lo que preguntó el usuario. */
    public static Group resolve(String input, String proposed) {
        if (proposed != null) {
            try { return Group.valueOf(normalize(proposed).toUpperCase(Locale.ROOT)); }
            catch (IllegalArgumentException unknown) { /* se deduce abajo */ }
        }
        return groupFor(input);
    }

    static Group groupFor(String input) {
        String s = normalize(input);
        if (s.matches(".*\\b(?:que sensores|cuantos sensores|lista de sensores|tus sensores)\\b.*")) return Group.INVENTARIO;
        boolean energy = s.matches(".*\\b(?:bateria|cargando|enchufad[oa])\\b.*");
        boolean system = s.matches(".*\\b(?:almacenamiento|espacio|ram|wifi|conexion|internet|datos|cobertura|red|volumen|brillo|calentando|caliente)\\b.*");
        boolean motion = s.matches(".*\\b(?:moviendo|quieto|movimiento|caminando|pasos|giroscopio|acelerometro)\\b.*");
        boolean pose = s.matches(".*\\b(?:brujula|rumbo|norte|apunt\\w+|postura|boca|vertical|horizontal|apaisado)\\b.*");
        boolean world = s.matches(".*\\b(?:luz|oscuro|oscuras|iluminacion|altitud|altura|presion|barometro|humedad|temperatura|magnetico|magnetometro)\\b.*");
        int hits = (energy ? 1 : 0) + (system ? 1 : 0) + (motion ? 1 : 0) + (pose ? 1 : 0) + (world ? 1 : 0);
        if (hits != 1) return Group.TODO;
        return energy ? Group.ENERGIA : system ? Group.SISTEMA : motion ? Group.MOVIMIENTO : pose ? Group.ORIENTACION : Group.ENTORNO;
    }

    // ------------------------------------------------------------------ una percepción

    /** Lo medido en una ráfaga. La rellena el servicio Android y se descarta al devolver el texto. */
    public static final class Perception {
        private final EnumMap<Kind, Boolean> present = new EnumMap<>(Kind.class);
        private final EnumMap<Kind, float[]> samples = new EnumMap<>(Kind.class);
        private final EnumMap<Group, List<String>> facts = new EnumMap<>(Group.class);
        private final List<String> inventory = new ArrayList<>();
        private Float headingDegrees;
        private boolean compassPresent;

        /** El teléfono tiene (o no) ese sensor. */
        public synchronized void present(Kind kind, boolean available) { if (kind != null) present.put(kind, available); }

        /** Última muestra del sensor; valores no finitos o incompletos se ignoran. */
        public synchronized void sample(Kind kind, float[] values) {
            if (kind == null || values == null) return;
            int needed = kind.shape == Shape.SCALAR ? 1 : 3;
            if (values.length < needed) return;
            float[] copy = new float[needed];
            for (int i = 0; i < needed; i++) {
                if (!Float.isFinite(values[i])) return;
                copy[i] = values[i];
            }
            samples.put(kind, copy);
        }

        /** Rumbo de la parte superior del teléfono, en grados desde el norte magnético. */
        public synchronized void heading(boolean available, float degrees) {
            compassPresent = available;
            headingDegrees = available && Float.isFinite(degrees) ? ((degrees % 360f) + 360f) % 360f : null;
        }

        /** Dato del sistema ya medido (batería, RAM, red...), una línea con su unidad. */
        public synchronized void fact(Group group, String line) {
            if (group == null || line == null || line.trim().isEmpty()) return;
            List<String> lines = facts.get(group);
            if (lines == null) facts.put(group, lines = new ArrayList<>());
            lines.add(line.trim());
        }

        public synchronized void inventory(String sensorName) {
            if (sensorName != null && !sensorName.trim().isEmpty() && inventory.size() < 80) inventory.add(sensorName.trim());
        }

        public synchronized boolean has(Kind kind) { return samples.containsKey(kind); }

        public synchronized String render(Group wanted) {
            Group group = wanted == null ? Group.TODO : wanted;
            StringBuilder out = new StringBuilder();
            for (Group section : new Group[]{Group.ENTORNO, Group.MOVIMIENTO, Group.ORIENTACION, Group.ENERGIA, Group.SISTEMA}) {
                if (group != Group.TODO && group != section) continue;
                List<String> lines = new ArrayList<>();
                for (Kind kind : Kind.values()) {
                    if (kind.group != section) continue;
                    String line = describe(kind);
                    // En el resumen completo se omiten los sensores que este teléfono no tiene.
                    if (line != null && !(group == Group.TODO && line.endsWith("no disponible en este teléfono."))) lines.add(line);
                }
                if (section == Group.ORIENTACION) {
                    if (headingDegrees != null) lines.add("Rumbo: " + Math.round(headingDegrees) + "° (" + compass(headingDegrees)
                            + "), hacia donde apunta el borde superior del teléfono; orientativo si hay metal o imanes cerca.");
                    else if (group != Group.TODO) lines.add(compassPresent ? "Rumbo: sin lectura en esta ráfaga."
                            : "Rumbo: no disponible en este teléfono.");
                }
                List<String> extra = facts.get(section);
                if (extra != null) lines.addAll(extra);
                if (lines.isEmpty()) continue;
                out.append(section.name()).append(": ");
                for (int i = 0; i < lines.size(); i++) out.append(i == 0 ? "" : " ").append(lines.get(i));
                out.append('\n');
            }
            if (group == Group.INVENTARIO || group == Group.TODO) {
                if (!inventory.isEmpty()) {
                    out.append("INVENTARIO: ").append(inventory.size()).append(" sensores");
                    if (group == Group.INVENTARIO) {
                        out.append(": ");
                        for (int i = 0; i < inventory.size(); i++) out.append(i == 0 ? "" : "; ").append(inventory.get(i));
                    }
                    out.append(".\n");
                } else if (group == Group.INVENTARIO) out.append("INVENTARIO: Android no entregó la lista de sensores.\n");
            }
            if (out.length() == 0) return "Sin lecturas: ningún sensor de ese grupo respondió en esta ráfaga.";
            return out.toString().trim();
        }

        private String describe(Kind kind) {
            Boolean available = present.get(kind);
            if (available == null) return null;
            if (!available) return kind.label + ": no disponible en este teléfono.";
            float[] v = samples.get(kind);
            if (v == null) return kind.label + ": sin lectura en esta ráfaga"
                    + (kind == Kind.STEP_COUNTER ? " (necesita permiso de actividad física o dar algún paso)." : ".");
            switch (kind) {
                case LIGHT: return line(kind, v[0], light(v[0]));
                case PROXIMITY: return line(kind, v[0], v[0] < 1f ? "algo tapa el sensor (mano, bolsillo o boca abajo)" : "despejado");
                case PRESSURE: return line(kind, v[0], "≈ " + Math.round(altitudeMeters(v[0]))
                        + " m de altitud con atmósfera estándar; orientativo, cambia con el tiempo");
                case MAGNETIC_FIELD: {
                    float m = magnitude(v);
                    return line(kind, m, m > 100f ? "muy alto: imán o metal cerca" : m < 20f ? "muy bajo: apantallado" : "normal para el campo terrestre");
                }
                case ACCELEROMETER: return line(kind, magnitude(v), null);
                case LINEAR_ACCELERATION: return line(kind, magnitude(v), movement(magnitude(v)));
                case GYROSCOPE: { float g = magnitude(v); return line(kind, g, g < .05f ? "sin girar" : g < 1f ? "girando despacio" : "girando rápido"); }
                case STEP_COUNTER: return kind.label + ": " + Math.round(v[0]) + ".";
                case GRAVITY: return kind.label + ": " + posture(v[0], v[1], v[2]) + ".";
                default: return line(kind, v[0], null);
            }
        }

        private static String line(Kind kind, float value, String meaning) {
            return kind.label + ": " + String.format(Locale.ROOT, value >= 100f ? "%.0f" : "%.1f", value) + " " + kind.unit
                    + (meaning == null ? "" : " (" + meaning + ")") + ".";
        }
    }

    // ------------------------------------------------------------------ interpretación (pura)

    static float magnitude(float[] v) { return (float) Math.sqrt((double) v[0] * v[0] + (double) v[1] * v[1] + (double) v[2] * v[2]); }

    static String light(float lux) {
        if (lux < 10f) return "a oscuras";
        if (lux < 200f) return "luz tenue de interior";
        if (lux < 2000f) return "interior bien iluminado";
        if (lux < 20000f) return "exterior nublado o a la sombra";
        return "pleno sol";
    }

    static String movement(float metersPerSecondSquared) {
        if (metersPerSecondSquared < .3f) return "quieto";
        if (metersPerSecondSquared < 1.5f) return "movimiento suave";
        return "movimiento brusco";
    }

    /** Fórmula barométrica internacional con 1013,25 hPa al nivel del mar. */
    static float altitudeMeters(float hPa) {
        if (!(hPa > 0)) return Float.NaN;
        return (float) (44330.0 * (1.0 - Math.pow(hPa / 1013.25, 1.0 / 5.255)));
    }

    /** Postura según la gravedad en los ejes del teléfono (x a la derecha, y hacia arriba, z saliendo de la pantalla). */
    static String posture(float gx, float gy, float gz) {
        float g = (float) Math.sqrt(gx * gx + gy * gy + gz * gz);
        if (!(g > 1f)) return "en caída libre o sin gravedad medible";
        float x = gx / g, y = gy / g, z = gz / g;
        if (z > .85f) return "tumbado boca arriba (pantalla hacia el techo)";
        if (z < -.85f) return "tumbado boca abajo (pantalla hacia el suelo)";
        if (y > .7f) return "en vertical";
        if (y < -.7f) return "en vertical, cabeza abajo";
        if (Math.abs(x) > .7f) return "apaisado, apoyado sobre su lado " + (x > 0 ? "izquierdo" : "derecho");
        return "inclinado, como en la mano";
    }

    static String compass(float degrees) {
        String[] names = {"norte", "noreste", "este", "sureste", "sur", "suroeste", "oeste", "noroeste"};
        return names[Math.round((((degrees % 360f) + 360f) % 360f) / 45f) % 8];
    }

    static String normalize(String value) {
        if (value == null) return "";
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9 ]+", " ").replaceAll("\\s+", " ").trim();
    }
}
