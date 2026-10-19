package salve.data.salvente;

import android.app.Application;
import android.content.Context;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import salve.core.salvente.SalventeGenesisEngine;
import salve.core.salvente.SalventeGenesisSession;
import salve.core.salvente.SalventeProfile;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28, application = Application.class)
public class SalventeRepositoryTest {
    /** Mismo contrato que Android Keystore (AES-GCM con el id como AAD), en memoria. */
    static final class SoftVault implements SalventeKeyVault {
        final Map<String, SecretKey> keys = new HashMap<>();
        public void create(String id) throws Exception { KeyGenerator g = KeyGenerator.getInstance("AES"); g.init(256); keys.put(id, g.generateKey()); }
        public boolean exists(String id) { return keys.containsKey(id); }
        public Sealed seal(String id, byte[] plain) throws Exception {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE, key(id)); c.updateAAD(id.getBytes(StandardCharsets.UTF_8));
            return new Sealed(c.getIV(), c.doFinal(plain));
        }
        public byte[] open(String id, byte[] iv, byte[] data) throws Exception {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE, key(id), new GCMParameterSpec(128, iv));
            c.updateAAD(id.getBytes(StandardCharsets.UTF_8)); return c.doFinal(data);
        }
        public void destroy(String id) { keys.remove(id); }
        SecretKey key(String id) { SecretKey k = keys.get(id); if (k == null) throw new IllegalStateException("sin clave"); return k; }
    }

    Context context; SalventeDatabase db; SoftVault vault; SalventeRepository repo; long now = 5_000;

    @Before public void setup() {
        context = RuntimeEnvironment.getApplication(); context.deleteDatabase("salventes-test");
        db = SalventeDatabase.builder(context, "salventes-test").allowMainThreadQueries().build();
        vault = new SoftVault(); repo = new SalventeRepository(db, vault, () -> now);
    }

    @After public void close() { db.close(); context.deleteDatabase("salventes-test"); }

    SalventeProfile approve(String purpose, String name) {
        SalventeGenesisSession s = new SalventeGenesisSession(new SalventeGenesisEngine(null, SalventeGenesisEngine::newId, () -> now), repo);
        s.propose(purpose, name);
        return s.approve();
    }

    @Test public void approvalPersistsProfileWithKeyAndRoundTripsTraits() {
        SalventeProfile nyra = approve("acompañar a la abuela y contarle cuentos", "Nyra");
        assertTrue(vault.exists(nyra.id));
        SalventeProfile stored = repo.get(nyra.id);
        assertEquals("Nyra", stored.name);
        assertEquals(nyra.traits, stored.traits);
        assertEquals(nyra.capabilities, stored.capabilities);
        assertEquals(SalventeProfile.SPECIES, stored.species);
        assertEquals(Collections.singletonList("Nyra"), repo.names());
    }

    @Test public void cancelledGenesisWritesNothing() {
        SalventeGenesisSession s = new SalventeGenesisSession(new SalventeGenesisEngine(null, SalventeGenesisEngine::newId, () -> now), repo);
        s.propose("ayudar con los deberes", "Kael");
        s.cancel();
        assertTrue(repo.all().isEmpty());
        assertTrue(vault.keys.isEmpty());
    }

    @Test public void duplicateNameIsRejectedAndLeavesNoOrphanKey() {
        approve("acompañar a la abuela", "Nyra");
        SalventeGenesisSession s = new SalventeGenesisSession(new SalventeGenesisEngine(null, SalventeGenesisEngine::newId, () -> now), repo);
        s.propose("ayudar con los deberes", "NYRÁ");   // el motor cambia el nombre repetido
        assertNotEquals("Nyra", s.draft().profile.name);
        assertEquals(1, vault.keys.size());
    }

    @Test public void sameIdentityIsRejectedBeforeAnyNewKey() {
        SalventeProfile nyra = approve("acompañar a la abuela", "Nyra");
        // Misma identidad otra vez: el gestor la rechaza antes de crear clave.
        assertThrows(IllegalArgumentException.class, () -> repo.create(nyra));
        assertEquals(1, vault.keys.size());
    }

    @Test public void memoriesAreEncryptedIsolatedAndCryptoShreddedOnDelete() {
        SalventeProfile nyra = approve("acompañar a la abuela", "Nyra");
        SalventeProfile kael = approve("ayudar con los deberes", "Kael");
        now = 6_000; repo.remember(nyra.id, "A la abuela le encanta el cocido");
        now = 7_000; repo.remember(nyra.id, "Mañana tiene médico");
        List<SalventeRepository.Memory> mine = repo.memories(nyra.id, 10);
        assertEquals("Mañana tiene médico", mine.get(0).text);
        assertTrue(repo.memories(kael.id, 10).isEmpty());
        SalventeMemoryEntity raw = db.dao().memories(nyra.id, 1).get(0);
        assertFalse(new String(raw.ciphertext, StandardCharsets.ISO_8859_1).contains("médico"));
        // El cifrado está atado a su dueño: con la clave o el id de otro no se abre.
        assertThrows(Exception.class, () -> vault.open(kael.id, raw.iv, raw.ciphertext));

        assertTrue(repo.delete(nyra.id));
        assertFalse(vault.exists(nyra.id));
        assertEquals(0, repo.memoryCount(nyra.id));
        assertNull(repo.get(nyra.id));
        assertNotNull(repo.get(kael.id));
    }

    @Test public void memoriesAreCappedPerSalvente() {
        SalventeProfile nyra = approve("acompañar a la abuela", "Nyra");
        for (int i = 0; i < SalventeRepository.MAX_MEMORIES + 5; i++) { now++; repo.remember(nyra.id, "recuerdo " + i); }
        assertEquals(SalventeRepository.MAX_MEMORIES, repo.memoryCount(nyra.id));
        assertEquals("recuerdo " + (SalventeRepository.MAX_MEMORIES + 4), repo.memories(nyra.id, 1).get(0).text);
    }

    @Test public void pauseKeepsIdentity() {
        SalventeProfile nyra = approve("acompañar a la abuela", "Nyra");
        assertTrue(repo.setState(nyra.id, SalventeProfile.State.PAUSADO));
        assertEquals(SalventeProfile.State.PAUSADO, repo.get(nyra.id).state);
        assertEquals(nyra.id, repo.get(nyra.id).id);
    }
}
