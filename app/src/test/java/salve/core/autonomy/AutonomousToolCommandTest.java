package salve.core.autonomy;

import org.junit.Test;

import static org.junit.Assert.*;

public class AutonomousToolCommandTest {
    @Test public void explicitSpanishCommandsAreNormalized() {
        assertEquals(AutonomousToolCommand.Action.STATUS, parse("  LABORATORIO AUTÓNOMO  ").action);
        assertEquals(AutonomousToolCommand.Action.STATUS, parse("Estado del laboratorio").action);
        assertEquals(AutonomousToolCommand.Action.STATUS, parse("¿Qué herramientas has aprendido?").action);
        assertEquals(AutonomousToolCommand.Action.PAUSE, parse("pausa el laboratorio").action);
        assertEquals(AutonomousToolCommand.Action.RESUME, parse("reanuda el laboratorio").action);
        assertEquals("route", parse("revierte herramienta ROUTE").payload);
        assertTrue(AutonomousToolCommand.isFamily("route"));
        assertFalse(AutonomousToolCommand.isFamily("shell"));
    }

    @Test public void ordinaryConversationAndJsonAreNeverCommands() {
        assertNull(parse(null));
        assertNull(parse("¿Qué es un laboratorio autónomo?"));
        assertNull(parse("Dijo: pausa el laboratorio"));
        assertNull(parse("{\"tool\":\"SOLVE_CHALLENGE\"}"));
        assertNull(parse("resuelve retocado"));
    }

    @Test public void directJsonIsPreservedAndMalformedCommandStaysLocal() {
        assertEquals("{\"id\":\"ÁRBOL\"}", parse("Resuelve reto: {\"id\":\"ÁRBOL\"}").payload);
        assertEquals(AutonomousToolCommand.Action.SOLVE, parse("resuelve reto").action);
        assertEquals(AutonomousToolCommand.Action.SOLVE, parse("resuelve reto sin JSON").action);
        assertEquals(AutonomousToolCommand.Action.ROLLBACK, parse("revierte herramienta shell").action);
    }

    @Test public void duplicateKeysAndTrailingDataAreRejected() {
        rejects("{\"schema\":1,\"schema\":2}");
        rejects("{\"input\":{\"nodes\":1,\"nodes\":2}}");
        rejects("{} {}");
        rejects("Una propuesta: {}");
        rejects("```json\n{}\n```");
    }

    @Test public void nonJsonAndLenientSyntaxAreRejected() {
        rejects("{'schema':1}");
        rejects("{schema:1}");
        rejects("{\"schema\":NaN}");
        rejects("{/*comment*/\"schema\":1}");
        rejects("[]");
        rejects("null");
        rejects("");
    }

    @Test public void deepAndOversizedJsonAreRejectedWithoutStackOverflow() {
        String nested = "0";
        for (int i = 0; i < 40; i++) nested = "[" + nested + "]";
        rejects("{\"input\":" + nested + "}");
        StringBuilder large = new StringBuilder("{\"id\":\"");
        for (int i = 0; i < 33000; i++) large.append('é');
        rejects(large.append("\"}").toString());
    }

    @Test public void validObjectKeepsNumberTypesAndStrings() {
        assertEquals(3, AutonomousToolCommand.parseObject("{\"nodes\":3,\"id\":\"Árbol\"}")
                .get("nodes").getAsInt());
        assertEquals("Árbol", AutonomousToolCommand.parseObject("{\"id\":\"Árbol\"}").get("id").getAsString());
    }

    @Test public void modelToolRequiresScopedProblemAndExplicitNumbers() {
        assertTrue(AutonomousToolCommand.offersToolFor("Ruta más corta entre 0 y 3 con estas aristas"));
        assertTrue(AutonomousToolCommand.offersToolFor("Ordena las dependencias de 4 nodos"));
        assertTrue(AutonomousToolCommand.offersToolFor("Mochila de capacidad 8"));
        assertFalse(AutonomousToolCommand.offersToolFor("Explícame las dependencias"));
        assertFalse(AutonomousToolCommand.offersToolFor("Tengo 4 recuerdos"));
        assertFalse(AutonomousToolCommand.offersToolFor("Compara mochila y dependencias con 4 ejemplos"));
        assertFalse(AutonomousToolCommand.offersToolFor(null));
    }

    private static AutonomousToolCommand parse(String text) { return AutonomousToolCommand.parse(text); }

    private static void rejects(String json) {
        try { AutonomousToolCommand.parseObject(json); fail("Expected invalid JSON"); }
        catch (IllegalArgumentException expected) { }
    }
}
