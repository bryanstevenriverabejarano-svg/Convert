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
    static final int BG = Color.BLACK, CARD = Color.rgb(8, 14, 28), CARD_2 = Color.rgb(14, 24, 46),
            BLUE = Color.rgb(30, 144, 255), MUTED = Color.rgb(150, 168, 200), LIGHT = Color.rgb(220, 230, 248),
            STROKE = Color.rgb(28, 96, 190), NEON = Color.rgb(41, 160, 255), NIGHT_TOP = Color.rgb(4, 10, 26),
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

    /** Tarjeta neón: fondo casi negro con un halo azul y borde brillante. */
    static android.graphics.drawable.Drawable glass(Activity a) { return neon(a, false, 16); }

    /**
     * Borde neón: una capa exterior translúcida hace de halo y la interior es el borde nítido.
     * {@code strong} = seleccionado o destacado (más brillo y relleno azulado).
     */
    static android.graphics.drawable.Drawable neon(Activity a, boolean strong, int radiusDp) {
        float r = dp(a, radiusDp);
        GradientDrawable halo = new GradientDrawable();
        halo.setCornerRadius(r + dp(a, 2));
        halo.setColor(Color.TRANSPARENT);
        halo.setStroke(dp(a, strong ? 5 : 3), Color.argb(strong ? 90 : 45, 30, 144, 255));
        GradientDrawable body = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                strong ? new int[]{Color.rgb(10, 40, 92), Color.rgb(4, 16, 40)} : new int[]{Color.rgb(9, 17, 34), Color.rgb(2, 5, 12)});
        body.setCornerRadius(r);
        body.setStroke(Math.max(2, dp(a, strong ? 2 : 1)), strong ? NEON : STROKE);
        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[]{halo, body});
        int inset = dp(a, 2);
        layers.setLayerInset(1, inset, inset, inset, inset);
        return layers;
    }

    /** Botón principal: degradado azul con borde brillante. */
    static android.graphics.drawable.Drawable neonButton(Activity a) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{Color.rgb(18, 98, 230), Color.rgb(36, 150, 255), Color.rgb(18, 98, 230)});
        g.setCornerRadius(dp(a, 14));
        g.setStroke(Math.max(2, dp(a, 1)), Color.rgb(140, 205, 255));
        return g;
    }

    /** Fondo de pantalla: noche azul arriba que funde a negro. */
    static GradientDrawable night() {
        return new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{NIGHT_TOP, Color.BLACK, Color.BLACK});
    }

    /** Icono (emoji) dentro de un círculo. */
    static TextView icon(Activity a, String emoji, int sizeDp) {
        TextView t = new TextView(a); t.setText(emoji); t.setTextSize(sizeDp * 0.45f); t.setGravity(Gravity.CENTER);
        GradientDrawable c = new GradientDrawable(); c.setShape(GradientDrawable.OVAL); c.setColor(CARD); c.setStroke(Math.max(2, dp(a, 2)), NEON);
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

    /**
     * Escala del texto según el ancho de la pantalla (411 dp = 1). Los tamaños van en dp para que
     * la letra grande del sistema no rompa el panel; en móviles estrechos todo se reduce un poco.
     */
    static float scale(Activity a) {
        int w = a.getResources().getConfiguration().screenWidthDp;
        return Math.max(0.78f, Math.min(1.15f, w / 411f));
    }

    static boolean narrow(Activity a) { return a.getResources().getConfiguration().screenWidthDp < 360; }

    static void size(Activity a, TextView t, float dpSize) { t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_DIP, dpSize * scale(a)); }

    static TextView text(Activity a, LinearLayout parent, String value, int size, int color, boolean bold) {
        return text(a, parent, value, (float) size, color, bold);
    }

    static TextView text(Activity a, LinearLayout parent, String value, float size, int color, boolean bold) {
        TextView t = new TextView(a); t.setText(value); size(a, t, size); t.setTextColor(color);
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
        t.setTypeface(Typeface.create(Typeface.DEFAULT_BOLD, Typeface.BOLD_ITALIC));
        t.setPadding(0, dp(a, 18), 0, dp(a, 4));
        return t;
    }

    static Button button(Activity a, String label, int color) {
        Button b = new Button(a); b.setText(label); b.setAllCaps(false); b.setTextColor(Color.WHITE); size(a, b, 15);
        b.setBackground(color == BLUE ? neonButton(a) : neon(a, false, 12));
        b.setMinHeight(dp(a, 48));
        return b;
    }

    static TextView chip(Activity a, String label, boolean selected) {
        TextView t = new TextView(a); t.setText(label); size(a, t, 14);
        t.setTextColor(selected ? Color.WHITE : LIGHT);
        t.setTypeface(selected ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        int p = dp(a, 14); t.setPadding(p, dp(a, 8), p, dp(a, 8));
        t.setBackground(rounded(a, selected ? BLUE : CARD_2, 18));
        return t;
    }

    static TextView link(Activity a, String label, View.OnClickListener click) {
        TextView t = new TextView(a); t.setText(label); t.setTextColor(BLUE); size(a, t, 14);
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
