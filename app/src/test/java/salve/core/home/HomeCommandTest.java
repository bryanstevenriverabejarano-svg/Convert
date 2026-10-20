package salve.core.home;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public class HomeCommandTest {
    static final List<HomeCommand.Entity> HOUSE = Arrays.asList(
            new HomeCommand.Entity("light.salon", "Luz salón", "light", "off"),
            new HomeCommand.Entity("light.cocina", "Luz cocina", "light", "on"),
            new HomeCommand.Entity("switch.cafetera", "Enchufe cafetera", "switch", "off"),
            new HomeCommand.Entity("cover.dormitorio", "Persiana dormitorio", "cover", "open"),
            new HomeCommand.Entity("climate.casa", "Calefacción", "climate", "heat"),
            new HomeCommand.Entity("scene.cine", "Modo cine", "scene", "scening"));

    @Test public void understandsHomeOrdersOnly() {
        HomeCommand c = HomeCommand.parse("Salve, enciende la luz del salón");
        assertEquals(HomeCommand.Action.ON, c.action);
        assertEquals("light.salon", c.resolve(HOUSE).entity.id);
        assertEquals("¿Enciendo «Luz salón»?", c.confirmation(c.resolve(HOUSE).entity));
        assertEquals("cover.dormitorio", HomeCommand.parse("baja la persiana del dormitorio").resolve(HOUSE).entity.id);
        assertEquals(HomeCommand.Action.CLOSE, HomeCommand.parse("baja la persiana del dormitorio").action);
        HomeCommand t = HomeCommand.parse("pon la calefacción a 21,5 grados");
        assertEquals(21.5, t.value, 1e-9);
        assertEquals("¿Pongo «Calefacción» a 21,5 grados?", t.confirmation(t.resolve(HOUSE).entity));
        HomeCommand b = HomeCommand.parse("baja la luz de la cocina al 30%");
        assertEquals(HomeCommand.Action.BRIGHTNESS, b.action);
        assertEquals("light.cocina", b.resolve(HOUSE).entity.id);
        assertEquals("switch.cafetera", HomeCommand.parse("apaga el enchufe de la cafetera").resolve(HOUSE).entity.id);
        assertEquals("scene.cine", HomeCommand.parse("activa la escena cine").resolve(HOUSE).entity.id);
    }

    @Test public void ignoresPhoneAndAppOrders() {
        for (String other : new String[]{"enciende la linterna", "activa el modo coche", "abre spotify", "abre maps",
                "apaga la música", "baja el volumen", "pon música de Queen", "cierra la app", "activa la verificación",
                "pon la calefacción a 80 grados", "sube la luz al 300%", "¿está encendida la luz?"})
            assertNull(other, HomeCommand.parse(other));
    }

    @Test public void ambiguousOrdersAskInsteadOfGuessing() {
        HomeCommand.Match m = HomeCommand.parse("apaga las luces").resolve(HOUSE);
        assertNull(m.entity);
        assertEquals(2, m.candidates.size());
        assertNull(HomeCommand.parse("enciende el ventilador").resolve(HOUSE).entity);   // no hay ventilador
    }
}
