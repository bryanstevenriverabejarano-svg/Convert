package salve.core.voice;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class LiveVoiceChannelTest {
    private static final class Observer implements LiveVoiceChannel.Listener {
        final List<String> events = new ArrayList<>();
        public void onReply(long id, String text, boolean audio) { events.add(id + ":" + text + ":" + audio); }
        public void onSpeechStarted(long id) { events.add(id + ":start"); }
        public void onSpeechFinished(long id, boolean completed) { events.add(id + ":end:" + completed); }
    }
    @Test public void activeTurnReceivesReplyAndActualAudioResult() {
        LiveVoiceChannel channel = new LiveVoiceChannel(); Observer observer = new Observer();
        LiveVoiceChannel.Ticket ticket = channel.open(1, observer);
        channel.reply(ticket, "Hola", true); channel.started(ticket); channel.finished(ticket, false);
        assertEquals(java.util.Arrays.asList("1:Hola:true", "1:start", "1:end:false"), observer.events);
    }
    @Test public void diagnosticsFollowTicketOwnershipWithoutLeakingStaleEvents() {
        LiveVoiceChannel channel = new LiveVoiceChannel();
        final java.util.List<String> events = new java.util.ArrayList<>();
        LiveVoiceChannel.Listener listener = new LiveVoiceChannel.Listener() {
            @Override public void onReply(long token, String text, boolean queued) { }
            @Override public void onSpeechStarted(long token) { }
            @Override public void onSpeechFinished(long token, boolean completed) { }
            @Override public void onDiagnostic(long token, VoiceDiagnosticLog.Event event, long code) {
                events.add(token + ":" + event.name() + ":" + code);
            }
        };
        LiveVoiceChannel.Ticket old = channel.open(1, listener);
        LiveVoiceChannel.Ticket current = channel.open(2, listener);
        channel.diagnostic(old, VoiceDiagnosticLog.Event.TTS_ERROR_CODE, 7);
        channel.diagnostic(current, VoiceDiagnosticLog.Event.TTS_STOPPED, 1);
        assertEquals(java.util.Collections.singletonList("2:TTS_STOPPED:1"), events);
    }

    @Test public void stoppingRevokesLateReplyAndAudioCallbacks() {
        LiveVoiceChannel channel = new LiveVoiceChannel(); Observer observer = new Observer();
        LiveVoiceChannel.Ticket ticket = channel.open(1, observer); channel.cancel();
        channel.reply(ticket, "late", true); channel.started(ticket); channel.finished(ticket, true);
        assertTrue(observer.events.isEmpty()); assertFalse(channel.isActive());
    }
    @Test public void sameNumericTokenFromAnotherDialogCannotReviveOldSession() {
        LiveVoiceChannel channel = new LiveVoiceChannel(); Observer first = new Observer(), second = new Observer();
        LiveVoiceChannel.Ticket old = channel.open(1, first), current = channel.open(1, second);
        channel.reply(old, "stale", true); channel.finished(old, true); channel.reply(current, "new", false);
        assertTrue(first.events.isEmpty()); assertEquals(java.util.Collections.singletonList("1:new:false"), second.events);
    }
    @Test public void callbacksMayStopTheirOwnSession() {
        LiveVoiceChannel channel = new LiveVoiceChannel();
        LiveVoiceChannel.Ticket ticket = channel.open(3, new LiveVoiceChannel.Listener() {
            public void onReply(long id, String text, boolean audio) { channel.cancel(); }
            public void onSpeechStarted(long id) { fail("Revoked session"); }
            public void onSpeechFinished(long id, boolean completed) { fail("Revoked session"); }
        });
        channel.reply(ticket, "no audio", false); channel.started(ticket); channel.finished(ticket, true);
        assertFalse(channel.owns(ticket));
    }
}
