package salve.core.satellite;

import static org.junit.Assert.*;

import org.junit.Test;

public class InstallRoleTest {
    @Test public void bryansExistingInstallIsAlwaysOwner() {
        // Antes de esta función ya tenía memoria: nunca se le pregunta ni se le degrada.
        assertEquals(InstallRole.DUENO, InstallRole.decide(false, false, true));
        assertTrue(InstallRole.decide(false, false, true).fullSalve());
    }

    @Test public void freshCompileWithNothingIsUnclaimed() {
        assertEquals(InstallRole.SIN_RECLAMAR, InstallRole.decide(false, false, false));
        assertFalse(InstallRole.decide(false, false, false).fullSalve());
    }

    @Test public void claimingOwnerMakesItAFullSalve() {
        assertEquals(InstallRole.DUENO, InstallRole.decide(true, false, false));
    }

    @Test public void joiningAsSatelliteWinsAndIsNotAFullSalve() {
        assertEquals(InstallRole.SATELITE, InstallRole.decide(false, true, false));
        // Aunque hubiera datos, si está unida como satélite, es satélite.
        assertEquals(InstallRole.SATELITE, InstallRole.decide(false, true, true));
        assertFalse(InstallRole.decide(false, true, false).fullSalve());
    }
}
