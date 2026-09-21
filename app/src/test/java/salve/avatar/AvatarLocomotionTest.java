package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AvatarLocomotionTest {
    @Test public void deviceUptimeCannotChooseTheStartingFootPose() {
        AvatarLocomotion a=new AvatarLocomotion(),b=new AvatarLocomotion();
        for(int i=0;i<25;i++) {
            a.advance(1000+i*20,.3f+i*.0036f,true);
            b.advance(589970+i*20,.3f+i*.0036f,true);
            assertEquals(a.stride(),b.stride(),.0001f);
        }
    }
    @Test public void noDisplacementMeansNoInventedSteps() {
        AvatarLocomotion gait=new AvatarLocomotion();
        for(int i=0;i<50;i++)gait.advance(1000+i*20,.3f,true);
        assertEquals(0,gait.phase(),0);assertEquals(0,gait.stride(),0);
    }
    @Test public void stoppingBlendsOutInsteadOfSnappingTheLegs() {
        AvatarLocomotion gait=new AvatarLocomotion();
        for(int i=0;i<15;i++)gait.advance(1000+i*20,.3f+i*.002f,true);
        float before=gait.stride();gait.advance(1300,.328f,false);
        assertTrue(Math.abs(gait.stride())>0);assertTrue(Math.abs(gait.stride())<Math.abs(before));
        for(int i=1;i<50;i++)gait.advance(1300+i*20,.328f,false);
        assertEquals(0,gait.stride(),.0005f);
    }
    @Test public void movingTheOverlayByHandDoesNotTrainOrAdvanceGait() {
        AvatarLocomotion gait=new AvatarLocomotion();gait.advance(1000,.3f,false);gait.advance(1020,.9f,false);
        assertEquals(0,gait.phase(),0);assertEquals(0,gait.stride(),0);
    }
    @Test public void simultaneousViewsCannotAdvanceTheSameFrameTwice() {
        AvatarLocomotion gait=new AvatarLocomotion();gait.advance(1000,.3f,true);gait.advance(1020,.305f,true);
        float phase=gait.phase(),stride=gait.stride();gait.advance(1020,.305f,true);
        assertEquals(phase,gait.phase(),0);assertEquals(stride,gait.stride(),0);
    }
}
