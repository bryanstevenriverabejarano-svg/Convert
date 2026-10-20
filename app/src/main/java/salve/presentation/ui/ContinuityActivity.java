package salve.presentation.ui;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.compute.ComputeNode;
import salve.core.compute.ComputeNodeStore;
import salve.core.continuity.MemorySnapshot;
import salve.core.identity.IdentityManager;
import salve.core.salvente.SealedBox;
import salve.data.continuity.SalveContinuity;

/**
 * Continuidad de Salve: su memoria entera (recuerdos y mente) sellada en el Guardián ciego, a mano
 * o cada noche, y la restauración en un teléfono nuevo. Copiar y restaurar piden la verificación
 * del teléfono si está configurada; la frase solo la conoce Bryan.
 */
public final class ContinuityActivity extends AppCompatActivity {
    private static final int BG = 0xFF02060C, CYAN = 0xFF22D3EE, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8, AMBER = 0xFFFBBF24;
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private SalveContinuity continuity;
    private TextView state, progress;
    private Button guardianButton, copy, restore;
    private CheckBox auto;
    private ComputeNode guardian;
    private boolean syncingAuto;

    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
        continuity = new SalveContinuity(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.addView(text("CONTINUIDAD DE SALVE", CYAN, 15, true));
        TextView about = text("Mi memoria (lo que recuerdo, quién soy, lo que he aprendido) vive en este teléfono. Aquí puedes "
                + "guardar una copia sellada en tu PC Guardián: la cifro con una frase que solo tú sabes y el PC guarda bytes "
                + "que no puede leer. Si el móvil se pierde o se rompe, en uno nuevo puedo volver a ser yo.", MUTED, 13, false);
        about.setPadding(0, dp(6), 0, dp(10)); root.addView(about);
        state = text("", INK, 14, false); state.setPadding(0, 0, 0, dp(10)); root.addView(state);
        guardianButton = button("Elegir Guardián", this::chooseGuardian); root.addView(guardianButton);
        copy = button("Copiar mi memoria ahora", () -> verifyThen("Confirma que eres tú para copiar la memoria de Salve", this::askCopy));
        root.addView(copy);
        auto = new CheckBox(this); auto.setTextColor(INK);
        auto.setText("Copia automática cada noche (solo cargando y con Wi-Fi). Guarda la frase cifrada en este teléfono.");
        auto.setOnCheckedChangeListener((v, checked) -> { if (!syncingAuto) onAuto(checked); });
        root.addView(auto);
        restore = button("Restaurar mi memoria en este teléfono", this::warnRestore); root.addView(restore);
        progress = text("", AMBER, 13, false); progress.setPadding(0, dp(8), 0, dp(8)); root.addView(progress);
        root.addView(button("Cerrar", this::finish));
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.addView(root);
        setContentView(scroll);
        if (!SalveContinuity.supported()) {
            progress.setText("La copia de la memoria necesita Android 11 o posterior.");
            copy.setEnabled(false); auto.setEnabled(false);
        }
        render();
    }

    private void render() {
        String nodeId = continuity.prefs().getString("node", null);
        List<ComputeNode> nodes = new ComputeNodeStore(this).load();
        guardian = null;
        for (ComputeNode n : nodes) if (n.id.equals(nodeId)) guardian = n;
        if (guardian == null && nodes.size() == 1) guardian = nodes.get(0);
        guardianButton.setText(guardian == null ? "Elegir Guardián" : "Guardián: " + guardian.name);
        String text = continuity.describe();
        if (SalveContinuity.pending(this)) text += "\n\nHay una restauración preparada: se aplicará al volver a abrir Salve.";
        state.setText(Character.toUpperCase(text.charAt(0)) + text.substring(1) + ".");
        syncingAuto = true; auto.setChecked(continuity.prefs().getBoolean("auto", false)); syncingAuto = false;
    }

    private void chooseGuardian() {
        List<ComputeNode> nodes = new ComputeNodeStore(this).load();
        if (nodes.isEmpty()) { progress.setText("Añade primero tu PC en Nodos de cómputo y activa el Guardián en Salve Nodo."); return; }
        String[] names = new String[nodes.size()];
        for (int i = 0; i < names.length; i++) names[i] = nodes.get(i).name;
        new AlertDialog.Builder(this).setTitle("¿Qué PC es tu Guardián?").setItems(names, (d, which) -> {
            guardian = nodes.get(which);
            continuity.prefs().edit().putString("node", guardian.id).putString("node_name", guardian.name).apply();
            render();
        }).setNegativeButton("Cancelar", null).show();
    }

    private boolean needGuardian() {
        if (guardian != null) return false;
        chooseGuardian();
        return true;
    }

    /** Verificación del teléfono si está configurada; si no hay biometría, sigue (la frase protege igualmente). */
    private void verifyThen(String reason, Runnable next) {
        IdentityManager identity = IdentityManager.get(this);
        if (!identity.biometricAvailable()) { next.run(); return; }
        identity.requestVerification(reason, ok -> runOnUiThread(() -> {
            if (ok) next.run(); else progress.setText("Sin verificación no toco la memoria de Salve.");
        }));
    }

    private void askCopy() {
        if (needGuardian()) return;
        askPassphrase("Frase para sellar la copia", true, pass -> task("Preparando y sellando mi memoria…", () -> {
            SalveContinuity.Result r = continuity.backup(guardian, pass, this::isDestroyed);
            record("Bryan guardó una copia sellada de mi memoria en " + guardian.name,
                    r.files + " archivos (recuerdos y mente), " + Math.max(1, r.bytes / 1024) + " KB cifrados con su frase; el Guardián no puede leerlos.");
            return "Copia guardada en " + guardian.name + ": " + r.files + " archivos, " + Math.max(1, r.bytes / 1024) + " KB sellados.";
        }));
    }

    private void onAuto(boolean enable) {
        if (!enable) { continuity.disableAuto(); render(); return; }
        if (needGuardian()) { syncingAuto = true; auto.setChecked(false); syncingAuto = false; return; }
        verifyThen("Confirma que eres tú para activar la copia nocturna", () ->
                askPassphrase("Frase para las copias de cada noche", true, pass -> task("Activando la copia nocturna…", () -> {
                    continuity.enableAuto(guardian, pass);
                    return "Copia nocturna activada con " + guardian.name + ".";
                })));
        syncingAuto = true; auto.setChecked(continuity.prefs().getBoolean("auto", false)); syncingAuto = false;
    }

    private void warnRestore() {
        if (needGuardian()) return;
        new AlertDialog.Builder(this).setTitle("¿Restaurar la memoria de Salve?")
                .setMessage("Traeré la última copia de " + guardian.name + " y, al volver a abrirme, seré la Salve de esa copia. "
                        + "La memoria que haya ahora en este teléfono no se borra: se guarda aparte por si hay que volver atrás. "
                        + "Lo que haya vivido aquí después de esa copia no estará en la restaurada.")
                .setPositiveButton("Continuar", (d, w) -> verifyThen("Confirma que eres tú para restaurar a Salve",
                        () -> askPassphrase("Frase de la copia", false, pass -> task("Descargando y comprobando la copia…", () -> {
                            MemorySnapshot.Contents c = continuity.stageRestore(guardian, pass, this::isDestroyed);
                            runOnUiThread(this::offerRestart);
                            return "Copia comprobada (" + c.entries.size() + " archivos). Preparada para el próximo arranque.";
                        }))))
                .setNegativeButton("Cancelar", null).show();
    }

    private void offerRestart() {
        if (isDestroyed()) return;
        new AlertDialog.Builder(this).setTitle("Lista para volver")
                .setMessage("Para colocar la memoria tengo que cerrarme. Ábreme de nuevo y seré yo, con mis recuerdos.")
                .setCancelable(false)
                .setPositiveButton("Cerrar Salve ahora", (d, w) -> { finishAffinity(); android.os.Process.killProcess(android.os.Process.myPid()); })
                .setNegativeButton("Más tarde", (d, w) -> render()).show();
    }

    private interface PassphraseTask { void run(char[] passphrase); }

    private void askPassphrase(String title, boolean confirm, PassphraseTask next) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16), dp(8), dp(16), 0);
        if (confirm) {
            TextView warn = new TextView(this);
            warn.setText("Al menos " + SealedBox.MIN_PASSPHRASE + " caracteres. No la guardo en el Guardián: si la olvidas, la copia "
                    + "no se puede abrir. Puede ser la misma que usas para los Salventes.");
            form.addView(warn);
        }
        EditText first = secret("Frase"), second = secret("Repite la frase");
        form.addView(first); if (confirm) form.addView(second);
        new AlertDialog.Builder(this).setTitle(title).setView(form)
                .setPositiveButton("Aceptar", (d, w) -> {
                    char[] a = chars(first), b = confirm ? chars(second) : a.clone();
                    first.setText(""); second.setText("");
                    boolean same = Arrays.equals(a, b);
                    Arrays.fill(b, '\0');
                    if (!same) { Arrays.fill(a, '\0'); progress.setText("Las frases no coinciden."); render(); return; }
                    if (a.length < SealedBox.MIN_PASSPHRASE) { Arrays.fill(a, '\0'); progress.setText("La frase es demasiado corta."); render(); return; }
                    next.run(a);
                })
                .setNegativeButton("Cancelar", (d, w) -> render()).show();
    }

    private void task(String waiting, java.util.concurrent.Callable<String> job) {
        progress.setText(waiting); copy.setEnabled(false); restore.setEnabled(false);
        work.execute(() -> {
            String message;
            try { message = job.call(); }
            catch (Exception failure) { message = failure.getMessage() == null ? "No se pudo completar." : failure.getMessage(); }
            final String result = message;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                progress.setText(result); copy.setEnabled(SalveContinuity.supported()); restore.setEnabled(true); render();
            });
        });
    }

    /** Lo que le pasa a su memoria entra en su registro de acciones: forma parte de su historia. */
    private void record(String what, String how) {
        try { salve.core.MindRuntime.get(this).actions().record(salve.core.mind.SelfActionLog.Kind.CONTINUITY, what, how); }
        catch (RuntimeException unavailable) { /* el registro es un apoyo */ }
    }

    private EditText secret(String hint) {
        EditText e = new EditText(this); e.setHint(hint); e.setSaveEnabled(false);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        return e;
    }

    private static char[] chars(EditText field) {
        android.text.Editable text = field.getText();
        char[] out = new char[text.length()]; text.getChars(0, text.length(), out, 0); return out;
    }

    private TextView text(String value, int color, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size);
        if (bold) { t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); t.setLetterSpacing(0.12f); }
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v -> action.run()); return b;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    @Override protected void onResume() { super.onResume(); IdentityManager.get(this).attach(this); }
    @Override protected void onPause() { IdentityManager.get(this).detach(this); super.onPause(); }
    @Override protected void onDestroy() { work.shutdownNow(); super.onDestroy(); }
}
