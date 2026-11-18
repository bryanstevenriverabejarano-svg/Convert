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
    private static final String[] TABS = {"Hoy", "Gym", "Horario", "Proyectos", "Comidas", "Sueño", "Objetivos", "Semana"};

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
        LinearLayout head = row(this);
        head.setPadding(dp(this, 16), dp(this, 30), dp(this, 16), dp(this, 6));
        TextView logo = text(this, null, "💙", 26, Color.WHITE, false);
        logo.setGravity(Gravity.CENTER); logo.setBackground(neon(this, true, 16));
        head.addView(logo, new LinearLayout.LayoutParams(dp(this, 58), dp(this, 58)));
        LinearLayout titles = column(this); titles.setPadding(dp(this, 12), 0, 0, 0);
        TextView big = text(this, titles, "Vida", 38, Color.WHITE, true);
        big.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD_ITALIC));
        big.setShadowLayer(dp(this, 8), 0, 0, NEON);
        text(this, titles, "Tu organización, bienestar y progreso.", 14, MUTED, false);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));
        root.addView(head);
        HorizontalScrollView bar = new HorizontalScrollView(this); bar.setHorizontalScrollBarEnabled(false);
        tabs = row(this); tabs.setPadding(dp(this, 12), dp(this, 6), dp(this, 12), dp(this, 6));
        bar.addView(tabs); root.addView(bar);
        ScrollView scroll = new ScrollView(this);
        content = page(this);
        scroll.addView(content);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);
        paintTabs();
        text(this, content, "Cargando…", 16, MUTED, false);
        change(l -> VidaCoach.seedProjects(VidaPlanner.syncSleepHabit(VidaPlanner.syncGymHabit(VidaPlanner.seedDefaults(l))), LocalDate.now()));
        if (android.os.Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 71);
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
        String[] icons = {"🏠", "🏋", "📅", "🗂", "🍽", "🌙", "🎯", "📊"};
        for (int i = 0; i < TABS.length; i++) {
            String t = TABS[i];
            boolean sel = t.equals(tab);
            LinearLayout c = column(this);
            c.setGravity(Gravity.CENTER);
            c.setPadding(dp(this, 14), dp(this, 8), dp(this, 14), dp(this, 8));
            c.setBackground(neon(this, sel, 14));
            TextView ic = text(this, c, icons[i], 18, Color.WHITE, false); ic.setGravity(Gravity.CENTER);
            if (sel) ic.setShadowLayer(dp(this, 6), 0, 0, NEON);
            TextView lb = text(this, c, t, 13, sel ? Color.WHITE : LIGHT, sel); lb.setGravity(Gravity.CENTER);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(dp(this, 4), 0, dp(this, 4), 0);
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
            case "Objetivos": goals(); break;
            case "Proyectos": projects(); break;
            case "Semana": week(); break;
            default: today();
        }
    }

    // ================================================================ Hoy

    private LocalDate viewDate = LocalDate.now();

    private static final String[] QUOTES = {
            "Disciplina hoy, libertad mañana.", "Termina una cosa. Luego la siguiente.", "No hace falta motivación, hace falta el primer paso.",
            "Lo que repites cada día es lo que te convierte.", "Hecho es mejor que perfecto.", "Tu yo futuro te está mirando.",
            "Un día ganado cada vez.", "La constancia gana al talento que no se presenta."};

    private void today() {
        LocalDate today = viewDate;
        boolean isToday = today.equals(LocalDate.now());
        int dow = today.getDayOfWeek().getValue();

        // Fecha con flechas y frase del día
        LinearLayout nav = row(this);
        nav.setBackground(neon(this, false, 12)); nav.setPadding(dp(this, 6), dp(this, 2), dp(this, 6), dp(this, 2));
        nav.addView(link(this, "‹", v -> { viewDate = viewDate.minusDays(1); render(); }));
        TextView date = text(this, null, "📅  " + VidaPlanner.cap(today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", new Locale("es")))) + (isToday ? "" : "  ·  volver a hoy"), 16, Color.WHITE, false);
        date.setGravity(Gravity.CENTER);
        date.setOnClickListener(v -> { viewDate = LocalDate.now(); render(); });
        nav.addView(date, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(link(this, "›", v -> { viewDate = viewDate.plusDays(1); render(); }));
        content.addView(nav, wide(this, 2));
        TextView quote = text(this, content, "//  “" + QUOTES[(int) (today.toEpochDay() % QUOTES.length)] + "”", 16, Color.WHITE, false);
        quote.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC));
        quote.setPadding(dp(this, 10), dp(this, 8), 0, dp(this, 2));

        // Datos del día
        List<VidaLog.Block> blocks = new ArrayList<>();
        for (VidaLog.Block b : log.blocks) if (b.day == dow) blocks.add(b);
        blocks.sort(Comparator.comparing(b -> b.time));
        List<VidaLog.Block> agenda = new ArrayList<>();
        for (VidaLog.Block b : blocks) if (VidaPlanner.kind(b.title) != VidaPlanner.Kind.DESCANSO || b.title.toLowerCase(Locale.ROOT).contains("desconectar")) agenda.add(b);
        int blocksDone = 0; for (VidaLog.Block b : agenda) if (VidaPlanner.blockDone(log, b, today)) blocksDone++;
        VidaLog.Focus f = VidaCoach.focusOn(log, today);
        int dailyHabits = 0, dailyDone = 0;
        for (VidaLog.Habit h : log.habits) { dailyHabits++; if (VidaPlanner.checked(log, h.id, today)) dailyDone++; }
        int tasks = agenda.size() + dailyHabits + (f == null ? 0 : 1);
        int tasksDone = blocksDone + dailyDone + (f != null && f.done ? 1 : 0);
        List<VidaLog.Meal> eaten = VidaPlanner.mealsOn(log, today);
        int planned = 0; for (String k : VidaPlanner.MEAL_KINDS) if (VidaPlanner.planned(log, dow, k) != null) planned++;
        int mealsGoal = Math.max(3, planned);
        VidaLog.SleepEntry night = null; for (VidaLog.SleepEntry e : log.sleep) if (e.date.equals(today.toString())) night = e;
        boolean gymToday = false; for (VidaLog.GymSet s : log.sets) if (s.date.equals(today.toString())) { gymToday = true; break; }
        boolean gymPlanned = false; for (VidaLog.Block b : blocks) if (VidaPlanner.kind(b.title) == VidaPlanner.Kind.GYM) gymPlanned = true;

        // Fila: progreso diario | resumen
        LinearLayout top = row(this); top.setGravity(Gravity.TOP);
        LinearLayout progress = tile();
        LinearLayout pr = row(this);
        VidaWidgets.Ring ring = new VidaWidgets.Ring(this, BLUE);
        int pct = tasks == 0 ? 0 : Math.round(100f * tasksDone / tasks);
        ring.set(tasks == 0 ? 0 : (float) tasksDone / tasks, pct + "%");
        pr.addView(ring, new LinearLayout.LayoutParams(dp(this, 74), dp(this, 74)));
        LinearLayout pt = column(this); pt.setPadding(dp(this, 10), 0, 0, 0);
        text(this, pt, "Progreso diario", 16, Color.WHITE, true);
        text(this, pt, tasksDone + " de " + tasks + " tareas", 13, MUTED, false);
        pr.addView(pt, new LinearLayout.LayoutParams(0, -2, 1));
        progress.addView(pr);
        LinearLayout summary = tile();
        text(this, summary, "Resumen de hoy", 16, Color.WHITE, true);
        LinearLayout g1 = row(this), g2 = row(this);
        g1.addView(stat("🍽", eaten.size() + "/" + mealsGoal), weight(this, 1, 2));
        g1.addView(stat("🏋", (gymToday ? 1 : 0) + "/" + (gymPlanned || gymToday ? 1 : 0)), weight(this, 1, 2));
        g2.addView(stat("🌙", night == null ? "—" : String.format(Locale.ROOT, "%.1f h", night.minutes / 60.0)), weight(this, 1, 2));
        g2.addView(stat("☑", blocksDone + "/" + agenda.size()), weight(this, 1, 2));
        summary.addView(g1, wide(this, 6)); summary.addView(g2, wide(this, 6));
        top.addView(progress, weight(this, 1, 4)); top.addView(summary, weight(this, 1, 4));
        content.addView(top, wide(this, 10));

        // Lo más importante hoy
        LinearLayout focus = card(this, content);
        focus.addView(header("🎯", "Lo más importante hoy"));
        if (f == null) {
            text(this, focus, "Elige UNA cosa. Si la terminas, el día está ganado.", 14, MUTED, false);
            Button pick = button(this, "Elegir mi tarea de hoy   »", BLUE);
            pick.setOnClickListener(v -> askFocus(today));
            focus.addView(pick, wide(this, 6));
        } else {
            LinearLayout r = row(this);
            TextView box = text(this, null, f.done ? "✔" : "○", 26, f.done ? GREEN : Color.WHITE, true);
            box.setPadding(0, 0, dp(this, 12), 0);
            r.addView(box);
            r.addView(text(this, null, f.text, 18, f.done ? GREEN : Color.WHITE, true), new LinearLayout.LayoutParams(0, -2, 1));
            r.addView(link(this, "Cambiar", v -> askFocus(today)));
            r.setOnClickListener(v -> change(l -> VidaCoach.toggleFocus(l, today)));
            focus.addView(r, wide(this, 4));
            if (f.done) text(this, focus, "¡Día ganado! 🏆", 15, GREEN, true);
        }
        LinearLayout dots = row(this);
        LocalDate monday = VidaCoach.monday(today);
        int wins = VidaCoach.focusWinsThisWeek(log, today);
        TextView winsText = text(this, null, wins + " días ganados esta semana", 13, MUTED, false);
        dots.addView(winsText, new LinearLayout.LayoutParams(0, -2, 1));
        for (int i = 0; i < 7; i++) {
            VidaLog.Focus df = VidaCoach.focusOn(log, monday.plusDays(i));
            View dot = new View(this);
            android.graphics.drawable.GradientDrawable o = new android.graphics.drawable.GradientDrawable();
            o.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            o.setColor(df != null && df.done ? NEON : Color.TRANSPARENT);
            o.setStroke(dp(this, 2), monday.plusDays(i).equals(today) ? NEON : STROKE);
            dot.setBackground(o);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(this, 12), dp(this, 12)); lp.setMargins(dp(this, 3), 0, dp(this, 3), 0);
            dots.addView(dot, lp);
        }
        focus.addView(dots, wide(this, 8));
        String priority = VidaCoach.currentPriority(log, today);
        if (priority != null) text(this, focus, "Prioridad de la semana: " + priority, 13, AMBER, false);
        if (dow == 7 && VidaCoach.reviewFor(log, monday) == null)
            focus.addView(link(this, "Hoy toca la revisión semanal →", v -> { tab = "Semana"; paintTabs(); render(); }));

        // Agenda en línea de tiempo
        LinearLayout ag = card(this, content);
        LinearLayout agh = header("📅", "Agenda de hoy");
        agh.addView(link(this, "+", v -> askBlock(dow, null)));
        ag.addView(agh);
        if (agenda.isEmpty()) {
            text(this, ag, "Nada planificado. Usa «Proponer mi semana» en Horario.", 14, MUTED, false);
            ag.addView(link(this, "Ir a Horario →", v -> { tab = "Horario"; paintTabs(); render(); }));
        }
        String now = LocalTime.now().withSecond(0).withNano(0).toString();
        for (int i = 0; i < agenda.size(); i++) {
            VidaLog.Block b = agenda.get(i);
            boolean done = VidaPlanner.blockDone(log, b, today);
            boolean current = isToday && b.time.compareTo(now) <= 0 && endOf(b).compareTo(now) > 0;
            LinearLayout r = row(this); r.setPadding(0, dp(this, 6), 0, dp(this, 6));
            TextView time = text(this, null, b.time, 15, current ? BLUE : Color.WHITE, current);
            r.addView(time, new LinearLayout.LayoutParams(dp(this, 52), -2));
            View dotLine = new View(this);
            android.graphics.drawable.GradientDrawable o = new android.graphics.drawable.GradientDrawable();
            o.setShape(android.graphics.drawable.GradientDrawable.OVAL); o.setColor(done ? GREEN : BLUE); o.setStroke(dp(this, 3), Color.rgb(13, 40, 80));
            dotLine.setBackground(o);
            LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(this, 14), dp(this, 14)); dl.setMargins(0, 0, dp(this, 10), 0);
            r.addView(dotLine, dl);
            r.addView(text(this, null, emojiFor(VidaPlanner.kind(b.title)), 18, Color.WHITE, false));
            LinearLayout tt = column(this); tt.setPadding(dp(this, 10), 0, 0, 0);
            text(this, tt, b.title, 15, done ? MUTED : Color.WHITE, true);
            text(this, tt, b.time + " – " + endOf(b) + (current ? "  · ahora" : ""), 12, MUTED, false);
            r.addView(tt, new LinearLayout.LayoutParams(0, -2, 1));
            TextView check = text(this, null, done ? "☑" : "☐", 24, done ? BLUE : MUTED, false);
            check.setPadding(dp(this, 8), 0, dp(this, 4), 0);
            r.addView(check);
            r.setOnClickListener(v -> change(l -> VidaPlanner.toggleBlock(l, b, today)));
            ag.addView(r);
            if (i < agenda.size() - 1) { View sep = new View(this); sep.setBackgroundColor(STROKE); ag.addView(sep, new LinearLayout.LayoutParams(-1, 1)); }
        }

        // Comidas | Sueño
        LinearLayout pair = row(this); pair.setGravity(Gravity.TOP);
        LinearLayout meals = tile();
        meals.addView(header("🍽", "Comidas"));
        text(this, meals, eaten.size() + " de " + mealsGoal + " comidas", 13, MUTED, false);
        VidaWidgets.Line ml = new VidaWidgets.Line(this, BLUE); ml.set((float) eaten.size() / mealsGoal);
        meals.addView(ml, lineParams());
        String nextMeal = null;
        for (String k : VidaPlanner.MEAL_KINDS) {
            boolean ate = false; for (VidaLog.Meal m : eaten) if (m.kind.equals(k)) ate = true;
            String p = VidaPlanner.planned(log, dow, k);
            if (!ate && p != null) { nextMeal = VidaPlanner.cap(k) + ": " + p; break; }
        }
        if (nextMeal != null) text(this, meals, nextMeal, 12, LIGHT, false).setMaxLines(2);
        meals.setOnClickListener(v -> { tab = "Comidas"; paintTabs(); render(); });
        LinearLayout sleep = tile();
        sleep.addView(header("🌙", "Sueño"));
        text(this, sleep, "Anoche: " + (night == null ? "—" : hours(night.minutes)), 13, night == null ? MUTED : sleepColor(night.minutes), night != null);
        text(this, sleep, "Objetivo: 7,5 – 8 h", 12, MUTED, false);
        VidaWidgets.Line sl = new VidaWidgets.Line(this, night == null ? BLUE : sleepColor(night.minutes)); sl.set(night == null ? 0 : night.minutes / 480f);
        sleep.addView(sl, lineParams());
        if (night == null && isToday) sleep.addView(link(this, "Anotar", v -> askSleep(today)));
        sleep.setOnClickListener(v -> { tab = "Sueño"; paintTabs(); render(); });
        pair.addView(meals, weight(this, 1, 4)); pair.addView(sleep, weight(this, 1, 4));
        content.addView(pair, wide(this, 10));

        // Gym
        LinearLayout gym = card(this, content);
        LinearLayout gh = header("🏋", "Gym");
        gym.addView(gh);
        VidaLog.Habit gymHabit = habit("gym");
        int weekGym = VidaPlanner.doneThisWeek(log, "gym", today);
        text(this, gym, weekGym + " de " + (gymHabit == null ? 4 : gymHabit.perWeek) + " entrenos esta semana", 14, MUTED, false);
        LinearLayout days = row(this);
        String[] letters = {"L", "M", "X", "J", "V", "S", "D"};
        for (int i = 0; i < 7; i++) {
            LocalDate d = monday.plusDays(i);
            boolean trained = VidaPlanner.checked(log, "gym", d);
            TextView c = text(this, null, letters[i], 14, Color.WHITE, d.equals(today));
            c.setGravity(Gravity.CENTER);
            c.setBackground(trained ? rounded(this, GREEN, 8) : neon(this, d.equals(today), 8));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(this, 34), 1); lp.setMargins(dp(this, 3), 0, dp(this, 3), 0);
            days.addView(c, lp);
        }
        gym.addView(days, wide(this, 6));
        if (night != null && night.minutes < 6 * 60 && gymPlanned)
            text(this, gym, "Dormiste poco y hoy toca gym: baja un poco el peso o el volumen.", 13, AMBER, false);
        gym.setOnClickListener(v -> startActivity(new Intent(this, GymActivity.class)));

        // Objetivo principal
        VidaLog.Goal main = null;
        for (VidaLog.Goal g : VidaPlanner.sortedGoals(log)) if (!g.done) { main = g; break; }
        if (main != null) {
            LinearLayout goal = card(this, content);
            LinearLayout gr = row(this);
            LinearLayout gt = column(this);
            gt.addView(header("🎯", "Objetivo principal"));
            text(this, gt, main.title, 16, Color.WHITE, true);
            text(this, gt, "📆 " + dueText(main), 13, dueColor(main) == MUTED ? AMBER : dueColor(main), false);
            gr.addView(gt, new LinearLayout.LayoutParams(0, -2, 1));
            float gp = goalProgress(main);
            VidaWidgets.Ring gring = new VidaWidgets.Ring(this, Color.rgb(171, 71, 188));
            gring.set(gp, Math.round(gp * 100) + "%");
            gr.addView(gring, new LinearLayout.LayoutParams(dp(this, 70), dp(this, 70)));
            goal.addView(gr);
            goal.setOnClickListener(v -> { tab = "Objetivos"; paintTabs(); render(); });
        }

        // Hábitos en fichas
        LinearLayout hb = card(this, content);
        LinearLayout hh = header("✅", "Hábitos de hoy");
        hh.addView(link(this, "Ver todos ›", v -> { tab = "Objetivos"; paintTabs(); render(); }));
        hb.addView(hh);
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.setHorizontalScrollBarEnabled(false);
        LinearLayout tiles = row(this);
        for (VidaLog.Habit h : log.habits) tiles.addView(habitTile(h, today), habitParams());
        hs.addView(tiles); hb.addView(hs, wide(this, 6));

        // Evolución semanal
        LinearLayout evo = card(this, content);
        LinearLayout er = row(this);
        LinearLayout et = column(this);
        et.addView(header("📊", "Evolución semanal"));
        text(this, et, "Hábitos · Entrenos · Sueño · Proyectos", 13, MUTED, false);
        er.addView(et, new LinearLayout.LayoutParams(0, -2, 1));
        VidaWidgets.Bars bars = new VidaWidgets.Bars(this);
        float[] weeks = new float[6];
        for (int i = 0; i < 6; i++) {
            LocalDate w = today.minusWeeks(5 - i);
            int sum = 0; for (VidaLog.Habit h : log.habits) sum += Math.min(h.perWeek, VidaPlanner.doneThisWeek(log, h.id, w));
            weeks[i] = sum;
        }
        bars.set(weeks);
        er.addView(bars, new LinearLayout.LayoutParams(dp(this, 90), dp(this, 44)));
        evo.addView(er);
        evo.setOnClickListener(v -> { tab = "Semana"; paintTabs(); render(); });
    }

    private float goalProgress(VidaLog.Goal g) {
        // Si un proyecto activo comparte palabras con el objetivo, su avance cuenta como el del objetivo.
        String t = VidaPlanner.cap(g.title).toLowerCase(Locale.ROOT);
        for (VidaLog.Project j : log.projects) {
            String jt = j.title.toLowerCase(Locale.ROOT).replaceAll("[«»\"]", "");
            for (String word : jt.split("\\s+"))
                if (word.length() > 4 && t.contains(word)) { int[] p = VidaCoach.progress(j); return p[1] == 0 ? 0 : (float) p[0] / p[1]; }
        }
        return g.done ? 1 : 0;
    }

    private LinearLayout tile() {
        LinearLayout t = column(this);
        int p = dp(this, 14); t.setPadding(p, p, p, p);
        t.setBackground(glass(this));
        return t;
    }

    private LinearLayout header(String emoji, String title) {
        LinearLayout r = row(this);
        TextView i = text(this, null, emoji, 17, Color.WHITE, false);
        i.setPadding(0, 0, dp(this, 8), 0);
        r.addView(i);
        TextView t = text(this, null, title, 18, Color.WHITE, true);
        t.setTypeface(android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT_BOLD, android.graphics.Typeface.BOLD_ITALIC));
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        return r;
    }

    private View stat(String emoji, String value) {
        TextView t = text(this, null, emoji + "  " + value, 14, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(this, 8), 0, dp(this, 8));
        t.setBackground(neon(this, false, 10));
        return t;
    }

    private View habitTile(VidaLog.Habit h, LocalDate day) {
        boolean done = VidaPlanner.checked(log, h.id, day);
        int week = VidaPlanner.doneThisWeek(log, h.id, day);
        LinearLayout t = column(this);
        t.setGravity(Gravity.CENTER_HORIZONTAL);
        int p = dp(this, 10); t.setPadding(p, p, p, p);
        t.setBackground(neon(this, done, 14));
        TextView i = text(this, t, habitEmoji(h), 22, Color.WHITE, false); i.setGravity(Gravity.CENTER);
        TextView n = text(this, t, h.name, 13, Color.WHITE, true); n.setGravity(Gravity.CENTER); n.setMaxLines(2);
        TextView s = text(this, t, done ? "✔ Hoy" : week + "/" + h.perWeek + " sem", 12, done ? NEON : MUTED, done); s.setGravity(Gravity.CENTER);
        VidaWidgets.Line line = new VidaWidgets.Line(this, NEON); line.set((float) week / h.perWeek);
        t.addView(line, lineParams());
        t.setOnClickListener(v -> change(l -> VidaPlanner.toggle(l, h.id, day)));
        return t;
    }

    private LinearLayout.LayoutParams habitParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(this, 112), dp(this, 140));
        lp.setMargins(0, 0, dp(this, 8), 0);
        return lp;
    }

    private LinearLayout.LayoutParams lineParams() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(this, 6));
        lp.topMargin = dp(this, 8);
        return lp;
    }

    private static String emojiFor(VidaPlanner.Kind k) {
        switch (k) {
            case GYM: return "🏋"; case TRABAJO: return "💼"; case ESTUDIO: return "📚"; case PROYECTO: return "💻";
            case CREAR: return "🎬"; case DESCANSO: return "🌙"; case REVISION: return "🧭"; default: return "•";
        }
    }

    private static String habitEmoji(VidaLog.Habit h) {
        String n = h.name.toLowerCase(Locale.ROOT);
        if (h.id.equals("gym") || n.contains("gym")) return "🏋";
        if (n.contains("mate")) return "📐";
        if (h.id.equals("sueno") || n.contains("dorm")) return "🌙";
        if (n.contains("agua")) return "💧";
        if (n.contains("creatina") || n.contains("suplemento")) return "💊";
        if (n.contains("historia") || n.contains("escrib")) return "✍";
        if (n.contains("revis")) return "🧭";
        if (n.contains("prioridad") || n.contains("proyecto")) return "🚀";
        if (n.contains("leer")) return "📖";
        return "⭐";
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
