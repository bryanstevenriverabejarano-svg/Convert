package salve.core.identity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Locale;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Guards the actual resource spoken by MainActivity's legacy presentation shortcut. */
public class CapabilityPresentationTest {
    @Test
    public void presentationRequiresConfiguredCapabilitiesAndDoesNotClaimPhysicalControl() throws Exception {
        File resource = new File("src/main/res/values/strings.xml");
        if (!resource.isFile()) resource = new File("app/src/main/res/values/strings.xml");
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        NodeList strings = factory.newDocumentBuilder().parse(resource).getElementsByTagName("string");
        String presentation = null;
        for (int i = 0; i < strings.getLength(); i++) {
            Element entry = (Element) strings.item(i);
            if ("salve_gestiona".equals(entry.getAttribute("name"))) presentation = entry.getTextContent();
        }
        assertNotNull("The spoken capability description must exist", presentation);
        String text = presentation.toLowerCase(Locale.ROOT);
        assertTrue(FunctionalIdentityPolicy.isAllowed(presentation));
        assertTrue("Capabilities must depend on configured models", text.contains("modelos"));
        assertTrue("Device access must depend on permissions", text.contains("permisos"));
        assertFalse("A product ambition is not an established capability", text.contains("superinteligente"));
        assertFalse("No verified traffic control integration exists", text.contains("regulo el tráfico"));
        assertFalse("No verified Smart Towers control integration exists", text.contains("gestiono las smart towers"));
    }
}
