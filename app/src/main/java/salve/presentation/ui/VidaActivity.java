package salve.presentation.ui;

import static salve.presentation.ui.VidaUi.*;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.UnaryOperator;
import salve.core.vida.GymTracker;
import salve.core.vida.VidaLog;
import salve.core.vida.VidaCoach;
import salve.core.vida.VidaPlanner;
import salve.core.vida.VidaReminders;
import salve.core.vida.VidaRuntime;
import salve.core.vida.VidaService;

/**
 * Sección Vida de Salve: Hoy, Gym, Comidas, Sueño, Horario y Objetivos. Todo se guarda en el
 * mismo registro local de Vida (con copia en pCloud si está conectada) y el chat lo ve igual.
 */
public final class VidaActivity extends Activity {
    private static final String[] TABS = {"Hoy", "Horario", "Proyectos", "Hábitos", "Gym", "Comidas", "Sueño", "Evolución"};

    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler ui = new Handler(Looper.getMainLooper());
    private VidaService vida;
    private volatile VidaLog log = new VidaLog();
    private String tab = "Hoy";
    private int mealDay = LocalDate.now().getDayOfWeek().getValue();
    private LinearLayout tabs, content;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Vida");
        vida = VidaRuntime.get(this);
        if (state != null) tab = state.getString("tab", tab);
        LinearLayout root = column(this); root.setBackground(night());
        root.addView(header());
        // Las 8 pestañas caben en una fila en cualquier ancho (repartidas a partes iguales).
        tabs = row(this); tabs.setPadding(dp(this, 10), dp(this, 4), dp(this, 10), dp(this, 4));
        root.addView(tabs, new LinearLayout.LayoutParams(-1, -2));
        ScrollView scroll = new ScrollView(this);
        content = page(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(bottomNav(), new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
        paintTabs();
        text(this, content, "Cargando…", 16, MUTED, false);
        change(l -> VidaCoach.seedProjects(VidaPlanner.syncSleepHabit(VidaPlanner.syncGymHabit(VidaPlanner.seedDefaults(l))), LocalDate.now()));
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 71);
    }

    /** Cabecera: corazón de Vida, título y el retrato de Salve (recurso existente salve_chat_avatar). */
    private View header() {
        LinearLayout head = row(this);
        head.setPadding(dp(this, 14), dp(this, 24), dp(this, 14), dp(this, 4));
        android.widget.FrameLayout logo = new android.widget.FrameLayout(this);
        logo.setBackground(neon(this, true, 14));
        logo.addView(icon(VidaIcon.Type.HEART_PULSE, NEON, 26), new android.widget.FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        int logoSize = Math.round(dp(this, 48) * scale(this));
        head.addView(logo, new LinearLayout.LayoutParams(logoSize, logoSize));
        LinearLayout titles = column(this); titles.setPadding(dp(this, 12), 0, dp(this, 6), 0);
        LinearLayout tr = row(this);
        TextView big = text(this, null, "Vida", 28, Color.WHITE, true);
        big.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD_ITALIC));
        big.setShadowLayer(dp(this, 8), 0, 0, NEON); big.setPadding(0, 0, 0, 0);
        tr.addView(big); tr.addView(icon(VidaIcon.Type.SPARKLE, CYAN, 16));
        titles.addView(tr);
        TextView sub = text(this, titles, "Tu organización, bienestar y progreso.", 12, LIGHT, false); sub.setPadding(0, 0, 0, 0);
        android.text.SpannableString with = new android.text.SpannableString("Con el acompañamiento de Salve.");
        with.setSpan(new android.text.style.ForegroundColorSpan(CYAN), 25, 30, 0);
        with.setSpan(new android.text.style.StyleSpan(android.graphics.Typeface.BOLD), 25, 30, 0);
        TextView w = text(this, titles, "", 12, LIGHT, false); w.setText(with); w.setPadding(0, 0, 0, 0);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        int face = Math.round(dp(this, 58) * scale(this));
        View avatar = avatar(face);
        avatar.setContentDescription("Salve");
        avatar.setOnClickListener(v -> openMain(salve.core.voice.AssistantLaunch.ACTION_WAKE, null));
        head.addView(avatar, new LinearLayout.LayoutParams(face, face));
        return head;
    }

    /** Retrato circular de Salve con aro de neón (se recorta en tiempo de ejecución, sin archivos nuevos). */
    private View avatar(int sizePx) {
        android.widget.ImageView img = new android.widget.ImageView(this);
        img.setImageResource(com.salve.app.R.drawable.salve_chat_avatar);
        img.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        img.setClipToOutline(true);
        img.setOutlineProvider(new android.view.ViewOutlineProvider() {
            @Override public void getOutline(View view, android.graphics.Outline outline) { outline.setOval(0, 0, view.getWidth(), view.getHeight()); }
        });
        android.widget.FrameLayout ring = new android.widget.FrameLayout(this);
        android.graphics.drawable.GradientDrawable halo = new android.graphics.drawable.GradientDrawable();
        halo.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        halo.setColor(CARD); halo.setStroke(Math.max(2, dp(this, 2)), CYAN);
        ring.setBackground(halo);
        int pad = Math.max(2, dp(this, 3)); ring.setPadding(pad, pad, pad, pad);
        ring.addView(img, new android.widget.FrameLayout.LayoutParams(-1, -1));
        ring.setElevation(dp(this, 4));
        return ring;
    }

    /**
     * Navegación inferior. La app no tenía barra inferior: se conecta a lo que ya existe —
     * Inicio (MainActivity), Salve (modo voz por AssistantLaunch.ACTION_WAKE), Memoria (panel de
     * memoria de MainActivity) y Evolución (pestaña de Vida).
     */
    private View bottomNav() {
        LinearLayout bar = row(this);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(this, 8), dp(this, 6), dp(this, 8), dp(this, 10));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(CARD); bg.setStroke(Math.max(1, dp(this, 1)), STROKE);
        bar.setBackground(bg);
        bar.addView(navItem(VidaIcon.Type.HOME, "Inicio", false, v -> openMain(null, null)), new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(navItem(VidaIcon.Type.HEART_PULSE, "Vida", true, v -> go("Hoy")), new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout mid = column(this); mid.setGravity(Gravity.CENTER_HORIZONTAL);
        int face = Math.round(dp(this, 46) * scale(this));
        View talk = avatar(face);
        talk.setContentDescription("Hablar con Salve");
        mid.addView(talk, new LinearLayout.LayoutParams(face, face));
        TextView sl = text(this, mid, "Salve", 10, CYAN, true); sl.setPadding(0, dp(this, 2), 0, 0);
        mid.setOnClickListener(v -> openMain(salve.core.voice.AssistantLaunch.ACTION_WAKE, null));
        talk.setOnClickListener(v -> openMain(salve.core.voice.AssistantLaunch.ACTION_WAKE, null));
        bar.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(navItem(VidaIcon.Type.LAYERS, "Memoria", false, v -> openMain(null, MainActivity.EXTRA_OPEN_MEMORY)), new LinearLayout.LayoutParams(0, -2, 1));
        bar.addView(navItem(VidaIcon.Type.BARS, "Evolución", false, v -> go("Evolución")), new LinearLayout.LayoutParams(0, -2, 1));
        return bar;
    }

    private View navItem(VidaIcon.Type type, String label, boolean selected, View.OnClickListener click) {
        LinearLayout c = column(this); c.setGravity(Gravity.CENTER_HORIZONTAL);
        c.setPadding(0, dp(this, 4), 0, dp(this, 2));
        c.addView(icon(type, selected ? CYAN : MUTED, 22));
        TextView t = new TextView(this); t.setText(label); t.setGravity(Gravity.CENTER); t.setMaxLines(1);
        t.setTextColor(selected ? CYAN : MUTED);
        t.setAutoSizeTextTypeUniformWithConfiguration(8, 11, 1, android.util.TypedValue.COMPLEX_UNIT_DIP);
        c.addView(t, new LinearLayout.LayoutParams(-1, dp(this, 16)));
        c.setOnClickListener(click);
        return c;
    }

    /** Vuelve a la pantalla principal de Salve (la reutiliza si ya está abierta). */
    private void openMain(String action, String flagExtra) {
        Intent i = new Intent(this, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (action != null) i.setAction(action);
        if (flagExtra != null) i.putExtra(flagExtra, true);
        startActivity(i);
    }

    @Override protected void onResume() { super.onResume(); if (vida != null) change(VidaPlanner::syncGymHabit); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("tab", tab); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }

    /** Aplica un cambio fuera del hilo de la pantalla y repinta la pestaña. */
    private void change(UnaryOperator<VidaLog> c) {
        io.execute(() -> {
            String error = null;
            try { log = vida.update(c); VidaReminders.schedule(this); }
            catch (IllegalArgumentException invalid) { error = invalid.getMessage(); }
            catch (Exception failed) { error = "No pude guardar el cambio; tus datos anteriores se conservan."; }
            final String e = error;
            ui.post(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (e != null) Toast.makeText(this, e, Toast.LENGTH_LONG).show();
                render();
            });
        });
    }

    private void paintTabs() {
        tabs.removeAllViews();
        VidaIcon.Type[] icons = {VidaIcon.Type.HOME, VidaIcon.Type.CALENDAR, VidaIcon.Type.LAYERS, VidaIcon.Type.CHECK_CIRCLE,
                VidaIcon.Type.DUMBBELL, VidaIcon.Type.FORK_KNIFE, VidaIcon.Type.MOON, VidaIcon.Type.BARS};
        for (int i = 0; i < TABS.length; i++) {
            String t = TABS[i];
            boolean sel = t.equals(tab);
            LinearLayout c = column(this);
            c.setGravity(Gravity.CENTER_HORIZONTAL);
            c.setPadding(0, dp(this, 7), 0, dp(this, 6));
            c.setBackground(neon(this, sel, 12));
            View ic = icon(icons[i], sel ? Color.WHITE : LIGHT, 20);
            c.addView(ic);
            TextView lb = new TextView(this); lb.setText(t); lb.setTextColor(sel ? Color.WHITE : LIGHT);
            lb.setGravity(Gravity.CENTER); lb.setMaxLines(1);
            if (sel) lb.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            // El texto se encoge solo si la pestaña es estrecha
            lb.setAutoSizeTextTypeUniformWithConfiguration(7, 11, 1, android.util.TypedValue.COMPLEX_UNIT_DIP);
            c.addView(lb, new LinearLayout.LayoutParams(-1, dp(this, 16)));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            lp.setMargins(dp(this, 2), 0, dp(this, 2), 0);
            c.setOnClickListener(v -> {
                if (t.equals("Gym")) { startActivity(new Intent(this, GymActivity.class)); return; }
                tab = t; paintTabs(); render();
            });
            tabs.addView(c, lp);
        }
    }

    private void render() {
        content.removeAllViews();
        switch (tab) {
            case "Comidas": meals(); break;
            case "Sueño": sleep(); break;
            case "Horario": schedule(); break;
            case "Hábitos": goals(); break;
            case "Proyectos": projects(); break;
            case "Evolución": week(); break;
            default: today();
        }
    }

    // ================================================================ Hoy (panel de un vistazo)

    private LocalDate viewDate = LocalDate.now();

    private void today() {
        LocalDate today = viewDate;
        boolean isToday = today.equals(LocalDate.now());
        int dow = today.getDayOfWeek().getValue();
        LocalDate monday = VidaCoach.monday(today);

        // ---- datos reales del día (nada de cifras de ejemplo)
        List<VidaLog.Block> agenda = new ArrayList<>();
        for (VidaLog.Block b : log.blocks)
            if (b.day == dow && VidaPlanner.kind(b.title) != VidaPlanner.Kind.TRABAJO
                    && !(VidaPlanner.kind(b.title) == VidaPlanner.Kind.DESCANSO && b.minutes > 180)) agenda.add(b);
        agenda.sort(Comparator.comparing(b -> b.time));
        int blocksDone = 0; for (VidaLog.Block b : agenda) if (VidaPlanner.blockDone(log, b, today)) blocksDone++;
        VidaLog.Focus f = VidaCoach.focusOn(log, today);
        int habitsDone = 0; for (VidaLog.Habit h : log.habits) if (VidaPlanner.checked(log, h.id, today)) habitsDone++;
        List<VidaLog.Meal> eaten = VidaPlanner.mealsOn(log, today);
        VidaLog.SleepEntry night = null; for (VidaLog.SleepEntry e : log.sleep) if (e.date.equals(today.toString())) night = e;
        boolean gymToday = VidaPlanner.checked(log, "gym", today);
        VidaLog.Block gymBlock = null; for (VidaLog.Block b : agenda) if (VidaPlanner.kind(b.title) == VidaPlanner.Kind.GYM) gymBlock = b;
        int tasks = agenda.size() + (f == null ? 0 : 1), tasksDone = blocksDone + (f != null && f.done ? 1 : 0);
        float total = 0, parts = 0;
        if (tasks > 0) { total += (float) tasksDone / tasks; parts++; }
        if (!log.habits.isEmpty()) { total += (float) habitsDone / log.habits.size(); parts++; }
        total += Math.min(1f, eaten.size() / 3f); parts++;
        if (gymBlock != null || gymToday) { total += gymToday ? 1 : 0; parts++; }
        total += night == null ? 0 : Math.min(1f, night.minutes / 450f); parts++;
        float dayProgress = parts == 0 ? 0 : total / parts;
        List<VidaCoach.PlanItem> plan = VidaCoach.dayPlan(log, today);
        // En móviles de menos de 400 dp de ancho las parejas de tarjetas se apilan para que nada se corte.
        boolean compact = getResources().getConfiguration().screenWidthDp < 400;

        // ---- 3. fecha + semana + avisos + ajustes
        LinearLayout dateRow = row(this);
        LinearLayout nav = row(this);
        nav.setBackground(neon(this, false, 12)); nav.setPadding(dp(this, 2), 0, dp(this, 2), 0);
        nav.addView(tapIcon(VidaIcon.Type.CHEVRON, MUTED, 16, v -> { viewDate = viewDate.minusDays(1); render(); }, true));
        nav.addView(icon(VidaIcon.Type.CALENDAR, CYAN, 16));
        TextView date = text(this, null, VidaPlanner.cap(today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es")))), 13, Color.WHITE, true);
        date.setPadding(dp(this, 6), dp(this, 8), dp(this, 2), dp(this, 8)); date.setMaxLines(1);
        date.setEllipsize(android.text.TextUtils.TruncateAt.END);
        date.setOnClickListener(v -> new DatePickerDialog(this, (picker, y, m, d) -> { viewDate = LocalDate.of(y, m + 1, d); render(); },
                today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth()).show());
        nav.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(tapIcon(VidaIcon.Type.CHEVRON, MUTED, 16, v -> { viewDate = viewDate.plusDays(1); render(); }, false));
        dateRow.addView(nav, new LinearLayout.LayoutParams(0, -2, 1));
        TextView week = text(this, null, "Semana " + today.get(java.time.temporal.WeekFields.ISO.weekOfWeekBasedYear()), 12, Color.WHITE, false);
        week.setMaxLines(1); week.setPadding(dp(this, 10), dp(this, 8), dp(this, 10), dp(this, 8));
        week.setBackground(neon(this, false, 10));
        week.setOnClickListener(v -> { viewDate = LocalDate.now(); render(); });
        LinearLayout.LayoutParams wl = new LinearLayout.LayoutParams(-2, -2); wl.setMargins(dp(this, 6), 0, 0, 0);
        dateRow.addView(week, wl);
        View bell = tapIcon(VidaIcon.Type.BELL, VidaReminders.enabled(this) ? CYAN : MUTED, 18, v -> askReminders(), false);
        bell.setBackground(neon(this, false, 10)); bell.setContentDescription("Avisos");
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(-2, -2); bl.setMargins(dp(this, 6), 0, 0, 0);
        dateRow.addView(bell, bl);
        View gear = tapIcon(VidaIcon.Type.GEAR, LIGHT, 18, v -> askSettings(), false);
        gear.setBackground(neon(this, false, 10)); gear.setContentDescription("Ajustes de Vida");
        LinearLayout.LayoutParams gl0 = new LinearLayout.LayoutParams(-2, -2); gl0.setMargins(dp(this, 6), 0, 0, 0);
        dateRow.addView(gear, gl0);
        content.addView(dateRow);

        // ---- 4. Salve te recomienda (plan real calculado de tus datos)
        LinearLayout rec = panel();
        rec.setBackground(neon(this, true, 16));
        LinearLayout rh = row(this); rh.setGravity(Gravity.TOP);
        int face = Math.round(dp(this, 54) * scale(this));
        rh.addView(avatar(face), new LinearLayout.LayoutParams(face, face));
        LinearLayout rt = column(this); rt.setPadding(dp(this, 10), 0, 0, 0);
        LinearLayout rtitle = row(this);
        TextView rtt = text(this, null, "Salve te recomienda", 15, Color.WHITE, true);
        rtt.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD_ITALIC));
        rtitle.addView(rtt); rtitle.addView(icon(VidaIcon.Type.SPARKLE, CYAN, 14));
        rt.addView(rtitle);
        TextView rtext = text(this, rt, VidaCoach.planSummary(plan), 12, LIGHT, false);
        rtext.setPadding(0, dp(this, 3), 0, 0);
        rh.addView(rt, new LinearLayout.LayoutParams(0, -2, 1));
        rec.addView(rh);
        LinearLayout items = column(this); items.setPadding(0, dp(this, 6), 0, dp(this, 4));
        for (VidaCoach.PlanItem p : plan) {
            LinearLayout r = row(this); r.setPadding(dp(this, 4), dp(this, 3), 0, dp(this, 3));
            r.addView(icon(planIcon(p.kind), CYAN, 16));
            TextView t = text(this, null, "  " + p.title, 12, Color.WHITE, false);
            t.setMaxLines(2); t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(text(this, null, "  " + VidaCoach.duration(p.minutes), 11, MUTED, false));
            items.addView(r);
        }
        rec.addView(items);
        Button apply = button(this, "Aplicar plan de hoy", BLUE);
        size(this, apply, 13); apply.setMinHeight(dp(this, 40)); apply.setMinimumHeight(dp(this, 40)); apply.setAllCaps(false);
        apply.setEnabled(!plan.isEmpty());
        apply.setOnClickListener(v -> confirmPlan(today, plan));
        rec.addView(apply, new LinearLayout.LayoutParams(-1, -2));
        LinearLayout rb = row(this);
        Button full = button(this, "Ver plan completo", CARD);
        Button why = button(this, "Explicación del plan", CARD);
        for (Button b : new Button[]{full, why}) {
            size(this, b, 12); b.setAllCaps(false); b.setMaxLines(1);
            b.setMinHeight(dp(this, 36)); b.setMinimumHeight(dp(this, 36)); b.setPadding(dp(this, 4), 0, dp(this, 4), 0);
        }
        full.setOnClickListener(v -> go("Horario"));
        why.setOnClickListener(v -> explainPlan(today, plan));
        LinearLayout.LayoutParams fl = new LinearLayout.LayoutParams(0, -2, 1); fl.setMargins(0, dp(this, 6), dp(this, 3), 0);
        LinearLayout.LayoutParams wl2 = new LinearLayout.LayoutParams(0, -2, 1); wl2.setMargins(dp(this, 3), dp(this, 6), 0, 0);
        rb.addView(full, fl); rb.addView(why, wl2);
        rec.addView(rb);
        content.addView(cols(new View[]{rec}, new float[]{1}));

        // ---- 5. objetivo de hoy | progreso del día
        LinearLayout goal = panel();
        goal.addView(head(VidaIcon.Type.TARGET, "Objetivo de hoy", null, null));
        String goalText = f != null ? f.text : plan.isEmpty() ? null : plan.get(0).title;
        VidaLog.Project gp = VidaCoach.projectOf(log, goalText);
        LinearLayout gb = row(this); gb.setPadding(0, dp(this, 6), 0, dp(this, 4));
        String low = goalText == null ? "" : goalText.toLowerCase(Locale.ROOT);
        gb.addView(icon(low.contains("vídeo") || low.contains("video") || low.contains("reel") || low.contains("escena") ? VidaIcon.Type.CLAPPER : VidaIcon.Type.STAR, Color.WHITE, 30));
        LinearLayout gt = column(this); gt.setPadding(dp(this, 10), 0, 0, 0);
        TextView gtitle = text(this, gt, goalText == null ? "Elige lo más importante de hoy" : goalText, 13, f != null && f.done ? GREEN : Color.WHITE, true);
        gtitle.setMaxLines(3); gtitle.setEllipsize(android.text.TextUtils.TruncateAt.END);
        text(this, gt, f == null ? (goalText == null ? "Una sola cosa: si la terminas, día ganado" : "Propuesta de Salve") : f.done ? "¡Día ganado!" : "Prioridad alta", 11, MUTED, false);
        gb.addView(gt, new LinearLayout.LayoutParams(0, -2, 1));
        goal.addView(gb);
        // Barra: progreso del proyecto al que pertenece (si lo hay) o la propia tarea.
        float gfrac; String glabel;
        if (gp != null) { int[] pp = VidaCoach.progress(gp); gfrac = pp[1] == 0 ? 0 : (float) pp[0] / pp[1]; glabel = "Proyecto: " + pp[0] + "/" + pp[1] + " pasos"; }
        else { gfrac = f != null && f.done ? 1 : 0; glabel = f != null && f.done ? "Completado" : "Pendiente"; }
        LinearLayout gr = row(this);
        TextView gl = text(this, null, glabel, 10, MUTED, false); gl.setMaxLines(1); gl.setEllipsize(android.text.TextUtils.TruncateAt.END);
        gr.addView(gl, new LinearLayout.LayoutParams(0, -2, 1));
        gr.addView(text(this, null, Math.round(gfrac * 100) + "%", 12, CYAN, true));
        goal.addView(gr);
        VidaWidgets.Line gline = new VidaWidgets.Line(this, NEON); gline.set(gfrac);
        LinearLayout.LayoutParams gll = new LinearLayout.LayoutParams(-1, dp(this, 5)); gll.setMargins(0, dp(this, 3), 0, dp(this, 8));
        goal.addView(gline, gll);
        Button gbtn = button(this, f == null ? (goalText == null ? "Elegir objetivo" : "Marcar como objetivo") : f.done ? "Deshacer" : "Marcar como completado", BLUE);
        size(this, gbtn, 12); gbtn.setAllCaps(false); gbtn.setMinHeight(dp(this, 36)); gbtn.setMinimumHeight(dp(this, 36)); gbtn.setPadding(dp(this, 4), 0, dp(this, 4), 0);
        String chosen = goalText;
        gbtn.setOnClickListener(v -> {
            if (f != null) change(l -> VidaCoach.completeFocus(l, today));
            else if (chosen != null) change(l -> VidaCoach.setFocus(l, today, chosen));
            else askFocus(today);
        });
        gbtn.setOnLongClickListener(v -> { askFocus(today); return true; });
        goal.addView(gbtn, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout prog = panel();
        prog.addView(head(VidaIcon.Type.HEART_PULSE, "Progreso del día", null, null));
        LinearLayout pr = row(this); pr.setPadding(0, dp(this, 6), 0, 0); pr.setGravity(Gravity.CENTER_VERTICAL);
        VidaWidgets.Ring ring = new VidaWidgets.Ring(this, CYAN);
        ring.set(dayProgress, Math.round(dayProgress * 100) + "%");
        int ringSize = Math.round(dp(this, 66) * scale(this));
        pr.addView(ring, new LinearLayout.LayoutParams(ringSize, ringSize));
        LinearLayout list = column(this); list.setPadding(dp(this, 8), 0, 0, 0);
        list.addView(meter(VidaIcon.Type.CHECKBOX, "Tareas", tasksDone + "/" + tasks, tasks == 0 ? 0 : (float) tasksDone / tasks));
        list.addView(meter(VidaIcon.Type.CHECK_CIRCLE, "Hábitos", habitsDone + "/" + log.habits.size(), log.habits.isEmpty() ? 0 : (float) habitsDone / log.habits.size()));
        list.addView(meter(VidaIcon.Type.DUMBBELL, "Gym", (gymToday ? 1 : 0) + "/" + (gymBlock != null || gymToday ? 1 : 0), gymToday ? 1 : 0));
        list.addView(meter(VidaIcon.Type.FORK_KNIFE, "Comidas", eaten.size() + "/3", Math.min(1f, eaten.size() / 3f)));
        list.addView(meter(VidaIcon.Type.MOON, "Sueño", (night == null ? "—" : String.format(new Locale("es"), "%.1f", night.minutes / 60.0)) + "/7,5 h", night == null ? 0 : night.minutes / 450f));
        pr.addView(list, new LinearLayout.LayoutParams(0, -2, 1));
        prog.addView(pr);
        content.addView(cols(new View[]{goal, prog}, new float[]{1, 1.15f}, compact));

        // ---- 6. agenda | hábitos (esta semana)
        LinearLayout ag = panel();
        ag.addView(head(VidaIcon.Type.CALENDAR, "Agenda de hoy", null, tapIcon(VidaIcon.Type.PLUS, CYAN, 18, v -> askBlock(dow, null), false)));
        if (agenda.isEmpty()) {
            text(this, ag, "Nada planificado. Aplica el plan de Salve o añade un bloque.", 12, MUTED, false);
            ag.addView(link(this, "Abrir horario ›", v -> go("Horario")));
        }
        String now = nowHm();
        int shown = Math.min(7, agenda.size());
        for (int i = 0; i < shown; i++) {
            VidaLog.Block b = agenda.get(i);
            boolean done = VidaPlanner.blockDone(log, b, today);
            boolean current = isToday && b.time.compareTo(now) <= 0 && endOf(b).compareTo(now) > 0;
            LinearLayout r = row(this); r.setPadding(dp(this, 4), dp(this, 5), dp(this, 4), dp(this, 5));
            if (current) r.setBackground(neon(this, true, 10));
            TextView time = text(this, null, b.time, 11, current ? CYAN : MUTED, current);
            r.addView(time, new LinearLayout.LayoutParams(Math.round(dp(this, 40) * scale(this)), -2));
            View dot = new View(this);
            android.graphics.drawable.GradientDrawable dd = new android.graphics.drawable.GradientDrawable();
            dd.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            dd.setColor(done ? GREEN : current ? CYAN : CARD); dd.setStroke(Math.max(1, dp(this, 1)), done ? GREEN : CYAN);
            dot.setBackground(dd);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(this, 8), dp(this, 8)); dl.setMargins(0, 0, dp(this, 6), 0);
            r.addView(dot, dl);
            r.addView(icon(blockIcon(b), done ? MUTED : current ? CYAN : LIGHT, 16));
            TextView t = text(this, null, " " + b.title, 12, done ? MUTED : Color.WHITE, current);
            t.setMaxLines(2); t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            if (done) t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(text(this, null, " " + VidaCoach.duration(b.minutes), 10, MUTED, false));
            r.setOnClickListener(v -> change(l -> VidaPlanner.toggleBlock(l, b, today)));
            r.setOnLongClickListener(v -> { blockMenu(b, today); return true; });
            ag.addView(r);
        }
        if (agenda.size() > shown) ag.addView(link(this, "+" + (agenda.size() - shown) + " más ›", v -> go("Horario")));

        LinearLayout hb = panel();
        hb.addView(head(VidaIcon.Type.CHECK_CIRCLE, "Hábitos", "(esta semana)", null));
        for (int i = 0; i < Math.min(6, log.habits.size()); i++) {
            VidaLog.Habit h = log.habits.get(i);
            boolean done = VidaPlanner.checked(log, h.id, today);
            int weekDone = VidaPlanner.doneThisWeek(log, h.id, today);
            LinearLayout r = row(this); r.setPadding(0, dp(this, 4), 0, dp(this, 4));
            r.addView(icon(habitIcon(h), done ? CYAN : LIGHT, 16));
            LinearLayout mid = column(this); mid.setPadding(dp(this, 8), 0, dp(this, 6), 0);
            LinearLayout top = row(this);
            TextView n = text(this, null, h.name, 12, Color.WHITE, false);
            n.setMaxLines(2); n.setEllipsize(android.text.TextUtils.TruncateAt.END);
            top.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
            top.addView(text(this, null, weekDone + "/" + h.perWeek, 11, weekDone >= h.perWeek ? GREEN : MUTED, true));
            mid.addView(top);
            // Casillas segmentadas: una por vez de la meta semanal.
            LinearLayout seg = row(this);
            int boxes = Math.max(1, Math.min(7, h.perWeek));
            for (int k = 0; k < boxes; k++) {
                View box = new View(this);
                box.setBackground(rounded(this, k < weekDone ? CYAN : CARD_2, 2));
                LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, dp(this, 5), 1);
                sp.setMargins(k == 0 ? 0 : dp(this, 2), dp(this, 4), 0, 0);
                seg.addView(box, sp);
            }
            mid.addView(seg);
            r.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(icon(done ? VidaIcon.Type.CHECKBOX_ON : VidaIcon.Type.CHECKBOX, done ? CYAN : MUTED, 18));
            r.setOnClickListener(v -> change(l -> VidaPlanner.toggle(l, h.id, today)));
            hb.addView(r);
        }
        if (log.habits.isEmpty()) text(this, hb, "Sin hábitos todavía.", 12, MUTED, false);
        hb.addView(link(this, "Ver todos ›", v -> go("Hábitos")));
        content.addView(cols(new View[]{ag, hb}, new float[]{1.1f, 1}, compact));

        // ---- 7. gym | sueño | evolución semanal
        LinearLayout gym = panel();
        gym.addView(head(VidaIcon.Type.DUMBBELL, "Gym", null, null));
        VidaLog.Habit gh = habit("gym");
        int weekGym = VidaPlanner.doneThisWeek(log, "gym", today), gymGoal = gh == null ? 4 : gh.perWeek;
        text(this, gym, weekGym + " de " + gymGoal + " entrenos", 11, LIGHT, false);
        LinearLayout days = row(this); days.setPadding(0, dp(this, 6), 0, dp(this, 6));
        String[] letters = {"L", "M", "X", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            boolean trained = VidaPlanner.checked(log, "gym", d);
            TextView c = new TextView(this); c.setText(letters[i]); c.setTextColor(Color.WHITE); c.setGravity(Gravity.CENTER);
            c.setAutoSizeTextTypeUniformWithConfiguration(7, 11, 1, android.util.TypedValue.COMPLEX_UNIT_DIP);
            if (d.equals(today)) c.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
            c.setBackground(trained ? rounded(this, BLUE, 5) : neon(this, d.equals(today), 5));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(this, 20), 1); lp.setMargins(dp(this, 1), 0, dp(this, 1), 0);
            days.addView(c, lp);
        }
        gym.addView(days);
        VidaWidgets.Line gyl = new VidaWidgets.Line(this, NEON); gyl.set((float) weekGym / gymGoal);
        gym.addView(gyl, new LinearLayout.LayoutParams(-1, dp(this, 4)));
        gym.addView(link(this, "Abrir gym ›", v -> startActivity(new Intent(this, GymActivity.class))));
        gym.setOnClickListener(v -> startActivity(new Intent(this, GymActivity.class)));

        LinearLayout sl = panel();
        sl.addView(head(VidaIcon.Type.MOON, "Sueño", null, null));
        int avg = VidaPlanner.averageMinutes(VidaPlanner.lastNights(log, 7));
        text(this, sl, "Anoche: " + (night == null ? "sin registro" : hours(night.minutes)), 11, night == null ? MUTED : sleepColor(night.minutes), night != null);
        text(this, sl, "Media 7 días: " + (avg == 0 ? "—" : hours(avg)), 11, LIGHT, false);
        text(this, sl, "Objetivo: 7,5 – 8 h", 10, MUTED, false);
        VidaWidgets.Line sbar = new VidaWidgets.Line(this, night == null ? NEON : sleepColor(night.minutes)); sbar.set(night == null ? 0 : night.minutes / 480f);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(-1, dp(this, 4)); sp.topMargin = dp(this, 6);
        sl.addView(sbar, sp);
        final boolean noNight = night == null;
        sl.addView(link(this, noNight && isToday ? "Registrar ›" : "Ver sueño ›", v -> { if (noNight && isToday) askSleep(today); else go("Sueño"); }));

        LinearLayout evo = panel();
        evo.addView(head(VidaIcon.Type.BARS, "Evolución semanal", null, null));
        float[] daily = new float[7];
        for (int i = 0; i < 7; i++) { LocalDate d = monday.plusDays(i); daily[i] = d.isAfter(LocalDate.now()) ? 0 : VidaCoach.dayScore(log, d); }
        int thisWeek = 0, lastWeek = 0;
        for (int i = 0; i < dow; i++) { thisWeek += VidaCoach.dayScore(log, monday.plusDays(i)); lastWeek += VidaCoach.dayScore(log, monday.minusWeeks(1).plusDays(i)); }
        Integer overall = VidaCoach.change(lastWeek, thisWeek);
        LinearLayout er = row(this); er.setGravity(Gravity.BOTTOM); er.setPadding(0, dp(this, 4), 0, 0);
        LinearLayout barsCol = column(this);
        VidaWidgets.Bars bars = new VidaWidgets.Bars(this); bars.set(daily, dow - 1);
        barsCol.addView(bars, new LinearLayout.LayoutParams(-1, dp(this, 38)));
        LinearLayout lettersRow = row(this);
        for (String l : letters) { TextView t = text(this, null, l, 9, MUTED, false); t.setGravity(Gravity.CENTER); t.setPadding(0, 0, 0, 0); lettersRow.addView(t, new LinearLayout.LayoutParams(0, -2, 1)); }
        barsCol.addView(lettersRow);
        er.addView(barsCol, new LinearLayout.LayoutParams(0, -2, 1));
        TextView ov = text(this, null, deltaText(overall), 13, overall == null || overall >= 0 ? CYAN : AMBER, true);
        ov.setPadding(dp(this, 6), 0, 0, dp(this, 12));
        er.addView(ov);
        evo.addView(er);
        int[] cur = VidaCoach.weekStats(log, today), prev = VidaCoach.weekStats(log, today.minusWeeks(1));
        String[] names = {"Hábitos", "Entrenos", "Sueño", "Proyectos"};
        for (int i = 0; i < 4; i++) {
            Integer ch = VidaCoach.change(prev[i], cur[i]);
            LinearLayout r = row(this);
            TextView n = text(this, null, names[i], 10, LIGHT, false); n.setPadding(0, 0, 0, 0);
            r.addView(n, new LinearLayout.LayoutParams(0, -2, 1));
            TextView v = text(this, null, deltaText(ch), 10, ch == null || ch >= 0 ? CYAN : AMBER, true); v.setPadding(0, 0, 0, 0);
            r.addView(v);
            evo.addView(r);
        }
        text(this, evo, "vs. semana anterior (hasta hoy)", 9, MUTED, false);
        evo.setOnClickListener(v -> go("Evolución"));
        if (compact) {
            content.addView(cols(new View[]{gym, sl}, new float[]{1, 1}));
            content.addView(cols(new View[]{evo}, new float[]{1}));
        } else content.addView(cols(new View[]{gym, sl, evo}, new float[]{1, 0.95f, 1.2f}));

        if (dow == 7 && VidaCoach.reviewFor(log, monday) == null)
            content.addView(link(this, "Hoy es domingo: toca tu revisión semanal ›", v -> go("Evolución")));
    }

    private static String deltaText(Integer change) {
        if (change == null) return "▲ nuevo";
        if (change == 0) return "= 0%";
        return (change > 0 ? "▲ +" : "▼ ") + change + "%";
    }

    private static VidaIcon.Type planIcon(String kind) {
        switch (kind) {
            case "gym": return VidaIcon.Type.DUMBBELL;
            case "estudio": return VidaIcon.Type.BOOK;
            case "proyecto": return VidaIcon.Type.CLAPPER;
            case "descanso": return VidaIcon.Type.MOON;
            default: return VidaIcon.Type.CHECK_CIRCLE;
        }
    }

    private static VidaIcon.Type blockIcon(VidaLog.Block b) {
        switch (VidaPlanner.kind(b.title)) {
            case GYM: return VidaIcon.Type.DUMBBELL;
            case TRABAJO: return VidaIcon.Type.BRIEFCASE;
            case ESTUDIO: return VidaIcon.Type.BOOK;
            case PROYECTO: return VidaIcon.Type.LAPTOP;
            case CREAR: return VidaIcon.Type.CLAPPER;
            case DESCANSO: return VidaIcon.Type.MOON;
            case REVISION: return VidaIcon.Type.COMPASS;
            default: return VidaIcon.Type.CLOCK;
        }
    }

    /** Muestra lo que se va a añadir y solo lo aplica si Bryan acepta. */
    private void confirmPlan(LocalDate day, List<VidaCoach.PlanItem> plan) {
        StringBuilder msg = new StringBuilder("Salve fijará tu tarea principal (si no tienes) y añadirá a la agenda, en huecos libres, lo que aún no tenga hora:\n\n");
        for (VidaCoach.PlanItem p : plan) msg.append("• ").append(p.title).append(" — ").append(VidaCoach.duration(p.minutes)).append('\n');
        msg.append("\nNo se mueve ni se borra nada de lo que ya tienes.");
        new AlertDialog.Builder(this).setTitle("Aplicar plan de hoy").setMessage(msg.toString())
                .setPositiveButton("Aplicar", (d, w) -> {
                    change(l -> VidaCoach.applyPlan(l, day, plan));
                    Toast.makeText(this, "Plan aplicado. Puedes moverlo en Horario.", Toast.LENGTH_LONG).show();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    /**
     * Explicación del plan: primero los motivos calculados de tus datos (siempre disponibles) y
     * después la explicación del modelo de Salve (SalveLLM, rol PLANIFICADOR) fuera del hilo de la
     * pantalla. Si el modelo no está disponible se dice, sin inventar respuesta.
     */
    private void explainPlan(LocalDate day, List<VidaCoach.PlanItem> plan) {
        String reasons = plan.isEmpty() ? "No hay actividades pendientes: vas al día con tus metas de esta semana." : VidaCoach.planReasons(plan);
        TextView body = new TextView(this);
        body.setTextColor(LIGHT); body.setTextIsSelectable(true);
        int p = dp(this, 20); body.setPadding(p, dp(this, 8), p, p);
        body.setText("De tus datos:\n" + reasons + "\n\nSalve está pensando…");
        ScrollView sv = new ScrollView(this); sv.addView(body);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Explicación del plan").setView(sv).setPositiveButton("Cerrar", null).show();
        StringBuilder prompt = new StringBuilder("Explica a Bryan en español, en 3–5 frases cercanas y concretas, por qué este plan de hoy es realista y qué ")
                .append("conseguirá. Usa SOLO estos datos, no inventes cifras.\nFecha: ").append(day).append('\n');
        for (VidaCoach.PlanItem it : plan) prompt.append("- ").append(it.title).append(" (").append(VidaCoach.duration(it.minutes)).append("): ").append(it.reason).append('\n');
        for (VidaLog.Goal g : log.goals) if (!g.done) prompt.append("Objetivo: ").append(g.title).append(g.due.isEmpty() ? "" : " (fecha " + g.due + ")").append('\n');
        String priority = VidaCoach.currentPriority(log, day);
        if (priority != null && !priority.isEmpty()) prompt.append("Prioridad de la semana: ").append(priority).append('\n');
        new Thread(() -> {
            String extra;
            try {
                salve.core.ModelResult r = salve.core.SalveLLM.getInstance(getApplicationContext())
                        .generateResult(prompt.toString(), salve.core.SalveLLM.Role.PLANIFICADOR);
                extra = r != null && r.isSuccess() && r.getText() != null && !r.getText().trim().isEmpty()
                        ? "Salve:\n" + r.getText().trim()
                        : "El modelo de Salve no está disponible ahora (" + (r == null ? "sin respuesta" : r.getStatus()) + "); arriba tienes los motivos calculados de tus datos.";
            } catch (RuntimeException failed) {
                extra = "El modelo de Salve no está disponible ahora; arriba tienes los motivos calculados de tus datos.";
            }
            final String text = extra;
            ui.post(() -> { if (dialog.isShowing()) body.setText("De tus datos:\n" + reasons + "\n\n" + text); });
        }, "vida-explicacion").start();
    }

    private void askReminders() {
        boolean on = VidaReminders.enabled(this);
        new AlertDialog.Builder(this).setTitle("Avisos de Vida")
                .setMessage(on ? "Activados: " + VidaReminders.lead(this) + " min antes de cada bloque y resumen del día a las " + VidaReminders.morning(this) + "."
                        : "Desactivados.")
                .setPositiveButton(on ? "Desactivar" : "Activar", (d, w) -> {
                    VidaReminders.configure(this, !on, VidaReminders.morning(this), VidaReminders.lead(this));
                    io.execute(() -> VidaReminders.schedule(this)); render();
                })
                .setNeutralButton("Más ajustes", (d, w) -> go("Horario"))
                .setNegativeButton("Cerrar", null).show();
    }

    private void askSettings() {
        String[] opts = {"Avisos", "Hábitos y objetivos", "Proyectos", "Gym e importar de Hevy", "Revisión semanal"};
        new AlertDialog.Builder(this).setTitle("Ajustes de Vida").setItems(opts, (d, w) -> {
            switch (w) {
                case 0: askReminders(); break;
                case 1: go("Hábitos"); break;
                case 2: go("Proyectos"); break;
                case 3: startActivity(new Intent(this, GymActivity.class)); break;
                default: go("Evolución");
            }
        }).show();
    }

    private void go(String t) { tab = t; paintTabs(); render(); }

    /** Fila de columnas que se apila en pantallas estrechas. */
    private LinearLayout cols(View[] views, float[] weights) { return cols(views, weights, false); }

    private LinearLayout cols(View[] views, float[] weights, boolean forceStack) {
        boolean stack = (forceStack || narrow(this)) && views.length > 1;
        LinearLayout r = stack ? column(this) : row(this);
        r.setGravity(Gravity.TOP);
        for (int i = 0; i < views.length; i++) {
            LinearLayout.LayoutParams lp = stack ? new LinearLayout.LayoutParams(-1, -2) : new LinearLayout.LayoutParams(0, -1, weights[i]);
            int m = dp(this, 3);
            lp.setMargins(stack ? 0 : (i == 0 ? 0 : m), m, stack ? 0 : (i == views.length - 1 ? 0 : m), m);
            r.addView(views[i], lp);
        }
        LinearLayout.LayoutParams outer = new LinearLayout.LayoutParams(-1, -2); outer.topMargin = dp(this, 4);
        r.setLayoutParams(outer);
        return r;
    }

    private LinearLayout panel() {
        LinearLayout t = column(this);
        int p = Math.round(dp(this, 11) * scale(this)); t.setPadding(p, p, p, p);
        t.setBackground(glass(this));
        return t;
    }

    private LinearLayout head(VidaIcon.Type type, String title, String extra, View right) {
        LinearLayout r = row(this);
        r.addView(icon(type, NEON, 20));
        TextView t = text(this, null, "  " + title, 14, Color.WHITE, true);
        t.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD_ITALIC));
        t.setMaxLines(2); t.setEllipsize(android.text.TextUtils.TruncateAt.END);
        r.addView(t, new LinearLayout.LayoutParams(right == null && extra == null ? 0 : -2, -2, right == null && extra == null ? 1 : 0));
        if (extra != null) r.addView(text(this, null, " " + extra, 12, MUTED, false), new LinearLayout.LayoutParams(0, -2, 1));
        else if (right != null) r.addView(new View(this), new LinearLayout.LayoutParams(0, 1, 1));
        if (right != null) r.addView(right);
        return r;
    }

    private View meter(VidaIcon.Type type, String label, String value, float fraction) {
        LinearLayout r = row(this); r.setPadding(0, dp(this, 2), 0, dp(this, 2));
        r.addView(icon(type, LIGHT, 13));
        TextView t = text(this, null, " " + label, 10, LIGHT, false); t.setMaxLines(1); t.setPadding(0, 0, 0, 0);
        // Ancho fijo para la etiqueta: «Comidas» o «Hábitos» nunca se recortan y las barras quedan alineadas.
        r.addView(t, new LinearLayout.LayoutParams(Math.round(dp(this, 52) * scale(this)), -2));
        VidaWidgets.Line line = new VidaWidgets.Line(this, NEON); line.set(fraction);
        LinearLayout.LayoutParams bl = new LinearLayout.LayoutParams(0, dp(this, 4), 1); bl.setMargins(dp(this, 2), 0, dp(this, 2), 0);
        r.addView(line, bl);
        TextView v = text(this, null, " " + value, 10, Color.WHITE, false); v.setMaxLines(1); v.setPadding(0, 0, 0, 0);
        r.addView(v);
        return r;
    }

    private View icon(VidaIcon.Type type, int color, int sizeDp) {
        VidaIcon i = new VidaIcon(this, type, color);
        int s = Math.round(dp(this, sizeDp) * scale(this));
        i.setLayoutParams(new LinearLayout.LayoutParams(s, s));
        return i;
    }

    private View tapIcon(VidaIcon.Type type, int color, int sizeDp, View.OnClickListener click, boolean mirrored) {
        View i = icon(type, color, sizeDp);
        if (mirrored) i.setScaleX(-1);
        int pad = dp(this, 8);
        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
        box.setPadding(pad, pad, pad, pad);
        box.addView(i);
        box.setOnClickListener(click);
        return box;
    }

    private TextView linkSmall(String label, View.OnClickListener click) {
        TextView t = text(this, null, label, 11, NEON, false);
        t.setOnClickListener(click);
        return t;
    }

    private View divider() {
        View sep = new View(this); sep.setBackgroundColor(Color.rgb(20, 40, 75));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
        sep.setLayoutParams(lp);
        return sep;
    }

    private static VidaIcon.Type habitIcon(VidaLog.Habit h) {
        String n = h.name.toLowerCase(Locale.ROOT);
        if (h.id.equals("gym") || n.contains("gym")) return VidaIcon.Type.DUMBBELL;
        if (n.contains("mate") || n.contains("vídeo")) return VidaIcon.Type.PLAY;
        if (h.id.equals("sueno") || n.contains("dorm")) return VidaIcon.Type.BED;
        if (n.contains("agua")) return VidaIcon.Type.DROP;
        if (n.contains("creatina") || n.contains("suplemento")) return VidaIcon.Type.PILL;
        if (n.contains("historia") || n.contains("escrib")) return VidaIcon.Type.PENCIL;
        if (n.contains("revis")) return VidaIcon.Type.COMPASS;
        if (n.contains("prioridad") || n.contains("proyecto")) return VidaIcon.Type.LAYERS;
        if (n.contains("leer")) return VidaIcon.Type.BOOK;
        return VidaIcon.Type.STAR;
    }

    // ================================================================ Comidas

    private void meals() {
        section(this, content, "Plan semanal");
        HorizontalScrollView daysBar = new HorizontalScrollView(this); daysBar.setHorizontalScrollBarEnabled(false);
        LinearLayout days = row(this);
        for (int d = 1; d <= 7; d++) {
            int day = d;
            TextView c = chip(this, VidaPlanner.dayName(d).substring(0, 3), d == mealDay);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, 0, dp(this, 6), 0);
            c.setOnClickListener(v -> { mealDay = day; render(); });
            days.addView(c, lp);
        }
        daysBar.addView(days); content.addView(daysBar);

        LinearLayout plan = card(this, content);
        text(this, plan, VidaPlanner.dayName(mealDay), 18, BLUE, true);
        for (String kind : VidaPlanner.MEAL_KINDS) {
            String value = VidaPlanner.planned(log, mealDay, kind);
            LinearLayout r = row(this); r.setPadding(0, dp(this, 6), 0, dp(this, 6));
            TextView k = text(this, null, VidaPlanner.cap(kind), 15, MUTED, false);
            r.addView(k, new LinearLayout.LayoutParams(dp(this, 90), -2));
            TextView v = text(this, null, value == null ? "+ Añadir" : value, 16, value == null ? BLUE : Color.WHITE, false);
            r.addView(v, new LinearLayout.LayoutParams(0, -2, 1));
            r.setOnClickListener(x -> askPlanned(mealDay, kind, value));
            plan.addView(r);
        }
        LinearLayout actions = row(this);
        actions.addView(link(this, "Copiar este día a…", v -> askCopyDay()));
        actions.addView(link(this, "Lista de la compra", v -> showShopping()));
        plan.addView(actions);

        section(this, content, "Comido hoy");
        LinearLayout ate = card(this, content);
        List<VidaLog.Meal> meals = VidaPlanner.mealsOn(log, LocalDate.now());
        if (meals.isEmpty()) text(this, ate, "Aún nada anotado hoy.", 15, MUTED, false);
        for (VidaLog.Meal m : meals) text(this, ate, m.time + " · " + VidaPlanner.cap(m.kind) + ": " + m.text, 15, Color.WHITE, false);
        ate.addView(link(this, "+ Anotar comida", v -> askMeal(LocalDate.now())));
        text(this, content, "Consejo: planifica la semana el domingo y usa la lista de la compra. También puedes decirle a Salve «he comido …».", 13, MUTED, false);
    }

    private void askPlanned(int day, String kind, String current) {
        EditText input = field("Ej.: lentejas con arroz", current);
        new AlertDialog.Builder(this).setTitle(VidaPlanner.cap(kind) + " del " + VidaPlanner.dayName(day).toLowerCase(Locale.ROOT))
                .setView(pad(input))
                .setPositiveButton("Guardar", (d, w) -> { String t = input.getText().toString(); change(l -> VidaPlanner.setPlanned(l, day, kind, t)); })
                .setNeutralButton("Quitar", (d, w) -> change(l -> VidaPlanner.setPlanned(l, day, kind, "")))
                .setNegativeButton("Cancelar", null).show();
    }

    private void askCopyDay() {
        String[] names = VidaPlanner.DAYS.clone();
        int from = mealDay;
        new AlertDialog.Builder(this).setTitle("Copiar " + VidaPlanner.dayName(from).toLowerCase(Locale.ROOT) + " a…")
                .setItems(names, (d, which) -> change(l -> VidaPlanner.copyDay(l, from, which + 1))).show();
    }

    private void showShopping() {
        List<String> items = VidaPlanner.shoppingList(log);
        String body = items.isEmpty() ? "Planifica comidas primero." : "• " + String.join("\n• ", items);
        new AlertDialog.Builder(this).setTitle("Lista de la compra (platos de la semana)").setMessage(body)
                .setPositiveButton("Vale", null).show();
    }

    private void askMeal(LocalDate day) {
        LinearLayout box = column(this);
        String[] kinds = VidaPlanner.MEAL_KINDS;
        final String[] chosen = {guessKind()};
        LinearLayout chips = row(this);
        List<TextView> chipViews = new ArrayList<>();
        for (String k : kinds) {
            TextView c = chip(this, VidaPlanner.cap(k), k.equals(chosen[0]));
            c.setOnClickListener(v -> {
                chosen[0] = k;
                for (int i = 0; i < kinds.length; i++) {
                    TextView cv = chipViews.get(i);
                    boolean sel = kinds[i].equals(k);
                    cv.setBackground(rounded(this, sel ? BLUE : CARD_2, 18));
                    cv.setTextColor(sel ? Color.WHITE : LIGHT);
                }
            });
            chipViews.add(c);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, 0, dp(this, 4), 0);
            chips.addView(c, lp);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.addView(chips);
        box.addView(hs);
        EditText input = field("¿Qué has comido?", null);
        box.addView(input);
        new AlertDialog.Builder(this).setTitle("Anotar comida").setView(pad(box))
                .setPositiveButton("Guardar", (d, w) -> {
                    String t = input.getText().toString().trim();
                    if (t.isEmpty()) return;
                    change(l -> VidaPlanner.logMeal(l, day, nowHm(), chosen[0], t));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // ================================================================ Sueño

    private void sleep() {
        LocalDate today = LocalDate.now();
        section(this, content, "Anotar noche");
        LinearLayout add = card(this, content);
        text(this, add, "Registra a qué hora te acostaste y te levantaste. Las noches de 7 h o más marcan solas el hábito «Dormir 7 h».", 14, MUTED, false);
        Button now = button(this, "Anotar cómo dormí anoche", BLUE);
        now.setOnClickListener(v -> askSleep(today));
        add.addView(now, wide(this, 6));
        add.addView(link(this, "Anotar otra noche (elegir fecha)", v -> {
            new DatePickerDialog(this, (dp, y, m, d) -> askSleep(LocalDate.of(y, m + 1, d)),
                    today.getYear(), today.getMonthValue() - 1, today.getDayOfMonth()).show();
        }));

        List<VidaLog.SleepEntry> nights = VidaPlanner.lastNights(log, 14);
        section(this, content, "Últimas noches");
        LinearLayout list = card(this, content);
        if (nights.isEmpty()) text(this, list, "Aún no hay noches registradas.", 15, MUTED, false);
        int avg7 = VidaPlanner.averageMinutes(VidaPlanner.lastNights(log, 7));
        int avg30 = VidaPlanner.averageMinutes(VidaPlanner.lastNights(log, 30));
        if (avg7 > 0) {
            text(this, list, "Media 7 noches: " + hours(avg7) + " · 30 noches: " + hours(avg30), 15, sleepColor(avg7), true);
            if (avg7 < 7 * 60) text(this, list, "Estás por debajo de 7 h. En tus notas de Hevy, tus mejores entrenos coinciden con dormir bien.", 13, AMBER, false);
        }
        for (int i = nights.size() - 1; i >= 0; i--) {
            VidaLog.SleepEntry e = nights.get(i);
            LinearLayout r = row(this); r.setPadding(0, dp(this, 3), 0, dp(this, 3));
            TextView date = text(this, null, shortDate(e.date), 14, MUTED, false);
            r.addView(date, new LinearLayout.LayoutParams(dp(this, 80), -2));
            View barView = new View(this);
            barView.setBackground(rounded(this, sleepColor(e.minutes), 4));
            int width = (int) (dp(this, 170) * Math.min(1.0, e.minutes / 600.0));
            r.addView(barView, new LinearLayout.LayoutParams(Math.max(dp(this, 4), width), dp(this, 14)));
            TextView h = text(this, null, "  " + hours(e.minutes), 14, Color.WHITE, false);
            r.addView(h);
            list.addView(r);
        }
    }

    private void askSleep(LocalDate day) {
        final String[] bed = {null};
        TimePickerDialog wake = new TimePickerDialog(this, (tp, h, m) -> {
            String w = String.format(Locale.ROOT, "%02d:%02d", h, m);
            String b = bed[0];
            change(l -> VidaPlanner.logSleep(l, day, b, w));
        }, 8, 0, true);
        wake.setTitle("¿A qué hora te levantaste?");
        TimePickerDialog bedPicker = new TimePickerDialog(this, (tp, h, m) -> {
            bed[0] = String.format(Locale.ROOT, "%02d:%02d", h, m);
            wake.show();
        }, 0, 0, true);
        bedPicker.setTitle("¿A qué hora te acostaste?");
        bedPicker.show();
    }

    // ================================================================ Horario

    // ================================================================ Horario (tabla semanal)

    private static final int HOUR_DP = 54, COL_DP = 120, AXIS_DP = 46, HEAD_DP = 44;

    private void schedule() {
        LocalDate today = LocalDate.now();
        int dow = today.getDayOfWeek().getValue();
        LocalDate monday = VidaCoach.monday(today);

        int[] c = VidaPlanner.weekCompliance(log, today);
        LinearLayout top = card(this, content);
        text(this, top, "Esta semana: " + c[0] + " de " + c[1] + " bloques hechos", 17, Color.WHITE, true);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, c[1])); bar.setProgress(c[0]);
        top.addView(bar, wide(this, 4));
        StringBuilder split = new StringBuilder();
        for (java.util.Map.Entry<VidaPlanner.Kind, Integer> e : VidaPlanner.minutesByKind(log).entrySet()) {
            if (split.length() > 0) split.append(" · ");
            split.append(kindName(e.getKey())).append(" ").append(e.getValue() / 60).append(" h");
            if (e.getValue() % 60 != 0) split.append(" ").append(e.getValue() % 60);
        }
        if (split.length() > 0) text(this, top, "Tu semana planificada: " + split, 13, MUTED, false);
        LinearLayout actions = row(this);
        Button add = button(this, "+ Bloque", BLUE);
        add.setOnClickListener(v -> askBlock(null, null));
        Button propose = button(this, "Proponer mi semana", CARD_2);
        propose.setOnClickListener(v -> askProposal());
        actions.addView(add, weight(this, 1, 3)); actions.addView(propose, weight(this, 1.4f, 3));
        top.addView(actions, wide(this, 8));
        text(this, top, "Toca un hueco para añadir · toca un bloque para marcarlo hecho, moverlo o quitarlo.", 12, MUTED, false);

        // Rango de horas: de 6:00 (o antes si hay bloques) a medianoche
        int startHour = 6;
        for (VidaLog.Block b : log.blocks) startHour = Math.min(startHour, VidaPlanner.minutes(b.time) / 60);
        final int firstHour = startHour, hours = 24 - startHour;
        int hourPx = dp(this, HOUR_DP), colPx = dp(this, COL_DP), headPx = dp(this, HEAD_DP);

        LinearLayout grid = row(this);
        grid.setGravity(android.view.Gravity.TOP);
        LinearLayout axis = column(this);
        View spacer = new View(this); axis.addView(spacer, new LinearLayout.LayoutParams(dp(this, AXIS_DP), headPx));
        for (int h = firstHour; h < 24; h++) {
            TextView t = text(this, null, String.format(Locale.ROOT, "%02d:00", h), 11, MUTED, false);
            t.setPadding(0, 0, 0, 0);
            axis.addView(t, new LinearLayout.LayoutParams(dp(this, AXIS_DP), hourPx));
        }
        grid.addView(axis);

        HorizontalScrollView hs = new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout columns = row(this); columns.setGravity(android.view.Gravity.TOP);
        for (int d = 1; d <= 7; d++) {
            final int day = d;
            LocalDate date = monday.plusDays(d - 1);
            LinearLayout col = column(this);
            TextView head = text(this, null, VidaPlanner.dayName(d).substring(0, 3) + " " + date.getDayOfMonth(), 15, d == dow ? Color.WHITE : LIGHT, d == dow);
            head.setGravity(android.view.Gravity.CENTER);
            head.setBackground(rounded(this, d == dow ? BLUE : CARD_2, 8));
            LinearLayout.LayoutParams hp = new LinearLayout.LayoutParams(colPx - dp(this, 4), headPx - dp(this, 6));
            hp.setMargins(dp(this, 2), 0, dp(this, 2), dp(this, 6));
            col.addView(head, hp);

            android.widget.FrameLayout body = new android.widget.FrameLayout(this);
            body.setBackgroundColor(d == dow ? Color.rgb(16, 22, 34) : Color.rgb(12, 12, 14));
            for (int h = 0; h <= hours; h++) {
                View lineView = new View(this); lineView.setBackgroundColor(Color.rgb(40, 40, 44));
                android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(-1, 1);
                lp.topMargin = h * hourPx; body.addView(lineView, lp);
            }
            final float[] touchY = {0};
            body.setOnTouchListener((v, e) -> { if (e.getAction() == android.view.MotionEvent.ACTION_DOWN) touchY[0] = e.getY(); return false; });
            body.setOnClickListener(v -> {
                int minute = firstHour * 60 + (int) (touchY[0] / hourPx * 60);
                minute = Math.max(0, Math.min(23 * 60 + 30, minute / 30 * 30));
                askBlock(day, VidaPlanner.hm(minute));
            });
            List<VidaLog.Block> blocks = new ArrayList<>();
            for (VidaLog.Block b : log.blocks) if (b.day == d) blocks.add(b);
            for (VidaLog.Block b : blocks) {
                int start = VidaPlanner.minutes(b.time) - firstHour * 60;
                int len = Math.min(b.minutes, 24 * 60 - VidaPlanner.minutes(b.time));
                boolean done = VidaPlanner.blockDone(log, b, date);
                TextView t = new TextView(this);
                t.setText((done ? "✓ " : "") + b.title + "\n" + b.time);
                t.setTextColor(Color.WHITE); t.setTextSize(12);
                t.setPadding(dp(this, 6), dp(this, 3), dp(this, 4), dp(this, 2));
                int color = kindColor(VidaPlanner.kind(b.title));
                t.setBackground(rounded(this, done ? Color.rgb(46, 125, 50) : color, 6));
                if (date.isBefore(today) && !done && VidaPlanner.kind(b.title) != VidaPlanner.Kind.DESCANSO) t.setAlpha(0.55f);
                android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(-1, Math.max(dp(this, 22), len * hourPx / 60 - dp(this, 2)));
                lp.topMargin = start * hourPx / 60 + 1; lp.leftMargin = dp(this, 2); lp.rightMargin = dp(this, 2);
                t.setOnClickListener(v -> blockMenu(b, date));
                body.addView(t, lp);
            }
            if (d == dow) {
                int nowMin = java.time.LocalTime.now().getHour() * 60 + java.time.LocalTime.now().getMinute() - firstHour * 60;
                if (nowMin >= 0) {
                    View now = new View(this); now.setBackgroundColor(RED);
                    android.widget.FrameLayout.LayoutParams lp = new android.widget.FrameLayout.LayoutParams(-1, dp(this, 2));
                    lp.topMargin = nowMin * hourPx / 60; body.addView(now, lp);
                }
            }
            col.addView(body, new LinearLayout.LayoutParams(colPx, hours * hourPx));
            columns.addView(col);
        }
        hs.addView(columns);
        grid.addView(hs, new LinearLayout.LayoutParams(0, -2, 1));
        content.addView(grid, wide(this, 10));
        hs.post(() -> hs.scrollTo(Math.max(0, (dow - 1) * colPx - colPx / 2), 0));

        LinearLayout legend = card(this, content);
        text(this, legend, "Colores", 15, Color.WHITE, true);
        StringBuilder l = new StringBuilder();
        for (VidaPlanner.Kind k : VidaPlanner.Kind.values()) { if (l.length() > 0) l.append("   "); l.append("■ ").append(kindName(k)); }
        android.text.SpannableString span = new android.text.SpannableString(l.toString());
        int pos = 0;
        for (VidaPlanner.Kind k : VidaPlanner.Kind.values()) {
            int i = l.indexOf("■", pos);
            span.setSpan(new android.text.style.ForegroundColorSpan(kindColor(k)), i, i + 1, 0);
            pos = i + 1;
        }
        TextView lt = text(this, legend, "", 13, LIGHT, false); lt.setText(span);

        reminders();
    }

    private void reminders() {
        LinearLayout card = card(this, content);
        text(this, card, "Avisos", 17, Color.WHITE, true);
        CheckBox on = new CheckBox(this);
        on.setText("Avisarme " + VidaReminders.lead(this) + " min antes de cada bloque y resumen del día a las " + VidaReminders.morning(this));
        on.setTextColor(LIGHT); on.setChecked(VidaReminders.enabled(this));
        on.setOnCheckedChangeListener((b, checked) -> {
            VidaReminders.configure(this, checked, VidaReminders.morning(this), VidaReminders.lead(this));
            io.execute(() -> VidaReminders.schedule(this));
        });
        card.addView(on);
        LinearLayout r = row(this);
        r.addView(link(this, "Cambiar hora del resumen", v -> {
            String[] p = VidaReminders.morning(this).split(":");
            new TimePickerDialog(this, (tp, h, m) -> {
                VidaReminders.configure(this, true, String.format(Locale.ROOT, "%02d:%02d", h, m), VidaReminders.lead(this));
                io.execute(() -> VidaReminders.schedule(this)); render();
            }, Integer.parseInt(p[0]), Integer.parseInt(p[1]), true).show();
        }));
        r.addView(link(this, "Minutos antes", v -> {
            String[] opts = {"5", "10", "15", "30"};
            new AlertDialog.Builder(this).setTitle("¿Cuántos minutos antes?").setItems(opts, (d, w) -> {
                VidaReminders.configure(this, true, VidaReminders.morning(this), Integer.parseInt(opts[w]));
                io.execute(() -> VidaReminders.schedule(this)); render();
            }).show();
        }));
        card.addView(r);
        text(this, card, "Los bloques de descanso no avisan. Android puede retrasar un aviso unos minutos para ahorrar batería.", 12, MUTED, false);
    }

    private void blockMenu(VidaLog.Block b, LocalDate date) {
        boolean done = VidaPlanner.blockDone(log, b, date);
        String head = b.title + " · " + VidaPlanner.dayName(b.day) + " " + b.time + "–" + endOf(b);
        String[] items = {done ? "Desmarcar (no hecho)" : "✓ Marcar como hecho", "Cambiar hora o duración", "Quitar este día", "Quitar todos los días"};
        new AlertDialog.Builder(this).setTitle(head).setItems(items, (d, which) -> {
            if (which == 0) change(l -> VidaPlanner.toggleBlock(l, b, date));
            else if (which == 1) askMove(b);
            else change(l -> VidaPlanner.removeBlock(l, b, which == 3));
        }).show();
    }

    private void askMove(VidaLog.Block b) {
        String[] p = b.time.split(":");
        new TimePickerDialog(this, (tp, h, m) -> {
            EditText mins = field("Duración en minutos", String.valueOf(b.minutes));
            mins.setInputType(InputType.TYPE_CLASS_NUMBER);
            new AlertDialog.Builder(this).setTitle("Duración").setView(pad(mins))
                    .setPositiveButton("Guardar", (d, w) -> {
                        int dur;
                        try { dur = Integer.parseInt(mins.getText().toString().trim()); } catch (NumberFormatException e) { dur = b.minutes; }
                        int fd = Math.max(5, Math.min(16 * 60, dur));
                        String t = String.format(Locale.ROOT, "%02d:%02d", h, m);
                        change(l -> VidaPlanner.moveBlock(l, b, t, fd));
                    }).setNegativeButton("Cancelar", null).show();
        }, Integer.parseInt(p[0]), Integer.parseInt(p[1]), true).show();
    }

    private void askBlock(Integer presetDay, String presetTime) {
        LinearLayout box = column(this);
        EditText title = field("Título (Gym, Matemáticas, Salve…)", null);
        box.addView(title);
        LinearLayout quick = row(this);
        for (String q : new String[]{"Gym", "Matemáticas", "Salve", "Canal", "Trabajo"}) {
            TextView c = chip(this, q, false);
            c.setOnClickListener(v -> title.setText(q));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2); lp.setMargins(0, dp(this, 4), dp(this, 4), dp(this, 4));
            quick.addView(c, lp);
        }
        HorizontalScrollView qs = new HorizontalScrollView(this); qs.addView(quick); box.addView(qs);
        LinearLayout days = row(this);
        List<CheckBox> checks = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            CheckBox c = new CheckBox(this); c.setText(VidaPlanner.dayName(d).substring(0, 2)); c.setTextColor(Color.WHITE);
            if (presetDay != null && presetDay == d) c.setChecked(true);
            checks.add(c); days.addView(c);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.addView(days); box.addView(hs);
        EditText time = field("Hora (HH:mm)", presetTime == null ? "19:00" : presetTime);
        time.setInputType(InputType.TYPE_CLASS_DATETIME | InputType.TYPE_DATETIME_VARIATION_TIME);
        box.addView(time);
        EditText minutes = field("Duración en minutos", "60");
        minutes.setInputType(InputType.TYPE_CLASS_NUMBER);
        box.addView(minutes);
        new AlertDialog.Builder(this).setTitle("Nuevo bloque").setView(pad(box))
                .setPositiveButton("Guardar", (d, w) -> {
                    String t = title.getText().toString().trim();
                    String hm = time.getText().toString().trim();
                    int mins;
                    try { mins = Integer.parseInt(minutes.getText().toString().trim()); } catch (NumberFormatException e) { mins = 60; }
                    if (t.isEmpty() || !hm.matches("\\d{1,2}[:.]\\d{2}")) { Toast.makeText(this, "Pon un título y una hora como 19:00.", Toast.LENGTH_LONG).show(); return; }
                    String[] p = hm.split("[:.]");
                    String clean = String.format(Locale.ROOT, "%02d:%02d", Integer.parseInt(p[0]) % 24, Math.min(59, Integer.parseInt(p[1])));
                    java.util.Set<Integer> chosen = new java.util.TreeSet<>();
                    for (int i = 0; i < 7; i++) if (checks.get(i).isChecked()) chosen.add(i + 1);
                    if (chosen.isEmpty()) { Toast.makeText(this, "Elige al menos un día.", Toast.LENGTH_LONG).show(); return; }
                    int dur = Math.max(5, Math.min(16 * 60, mins));
                    change(l -> VidaPlanner.addBlocks(l, t, chosen, clean, dur));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void askProposal() {
        LinearLayout box = column(this);
        text(this, box, "Te propongo una semana según tus objetivos: gym 4 días, matemáticas para la UNED, 2 h de tu prioridad, microhistoria, canal y revisión el domingo, sábado libre y desconectar a las 23:00. Dime cuándo trabajas:", 14, LIGHT, false);
        EditText from = field("Empiezo a trabajar (hora)", "9"); from.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(from);
        EditText to = field("Termino de trabajar (hora)", "17"); to.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(to);
        LinearLayout days = row(this);
        List<CheckBox> checks = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            CheckBox c = new CheckBox(this); c.setText(VidaPlanner.dayName(d).substring(0, 2)); c.setTextColor(Color.WHITE);
            c.setChecked(d <= 5); checks.add(c); days.addView(c);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.addView(days); box.addView(hs);
        new AlertDialog.Builder(this).setTitle("Proponer mi semana").setView(pad(box))
                .setPositiveButton("Proponer", (d, w) -> {
                    int a, b;
                    try { a = Integer.parseInt(from.getText().toString().trim()); b = Integer.parseInt(to.getText().toString().trim()); }
                    catch (NumberFormatException e) { a = 9; b = 17; }
                    a = Math.max(0, Math.min(23, a)); b = Math.max(a + 1, Math.min(23, b));
                    java.util.Set<Integer> work = new java.util.HashSet<>();
                    for (int i = 0; i < 7; i++) if (checks.get(i).isChecked()) work.add(i + 1);
                    List<VidaLog.Block> week = VidaPlanner.suggestedWeek(a, b, work);
                    new AlertDialog.Builder(this).setTitle("¿Aplicar la propuesta?")
                            .setMessage("Se añaden " + week.size() + " bloques. Tus bloques que choquen con ellos se quitan; el resto se conserva. Luego puedes ajustarlo todo.")
                            .setPositiveButton("Aplicar", (dd, ww) -> change(l -> VidaPlanner.applySuggested(l, week)))
                            .setNegativeButton("Cancelar", null).show();
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private static int kindColor(VidaPlanner.Kind k) {
        switch (k) {
            case GYM: return Color.rgb(46, 125, 50);
            case TRABAJO: return Color.rgb(84, 110, 122);
            case ESTUDIO: return Color.rgb(123, 31, 162);
            case PROYECTO: return Color.rgb(21, 101, 192);
            case CREAR: return Color.rgb(230, 81, 0);
            case DESCANSO: return Color.rgb(55, 71, 79);
            case REVISION: return Color.rgb(0, 131, 143);
            default: return Color.rgb(97, 97, 97);
        }
    }

    private static String kindName(VidaPlanner.Kind k) {
        switch (k) {
            case GYM: return "Gym"; case TRABAJO: return "Trabajo"; case ESTUDIO: return "Estudio";
            case PROYECTO: return "Proyecto"; case CREAR: return "Crear"; case DESCANSO: return "Descanso";
            case REVISION: return "Revisión"; default: return "Otros";
        }
    }

    // ================================================================ Proyectos

    private void projects() {
        int activeCount = VidaCoach.activeCount(log);
        section(this, content, "Proyectos activos (" + activeCount + "/" + VidaCoach.MAX_ACTIVE + ")");
        text(this, content, "Máximo " + VidaCoach.MAX_ACTIVE + " a la vez. Para abrir otro, termina o pausa uno: así se acaban las cosas.", 13, MUTED, false);
        Button add = button(this, "+ Nuevo proyecto", activeCount >= VidaCoach.MAX_ACTIVE ? CARD_2 : BLUE);
        add.setOnClickListener(v -> askProject());
        content.addView(add, wide(this, 6));
        for (VidaLog.Project j : log.projects) if (VidaCoach.ACTIVE.equals(j.status)) projectCard(j);
        boolean header = false;
        for (VidaLog.Project j : log.projects) {
            if (!VidaCoach.PAUSED.equals(j.status)) continue;
            if (!header) { section(this, content, "En pausa"); header = true; }
            projectCard(j);
        }
        header = false;
        for (VidaLog.Project j : log.projects) {
            if (!VidaCoach.DONE.equals(j.status)) continue;
            if (!header) { section(this, content, "Terminados 🏁"); header = true; }
            LinearLayout c = card(this, content);
            text(this, c, "✔ " + j.title, 16, GREEN, true);
            text(this, c, j.steps.size() + " pasos", 13, MUTED, false);
        }
    }

    private void projectCard(VidaLog.Project j) {
        LinearLayout c = card(this, content);
        boolean active = VidaCoach.ACTIVE.equals(j.status);
        LinearLayout head = row(this);
        head.addView(text(this, null, j.title, 18, active ? Color.WHITE : MUTED, true), new LinearLayout.LayoutParams(0, -2, 1));
        TextView more = text(this, null, "•••", 20, Color.WHITE, true);
        more.setPadding(dp(this, 12), 0, 0, 0);
        more.setOnClickListener(v -> {
            String[] items = active ? new String[]{"Marcar como terminado", "Pausar", "Borrar"} : new String[]{"Reactivar", "Marcar como terminado", "Borrar"};
            new AlertDialog.Builder(this).setTitle(j.title).setItems(items, (d, w) -> {
                String choice = items[w];
                if (choice.startsWith("Marcar")) change(l -> VidaCoach.setStatus(l, j.id, VidaCoach.DONE));
                else if (choice.equals("Pausar")) change(l -> VidaCoach.setStatus(l, j.id, VidaCoach.PAUSED));
                else if (choice.equals("Reactivar")) change(l -> VidaCoach.setStatus(l, j.id, VidaCoach.ACTIVE));
                else new AlertDialog.Builder(this).setTitle("¿Borrar «" + j.title + "»?")
                            .setPositiveButton("Borrar", (dd, ww) -> change(l -> VidaCoach.removeProject(l, j.id)))
                            .setNegativeButton("Cancelar", null).show();
            }).show();
        });
        head.addView(more);
        c.addView(head);
        int[] p = VidaCoach.progress(j);
        android.widget.ProgressBar bar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(Math.max(1, p[1])); bar.setProgress(p[0]);
        c.addView(bar, wide(this, 4));
        text(this, c, p[0] + " de " + p[1] + " pasos", 13, MUTED, false);
        for (int i = 0; i < j.steps.size(); i++) {
            final int index = i;
            VidaLog.Project.Step st = j.steps.get(i);
            LinearLayout r = row(this); r.setPadding(0, dp(this, 4), 0, dp(this, 4));
            TextView box = text(this, null, st.done ? "✔" : "○", 20, st.done ? GREEN : MUTED, true);
            box.setPadding(0, 0, dp(this, 10), 0);
            r.addView(box);
            r.addView(text(this, null, st.text, 15, st.done ? MUTED : Color.WHITE, false), new LinearLayout.LayoutParams(0, -2, 1));
            r.setOnClickListener(v -> change(l -> VidaCoach.toggleStep(l, j.id, index)));
            r.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle(st.text).setItems(new String[]{"Hacerlo mi tarea de hoy", "Quitar paso"}, (d, w) -> {
                    if (w == 0) change(l -> VidaCoach.setFocus(l, LocalDate.now(), j.title + ": " + st.text));
                    else change(l -> VidaCoach.removeStep(l, j.id, index));
                }).show();
                return true;
            });
            c.addView(r);
        }
        LinearLayout actions = row(this);
        actions.addView(link(this, "+ Paso", v -> {
            EditText input = field("Siguiente paso", null);
            new AlertDialog.Builder(this).setTitle("Nuevo paso").setView(pad(input))
                    .setPositiveButton("Añadir", (d, w) -> { String t = input.getText().toString().trim(); if (!t.isEmpty()) change(l -> VidaCoach.addStep(l, j.id, t)); })
                    .setNegativeButton("Cancelar", null).show();
        }));
        VidaLog.Project.Step next = VidaCoach.nextStep(j);
        if (active && next != null)
            actions.addView(link(this, "Hacer el siguiente mi tarea de hoy", v -> change(l -> VidaCoach.setFocus(l, LocalDate.now(), j.title + ": " + next.text))));
        c.addView(actions);
    }

    private void askProject() {
        if (VidaCoach.activeCount(log) >= VidaCoach.MAX_ACTIVE) {
            new AlertDialog.Builder(this).setTitle("Ya tienes " + VidaCoach.MAX_ACTIVE + " proyectos activos")
                    .setMessage("Termina o pausa uno antes de abrir otro. Así te centras en acabar.")
                    .setPositiveButton("Entendido", null).show();
            return;
        }
        LinearLayout box = column(this);
        EditText title = field("Nombre del proyecto", null); box.addView(title);
        EditText steps = new EditText(this); steps.setHint("Pasos, uno por línea"); steps.setMinLines(4);
        steps.setGravity(android.view.Gravity.TOP); box.addView(steps);
        new AlertDialog.Builder(this).setTitle("Nuevo proyecto").setView(pad(box))
                .setPositiveButton("Crear", (d, w) -> {
                    String t = title.getText().toString().trim();
                    if (t.isEmpty()) return;
                    List<String> list = java.util.Arrays.asList(steps.getText().toString().split("\n"));
                    change(l -> VidaCoach.addProject(l, t, list, LocalDate.now()));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // ================================================================ Tarea más importante

    private void askFocus(LocalDate day) {
        LinearLayout box = column(this);
        EditText input = field("¿Qué es lo más importante hoy?", VidaCoach.focusOn(log, day) == null ? null : VidaCoach.focusOn(log, day).text);
        box.addView(input);
        List<String> ideas = VidaCoach.focusIdeas(log, day);
        if (!ideas.isEmpty()) {
            text(this, box, "Ideas de tus proyectos y objetivos:", 13, MUTED, false);
            for (String idea : ideas) {
                TextView t = link(this, "• " + idea, v -> input.setText(idea));
                box.addView(t);
            }
        }
        ScrollView sv = new ScrollView(this); sv.addView(pad(box));
        new AlertDialog.Builder(this).setTitle("Tarea más importante").setView(sv)
                .setPositiveButton("Guardar", (d, w) -> { String t = input.getText().toString(); change(l -> VidaCoach.setFocus(l, day, t)); })
                .setNegativeButton("Cancelar", null).show();
    }

    // ================================================================ Revisión semanal

    private void week() {
        LocalDate today = LocalDate.now();
        LocalDate monday = VidaCoach.monday(today);
        section(this, content, "Tu semana");
        LinearLayout report = card(this, content);
        text(this, report, "Del " + shortDate(monday.toString()) + " al " + shortDate(monday.plusDays(6).toString()), 14, MUTED, false);
        text(this, report, VidaCoach.weekReport(log, today), 15, Color.WHITE, false);

        section(this, content, "Revisión del domingo");
        VidaLog.Review r = VidaCoach.reviewFor(log, monday);
        LinearLayout form = card(this, content);
        text(this, form, r == null ? "Tres preguntas, 5 minutos. La prioridad que elijas aparecerá cada día en «Hoy»." : "Ya la hiciste esta semana; puedes editarla.", 14, MUTED, false);
        EditText good = multi("¿Qué salió bien?", r == null ? null : r.good);
        EditText bad = multi("¿Qué falló y por qué?", r == null ? null : r.bad);
        EditText prio = multi("Prioridad de la semana que viene (una)", r == null ? null : r.priority);
        form.addView(good); form.addView(bad); form.addView(prio);
        Button save = button(this, r == null ? "Guardar revisión" : "Actualizar revisión", BLUE);
        save.setOnClickListener(v -> {
            String g = good.getText().toString(), b = bad.getText().toString(), p = prio.getText().toString();
            if (p.trim().isEmpty()) { Toast.makeText(this, "Elige al menos la prioridad de la semana.", Toast.LENGTH_LONG).show(); return; }
            change(l -> VidaCoach.saveReview(l, monday, g, b, p));
            Toast.makeText(this, "Revisión guardada. ¡Buena semana!", Toast.LENGTH_SHORT).show();
        });
        form.addView(save, wide(this, 8));

        if (log.reviews.size() > (r == null ? 0 : 1)) {
            section(this, content, "Revisiones anteriores");
            for (int i = log.reviews.size() - 1; i >= 0; i--) {
                VidaLog.Review old = log.reviews.get(i);
                if (old.week.equals(monday.toString())) continue;
                LinearLayout c = card(this, content);
                text(this, c, "Semana del " + shortDate(old.week), 15, BLUE, true);
                if (!old.good.isEmpty()) text(this, c, "Bien: " + old.good, 14, LIGHT, false);
                if (!old.bad.isEmpty()) text(this, c, "Falló: " + old.bad, 14, LIGHT, false);
                if (!old.priority.isEmpty()) text(this, c, "Prioridad: " + old.priority, 14, AMBER, false);
            }
        }
    }

    private EditText multi(String hint, String value) {
        EditText e = new EditText(this); e.setHint(hint); e.setHintTextColor(Color.GRAY); e.setTextColor(Color.WHITE);
        e.setMinLines(2); e.setGravity(android.view.Gravity.TOP);
        if (value != null) e.setText(value);
        return e;
    }

    // ================================================================ Objetivos y hábitos

    private void goals() {
        section(this, content, "Objetivos");
        Button add = button(this, "+ Nuevo objetivo", BLUE);
        add.setOnClickListener(v -> askGoal());
        content.addView(add, wide(this, 4));
        int done = 0;
        for (VidaLog.Goal g : log.goals) if (g.done) done++;
        if (!log.goals.isEmpty()) text(this, content, done + " de " + log.goals.size() + " completados", 14, MUTED, false);
        for (VidaLog.Goal g : VidaPlanner.sortedGoals(log)) {
            LinearLayout card = card(this, content);
            LinearLayout r = row(this);
            TextView box = text(this, null, g.done ? "✔" : "○", 22, g.done ? GREEN : MUTED, true);
            box.setPadding(0, 0, dp(this, 12), 0);
            LinearLayout texts = column(this);
            text(this, texts, g.title, 16, g.done ? MUTED : Color.WHITE, true);
            text(this, texts, (g.area.isEmpty() ? "" : g.area + " · ") + dueText(g), 13, g.done ? MUTED : dueColor(g), false);
            r.addView(box); r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
            card.addView(r);
            card.setOnClickListener(v -> change(l -> VidaPlanner.setGoalDone(l, g.id, !g.done)));
            card.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("¿Borrar «" + g.title + "»?")
                        .setPositiveButton("Borrar", (d, w) -> change(l -> VidaPlanner.removeGoal(l, g.id)))
                        .setNegativeButton("Cancelar", null).show();
                return true;
            });
        }

        section(this, content, "Hábitos");
        Button addHabit = button(this, "+ Nuevo hábito", CARD_2);
        addHabit.setOnClickListener(v -> askHabit());
        content.addView(addHabit, wide(this, 4));
        for (VidaLog.Habit h : log.habits) {
            LinearLayout card = card(this, content);
            text(this, card, h.name, 16, Color.WHITE, true);
            int streak = VidaPlanner.weekStreak(log, h, LocalDate.now());
            text(this, card, "Meta: " + h.perWeek + (h.perWeek == 7 ? " días (cada día)" : " veces por semana")
                    + " · esta semana " + VidaPlanner.doneThisWeek(log, h.id, LocalDate.now()) + " · racha " + streak + " sem.", 13, MUTED, false);
            card.setOnLongClickListener(v -> {
                new AlertDialog.Builder(this).setTitle("¿Borrar el hábito «" + h.name + "»?")
                        .setMessage("Se borra también su historial de días cumplidos.")
                        .setPositiveButton("Borrar", (d, w) -> change(l -> VidaPlanner.removeHabit(l, h.id)))
                        .setNegativeButton("Cancelar", null).show();
                return true;
            });
        }
        text(this, content, "Toca un objetivo para marcarlo; mantén pulsado para borrar. El gym y dormir 7 h se marcan solos.", 13, MUTED, false);
    }

    private void askGoal() {
        LinearLayout box = column(this);
        EditText title = field("Objetivo", null); box.addView(title);
        EditText area = field("Área (Salud, YouTube, UNED, Zenitcorp…)", null); box.addView(area);
        final String[] due = {""};
        TextView dueView = link(this, "Elegir fecha límite (opcional)", null);
        dueView.setOnClickListener(v -> {
            LocalDate t = LocalDate.now().plusMonths(1);
            new DatePickerDialog(this, (dp, y, m, d) -> {
                due[0] = LocalDate.of(y, m + 1, d).toString();
                dueView.setText("Fecha: " + shortDate(due[0]));
            }, t.getYear(), t.getMonthValue() - 1, t.getDayOfMonth()).show();
        });
        box.addView(dueView);
        new AlertDialog.Builder(this).setTitle("Nuevo objetivo").setView(pad(box))
                .setPositiveButton("Guardar", (d, w) -> {
                    String t = title.getText().toString().trim();
                    if (t.isEmpty()) return;
                    String a = area.getText().toString().trim();
                    String dd = due[0];
                    change(l -> VidaPlanner.addGoal(l, t, dd, a));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    private void askHabit() {
        LinearLayout box = column(this);
        EditText name = field("Hábito (p. ej. Leer 20 min)", null); box.addView(name);
        EditText per = field("Veces por semana (1–7)", "7"); per.setInputType(InputType.TYPE_CLASS_NUMBER); box.addView(per);
        new AlertDialog.Builder(this).setTitle("Nuevo hábito").setView(pad(box))
                .setPositiveButton("Guardar", (d, w) -> {
                    String n = name.getText().toString().trim();
                    if (n.isEmpty()) return;
                    int times;
                    try { times = Integer.parseInt(per.getText().toString().trim()); } catch (NumberFormatException e) { times = 7; }
                    int t = times;
                    change(l -> VidaPlanner.addHabit(l, n, t));
                })
                .setNegativeButton("Cancelar", null).show();
    }

    // ================================================================ utilidades

    private VidaLog.Habit habit(String id) { for (VidaLog.Habit h : log.habits) if (h.id.equals(id)) return h; return null; }

    private static List<VidaLog.Block> blocksWith(List<VidaLog.Block> blocks, String word) {
        List<VidaLog.Block> out = new ArrayList<>();
        for (VidaLog.Block b : blocks) if (b.title.toLowerCase(Locale.ROOT).contains(word)) out.add(b);
        return out;
    }

    private static String endOf(VidaLog.Block b) {
        try { return LocalTime.parse(b.time).plusMinutes(b.minutes).toString(); } catch (RuntimeException e) { return b.time; }
    }

    private static String nowHm() { return LocalTime.now().withSecond(0).withNano(0).toString(); }

    private static String guessKind() {
        int h = LocalTime.now().getHour();
        if (h < 11) return "desayuno";
        if (h < 16) return "comida";
        if (h < 19) return "merienda";
        return "cena";
    }

    private static int sleepColor(int minutes) { return minutes >= 7 * 60 ? GREEN : minutes >= 6 * 60 ? AMBER : RED; }

    private static String dueText(VidaLog.Goal g) {
        if (g.done) return "Completado";
        if (g.due.isEmpty()) return "Sin fecha";
        long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(g.due));
        if (days < 0) return "Venció hace " + (-days) + " días (" + shortDate(g.due) + ")";
        if (days == 0) return "Vence hoy";
        return "Quedan " + days + " días (" + shortDate(g.due) + ")";
    }

    private static int dueColor(VidaLog.Goal g) {
        if (g.done || g.due.isEmpty()) return MUTED;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(g.due));
        return days < 0 ? RED : days <= 14 ? AMBER : MUTED;
    }

    private static String shortDate(String iso) {
        try { return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEE d MMM", new Locale("es"))); }
        catch (RuntimeException e) { return iso; }
    }

    private EditText field(String hint, String value) {
        EditText e = new EditText(this); e.setHint(hint); e.setSingleLine(true);
        if (value != null) e.setText(value);
        return e;
    }

    private View pad(View inner) {
        LinearLayout box = column(this);
        int p = dp(this, 20); box.setPadding(p, dp(this, 8), p, 0);
        box.addView(inner);
        return box;
    }
}
