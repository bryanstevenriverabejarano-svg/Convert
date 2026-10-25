package salve.core.synergy;

import org.junit.Test;
import static org.junit.Assert.*;

public class SynergyCoreTest {
    @Test public void realEventsAdjustBoundedPriorities() {
        SynergyCore.State initial=new SynergyCore.State();
        SynergyCore.State failed=SynergyCore.advance(initial,SynergyCore.Event.FAILED,false,100);
        assertTrue(failed.uncertainty>initial.uncertainty);
        assertEquals("un fallo en el chat no gasta el presupuesto nocturno",initial.energy,failed.energy,1e-9);
        assertEquals(0,failed.verified);
        SynergyCore.State checked=SynergyCore.advance(failed,SynergyCore.Event.VERIFIED,true,101);
        assertEquals(1,checked.verified);
        assertEquals(0,initial.events);
    }
    @Test public void chargeCanRecoverBudgetAndCuriosity() {
        SynergyCore.State s=new SynergyCore.State();
        s.at=100; s.energy=.1; s.curiosity=.1;
        SynergyCore.State recovered=SynergyCore.advance(s,SynergyCore.Event.NONE,true,43_200_100);
        assertTrue(recovered.energy>.1);
        assertTrue(recovered.curiosity>.1);
    }
    @Test public void thermalBatteryAndPausesBlockWork() {
        SynergyCore.State s=new SynergyCore.State();
        SynergyCore.Device ok=new SynergyCore.Device(85,true,false,false,false);
        assertTrue(SynergyCore.mayExplore(s,ok,false,false));
        assertTrue(SynergyCore.mayReflect(s,ok,false,false));
        assertFalse(SynergyCore.mayExplore(s,new SynergyCore.Device(10,true,false,false,false),false,false));
        assertFalse(SynergyCore.mayExplore(s,new SynergyCore.Device(85,true,false,true,false),false,false));
        assertFalse(SynergyCore.mayExplore(s,new SynergyCore.Device(85,true,false,false,true),false,false));
        assertFalse(SynergyCore.mayExplore(s,ok,true,false));
        assertFalse(SynergyCore.mayReflect(s,ok,false,true));
    }
    @Test public void statusCommandOnlyMatchesRegulator() {
        assertTrue(SynergyRequests.isStatus("Estado de tu sinergía"));
        assertFalse(SynergyRequests.isStatus("¿Cuánto consume mi coche?"));
    }
}