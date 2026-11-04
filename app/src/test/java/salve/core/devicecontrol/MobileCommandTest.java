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

        MobileCommand accented = MobileCommand.parse("abre el chat de Mami y escríbele que la quiero mucho");
        assertNotNull(accented);
        assertEquals("Te quiero mucho", accented.message);
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

    @Test public void dictatedWhatsAppPhrasings() {
        MobileCommand bryan = MobileCommand.parse("Abre whasa y en el chat de mami dile que la quiero");
        assertNotNull(bryan);
        assertEquals(MobileCommand.Type.WHATSAPP, bryan.type);
        assertEquals("mami", bryan.contact);
        assertEquals("Te quiero", bryan.message);

        MobileCommand tell = MobileCommand.parse("en WhatsApp dile a mamá que llego tarde");
        assertNotNull(tell);
        assertEquals("mamá", tell.contact);
        assertEquals("Llego tarde", tell.message);

        MobileCommand by = MobileCommand.parse("dile a Ana por guasap que ya salgo");
        assertNotNull(by);
        assertEquals("Ana", by.contact);
        assertEquals("Ya salgo", by.message);

        MobileCommand noAnd = MobileCommand.parse("abre el chat de Mami dile que la quiero mucho");
        assertEquals("Te quiero mucho", noAnd.message);

        assertNull("sin WhatsApp no es un borrador", MobileCommand.parse("dile a Ana que ya salgo"));
        assertNull("nunca enviar", MobileCommand.parse("envía un wasap a mami diciendo que la quiero"));
    }
}
