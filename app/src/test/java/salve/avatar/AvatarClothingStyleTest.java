package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AvatarClothingStyleTest {
    private static final int PALETTE=0xffe19cad;
    @Test public void noRecipeLeavesEveryPixelUnchanged() {
        for(int color:new int[]{0,0xfff7e7dd,0xff113035,0x44222222})
            assertEquals(color,AvatarClothingStyle.pixel(480,700,color,0,"NONE"));
    }
    @Test public void faceHandsLegsAndFrontHairAreOutsideTheStyleMask() {
        for(int[] p:new int[][]{{480,320},{225,860},{755,880},{410,1190},{368,500},{580,510}})
            assertEquals(0xff344050,AvatarClothingStyle.pixel(p[0],p[1],0xff344050,PALETTE,"STARS"));
    }
    @Test public void skinAndWhiteHairPixelsStayUncoloredEvenInsideThePanel() {
        assertEquals(0xffffd7c1,AvatarClothingStyle.pixel(475,430,0xffffd7c1,PALETTE,"STRIPES"));
        assertEquals(0xfff4f1ed,AvatarClothingStyle.pixel(480,700,0xfff4f1ed,PALETTE,"STARS"));
    }
    @Test public void paletteChangesFabricButPreservesItsAlpha() {
        int styled=AvatarClothingStyle.pixel(480,704,0xe0123035,PALETTE,"NONE");
        assertNotEquals(0xe0123035,styled);assertEquals(0xe0,styled>>>24);
    }
    @Test public void patternsActuallyMarkOnlyTextilePixels() {
        assertNotEquals(0xff123035,AvatarClothingStyle.pixel(480,704,0xff123035,0,"STRIPES"));
        assertEquals(0xff123035,AvatarClothingStyle.pixel(480,709,0xff123035,0,"STRIPES"));
    }
    @Test public void pajamaAccentsEndOnTheShirtWithoutPaintingARectangleOnThePants() {
        int fabric=0xff344050;
        assertNotEquals(fabric,AvatarClothingStyle.pixel("pajamas",480,760,fabric,PALETTE,"NONE"));
        for(int y:new int[]{860,880,920,960})
            assertEquals(fabric,AvatarClothingStyle.pixel("pajamas",480,y,fabric,PALETTE,"STRIPES"));
    }
    @Test public void pajamaHemColorFadesBeforeReachingTheMaskBoundary() {
        int fabric=0xff344050,previous=Integer.MAX_VALUE;
        for(int y:new int[]{770,800,825,850,859,860}) {
            int styled=AvatarClothingStyle.pixel("pajamas",480,y,fabric,PALETTE,"NONE");
            int difference=Math.abs(((styled>>>16)&255)-((fabric>>>16)&255));
            assertTrue("A hem accent must fade, not become a hard edge",difference<=previous);
            previous=difference;
        }
        assertEquals(0,previous);
    }
    @Test public void approvedHeadIsExactAboveTheBlendBoundary() {
        int original=0xffffede1;
        assertEquals(original,AvatarClothingStyle.blendHead(original,0xff992244,2));
        assertEquals(original,AvatarClothingStyle.blendHead(original,0xff992244,1));
    }
    @Test public void transparentTemplatePixelsCannotAddADarkHaloToHeadBlending() {
        int result=AvatarClothingStyle.blendHead(0xffffffff,0x00000000,.5f);
        assertEquals(128,result>>>24);assertEquals(0xffffff,result&0xffffff);
    }
}
