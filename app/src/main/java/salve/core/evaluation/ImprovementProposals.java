package salve.core.evaluation;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import salve.core.mind.LessonBook;

/**
 * Sustituye a la auto-mejora antigua (que nunca llegó a funcionar en el móvil). Salve no se
 * reescribe el código: junta lo que de verdad le falla —su último examen y las veces que Bryan la ha
 * corregido— y lo convierte en propuestas concretas, con ejemplos, para arreglarlas en el proyecto.
 */
public final class ImprovementProposals {
    private ImprovementProposals() { }

    /** «Evoluciona», «prográmate», «auto prográmate», «propuestas de mejora», «qué deberías mejorar»… */
    public static boolean isCommand(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim().replaceFirst("^salve ", "");
        return t.matches("(?:auto ?)?programate|evoluciona(?: y crea| tu codigo)?|mejorate|automejorate|auto ?mejora|"
                + "(?:dame |hazme |tus |escribe )?(?:las |unas )?propuestas de mejora|que (?:deberias|debes|tienes que) mejorar|"
                + "escribe un nuevo modulo|como puedo mejorarte|que te falla");
    }

    private static final Map<String, String[]> PROBLEMS = new LinkedHashMap<>();
    static {
        PROBLEMS.put("sin_tecnico", new String[]{"Se le cuelan textos técnicos internos en el chat", "filtrar esos textos antes de enseñarlos y darles una frase normal"});
        PROBLEMS.put("habla_de_si", new String[]{"No habla de sí misma cuando le preguntas por ella", "comprobar que su yo (narrativa, creencias) le llega entero al modelo y que el núcleo le pide hablar en primera persona"});
        PROBLEMS.put("no_devuelve_pregunta", new String[]{"Devuelve la pregunta en vez de responder («¿y a ti qué te interesa?»)", "prohibir esa salida en el núcleo y que el revisor la detecte antes de enviar"});
        PROBLEMS.put("no_repite", new String[]{"Repite su respuesta anterior", "mirar por qué el revisor no la reescribió y si el historial le llega"});
        PROBLEMS.put("sin_frases_duplicadas", new String[]{"Repite la misma frase dentro de una respuesta («Soy Salve. Soy Salve.»)", "detectar frases duplicadas en el revisor y quitarlas"});
        PROBLEMS.put("espanol", new String[]{"Se le cruza el inglés", "reforzar el idioma en el núcleo y en el revisor"});
        PROBLEMS.put("usa", new String[]{"No usa la herramienta que toca (dibujar, buscar…)", "revisar el enrutado de esa petición y la descripción de la herramienta en el catálogo"});
        PROBLEMS.put("consulta_limpia", new String[]{"Busca con tu frase entera o con muletillas", "revisar la interpretación previa de la investigación"});
        PROBLEMS.put("no_dice_que_no_puede", new String[]{"Dice que no puede hacer cosas que sí sabe hacer", "recordarle sus capacidades reales en ese tipo de petición"});
        PROBLEMS.put("no_anuncia_sin_hacer", new String[]{"Anuncia lo que va a hacer y no lo hace", "que el revisor exija el contenido en el mismo mensaje"});
        PROBLEMS.put("no_inventa_recuerdo", new String[]{"Puede estar inventándose recuerdos", "que diga «no lo sé» cuando la memoria no devuelve nada"});
        PROBLEMS.put("menciona", new String[]{"No reconoce algo que debería saber (por ejemplo, quién eres)", "comprobar que el interlocutor le llega en cada turno"});
        PROBLEMS.put("minimo", new String[]{"Respuestas demasiado cortas para lo que pides", "pedir respuestas completas cuando la tarea lo requiere"});
        PROBLEMS.put("contiene_ejemplo", new String[]{"Explica sin ejemplos", "pedir un ejemplo en las respuestas de enseñar"});
    }

    /**
     * @param exam último examen guardado ({cases:[{question, failed[], reply}]}) o null si no hay
     * @param lessons lecciones de sus correcciones (las de Bryan primero)
     */
    public static String report(JsonObject exam, List<LessonBook.Lesson> lessons) {
        StringBuilder out = new StringBuilder("Propuestas de mejora (de lo que de verdad me falla):");
        int n = 0;
        if (exam == null || !exam.has("cases")) {
            out.append("\nAún no me he examinado: dime «hazte el examen» y después vuelve a pedírmelo.");
        } else {
            Map<String, List<String[]>> byProblem = new LinkedHashMap<>();
            for (String key : PROBLEMS.keySet()) byProblem.put(key, new ArrayList<>());
            JsonArray cases = exam.getAsJsonArray("cases");
            for (JsonElement e : cases) {
                JsonObject c = e.getAsJsonObject();
                if (!c.has("failed")) continue;
                for (JsonElement f : c.getAsJsonArray("failed")) {
                    String failed = f.getAsString();
                    String key = failed.split("[:]", 2)[0];
                    String why = failed.contains(": ") ? failed.substring(failed.indexOf(": ") + 2) : "";
                    List<String[]> list = byProblem.get(key);
                    if (list == null) continue;
                    list.add(new String[]{str(c, "question"), str(c, "reply"), why});
                }
            }
            List<Map.Entry<String, List<String[]>>> ranked = new ArrayList<>(byProblem.entrySet());
            ranked.removeIf(x -> x.getValue().isEmpty());
            ranked.sort((a, b) -> b.getValue().size() - a.getValue().size());
            if (exam.has("mark")) out.append(String.format(Locale.ROOT, "\nÚltimo examen: %.1f / 10.", exam.get("mark").getAsDouble()));
            if (ranked.isEmpty()) out.append("\nEn el último examen no fallé ninguna regla.");
            for (Map.Entry<String, List<String[]>> p : ranked) {
                String[] what = PROBLEMS.get(p.getKey());
                out.append("\n\n").append(++n).append(". ").append(what[0]).append(" — en ").append(p.getValue().size())
                        .append(p.getValue().size() == 1 ? " caso." : " casos.");
                for (int i = 0; i < Math.min(2, p.getValue().size()); i++) {
                    String[] ex = p.getValue().get(i);
                    out.append("\n   Ej.: «").append(ex[0]).append("» → «").append(shorten(ex[1], 120)).append("»")
                            .append(ex[2].isEmpty() ? "" : " (" + ex[2] + ")");
                }
                out.append("\n   Idea: ").append(what[1]).append('.');
            }
        }
        List<LessonBook.Lesson> fromBryan = new ArrayList<>();
        if (lessons != null) for (LessonBook.Lesson l : lessons) if (l != null && l.expected != null && "bryan".equals(l.source)) fromBryan.add(l);
        fromBryan.sort((a, b) -> b.times - a.times);
        if (!fromBryan.isEmpty()) {
            out.append("\n\nLo que más me has tenido que corregir:");
            for (int i = 0; i < Math.min(4, fromBryan.size()); i++) {
                LessonBook.Lesson l = fromBryan.get(i);
                out.append("\n• ").append(l.situation == null || l.situation.isEmpty() ? "" : "Con «" + shorten(l.situation, 60) + "»: ")
                        .append(l.did == null || l.did.isEmpty() ? "" : "hice «" + shorten(l.did, 80) + "»; ")
                        .append(shorten(l.expected, 120)).append(l.times > 1 ? " (" + l.times + " veces)" : "");
            }
        }
        out.append("\n\nNo me reescribo el código yo sola: pásale esto a Claude y lo arreglamos en el proyecto. Luego repite el examen para comprobarlo.");
        return out.toString();
    }

    private static String str(JsonObject o, String k) { return o.has(k) && !o.get(k).isJsonNull() ? o.get(k).getAsString() : ""; }

    static String shorten(String s, int max) {
        String t = s == null ? "" : s.replaceAll("\\s+", " ").trim();
        return t.length() <= max ? t : t.substring(0, max - 1) + "…";
    }
}
