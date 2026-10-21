package salve.core.conversation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Revisor único del borrador antes de enviarlo. Antes había tres reintentos encadenados (idioma,
 * repetición y autocrítica) y cada uno podía pedir otra inferencia: hasta cuatro llamadas al modelo
 * en un turno. Ahora se revisa todo a la vez y, si hace falta, se reescribe UNA sola vez con todas
 * las correcciones juntas; se queda la versión con menos fallos.
 */
public final class ReplyReviewer {
    private ReplyReviewer() { }

    /** Lo que encontró en un borrador. */
    public static final class Review {
        public final boolean english, repeats;
        public final List<ResponseCritic.Problem> critic;

        Review(boolean english, boolean repeats, List<ResponseCritic.Problem> critic) {
            this.english = english; this.repeats = repeats;
            this.critic = Collections.unmodifiableList(critic);
        }

        public int issues() { return (english ? 1 : 0) + (repeats ? 1 : 0) + critic.size(); }
        public boolean needsRewrite() { return issues() > 0; }

        /** Una sola instrucción de reescritura con todo lo que falló. */
        public String instruction() {
            StringBuilder b = new StringBuilder(" | REVISION (leíste tu borrador antes de enviarlo y no servía): ");
            if (english) b.append("salió en inglés: responde SOLO en español, con naturalidad. ");
            if (repeats) b.append("repetías frases de respuestas anteriores: responde con palabras nuevas y concretas al mensaje actual. ");
            for (ResponseCritic.Problem p : critic) b.append(p.fix).append(". ");
            return b.append("Reescribe la respuesta corrigiéndolo todo a la vez.").toString();
        }

        /** Qué corrigió, para su registro de acciones y sus lecciones (vacío si solo era idioma o repetición). */
        public String criticSummary() {
            StringBuilder what = new StringBuilder();
            for (ResponseCritic.Problem p : critic) what.append(what.length() == 0 ? "" : "; ").append(p.fix);
            return what.toString();
        }
    }

    /**
     * Revisa el borrador.
     * @param input mensaje de Bryan (decide si el inglés estaba pedido)
     * @param recent respuestas recientes de Salve (para detectar que repite)
     * @param reading lo que entendió del hilo (para la autocrítica); null si no hay
     * @param critique false en escenas de historia, donde el estilo narrativo es lo pedido
     */
    public static Review review(String draft, String input, List<String> recent, DialogueFocus.Reading reading, boolean critique) {
        if (draft == null || draft.trim().isEmpty()) return new Review(false, false, new ArrayList<>());
        boolean english = LanguageGuard.isMostlyEnglish(draft) && !englishWanted(input);
        boolean repeats = ReplyStyle.repeats(draft, recent) || ReplyStyle.mostlyEchoes(draft, recent);
        List<ResponseCritic.Problem> critic = new ArrayList<>();
        if (critique) {
            critic.addAll(ResponseCritic.check(draft, reading));
            critic.remove(ResponseCritic.Problem.THIRD_PERSON); // se arregla sin reescribir (firstPerson)
        }
        return new Review(english, repeats, critic);
    }

    /** Con la reescritura en la mano: ¿mejora? Solo se cambia si tiene menos fallos que el borrador. */
    public static boolean better(Review rewritten, Review draft) {
        return rewritten != null && draft != null && rewritten.issues() < draft.issues();
    }

    static boolean englishWanted(String input) {
        if (input == null) return false;
        String t = input.toLowerCase(Locale.ROOT);
        return LanguageGuard.isMostlyEnglish(input)
                || t.matches("(?s).*\\b(en ingles|en inglés|in english|traduce al ingles|traduce al inglés)\\b.*");
    }
}
