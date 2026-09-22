package salve.core.conversation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class DeviceClockContextTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-21T23:45:00Z"), ZoneOffset.UTC);

    @Test public void usesActualInstantAndConfiguredDeviceZone() {
        DeviceClockContext context = new DeviceClockContext(clock, () -> ZoneId.of("America/Bogota"));
        assertEquals("Según el reloj del teléfono, son las 18:45 (zona America/Bogota, UTC-05:00).",
                context.directReply("¿Qué hora es?"));
        assertTrue(context.promptContext().contains("2026-09-21T18:45:00-05:00"));
        assertTrue(context.promptContext().contains("no demuestra la ubicación"));
    }

    @Test public void dateRespectsLocalDayBoundary() {
        DeviceClockContext context = new DeviceClockContext(clock, () -> ZoneId.of("Europe/Madrid"));
        assertEquals("Según el calendario del teléfono, hoy es 22/09/2026 (zona Europe/Madrid, UTC+02:00).",
                context.directReply("¿Qué fecha es hoy?"));
    }

    @Test public void readsTimezoneAgainAfterSettingsChange() {
        AtomicReference<ZoneId> zone = new AtomicReference<>(ZoneId.of("Europe/Madrid"));
        DeviceClockContext context = new DeviceClockContext(clock, zone::get);
        assertTrue(context.directReply("dime la hora").contains("01:45"));
        zone.set(ZoneId.of("America/Bogota"));
        assertTrue(context.directReply("dime la hora").contains("18:45"));
        assertTrue(context.promptContext().contains("zona configurada=America/Bogota"));
    }

    @Test public void usesTimezoneRulesAcrossDaylightSavingChanges() {
        DeviceClockContext winter = new DeviceClockContext(
                Clock.fixed(Instant.parse("2026-01-15T12:00:00Z"), ZoneOffset.UTC),
                () -> ZoneId.of("Europe/Madrid"));
        DeviceClockContext summer = new DeviceClockContext(
                Clock.fixed(Instant.parse("2026-07-15T12:00:00Z"), ZoneOffset.UTC),
                () -> ZoneId.of("Europe/Madrid"));
        assertTrue(winter.directReply("hora actual").contains("13:00"));
        assertTrue(winter.directReply("hora actual").contains("UTC+01:00"));
        assertTrue(summer.directReply("hora actual").contains("14:00"));
        assertTrue(summer.directReply("hora actual").contains("UTC+02:00"));
    }

    @Test public void leavesOtherPlacesAndAmbiguousLocalReferencesToConversation() {
        DeviceClockContext context = new DeviceClockContext(clock, () -> ZoneOffset.UTC);
        for (String question : new String[]{"¿Qué hora es en Tokio?", "¿Qué hora es aquí?",
                "¿Qué hora es allí?", "¿Qué hora es en mi ubicación?", "¿Qué fecha es en Bogotá?"}) {
            assertNull(question, context.directReply(question));
        }
    }

    @Test public void doesNotSwallowEventsMixedRequestsOrHypotheticals() {
        DeviceClockContext context = new DeviceClockContext(clock, () -> ZoneOffset.UTC);
        for (String question : new String[]{"¿A qué hora es mi reunión?", "¿Qué hora es y qué recuerdas?",
                "Dime la hora y la fecha", "¿Qué día es Navidad?", "Si pregunto qué hora es, ¿qué dirías?",
                "Estoy en Bogotá. ¿Qué hora es?", "Dije: qué hora es", "No me digas qué hora es"}) {
            assertNull(question, context.directReply(question));
        }
    }

    @Test public void acceptsPoliteSimpleQuestionAndLabelsUtc() {
        DeviceClockContext context = new DeviceClockContext(clock, () -> ZoneOffset.UTC);
        assertTrue(context.directReply("Dime la hora, por favor.").contains("23:45"));
        assertTrue(context.directReply("¿Qué hora es?").contains("UTC+00:00"));
        assertNull(context.directReply(null));
        assertNull(context.directReply(""));
    }
}
