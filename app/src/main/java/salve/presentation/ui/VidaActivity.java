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
import salve.core.vida.VidaPlanner;
import salve.core.vida.VidaRuntime;
import salve.core.vida.VidaService;

/**
 * Sección Vida de Salve: Hoy, Gym, Comidas, Sueño, Horario y Objetivos. Todo se guarda en el
 * mismo registro local de Vida (con copia en pCloud si está conectada) y el chat lo ve igual.
 */
public final class VidaActivity extends Activity {
    private static final String[] TABS = {"Hoy", "Gym", "Comidas", "Sueño", "Horario", "Objetivos"};

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
        LinearLayout root = column(this); root.setBackgroundColor(BG);
        LinearLayout head = column(this);
        head.setPadding(dp(this, 16), dp(this, 28), dp(this, 16), dp(this, 4));
        text(this, head, "Vida", 30, Color.WHITE, true);
        text(this, head, LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", new Locale("es"))), 15, MUTED, false);
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
        change(l -> VidaPlanner.syncSleepHabit(VidaPlanner.syncGymHabit(VidaPlanner.seedDefaults(l))));
    }

    @Override protected void onResume() { super.onResume(); if (vida != null) change(VidaPlanner::syncGymHabit); }
    @Override protected void onSaveInstanceState(Bundle out) { out.putString("tab", tab); super.onSaveInstanceState(out); }
    @Override protected void onDestroy() { io.shutdown(); super.onDestroy(); }

    /** Aplica un cambio fuera del hilo de la pantalla y repinta la pestaña. */
    private void change(UnaryOperator<VidaLog> c) {
        io.execute(() -> {
            String error = null;
            try { log = vida.update(c); }
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
        for (String t : TABS) {
            TextView c = chip(this, t, t.equals(tab));
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
            default: today();
        }
    }

    // ================================================================ Hoy

    private void today() {
        LocalDate today = LocalDate.now();
        int dow = today.getDayOfWeek().getValue();

        section(this, content, "Hábitos de hoy");
        LinearLayout habits = card(this, content);
        if (log.habits.isEmpty()) text(this, habits, "Sin hábitos. Añádelos en Objetivos.", 15, MUTED, false);
        for (VidaLog.Habit h : log.habits) habitRow(habits, h, today);

        section(this, content, "Tu día");
        LinearLayout day = card(this, content);
        List<VidaLog.Block> blocks = new ArrayList<>();
        for (VidaLog.Block b : log.blocks) if (b.day == dow) blocks.add(b);
        blocks.sort(Comparator.comparing(b -> b.time));
        if (blocks.isEmpty()) text(this, day, "Nada en el horario de hoy.", 15, MUTED, false);
        String now = LocalTime.now().withSecond(0).withNano(0).toString();
        for (VidaLog.Block b : blocks) {
            boolean past = endOf(b).compareTo(now) <= 0;
            text(this, day, b.time + "  " + b.title + " · " + b.minutes + " min", 16, past ? MUTED : Color.WHITE, !past);
        }
        day.addView(link(this, "Editar horario", v -> { tab = "Horario"; paintTabs(); render(); }));

        section(this, content, "Comidas de hoy");
        LinearLayout food = card(this, content);
        List<VidaLog.Meal> eaten = VidaPlanner.mealsOn(log, today);
        for (String kind : VidaPlanner.MEAL_KINDS) {
            String planned = VidaPlanner.planned(log, dow, kind);
            VidaLog.Meal ate = null; for (VidaLog.Meal m : eaten) if (m.kind.equals(kind)) ate = m;
            if (planned == null && ate == null) continue;
            LinearLayout r = row(this);
            TextView label = text(this, null, VidaPlanner.cap(kind) + ": " + (ate != null ? ate.text : planned), 16, ate != null ? GREEN : Color.WHITE, false);
            r.addView(label, new LinearLayout.LayoutParams(0, -2, 1));
            if (ate == null) {
                String plan = planned;
                r.addView(link(this, "✓ Comido", v -> change(l -> VidaPlanner.logMeal(l, today, nowHm(), kind, plan))));
            } else r.addView(text(this, null, "✔", 18, GREEN, true));
            food.addView(r);
        }
        if (food.getChildCount() == 0) text(this, food, "No hay comidas planificadas para hoy.", 15, MUTED, false);
        food.addView(link(this, "+ Anotar otra comida", v -> askMeal(today)));

        section(this, content, "Sueño");
        LinearLayout night = card(this, content);
        VidaLog.SleepEntry last = null; for (VidaLog.SleepEntry e : log.sleep) if (e.date.equals(today.toString())) last = e;
        if (last != null) text(this, night, "Anoche: " + hours(last.minutes) + " (" + last.bed + " → " + last.wake + ")", 16, sleepColor(last.minutes), true);
        else night.addView(link(this, "Anotar cómo dormí anoche", v -> askSleep(today)));
        int avg = VidaPlanner.averageMinutes(VidaPlanner.lastNights(log, 7));
        if (avg > 0) text(this, night, "Media de tus últimas noches: " + hours(avg), 14, MUTED, false);

        section(this, content, "Gym");
        LinearLayout gym = card(this, content);
        VidaLog.Habit gh = habit("gym");
        int weekGym = VidaPlanner.doneThisWeek(log, "gym", today);
        text(this, gym, weekGym + (gh != null ? " de " + gh.perWeek : "") + " entrenos esta semana", 16, Color.WHITE, true);
        if (last != null && last.minutes < 6 * 60 && !blocksWith(blocks, "gym").isEmpty())
            text(this, gym, "Dormiste poco y hoy tienes gym: baja un poco el peso o el volumen.", 14, AMBER, false);
        Button go = button(this, "Abrir gym", BLUE);
        go.setOnClickListener(v -> startActivity(new Intent(this, GymActivity.class)));
        gym.addView(go, wide(this, 6));

        VidaLog.Goal next = null;
        for (VidaLog.Goal g : VidaPlanner.sortedGoals(log)) if (!g.done) { next = g; break; }
        if (next != null) {
            section(this, content, "Próximo objetivo");
            LinearLayout goal = card(this, content);
            text(this, goal, next.title, 17, Color.WHITE, true);
            text(this, goal, dueText(next), 14, dueColor(next), false);
        }
    }

    private void habitRow(LinearLayout parent, VidaLog.Habit h, LocalDate today) {
        boolean done = VidaPlanner.checked(log, h.id, today);
        LinearLayout r = row(this); r.setPadding(0, dp(this, 4), 0, dp(this, 4));
        TextView box = text(this, null, done ? "✔" : "○", 22, done ? GREEN : MUTED, true);
        box.setPadding(0, 0, dp(this, 12), 0);
        LinearLayout texts = column(this);
        text(this, texts, h.name, 16, done ? GREEN : Color.WHITE, true);
        int week = VidaPlanner.doneThisWeek(log, h.id, today);
        int streak = VidaPlanner.weekStreak(log, h, today);
        text(this, texts, week + "/" + h.perWeek + " esta semana" + (streak > 0 ? " · racha " + streak + (streak == 1 ? " semana" : " semanas") : ""), 13, MUTED, false);
        r.addView(box); r.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        r.setOnClickListener(v -> change(l -> VidaPlanner.toggle(l, h.id, today)));
        parent.addView(r);
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

    private void schedule() {
        section(this, content, "Horario semanal");
        Button add = button(this, "+ Añadir bloque", BLUE);
        add.setOnClickListener(v -> askBlock());
        content.addView(add, wide(this, 4));
        for (int d = 1; d <= 7; d++) {
            List<VidaLog.Block> blocks = new ArrayList<>();
            for (VidaLog.Block b : log.blocks) if (b.day == d) blocks.add(b);
            blocks.sort(Comparator.comparing(b -> b.time));
            LinearLayout card = card(this, content);
            text(this, card, VidaPlanner.dayName(d), 17, d == LocalDate.now().getDayOfWeek().getValue() ? BLUE : Color.WHITE, true);
            if (blocks.isEmpty()) text(this, card, "Libre", 14, MUTED, false);
            for (VidaLog.Block b : blocks) {
                TextView t = text(this, card, b.time + "–" + endOf(b) + "  " + b.title, 15, LIGHT, false);
                t.setOnLongClickListener(v -> {
                    new AlertDialog.Builder(this).setTitle(b.title + " · " + VidaPlanner.dayName(b.day))
                            .setItems(new String[]{"Quitar este día", "Quitar todos los días"}, (dlg, which) -> change(l -> {
                                List<VidaLog.Block> next = new ArrayList<>();
                                for (VidaLog.Block o : l.blocks) {
                                    boolean same = o.title.equalsIgnoreCase(b.title) && (which == 1 || (o.day == b.day && o.time.equals(b.time)));
                                    if (!same) next.add(o);
                                }
                                return l.withBlocks(next);
                            })).show();
                    return true;
                });
            }
        }
        text(this, content, "Mantén pulsado un bloque para quitarlo. Ideas del plan: Gym, Prioridad del trimestre (2 h), Matemáticas (1 h), Revisión semanal (domingo, 20 min).", 13, MUTED, false);
    }

    private void askBlock() {
        LinearLayout box = column(this);
        EditText title = field("Título (p. ej. Gym, Matemáticas)", null);
        box.addView(title);
        LinearLayout days = row(this);
        List<CheckBox> checks = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            CheckBox c = new CheckBox(this); c.setText(VidaPlanner.dayName(d).substring(0, 1)); c.setTextColor(Color.WHITE);
            checks.add(c); days.addView(c);
        }
        HorizontalScrollView hs = new HorizontalScrollView(this); hs.addView(days); box.addView(hs);
        EditText time = field("Hora (HH:mm)", "19:00");
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
                    List<Integer> chosen = new ArrayList<>();
                    for (int i = 0; i < 7; i++) if (checks.get(i).isChecked()) chosen.add(i + 1);
                    if (chosen.isEmpty()) { Toast.makeText(this, "Elige al menos un día.", Toast.LENGTH_LONG).show(); return; }
                    int dur = Math.max(5, Math.min(16 * 60, mins));
                    change(l -> {
                        List<VidaLog.Block> next = new ArrayList<>(l.blocks);
                        for (int day : chosen) next.add(new VidaLog.Block(t, day, clean, dur));
                        return l.withBlocks(next);
                    });
                })
                .setNegativeButton("Cancelar", null).show();
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
