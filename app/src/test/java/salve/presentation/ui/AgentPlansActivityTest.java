package salve.presentation.ui;

import android.app.Application;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.LooperMode;
import salve.core.agent.AgentRun;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class AgentPlansActivityTest {
    static final ArrayDeque<Runnable> work = new ArrayDeque<>();
    static final Map<String, AgentRun> rows = new LinkedHashMap<>();
    static int submissions; static boolean paused;
    public static class Screen extends AgentPlansActivity {
        @Override protected Executor workExecutor() { return work::add; }
        @Override protected Backend createBackend() {
            return new Backend() {
                public List<AgentRun> recent() { return new ArrayList<>(rows.values()); }
                public boolean paused() { return paused; }
                public AgentRun create(String id, String goal) {
                    submissions++;
                    if (!rows.containsKey(id)) {
                        AgentRun run = new AgentRun(); run.id = id; run.goal = goal; rows.put(id, run);
                    }
                    return rows.get(id);
                }
                public boolean cancel(String id) { rows.get(id).status = "CANCELLED"; return true; }
                public boolean resume(String id) { rows.get(id).status = "QUEUED"; return true; }
                public void pause(boolean value) { paused = value; }
            };
        }
    }
    @Before public void reset() { work.clear(); rows.clear(); submissions = 0; paused = false; }
    void drain() {
        for (int i = 0; i < 20; i++) {
            while (!work.isEmpty()) work.remove().run();
            Shadows.shadowOf(Looper.getMainLooper()).idle();
            if (work.isEmpty()) return;
        }
        fail("UI kept scheduling immediate work");
    }
    View find(View node, java.util.function.Predicate<View> match) {
        if (match.test(node)) return node;
        if (node instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) node).getChildCount(); i++) {
            View result = find(((ViewGroup) node).getChildAt(i), match); if (result != null) return result;
        }
        return null;
    }
    View root(Screen screen) { return screen.findViewById(android.R.id.content); }
    Button button(Screen screen, String label) {
        Button button = (Button) find(root(screen), v -> v instanceof Button && ((Button) v).getText().toString().equals(label));
        assertNotNull(label, button); return button;
    }
    void draft(Screen screen, String text) { ((EditText) find(root(screen), v -> v instanceof EditText)).setText(text); }
    boolean text(Screen screen, String contains) {
        return find(root(screen), v -> v instanceof TextView && ((TextView) v).getText().toString().contains(contains)) != null;
    }
    @Test public void creationCancellationContinuationPauseAndChatResultUseSamePlan() {
        ActivityController<Screen> controller = Robolectric.buildActivity(Screen.class).setup();
        Screen screen = controller.get(); drain();
        draft(screen, "Recuerda el origen de Salve"); button(screen, "Crear plan").performClick(); drain();
        assertEquals(1, rows.size()); AgentRun run = rows.values().iterator().next();
        assertTrue(text(screen, "0/8 intentos"));
        button(screen, "Cancelar este plan").performClick(); drain(); assertEquals("CANCELLED", run.status);
        button(screen, "Continuar este plan").performClick(); drain(); assertEquals("QUEUED", run.status);
        button(screen, "Pausar planes y búsquedas").performClick(); drain(); assertTrue(paused);
        button(screen, "Reanudar planes y búsquedas").performClick(); drain(); assertFalse(paused);
        run.status = "PARTIAL"; run.answer = "Registro de 2025; falta confirmar la fecha.";
        run.skippedRepeats = 2;
        AgentRun previous = new AgentRun(); previous.id = "old-plan"; previous.goal = run.goal;
        previous.status = "SUCCEEDED"; previous.updatedAt = 100;
        previous.observations.add(new AgentRun.Observation("receipt", "memory.lookup", "SUCCESS", "2025"));
        run.experience = salve.core.agent.AgentExperience.contextFor(run, java.util.Collections.singletonList(previous), 100);
        controller.pause().resume(); drain();
        assertTrue(text(screen, "revisión incompleta"));
        assertTrue(text(screen, "Llamadas repetidas evitadas: 2"));
        assertTrue(text(screen, "Experiencia disponible de 1"));
        assertTrue(text(screen, "old-plan"));
        button(screen, "Llevar resultado al chat").performClick();
        assertEquals(android.app.Activity.RESULT_OK, Shadows.shadowOf(screen).getResultCode());
        assertEquals(run.id, Shadows.shadowOf(screen).getResultIntent().getStringExtra(AgentPlansActivity.EXTRA_PLAN_ID));
        controller.pause().stop().destroy();
    }
    @Test public void rotationDuringSaveReplaysSameIdAndDoesNotDuplicateObjective() {
        ActivityController<Screen> first = Robolectric.buildActivity(Screen.class).setup(); drain();
        draft(first.get(), "Mi objetivo persistente"); button(first.get(), "Crear plan").performClick();
        assertFalse(button(first.get(), "Crear plan").isEnabled());
        Bundle state = new Bundle(); first.pause().saveInstanceState(state).stop().destroy();
        ActivityController<Screen> second = Robolectric.buildActivity(Screen.class).create(state).start().resume().visible();
        drain();
        assertEquals(2, submissions); assertEquals(1, rows.size()); assertTrue(text(second.get(), "Mi objetivo persistente"));
        second.pause().stop().destroy(); drain();
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(java.time.Duration.ofSeconds(5));
        assertTrue(work.isEmpty()); // No polling after leaving the screen.
    }
    @Test public void blankDraftDoesNotCreateOrScheduleAPlan() {
        ActivityController<Screen> controller = Robolectric.buildActivity(Screen.class).setup(); drain();
        draft(controller.get(), "   "); button(controller.get(), "Crear plan").performClick(); drain();
        assertEquals(0, submissions); assertTrue(rows.isEmpty()); controller.pause().stop().destroy();
    }
}
