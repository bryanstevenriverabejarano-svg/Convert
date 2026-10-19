package salve.core.salvente;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Comprueba que una ficha es un Salvente válido antes de enseñarla y, otra vez, antes de guardarla:
 * identificador inmutable con formato propio, nombre que no suplanta a Salve ni repite otro,
 * los 14 rasgos en [0, 1], una personalidad con carácter (no plana) y capacidades coherentes con
 * esos rasgos. Devuelve todos los problemas a la vez, en español, para poder enseñarlos.
 */
public final class SalventeProfileValidator {
    public static final Pattern ID = Pattern.compile("slv-[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}");
    private static final Pattern NAME = Pattern.compile("\\p{L}[\\p{L}' -]{1,23}");
    private static final Set<String> RESERVED = new HashSet<>(java.util.Arrays.asList(
            "salve", "salvente", "salventes", "sistema", "system", "admin", "root", "claude", "asistente", "assistant"));
    /** Diferencia mínima entre el rasgo más alto y el más bajo: por debajo no hay carácter definido. */
    public static final double MIN_SPREAD = 0.25;

    public static final class Result {
        public final List<String> problems;
        Result(List<String> problems) { this.problems = Collections.unmodifiableList(problems); }
        public boolean ok() { return problems.isEmpty(); }
        @Override public String toString() { return String.join(" ", problems); }
    }

    private SalventeProfileValidator() { }

    /** @param takenNames nombres ya usados por otros Salventes (se comparan sin tildes ni mayúsculas). */
    public static Result validate(SalventeProfile p, Collection<String> takenNames) {
        List<String> problems = new ArrayList<>();
        if (p == null) { problems.add("No hay ficha."); return new Result(problems); }
        if (p.id == null || !ID.matcher(p.id).matches()) problems.add("Identificador no válido.");
        if (!SalventeProfile.SPECIES.equals(p.species)) problems.add("La especie debe ser SALVENTE.");
        String name = p.name == null ? "" : p.name.trim();
        if (!NAME.matcher(name).matches() || !name.equals(p.name)) problems.add("El nombre debe tener de 2 a 24 letras.");
        else {
            String key = key(name);
            for (String word : key.split("[ '-]+")) if (RESERVED.contains(word)) { problems.add("Ese nombre está reservado: Salve es única."); break; }
            if (takenNames != null) for (String other : takenNames)
                if (other != null && key(other).equals(key)) { problems.add("Ya existe un Salvente llamado " + other + "."); break; }
        }
        if (p.purpose == null || p.purpose.trim().length() < 3 || p.purpose.length() > 280 || hasControl(p.purpose))
            problems.add("El propósito debe tener entre 3 y 280 caracteres.");
        if (p.greeting.length() > 280 || hasControl(p.greeting)) problems.add("El saludo es demasiado largo.");

        double min = 1, max = 0; boolean traitsOk = true;
        for (SalventeTrait trait : SalventeTrait.values()) {
            double v = p.trait(trait);
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0 || v > 1) {
                problems.add("Rasgo " + trait.label() + " fuera de rango [0, 1]."); traitsOk = false;
            } else { min = Math.min(min, v); max = Math.max(max, v); }
        }
        if (p.traits.size() != SalventeTrait.values().length) traitsOk = false;
        if (traitsOk && max - min < MIN_SPREAD) problems.add("La personalidad es plana: ningún rasgo destaca.");

        for (SalventeCapability required : SalventeCapability.REQUIRED)
            if (!p.capabilities.contains(required)) problems.add("Falta la capacidad obligatoria " + required.label + ".");
        if (p.capabilities.size() > SalventeCapability.MAX) problems.add("Como mucho " + SalventeCapability.MAX + " capacidades.");
        if (traitsOk) for (SalventeCapability c : p.capabilities)
            for (Map.Entry<SalventeTrait, Double> need : c.minimums.entrySet())
                if (p.trait(need.getKey()) < need.getValue())
                    problems.add(c.label + " necesita " + need.getKey().label().toLowerCase(Locale.ROOT) + " ≥ "
                            + Math.round(need.getValue() * 100) + " %.");
        return new Result(problems);
    }

    public static void requireValid(SalventeProfile p, Collection<String> takenNames) {
        Result result = validate(p, takenNames);
        if (!result.ok()) throw new IllegalArgumentException(result.toString());
    }

    /** Capacidades que la personalidad sí sostiene (para que el motor no proponga incoherencias). */
    public static boolean supports(Map<SalventeTrait, Double> traits, SalventeCapability capability) {
        for (Map.Entry<SalventeTrait, Double> need : capability.minimums.entrySet()) {
            Double v = traits.get(need.getKey());
            if (v == null || v < need.getValue()) return false;
        }
        return true;
    }

    /** Forma comparable de un nombre (sin tildes ni mayúsculas): la que debe ser única. */
    public static String nameKey(String name) { return key(name); }

    static String key(String name) {
        return Normalizer.normalize(name.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static boolean hasControl(String text) { return text != null && text.matches("(?s).*\\p{Cntrl}.*"); }
}
