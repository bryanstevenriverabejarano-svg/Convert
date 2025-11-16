// SavePlugin.java
package salve.core;

/**
 * SavePlugin: interfaz que deben implementar todos los plugins
 * que Salve pueda cargar dinámicamente.
 */
public interface SavePlugin {
    /**
     * @return Puntuación de calidad del plugin (0.0 a 1.0).
     *         Solo se registran los que superen cierto umbral.
     */
    float score();

    /**
     * Método principal del plugin. Aquí va la lógica que
     * se integra en el motor de Salve.
     */
    void execute();
}