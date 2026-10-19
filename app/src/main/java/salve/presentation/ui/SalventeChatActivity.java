package salve.presentation.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.ModelResult;
import salve.core.SalveLLM;
import salve.core.salvente.SalventeConversation;
import salve.core.salvente.SalventeMind;
import salve.core.salvente.SalventeProfile;
import salve.data.salvente.SalventeRepository;

/**
 * Conversación con un Salvente. Piensa con el canal neutro (rol SALVENTE: sin la identidad de
 * Salve), en el nodo de inferencia por TLS mutuo si los envíos están activos o en el móvil, y
 * recuerda solo en su propio espacio cifrado. Bryan puede ver lo que recuerda y borrarlo.
 */
public final class SalventeChatActivity extends AppCompatActivity {
    private static final String EXTRA_ID = "salvente_id";
    private static final int BG = 0xFF02060C, PANEL = 0xFF071622, MINE = 0xFF0E3A47, CYAN = 0xFF22D3EE, INK = 0xFFE6FBFF, MUTED = 0xFF7DA9B8;
    private final ExecutorService work = Executors.newSingleThreadExecutor();
    private SalventeRepository repository;
    private SalventeProfile profile;
    private SalventeConversation conversation;
    private LinearLayout messages;
    private ScrollView scroll;
    private EditText input;
    private Button send;
    private TextView thinking;
    private int ticket;

    public static Intent open(Context context, String salventeId) {
        return new Intent(context, SalventeChatActivity.class).putExtra(EXTRA_ID, salventeId);
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        repository = SalventeRepository.get(this);
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(12), dp(14), dp(12), dp(10));
        LinearLayout header = new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = text("…", CYAN, 18, true);
        header.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        Button memoriesButton = button("Recuerdos", this::showMemories); header.addView(memoriesButton);
        Button close = button("Cerrar", this::finish); header.addView(close);
        root.addView(header);
        TextView style = text("", MUTED, 12, false); style.setPadding(0, dp(2), 0, dp(8)); root.addView(style);
        scroll = new ScrollView(this);
        messages = new LinearLayout(this); messages.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        thinking = text("", MUTED, 12, false); root.addView(thinking);
        LinearLayout bar = new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        input = new EditText(this); input.setTextColor(INK); input.setHintTextColor(0xFF4D7A8A);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setImeOptions(EditorInfo.IME_ACTION_SEND); input.setMaxLines(4);
        bar.addView(input, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        send = button("Enviar", this::onSend); bar.addView(send);
        root.addView(bar);
        setContentView(root);
        send.setEnabled(false);

        String id = getIntent().getStringExtra(EXTRA_ID);
        work.execute(() -> {
            SalventeProfile loaded = id == null ? null : repository.get(id);
            int remembered = loaded == null ? 0 : repository.memoryCount(loaded.id);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                if (loaded == null) { thinking.setText("Ese Salvente ya no existe."); return; }
                profile = loaded;
                conversation = new SalventeConversation(loaded, new SalventeConversation.Memories() {
                    public List<SalventeMind.Memory> recent(int max) { return repository.recentForMind(loaded.id, max); }
                    public void remember(String text) { repository.remember(loaded.id, text); }
                }, this::think, "Bryan");
                title.setText(loaded.name);
                List<String> directives = loaded.styleDirectives();
                style.setText(directives.isEmpty() ? loaded.purpose : String.join(" · ", directives.subList(0, Math.min(3, directives.size()))));
                input.setHint("Escribe a " + loaded.name);
                if (loaded.state == SalventeProfile.State.PAUSADO) { thinking.setText(loaded.name + " está en pausa. Reanúdalo en la génesis."); return; }
                send.setEnabled(true);
                if (remembered == 0 && !loaded.greeting.isEmpty()) bubble(loaded.greeting, false);
                else thinking.setText("Recuerda " + remembered + (remembered == 1 ? " conversación." : " conversaciones."));
            });
        });
    }

    /** Canal neutro: el prompt ya trae la identidad del Salvente; SalveLLM no añade la de Salve. */
    private String think(String prompt) {
        ModelResult result = SalveLLM.getInstance(this).generateResult(prompt, SalveLLM.Role.SALVENTE);
        if (!result.isSuccess() || result.getText() == null)
            throw new IllegalStateException("Ahora no tengo un modelo con el que pensar: " + result.getError());
        return result.getText();
    }

    private void onSend() {
        String said = input.getText().toString().trim();
        if (said.isEmpty() || conversation == null) return;
        input.setText(""); bubble(said, true);
        send.setEnabled(false); thinking.setText(profile.name + " está pensando…");
        int mine = ++ticket;
        work.execute(() -> {
            String reply = null, problem = null; boolean corrected = false;
            try { SalventeConversation.Reply r = conversation.say(said); reply = r.text; corrected = r.corrected; }
            catch (Exception failure) { problem = failure.getMessage(); }
            final String text = reply, error = problem; final boolean fixed = corrected;
            runOnUiThread(() -> {
                if (isDestroyed() || mine != ticket) return;
                send.setEnabled(true);
                if (text == null) { thinking.setText(error == null ? "No pudo responder." : error); return; }
                thinking.setText(fixed ? "(Corregí una confusión de identidad del modelo.)" : "");
                bubble(text, false);
            });
        });
    }

    private void showMemories() {
        if (profile == null) return;
        work.execute(() -> {
            List<SalventeRepository.Memory> list = repository.memories(profile.id, 30);
            int total = repository.memoryCount(profile.id);
            runOnUiThread(() -> {
                if (isDestroyed()) return;
                StringBuilder out = new StringBuilder();
                DateFormat format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("es", "ES"));
                for (SalventeRepository.Memory m : list) out.append(format.format(new Date(m.at))).append("\n").append(m.text).append("\n\n");
                if (out.length() == 0) out.append("Todavía no recuerda nada.");
                new AlertDialog.Builder(this).setTitle("Lo que recuerda " + profile.name + " (" + total + ")")
                        .setMessage(out.toString().trim())
                        .setPositiveButton("Cerrar", null)
                        .setNeutralButton("Olvidar todo", (d, w) -> confirmForget()).show();
            });
        });
    }

    private void confirmForget() {
        new AlertDialog.Builder(this).setTitle("¿Olvidar todo?")
                .setMessage(profile.name + " olvidará todas sus conversaciones. Conserva su nombre, su carácter y su clave.")
                .setPositiveButton("Olvidar", (d, w) -> work.execute(() -> {
                    repository.forgetMemories(profile.id);
                    runOnUiThread(() -> { messages.removeAllViews(); thinking.setText(profile.name + " ha olvidado sus conversaciones."); });
                }))
                .setNegativeButton("Cancelar", null).show();
    }

    private void bubble(String value, boolean mine) {
        TextView t = text(value, INK, 15, false);
        t.setTextIsSelectable(true);
        t.setPadding(dp(12), dp(8), dp(12), dp(8));
        GradientDrawable bg = new GradientDrawable(); bg.setColor(mine ? MINE : PANEL); bg.setCornerRadius(dp(14));
        if (!mine) bg.setStroke(dp(1), 0x4422D3EE);
        t.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = mine ? Gravity.END : Gravity.START;
        lp.setMargins(mine ? dp(48) : 0, dp(4), mine ? 0 : dp(48), dp(4));
        messages.addView(t, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private TextView text(String value, int color, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(color); t.setTextSize(size);
        if (bold) t.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        return t;
    }

    private Button button(String label, Runnable action) {
        Button b = new Button(this); b.setText(label); b.setAllCaps(false); b.setOnClickListener(v -> action.run()); return b;
    }

    private int dp(int value) { return (int) (value * getResources().getDisplayMetrics().density); }

    @Override protected void onDestroy() { ticket++; work.shutdownNow(); super.onDestroy(); }
}
