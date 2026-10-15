package salve.core.identity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.lang.ref.WeakReference;
import java.util.function.Consumer;

/**
 * Gestiona la certeza sobre quién habla con Salve.
 *
 * <p>Usa el {@code BiometricPrompt} del sistema: Android elige rostro o huella según lo que haya
 * registrado y permite el PIN como alternativa. Eso prueba que quien lo usa es el dueño del
 * teléfono, que en este dispositivo se presume Bryan. No se analizan caras con modelos propios:
 * no satura la memoria ni compite con el modelo de lenguaje.</p>
 *
 * <p>La verificación automática al abrir la app solo se lanza cuando no hay un turno de
 * conversación en curso y tras dejar que la interfaz se asiente; si el sistema está ocupado, se
 * aplaza hasta que haga falta (por ejemplo, al pedir algo privado).</p>
 */
public final class IdentityManager {
    private static final String TAG = "Salve/Identity";
    private static final String PREFS = "salve_identity";
    private static final String KEY_VERIFIED_AT = "verified_at";
    private static final int AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL;
    private static final long START_DELAY_MS = 1500;

    private static volatile IdentityManager instance;

    private final Context app;
    private final SessionIdentity session = new SessionIdentity();
    private final Handler main = new Handler(Looper.getMainLooper());
    private WeakReference<FragmentActivity> activity = new WeakReference<>(null);
    private volatile boolean prompting;
    private volatile boolean askedThisResume;

    private IdentityManager(Context context) {
        app = context.getApplicationContext();
        session.restore(prefs().getLong(KEY_VERIFIED_AT, 0L), System.currentTimeMillis());
    }

    public static IdentityManager get(Context context) {
        if (instance == null) {
            synchronized (IdentityManager.class) {
                if (instance == null) instance = new IdentityManager(context);
            }
        }
        return instance;
    }

    public SessionIdentity session() {
        session.expire(System.currentTimeMillis());
        return session;
    }

    public boolean biometricAvailable() {
        try {
            return BiometricManager.from(app).canAuthenticate(AUTHENTICATORS) == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (RuntimeException unavailable) {
            return false;
        }
    }

    public boolean privateAccess() {
        return session().privateAccess(biometricAvailable(), System.currentTimeMillis());
    }

    /** La actividad visible; se guarda débilmente para no retenerla. */
    public void attach(FragmentActivity current) {
        activity = new WeakReference<>(current);
        askedThisResume = false;
    }

    public void detach(FragmentActivity current) {
        if (activity.get() == current) activity = new WeakReference<>(null);
    }

    /**
     * Al volver a la app: si no hay verificación vigente y el sistema está libre, pide la
     * verificación una vez. {@code busy} debe ser true mientras haya un turno o una carga de modelo.
     */
    public void verifyOnStartIfIdle(java.util.function.BooleanSupplier busy) {
        if (askedThisResume || !biometricAvailable()) return;
        if (session().state() == SessionIdentity.State.VERIFIED_CREATOR) return;
        askedThisResume = true;
        main.postDelayed(() -> {
            if (busy != null && busy.getAsBoolean()) {
                Log.i(TAG, "Verificación aplazada: el sistema está ocupado.");
                return;
            }
            requestVerification("Salve quiere saber que eres tú", null);
        }, START_DELAY_MS);
    }

    /** Pide huella, rostro o PIN. {@code result} recibe true si se verifica. */
    public void requestVerification(String subtitle, Consumer<Boolean> result) {
        main.post(() -> {
            FragmentActivity host = activity.get();
            if (host == null || host.isFinishing() || prompting || !biometricAvailable()) {
                if (result != null) result.accept(false);
                return;
            }
            prompting = true;
            BiometricPrompt prompt = new BiometricPrompt(host, ContextCompat.getMainExecutor(host),
                    new BiometricPrompt.AuthenticationCallback() {
                        @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                            prompting = false;
                            long now = System.currentTimeMillis();
                            session.onBiometricSuccess(now);
                            prefs().edit().putLong(KEY_VERIFIED_AT, now).apply();
                            if (result != null) result.accept(true);
                        }
                        @Override public void onAuthenticationError(int code, CharSequence message) {
                            prompting = false;
                            session.onBiometricRejected(System.currentTimeMillis());
                            if (result != null) result.accept(false);
                        }
                        // onAuthenticationFailed: intento no reconocido; el diálogo sigue abierto.
                    });
            try {
                prompt.authenticate(new BiometricPrompt.PromptInfo.Builder()
                        .setTitle("¿Eres tú?")
                        .setSubtitle(subtitle == null ? "Verificación de Salve" : subtitle)
                        .setAllowedAuthenticators(AUTHENTICATORS)
                        .build());
            } catch (RuntimeException failure) {
                prompting = false;
                Log.w(TAG, "No se pudo mostrar la verificación: " + failure.getClass().getSimpleName());
                if (result != null) result.accept(false);
            }
        });
    }

    /** Olvida la verificación (por ejemplo, «no soy Bryan»). */
    public void clearVerification() {
        prefs().edit().remove(KEY_VERIFIED_AT).apply();
    }

    private SharedPreferences prefs() {
        return app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
