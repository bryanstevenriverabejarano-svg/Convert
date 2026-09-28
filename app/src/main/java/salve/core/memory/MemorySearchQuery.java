package salve.core.memory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bounded lexical expansion, not embeddings or inferred facts. Never accepts FTS syntax. */
public final class MemorySearchQuery {
    public static final int MAX_TERMS = 4;
    private static final Set<String> NOISE = new LinkedHashSet<>(Arrays.asList(
            "cual", "cuales", "recuerda", "recuerdo", "recuerdos", "recuerdas", "memoria", "memorias",
            "nodos", "nodo", "grafo", "primer", "primero", "ultimo", "ultima", "guardado", "guardados",
            "hola", "buenos", "buenas", "dias", "tardes", "noches", "gracias", "adios",
            "dije", "dijiste", "hablamos", "hablado", "conmigo", "juntos", "tenemos", "tienes"));
    private static final List<List<String>> VOCABULARY = Arrays.asList(
            Arrays.asList("movil", "telefono", "smartphone"),
            Arrays.asList("ordenador", "computadora", "computador"),
            Arrays.asList("trabajo", "empleo"),
            Arrays.asList("coche", "auto", "automovil"),
            Arrays.asList("foto", "fotografia", "fotos", "fotografias"),
            Arrays.asList("vestuario", "vestido", "atuendo"));

    private final List<String> terms;
    private final List<List<String>> groups;
    private MemorySearchQuery(List<String> terms) {
        this.terms = Collections.unmodifiableList(terms);
        this.groups = new ArrayList<>();
        for (String term : terms) {
            LinkedHashSet<String> group = new LinkedHashSet<>();
            group.add(term);
            for (List<String> family : VOCABULARY) if (family.contains(term)) group.addAll(family);
            groups.add(Collections.unmodifiableList(new ArrayList<>(group)));
        }
    }
    public static MemorySearchQuery parse(String input) {
        List<String> terms = new ArrayList<>();
        String bounded = input == null ? "" : input.substring(0, Math.min(input.length(), 4096));
        for (String term : MemoryQueryTerms.extract(bounded, 24)) {
            String folded = normalize(term);
            if (!NOISE.contains(folded) && !terms.contains(folded)) terms.add(folded);
            if (terms.size() == MAX_TERMS) break;
        }
        return new MemorySearchQuery(terms);
    }
    public List<String> terms() { return terms; }
    public List<List<String>> groups() { return Collections.unmodifiableList(groups); }
    public boolean isEmpty() { return terms.isEmpty(); }
    /** Single words are quoted; operators and punctuation from the input are never copied. */
    public String exactExpression() {
        List<String> quoted = new ArrayList<>();
        for (String term : terms) quoted.add(quote(term));
        return String.join(" ", quoted);
    }
    public String expandedExpression() {
        List<String> expressions = new ArrayList<>();
        for (List<String> group : groups) expressions.add("(" + expression(group) + ")");
        return String.join(" AND ", expressions);
    }
    public boolean hasExpansion() {
        for (List<String> group : groups) if (group.size() > 1) return true;
        return false;
    }
    public static String expression(List<String> group) {
        List<String> quoted = new ArrayList<>();
        for (String word : group) quoted.add(quote(word));
        return String.join(" OR ", quoted);
    }
    private static String quote(String word) {
        if (!word.matches("[\\p{L}0-9]+")) throw new IllegalArgumentException("Invalid search token");
        return "\"" + word + "\"";
    }
    static String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
    }
}
