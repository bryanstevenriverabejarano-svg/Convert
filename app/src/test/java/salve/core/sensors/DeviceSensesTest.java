package salve.core.sensors;

import static org.junit.Assert.*;
import org.junit.Test;

/** Sus sentidos físicos: se ofrecen solo si el turno pregunta por algo medible y nunca inventan lecturas. */
public class DeviceSensesTest {
    @Test public void offeredOnlyWhenTheTurnAsksForSomethingThePhoneCanMeasure() {
        assertTrue(DeviceSenses.offered("¿Cuánta batería te queda?"));
        assertTrue(DeviceSenses.offered("Salve, ¿hay luz donde estamos?"));
        assertTrue(DeviceSenses.offered("¿Hacia dónde apunto ahora?"));
        assertTrue(DeviceSenses.offered("¿A qué altitud estamos?"));
        assertTrue(DeviceSenses.offered("¿Qué sensores tienes?"));
        assertTrue(DeviceSenses.offered("¿Cómo está el móvil?"));
        assertTrue(DeviceSenses.offered("¿Me estoy moviendo?"));
        assertFalse(DeviceSenses.offered("Cuéntame un cuento"));
        assertFalse(DeviceSenses.offered("¿Qué temperatura hará mañana en Vitoria?"));
        assertFalse(DeviceSenses.offered("¿Qué sientes por mí?"));
        assertFalse(DeviceSenses.offered("¿Cómo está tu memoria?"));
        assertFalse(DeviceSenses.offered(null));
    }

    @Test public void theGroupComesFromTheModelOrElseFromTheQuestion() {
        assertEquals(DeviceSenses.Group.ENERGIA, DeviceSenses.resolve("¿cuánta batería queda?", "energia"));
        assertEquals(DeviceSenses.Group.ENERGIA, DeviceSenses.resolve("¿cuánta batería queda?", "LO_QUE_SEA"));
        assertEquals(DeviceSenses.Group.ENERGIA, DeviceSenses.resolve("¿cuánta batería queda?", null));
        assertEquals(DeviceSenses.Group.ORIENTACION, DeviceSenses.resolve("¿dónde está el norte?", null));
        assertEquals(DeviceSenses.Group.ENTORNO, DeviceSenses.resolve("¿hay luz aquí?", null));
        assertEquals(DeviceSenses.Group.MOVIMIENTO, DeviceSenses.resolve("¿me estoy moviendo?", null));
        assertEquals(DeviceSenses.Group.SISTEMA, DeviceSenses.resolve("¿cuánto almacenamiento queda?", null));
        assertEquals(DeviceSenses.Group.INVENTARIO, DeviceSenses.resolve("¿qué sensores tienes?", null));
        assertEquals(DeviceSenses.Group.TODO, DeviceSenses.resolve("¿qué percibes de tu entorno?", null));
        assertEquals(DeviceSenses.Group.TODO, DeviceSenses.resolve("batería y luz, por favor", null));
    }

    @Test public void everySensorKindMapsToItsAndroidTypeOnce() {
        java.util.Set<Integer> types = new java.util.HashSet<>();
        for (DeviceSenses.Kind kind : DeviceSenses.Kind.values()) {
            assertTrue(kind.name(), types.add(kind.type));
            assertSame(kind, DeviceSenses.Kind.of(kind.type));
        }
        assertNull(DeviceSenses.Kind.of(9999));
    }

    @Test public void readingsAreInterpretedWithUnitsAndMissingOnesAreSaidSo() {
        DeviceSenses.Perception p = new DeviceSenses.Perception();
        p.present(DeviceSenses.Kind.LIGHT, true);
        p.sample(DeviceSenses.Kind.LIGHT, new float[]{3f});
        p.present(DeviceSenses.Kind.PRESSURE, true);
        p.sample(DeviceSenses.Kind.PRESSURE, new float[]{951f});
        p.present(DeviceSenses.Kind.PROXIMITY, true);          // presente pero sin muestra
        p.present(DeviceSenses.Kind.AMBIENT_TEMPERATURE, false); // este teléfono no lo tiene
        p.present(DeviceSenses.Kind.HUMIDITY, true);
        p.sample(DeviceSenses.Kind.HUMIDITY, new float[]{Float.NaN}); // lectura inválida: no cuenta
        String text = p.render(DeviceSenses.Group.ENTORNO);
        assertTrue(text, text.startsWith("ENTORNO: "));
        assertTrue(text, text.contains("Luz: 3.0 lux (a oscuras)."));
        assertTrue(text, text.contains("Presión atmosférica: 951 hPa"));
        assertTrue(text, text.contains("Proximidad: sin lectura en esta ráfaga."));
        assertTrue(text, text.contains("Temperatura ambiente: no disponible en este teléfono."));
        assertTrue(text, text.contains("Humedad relativa: sin lectura en esta ráfaga."));
        assertFalse(p.has(DeviceSenses.Kind.HUMIDITY));
        // En el resumen completo no se enumeran los sensores que no existen.
        assertFalse(p.render(DeviceSenses.Group.TODO).contains("no disponible en este teléfono"));
        assertFalse("un grupo no pedido no aparece", text.contains("MOVIMIENTO"));
    }

    @Test public void movementPostureAndCompassAreDescribedFromTheVectors() {
        DeviceSenses.Perception p = new DeviceSenses.Perception();
        p.present(DeviceSenses.Kind.LINEAR_ACCELERATION, true);
        p.sample(DeviceSenses.Kind.LINEAR_ACCELERATION, new float[]{.05f, .02f, .01f});
        p.present(DeviceSenses.Kind.GYROSCOPE, true);
        p.sample(DeviceSenses.Kind.GYROSCOPE, new float[]{0f, 2f, 0f});
        p.present(DeviceSenses.Kind.GRAVITY, true);
        p.sample(DeviceSenses.Kind.GRAVITY, new float[]{0f, 0f, 9.81f});
        p.heading(true, -90f);
        String all = p.render(DeviceSenses.Group.TODO);
        assertTrue(all, all.contains("(quieto)"));
        assertTrue(all, all.contains("girando rápido"));
        assertTrue(all, all.contains("tumbado boca arriba"));
        assertTrue(all, all.contains("Rumbo: 270° (oeste)"));
        assertEquals("en vertical", DeviceSenses.posture(0f, 9.8f, 0f));
        assertEquals("tumbado boca abajo (pantalla hacia el suelo)", DeviceSenses.posture(0f, 0f, -9.8f));
        assertTrue(DeviceSenses.posture(9.8f, 0f, 0f).startsWith("apaisado"));
        assertEquals("inclinado, como en la mano", DeviceSenses.posture(3f, 6f, 7f));
        assertEquals("norte", DeviceSenses.compass(359f));
        assertEquals("sureste", DeviceSenses.compass(135f));
        assertEquals("pleno sol", DeviceSenses.light(60000f));
        assertEquals("movimiento brusco", DeviceSenses.movement(4f));
    }

    @Test public void barometerGivesAPlausibleAltitude() {
        assertEquals(0f, DeviceSenses.altitudeMeters(1013.25f), .5f);
        // Unos 951 hPa corresponden a ~530 m con atmósfera estándar.
        assertEquals(530f, DeviceSenses.altitudeMeters(951f), 15f);
        assertTrue(Float.isNaN(DeviceSenses.altitudeMeters(0f)));
    }

    @Test public void systemFactsAndInventoryAreReportedAsMeasured() {
        DeviceSenses.Perception p = new DeviceSenses.Perception();
        p.fact(DeviceSenses.Group.ENERGIA, "Batería: 82 % (descargándose, sin enchufar).");
        p.fact(DeviceSenses.Group.SISTEMA, "RAM libre: 4.2 de 11.3 GB.");
        p.fact(DeviceSenses.Group.SISTEMA, "  ");
        p.inventory("LSM6DSO Accelerometer");
        p.inventory("TCS3407 Light");
        assertEquals("ENERGIA: Batería: 82 % (descargándose, sin enchufar).", p.render(DeviceSenses.Group.ENERGIA));
        assertEquals("INVENTARIO: 2 sensores: LSM6DSO Accelerometer; TCS3407 Light.", p.render(DeviceSenses.Group.INVENTARIO));
        String all = p.render(DeviceSenses.Group.TODO);
        assertTrue(all, all.contains("SISTEMA: RAM libre: 4.2 de 11.3 GB."));
        assertTrue(all, all.endsWith("INVENTARIO: 2 sensores."));
        assertEquals("Sin lecturas: ningún sensor de ese grupo respondió en esta ráfaga.",
                new DeviceSenses.Perception().render(DeviceSenses.Group.MOVIMIENTO));
    }
}
