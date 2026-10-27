package salve.core.devicecontrol;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class AppNameMatcherTest {
    private static final List<String> LABELS = Arrays.asList(
            "Cámara", "WhatsApp", "WhatsApp Business", "Ajustes", "Spotify", "Fotos", "Teléfono", "Reloj");
    private static final List<String> PACKAGES = Arrays.asList(
            "com.android.camera", "com.whatsapp", "com.whatsapp.w4b", "com.android.settings",
            "com.spotify.music", "com.google.android.apps.photos", "com.google.android.dialer", "com.google.android.deskclock");

    private static String pick(String spoken) {
        int i = AppNameMatcher.bestMatch(spoken, LABELS, PACKAGES);
        return i < 0 ? null : LABELS.get(i);
    }

    @Test public void spokenNamesFindTheInstalledApp() {
        assertEquals("Cámara", pick("la cámara"));
        assertEquals("Cámara", pick("la app de la camara"));
        assertEquals("WhatsApp", pick("WhatsApp"));
        assertEquals("WhatsApp Business", pick("whatsapp business"));
        assertEquals("Ajustes", pick("configuración"));
        assertEquals("Fotos", pick("galería"));
        assertEquals("Teléfono", pick("el teléfono"));
        assertEquals("Reloj", pick("alarma"));
    }

    @Test public void englishLabelsStillMatchSpanishWords() {
        List<String> labels = Arrays.asList("Camera", "Settings", "Calculator");
        List<String> pkgs = Arrays.asList("com.sec.android.app.camera", "com.android.settings", "com.sec.android.app.popupcalculator");
        assertEquals(0, AppNameMatcher.bestMatch("cámara", labels, pkgs));
        assertEquals(1, AppNameMatcher.bestMatch("ajustes", labels, pkgs));
        assertEquals(2, AppNameMatcher.bestMatch("calculadora", labels, pkgs));
    }

    @Test public void unknownOrAmbiguousNamesOpenNothing() {
        assertNull(pick("la puerta"));
        assertEquals(-1, AppNameMatcher.bestMatch("notas", Arrays.asList("Notas", "Notas"), Arrays.asList("a.b", "c.d")));
        assertNull(pick(""));
    }
}
