package salve.core.voice;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;
import static salve.core.voice.VoiceConversationLoop.Action;
import static salve.core.voice.VoiceConversationLoop.State;
import static salve.core.voice.VoiceConversationLoop.StopReason;

/** Portable component integration; these tests do not emulate Android ASR or TTS hardware. */
public class LiveVoicePipelineTest {
    /** Test listener forwards actual channel events into the real conversation state machine. */
    private static final class Dialog implements LiveVoiceChannel.Listener {
        final VoiceConversationLoop loop = new VoiceConversationLoop();
        final List<String> replies = new ArrayList<>();
        final LiveVoiceChannel.Ticket ticket;
        final VoiceReplyBatch<String> batch;
        long now = 100L;
        int starts;
        int finishes;

        Dialog(LiveVoiceChannel channel) {
            long token = loop.start(0L).token;
            assertEquals(Action.SUBMIT, loop.recognized(token, "Hola Salve", now).action);
            ticket = channel.open(token, this);
            batch = new VoiceReplyBatch<>(text -> channel.reply(ticket, text, true));
        }

        @Override public void onReply(long token, String text, boolean audioQueued) {
            replies.add(text);
            loop.replied(token, audioQueued, now);
        }

        @Override public void onSpeechStarted(long token) {
            starts++;
            loop.speechStarted(token, now);
        }

        @Override public void onSpeechFinished(long token, boolean completed) {
            finishes++;
            loop.speechFinished(token, completed, now);
        }
    }

    @Test public void provisionalAcknowledgementDoesNotFinishTurnAndTtsDoneReopensAfterGuard() {
        LiveVoiceChannel channel = new LiveVoiceChannel();
        Dialog dialog = new Dialog(channel);
        long originalToken = dialog.loop.token();
        long responseDeadline = dialog.loop.nextDeadlineMillis();

        assertTrue(dialog.batch.offer("Lo estoy calculando"));
        assertTrue(dialog.replies.isEmpty());
        assertEquals(State.THINKING, dialog.loop.state());
        assertEquals(responseDeadline, dialog.loop.nextDeadlineMillis());
        assertEquals(Action.NONE, dialog.loop.tick(500L).action);

        dialog.now = 1_000L;
        assertTrue(dialog.batch.offer("El resultado comprobado es 42"));
        dialog.batch.complete();
        assertEquals(1, dialog.replies.size());
        assertEquals("El resultado comprobado es 42", dialog.replies.get(0));
        assertEquals(State.SPEAKING, dialog.loop.state());
        dialog.now = 1_050L;
        channel.started(dialog.ticket);
        assertEquals(1, dialog.starts);
        assertEquals(State.SPEAKING, dialog.loop.state());

        dialog.now = 1_500L;
        channel.finished(dialog.ticket, true);
        assertEquals(1, dialog.finishes);
        assertEquals(State.COOLDOWN, dialog.loop.state());
        long reopenAt = dialog.now + VoiceConversationLoop.ECHO_GUARD_MS;
        assertEquals(Action.NONE, dialog.loop.tick(reopenAt - 1).action);
        assertEquals(State.COOLDOWN, dialog.loop.state());
        VoiceConversationLoop.Step nextCapture = dialog.loop.tick(reopenAt);
        assertEquals(Action.LISTEN, nextCapture.action);
        assertEquals(State.LISTENING, dialog.loop.state());
        assertNotEquals(originalToken, nextCapture.token);
    }

    @Test public void retainedConfirmationCallbackDeliversAfterWorkerHasAlreadyFinished() {
        LiveVoiceChannel channel = new LiveVoiceChannel();
        Dialog dialog = new Dialog(channel);
        assertTrue(dialog.batch.offer("Preparando la confirmación"));
        assertTrue(dialog.batch.retain());
        Runnable queuedUiConfirmation = () -> {
            assertTrue(dialog.batch.offer("¿Quieres guardar este recuerdo?"));
            dialog.batch.complete();
        };

        // The source worker finishes before the UI runs its retained continuation.
        dialog.batch.complete();
        assertTrue(dialog.replies.isEmpty());
        assertEquals(State.THINKING, dialog.loop.state());
        assertEquals(Action.NONE, dialog.loop.tick(600L).action);

        dialog.now = 700L;
        queuedUiConfirmation.run();
        assertEquals(1, dialog.replies.size());
        assertEquals("¿Quieres guardar este recuerdo?", dialog.replies.get(0));
        assertEquals(State.SPEAKING, dialog.loop.state());
        dialog.now = 800L;
        channel.started(dialog.ticket);
        dialog.now = 1_200L;
        channel.finished(dialog.ticket, true);
        assertEquals(State.COOLDOWN, dialog.loop.state());
        assertEquals(Action.LISTEN,
                dialog.loop.tick(dialog.now + VoiceConversationLoop.ECHO_GUARD_MS).action);
        assertFalse(dialog.batch.offer("Respuesta duplicada tardía"));
        dialog.batch.complete();
        assertEquals(1, dialog.replies.size());
    }

    @Test public void reopenedDialogWithSameNumericTokenRejectsOldReplyAndAudioEvents() {
        LiveVoiceChannel channel = new LiveVoiceChannel();
        Dialog oldDialog = new Dialog(channel);
        long oldToken = oldDialog.loop.token();
        oldDialog.batch.offer("Respuesta del diálogo cerrado");
        oldDialog.loop.stop(StopReason.LIFECYCLE);
        channel.cancel();

        Dialog current = new Dialog(channel);
        assertEquals(oldToken, current.loop.token());
        assertNotSame(oldDialog.ticket, current.ticket);
        assertFalse(channel.owns(oldDialog.ticket));
        assertTrue(channel.owns(current.ticket));

        oldDialog.batch.complete();
        channel.started(oldDialog.ticket);
        channel.finished(oldDialog.ticket, true);
        assertTrue(oldDialog.replies.isEmpty());
        assertTrue(current.replies.isEmpty());
        assertEquals(0, oldDialog.starts);
        assertEquals(0, oldDialog.finishes);
        assertEquals(0, current.starts);
        assertEquals(0, current.finishes);
        assertEquals(State.STOPPED, oldDialog.loop.state());
        assertEquals(State.THINKING, current.loop.state());

        current.now = 1_000L;
        current.batch.offer("Respuesta del diálogo actual");
        current.batch.complete();
        channel.started(current.ticket);
        assertEquals(State.SPEAKING, current.loop.state());
        current.now = 1_100L;
        channel.finished(oldDialog.ticket, false);
        assertEquals(State.SPEAKING, current.loop.state());
        assertEquals(0, current.finishes);
        assertEquals(1, current.replies.size());
        assertEquals("Respuesta del diálogo actual", current.replies.get(0));

        current.now = 1_500L;
        channel.finished(current.ticket, true);
        assertEquals(1, current.finishes);
        assertEquals(State.COOLDOWN, current.loop.state());
    }
}
