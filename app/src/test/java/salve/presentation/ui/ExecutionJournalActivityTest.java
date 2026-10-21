package salve.presentation.ui;

import android.app.Application;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest=Config.NONE, sdk=28, application=Application.class)
public class ExecutionJournalActivityTest {
    @Test public void showsAndCopiesTheStoredTranscriptAsSelectableLiteralText() throws Exception {
        Application app = RuntimeEnvironment.getApplication(); ExecutionJournal journal = ExecutionTrace.journal(app);
        journal.clear(); String id = journal.begin("Dibujo", "sombrero");
        String note = "  una nota\n\n\tsegunda línea <script>no ejecutar</script>";
        journal.append(id, "NOTA", note); journal.finish(id, "TURNO_TERMINADO");
        String expected = ExecutionJournal.transcript(journal.recent().get(0));
        ActivityController<ExecutionJournalActivity> controller = Robolectric.buildActivity(ExecutionJournalActivity.class)
                .create().start().resume().visible();
        try {
            View root = controller.get().findViewById(android.R.id.content); TextView log = null;
            for (int i = 0; i < 150 && log == null; i++) {
                org.robolectric.shadows.ShadowLooper.idleMainLooper();
                log = find(root, expected); if (log == null) Thread.sleep(10);
            }
            assertNotNull("stored literal transcript is visible", log); assertTrue(log.isTextSelectable());
            Button copy = (Button) find(root, "Copiar"); assertNotNull(copy); copy.performClick();
            ClipboardManager clipboard = (ClipboardManager) controller.get().getSystemService(Context.CLIPBOARD_SERVICE);
            assertEquals(expected, clipboard.getPrimaryClip().getItemAt(0).getText().toString());
            assertNull(org.robolectric.Shadows.shadowOf(app).getNextStartedActivity());
        } finally { controller.pause().stop().destroy(); }
    }
    private TextView find(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return (TextView) view;
        if (view instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
            TextView found = find(((ViewGroup) view).getChildAt(i), text); if (found != null) return found;
        }
        return null;
    }
}
