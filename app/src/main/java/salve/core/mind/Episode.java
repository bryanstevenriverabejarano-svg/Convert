package salve.core.mind;

/** Una experiencia registrada (normalmente un recuerdo de Room) que los ciclos pueden citar. */
public final class Episode {
    public final String id;
    public final String text;
    public final long at;

    public Episode(String id, String text, long at) {
        if (id == null || id.trim().isEmpty()) throw new IllegalArgumentException("Episodio sin identificador.");
        this.id = id.trim();
        this.text = text == null ? "" : text;
        this.at = at;
    }

    /** Identificador estable con el mismo formato que usa la memoria conversacional. */
    public static String memoryId(long recordId) { return "recuerdos:" + recordId; }

    /** Devuelve el número de recuerdo o -1 si el identificador no procede de la tabla de recuerdos. */
    public long memoryNumber() {
        if (!id.startsWith("recuerdos:")) return -1;
        try { return Long.parseLong(id.substring("recuerdos:".length())); }
        catch (NumberFormatException e) { return -1; }
    }
}
