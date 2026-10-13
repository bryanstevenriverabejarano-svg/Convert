package salve.core.devicecontrol;
import org.junit.Test;
import salve.core.web.SearchEngine;
import static org.junit.Assert.*;
public class MobileBrowserPolicyTest {
    @Test public void realBrowserSearchUsesHtmlInsteadOfRssAndEncodesQuery() {
        String bing = MobileBrowserPolicy.searchUrl(new SearchEngine("bing", ""), "colada & frío");
        assertTrue(bing.contains("colada+%26+fr%C3%ADo")); assertFalse(bing.contains("format=rss"));
        assertTrue(MobileBrowserPolicy.searchUrl(new SearchEngine("duckduckgo", ""), "colada").startsWith("https://duckduckgo.com/"));
    }
    @Test public void acceptsCustomEnginesAndDirectHttpsPages() {
        assertEquals("https://www.example.org/?s=colada", MobileBrowserPolicy.searchUrl(new SearchEngine("custom", "https://www.example.org/?s={query}"), "colada"));
        assertEquals("https://www.google.com/search?q=colada", MobileBrowserPolicy.searchUrl(new SearchEngine("bing", ""), "https://www.google.com/search?q=colada"));
    }
    @Test public void blocksAppIntentsLocalFilesInsecureAndLocalDestinations() {
        for (String url : new String[]{"intent://send#Intent;end", "javascript:alert(1)", "file:///sdcard/file", "content://salve/memories", "http://google.com", "https://127.0.0.1", "https://localhost", "https://192.168.1.1", "https://user:password@google.com/"})
            assertFalse(url, MobileBrowserPolicy.allowed(url));
        assertTrue(MobileBrowserPolicy.allowed("https://www.google.com/search?q=hello"));
    }
}
