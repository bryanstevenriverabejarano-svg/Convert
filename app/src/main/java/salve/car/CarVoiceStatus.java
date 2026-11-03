package salve.car;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Estado de la conversación por voz para la pantalla del coche («Te escucho…», «Pensando…»,
 * «Hablando…»). Lo publica el servicio de voz; la pantalla de Android Auto lo escucha y se redibuja.
 * {@code null} = no hay conversación abierta. Lógica pura para pruebas JVM.
 */
public final class CarVoiceStatus {
    public interface Listener { void onChanged(); }

    private static volatile String state;
    private static final CopyOnWriteArrayList<Listener> LISTENERS = new CopyOnWriteArrayList<>();

    private CarVoiceStatus() { }

    /** Nuevo estado de la voz, o {@code null} al terminar la conversación. */
    public static void set(String value) {
        String next = value == null || value.trim().isEmpty() ? null : value.trim();
        String previous = state;
        state = next;
        if (previous == null ? next == null : previous.equals(next)) return;
        for (Listener l : LISTENERS) {
            try { l.onChanged(); } catch (RuntimeException ignored) { /* una pantalla rota no bloquea a las demás */ }
        }
    }

    public static boolean active() { return state != null; }

    /** Línea principal de la pantalla del coche. */
    public static String line() { return state == null ? "Lista para hablar contigo" : state; }

    /** Ayuda corta, sin distraer al volante. */
    public static String hint() {
        return state == null ? "Pulsa «Hablar con Salve»." : "Di «adiós, Salve» para terminar.";
    }

    public static void addListener(Listener l) { if (l != null) LISTENERS.addIfAbsent(l); }
    public static void removeListener(Listener l) { LISTENERS.remove(l); }
}
