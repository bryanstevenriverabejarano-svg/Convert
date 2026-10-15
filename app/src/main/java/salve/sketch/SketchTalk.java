package salve.sketch;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Conversación sobre sus propios dibujos: «¿por qué lo dibujaste así?», «no se parece a un gorila»,
 * «mírate el dibujo». Y la lectura de lo que ve cuando mira su dibujo con visión.
 */
public final class SketchTalk {
    public enum Kind { NONE, WHY, CRITIQUE, LOOK }

    private SketchTalk() { }

    private static final Pattern WHY = Pattern.compile(
            "\\bpor ?que (lo |la |los |las |me )?(dibujaste|dibujas|hiciste|haces|pintaste|trazaste|has dibujado|has hecho)\\b"
            + "|\\bpor ?que (se ve|parece|esta|es|salio|quedo|te salio|te quedo) (asi|tan)\\b"
            + "|\\bcomo (lo |la )?(dibujaste|pensaste|hiciste el dibujo|has dibujado)\\b"
            + "|\\b(en que|de donde) (te basaste|sacaste|salio)\\b"
            + "|\\bexplica(me)? (tu|el|ese|este) dibujo\\b|\\bque (has dibujado|dibujaste)\\b"
            + "|\\bpor ?que (de )?forma (tan )?abstracta\\b");
    private static final Pattern CRITIQUE = Pattern.compile(
            "\\bno se parece\\b|\\bno parece (un|una|el|la)\\b|\\b(esta|es|quedo|salio|estaba) (muy |un poco |bastante )?(feo|fea|raro|rara|mal)\\b"
            + "|\\b(muy|demasiado) abstract[oa]\\b|\\bno me gusta (el|tu|ese|este) dibujo\\b|\\beso no es (un|una)\\b"
            + "|\\bsabes (lo )?que es (un|una)\\b");
    private static final Pattern LOOK = Pattern.compile(
            "\\b(mira|mirate|observa|revisa|analiza) (tu|el|ese|este) (propio )?dibujo\\b|\\bque ves en (tu|el|ese) dibujo\\b");

    public static Kind parse(String input) {
        String t = SketchLibrary.normalize(input);
        if (t.isEmpty() || t.length() > 300) return Kind.NONE;
        if (LOOK.matcher(t).find()) return Kind.LOOK;
        if (WHY.matcher(t).find()) return Kind.WHY;
        if (CRITIQUE.matcher(t).find()) return Kind.CRITIQUE;
        return Kind.NONE;
    }

    // ───────────── mirar su propio dibujo ─────────────

    public enum Verdict { YES, PARTLY, NO, UNKNOWN }

    public static final class Look {
        public final Verdict verdict;
        /** Lo que dice que ve («un rectángulo con un círculo dentro»). */
        public final String seen;
        Look(Verdict verdict, String seen) { this.verdict = verdict; this.seen = seen; }
        public boolean acceptable() { return verdict == Verdict.YES || verdict == Verdict.PARTLY || verdict == Verdict.UNKNOWN; }
        public String describe(String subject) {
            String v = verdict == Verdict.YES ? "se reconoce como " + subject
                    : verdict == Verdict.PARTLY ? "se parece un poco a " + subject
                    : verdict == Verdict.NO ? "no se reconoce como " + subject : "no supe juzgarlo";
            return (seen.isEmpty() ? "" : "veo " + seen + "; ") + v + ".";
        }
    }

    /** Pregunta para su modelo con visión, sobre la imagen de su propio dibujo. */
    public static String lookPrompt(String subject) {
        String what = VisualConcept.clip(subject == null ? "" : subject.replaceAll("[\\r\\n\"«»]+", " "), 60);
        return "Es un dibujo de líneas que acabas de hacer tú. Míralo sin saber qué querías dibujar y responde SOLO con dos líneas:\n"
                + "VEO: qué parece, en pocas palabras\n"
                + "PARECE_OBJETIVO: si, un poco o no (¿se reconoce como «" + what + "»?)\n";
    }

    private static final Pattern SEEN = Pattern.compile("(?im)^\\s*[*-]*\\s*veo\\s*[:=]\\s*(.+)$");
    private static final Pattern VERDICT = Pattern.compile("(?im)^\\s*[*-]*\\s*parece[_ ]?objetivo\\s*[:=]\\s*(.+)$");

    public static Look parseLook(String text) {
        if (text == null) return new Look(Verdict.UNKNOWN, "");
        Matcher s = SEEN.matcher(text);
        String seen = s.find() ? VisualConcept.clip(s.group(1).replaceAll("[«»\"]", ""), 120) : "";
        Matcher v = VERDICT.matcher(text);
        Verdict verdict = Verdict.UNKNOWN;
        if (v.find()) {
            String a = SketchLibrary.normalize(v.group(1));
            if (a.startsWith("un poco") || a.startsWith("algo") || a.startsWith("parcial")) verdict = Verdict.PARTLY;
            else if (a.startsWith("si") || a.startsWith("yes")) verdict = Verdict.YES;
            else if (a.startsWith("no")) verdict = Verdict.NO;
        }
        return new Look(verdict, seen);
    }

    // ───────────── recortar: dónde está el objeto en una foto ─────────────

    /** Pregunta para su visión: dónde está el objeto, para recortarlo antes de calcarlo. */
    public static String locatePrompt(String subject) {
        String what = VisualConcept.clip(subject == null ? "" : subject.replaceAll("[\\r\\n\"«»]+", " "), 60);
        return "Mira la foto. ¿Dónde está «" + what + "»? Responde SOLO con una línea:\n"
                + "CAJA: izquierda arriba derecha abajo (porcentajes de 0 a 100 del ancho y del alto)\n"
                + "Si no aparece, responde: CAJA: no";
    }

    private static final Pattern BOX = Pattern.compile("(?i)caja\\s*[:=]\\s*(\\d{1,3})\\D+(\\d{1,3})\\D+(\\d{1,3})\\D+(\\d{1,3})");

    /** Caja en fracciones {izquierda, arriba, derecha, abajo}, o null si no la ve o no tiene sentido. */
    public static float[] parseBox(String text) {
        if (text == null) return null;
        Matcher m = BOX.matcher(text);
        if (!m.find()) return null;
        float[] b = new float[4];
        for (int i = 0; i < 4; i++) b[i] = Math.min(100, Integer.parseInt(m.group(i + 1))) / 100f;
        if (b[2] - b[0] < .12f || b[3] - b[1] < .12f) return null; // demasiado pequeña o al revés
        if (b[2] - b[0] > .97f && b[3] - b[1] > .97f) return null; // toda la imagen: no recorta nada
        return b;
    }

    // ───────────── honestidad: no decir que dibujó si no dibujó ─────────────

    private static final Pattern CLAIM = Pattern.compile(
            "\\b(aqui tienes (el|tu|un) dibujo|he terminado (de (refinar|dibujar|hacer) )?(el|tu) dibujo|estoy (dibujando|terminando|"
            + "refinando|anadiendo|trazando)|ya (lo )?(he )?dibujad[oa]|te (lo )?muestro el dibujo|mira (el|mi) dibujo)\\b");

    /** ¿La respuesta dice que dibujó o está dibujando? */
    public static boolean claimsDrawing(String reply) {
        return reply != null && CLAIM.matcher(SketchLibrary.normalize(reply)).find();
    }
}
