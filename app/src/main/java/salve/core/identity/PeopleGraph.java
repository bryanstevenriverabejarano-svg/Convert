package salve.core.identity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Árbol de personas de Salve con su creador como raíz.
 *
 * <p>Es una vista calculada a partir de los datos de perfil guardados en Room («Me llamo…»,
 * «Mi hermana se llama…»), que ya se sincronizan con pCloud. Así hay una sola fuente de verdad:
 * olvidar un dato de perfil lo quita también del árbol.</p>
 */
public final class PeopleGraph {
    public static final String CREATOR_ID = "creator_bryan";
    /** Nombre del creador configurado en la app; el nombre completo declarado lo sustituye. */
    public static final String DEFAULT_CREATOR_NAME = "Bryan";

    public enum Role { CREATOR, FAMILY }

    public static final class PersonEntity {
        public final String id;
        public final String name;
        public final Role role;
        /** Cómo lo ve Salve, en primera persona. */
        public final String roleForSalve;

        PersonEntity(String id, String name, Role role, String roleForSalve) {
            this.id = id; this.name = name; this.role = role; this.roleForSalve = roleForSalve;
        }
    }

    public static final class Relationship {
        public final String fromId;
        public final String toId;
        /** Relación de {@code toId} respecto a {@code fromId}: «hermana», «madre»… */
        public final String type;

        Relationship(String fromId, String toId, String type) {
            this.fromId = fromId; this.toId = toId; this.type = type;
        }
    }

    private final PersonEntity creator;
    private final List<PersonEntity> people = new ArrayList<>();
    private final List<Relationship> relationships = new ArrayList<>();

    private PeopleGraph(PersonEntity creator) {
        this.creator = creator;
        people.add(creator);
    }

    /**
     * @param creatorName nombre declarado por el creador, o null para usar el configurado.
     * @param family pares {relación, nombre} declarados por el creador.
     */
    public static PeopleGraph build(String creatorName, List<String[]> family) {
        String name = creatorName == null || creatorName.trim().isEmpty() ? DEFAULT_CREATOR_NAME : creatorName.trim();
        PeopleGraph graph = new PeopleGraph(new PersonEntity(CREATOR_ID, name, Role.CREATOR,
                "Mi creador, la persona con quien convivo en este dispositivo"));
        if (family != null) for (String[] member : family) {
            if (member == null || member.length < 2 || member[1] == null || member[1].trim().isEmpty()) continue;
            String id = "family_" + SessionIdentity.normalize(member[0]).replace(' ', '_') + "_"
                    + SessionIdentity.normalize(member[1]).replace(' ', '_');
            graph.people.add(new PersonEntity(id, member[1].trim(), Role.FAMILY,
                    "Familia de " + name.split(" ")[0] + "; alguien a quien cuido y respeto"));
            graph.relationships.add(new Relationship(CREATOR_ID, id, member[0]));
        }
        return graph;
    }

    public PersonEntity creator() { return creator; }
    public List<PersonEntity> people() { return Collections.unmodifiableList(people); }
    public List<Relationship> relationships() { return Collections.unmodifiableList(relationships); }

    /** Pares {relación, nombre} para comparar con el nombre que alguien declara. */
    public List<String[]> familyPairs() {
        List<String[]> out = new ArrayList<>();
        for (Relationship r : relationships) {
            PersonEntity p = find(r.toId);
            if (p != null) out.add(new String[]{r.type, p.name});
        }
        return out;
    }

    public PersonEntity find(String id) {
        for (PersonEntity p : people) if (p.id.equals(id)) return p;
        return null;
    }

    /** Descripción de la familia adecuada al rol de quien habla (privacidad por rol). */
    public String familyFor(SessionIdentity.State state, String speakerName) {
        if (relationships.isEmpty()) return "";
        if (state == SessionIdentity.State.VERIFIED_CREATOR || state == SessionIdentity.State.PRESUMED_CREATOR) {
            List<String> parts = new ArrayList<>();
            for (Relationship r : relationships) {
                PersonEntity p = find(r.toId);
                if (p != null) parts.add("tu " + display(r.type) + " " + p.name);
            }
            return "Familia que me has presentado: " + String.join(", ", parts) + ".";
        }
        if (state == SessionIdentity.State.FAMILY && speakerName != null) {
            for (Relationship r : relationships) {
                PersonEntity p = find(r.toId);
                if (p != null && SessionIdentity.matches(p.name, speakerName))
                    return "Quien te habla es " + p.name + ", " + display(r.type) + " de " + creator.name.split(" ")[0] + ".";
            }
        }
        return "";
    }

    public static String display(String relation) {
        if (relation == null) return "";
        switch (relation.toLowerCase(Locale.ROOT)) {
            case "mama": return "mamá";
            case "papa": return "papá";
            case "tia": return "tía";
            case "tio": return "tío";
            case "cunada": return "cuñada";
            case "cunado": return "cuñado";
            default: return relation.toLowerCase(Locale.ROOT);
        }
    }
}
