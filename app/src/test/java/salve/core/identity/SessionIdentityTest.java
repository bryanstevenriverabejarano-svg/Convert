package salve.core.identity;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

public class SessionIdentityTest {
    private static final long NOW = 1_000_000_000L;
    private static final List<String[]> FAMILY = Collections.singletonList(new String[]{"hermana", "Ana"});

    @Test public void startsPresumingTheCreatorOnHisDevice() {
        SessionIdentity s = new SessionIdentity();
        assertEquals(SessionIdentity.State.PRESUMED_CREATOR, s.state());
        assertTrue(s.isCreatorSide());
        assertFalse("con biometría disponible, la presunción no abre lo íntimo", s.privateAccess(true, NOW));
        assertTrue("sin biometría, el dispositivo es la mejor prueba", s.privateAccess(false, NOW));
    }

    @Test public void biometricSuccessVerifiesAndExpires() {
        SessionIdentity s = new SessionIdentity();
        s.onBiometricSuccess(NOW);
        assertEquals(SessionIdentity.State.VERIFIED_CREATOR, s.state());
        assertTrue(s.privateAccess(true, NOW + 1000));
        s.expire(NOW + SessionIdentity.VERIFICATION_TTL + 1);
        assertEquals(SessionIdentity.State.PRESUMED_CREATOR, s.state());
    }

    @Test public void restoreKeepsOnlyFreshVerifications() {
        SessionIdentity fresh = new SessionIdentity();
        fresh.restore(NOW - 1000, NOW);
        assertEquals(SessionIdentity.State.VERIFIED_CREATOR, fresh.state());
        SessionIdentity stale = new SessionIdentity();
        stale.restore(NOW - SessionIdentity.VERIFICATION_TTL - 1, NOW);
        assertEquals(SessionIdentity.State.PRESUMED_CREATOR, stale.state());
    }

    @Test public void cancelledBiometricOrVoiceRaisesDoubt() {
        SessionIdentity cancelled = new SessionIdentity();
        cancelled.onBiometricRejected(NOW);
        assertEquals(SessionIdentity.State.UNCERTAIN, cancelled.state());
        SessionIdentity voice = new SessionIdentity();
        voice.onVoiceSession(NOW);
        assertEquals(SessionIdentity.State.UNCERTAIN, voice.state());
        SessionIdentity verified = new SessionIdentity();
        verified.onBiometricSuccess(NOW);
        verified.onVoiceSession(NOW + 10);
        assertEquals("una verificación reciente se mantiene en voz", SessionIdentity.State.VERIFIED_CREATOR, verified.state());
    }

    @Test public void declaredNamesMoveToFamilyOrGuest() {
        SessionIdentity family = new SessionIdentity();
        family.onDeclaredName("ana", "Bryan", FAMILY, NOW);
        assertEquals(SessionIdentity.State.FAMILY, family.state());
        assertEquals("hermana", family.relation());
        assertFalse(family.isCreatorSide());

        SessionIdentity guest = new SessionIdentity();
        guest.onDeclaredName("Lucía", "Bryan", FAMILY, NOW);
        assertEquals(SessionIdentity.State.GUEST, guest.state());
        assertEquals("Lucía", guest.name());
    }

    @Test public void claimingTheCreatorAfterAGuestNeedsVerification() {
        SessionIdentity s = new SessionIdentity();
        s.onNotCreator();
        s.onDeclaredName("Bryan", "Bryan Steven", FAMILY, NOW);
        assertEquals(SessionIdentity.State.UNCERTAIN, s.state());
        assertTrue(s.claimedCreator());
        assertFalse(s.privateAccess(false, NOW));
    }

    @Test public void namesMatchByFirstNameWithoutAccents() {
        assertTrue(SessionIdentity.matches("bryan", "Bryan Steven Rivera"));
        assertTrue(SessionIdentity.matches("Lucía", "LUCIA"));
        assertFalse(SessionIdentity.matches("Bryan Pérez", "Bryan Steven"));
        assertFalse(SessionIdentity.matches("", "Bryan"));
        assertEquals(Arrays.asList("ana"), Arrays.asList(SessionIdentity.normalize(" Ána! ")));
    }

    @Test public void privateCheckpointOnlyWhenItCouldBeTheCreatorAndIsNotVerified() {
        SessionIdentity s = new SessionIdentity();
        assertTrue("presunción + biometría: pedir verificación", s.needsPrivateCheckpoint(true, NOW));
        assertFalse("sin biometría no hay nada que pedir", s.needsPrivateCheckpoint(false, NOW));
        s.onBiometricSuccess(NOW);
        assertFalse("ya verificado", s.needsPrivateCheckpoint(true, NOW + 1000));
        assertTrue("al caducar vuelve a pedirse", s.needsPrivateCheckpoint(true, NOW + SessionIdentity.VERIFICATION_TTL + 1));
        SessionIdentity voice = new SessionIdentity();
        voice.onVoiceSession(NOW);
        assertTrue("voz sin verificar: en duda", voice.needsPrivateCheckpoint(true, NOW));
        SessionIdentity guest = new SessionIdentity();
        guest.onNotCreator();
        assertFalse("un invitado declarado ya recibe solo lo suyo", guest.needsPrivateCheckpoint(true, NOW));
        SessionIdentity family = new SessionIdentity();
        family.onDeclaredName("Ana", "Bryan", FAMILY, NOW);
        assertFalse(family.needsPrivateCheckpoint(true, NOW));
    }
}
