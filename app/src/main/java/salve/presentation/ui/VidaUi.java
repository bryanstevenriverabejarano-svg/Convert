package salve.presentation.ui;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.Locale;

/** Piezas de interfaz compartidas por las pantallas de Vida (tema oscuro, tarjetas, botones). */
final class VidaUi {
    static final int BG = Color.rgb(6, 10, 22), CARD = Color.rgb(17, 27, 49), CARD_2 = Color.rgb(26, 40, 70),
            BLUE = Color.rgb(33, 150, 243), MUTED = Color.rgb(143, 163, 199), LIGHT = Color.rgb(214, 224, 242),
            STROKE = Color.rgb(42, 63, 107), NIGHT_TOP = Color.rgb(12, 24, 52),
            GREEN = Color.rgb(67, 160, 71), AMBER = Color.rgb(255, 179, 0), RED = Color.rgb(229, 57, 53),
            DONE_ROW = Color.rgb(27, 94, 32);

    private VidaUi() {}

    static int dp(Activity a, int v) { return (int) (v * a.getResources().getDisplayMetrics().density); }

    static LinearLayout column(Activity a) {
        LinearLayout root = new LinearLayout(a); root.setOrientation(LinearLayout.VERTICAL);
        return root;
    }

    static LinearLayout page(Activity a) {
        LinearLayout root = column(a);
        int p = dp(a, 16); root.setPadding(p, dp(a, 12), p, dp(a, 96));
        return root;
    }

    static LinearLayout card(Activity a, LinearLayout parent) {
        LinearLayout card = column(a);
        int p = dp(a, 14); card.setPadding(p, p, p, p);
        card.setBackground(glass(a));
        parent.addView(card, wide(a, 10));
        return card;
    }

    /** Fondo de tarjeta: degradado azul noche con borde fino luminoso. */
    static GradientDrawable glass(Activity a) {
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.rgb(22, 35, 63), Color.rgb(11, 18, 34)});
        bg.setCornerRadius(dp(a, 18));
        bg.setStroke(Math.max(1, dp(a, 1)), STROKE);
        return bg;
    }

    /** Fondo de pantalla: noche azul arriba que funde a negro. */
    static GradientDrawable night() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{NIGHT_TOP, BG, Color.BLACK});
    }

    /** Icono (emoji) dentro de un círculo. */
    static TextView icon(Activity a, String emoji, int sizeDp) {
        TextView t = new TextView(a); t.setText(emoji); t.setTextSize(sizeDp * 0.45f); t.setGravity(Gravity.CENTER);
        GradientDrawable c = new GradientDrawable(); c.setShape(GradientDrawable.OVAL); c.setColor(CARD_2); c.setStroke(Math.max(1, dp(a, 1)), STROKE);
        t.setBackground(c);
        t.setLayoutParams(new LinearLayout.LayoutParams(dp(a, sizeDp), dp(a, sizeDp)));
        return t;
    }

    static GradientDrawable rounded(Activity a, int color, int radiusDp) {
        GradientDrawable bg = new GradientDrawable(); bg.setColor(color); bg.setCornerRadius(dp(a, radiusDp));
        return bg;
    }

    static LinearLayout row(Activity a) {
        LinearLayout row = new LinearLayout(a); row.setGravity(Gravity.CENTER_VERTICAL);
        return row;
    }

    static TextView text(Activity a, LinearLayout parent, String value, int size, int color, boolean bold) {
        TextView t = new TextView(a); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(a, 3), 0, dp(a, 3));
        if (parent != null) parent.addView(t);
        return t;
    }

    static TextView ellipsized(Activity a, LinearLayout parent, String value, int lines) {
        TextView t = text(a, parent, value, 15, MUTED, false);
        t.setMaxLines(lines); t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    static TextView section(Activity a, LinearLayout parent, String value) {
        TextView t = text(a, parent, value, 21, Color.WHITE, true);
        t.setPadding(0, dp(a, 18), 0, dp(a, 4));
        return t;
    }

    static Button button(Activity a, String label, int color) {
        Button b = new Button(a); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(16);
        b.setBackground(rounded(a, color, 10));
        b.setMinHeight(dp(a, 48));
        return b;
    }

    static TextView chip(Activity a, String label, boolean selected) {
        TextView t = new TextView(a); t.setText(label); t.setTextSize(15);
        t.setTextColor(selected ? Color.WHITE : LIGHT);
        t.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        int p = dp(a, 14); t.setPadding(p, dp(a, 8), p, dp(a, 8));
        t.setBackground(rounded(a, selected ? BLUE : CARD_2, 18));
        return t;
    }

    static TextView link(Activity a, String label, View.OnClickListener click) {
        TextView t = new TextView(a); t.setText(label); t.setTextColor(BLUE); t.setTextSize(15);
        t.setPadding(dp(a, 8), dp(a, 8), dp(a, 8), dp(a, 8)); t.setOnClickListener(click);
        return t;
    }

    static LinearLayout.LayoutParams wide(Activity a, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(a, topDp);
        return lp;
    }

    static LinearLayout.LayoutParams weight(Activity a, float w, int marginDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, w);
        lp.setMargins(dp(a, marginDp), 0, dp(a, marginDp), 0);
        return lp;
    }

    interface Changed { void on(String value); }
    static TextWatcher after(Changed listener) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { listener.on(e.toString()); }
        };
    }

    static String hours(int minutes) { return minutes / 60 + " h " + String.format(Locale.ROOT, "%02d", minutes % 60) + " min"; }
}
