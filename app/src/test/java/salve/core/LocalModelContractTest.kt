package salve.core

import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Message
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class LocalModelContractTest {
    private fun catalog(): File = File("src/main/assets/config/models.json").takeIf { it.exists() }
        ?: File("app/src/main/assets/config/models.json")

    @Test fun catalogPinsPrimaryAndFallbackWithoutBundledWeights() {
        val items = ModelDownloader.loadItems(catalog().inputStream())
        assertEquals(listOf(LocalModelPolicy.PRIMARY, LocalModelPolicy.FALLBACK), items.map { it.id })
        val primary = items.first()
        assertEquals(2019382400L, primary.sizeBytes)
        assertEquals("5d6d02eeefa1ab5dbf23f97afdf5c2c95ad3d946dc3b6e9ab72e6c1637d54177", primary.sha256)
        assertTrue(primary.url.contains("/resolve/ac6b1ee98e3864ebd5998216f800a07d74b166b5/"))
        assertFalse(primary.supportsVision)
        val item = items.single { it.id == LocalModelPolicy.FALLBACK }
        assertEquals("Gemma 4 E2B", item.id)
        assertEquals(2588147712L, item.sizeBytes)
        assertEquals("181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c", item.sha256)
        assertTrue(item.url.contains("/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/"))
        assertTrue(item.supportsVision)
        assertEquals("Apache-2.0", item.license)
        assertNull("Unknown hardware must stay unknown", item.ramBytes)
        val bundled = catalog().parentFile!!.parentFile!!.walkTopDown().filter { it.isFile }
            .any { it.extension in setOf("litertlm", "task", "gguf") }
        assertFalse("Language model weights must not enter APK assets", bundled)
    }

    @Test fun rejectsUnpinnedOrUncheckedCatalog() {
        val json = catalog().readText()
        for (invalid in listOf(json.replace("6e5c4f1e395deb959c494953478fa5cec4b8008f", "main"),
            json.replace("181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c", ""),
            json.replace("gemma-4-E2B-it-6e5c4f1.litertlm", "../model.litertlm"))) {
            assertThrows(IllegalArgumentException::class.java) { ModelDownloader.loadItems(invalid.byteInputStream()) }
        }
    }

    @Test fun privateModelChannelsNeverBecomeSpokenText() {
        val message = Message.model(Contents.of(Content.Text("Respuesta pública")),
            channels = mapOf("analysis" to "Texto privado", "thought" to "Otro contenido interno"))
        assertEquals("Respuesta pública", LiteRTLlm.publicText(message))
    }

    @Test fun reasoningAloneIsNotAUserResponse() {
        assertEquals("", LiteRTLlm.publicText(Message.model(channels = mapOf("analysis" to "Texto privado"))))
    }
}
