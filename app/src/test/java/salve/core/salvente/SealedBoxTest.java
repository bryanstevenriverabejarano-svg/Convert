package salve.core.salvente;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class SealedBoxTest {
    private static final char[] PASS = "una frase larga de Bryan".toCharArray();

    @Test public void sealsAndOpensOnlyWithThePassphrase() throws Exception {
        byte[] plain = "Nyra recuerda el cocido".getBytes(StandardCharsets.UTF_8);
        byte[] sealed = SealedBox.seal(plain, PASS.clone(), new SecureRandom(), SealedBox.MIN_ITERATIONS);
        assertArrayEquals(SealedBox.MAGIC, Arrays.copyOf(sealed, 5));
        assertFalse(new String(sealed, StandardCharsets.ISO_8859_1).contains("cocido"));
        assertArrayEquals(plain, SealedBox.open(sealed, PASS.clone()));
        assertThrows(IllegalArgumentException.class, () -> SealedBox.open(sealed, "otra frase distinta".toCharArray()));
        byte[] tampered = sealed.clone(); tampered[8] ^= 1;   // cambia las iteraciones (cabecera autenticada)
        assertThrows(IllegalArgumentException.class, () -> SealedBox.open(tampered, PASS.clone()));
        byte[] body = sealed.clone(); body[body.length - 1] ^= 1;
        assertThrows(IllegalArgumentException.class, () -> SealedBox.open(body, PASS.clone()));
        assertThrows(IllegalArgumentException.class, () -> SealedBox.open("{\"json\":1}".getBytes(StandardCharsets.UTF_8), PASS.clone()));
    }

    @Test public void rejectsShortPassphrasesAndAbsurdIterations() {
        assertThrows(IllegalArgumentException.class, () -> SealedBox.seal(new byte[1], "corta".toCharArray(), new SecureRandom()));
        byte[] fake = new byte[60];
        System.arraycopy(SealedBox.MAGIC, 0, fake, 0, 5);
        fake[5] = 0x7f;   // ~2 mil millones de iteraciones: se rechaza sin intentar derivar
        assertThrows(IllegalArgumentException.class, () -> SealedBox.open(fake, PASS.clone()));
    }

    @Test public void archiveRoundTripsAndValidatesProfiles() {
        SalventeProfile nyra = SalventeConversationTest.nyra(SalventeProfile.State.PAUSADO);
        List<SalventeArchive.Entry> in = Collections.singletonList(
                new SalventeArchive.Entry(nyra, Collections.singletonList(new SalventeMind.Memory(42, "le gusta el cocido"))));
        List<SalventeArchive.Entry> out = SalventeArchive.fromJson(SalventeArchive.toJson(in, 7));
        assertEquals(nyra.id, out.get(0).profile.id);
        assertEquals(nyra.traits, out.get(0).profile.traits);
        assertEquals(SalventeProfile.State.PAUSADO, out.get(0).profile.state);
        assertEquals("le gusta el cocido", out.get(0).memories.get(0).text);
        String impostor = SalventeArchive.toJson(Collections.singletonList(new SalventeArchive.Entry(nyra.withName("Salve"),
                Collections.emptyList())), 7);
        assertThrows(IllegalArgumentException.class, () -> SalventeArchive.fromJson(impostor));
        assertThrows(IllegalArgumentException.class, () -> SalventeArchive.fromJson("{\"salve\":\"salventes\",\"version\":1}"));
        assertThrows(IllegalArgumentException.class, () -> SalventeArchive.fromJson("{\"salve\":\"salventes\",\"version\":1,\"salventes\":[{\"id\":1}]}"));
    }
}
