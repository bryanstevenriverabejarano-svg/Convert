package salve.core.salvente;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ficha inmutable de un Salvente: una entidad distinta de Salve, con identificador propio que no
 * cambia nunca, personalidad numérica, capacidades acotadas y propósito. Salve la crea y la
 * coordina, pero no comparte con ella su identidad ni su memoria. Sin Android.
 */
public final class SalventeProfile {
    public static final String SPECIES = "SALVENTE";

    public enum State { ACTIVO, PAUSADO }
    /** De dónde salió la propuesta: el modelo (nodo o móvil) o el motor determinista de contingencia. */
    public enum Origin { MODELO, DETERMINISTA }

    public final String id, name, species, purpose, greeting;
    public final Map<SalventeTrait, Double> traits;
    public final Set<SalventeCapability> capabilities;
    public final State state;
    public final Origin origin;
    public final long createdAt;

    public SalventeProfile(String id, String name, String species, String purpose, String greeting,
                           Map<SalventeTrait, Double> traits, Set<SalventeCapability> capabilities,
                           State state, Origin origin, long createdAt) {
        this.id = id; this.name = name; this.species = species; this.purpose = purpose;
        this.greeting = greeting == null ? "" : greeting;
        EnumMap<SalventeTrait, Double> t = new EnumMap<>(SalventeTrait.class);
        if (traits != null) t.putAll(traits);
        this.traits = Collections.unmodifiableMap(t);
        EnumSet<SalventeCapability> c = EnumSet.noneOf(SalventeCapability.class);
        if (capabilities != null) c.addAll(capabilities);
        this.capabilities = Collections.unmodifiableSet(c);
        this.state = state == null ? State.ACTIVO : state;
        this.origin = origin == null ? Origin.DETERMINISTA : origin;
        this.createdAt = createdAt;
    }

    public double trait(SalventeTrait trait) { Double v = traits.get(trait); return v == null ? Double.NaN : v; }

    public SalventeProfile withName(String newName) {
        return new SalventeProfile(id, newName, species, purpose, greeting, traits, capabilities, state, origin, createdAt);
    }

    public SalventeProfile withCapabilities(Set<SalventeCapability> newCapabilities) {
        return new SalventeProfile(id, name, species, purpose, greeting, traits, newCapabilities, state, origin, createdAt);
    }

    public SalventeProfile withState(State newState) {
        return new SalventeProfile(id, name, species, purpose, greeting, traits, capabilities, newState, origin, createdAt);
    }

    /** Los rasgos marcados (≥0.7 o ≤0.3) convertidos en conducta, de más a menos extremo. */
    public List<String> styleDirectives() {
        List<SalventeTrait> marked = new ArrayList<>();
        for (SalventeTrait trait : SalventeTrait.values()) {
            double v = trait(trait);
            if (!Double.isNaN(v) && (v >= 0.7 || v <= 0.3)) marked.add(trait);
        }
        marked.sort((a, b) -> Double.compare(Math.abs(trait(b) - 0.5), Math.abs(trait(a) - 0.5)));
        List<String> out = new ArrayList<>();
        for (SalventeTrait trait : marked) out.add(trait(trait) >= 0.7 ? trait.high : trait.low);
        return out;
    }

    /**
     * Instrucción de sistema con la que un modelo encarnaría a este Salvente. Deja explícito que no
     * es Salve, que no habla en su nombre y qué no puede hacer.
     */
    public String systemPrompt() {
        StringBuilder out = new StringBuilder("Eres ").append(name)
                .append(", un Salvente: una inteligencia distinta de Salve, creada por ella para ").append(purpose).append(". ");
        List<String> style = styleDirectives();
        if (!style.isEmpty()) out.append("Tu forma de ser: ").append(String.join("; ", style)).append(". ");
        out.append("Puedes: ");
        List<String> can = new ArrayList<>();
        for (SalventeCapability c : capabilities) can.add(c.description);
        out.append(String.join("; ", can)).append(". ");
        out.append("No eres Salve ni hablas en su nombre, no conoces sus recuerdos ni los de Bryan, y no controlas el teléfono.");
        return out.toString();
    }
}
