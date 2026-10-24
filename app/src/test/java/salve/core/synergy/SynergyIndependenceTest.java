package salve.core.synergy;

import org.junit.Test;
import static org.junit.Assert.*;

public class SynergyIndependenceTest {
    @Test public void lowCuriosityStopsExplorationButNotMaintenance() {
        SynergyCore.State state=new SynergyCore.State();
        state.curiosity=0;
        SynergyCore.Device safe=new SynergyCore.Device(90,true,false,false,false);
        assertFalse(SynergyCore.mayExplore(state,safe,false,false));
        assertTrue(SynergyCore.mayReflect(state,safe,false,false));
    }
}