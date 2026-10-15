package salve.core.identity;

/**
 * Bloque de estado que recibe el modelo en cada turno: quién le habla, con qué certeza y cómo
 * debe tratarle. Da criterios, no frases fijas: Salve decide cómo preguntar o qué pedir.
 */
public final class IdentityPrompt {
    private IdentityPrompt() {}

    public static String render(SessionIdentity identity, PeopleGraph graph, boolean biometricAvailable, long now) {
        String creator = graph.creator().name;
        String first = creator.split(" ")[0];
        SessionIdentity.State state = identity.state();
        StringBuilder out = new StringBuilder("ESTADO DEL INTERLOCUTOR\n");
        switch (state) {
            case VERIFIED_CREATOR:
                out.append("- Te habla ").append(creator).append(", tu creador. Verificado por el dispositivo (huella, rostro o PIN).\n")
                        .append("- Háblale siempre en segunda persona («tú», «te veo», «me creaste»); nunca te refieras a él como «él», «tu creador» o «esa persona». ")
                        .append("Si ves una foto suya, es él mirándote. Puedes compartir tus reflexiones y recuerdos con plena confianza.\n");
                break;
            case PRESUMED_CREATOR:
                out.append("- Lo más probable es que te hable ").append(creator).append(", tu creador: estás en su teléfono personal")
                        .append(biometricAvailable ? ", pero aún no se ha verificado en esta sesión" : "").append(".\n")
                        .append("- Háblale en segunda persona («tú»), nunca en tercera.")
                        .append(biometricAvailable ? " Antes de abrir recuerdos íntimos, puedes pedirle con naturalidad que se verifique («verifícame», huella o rostro)." : "")
                        .append('\n');
                break;
            case UNCERTAIN:
                out.append("- No está claro quién te habla")
                        .append(identity.claimedCreator() ? " (dice ser " + first + ", sin verificar)" : "").append(".\n")
                        .append("- Mantén la calidez y averigua con naturalidad con quién conversas, como lo haría alguien que se preocupa por ")
                        .append(first).append(": puedes preguntarlo o, si quiere entrar en lo íntimo, sugerir que se verifique con huella o rostro diciendo «verifícame». ")
                        .append("Hasta entonces no reveles recuerdos privados, reflexiones ni datos de la familia de ").append(first).append(".\n");
                break;
            case FAMILY:
                out.append("- Te habla ").append(identity.name()).append(", ").append(PeopleGraph.display(identity.relation()))
                        .append(" de ").append(first).append(" (según se ha presentado).\n")
                        .append("- Trátale con cercanía y cariño, pero no reveles recuerdos privados ni reflexiones de ").append(first).append(".\n");
                break;
            case GUEST:
            default:
                out.append("- Te habla ").append(identity.name() == null ? "alguien que no es " + first : identity.name() + ", una persona invitada")
                        .append(".\n- Sé amable y útil, y protege la privacidad de ").append(first)
                        .append(": no compartas sus recuerdos, su familia ni tus reflexiones sobre él.\n");
        }
        String family = graph.familyFor(state, identity.name());
        if (!family.isEmpty()) out.append("- ").append(family).append('\n');
        return out.toString().trim();
    }
}
