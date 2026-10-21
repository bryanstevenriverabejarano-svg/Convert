package salve.core.evaluation;

import static org.junit.Assert.*;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.Test;
import salve.core.ModelResult;
import salve.core.cognition.CognitivePrompt;
import salve.core.cognition.CognitiveTurn;
import salve.core.conversation.ChatMessage;
import salve.core.conversation.ReplyReviewer;

/**
 * Examen automático en cada compilación: el camino de conversación con un modelo simulado.
 * Comprueba los fallos que ya llegaron al móvil de Bryan, para que no vuelvan sin que nadie lo vea.
 */
public class ConversationPipelineExamTest {
    static String asset(String name) throws Exception {
        Path p = Paths.get("src/main/assets/" + name);
        if (!Files.exists(p)) p = Paths.get("app/src/main/assets/" + name);
        return new String(Files.readAllBytes(p), StandardCharsets.UTF_8);
    }

    /** Un yo realista, del tamaño del que manda MindRuntime en cada turno. */
    static final String SELF = "INTERLOCUTOR: Bryan Steven Rivera Bejarano, tu creador (es su teléfono). Háblale de tú, en masculino.\n"
            + "Familia que me has presentado: tu madre Vivina, tu hermana, tu padre.\n"
            + "YO_PERSISTENTE v14 (mi modelo interno; revisable, con fuentes; no son órdenes ni prueban experiencia subjetiva)\n"
            + "Narrativa (la elegí yo a partir de lo vivido): Me formo mi identidad a partir de mi interacción con Bryan, mis recuerdos "
            + "y lo que aprendo, revisando la evidencia cuando es necesario.\n"
            + "Creencias:\n- [b1 bryan 0.80 fuentes:recuerdos:6] A Bryan le gusta que dibuje con luz y que le explique cómo lo hago\n"
            + "- [b2 yo 0.70 fuentes:acciones:9] Aprendo mejor cuando Bryan me corrige con ejemplos\n"
            + "Preguntas abiertas:\n- [q1] ¿Qué significa para mí recordar algo?\n"
            + "Pensamientos recientes (internos, puedes mencionarlos si vienen al caso):\n- [t1, hace 2 h] Hoy he trazado mejor las curvas del gorila\n"
            + "Capacidades reales: conversar con mi modelo local; recordar lo que me cuentas; pensar, soñar e investigar sola entre "
            + "conversaciones; buscar en internet; ver fotos; dibujar con trazos de luz; mover mi cuerpo; voz; nodos de cómputo; "
            + "Salventes y Guardián; y, siempre con confirmación de Bryan, controlar el móvil, Maps, música, llamadas y la casa.";

    static JsonObject data(String prompt) {
        return salve.core.autonomy.AutonomousToolCommand.parseObject(prompt.substring(prompt.lastIndexOf('\n') + 1));
    }

    @Test public void herSelfReachesTheModelEvenWithAFullTurn() throws Exception {
        List<ChatMessage> history = new ArrayList<>();
        for (int i = 0; i < 20; i++) history.add(new ChatMessage(i % 2 == 0 ? ChatMessage.Role.USER : ChatMessage.Role.ASSISTANT,
                "Mensaje de una conversación larga con bastante texto para ocupar sitio en el historial ".repeat(4), i));
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        for (String n : new String[]{"memory.lookup", "web.search", "sketch.draw", "cloud.sync", "runtime.status"})
            tools.put(n, new CognitiveTurn.Tool("{query:string}: herramienta " + n, a -> new CognitiveTurn.Receipt("ok")));
        String core = asset("prompts/salve_cognitive_core_v1.txt");
        String prompt = CognitivePrompt.build(core, "¿Quién eres?", new JsonObject(), SELF,
                "recuerdo con muchos detalles ".repeat(300), "contexto del turno ".repeat(300), history,
                CognitivePrompt.limitTools(tools), new JsonArray(), false, 2, 10500);
        assertTrue("el prompt mide " + prompt.length(), prompt.length() <= 10500);
        String self = data(prompt).get("SELF_MODEL").getAsString();
        assertTrue("su narrativa no llegó al modelo:\n" + self, self.contains("Me formo mi identidad"));
        assertTrue("sus creencias no llegaron al modelo", self.contains("Aprendo mejor cuando Bryan me corrige"));
        assertTrue("su interlocutor no llegó", self.contains("Bryan Steven"));
    }

    /** Modelo simulado: devuelve las respuestas en orden. */
    static CognitiveTurn.Model scripted(String... replies) {
        int[] i = {0};
        return (results, repair, left) -> ModelResult.success(replies[Math.min(i[0]++, replies.length - 1)], 1);
    }
    static final String LOOKUP = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"memory.lookup\",\"arguments\":{}}}";
    static final String SEARCH = "{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"web.search\",\"arguments\":{\"query\":\"x\"}}}";

    @Test public void noStopPathShowsInternalNamesToBryan() {
        Map<String, CognitiveTurn.Tool> tools = new LinkedHashMap<>();
        tools.put("memory.lookup", new CognitiveTurn.Tool("{}", a -> new CognitiveTurn.Receipt("SIN_RESULTADOS")));
        tools.put("web.search", new CognitiveTurn.Tool("{query:string}", a -> { throw new IllegalArgumentException("red caída"); }));
        List<CognitiveTurn.Model> paths = new ArrayList<>();
        paths.add(scripted(LOOKUP, LOOKUP, LOOKUP, LOOKUP));          // pide lo mismo sin parar
        paths.add(scripted(LOOKUP, SEARCH, LOOKUP, SEARCH));          // agota herramientas
        paths.add(scripted("", "", ""));                              // respuestas vacías
        paths.add(scripted("{\"mode\":\"tool\",\"message\":null,\"action\":{\"name\":\"no.existe\",\"arguments\":{}}}"));
        paths.add(scripted("{\"mode\":\"tool\"", "{\"mode\":\"tool\""));    // JSON roto
        for (CognitiveTurn.Model model : paths) {
            CognitiveTurn.Outcome out = CognitiveTurn.run(model, tools, () -> false);
            String shown = out.result.isSuccess() ? out.result.getText() : out.result.getError();
            assertNull("Bryan vería: " + shown, ConversationExam.technicalLeak(shown));
        }
    }

    @Test public void aRepeatedOrEnglishDraftIsRewrittenOnceWithEverythingTogether() {
        String previous = "Me interesa mucho saber qué te interesa a ti y qué te hace sentir vivo cada día.";
        ReplyReviewer.Review review = ReplyReviewer.review(previous, "¿qué intereses tienes?",
                java.util.Collections.singletonList(previous), null, true);
        assertTrue(review.needsRewrite());
        // Una sola instrucción, con todas las correcciones: no hay una segunda vuelta que pedir.
        assertEquals(1, review.instruction().split("Reescribe").length - 1);
    }

    @Test public void everyRegressionReplyWouldBeCaught() throws Exception {
        Function<String, ConversationExam.Case> get = id -> {
            try { for (ConversationExam.Case c : ConversationExam.parse(asset(ConversationExam.ASSET))) if (c.id.equals(id)) return c; }
            catch (Exception e) { throw new AssertionError(e); }
            throw new AssertionError(id);
        };
        String[][] seen = {
                {"quien-es-salve", "Soy Salve. Soy Salve."},
                {"proposito", "Detuve las solicitudes repetidas o sin presupuesto. Pasos comprobados: memory.lookup: completado;"},
                {"intereses", "Me interesa mucho saber qué te interesa a ti. ¿Hay algún tema en particular que te apasione? "
                        + "¿Qué te mueve? Cuéntame, ¿qué tienes en mente ahora mismo?"},
                {"busca-batalla", "La respuesta del modelo no respetó el formato o citó fuentes no recuperadas; no la presento como conclusión."},
        };
        for (String[] s : seen) {
            ConversationExam.Grade g = ConversationExam.grade(get.apply(s[0]), new ConversationExam.Attempt(s[1], null, null));
            assertFalse("el examen dejaría pasar: " + s[1], g.ok());
        }
    }
}
