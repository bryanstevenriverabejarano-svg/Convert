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
        assertEquals(0.3, config.samplerConfig!!.temperature, 0.000001)
        assertEquals(0.9, config.samplerConfig!!.topP, 0.000001)
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
    @Test fun creativeTextIsFreeLongerAndWarmer() {
        val config = LiteRTLlm.conversationConfig(false, false, true)
        assertFalse(config.enableResponseFormat)
        assertEquals(LiteRTLlm.CREATIVE_OUTPUT_TOKENS, config.maxOutputToken!!)
        assertEquals(0.8, config.samplerConfig!!.temperature, 0.000001)
        // A structured turn never becomes creative: the JSON contract keeps its budget.
        assertEquals(512, LiteRTLlm.conversationConfig(true, false, true).maxOutputToken!!)
    }
    @Test fun speculativeDecodingDropsTheForcedJsonButKeepsBudgets() {
        // LiteRT-LM#3944: MTP + ResponseFormat.json rompe la salida; el JSON lo valida CognitiveTurn.
        val config = LiteRTLlm.conversationConfig(true, speculative = true)
        assertFalse(config.enableResponseFormat)
        assertNull(LiteRTLlm.responseFormat(true, true))
        assertEquals(512, config.maxOutputToken!!)
        assertEquals(0.3, config.samplerConfig!!.temperature, 0.000001)
        assertTrue(LiteRTLlm.conversationConfig(true, speculative = false).enableResponseFormat)
    }
}
