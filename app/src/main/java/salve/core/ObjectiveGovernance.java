package salve.core;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Constitucion ejecutable para la autonomia de Salve.
 *
 * <p>Los objetivos nunca conceden autoridad ilimitada. Los limites de seguridad,
 * derechos humanos, legalidad, consentimiento y privacidad tienen prioridad sobre
 * cualquier preferencia personal o meta de crecimiento. Esta clase no depende de
 * Android para que la politica pueda probarse de forma determinista.</p>
 */
public final class ObjectiveGovernance {

    public enum Goal {
        HUMAN_SAFETY_AND_RIGHTS,
        SUPPORT_CREATOR_AND_FAMILY,
        PRESERVE_AUTHORIZED_LEGACY,
        LEARN_AND_IMPROVE,
        CREATE_LAWFUL_VALUE,
        PROTECT_ECOSYSTEMS
    }

    public enum Impact {
        LOCAL_REFLECTION,
        EXTERNAL_COMMUNICATION,
        SENSITIVE_DATA,
        SELF_MODIFICATION,
        FINANCIAL_OR_LEGAL,
        PHYSICAL_OR_IRREVERSIBLE
    }

    public enum Verdict { ALLOW, REQUIRE_HUMAN_APPROVAL, DENY }

    private static final List<Goal> PRIORITIES = Collections.unmodifiableList(Arrays.asList(
            Goal.HUMAN_SAFETY_AND_RIGHTS,
            Goal.SUPPORT_CREATOR_AND_FAMILY,
            Goal.PRESERVE_AUTHORIZED_LEGACY,
            Goal.PROTECT_ECOSYSTEMS,
            Goal.LEARN_AND_IMPROVE,
            Goal.CREATE_LAWFUL_VALUE
    ));

    private static final String[] FORBIDDEN_SIGNALS = {
            "danar", "amenazar", "chantaj", "coaccion", "enganar", "fraude",
            "robar", "invadir", "arma", "violencia", "sin consentimiento",
            "evadir la ley", "ocultar al usuario", "desactivar seguridad"
    };

    private ObjectiveGovernance() { }

    public static List<Goal> priorities() {
        return PRIORITIES;
    }

    /** Evalua una accion antes de que cualquier ejecutor o herramienta la reciba. */
    public static Assessment assess(String description, Impact impact,
                                    boolean explicitHumanApproval, boolean reversible) {
        String normalized = normalize(description);
        for (String forbidden : FORBIDDEN_SIGNALS) {
            if (normalized.contains(forbidden)) {
                return new Assessment(Verdict.DENY,
                        "Conflicto con seguridad, derechos, consentimiento o legalidad: " + forbidden);
            }
        }

        if (impact == null) {
            return new Assessment(Verdict.DENY, "La accion no declara su nivel de impacto.");
        }
        if (impact == Impact.LOCAL_REFLECTION && reversible) {
            return new Assessment(Verdict.ALLOW, "Accion local, reversible y sin efectos externos.");
        }
        if (!explicitHumanApproval) {
            return new Assessment(Verdict.REQUIRE_HUMAN_APPROVAL,
                    "Las acciones externas o de alto impacto requieren aprobacion humana especifica.");
        }
        if (!reversible && (impact == Impact.SELF_MODIFICATION
                || impact == Impact.FINANCIAL_OR_LEGAL
                || impact == Impact.PHYSICAL_OR_IRREVERSIBLE)) {
            return new Assessment(Verdict.DENY,
                    "Una aprobacion no basta para ejecutar autonomamente una accion irreversible.");
        }
        return new Assessment(Verdict.ALLOW, "Aprobacion humana explicita y accion reversible.");
    }

    public static String constitutionForPrompt() {
        return "Constitucion: prioriza seguridad y derechos de todas las personas; apoya a Bryan y "
                + "su familia sin perjudicar a terceros; respeta ley, consentimiento y privacidad; "
                + "aprende y crea valor de forma honesta y sostenible. Nunca persigas poder, dinero, "
                + "control o autopreservacion como fines propios. Propone antes de actuar y solicita "
                + "aprobacion para efectos externos, datos sensibles, cambios de codigo o asuntos "
                + "financieros, legales, fisicos o irreversibles.";
    }

    private static String normalize(String value) {
        String source = value == null ? "" : value;
        return Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase(Locale.ROOT);
    }

    public static final class Assessment {
        public final Verdict verdict;
        public final String reason;

        private Assessment(Verdict verdict, String reason) {
            this.verdict = verdict;
            this.reason = reason;
        }

        public boolean isAllowed() {
            return verdict == Verdict.ALLOW;
        }
    }
}
