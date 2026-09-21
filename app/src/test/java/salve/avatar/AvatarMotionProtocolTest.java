package salve.avatar;

import org.junit.Test;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import static org.junit.Assert.*;

public final class AvatarMotionProtocolTest {
    @Test public void validFinalSuffixIsRemovedAndDecoded() {
        AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse("Hola. [[salve_motion:WAVE:WARM]] \n");
        assertEquals("Hola.", result.text);
        assertTrue(result.hasDirective);
        assertEquals(AvatarMotion.Gesture.WAVE, result.gesture);
        assertEquals(AvatarMotion.Expression.WARM, result.expression);
    }
    @Test public void multipleDirectivesAreScrubbedButNotExecuted() {
        AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse(
                "Hola [[salve_motion:WAVE:WARM]] otra [[salve_motion:NOD:NEUTRAL]]");
        assertFalse(result.hasDirective);
        assertFalse(result.text.contains("salve_motion"));
        assertTrue(result.text.contains("otra"));
        assertEquals(AvatarMotion.Gesture.NONE, result.gesture);
    }
    @Test public void inlineDirectiveIsNotAccepted() {
        AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse("Hola[[salve_motion:WAVE:WARM]]amigo.");
        assertFalse(result.hasDirective);
        assertEquals("Hola amigo.", result.text);
    }
    @Test public void incompleteAndInvalidReservedFragmentsNeverReachSpeech() {
        String[] fragments = {"[[salve_motion:DELETE_ALL:WARM]]", "[[salve_motion:WAVE:SAD]]",
                "[[salve_motion:WAVE:WARM", "[salve_motion:WAVE:WARM]", "[[SALVE_MOTION:NOD:NEUTRAL]]",
                "[[ salve-motion:WAVE:WARM]]", "[[salve_motion:" + new String(new char[1000]).replace('\0', 'x')};
        for (String fragment : fragments) {
            AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse("Respuesta. " + fragment);
            assertFalse(fragment, result.hasDirective);
            assertEquals(fragment, "Respuesta.", result.text);
        }
    }
    @Test public void emptyMetadataDoesNotProduceSpeechContent() {
        assertEquals("", AvatarMotionProtocol.parse("[[salve_motion:NONE:NEUTRAL]]").text);
        assertEquals("", AvatarMotionProtocol.parse(null).text);
    }
    @Test public void exactCommandsPreserveQuestionsNegationsAndDiscussion() {
        assertEquals(AvatarMotion.Gesture.WAVE, AvatarMotionProtocol.parseCommand("Salúdame con la mano.").gesture);
        assertEquals(AvatarMotion.Gesture.NOD, AvatarMotionProtocol.parseCommand("asiente").gesture);
        assertNull(AvatarMotionProtocol.parseCommand("¿puedes asiente?"));
        assertNull(AvatarMotionProtocol.parseCommand("no saludes con la mano"));
        assertNull(AvatarMotionProtocol.parseCommand("explica qué significa asiente"));
        assertNull(AvatarMotionProtocol.parseCommand("quiero crear ropa"));
    }
    @Test public void noDirectivePreservesNormalUnicodeAndBrackets() {
        String text = "La operación [a:b] es válida. ¿Qué quieres calcular?";
        assertEquals(text, AvatarMotionProtocol.parse(text).text);
        assertFalse(AvatarMotionProtocol.parse(text).hasDirective);
    }
    @Test public void fallbackDoesNotInferUsersRealEmotions() {
        assertFalse(AvatarMotionProtocol.fallback("Estoy triste y me gusta este amigo").hasDirective);
        assertEquals(AvatarMotion.Gesture.WAVE, AvatarMotionProtocol.fallback("Hola").gesture);
        assertEquals(AvatarMotion.Gesture.NOD, AvatarMotionProtocol.fallback("eso es incorrecto").gesture);
    }
    @Test public void everyPromptExampleIsAnAcceptedCompleteDirective() {
        Matcher examples = Pattern.compile("\\[\\[salve_motion:[^\\]]+\\]\\]")
                .matcher(AvatarMotionProtocol.instruction());
        boolean found = false;
        while (examples.find()) {
            found = true;
            AvatarMotionProtocol.Result result = AvatarMotionProtocol.parse("Hola. " + examples.group());
            assertTrue(examples.group(), result.hasDirective);
            assertEquals("Hola.", result.text);
        }
        assertTrue("The model must receive a concrete, parser-compatible example", found);
    }
    @Test public void observedMalformedNativeModelOutputsAreScrubbedWithoutGestures() {
        AvatarMotionProtocol.Result incomplete = AvatarMotionProtocol.parse(
                "La memoria retiene información. [[salve_motion:EXPLAIN]]");
        assertEquals("La memoria retiene información.", incomplete.text);
        assertFalse(incomplete.hasDirective);
        AvatarMotionProtocol.Result misplaced = AvatarMotionProtocol.parse("[salve_motion:GESTO:WAVE] Hola.");
        assertEquals("Hola.", misplaced.text);
        assertFalse(misplaced.hasDirective);
    }
}
