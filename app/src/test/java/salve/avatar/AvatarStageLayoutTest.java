package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

public class AvatarStageLayoutTest {
    @Test public void fullArtworkFitsNarrowTallAndLandscapeStagesThroughoutWalking() {
        float[][] sizes = {{280, 420}, {360, 620}, {412, 760}, {760, 220}, {280, 100}, {900, 1000}};
        for (float[] size : sizes) for (float position : new float[]{-1, 0, .3f, .7f, 1, 2}) {
            AvatarStageLayout layout = AvatarStageLayout.fit(size[0], size[1], position);
            assertTrue(layout.floor - layout.height >= 0);
            assertTrue(layout.floor <= size[1]);
            assertTrue(layout.centerX - layout.height / 3f >= 0);
            assertTrue(layout.centerX + layout.height / 3f <= size[0]);
            assertTrue(layout.bedCenterX - 105 * layout.bedScale >= 0);
            assertTrue(layout.bedCenterX + 105 * layout.bedScale <= size[0]);
            assertTrue(layout.floor - 116 * layout.bedScale >= 0);
        }
    }
    @Test public void sleepingSceneStaysVisibleAndMovesAwayFromBottomOfTallPhones() {
        for (float[] size : new float[][]{{280, 100}, {360, 620}, {412, 900}, {760, 220}}) {
            AvatarStageLayout layout = AvatarStageLayout.fit(size[0], size[1], .7f);
            float bedFloor = layout.bedFloor(size[1], true);
            assertTrue(bedFloor - 100 * layout.bedScale >= 0);
            assertTrue(bedFloor + 8 * layout.bedScale <= size[1]);
        }
        AvatarStageLayout tall = AvatarStageLayout.fit(412, 900, .7f);
        assertTrue(tall.bedFloor(900, true) < tall.floor - 150);
    }
    @Test public void phonePortraitGivesCharacterMostOfAvailableHeight() {
        AvatarStageLayout layout = AvatarStageLayout.fit(360, 560, .3f);
        assertTrue(layout.height > 480);
    }
    @Test public void zeroViewportProducesNoCharacter() {
        assertEquals(0, AvatarStageLayout.fit(0, 0, .3f).height, 0);
    }
}
