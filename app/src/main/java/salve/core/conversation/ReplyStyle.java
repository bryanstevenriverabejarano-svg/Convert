package salve.core.conversation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quita las muletillas que un modelo pequeño repite aunque el prompt las prohíba: aperturas
 * de relleno («Entiendo tu pregunta.») y cierres genéricos («¿Hay algo en particular que te
 * gustaría compartir?»). Solo elimina frases completas al principio o al final y nunca deja la
 * respuesta vacía; una pregunta concreta de seguimiento se conserva.
 */
public final class ReplyStyle {
    private static final Pattern SENTENCE = Pattern.compile(".+?(?:[.!?…]+(?=\\s|$)|$)\\s*", Pattern.DOTALL);
    private static final Pattern FILLER_OPENING = Pattern.compile(
            "^(entiendo tu pregunta|entiendo perfectamente|buena pregunta|que buena pregunta|"
                    + "entiendo que (estas|me estas) (buscando|preguntando|pidiendo)|entiendo lo que (buscas|preguntas))\\b.*");
    private static final Pattern GENERIC_CLOSING = Pattern.compile(
            ".*\\b(hay algo (mas |en particular |especifico )|algo en particular|algo mas que|"
                    + "te gustaria (compartir|contarme|explorar|conversar|hablar|que exploremos|saber (mas )?sobre mi)|"
                    + "que te interese|te apasion|en que (mas )?(puedo|podria) ayudarte|podamos empezar|"
                    + "estoy aqui para (ayudarte|escucharte|conversar|acompanarte)|no dudes en|"
                    + "cuentame (que|lo que|mas)|construir esa conexion|que te trae por aqui)\\b.*");

    private ReplyStyle() {}

    /** Etiquetas prefabricadas que contradicen la identidad de Salve («como modelo de lenguaje, …»). */
    private static final Pattern LABEL = Pattern.compile(
            "(?iu),?\\s*\\b(como|al ser|siendo) (un |una )?(modelo de lenguaje|inteligencia artificial|ia|asistente virtual)( de ia)?\\b,?");

    /** true si la respuesta repite (casi) literalmente una de las anteriores de Salve. */
    public static boolean repeats(String reply, java.util.List<String> previousReplies) {
        String r = normalize(reply == null ? "" : reply);
        if (r.length() < 20 || previousReplies == null) return false;
        for (String previous : previousReplies) if (r.equals(normalize(previous == null ? "" : previous))) return true;
        return false;
    }

    public static String clean(String reply) {
        if (reply == null) return null;
        String labelFree = LABEL.matcher(reply).replaceAll(" ").replaceAll("\\s+([,.])", "$1").replaceAll("\\s{2,}", " ");
        if (labelFree.trim().length() >= 12) reply = labelFree;
        String trimmed = reply.trim();
        if (trimmed.isEmpty()) return reply;
        List<String> sentences = new ArrayList<>();
        Matcher m = SENTENCE.matcher(trimmed);
        while (m.find()) if (!m.group().trim().isEmpty()) sentences.add(m.group().trim());
        if (sentences.size() < 2) return trimmed;

        int start = 0, end = sentences.size();
        if (FILLER_OPENING.matcher(normalize(sentences.get(0))).matches()) start = 1;
        // Hasta dos cierres genéricos seguidos («¿Hay algo…? Estoy aquí para ayudarte.»).
        for (int i = 0; i < 2 && end - start > 1; i++) {
            if (GENERIC_CLOSING.matcher(normalize(sentences.get(end - 1))).matches()) end--;
            else break;
        }
        if (end - start < 1 || (start == 0 && end == sentences.size())) return trimmed;
        StringBuilder out = new StringBuilder();
        for (int i = start; i < end; i++) {
            if (out.length() > 0) out.append(' ');
            out.append(sentences.get(i));
        }
        String result = out.toString().trim();
        return result.length() < 12 ? trimmed : result;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!.,;:…]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
