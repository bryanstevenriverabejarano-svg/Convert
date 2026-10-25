package salve.presentation.ui;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import salve.core.mind.MindFileStore;
import salve.core.reading.StoryShelf;

/**
 * Su repertorio de cuentos: los que ha inventado ella y los que ha leído. Al tocar uno se cierra y
 * Salve lo cuenta en el chat. La elección pasa por {@link #PICKED} (dentro de la app), no por un
 * Intent: ninguna otra app puede pedirle que cuente algo.
 */
public final class StoryRepertoireActivity extends Activity {
    /** El título elegido; MainActivity lo recoge al volver. */
    public static final AtomicReference<String> PICKED = new AtomicReference<>();

    private static final int BG = 0xFF0B1420, CARD = 0xFF14233A, TEXT = 0xFFDCECFF, MUTED = 0xFF8FA6C1,
            INVENTED = 0xFFFFC46B, READ = 0xFF5AE0C8;
    private final Handler main = new Handler(Looper.getMainLooper());

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Repertorio de cuentos");
        TextView wait = text("Abriendo mi repertorio…", 16, MUTED, false);
        wait.setGravity(Gravity.CENTER); wait.setBackgroundColor(BG);
        setContentView(wait);
        new Thread(() -> {
            List<StoryShelf.Story> all;
            try {
                all = new StoryShelf(new MindFileStore(new File(new File(getNoBackupFilesDir(), "mind"), StoryShelf.FILE)),
                        System::currentTimeMillis).all();
            } catch (RuntimeException unreadable) { all = null; }
            List<StoryShelf.Story> stories = all;
            main.post(() -> { if (!isFinishing() && !isDestroyed()) show(stories); });
        }, "Salve-Repertorio").start();
    }

    private void show(List<StoryShelf.Story> all) {
        LinearLayout list = new LinearLayout(this);
        list.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(16);
        list.setPadding(pad, dp(24), pad, dp(32));
        list.addView(text("Repertorio de cuentos", 24, TEXT, true));
        if (all == null) {
            list.addView(gap(text("No he podido abrir mi repertorio ahora. Inténtalo en un momento.", 15, MUTED, false)));
        } else if (all.isEmpty()) {
            list.addView(gap(text("Todavía está vacío. Los cuentos que te cuente se irán guardando aquí solos, "
                    + "y los que me des a leer desde «Documentos» también.", 15, MUTED, false)));
        } else {
            List<StoryShelf.Story> invented = new ArrayList<>(), read = new ArrayList<>();
            for (int i = all.size() - 1; i >= 0; i--) (all.get(i).told ? invented : read).add(all.get(i));
            list.addView(gap(text("Toca uno y te lo cuento.", 15, MUTED, false)));
            section(list, "Inventados por mí", invented, INVENTED);
            section(list, "Que he leído", read, READ);
        }
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(list);
        setContentView(scroll);
    }

    private void section(LinearLayout list, String name, List<StoryShelf.Story> stories, int accent) {
        if (stories.isEmpty()) return;
        TextView header = text(name + " · " + stories.size(), 13, accent, true);
        header.setAllCaps(true);
        header.setLetterSpacing(0.08f);
        header.setPadding(0, dp(24), 0, dp(8));
        list.addView(header);
        SimpleDateFormat date = new SimpleDateFormat("d MMM yyyy", new Locale("es", "ES"));
        for (StoryShelf.Story s : stories) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(dp(16), dp(14), dp(16), dp(14));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(CARD); bg.setCornerRadius(dp(14)); bg.setStroke(dp(1), (accent & 0x00FFFFFF) | 0x55000000);
            card.setBackground(bg);
            card.addView(text(s.title == null ? "Sin título" : s.title, 17, TEXT, true));
            StringBuilder meta = new StringBuilder(date.format(new Date(s.at)));
            if (s.versions() > 1) meta.append(" · ").append(s.versions()).append(" versiones");
            if (s.characters != null && !s.characters.isEmpty()) {
                List<String> names = new ArrayList<>();
                for (String c : s.characters) { names.add(c.split("[:,(—-]", 2)[0].trim()); if (names.size() == 3) break; }
                meta.append(" · ").append(String.join(", ", names));
            }
            TextView m = text(meta.toString(), 12, MUTED, false);
            m.setPadding(0, dp(2), 0, dp(6));
            card.addView(m);
            if (s.summary != null && !s.summary.isEmpty()) {
                TextView sum = text(s.summary, 14, TEXT & 0xCCFFFFFF, false);
                sum.setMaxLines(3);
                sum.setEllipsize(android.text.TextUtils.TruncateAt.END);
                card.addView(sum);
            }
            card.setClickable(true);
            card.setForeground(getDrawable(android.R.drawable.list_selector_background));
            String title = s.title;
            card.setOnClickListener(v -> { PICKED.set(title); finish(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = dp(10);
            list.addView(card, lp);
        }
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private View gap(TextView t) { t.setPadding(0, dp(8), 0, 0); return t; }

    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }
}
