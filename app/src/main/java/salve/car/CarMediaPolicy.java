package salve.car;

/** Quién puede ver a Salve como reproductor. Lógica pura para pruebas JVM. */
public final class CarMediaPolicy {
    /** Android Auto (proyección desde el móvil). */
    static final String ANDROID_AUTO = "com.google.android.projection.gearhead";
    static final String TAP_PHONE = "Toca «Modo conducción» en el móvil para empezar.";

    private CarMediaPolicy() { }

    /**
     * Solo Android Auto y la propia Salve: la última frase de Salve puede ser privada y no debe verla
     * cualquier app que sepa listar reproductores.
     */
    public static boolean allowed(String clientPackage, String ownPackage) {
        if (clientPackage == null) return false;
        return ANDROID_AUTO.equals(clientPackage) || clientPackage.equals(ownPackage);
    }
}
