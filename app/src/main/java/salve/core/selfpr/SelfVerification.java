package salve.core.selfpr;

import android.content.Context;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.util.List;

/**
 * Cierra el ciclo de sus cambios: proponer → instalar → medir → veredicto.
 *
 * Un cambio suyo queda «sin comprobar» cuando la app se actualizó después de proponerlo y todavía no
 * se ha examinado con la versión nueva. El primer examen que haga a partir de ahí lo compara con el
 * que tenía al proponerlo y deja un veredicto (mejor, igual, peor) que ya no se reescribe. Así una
 * mejora deja de ser «compiló» y pasa a ser «arregló más de lo que rompió», o se señala para revertir.
 *
 * Límites que el propio texto dice: que la app se actualizara no demuestra que esa propuesta llegara a
 * fusionarse, y entre dos exámenes pueden haber cambiado más cosas (otro modelo, otros parches).
 */
public final class SelfVerification {
    private SelfVerification() { }

    public static File verdictsFile(Context context) {
        return new File(new File(context.getFilesDir(), "selfpr"), "veredictos.jsonl");
    }
    public static File lineageFile(Context context) {
        return new File(new File(context.getFilesDir(), "selfpr"), "linaje.jsonl");
    }
    /** Cuándo se instaló la versión que está corriendo; 0 si Android no lo dice. */
    public static long appUpdatedAt(Context context) {
        try { return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).lastUpdateTime; }
        catch (Exception unknown) { return 0L; }
    }

    /** Su último cambio, si la app se actualizó después de proponerlo y aún no tiene veredicto; si no, null. */
    public static JsonObject pending(List<JsonObject> lineage, List<JsonObject> verdicts, long appUpdatedAt) {
        if (lineage == null || lineage.isEmpty() || appUpdatedAt <= 0) return null;
        JsonObject last = lineage.get(lineage.size() - 1);
        long proposed = last.has("t") ? last.get("t").getAsLong() : 0L;
        String issue = text(last, "issue");
        if (proposed <= 0 || appUpdatedAt <= proposed || issue.isEmpty()) return null;
        if (verdicts != null) for (JsonObject v : verdicts) if (issue.equals(text(v, "issue"))) return null;
        return last;
    }

    /** El veredicto de un cambio con el examen recién hecho; null si falta alguna de las dos medidas. */
    public static JsonObject verdict(JsonObject change, int failuresNow, long now) {
        if (change == null || failuresNow < 0) return null;
        int before = change.has("fallos_examen") ? change.get("fallos_examen").getAsInt() : -1;
        if (before < 0) return null;
        JsonObject v = new JsonObject();
        v.addProperty("t", now);
        v.addProperty("issue", text(change, "issue"));
        v.addProperty("archivo", text(change, "archivo"));
        v.addProperty("objetivo", text(change, "objetivo"));
        v.addProperty("revision", text(change, "revision"));
        v.addProperty("antes", before);
        v.addProperty("ahora", failuresNow);
        v.addProperty("resultado", failuresNow < before ? "mejor" : failuresNow > before ? "peor" : "igual");
        return v;
    }

    public static String sentence(JsonObject verdict) {
        if (verdict == null) return "";
        String file = text(verdict, "archivo");
        file = file.substring(file.lastIndexOf('/') + 1);
        String goal = text(verdict, "objetivo");
        String what = "mi último cambio (" + file + (goal.isEmpty() ? "" : ": «" + goal + "»") + ")";
        int before = verdict.get("antes").getAsInt(), now = verdict.get("ahora").getAsInt();
        String counts = "de " + before + " a " + now + " casos fallidos";
        switch (text(verdict, "resultado")) {
            case "mejor": return "Veredicto de " + what + ": el examen mejoró, " + counts + ". Lo doy por bueno.";
            case "peor": return "Veredicto de " + what + ": el examen empeoró, " + counts + ". Si esa propuesta llegó a fusionarse, "
                    + "recomiendo revertir su commit (lo encuentras en " + text(verdict, "issue") + ") y repetir el examen. "
                    + "Entre los dos exámenes pudo cambiar algo más, así que es un indicio, no una prueba.";
            default: return "Veredicto de " + what + ": sin cambio medible en el examen (" + before + " casos fallidos antes y ahora). "
                    + "No lo rompió, pero mi examen tampoco demuestra que ayudara.";
        }
    }

    public static String pendingNotice(JsonObject change) {
        if (change == null) return "";
        String file = text(change, "archivo");
        return "Tengo un cambio mío sin comprobar (" + file.substring(file.lastIndexOf('/') + 1)
                + "): la app se actualizó después de proponerlo y aún no me he examinado con esta versión. "
                + "En cuanto haga el examen dejaré su veredicto.";
    }

    /** Lo que hay que decir ahora mismo sobre la comprobación de sus cambios; "" si nada. */
    public static String state(Context context) {
        List<JsonObject> verdicts = SelfLineage.read(verdictsFile(context), 50);
        JsonObject waiting = pending(SelfLineage.read(lineageFile(context), 5), verdicts, appUpdatedAt(context));
        if (waiting != null) return pendingNotice(waiting);
        return verdicts.isEmpty() ? "" : sentence(verdicts.get(verdicts.size() - 1));
    }

    /** Tras un examen: si había un cambio sin comprobar, guarda su veredicto y devuelve la frase; si no, "". */
    public static String afterExam(Context context, int failuresNow, long now) {
        File file = verdictsFile(context);
        JsonObject waiting = pending(SelfLineage.read(lineageFile(context), 5), SelfLineage.read(file, 50), appUpdatedAt(context));
        JsonObject verdict = verdict(waiting, failuresNow, now);
        if (verdict == null) return "";
        try { SelfLineage.append(file, verdict); }
        catch (java.io.IOException unsaved) { return ""; }
        return sentence(verdict);
    }

    private static String text(JsonObject e, String key) {
        JsonElement v = e.get(key);
        return v == null || !v.isJsonPrimitive() ? "" : v.getAsString();
    }
}
