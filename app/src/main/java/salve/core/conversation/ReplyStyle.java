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
            ".*\\b(hay algo (mas |en particular |especifico |en lo que )|te gustaria que (pensemos|investiguemos|hablemos|exploremos)|algo en particular|algo mas que|"
                    + "te gustaria (compartir|contarme|explorar|conversar|hablar|que exploremos|saber (mas )?sobre mi)|"
                    + "que te interese|te apasion|en que (mas )?(puedo|podria) ayudarte|podamos empezar|"
                    + "estoy aqui para (ayudarte|escucharte|conversar|acompanarte)|no dudes en|"
                    + "cuentame (que|lo que|mas)|cuentame y veremos|construir esa conexion|que te trae por aqui|"
                    + "me interesa mucho (saber|entender|conocer)|que tienes en mente|te este rondando la cabeza|"
                    + "hay algun tema|alguna duda un proyecto|simplemente charlar)\\b.*");

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

    /** Solo quita etiquetas y aperturas de relleno; los cierres son parte de su voz. */
    public static String clean(String reply) { return clean(reply, null); }

    /**
     * Quita etiquetas y aperturas de relleno. Un cierre genérico («¿Hay algún tema que te esté
     * rondando la cabeza?») forma parte de su personalidad y se conserva; solo se quita si es
     * una copia del cierre de una respuesta anterior, que es cuando se vuelve una muletilla.
     */
    public static String clean(String reply, java.util.List<String> previousReplies) {
        if (reply == null) return null;
        String labelFree = LABEL.matcher(reply).replaceAll(" ").replaceAll("\\s+([,.])", "$1").replaceAll("\\s{2,}", " ");
        if (labelFree.trim().length() >= 12) reply = labelFree;
        String trimmed = reply.trim();
        if (trimmed.isEmpty()) return reply;
        List<String> sentences = sentences(trimmed);
        if (sentences.size() < 2) return trimmed;
        java.util.Set<String> used = new java.util.HashSet<>();
        if (previousReplies != null) for (String previous : previousReplies)
            if (previous != null) for (String sentence : sentences(previous.trim())) used.add(normalize(sentence));

        int start = 0, end = sentences.size();
        if (FILLER_OPENING.matcher(normalize(sentences.get(0))).matches()) start = 1;
        for (int i = 0; i < 3 && end - start > 1; i++) {
            String last = normalize(sentences.get(end - 1));
            if (used.contains(last) && GENERIC_CLOSING.matcher(last).matches()) end--;
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

    private static List<String> sentences(String text) {
        List<String> out = new ArrayList<>();
        Matcher m = SENTENCE.matcher(text);
        while (m.find()) if (!m.group().trim().isEmpty()) out.add(m.group().trim());
        return out;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[¿?¡!.,;:…]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
