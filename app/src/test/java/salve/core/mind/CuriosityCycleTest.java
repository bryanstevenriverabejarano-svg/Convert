package salve.core.mind;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.Test;

public class CuriosityCycleTest {
    private static SelfModel.Question q(String id, String text, double salience) {
        SelfModel.Question q = new SelfModel.Question();
        q.id = id; q.text = text; q.salience = salience;
        return q;
    }

    @Test public void choosesTheMostSalientPublicQuestion() {
        SelfModel.Question picked = CuriosityCycle.choose(Arrays.asList(
                q("q1", "¿Cómo se forman los recuerdos en el hipocampo?", 0.6),
                q("q2", "¿Por qué los pulpos tienen tres corazones?", 0.9)), Collections.emptySet(), Collections.emptyList());
        assertEquals("q2", picked.id);
        assertEquals("q1", CuriosityCycle.choose(Arrays.asList(
                q("q1", "¿Cómo se forman los recuerdos en el hipocampo?", 0.6),
                q("q2", "¿Por qué los pulpos tienen tres corazones?", 0.9)), new HashSet<>(Collections.singletonList("q2")),
                Collections.emptyList()).id);
    }

    @Test public void privateQuestionsNeverLeaveThePhone() {
        assertNull(CuriosityCycle.choose(Arrays.asList(
                q("q1", "¿Qué siente Bryan cuando trabaja de noche?", 0.9),
                q("q2", "¿Cómo está su hermana Ana últimamente?", 0.8),
                q("q3", "¿Qué opina Lucía de mis respuestas?", 0.7)), Collections.emptySet(), Arrays.asList("Bryan", "Lucía")));
    }

    @Test public void summaryIsLiteralBoundedAndSkipsBoilerplate() {
        String summary = CuriosityCycle.summarize(Arrays.asList("Aceptar cookies. © 2026",
                "Los pulpos tienen tres corazones: dos bombean sangre a las branquias y uno al resto del cuerpo. "
                        + "Su sangre es azul porque usa hemocianina en lugar de hemoglobina para transportar oxígeno. Más texto aquí."));
        assertTrue(summary.startsWith("Los pulpos tienen tres corazones"));
        assertTrue(summary.length() <= CuriosityCycle.MAX_SUMMARY);
        assertNull(CuriosityCycle.summarize(Collections.singletonList("Menú. Inicio.")));
    }

    @Test public void findingBecomesHerOwnThoughtWithSource() {
        SelfModel.Question question = q("q2", "¿Por qué los pulpos tienen tres corazones?", 0.9);
        MindPatch patch = CuriosityCycle.patch(question, "Dos bombean sangre a las branquias.", "es.wikipedia.org");
        assertEquals("curiosidad", patch.origin);
        assertEquals(1, patch.thoughts.size());
        assertTrue(patch.thoughts.get(0).text.contains("es.wikipedia.org"));
        assertTrue(patch.thoughts.get(0).sources.contains("web:es.wikipedia.org"));
        assertTrue(patch.thoughts.get(0).text.length() <= 400);
        assertEquals("pulpos tienen tres corazones", CuriosityCycle.query(question.text));
    }

    /** 16/10/2026: buscó su reflexión entera y notificó un foro chino de AutoCAD. */
    @Test public void searchesKeywordsAndIgnoresUnrelatedPages() {
        String reflection = "Dado el historial de solicitudes sobre inglés y gramática, ¿cuál es el siguiente paso lógico para "
                + "estructurar una enseñanza más coherente?";
        String q = CuriosityCycle.query(reflection);
        assertEquals("inglés gramática enseñanza", q);
        String spam = "「教學」 AutoCAD 黑白列印 AutoCAD顧問 還沒有註冊嗎...即日起免費註冊，所有最完整的AutoCAD討論、教學及資源都在論壇裡喔~ "
                + "Join the forum, it's quick and easy AutoCAD顧問 還沒有註冊嗎...即日起免費註冊，所有最完整的AutoCAD討論。";
        String offTopic = "La receta tradicional de la paella valenciana lleva arroz, pollo, conejo y judía verde. Se cocina a fuego vivo.";
        String good = "La enseñanza del inglés a principiantes suele empezar por la gramática básica: el orden sujeto, verbo y "
                + "complemento, y los verbos más frecuentes. Después se practican frases cortas.";
        CuriosityCycle.Finding f = CuriosityCycle.summarize(Arrays.asList(spam, offTopic, good),
                Arrays.asList("https://spam.example", "https://paella.example", "https://ingles.example"), reflection);
        assertNotNull(f);
        assertEquals("https://ingles.example", f.url);
        assertNull("sin página que trate la pregunta, no hay hallazgo",
                CuriosityCycle.summarize(Arrays.asList(spam, offTopic), Arrays.asList("a", "b"), reflection));
    }

    @Test public void followsTheMostRelatedUnvisitedThread() {
        java.util.List<String> links = Arrays.asList(
                "https://www.google.com/search?q=pulpos",
                "https://es.wikipedia.org/wiki/Cookies",
                "https://es.wikipedia.org/wiki/Sistema_circulatorio_de_los_cefal%C3%B3podos",
                "https://es.wikipedia.org/wiki/Hemocianina",
                "https://es.wikipedia.org/wiki/Pulpo_com%C3%BAn");
        String q = "¿Por qué los pulpos tienen tres corazones y sangre azul?";
        assertEquals("https://es.wikipedia.org/wiki/Pulpo_com%C3%BAn", CuriosityCycle.pickThread(links, q, Collections.emptySet()));
        assertNull("sin otro enlace relacionado, no sigue ningún hilo", CuriosityCycle.pickThread(links, q,
                new HashSet<>(Collections.singletonList("https://es.wikipedia.org/wiki/Pulpo_com%C3%BAn"))));
        assertTrue(CuriosityCycle.patch(q("q", q, 1), "Tienen tres.", "es.wikipedia.org", true)
                .thoughts.get(0).text.startsWith("Seguí el hilo de"));
    }

    @Test public void recognisesWhenSheDoesNotKnowAWorldFact() {
        assertTrue(CuriosityCycle.admitsNotKnowing("No lo sé con certeza, mis datos pueden estar desactualizados."));
        assertTrue(CuriosityCycle.admitsNotKnowing("No tengo información sobre ese torneo."));
        assertFalse(CuriosityCycle.admitsNotKnowing("Navegar, para mí, significa acompañarte."));
        assertTrue(CuriosityCycle.isWorldQuestion("¿Quién ganó el último mundial de fútbol?"));
        assertTrue(CuriosityCycle.isWorldQuestion("cuántos habitantes tiene Vitoria"));
        assertFalse(CuriosityCycle.isWorldQuestion("¿Tú qué sientes cuando hablamos?"));
        assertFalse(CuriosityCycle.isWorldQuestion("¿Qué te dije ayer?"));
        assertFalse(CuriosityCycle.isWorldQuestion("Ser mejor eso quiero"));
    }
}
