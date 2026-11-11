package salve.core.voice;

import android.app.Application;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.trace.ExecutionJournal;
import salve.core.trace.ExecutionTrace;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class VoicePlaybackJournalTest {
    @Test public void recordsRealAudioLifecycleSeparatelyFromModelMetrics() throws Exception {
        Application context = RuntimeEnvironment.getApplication();
        ExecutionTrace.journal(context).clear();
        VoicePlaybackJournal.begin(context, "salve_tts_101", "respuesta del chat", 432);
        VoicePlaybackJournal.event("salve_tts_101", "ENQUEUE_ACCEPTED", 0);
        VoicePlaybackJournal.event("salve_tts_101", "TTS_STARTED", 0);
        VoicePlaybackJournal.event("salve_tts_101", "TTS_STOPPED", 1);
        VoicePlaybackJournal.finish("salve_tts_101", "INTERRUMPIDA");
        assertTrue(VoicePlaybackJournal.awaitIdleForTests());
        assertEquals(1, ExecutionTrace.journal(context).recent().size());
        ExecutionJournal.Task task = ExecutionTrace.journal(context).recent().get(0);
        String transcript = ExecutionJournal.transcript(task);
        assertTrue(task.type.contains("respuesta del chat"));
        assertEquals("INTERRUMPIDA", task.status);
        assertTrue(transcript.contains("ENQUEUE_ACCEPTED"));
        assertTrue(transcript.contains("TTS_STOPPED"));
        assertTrue(transcript.contains("chars=432"));
    }

    @Test public void rejectsUnstructuredDataSoNoSpeechTextCanBeLogged() throws Exception {
        Application context = RuntimeEnvironment.getApplication();
        ExecutionTrace.journal(context).clear();
        VoicePlaybackJournal.begin(context, "salve_tts_202", "respuesta del chat", 42);
        VoicePlaybackJournal.event("salve_tts_202", "Mi mensaje privado", 0);
        VoicePlaybackJournal.event("salve_tts_202", "WITH_TEXT=PERSONAL", 0);
        VoicePlaybackJournal.finish("salve_tts_202", "COMPLETADA");
        assertTrue(VoicePlaybackJournal.awaitIdleForTests());
        String report = ExecutionJournal.transcript(ExecutionTrace.journal(context).recent().get(0));
        assertFalse(report.contains("Mi mensaje privado"));
        assertFalse(report.contains("WITH_TEXT=PERSONAL"));
    }
}
