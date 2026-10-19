package salve.presentation.ui;

import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.ModelResult;
import salve.core.SalveLLM;
import salve.core.salvente.SalventeCapability;
import salve.core.salvente.SalventeGenesisEngine;
import salve.core.salvente.SalventeGenesisSession;
import salve.core.salvente.SalventeProfile;
import salve.core.salvente.SalventeProfileValidator;
import salve.core.salvente.SalventeTrait;
import salve.data.salvente.SalventeRepository;

/**
 * Génesis de Salventes: Bryan describe para qué o para quién es, Salve propone una ficha (con su
 * modelo, vía nodo o móvil, o con el motor determinista si no hay modelo), y solo al pulsar
 * «Crear» se guarda. «Cancelar» lo descarta sin dejar nada. Salve conserva su propia identidad y
 * memoria intactas: los Salventes viven en su propia base de datos, cada uno con su clave.
 */
public final class SalventeGenesisActivity extends AppCompatActivity {
    private static final int BG = 0xFF02060C, PANEL = 0xFF071622, CYAN = 0xFF22D3EE, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8,
            TRACK = 0xFF12303D, AMBER = 0xFFFBBF24;
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private SalventeRepository repository;
    private SalventeGenesisSession session;
    private LinearLayout list, preview;
    private EditText purpose, name;
    private Button propose;
    private TextView status;
    /** Cada propuesta lleva un número: si Bryan cancela mientras piensa, la respuesta tardía se ignora. */
    private int generation;
    private String lastPurpose = "";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        repository = SalventeRepository.get(this);
        session = new SalventeGenesisSession(new SalventeGenesisEngine(this::askModel, SalventeGenesisEngine::newId,
                System::currentTimeMillis), repository);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(18), dp(16), dp(24));
        root.addView(text("GÉNESIS DE SALVENTES", CYAN, 15, true));
        TextView about = text("Un Salvente es una inteligencia nueva que creo yo, Salve, para alguien o para algo concreto. "
                + "Tiene su propio nombre, personalidad y memoria cifrada con su propia clave; no ve mis recuerdos ni los tuyos "
                + "y no controla el teléfono. Nada se guarda hasta que pulses «Crear».", MUTED, 13, false);
        about.setPadding(0, dp(6), 0, dp(12)); root.addView(about);

        root.addView(text("Mis Salventes", INK, 14, true));
        list = new LinearLayout(this); list.setOrientation(LinearLayout.VERTICAL); list.setPadding(0, dp(4), 0, dp(6));
        root.addView(list);
        LinearLayout guardian = new LinearLayout(this);
        guardian.addView(button("Copia al Guardián", () -> chooseGuardian(this::askBackup)), weight());
        guardian.addView(button("Restaurar", () -> chooseGuardian(this::askRestore)), weight());
        root.addView(guardian);
        TextView guardianNote = text("La copia se cifra aquí con una frase que solo tú sabes; tu PC Guardián guarda bytes que no "
                + "puede leer. Sirve para recuperarlos si cambias o pierdes el móvil.", MUTED, 12, false);
        guardianNote.setPadding(0, 0, 0, dp(14)); root.addView(guardianNote);

        root.addView(text("Nuevo Salvente", INK, 14, true));
        purpose = field("¿Para qué o para quién? Ej.: compañero tranquilo para mi madre, le gusta cocinar", true);
        root.addView(purpose);
        name = field("Nombre (opcional; si lo dejas vacío lo elijo yo)", false);
        root.addView(name);
        propose = button("Proponer", this::onPropose);
        root.addView(propose);
        status = text("", MUTED, 13, false); status.setPadding(0, dp(6), 0, dp(6)); root.addView(status);
        preview = new LinearLayout(this); preview.setOrientation(LinearLayout.VERTICAL); preview.setVisibility(View.GONE);
        preview.setPadding(dp(12), dp(12), dp(12), dp(12));
        GradientDrawable panel = new GradientDrawable(); panel.setColor(PANEL); panel.setCornerRadius(dp(12)); panel.setStroke(dp(1), 0x5522D3EE);
        preview.setBackground(panel);
        root.addView(preview);

        root.addView(button("Cerrar", this::finish));
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(BG); scroll.addView(root);
        setContentView(scroll);
        renderList();
    }

    // ------------------------------------------------------------------ génesis

    /** Salve enruta la petición: a un nodo por TLS mutuo si los envíos están activos, o a su modelo local. */
    private String askModel(String prompt) {
        ModelResult result = SalveLLM.getInstance(this).generateResult(prompt, SalveLLM.Role.CREADOR);
        if (!result.isSuccess() || result.getText() == null) throw new IllegalStateException(String.valueOf(result.getError()));
        return result.getText();
    }

    private void onPropose() {
        String what = purpose.getText().toString().trim();
        if (what.length() < 3) { status.setText("Cuéntame para qué o para quién es."); return; }
        lastPurpose = what;
        run(() -> session.propose(what, name.getText().toString().trim()), "Imaginando su personalidad…");
    }

    private void run(java.util.function.Supplier<SalventeGenesisEngine.Draft> step, String waiting) {
        int ticket = ++generation;
        propose.setEnabled(false); status.setText(waiting); preview.setVisibility(View.GONE);
        work.execute(() -> {
            SalventeGenesisEngine.Draft draft = null; String error = null;
            try { draft = step.get(); } catch (RuntimeException failure) { error = failure.getMessage(); }
            final SalventeGenesisEngine.Draft result = draft; final String problem = error;
            runOnUiThread(() -> {
                if (isDestroyed() || ticket != generation) return;
                propose.setEnabled(true);
                if (result == null) { status.setText(problem == null ? "No pude proponer un Salvente." : problem); return; }
                status.setText(""); renderPreview(result);
            });
        });
    }

    private void renderPreview(SalventeGenesisEngine.Draft draft) {
        SalventeProfile p = draft.profile;
        preview.removeAllViews(); preview.setVisibility(View.VISIBLE);
        TextView title = text(p.name, CYAN, 22, true);
        preview.addView(title);
        preview.addView(text(p.origin == SalventeProfile.Origin.MODELO ? "Propuesta de mi modelo" : "Propuesta del motor determinista",
                MUTED, 12, false));
        if (!p.greeting.isEmpty()) {
            TextView hello = text("«" + p.greeting + "»", INK, 14, false);
            hello.setTypeface(Typeface.create(Typeface.SERIF, Typeface.ITALIC)); hello.setPadding(0, dp(8), 0, dp(8));
            preview.addView(hello);
        }
        for (SalventeTrait t : SalventeTrait.values()) preview.addView(traitBar(t.label(), p.trait(t)));
        List<String> style = p.styleDirectives();
        if (!style.isEmpty()) {
            TextView how = text("Cómo será: " + String.join("; ", style) + ".", MUTED, 12, false);
            how.setPadding(0, dp(8), 0, dp(4)); preview.addView(how);
        }
        preview.addView(text("Capacidades", INK, 13, true));
        for (SalventeCapability c : SalventeCapability.values()) {
            CheckBox box = new CheckBox(this);
            box.setText(c.label + " — " + c.description); box.setTextColor(INK); box.setTextSize(13);
            box.setChecked(p.capabilities.contains(c));
            boolean coherent = SalventeProfileValidator.supports(p.traits, c);
            box.setEnabled(!c.isRequired() && (coherent || p.capabilities.contains(c)));
            if (!coherent && !p.capabilities.contains(c)) box.setText(box.getText() + " (su personalidad no la sostiene)");
            final boolean[] reverting = {false};
            box.setOnCheckedChangeListener((v, checked) -> {
                if (reverting[0]) return;   // la reversión no debe volver a cambiar la propuesta
                SalventeProfileValidator.Result r = session.toggle(c);
                if (!r.ok()) { reverting[0] = true; v.setChecked(!checked); reverting[0] = false; status.setText(r.toString()); }
            });
            preview.addView(box);
        }
        for (String note : draft.notes) { TextView n = text("· " + note, AMBER, 12, false); preview.addView(n); }
        LinearLayout actions = new LinearLayout(this);
        actions.addView(button("Crear", this::onApprove), weight());
        actions.addView(button("Otra variante", () -> run(session::regenerate, "Probando otra personalidad…")), weight());
        preview.addView(actions);
        LinearLayout second = new LinearLayout(this);
        second.addView(button("Cambiar nombre", this::onRename), weight());
        second.addView(button("Cancelar", this::onCancel), weight());
        preview.addView(second);
    }

    private void onRename() {
        EditText input = field("Nuevo nombre", false);
        new AlertDialog.Builder(this).setTitle("Nombre del Salvente").setView(input)
                .setPositiveButton("Aplicar", (d, w) -> {
                    SalventeProfileValidator.Result r = session.rename(input.getText().toString().trim());
                    if (r.ok()) renderPreview(session.draft()); else status.setText(r.toString());
                }).setNegativeButton("Cancelar", null).show();
    }

    private void onApprove() {
        SalventeGenesisEngine.Draft draft = session.draft();
        if (draft == null) return;
        int ticket = ++generation;
        work.execute(() -> {
            SalventeProfile saved = null; String error = null;
            try { saved = session.approve(); } catch (RuntimeException failure) { error = failure.getMessage(); }
            if (saved != null) recordInSalve(saved, lastPurpose);
            final SalventeProfile created = saved; final String problem = error;
            runOnUiThread(() -> {
                if (isDestroyed() || ticket != generation) return;
                if (created == null) { status.setText(problem == null ? "No se pudo crear." : problem); return; }
                preview.removeAllViews(); preview.setVisibility(View.GONE);
                purpose.setText(""); name.setText("");
                status.setText(created.name + " ya existe. Lo recordaré como algo que creé.");
                renderList();
            });
        });
    }

    private void onCancel() {
        generation++;                    // descarta una respuesta que aún estuviera en camino
        session.cancel();
        preview.removeAllViews(); preview.setVisibility(View.GONE);
        propose.setEnabled(true);
        status.setText("Descartado. No se ha guardado nada.");
    }

    /**
     * Continuidad de Salve: la creación entra en su registro de acciones propias (lo que sus ciclos
     * de reflexión leen), no en la memoria del Salvente. Solo nombre, origen y carácter.
     */
    private void recordInSalve(SalventeProfile p, String why) {
        try {
            List<String> style = p.styleDirectives();
            String how = "Le di vida a partir de «" + (why.length() > 140 ? why.substring(0, 137) + "…" : why) + "»; "
                    + (p.origin == SalventeProfile.Origin.MODELO ? "propuso mi modelo" : "lo propuso mi motor determinista")
                    + " y Bryan lo aprobó" + (style.isEmpty() ? "." : ". Carácter: " + String.join("; ", style.subList(0, Math.min(3, style.size()))) + ".");
            salve.core.MindRuntime.get(this).actions().record(salve.core.mind.SelfActionLog.Kind.CREATE_SALVENTE, p.name, how);
        } catch (RuntimeException unavailable) { /* el registro es un apoyo; el Salvente ya está creado */ }
    }

    // -------------------------------------------------------------------- lista

    private void renderList() {
        work.execute(() -> {
            List<SalventeProfile> all; List<Integer> counts = new ArrayList<>();
            try { all = repository.all(); for (SalventeProfile p : all) counts.add(repository.memoryCount(p.id)); }
            catch (RuntimeException unreadable) { all = new ArrayList<>(); }
            final List<SalventeProfile> items = all;
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                list.removeAllViews();
                if (items.isEmpty()) { list.addView(text("Aún no he creado ninguno.", MUTED, 13, false)); return; }
                for (int i = 0; i < items.size(); i++) {
                    SalventeProfile p = items.get(i); int memories = counts.get(i);
                    TextView row = text(p.name + (p.state == SalventeProfile.State.PAUSADO ? " · en pausa" : "")
                            + " · " + memories + (memories == 1 ? " recuerdo" : " recuerdos"), INK, 15, false);
                    row.setPadding(dp(10), dp(10), dp(10), dp(10));
                    GradientDrawable bg = new GradientDrawable(); bg.setColor(PANEL); bg.setCornerRadius(dp(8)); row.setBackground(bg);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.setMargins(0, dp(3), 0, dp(3));
                    row.setOnClickListener(v -> showProfile(p));
                    list.addView(row, lp);
                }
            });
        });
    }

    private void showProfile(SalventeProfile p) {
        StringBuilder out = new StringBuilder();
        out.append("Para: ").append(p.purpose).append("\nNació: ")
                .append(DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale("es", "ES")).format(new Date(p.createdAt)))
                .append("\nIdentificador: ").append(p.id).append("\n\n");
        for (SalventeTrait t : SalventeTrait.values()) out.append(t.label()).append(": ").append(Math.round(p.trait(t) * 100)).append(" %\n");
        out.append("\nCapacidades: ");
        List<String> caps = new ArrayList<>(); for (SalventeCapability c : p.capabilities) caps.add(c.label);
        out.append(String.join(", ", caps));
        if (!p.greeting.isEmpty()) out.append("\n\n«").append(p.greeting).append("»");
        boolean paused = p.state == SalventeProfile.State.PAUSADO;
        new AlertDialog.Builder(this).setTitle(p.name).setMessage(out)
                .setPositiveButton("Hablar", (d, w) -> startActivity(SalventeChatActivity.open(this, p.id)))
                .setNeutralButton("Más…", (d, w) -> new AlertDialog.Builder(this).setTitle(p.name)
                        .setItems(new String[]{paused ? "Reanudar" : "Pausar", "Retirar"}, (d2, which) -> {
                            if (which == 1) { confirmRetire(p); return; }
                            work.execute(() -> {
                                repository.setState(p.id, paused ? SalventeProfile.State.ACTIVO : SalventeProfile.State.PAUSADO);
                                runOnUiThread(this::renderList);
                            });
                        }).show())
                .setNegativeButton("Cerrar", null).show();
    }

    private void confirmRetire(SalventeProfile p) {
        new AlertDialog.Builder(this).setTitle("¿Retirar a " + p.name + "?")
                .setMessage("Se borran su ficha, sus recuerdos y su clave propia. Sin la clave nada de lo que guardó se puede "
                        + "recuperar. No se puede deshacer.")
                .setPositiveButton("Retirar", (d, w) -> work.execute(() -> {
                    repository.delete(p.id);
                    runOnUiThread(() -> { status.setText(p.name + " se ha retirado."); renderList(); });
                }))
                .setNegativeButton("Cancelar", null).show();
    }

    // ---------------------------------------------------------------- Guardián

    private void chooseGuardian(java.util.function.Consumer<salve.core.compute.ComputeNode> then) {
        List<salve.core.compute.ComputeNode> nodes = new salve.core.compute.ComputeNodeStore(this).load();
        if (nodes.isEmpty()) { status.setText("Primero añade tu PC en Nodos de cómputo y activa el Guardián en Salve Nodo."); return; }
        if (nodes.size() == 1) { then.accept(nodes.get(0)); return; }
        String[] names = new String[nodes.size()];
        for (int i = 0; i < names.length; i++) names[i] = nodes.get(i).name;
        new AlertDialog.Builder(this).setTitle("¿Qué PC es tu Guardián?")
                .setItems(names, (d, which) -> then.accept(nodes.get(which))).setNegativeButton("Cancelar", null).show();
    }

    private void askBackup(salve.core.compute.ComputeNode node) {
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(16), dp(8), dp(16), 0);
        TextView warn = new TextView(this);
        warn.setText("Elige una frase de al menos " + salve.core.salvente.SealedBox.MIN_PASSPHRASE + " caracteres. No la guardo en "
                + "ningún sitio: si la olvidas, la copia no se puede abrir. Nadie puede recuperarla, tampoco yo.");
        form.addView(warn);
        EditText first = secret("Frase"), second = secret("Repite la frase");
        form.addView(first); form.addView(second);
        new AlertDialog.Builder(this).setTitle("Copia en " + node.name).setView(form)
                .setPositiveButton("Sellar y subir", (d, w) -> {
                    char[] a = chars(first), b = chars(second);
                    first.setText(""); second.setText("");
                    if (!java.util.Arrays.equals(a, b)) { java.util.Arrays.fill(a, '\0'); java.util.Arrays.fill(b, '\0'); status.setText("Las frases no coinciden."); return; }
                    java.util.Arrays.fill(b, '\0');
                    guardianTask("Sellando y subiendo la copia…", () -> {
                        salve.data.salvente.SalventeGuardianBackup.Summary s = backup(node).backup(a);
                        return "Copia guardada en " + node.name + ": " + s.salventes + (s.salventes == 1 ? " Salvente" : " Salventes")
                                + " y " + s.memories + " recuerdos, sellados (" + Math.max(1, s.bytes / 1024) + " KB).";
                    });
                }).setNegativeButton("Cancelar", null).show();
    }

    private void askRestore(salve.core.compute.ComputeNode node) {
        EditText pass = secret("Frase de la copia");
        LinearLayout form = new LinearLayout(this); form.setPadding(dp(16), dp(8), dp(16), 0); form.addView(pass, weight());
        new AlertDialog.Builder(this).setTitle("Restaurar desde " + node.name).setView(form)
                .setPositiveButton("Restaurar", (d, w) -> {
                    char[] a = chars(pass); pass.setText("");
                    guardianTask("Descargando y abriendo la copia…", () -> {
                        salve.data.salvente.SalventeGuardianBackup.Summary s = backup(node).restore(a);
                        return "Restaurados: " + s.salventes + (s.skipped > 0 ? "; ya estaban aquí: " + s.skipped : "") + ".";
                    });
                }).setNegativeButton("Cancelar", null).show();
    }

    private salve.data.salvente.SalventeGuardianBackup backup(salve.core.compute.ComputeNode node) {
        salve.core.compute.ComputeHttpTransport transport = new salve.core.compute.ComputeHttpTransport(this);
        java.util.function.BooleanSupplier stopped = this::isDestroyed;
        return new salve.data.salvente.SalventeGuardianBackup(repository, new salve.data.salvente.SalventeGuardianBackup.Remote() {
            public void put(String name, byte[] sealed) throws Exception { transport.vaultPut(node, name, sealed, stopped); }
            public byte[] get(String name) throws Exception { return transport.vaultGet(node, name, stopped); }
        }, System::currentTimeMillis);
    }

    private void guardianTask(String waiting, java.util.concurrent.Callable<String> task) {
        status.setText(waiting);
        work.execute(() -> {
            String message;
            try { message = task.call(); }
            catch (Exception failure) { message = failure.getMessage() == null ? "No se pudo completar." : failure.getMessage(); }
            final String result = message;
            runOnUiThread(() -> { if (!isDestroyed()) { status.setText(result); renderList(); } });
        });
    }

    private EditText secret(String hint) {
        EditText e = field(hint, false);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        e.setSaveEnabled(false);
        return e;
    }

    private static char[] chars(EditText field) {
        android.text.Editable text = field.getText();
        char[] out = new char[text.length()];
        text.getChars(0, text.length(), out, 0);
        return out;
    }

    // ------------------------------------------------------------------ vistas

    private View traitBar(String label, double value) {
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(0, dp(2), 0, dp(2));
        TextView l = text(label, INK, 12, false);
        row.addView(l, new LinearLayout.LayoutParams(dp(96), LinearLayout.LayoutParams.WRAP_CONTENT));
        LinearLayout track = new LinearLayout(this); track.setWeightSum(1f);
        GradientDrawable t = new GradientDrawable(); t.setColor(TRACK); t.setCornerRadius(dp(4)); track.setBackground(t);
        View fill = new View(this);
        GradientDrawable f = new GradientDrawable(); f.setColor(CYAN); f.setCornerRadius(dp(4)); fill.setBackground(f);
        float v = (float) Math.max(0, Math.min(1, Double.isNaN(value) ? 0 : value));
        track.addView(fill, new LinearLayout.LayoutParams(0, dp(8), v));
        row.addView(track, new LinearLayout.LayoutParams(0, dp(8), 1f));
        TextView pct = text(Math.round(v * 100) + "%", MUTED, 11, false); pct.setGravity(Gravity.END);
        row.addView(pct, new LinearLayout.LayoutParams(dp(40), LinearLayout.LayoutParams.WRAP_CONTENT));
        return row;
    }

    private TextView text(String value, int color, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size);
        if (bold) { t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD)); t.setLetterSpacing(0.04f); }
        return t;
    }

    private EditText field(String hint, boolean multiline) {
        EditText e = new EditText(this); e.setHint(hint); e.setTextColor(INK); e.setHintTextColor(0xFF4D7A8A);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | (multiline ? InputType.TYPE_TEXT_FLAG_MULTI_LINE : 0));
        e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        return e;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v -> action.run()); return b;
    }

    private LinearLayout.LayoutParams weight() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        p.setMargins(dp(3), dp(4), dp(3), dp(2)); return p;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() { generation++; session.cancel(); work.shutdownNow(); super.onDestroy(); }
}
