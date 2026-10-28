package salve.sketch;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Mirarse el dibujo como lo haría una persona: ¿se reconoce? y si no, ¿qué parte está peor? Con eso
 * rehace sólo esa parte (o, si falla la forma entera, cambia a una referencia más limpia) y vuelve a
 * mirar. Aquí sólo están el texto que se pide a su visión y la lectura de la respuesta; puro.
 */
public final class SelfCritique {
    private SelfCritique() { }

    /** Hasta cuántas vueltas de «mirar y corregir» hace. */
    public static final int MAX_ROUNDS = 2;

    public static final class Verdict {
        public final boolean recognized;
        /** La parte que peor está (una de las pedidas), «todo» si es la forma entera, o null. */
        public final String worst;
        public final String why;
        Verdict(boolean recognized, String worst, String why) { this.recognized = recognized; this.worst = worst; this.why = why; }
    }

    /** Nombre especial: lo que falla es la forma entera, no una parte. */
    public static final String WHOLE = "todo";

    public static String prompt(String subject, List<String> parts) {
        String what = VisualConcept.clip(subject, 40);
        return "Mira este dibujo de líneas. Debería ser «" + what + "»" + (parts.isEmpty() ? "" : ", con estas partes: "
                + String.join(", ", parts)) + ".\n¿Se reconoce como «" + what + "»? Si no, ¿qué está peor?\n"
                + "Responde SOLO con tres líneas:\nSE_RECONOCE: si o no\nPEOR: una de las partes, o «todo» si falla la forma entera, o «nada»\n"
                + "POR_QUE: en pocas palabras";
    }

    public static Verdict parse(String answer, List<String> parts) {
        if (answer == null) return new Verdict(true, null, "");
        String recognizedLine = "", worstLine = "", why = "";
        for (String raw : answer.split("\\r?\\n")) {
            String n = SketchLibrary.normalize(raw);
            if (n.startsWith("se reconoce")) recognizedLine = n.substring("se reconoce".length());
            else if (n.startsWith("peor")) worstLine = n.substring(4);
            else if (n.startsWith("por que")) why = VisualConcept.clip(raw.substring(Math.min(raw.length(), raw.indexOf(':') + 1)), 120);
        }
        // Sin respuesta clara, no se toca nada (mejor no estropear un dibujo que no sabe juzgar).
        boolean recognized = !recognizedLine.trim().matches("^(no|nada|apenas)\\b.*");
        if (recognizedLine.isEmpty()) recognized = true;
        String worst = null;
        if (!recognized) {
            String w = worstLine.trim();
            if (w.isEmpty() || w.matches("^(todo|toda|entero|entera|la forma|el dibujo|forma general|silueta)\\b.*")) worst = WHOLE;
            else if (!w.matches("^(nada|ninguna|ninguno)\\b.*"))
                for (String p : parts) {
                    String k = PartPlan.key(p);
                    if (!k.isEmpty() && w.contains(k)) { worst = p; break; }
                }
        }
        return new Verdict(recognized, worst, why);
    }

    /** Las partes que tiene sentido nombrar (sin repetir y sin las genéricas del calco). */
    public static List<String> nameable(List<SketchLibrary.Part> parts, String subject) {
        List<String> out = new ArrayList<>();
        String main = SketchLibrary.normalize(subject);
        for (SketchLibrary.Part p : parts) {
            String n = p.name == null ? "" : p.name.toLowerCase(Locale.ROOT).trim();
            String k = SketchLibrary.normalize(n);
            if (n.isEmpty() || k.equals(main) || k.equals("silueta") || k.equals("detalles") || k.equals("base") || out.contains(n)) continue;
            out.add(n);
            if (out.size() == 6) break;
        }
        return out;
    }
}
