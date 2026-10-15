package salve.core.identity;

/**
 * Bloque breve que recibe el modelo en cada turno: quién le habla y con qué certeza.
 *
 * <p>Es deliberadamente corto y sin frases de ejemplo: un modelo pequeño copia los ejemplos
 * («te veo…») y repite el bloque en lugar de responder. Da criterios; la forma la elige Salve.</p>
 */
public final class IdentityPrompt {
    private IdentityPrompt() {}

    public static String render(SessionIdentity identity, PeopleGraph graph, boolean biometricAvailable, long now) {
        String creator = graph.creator().name;
        String first = creator.split(" ")[0];
        SessionIdentity.State state = identity.state();
        StringBuilder out = new StringBuilder("INTERLOCUTOR: ");
        switch (state) {
            case VERIFIED_CREATOR:
                out.append(creator).append(", tu creador (verificado por el teléfono). Háblale de tú, en masculino; con plena confianza.");
                break;
            case PRESUMED_CREATOR:
                out.append(creator).append(", tu creador (es su teléfono). Háblale de tú, en masculino.");
                break;
            case UNCERTAIN:
                out.append("no está claro quién es").append(identity.claimedCreator() ? " (dice ser " + first + ")" : "")
                        .append(". Sé cálida y averigua con naturalidad con quién hablas; no compartas lo privado de ")
                        .append(first).append(biometricAvailable ? " salvo que se verifique («verifícame»)." : ".");
                break;
            case FAMILY:
                out.append(identity.name()).append(", ").append(PeopleGraph.display(identity.relation())).append(" de ").append(first)
                        .append(". Trato cercano; sin contar lo privado de ").append(first).append('.');
                break;
            case GUEST:
            default:
                out.append(identity.name() == null ? "alguien que no es " + first : identity.name() + " (invitado)")
                        .append(". Amable; protege la privacidad de ").append(first).append('.');
        }
        String family = graph.familyFor(state, identity.name());
        if (!family.isEmpty()) out.append('\n').append(family);
        return out.toString();
    }
}
