package salve.avatar;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AvatarDesignRequestTest {
    private String valid() {
        return "{\"tool\":\"AVATAR_CREATE\",\"name\":\"Noche lavanda\",\"template\":\"pajamas\",\"color\":\"LAVENDER\",\"pattern\":\"STARS\",\"wear\":true}";
    }
    @Test public void decodesBoundedTemplateRecipe() {
        AvatarDesignRequest request = AvatarDesignRequest.parse(valid());
        assertEquals(AvatarDesignRequest.Kind.CREATE, request.kind);
        assertEquals("pajamas", request.template); assertTrue(request.wear);
        assertEquals(AvatarDesignSpec.Palette.LAVENDER, request.palette);
        assertEquals(AvatarDesignSpec.Pattern.STARS, request.pattern);
    }
    @Test public void acceptsExactNameSelectorsAndOriginalId() {
        assertEquals("Noche lavanda", AvatarDesignRequest.parse("{\"tool\":\"AVATAR_WEAR\",\"name\":\"Noche lavanda\"}").name);
        assertEquals("original", AvatarDesignRequest.parse("{\"tool\":\"AVATAR_WEAR\",\"id\":\"original\"}").id);
        assertEquals(AvatarDesignRequest.Kind.LIST, AvatarDesignRequest.parse("{\"tool\":\"AVATAR_LIST\"}").kind);
    }
    @Test public void rejectsDuplicatesExtraFieldsAndTrailingCommands() {
        reject(valid().replace("\"wear\":true", "\"wear\":true,\"wear\":false"));
        reject(valid().replace("\"wear\":true", "\"wear\":true,\"code\":\"run()\""));
        reject(valid() + "{}");
        reject("{\"tool\":\"AVATAR_WEAR\",\"id\":\"original\",\"name\":\"otro\"}");
        reject("{\"tool\":\"AVATAR_LIST\",\"url\":\"https://example.com\"}");
    }
    @Test public void rejectsUnknownResourcesAndWrongTypes() {
        reject(valid().replace("pajamas", "../../other.png"));
        reject(valid().replace("pajamas", "https://example.com/image.png"));
        reject(valid().replace("LAVENDER", "#FFFFFF"));
        reject(valid().replace("STARS", "EXECUTE"));
        reject(valid().replace("true", "\"true\""));
        reject(valid().replace("\"Noche lavanda\"", "null"));
        reject("{\"tool\":\"AVATAR_DELETE\",\"id\":\"../original\"}");
    }
    @Test public void rejectsControlTextAndOversizedNames() {
        reject(valid().replace("Noche lavanda", "ropa<script>"));
        reject(valid().replace("Noche lavanda", new String(new char[49]).replace('\0', 'a')));
        reject(new String(new char[4097]).replace('\0', ' '));
        assertEquals("Café", AvatarDesignSpec.validateName(" Cafe\u0301 "));
    }
    @Test public void originalColorNeverRequestsATint() {
        assertEquals(0, AvatarDesignSpec.original().color);
        assertEquals(AvatarDesignSpec.Pattern.NONE, AvatarDesignSpec.original().pattern);
        try {
            new AvatarDesignSpec("original", "Vestido original", "pajamas", AvatarDesignSpec.Palette.BLUE, AvatarDesignSpec.Pattern.STARS);
            fail("Original must remain recoverable");
        } catch (IllegalArgumentException expected) { }
    }
    private void reject(String json) {
        try { AvatarDesignRequest.parse(json); fail("Should reject " + json); }
        catch (IllegalArgumentException expected) { }
    }
}
