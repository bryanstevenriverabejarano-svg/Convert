package salve.core.privatefamily;

import com.google.gson.JsonObject;

/** A single owner-approved fix, never a location history. Time is UTC epoch seconds. */
public final class PrivateLocation {
    public static final long GPS_TTL = 900;
    public final double latitude, longitude, accuracy;
    public final String city, zone, source;
    public final long capturedAt;
    public PrivateLocation(double latitude, double longitude, String city, String zone, String source, long capturedAt, double accuracy, long now) {
        if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90 || !Double.isFinite(longitude) || longitude < -180 || longitude > 180
                || !Double.isFinite(accuracy) || accuracy < 0 || accuracy > 100000 || capturedAt <= 0 || capturedAt > now + 60
                || !("static".equals(source) || "gps".equals(source)) || !label(city) || !label(zone))
            throw new IllegalArgumentException("Coordenadas o fecha inválidas");
        this.latitude = latitude; this.longitude = longitude; this.city = city; this.zone = zone;
        this.source = source; this.capturedAt = capturedAt; this.accuracy = accuracy;
    }
    private static boolean label(String value) { return value != null && value.length() <= 100 && !value.matches("(?s).*[\\p{Cntrl}].*"); }
    public boolean fresh(long now) { return capturedAt <= now + 60 && (!source.equals("gps") || now - capturedAt <= GPS_TTL); }
    public JsonObject json() {
        JsonObject result = new JsonObject(); result.addProperty("latitude", latitude); result.addProperty("longitude", longitude);
        result.addProperty("city", city); result.addProperty("zone", zone); result.addProperty("source", source);
        result.addProperty("captured_at", capturedAt); result.addProperty("accuracy_m", accuracy); return result;
    }
    public static PrivateLocation parse(JsonObject json, long now) {
        return new PrivateLocation(json.get("latitude").getAsDouble(), json.get("longitude").getAsDouble(),
                json.get("city").getAsString(), json.get("zone").getAsString(), json.get("source").getAsString(),
                json.get("captured_at").getAsLong(), json.get("accuracy_m").getAsDouble(), now);
    }
}
