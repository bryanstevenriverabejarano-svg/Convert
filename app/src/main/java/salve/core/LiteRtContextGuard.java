package salve.core;

/**
 * Protección para el contexto ampliado. Android puede cerrar la app de golpe si al cargar el motor
 * con mucho contexto se queda sin memoria, y eso no deja ninguna excepción que capturar. Por eso,
 * antes de cargar con más de 4.096 tokens se apunta el intento; al terminar bien se borra. Si en la
 * siguiente carga el intento sigue apuntado, la app murió a mitad: se baja un escalón y se recuerda.
 * Lógica pura sobre un pequeño almacén (SharedPreferences en la app, un mapa en las pruebas).
 */
public final class LiteRtContextGuard {
    public interface Store {
        int get(String key);
        void put(String key, int value); // 0 = borrar
    }

    static final String TRYING = "litert_ctx_trying", CAP = "litert_ctx_cap";

    private final Store store;

    public LiteRtContextGuard(Store store) { this.store = store; }

    /**
     * Contexto a usar de verdad: el pedido, sin pasar del tope aprendido. Si la carga anterior quedó a
     * medias con N tokens, el tope pasa a ser el escalón inferior a N.
     */
    public int admit(int requested) {
        int crashed = store.get(TRYING);
        if (crashed > ModelMemoryPolicy.LITERT_STANDARD) {
            int cap = below(crashed);
            int old = store.get(CAP);
            store.put(CAP, old == 0 ? cap : Math.min(old, cap));
            store.put(TRYING, 0);
        }
        int cap = store.get(CAP);
        return cap == 0 ? requested : Math.min(requested, cap);
    }

    /** Llamar justo antes de cargar el motor con {@code tokens}. */
    public void loading(int tokens) {
        store.put(TRYING, tokens > ModelMemoryPolicy.LITERT_STANDARD ? tokens : 0);
    }

    /** La carga terminó (bien, o con un error normal que ya reintentó con menos): nada que recordar. */
    public void loaded() { store.put(TRYING, 0); }

    /** Tope aprendido (0 = ninguno). */
    public int cap() { return store.get(CAP); }

    /** Bryan puede volver a probar el máximo (p. ej. tras liberar RAM o cambiar de móvil). */
    public void reset() { store.put(CAP, 0); store.put(TRYING, 0); }

    static int below(int tokens) {
        int[] tiers = {ModelMemoryPolicy.LITERT_16K, ModelMemoryPolicy.LITERT_8K,
                ModelMemoryPolicy.LITERT_LONG, ModelMemoryPolicy.LITERT_STANDARD};
        for (int t : tiers) if (t < tokens) return t;
        return ModelMemoryPolicy.LITERT_STANDARD;
    }

    /**
     * Caracteres de prompt de conversación según el contexto realmente cargado. Con 4.096 se mantiene
     * el tope de siempre; con más, crece por escalones dejando sitio a pensar y responder (unos 3,5
     * caracteres por token en español): 8.192 → 16.000 caracteres, 16.384 → 22.000.
     */
    public static int promptBudget(int loadedTokens) {
        if (loadedTokens >= ModelMemoryPolicy.LITERT_16K) return 22000;
        if (loadedTokens >= ModelMemoryPolicy.LITERT_8K) return 16000;
        return 10500;
    }
}
