package salve.core.satellite;

/**
 * Qué es esta instalación de la app. Separa el caso de Bryan (su Salve de siempre, con su memoria y
 * sus nodos) del de una copia recién compilada por otra persona desde el repositorio público.
 *
 * <p>No es un candado anticopia —el código es abierto y cualquiera puede recompilar— sino el
 * comportamiento por defecto: una instalación sin dueño no nace como una Salve independiente y vacía,
 * sino que se ofrece a unirse a una Salve existente como <b>satélite</b> (un móvil que la ayuda con
 * cómputo, pantalla o sensores). La que piensa, recuerda y decide sigue siendo la Salve del dueño.</p>
 *
 * <p>No toca el sistema de nodos de cómputo ni los Salventes: es una capa de identidad del arranque.</p>
 */
public enum InstallRole {
    /** La Salve del dueño: funciona completa, con su memoria, su identidad y sus nodos. */
    DUENO,
    /** Orbita y sirve a la Salve de un dueño; no lleva una Salve independiente. */
    SATELITE,
    /** Instalación nueva sin decidir: al abrir se ofrece elegir ser dueña o unirse como satélite. */
    SIN_RECLAMAR;

    /**
     * Decide el rol al arrancar, sin tocar nada.
     *
     * @param claimedOwner esta instalación ya se declaró dueña (hay una reclamación guardada)
     * @param satelliteOf  esta instalación ya está unida a una Salve como satélite
     * @param hasExistingData hay memoria, recuerdos o una verificación previa de un uso anterior
     *                        (la Salve de Bryan de antes de esta función: se respeta como dueña)
     */
    public static InstallRole decide(boolean claimedOwner, boolean satelliteOf, boolean hasExistingData) {
        if (satelliteOf) return SATELITE;
        if (claimedOwner) return DUENO;
        // Una instalación que ya venía usándose es de su dueño: esta función nunca la degrada.
        if (hasExistingData) return DUENO;
        return SIN_RECLAMAR;
    }

    public boolean fullSalve() { return this == DUENO; }
}
