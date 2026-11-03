package salve.core.driving;

/**
 * Policy for Android Auto (phone projection), never a substitute for a car-host app.
 * We intentionally do not enable driving mode for an ordinary USB/Bluetooth connection.
 */
public final class DrivingModePolicy {
    private DrivingModePolicy() { }

    /** CarConnection.CONNECTION_TYPE_PROJECTION == 2; kept pure for JVM unit tests. */
    public static boolean active(boolean allowed, int connectionType) {
        return allowed && connectionType == 2;
    }

    public static String status(boolean enabled, int connectionType) {
        if (!enabled) return "Modo conducción desactivado.";
        if (connectionType == 2) return "Android Auto conectado. Salve está en modo conducción.";
        if (connectionType == 1) return "Android Automotive detectado (no es Android Auto proyectado).";
        if (connectionType == 0) return "Sin conexión Android Auto. Salve seguirá en modo normal.";
        return "Comprobando conexión con Android Auto…";
    }
}
