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

    @Test fun catalogPinsExactlyOneCompleteExternalModel() {
        val item = ModelDownloader.loadItems(catalog().inputStream()).single()
        assertEquals("Gemma 4 E2B", item.id)
        assertEquals(2588147712L, item.sizeBytes)
        assertEquals("181938105e0eefd105961417e8da75903eacda102c4fce9ce90f50b97139a63c", item.sha256)
        assertTrue(item.url.contains("/resolve/6e5c4f1e395deb959c494953478fa5cec4b8008f/"))
        assertTrue(item.supportsVision)
        assertEquals("Apache-2.0", item.license)
        assertNull("Unknown hardware must stay unknown", item.ramBytes)
        val bundled = catalog().parentFile.parentFile.walkTopDown().filter { it.isFile }
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
