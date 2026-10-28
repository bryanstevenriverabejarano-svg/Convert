package salve.core.satellite;

/**
 * Qué es esta instalación de la app. Separa el caso de Bryan (su Salve de siempre, con su memoria y
 * sus nodos) del de una copia recién compilada por otra persona desde el repositorio público.
 *
 * <p>No es un candado anticopia —el código es abierto y cualquiera puede recompilar— sino el
 * comportamiento por defecto: una instalación sin dueño no nace como una Salve independiente y vacía.
 * Se instala <b>sola como satélite</b>, sin preguntar nada: un móvil que se une a una Salve existente
 * para ayudarla con cómputo, pantalla y sensores. La que piensa, recuerda y decide sigue siendo la
 * Salve del dueño.</p>
 *
 * <p>No toca el sistema de nodos de cómputo ni los Salventes: es una capa de identidad del arranque.</p>
 */
public enum InstallRole {
    /** La Salve del dueño: funciona completa, con su memoria, su identidad y sus nodos. */
    DUENO,
    /** Orbita y sirve a la Salve de un dueño; no lleva una Salve independiente. El valor por defecto. */
    SATELITE;

    /**
     * Decide el rol al arrancar, sin tocar nada y sin preguntar. Por defecto, satélite: solo es
     * dueña si ya se declaró dueña explícitamente o si ya venía usándose (memoria, recuerdos o una
     * verificación previa). Así la Salve de Bryan es siempre dueña y nunca se le pregunta nada, y
     * una copia nueva se instala sola como satélite.
     *
     * @param claimedOwner    esta instalación se declaró dueña por una acción explícita del dueño
     * @param hasExistingData hay memoria, recuerdos o una verificación previa de un uso anterior
     */
    public static InstallRole decide(boolean claimedOwner, boolean hasExistingData) {
        return claimedOwner || hasExistingData ? DUENO : SATELITE;
    }

    public boolean fullSalve() { return this == DUENO; }
}
