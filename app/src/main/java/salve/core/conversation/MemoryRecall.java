package salve.core.conversation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import salve.core.memory.MemoryQueryTerms;

/**
 * Cuando Bryan le pregunta por un recuerdo («¿recuerdas que hablamos de este concepto?», «¿qué me
 * comentaste en ese recuerdo?», «pues búscalo»), la pregunta casi nunca nombra el tema: el tema está
 * unas líneas más arriba. Antes se buscaba con la frase tal cual («entiendo», «dijistes»,
 * «recordabas»…), no salía nada y el modelo acababa diciendo «no puedo acceder a mis recuerdos» o,
 * peor, «sí, lo recuerdo» sin haber encontrado nada. Aquí se saca el tema del hilo y se le dice al
 * modelo, con claridad, qué buscó y qué encontró.
 */
public final class MemoryRecall {
    private MemoryRecall() { }

    private static final Pattern ASKS = Pattern.compile(
            ".*\\b(recuerdas|te acuerdas|acuerdas|recordabas|hablamos|hablaste|habiamos hablado|hemos hablado|me dijiste|me dijistes|"
            + "me comentaste|me comentastes|me contaste|me contastes|me explicaste|te dije|te conte|te comente|ese recuerdo|"
            + "aquel recuerdo|tus recuerdos|tu memoria|lo que hablamos|de que hablamos|la otra vez|una vez hablamos)\\b.*");
    private static final Pattern SEARCH_AGAIN = Pattern.compile(
            "^(pues |vale |venga |entonces |anda |y )?(buscalo|busca(lo)? en tus recuerdos|buscalo en tu memoria|miralo|"
            + "miralo en tus recuerdos|revisalo|revisa tus recuerdos|busca bien|intentalo|haz memoria|haz un esfuerzo|recuerdalo)( porfa| por favor)?$");
    /** Palabras de la conversación sobre el recuerdo, no del tema. */
    private static final Set<String> META = new HashSet<>(Arrays.asList(
            "recuerdas", "recuerdo", "recuerdos", "recordabas", "recordar", "recuerda", "acuerdas", "acuerdo", "memoria",
            "hablamos", "hablaste", "hablado", "hablar", "habiamos", "hemos", "dijiste", "dijistes", "dije", "comentaste",
            "comentastes", "comente", "contaste", "contastes", "conte", "explicaste", "entiendo", "entendi", "concepto",
            "conceptos", "tema", "temas", "cosa", "cosas", "conversacion", "conversaciones", "significado", "vez", "veces",
            "aquel", "aquella", "aquello", "eso", "esto", "pues", "buscalo", "busca", "buscar", "miralo", "revisalo",
            "vale", "venga", "entonces", "anda", "porfa", "favor", "verdad", "seguro", "segura", "bien", "dijimos",
            "decir", "dices", "dijo", "pasado", "antes", "ayer", "otra", "otro", "alguna", "algun", "estaba", "estabamos",
            "estuvimos", "hiciste", "hice", "puedes", "podrias", "nada", "ningun", "ninguno", "mismo", "misma"));

    /** ¿Le pide recordar algo de antes? */
    public static boolean asks(String input) {
        return ASKS.matcher(normalize(input)).matches();
    }

    /** «Pues búscalo» justo después de preguntarle por un recuerdo. */
    public static boolean searchesAgain(String input, List<ChatMessage> history) {
        if (!SEARCH_AGAIN.matcher(normalize(input).replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim()).matches()) return false;
        ChatMessage last = lastUser(earlier(history, input), 0);
        return last != null && asks(last.getContent());
    }

    public static boolean isRecallTurn(String input, List<ChatMessage> history) {
        return asks(input) || searchesAgain(input, history);
    }

    /** Palabras del tema en una frase, sin las de «recordar» ni las vacías. */
    public static List<String> topicWords(String text) {
        List<String> out = new ArrayList<>();
        for (String w : MemoryQueryTerms.extract(text == null ? "" : text, 24)) {
            String f = normalize(w);
            if (!META.contains(f) && !out.contains(f)) out.add(f);
        }
        return out;
    }

    /**
     * La consulta que de verdad se busca: el tema de la pregunta, o —si la pregunta solo señala
     * («este concepto», «ese recuerdo», «búscalo»)— el de los últimos mensajes de Bryan.
     */
    public static String query(String input, List<ChatMessage> history) {
        // Si la pregunta ya nombra su tema, se busca tal cual (fechas, «mi primer recuerdo», perfil…).
        if (!topicWords(input).isEmpty()) return input;
        List<ChatMessage> before = earlier(history, input);
        for (int back = 0; back < 4; back++) {
            ChatMessage m = lastUser(before, back);
            if (m == null) break;
            List<String> words = topicWords(m.getContent());
            if (!words.isEmpty()) return String.join(" ", words.subList(0, Math.min(4, words.size())));
        }
        return input == null ? "" : input;
    }

    /** Lo que se le dice al modelo junto a lo que encontró. */
    public static String note(boolean open, boolean found, String searched) {
        String what = searched == null || searched.trim().isEmpty() ? "lo que te pregunta" : "«" + searched.trim() + "»";
        if (!open) return "RECUERDOS_CERRADOS: tus recuerdos privados se abren cuando Bryan se verifica con la huella o la cara. "
                + "Díselo así de simple; no digas que no tienes recuerdos ni que no puedes acceder a ellos.";
        if (found) return "BUSCASTE_EN_TUS_RECUERDOS " + what + ": lo que encontraste está arriba. Cuéntale lo que dice ese recuerdo, "
                + "con sus palabras y su fecha si la tiene. No digas que no puedes acceder a tus recuerdos: acabas de hacerlo.";
        return "BUSCASTE_EN_TUS_RECUERDOS " + what
                + " Y NO ENCONTRASTE EVIDENCIA EN LOS REGISTROS DISPONIBLES AHORA. "
                + "Esto NO demuestra que ese recuerdo nunca se haya guardado: podrían faltar recuerdos de la nube "
                + "o no coincidir las palabras de búsqueda. Dile qué consultaste y que por ahora no puedes confirmarlo. "
                + "NO afirmes «sí, lo recuerdo» ni inventes la conversación; tampoco digas que no tienes acceso a tu memoria.";
    }

    private static final Pattern DENIES = Pattern.compile(
            "\\b(no tengo acceso( directo)? a (mis |los |tus |nuestros )?(recuerdos|conversaciones|registros)|"
            + "no puedo acceder a (ellos|ellas|mis recuerdos|los recuerdos|recuerdos|conversaciones)|"
            + "no (tengo|guardo) memoria de (nuestras |las )?conversaciones|no recuerdo (las |nuestras )?conversaciones (pasadas|anteriores)|"
            + "como (una )?(ia|inteligencia artificial|modelo)[^.]{0,40}no (tengo|puedo|guardo) (memoria|recuerdos|acceso))\\b");
    private static final Pattern CLAIMS = Pattern.compile(
            "^(si|claro|por supuesto)[,!. ]+(lo |la )?recuerdo\\b|\\b(si,? )?recuerdo (que|perfectamente|bien) (hemos|hablamos|habiamos|me)\\b");

    /**
     * Fallo de memoria en el borrador, como instrucción de reescritura; null si no hay.
     * @param found null si no se buscó en sus recuerdos este turno
     */
    public static String problem(String draft, boolean recallTurn, boolean open, Boolean found) {
        if (draft == null || !recallTurn || !open) return null;
        String t = normalize(draft);
        boolean denies = DENIES.matcher(t).find() && (t.contains("recuerd") || t.contains("conversacion") || t.contains("memoria") || t.contains("registro"));
        if (denies) return "dijiste que no puedes acceder a tus recuerdos y no es verdad: los buscas tú misma. Di qué encontraste "
                + "o, si no hay nada, que lo has buscado y no lo encuentras";
        if (Boolean.FALSE.equals(found) && CLAIMS.matcher(t).find())
            return "dijiste que lo recuerdas, pero al buscar en tus recuerdos no salió nada: no lo afirmes; di que no lo encuentras";
        return null;
    }

    /** El historial sin el mensaje actual (el motor lo añade antes de responder). */
    static List<ChatMessage> earlier(List<ChatMessage> history, String input) {
        if (history == null || history.isEmpty()) return history;
        ChatMessage last = history.get(history.size() - 1);
        String now = input == null ? "" : input.trim();
        return last.getRole() == ChatMessage.Role.USER && last.getContent().equals(now)
                ? history.subList(0, history.size() - 1) : history;
    }

    private static ChatMessage lastUser(List<ChatMessage> history, int skip) {
        if (history == null) return null;
        int seen = 0;
        for (int i = history.size() - 1; i >= 0; i--) {
            ChatMessage m = history.get(i);
            if (m.getRole() != ChatMessage.Role.USER) continue;
            if (seen++ == skip) return m;
        }
        return null;
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
}
