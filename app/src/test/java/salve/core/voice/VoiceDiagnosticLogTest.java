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
@Config(manifest=Config.NONE, sdk=28, application=Application.class)
public class VoiceDiagnosticLogTest {
    @Test public void technicalVoiceEventsAppearInExecutionJournalWithoutTranscripts() {
        Application app = RuntimeEnvironment.getApplication();
        ExecutionTrace.journal(app).clear();
        VoiceDiagnosticLog diagnostic = new VoiceDiagnosticLog(app);
        diagnostic.start();
        diagnostic.event(3, VoiceDiagnosticLog.Event.TTS_STARTED, 0);
        diagnostic.event(3, VoiceDiagnosticLog.Event.INTERRUPT_AUTOMATIC, 1);
        diagnostic.event(3, VoiceDiagnosticLog.Event.TTS_STOPPED, 1);
        diagnostic.metrics("voice_turn token=3 result=INTERRUPTED stop_reason=none asr_error=none tts_duration_ms=200");
        diagnostic.finish(3);
        assertEquals(1, ExecutionTrace.journal(app).recent().size());
        ExecutionJournal.Task task = ExecutionTrace.journal(app).recent().get(0);
        String report = ExecutionJournal.transcript(task);
        assertEquals("Conversación · diagnóstico de voz", task.type);
        assertTrue(report.contains("INTERRUPT_AUTOMATIC"));
        assertTrue(report.contains("TTS_STOPPED"));
        assertTrue(report.contains("tts_duration_ms=200"));
        assertFalse(report.contains("audio content"));
    }

    @Test public void arbitrarySpeechCannotBeAddedAsMetric() {
        Application app = RuntimeEnvironment.getApplication();
        ExecutionTrace.journal(app).clear();
        VoiceDiagnosticLog diagnostic = new VoiceDiagnosticLog(app);
        diagnostic.start();
        diagnostic.metrics("voice_turn token=3 result=INTERRUPTED texto privado: NO_GUARDAR");
        diagnostic.finish(3);
        String report = ExecutionJournal.transcript(ExecutionTrace.journal(app).recent().get(0));
        assertFalse(report.contains("NO_GUARDAR"));
    }
}
