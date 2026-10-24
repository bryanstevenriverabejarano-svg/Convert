package salve.core.synergy;

/** Device-aware resource gate. */
public final class SynergyCore {
    private SynergyCore() {}
    public static boolean batterySafe(int percentage, boolean overheated) {
        return percentage >= 20 && !overheated;
    }
}
