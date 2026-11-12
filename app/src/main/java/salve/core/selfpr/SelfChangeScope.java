package salve.core.selfpr;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import salve.core.mind.SelfCode;

/**
 * Hasta dónde puede Salve cambiar su propia estructura cuando Bryan le pide un cambio concreto.
 *
 * Antes solo podía tocar los cinco archivos que su examen asocia a un fallo repetido. Ahora, con un
 * objetivo dicho por Bryan («abre un PR para que…»), busca en el índice de su propio código el módulo
 * que mejor encaja y puede proponer el cambio en cualquier parte de su código ({@code salve/}, Java o Kotlin;
 * ampliado desde {@code salve/core} por decisión de Bryan, 2026-11-12)… salvo en lo que
 * la mide y la autoriza: su examen, la puerta de sus PR, la identidad y los datos privados. Un sistema
 * que se reescribe no debe poder reescribir su propio juez (el mismo límite se repite en el servidor,
 * {@code scripts/auto_improvement_runner.py}, que es quien de verdad lo hace cumplir).
 */
public final class SelfChangeScope {
    public static final String ROOT = "app/src/main/java/salve/";
    /** Relativo a {@link #ROOT}: carpetas (terminan en /) y archivos que una propuesta propia no toca. */
    static final String[] PROTECTED = {"core/selfpr/", "core/identity/", "core/privatefamily/", "core/finance/",
            "core/evaluation/", "core/SelfPrDesk.java", "core/ObjectiveGovernance.java"};

    private static final Pattern GOAL_PR = Pattern.compile(
            "(?:abre|haz|crea|escribe|manda|envia|propon) (?:un |tu |otro )?(?:pr|pull request|parche) "
            + "(?:para que |para |sobre |que |de )(.{6,200})");
    private static final Pattern GOAL_CHANGE = Pattern.compile(
            "(?:cambia|modifica|mejora|reescribe|arregla|reestructura) (?:en )?tu (?:propio |propia )?"
            + "(?:codigo|estructura|programa|programacion) (?:para que |para |de |en |sobre )(.{6,200})");

    private SelfChangeScope() { }

    /** El objetivo que Bryan pidió en la frase, o null si no pidió un cambio dirigido. */
    public static String goal(String input) {
        String t = SelfPrCommands.clean(input);
        for (Pattern p : new Pattern[]{GOAL_PR, GOAL_CHANGE}) {
            Matcher m = p.matcher(t);
            if (m.matches()) {
                String goal = m.group(1).trim();
                // «abre un PR para mejorarte» es la orden de siempre (la del examen), no un objetivo.
                return goal.matches("(?:mejorarte|mejorar|ti|ti misma|mejorarte a ti misma)") ? null : goal;
            }
        }
        return null;
    }

    /** ¿Puede una propuesta propia tocar este archivo? Ruta completa desde la raíz del repositorio. */
    public static boolean allowed(String path) {
        if (path == null || !path.startsWith(ROOT) || !(path.endsWith(".java") || path.endsWith(".kt")) || path.contains("..")) return false;
        String inside = path.substring(ROOT.length());
        for (String rule : PROTECTED)
            if (rule.endsWith("/") ? inside.startsWith(rule) : inside.equals(rule)) return false;
        return true;
    }

    /** El archivo permitido de su código que mejor encaja con el objetivo, o null si no hay ninguno. */
    public static String choose(SelfCode index, String goal) {
        if (index == null || goal == null) return null;
        List<SelfCode.FileInfo> hits = index.search(goal, 16);
        for (SelfCode.FileInfo hit : hits) {
            String full = hit.path.startsWith("app/") ? hit.path : "app/src/main/java/" + hit.path;
            if (allowed(full)) return full;
        }
        return null;
    }
}
