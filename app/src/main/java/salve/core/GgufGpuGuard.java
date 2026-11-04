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
    private final String tryingKey, offKey, installKey;

    public GgufGpuGuard(LiteRtContextGuard.Store store) { this(store, TRYING, OFF, INSTALL); }

    /** Independent crash quarantine for Hexagon: a NPU crash must not disable Adreno. */
    public static GgufGpuGuard forNpu(LiteRtContextGuard.Store store) {
        return new GgufGpuGuard(store, "gguf_npu_trying", "gguf_npu_off", "gguf_npu_install");
    }

    private GgufGpuGuard(LiteRtContextGuard.Store store, String tryingKey, String offKey, String installKey) {
        this.store = store;
        this.tryingKey = tryingKey;
        this.offKey = offKey;
        this.installKey = installKey;
    }

    /** ¿Se puede probar la gráfica? Cada versión nueva vuelve a probar desde cero. */
    public boolean allowed(int installStamp) {
        if (installStamp != 0 && store.get(installKey) != installStamp) {
            store.put(offKey, 0);
            store.put(tryingKey, 0);
            store.put(installKey, installStamp);
        }
        if (store.get(tryingKey) != 0) { // la última vez la app murió usando la gráfica
            store.put(offKey, 1);
            store.put(tryingKey, 0);
        }
        return store.get(offKey) == 0;
    }

    /** Justo antes de cargar o de pensar la primera respuesta en la gráfica. */
    public void trying() { store.put(tryingKey, 1); }

    /** Cargó o respondió bien: nada que recordar. */
    public void fine() { store.put(tryingKey, 0); }

    /** Falló con un error normal: el procesador se queda hasta la próxima versión. */
    public void disable() {
        store.put(offKey, 1);
        store.put(tryingKey, 0);
    }

    public boolean disabled() { return store.get(offKey) != 0; }
}
