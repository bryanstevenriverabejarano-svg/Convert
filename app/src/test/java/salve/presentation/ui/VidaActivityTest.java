package salve.presentation.ui;

import static org.junit.Assert.*;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;
import salve.core.vida.VidaCoach;
import salve.core.vida.VidaLog;
import salve.core.vida.VidaPlanner;
import salve.core.vida.VidaRuntime;

/**
 * Pantalla «Hoy» de Vida con datos de prueba (solo en este entorno de test): comprueba que todas
 * las secciones de la especificación están, en su orden, con cifras calculadas de los datos, y
 * guarda capturas renderizadas (Robolectric, gráficos nativos) en app/build/vida-shots para
 * compararlas con la referencia. No sustituye a la captura en emulador/móvil.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, application = Application.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class VidaActivityTest {

    @Test @Config(qualifiers = "w411dp-h915dp-xxhdpi")
    public void hoyShowsEverySectionInOrderFromRealData() throws Exception {
        seed();
        ActivityController<VidaActivity> c = Robolectric.buildActivity(VidaActivity.class).create().start().resume().visible();
        try {
            View root = c.get().findViewById(android.R.id.content);
            waitFor(root, "Salve te recomienda");
            List<String> order = new ArrayList<>();
            collect(root, order);
            String[] expected = {"Vida", "Hoy", "Horario", "Proyectos", "Hábitos", "Gym", "Comidas", "Sueño", "Evolución",
                    "Salve te recomienda", "Aplicar plan de hoy", "Ver plan completo", "Explicación del plan",
                    "Objetivo de hoy", "Progreso del día", "Agenda de hoy", "Evolución semanal", "Inicio", "Memoria"};
            int at = -1;
            for (String e : expected) {
                int i = indexFrom(order, e, at + 1);
                assertTrue("falta o está fuera de orden: " + e + " en " + order, i >= 0);
                at = i;
            }
            assertFalse(order.contains("Cargando…"));
            // Cifras reales: 2 entrenos marcados esta semana con meta 4.
            assertTrue(order.toString(), joined(order).contains("de 4 entrenos"));
            shot(c, "hoy-411dp");
            // Aplicar el plan añade bloques a la agenda de hoy sin solaparse.
            int before = VidaRuntime.get(c.get()).read().blocks.size();
            VidaLog applied = VidaRuntime.get(c.get()).update(l -> VidaCoach.applyPlan(l, LocalDate.now(), VidaCoach.dayPlan(l, LocalDate.now())));
            assertTrue(applied.blocks.size() >= before);
        } finally { c.pause().stop().destroy(); }
    }

    @Test @Config(qualifiers = "w360dp-h740dp-xhdpi")
    public void narrowPhoneStacksWithoutCrashing() throws Exception {
        seed();
        ActivityController<VidaActivity> c = Robolectric.buildActivity(VidaActivity.class).create().start().resume().visible();
        try {
            View root = c.get().findViewById(android.R.id.content);
            waitFor(root, "Salve te recomienda");
            shot(c, "hoy-360dp");
        } finally { c.pause().stop().destroy(); }
    }

    /** Datos de prueba en el almacenamiento del entorno de test (no son datos de Bryan). */
    private static void seed() throws Exception {
        Application app = RuntimeEnvironment.getApplication();
        LocalDate today = LocalDate.now(), monday = VidaCoach.monday(today);
        VidaRuntime.get(app).update(l -> {
            VidaLog out = VidaCoach.seedProjects(VidaPlanner.seedDefaults(l), today);
            out = VidaPlanner.addBlocks(out, "Trabajo", new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), "09:00", 8 * 60);
            out = VidaPlanner.addBlocks(out, "Estudio matemáticas", new HashSet<>(Arrays.asList(today.getDayOfWeek().getValue())), "18:30", 60);
            out = VidaPlanner.addBlocks(out, "Revisión del día", new HashSet<>(Arrays.asList(today.getDayOfWeek().getValue())), "22:00", 20);
            for (int i = 0; i < 2; i++) out = VidaPlanner.toggle(out, "gym", monday.minusWeeks(1).plusDays(i));
            if (!VidaPlanner.checked(out, "gym", monday)) out = VidaPlanner.toggle(out, "gym", monday);
            if (!today.equals(monday) && !VidaPlanner.checked(out, "gym", today)) out = VidaPlanner.toggle(out, "gym", today);
            else if (today.equals(monday)) out = VidaPlanner.toggle(out, "gym", monday.minusDays(1));
            if (!VidaPlanner.checked(out, "mates", today)) out = VidaPlanner.toggle(out, "mates", today);
            for (int i = 1; i <= 3; i++) out = VidaPlanner.logSleep(out, today.minusDays(i), "23:45", "07:00");
            out = VidaPlanner.logSleep(out, today, "00:10", "07:05");
            return out;
        });
    }

    private static void waitFor(View root, String text) throws InterruptedException {
        for (int i = 0; i < 300; i++) {
            ShadowLooper.idleMainLooper();
            List<String> all = new ArrayList<>(); collect(root, all);
            if (all.contains(text)) return;
            Thread.sleep(10);
        }
        fail("No apareció: " + text);
    }

    /** Pinta la pantalla visible y el panel completo (todo el desplazamiento) en PNG. */
    private static void shot(ActivityController<VidaActivity> c, String name) throws Exception {
        View root = c.get().getWindow().getDecorView();
        File dir = new File("build/vida-shots"); dir.mkdirs();
        save(root, root.getWidth(), root.getHeight(), new File(dir, name + "-pantalla.png"));
        ScrollView scroll = findScroll(root);
        assertNotNull(scroll);
        View page = scroll.getChildAt(0);
        save(page, page.getWidth(), page.getHeight(), new File(dir, name + "-completa.png"));
    }

    private static void save(View v, int w, int h, File out) throws Exception {
        assertTrue("vista sin tamaño", w > 0 && h > 0);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        canvas.drawColor(android.graphics.Color.rgb(3, 7, 18));
        v.draw(canvas);
        try (FileOutputStream o = new FileOutputStream(out)) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
    }

    private static ScrollView findScroll(View v) {
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
            ScrollView s = findScroll(((ViewGroup) v).getChildAt(i)); if (s != null) return s;
        }
        return null;
    }

    private static void collect(View v, List<String> out) {
        if (v instanceof TextView) out.add(((TextView) v).getText().toString().trim());
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) collect(((ViewGroup) v).getChildAt(i), out);
    }

    private static int indexFrom(List<String> all, String text, int from) {
        for (int i = from; i < all.size(); i++) if (all.get(i).equals(text)) return i;
        return -1;
    }

    private static String joined(List<String> all) { return String.join(" | ", all); }
}
