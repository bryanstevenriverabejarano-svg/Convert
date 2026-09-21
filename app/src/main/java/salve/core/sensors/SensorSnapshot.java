package salve.core.sensors;

import java.util.EnumMap;
import java.util.Locale;

/** Temporary observations only: no persistence, inferred emotions or user memories. */
public final class SensorSnapshot {
    public enum Reading {
        LIGHT("Luz", "lux"),
        ACCELERATION("Aceleración total, incluida la gravedad", "m/s²"),
        PROXIMITY("Proximidad", "cm"),
        AMBIENT_TEMPERATURE("Temperatura ambiente", "°C"),
        BATTERY("Batería", "%");

        final String label;
        final String unit;

        Reading(String label, String unit) {
            this.label = label;
            this.unit = unit;
        }
    }

    private static final class Sample {
        final double value;
        final long timestamp;

        Sample(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    private final EnumMap<Reading, Boolean> available = new EnumMap<>(Reading.class);
    private final EnumMap<Reading, Sample> samples = new EnumMap<>(Reading.class);

    public void setAvailable(Reading reading, boolean supported) {
        available.put(reading, supported);
        if (!supported) samples.remove(reading);
    }

    public boolean record(Reading reading, double value, long sampledAt) {
        if (!Boolean.TRUE.equals(available.get(reading)) || sampledAt < 0L
                || !Double.isFinite(value) || !validValue(reading, value)) {
            samples.remove(reading);
            return false;
        }
        samples.put(reading, new Sample(value, sampledAt));
        return true;
    }

    public boolean recordBattery(int level, int scale, long sampledAt) {
        if (scale <= 0 || level < 0 || level > scale) {
            samples.remove(Reading.BATTERY);
            return false;
        }
        return record(Reading.BATTERY, level * 100.0 / scale, sampledAt);
    }

    private static boolean validValue(Reading reading, double value) {
        if (reading == Reading.AMBIENT_TEMPERATURE) return value >= -273.15;
        if (reading == Reading.BATTERY) return value >= 0.0 && value <= 100.0;
        return value >= 0.0;
    }

    public boolean hasFreshReading(Reading reading, long now) {
        Sample sample = samples.get(reading);
        return sample != null && SessionPolicy.isFresh(sample.timestamp, now);
    }

    public String describe(long now) {
        StringBuilder result = new StringBuilder();
        for (Reading reading : Reading.values()) {
            if (result.length() > 0) result.append(' ');
            result.append(reading.label).append(": ");
            if (!Boolean.TRUE.equals(available.get(reading))) {
                result.append("no disponible.");
            } else if (!hasFreshReading(reading, now)) {
                result.append("sin lectura reciente.");
            } else {
                Sample sample = samples.get(reading);
                result.append(String.format(Locale.ROOT, "%.1f %s (hace %.1f s).",
                        sample.value, reading.unit, (now - sample.timestamp) / 1000.0));
            }
        }
        return result.toString();
    }

    public void clear() {
        available.clear();
        samples.clear();
    }
}
