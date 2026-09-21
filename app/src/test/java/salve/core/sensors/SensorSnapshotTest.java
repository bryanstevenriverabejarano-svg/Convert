package salve.core.sensors;

import org.junit.Test;

import static org.junit.Assert.*;
import static salve.core.sensors.SensorSnapshot.Reading.*;

public class SensorSnapshotTest {
    @Test public void missingSensorsNeverBecomeZeroReadings() {
        SensorSnapshot snapshot = new SensorSnapshot();
        String text = snapshot.describe(100);
        assertTrue(text.contains("Luz: no disponible"));
        assertFalse(text.contains("0.0"));
        assertFalse(snapshot.record(LIGHT, 42, 100));
        assertFalse(snapshot.hasFreshReading(LIGHT, 100));
    }

    @Test public void registeredSensorNeedsAnActualSample() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(LIGHT, true);
        assertTrue(snapshot.describe(100).contains("Luz: sin lectura reciente"));
        assertFalse(snapshot.hasFreshReading(LIGHT, 100));
    }

    @Test public void zeroLuxIsValidButOnlyWhenMeasured() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(LIGHT, true);
        assertTrue(snapshot.record(LIGHT, 0, 100));
        assertTrue(snapshot.describe(200).contains("0.0 lux (hace 0.1 s)"));
    }

    @Test public void measurementsExpireRatherThanBeingReportedAsCurrent() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(PROXIMITY, true);
        snapshot.record(PROXIMITY, 5, 100);
        assertTrue(snapshot.hasFreshReading(PROXIMITY, 5_100));
        assertFalse(snapshot.hasFreshReading(PROXIMITY, 5_101));
        assertTrue(snapshot.describe(5_101).contains("Proximidad: sin lectura reciente"));
        assertFalse(snapshot.describe(5_101).contains("5.0 cm"));
    }

    @Test public void negativeAmbientTemperatureIsValid() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(AMBIENT_TEMPERATURE, true);
        assertTrue(snapshot.record(AMBIENT_TEMPERATURE, -8.5, 100));
        assertTrue(snapshot.describe(100).contains("-8.5 °C"));
        assertFalse(snapshot.record(AMBIENT_TEMPERATURE, -274, 101));
    }

    @Test public void invalidValuesClearPriorSamples() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(ACCELERATION, true);
        snapshot.record(ACCELERATION, 9.8, 100);
        assertFalse(snapshot.record(ACCELERATION, Double.NaN, 101));
        assertFalse(snapshot.hasFreshReading(ACCELERATION, 101));
        assertFalse(snapshot.record(ACCELERATION, Double.POSITIVE_INFINITY, 101));
        assertFalse(snapshot.record(ACCELERATION, -1, 101));
        assertFalse(snapshot.record(ACCELERATION, 9.8, -1));
    }

    @Test public void batteryPercentageValidatesBothInputsWithoutOverflow() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(BATTERY, true);
        assertTrue(snapshot.recordBattery(1, 2, 100));
        assertTrue(snapshot.describe(100).contains("Batería: 50.0 %"));
        assertTrue(snapshot.recordBattery(Integer.MAX_VALUE, Integer.MAX_VALUE, 101));
        assertTrue(snapshot.describe(101).contains("Batería: 100.0 %"));
        assertFalse(snapshot.recordBattery(50, 0, 102));
        assertFalse(snapshot.hasFreshReading(BATTERY, 102));
        assertFalse(snapshot.recordBattery(-1, 100, 102));
        assertFalse(snapshot.recordBattery(101, 100, 102));
        assertFalse(snapshot.recordBattery(0, -1, 102));
    }

    @Test public void unavailableSensorAndSessionClearDiscardAllData() {
        SensorSnapshot snapshot = new SensorSnapshot();
        snapshot.setAvailable(LIGHT, true);
        snapshot.record(LIGHT, 100, 100);
        snapshot.setAvailable(LIGHT, false);
        assertFalse(snapshot.hasFreshReading(LIGHT, 100));
        snapshot.setAvailable(LIGHT, true);
        assertFalse(snapshot.hasFreshReading(LIGHT, 100));
        snapshot.record(LIGHT, 200, 101);
        snapshot.clear();
        assertFalse(snapshot.hasFreshReading(LIGHT, 101));
        assertFalse(snapshot.describe(101).contains("200.0 lux"));
    }
}
