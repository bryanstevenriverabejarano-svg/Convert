package salve.core.devicecontrol;
import org.junit.Test;
import static org.junit.Assert.*;

public class MobileCommandTest {
    @Test public void exactCommandsOpenPanelOrSearch() {
        assertEquals(MobileCommand.Type.PANEL, MobileCommand.parse("Salve, abre tu panel flotante").type);
        MobileCommand search = MobileCommand.parse("busca en una mini ventana: en Google qué es la colada");
        assertEquals(MobileCommand.Type.SEARCH, search.type); assertEquals("en Google qué es la colada", search.query);
    }
    @Test public void messageKeepsExactContentAndRequiresExplicitPhone() {
        MobileCommand command = MobileCommand.parse("prepara un whatsapp a +34 612-345-678: Hola, Ana.\n¿Llegas a las 8?");
        assertEquals("+34612345678", command.draft.getPhone());
        assertEquals("Hola, Ana.\n¿Llegas a las 8?", command.draft.getMessage());
        assertNull(command.contact);
    }
    @Test public void contactDraftKeepsNameAndExactText() {
        MobileCommand command = MobileCommand.parse("escribe un whatsapp a mamá: Llego en 10 minutos");
        assertEquals(MobileCommand.Type.WHATSAPP, command.type);
        assertNull(command.draft);
        assertEquals("mamá", command.contact);
        assertEquals("Llego en 10 minutos", command.message);
        assertNull(MobileCommand.parse("escribe un whatsapp a Ana:"));
    }

    @Test public void chainedChatRequestPreparesDraftWithoutSending() {
        MobileCommand command = MobileCommand.parse(
                "Salve, abre el chat de Mami y escribirle que la quiero mucho");
        assertNotNull(command);
        assertEquals(MobileCommand.Type.WHATSAPP, command.type);
        assertNull(command.draft);
        assertEquals("Mami", command.contact);
        assertEquals("Te quiero mucho", command.message);

        MobileCommand withWhatsApp = MobileCommand.parse(
                "abre WhatsApp y abre el chat de mamá y escribe Llegaré a las ocho");
        assertNotNull(withWhatsApp);
        assertEquals("mamá", withWhatsApp.contact);
        assertEquals("Llegaré a las ocho", withWhatsApp.message);
    }

    @Test public void chainedRequestDoesNotImplicitlySendOrGuessRecipient() {
        assertNull(MobileCommand.parse("abre el chat de Mami y envía que la quiero mucho"));
        assertNull(MobileCommand.parse("abre el chat de Mami y escribe"));
        assertNull(MobileCommand.parse("abre el chat de y escribe hola"));
        assertNull(MobileCommand.parse("no abras el chat de Mami y escribe hola"));
    }

    @Test public void discussionNegationAndSendAreNotDraftCommands() {
        for (String value : new String[]{"no abre el panel flotante", "¿Puedes abrir ventanas?", "envía un whatsapp a +34612345678: hola", "no prepara un whatsapp a +34612345678: hola", "busca en mini ventana:"})
            assertNull(value, MobileCommand.parse(value));
    }
}
