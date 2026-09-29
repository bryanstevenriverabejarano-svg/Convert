package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

/** Real turn/audio sequences, not screenshots presented as 3D validation. */
public class AvatarPerformanceTest {
    @Test public void oldProtocolKeepsBodyWhileNewProtocolCanChangeIt() {
        assertEquals(AvatarBodyCue.KEEP, AvatarMotionProtocol.parse("Sí. [[salve_motion:NOD:WARM]]").body);
        AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse("Hablemos. [[salve_motion:EXPLAIN:NEUTRAL:WALK]]");
        assertTrue(result.hasDirective); assertEquals("Hablemos.", result.text);
        assertEquals(AvatarBodyCue.WALK, result.body);
    }
    @Test public void malformedBodyCommandsCannotChangePosture() {
        for (String value : new String[]{"Hola [[salve_motion:NONE:NEUTRAL:FLY]]",
                "[[salve_motion:NONE:NEUTRAL:SIT]] aún no.",
                "[[salve_motion:NONE:NEUTRAL:SIT]][[salve_motion:NONE:NEUTRAL:WALK]]"}) {
            AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse(value);
            assertFalse(value, result.hasDirective); assertEquals(AvatarBodyCue.KEEP, result.body);
            assertFalse(result.text.contains("salve_motion"));
        }
    }
    @Test public void exactSpanishCommandsAreAcceptedButNegationsAreConversation() {
        assertEquals(AvatarBodyCue.SIT, AvatarMotionProtocol.parseCommand("Siéntate.").body);
        assertEquals(AvatarBodyCue.KNEEL, AvatarMotionProtocol.parseCommand("Arrodíllate").body);
        assertEquals(AvatarBodyCue.WALK, AvatarMotionProtocol.parseCommand("camina mientras hablamos").body);
        for (String value : new String[]{"no bailes", "¿por qué lloras?", "explica qué significa siéntate", "no te arrodilles"})
            assertNull(value, AvatarMotionProtocol.parseCommand(value));
    }
    @Test public void seatedPostureSurvivesSpeakingThinkingAndAnotherReply() {
        AvatarMotion motion = new AvatarMotion(); AvatarState body = new AvatarState();
        motion.activate(1); motion.beginTurn(1,2,"siéntate");
        motion.response(1,2,AvatarMotionProtocol.parseCommand("siéntate"));
        motion.snapshot().bodyCue.apply(body);
        long serial = motion.snapshot().bodyCueSerial;
        motion.speechPending(1,2,"voice"); motion.speechStart(1,"voice"); motion.advance(.1f);
        assertTrue(motion.snapshot().speaking); assertTrue(motion.snapshot().mouthOpen > 0);
        assertEquals(AvatarState.Pose.SEATED,body.getPose());
        motion.speechEnd(1,"voice"); motion.beginTurn(1,3,"cuéntame algo");
        motion.response(1,3,AvatarMotionProtocol.parse("Aquí sigo. [[salve_motion:EXPLAIN:WARM]]"));
        assertEquals(serial,motion.snapshot().bodyCueSerial); assertEquals(AvatarState.Pose.SEATED,body.getPose());
    }
    @Test public void staleTurnAndClosedSessionCannotReplayBodyAction() {
        AvatarMotion motion = new AvatarMotion(); motion.activate(1);
        motion.beginTurn(1,2,"camina"); motion.beginTurn(1,3,"hola");
        motion.response(1,2,AvatarMotionProtocol.parseCommand("camina"));
        assertEquals(0,motion.snapshot().bodyCueSerial);
        motion.close(1); motion.response(1,3,AvatarMotionProtocol.parseCommand("baila"));
        assertEquals(0,motion.snapshot().bodyCueSerial);
    }
    @Test public void walkingAndVoiceCanAdvanceAtTheSameTime() {
        AvatarState body = new AvatarState(); AvatarBodyCue.WALK.apply(body);
        float before = body.getX();
        AvatarMotion motion = new AvatarMotion(); motion.activate(1); motion.beginTurn(1,2,"hola");
        motion.speechPending(1,2,"voice"); motion.speechStart(1,"voice");
        body.advance(.1f); motion.advance(.1f);
        assertTrue(body.getX() > before); assertTrue(motion.snapshot().speaking);
        assertTrue(motion.snapshot().mouthOpen > 0);
    }
    @Test public void poseChangeCancelsPendingSleepAndNavigation() {
        AvatarState body = new AvatarState(); body.createBed(); body.sleep();
        AvatarBodyCue.KNEEL.apply(body);
        for(int i=0;i<100;i++)body.advance(.1f);
        assertEquals(AvatarState.Pose.KNEELING,body.getPose()); assertFalse(body.isGoingToSleep());
        assertEquals(body.getX(),body.getTargetX(),0);
    }
    @Test public void addedPosesRoundTripWithoutInventingMovement() {
        for (AvatarBodyCue cue : new AvatarBodyCue[]{AvatarBodyCue.SIT,AvatarBodyCue.KNEEL,AvatarBodyCue.CROUCH,
                AvatarBodyCue.DANCE_POP,AvatarBodyCue.DANCE_URBAN}) {
            AvatarState body = new AvatarState(); cue.apply(body);
            AvatarState restored = AvatarState.decode(body.encode());
            assertEquals(body.getPose(),restored.getPose());
            restored.advance(20); assertEquals(body.getX(),restored.getX(),0);
        }
    }
    @Test public void laughMovesMouthWithoutMarkingAudioAsStarted() {
        AvatarMotion motion = new AvatarMotion(); motion.previewGesture(AvatarMotion.Gesture.LAUGH,AvatarMotion.Expression.WARM);
        for(int i=0;i<10;i++)motion.advance(.1f);
        assertTrue(motion.snapshot().mouthOpen > 0); assertFalse(motion.snapshot().speaking);
        motion.advance(10); assertEquals(0,motion.snapshot().mouthOpen,0);
    }
    @Test public void bodyCueIsOneShotAcrossAudioCallbacks() {
        AvatarMotion motion = new AvatarMotion(); motion.activate(1); motion.beginTurn(1,2,"baila");
        motion.response(1,2,AvatarMotionProtocol.parseCommand("baila")); long serial = motion.snapshot().bodyCueSerial;
        motion.speechPending(1,2,"voice"); motion.speechStart(1,"voice");
        for(int i=0;i<20;i++){ motion.speechRange(1,"voice",i,i+1); motion.advance(.05f); }
        motion.speechEnd(1,"voice"); motion.endTurn(1,2);
        assertEquals(serial,motion.snapshot().bodyCueSerial); assertEquals(AvatarBodyCue.DANCE_POP,motion.snapshot().bodyCue);
    }
}
