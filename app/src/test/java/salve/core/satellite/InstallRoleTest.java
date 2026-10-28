package salve.core.satellite;

import static org.junit.Assert.*;

import org.junit.Test;

public class InstallRoleTest {
    @Test public void bryansExistingInstallIsAlwaysOwner() {
        // Antes de esta función ya tenía memoria: nunca se le pregunta ni se le degrada.
        assertEquals(InstallRole.DUENO, InstallRole.decide(false, true));
        assertTrue(InstallRole.decide(false, true).fullSalve());
    }

    @Test public void freshCompileBecomesSatelliteAutomatically() {
        // Sin dueño y sin datos: se instala sola como satélite, sin preguntar.
        assertEquals(InstallRole.SATELITE, InstallRole.decide(false, false));
        assertFalse(InstallRole.decide(false, false).fullSalve());
    }

    @Test public void claimingOwnerExplicitlyMakesItAFullSalve() {
        assertEquals(InstallRole.DUENO, InstallRole.decide(true, false));
    }
}
