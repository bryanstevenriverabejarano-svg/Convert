package salve.core;

/**
 * Protección del modo gráfica (OpenCL en la GPU Adreno) del motor GGUF. Si el driver de la GPU
 * tumba la app al cargar o al pensar, no queda ninguna excepción que capturar: por eso se apunta el
 * intento ANTES de cargar y mientras piensa la primera respuesta. Si al arrancar sigue apuntado, la
 * app murió a mitad y la gráfica se apaga hasta la siguiente versión instalada (que vuelve a probar).
 * Un fallo normal (excepción) también la apaga y Salve sigue con el procesador.
 * Lógica pura sobre un pequeño almacén (SharedPreferences en la app, un mapa en las pruebas).
 */
public final class GgufGpuGuard {
    static final String TRYING = "gguf_gpu_trying", OFF = "gguf_gpu_off", INSTALL = "gguf_gpu_install";

    private final LiteRtContextGuard.Store store;

    public GgufGpuGuard(LiteRtContextGuard.Store store) { this.store = store; }

    /** ¿Se puede probar la gráfica? Cada versión nueva vuelve a probar desde cero. */
    public boolean allowed(int installStamp) {
        if (installStamp != 0 && store.get(INSTALL) != installStamp) {
            store.put(OFF, 0);
            store.put(TRYING, 0);
            store.put(INSTALL, installStamp);
        }
        if (store.get(TRYING) != 0) { // la última vez la app murió usando la gráfica
            store.put(OFF, 1);
            store.put(TRYING, 0);
        }
        return store.get(OFF) == 0;
    }

    /** Justo antes de cargar o de pensar la primera respuesta en la gráfica. */
    public void trying() { store.put(TRYING, 1); }

    /** Cargó o respondió bien: nada que recordar. */
    public void fine() { store.put(TRYING, 0); }

    /** Falló con un error normal: el procesador se queda hasta la próxima versión. */
    public void disable() {
        store.put(OFF, 1);
        store.put(TRYING, 0);
    }

    public boolean disabled() { return store.get(OFF) != 0; }
}
