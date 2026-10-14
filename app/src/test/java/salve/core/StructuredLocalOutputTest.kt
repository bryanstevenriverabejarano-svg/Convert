package salve.core

import com.google.ai.edge.litertlm.ResponseFormat
import org.junit.Assert.*
import org.junit.Test
import salve.core.cognition.CognitiveOutputConstraints

/** Uses the pinned LiteRT-LM API; no model weights or native engine needed for config validation. */
class StructuredLocalOutputTest {
    @Test fun cognitiveTurnsEnableConstraintProviderAndPerMessageSchema() {
        val config = LiteRTLlm.conversationConfig(true)
        assertTrue(config.enableResponseFormat)
        assertFalse(config.automaticToolCalling)
        val format = LiteRTLlm.responseFormat(true)!!
        assertEquals(ResponseFormat.Type.JSON_OBJECT, format.type)
        assertEquals(CognitiveOutputConstraints.jsonSchema(), format.schemaOrPattern)
    }
    @Test fun ordinarySpecialistCallsRemainUnconstrained() {
        assertFalse(LiteRTLlm.conversationConfig(false).enableResponseFormat)
        assertNull(LiteRTLlm.responseFormat(false))
    }
    @Test fun singleRepairHasSmallerOutputBudget() {
        assertEquals(512, LiteRTLlm.conversationConfig(true).maxOutputToken!!)
        assertEquals(256, LiteRTLlm.conversationConfig(true, true).maxOutputToken!!)
    }
}
