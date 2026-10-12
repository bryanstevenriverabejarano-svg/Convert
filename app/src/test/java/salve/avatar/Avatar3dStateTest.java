package salve.avatar;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.Test;
import static org.junit.Assert.*;

public class Avatar3dStateTest {
    @Test public void realSpeechCannotChangeHeldBodyPose() {
        AvatarState body = new AvatarState();
        body.pose(AvatarState.Pose.KNEELING);
        AvatarMotion motion = new AvatarMotion();
        motion.activate(1); motion.beginTurn(1, 2, "hola");
        motion.speechPending(1, 2, "tts"); motion.speechStart(1, "tts"); motion.advance(.1f);
        JsonObject speaking = JsonParser.parseString(Avatar3dState.encode(body, motion.snapshot(), true)).getAsJsonObject();
        assertEquals("KNEELING", speaking.get("pose").getAsString());
        assertTrue(speaking.get("speaking").getAsBoolean());
        assertTrue(speaking.get("mouth").getAsFloat() > 0);
        motion.speechEnd(1, "tts");
        JsonObject stopped = JsonParser.parseString(Avatar3dState.encode(body, motion.snapshot(), true)).getAsJsonObject();
        assertEquals("KNEELING", stopped.get("pose").getAsString());
        assertFalse(stopped.get("speaking").getAsBoolean());
        assertEquals(0, stopped.get("mouth").getAsFloat(), 0);
    }
    @Test public void closedSessionCannotReopenMouth() {
        AvatarMotion motion = new AvatarMotion();
        motion.activate(1); motion.beginTurn(1, 2, "hola"); motion.speechPending(1, 2, "old");
        motion.close(1); motion.speechStart(1, "old"); motion.advance(.1f);
        JsonObject value = JsonParser.parseString(Avatar3dState.encode(new AvatarState(), motion.snapshot(), false)).getAsJsonObject();
        assertFalse(value.get("speaking").getAsBoolean());
        assertEquals(0, value.get("mouth").getAsFloat(), 0);
        assertFalse(value.has("text"));
    }
    @Test public void assetAllowlistRejectsNavigationAndOtherPrivateAssets() {
        assertTrue(Avatar3dState.allowedAsset("/avatar3d/index.html"));
        assertTrue(Avatar3dState.allowedAsset("/avatar3d/SALVE_Animada.glb"));
        assertFalse(Avatar3dState.allowedAsset("/avatar3d/../config/models.json"));
        assertFalse(Avatar3dState.allowedAsset("/config/models.json"));
        assertFalse(Avatar3dState.allowedAsset(null));
    }
}
