package salve.presentation.ui;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Build;
import android.widget.ScrollView;
import androidx.test.core.app.ActivityScenario;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import org.junit.Test;
import salve.core.vida.VidaCoach;
import salve.core.vida.VidaLog;
import salve.core.vida.VidaPlanner;
import salve.core.vida.VidaRuntime;

/**
 * Captura real de la pantalla «Hoy» de Vida en un emulador o móvil (opt-in). Siembra datos de
 * prueba en la instalación del emulador, abre VidaActivity y guarda capturas de la pantalla
 * (arriba, medio y abajo) en files/vida-shots de la app para descargarlas con run-as.
 *
 * ./gradlew :app:connectedDebugAndroidTest \
 *   -Pandroid.testInstrumentationRunnerArguments.vidaShots=true \
 *   -Pandroid.testInstrumentationRunnerArguments.class=salve.presentation.ui.VidaScreenshotDeviceTest
 */
public final class VidaScreenshotDeviceTest {
    @Test public void capturesHoy() throws Exception {
        assumeTrue("Capturas de Vida solo bajo petición",
                "true".equals(InstrumentationRegistry.getArguments().getString("vidaShots")));
        Instrumentation ins = InstrumentationRegistry.getInstrumentation();
        Context target = ins.getTargetContext();
        if (Build.VERSION.SDK_INT >= 33)
            ins.getUiAutomation().grantRuntimePermission(target.getPackageName(), android.Manifest.permission.POST_NOTIFICATIONS);
        LocalDate today = LocalDate.now(), monday = VidaCoach.monday(today);
        int dow = today.getDayOfWeek().getValue();
        VidaRuntime.get(target).update(l -> {
            VidaLog out = VidaCoach.seedProjects(VidaPlanner.seedDefaults(l), today);
            if (out.blocks.isEmpty()) {
                out = VidaPlanner.addBlocks(out, "Trabajo", new HashSet<>(Arrays.asList(1, 2, 3, 4, 5)), "09:00", 8 * 60);
                out = VidaPlanner.addBlocks(out, "Estudio matemáticas", new HashSet<>(Arrays.asList(dow)), "18:30", 60);
                out = VidaPlanner.addBlocks(out, "Revisión del día", new HashSet<>(Arrays.asList(dow)), "22:00", 20);
                for (int i = 0; i < 2; i++) out = VidaPlanner.toggle(out, "gym", monday.minusWeeks(1).plusDays(i));
                out = VidaPlanner.toggle(out, "gym", today);
                out = VidaPlanner.toggle(out, "mates", today);
                for (int i = 0; i <= 3; i++) out = VidaPlanner.logSleep(out, today.minusDays(i), "23:45", "07:00");
            }
            return out;
        });
        File dir = new File(target.getFilesDir(), "vida-shots"); dir.mkdirs();
        String size = target.getResources().getConfiguration().screenWidthDp + "dp";
        try (ActivityScenario<VidaActivity> scenario = ActivityScenario.launch(VidaActivity.class)) {
            Thread.sleep(2500);
            ins.waitForIdleSync();
            save(ins, new File(dir, "hoy-" + size + "-1.png"));
            for (int part = 2; part <= 4; part++) {
                scenario.onActivity(a -> {
                    ScrollView s = find(a.getWindow().getDecorView());
                    if (s != null) s.scrollBy(0, Math.round(s.getHeight() * 0.85f));
                });
                ins.waitForIdleSync(); Thread.sleep(600);
                save(ins, new File(dir, "hoy-" + size + "-" + part + ".png"));
            }
        }
    }

    private static void save(Instrumentation ins, File out) throws Exception {
        Bitmap b = ins.getUiAutomation().takeScreenshot();
        assertNotNull("sin captura", b);
        try (FileOutputStream o = new FileOutputStream(out)) { b.compress(Bitmap.CompressFormat.PNG, 100, o); }
    }

    private static ScrollView find(android.view.View v) {
        if (v instanceof ScrollView) return (ScrollView) v;
        if (v instanceof android.view.ViewGroup) for (int i = 0; i < ((android.view.ViewGroup) v).getChildCount(); i++) {
            ScrollView s = find(((android.view.ViewGroup) v).getChildAt(i)); if (s != null) return s;
        }
        return null;
    }
}
