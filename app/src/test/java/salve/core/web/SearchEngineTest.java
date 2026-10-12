package salve.core.web;
import org.junit.Test;
import static org.junit.Assert.*;

public class SearchEngineTest {
    @Test public void decodesGoogleAndDuckRedirectsAndKeepsCaseSensitiveTargets() {
        assertEquals("https://example.org/Case?x=1", SearchEngine.links("<a href='/url?q=https%3A%2F%2Fexample.org%2FCase%3Fx%3D1&amp;sa=U'>result</a>",
                "https://www.google.com/search?q=x").get(0));
        assertEquals("https://example.org/ok",SearchEngine.links("<a href='//duckduckgo.com/l/?uddg=https%3A%2F%2Fexample.org%2Fok'>result</a>",
                "https://html.duckduckgo.com/html/?q=x").get(0));
    }
    @Test public void rejectedLinksCannotEscapeIntoLocalNetworkOrScripts() {
        assertTrue(SearchEngine.links("<a href='/url?q=https%3A%2F%2F127.0.0.1%2Fprivate'>local</a><a href='javascript:alert(1)'>run</a>",
                "https://www.google.com/search?q=x").isEmpty());
    }
    @Test public void customQueryIsEncodedAndTemplateMustBePublicHttps() {
        SearchEngine custom = new SearchEngine("custom","https://search.example.org/?q={query}");
        assertTrue(custom.url("hola & secreto").contains("hola+%26+secreto"));
        for(String bad:new String[]{"http://example.org/?q={query}","https://127.0.0.1/?q={query}","https://user:pass@example.org/?q={query}","https://example.org/"})
            assertThrows(IllegalArgumentException.class,()->new SearchEngine("custom",bad));
    }
    @Test(timeout=2000) public void malformedHtmlHasBoundedParsing() {
        assertTrue(SearchEngine.links(String.join("",java.util.Collections.nCopies(100000,"<a ")),"https://www.google.com/search?q=x").isEmpty());
    }
}
