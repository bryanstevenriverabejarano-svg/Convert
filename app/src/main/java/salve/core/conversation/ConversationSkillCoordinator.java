package salve.core.conversation;

import java.util.function.Consumer;
import java.util.function.Supplier;
import salve.core.mind.SkillLab;

/**
 * Owns the conversational handshake with SkillLab.
 *
 * The model, tool executor, memory store and Android UI live elsewhere. This
 * coordinator only links a verified outcome to the plan that selected it and
 * produces the existing, bounded description for the next conversation turn.
 * It does not infer user approval, train weights, or invent a successful action.
 */
public final class ConversationSkillCoordinator {
    private final Supplier<SkillLab> laboratory;
    private final Consumer<SkillLab.Decision> journal;
    private final StringBuilder turnNotes = new StringBuilder();
    private SkillLab.Plan responsePlan;

    public ConversationSkillCoordinator(Supplier<SkillLab> laboratory,
                                        Consumer<SkillLab.Decision> journal) {
        if (laboratory == null || journal == null)
            throw new IllegalArgumentException("Faltan dependencias de aprendizaje");
        this.laboratory = laboratory;
        this.journal = journal;
    }

    /** Interpret feedback on the *previous* outcome, never as verified facts about the world. */
    public synchronized SkillLab.Feedback beginTurn(String input) {
        turnNotes.setLength(0);
        try {
            SkillLab lab=laboratory.get();
            SkillLab.Feedback confirmed=lab.hasPendingFeedback(input)
                    ? SkillLab.feedback(input) : SkillLab.Feedback.NONE;
            for (SkillLab.Decision decision : lab.observe(input))
                recordDecision(decision);
            return confirmed;
        } catch (RuntimeException unavailable) {
            // Learning is optional; it must not interrupt the conversation.
            return SkillLab.Feedback.NONE;
        }
    }

    public synchronized void recordDecision(SkillLab.Decision decision) {
        if (decision == null) return;
        journal.accept(decision);
        turnNotes.append(" | LABORATORIO (hecho de este turno, de mis resultados verificados): ")
                .append("experimento".equals(decision.verdict) ? "empiezo un experimento: "
                        : decision.verdict + ": ")
                .append(decision.why)
                .append(". Si viene al caso, menciónalo en una frase.");
    }

    /** Only the source-selection step receives the attribution for this outcome. */
    public synchronized void recordReference(SkillLab.Plan plan, boolean success, String evidence) {
        try {
            recordDecision(laboratory.get().record(plan, success,
                    success ? null : "fuente", "busqueda", evidence));
        } catch (RuntimeException unavailable) { /* Optional: never block rendering. */ }
    }

    /** A separate visual verifier may be absent; null means no automatic learning. */
    public synchronized void awaitDrawing(SkillLab.Plan plan, Boolean acceptable, String evidence) {
        if (plan == null) return;
        try { laboratory.get().await(plan, acceptable, "vision", evidence); }
        catch (RuntimeException unavailable) { /* No fabricated success. */ }
    }

    /** Prepare a reusable response variant if a task is actually in progress. */
    public synchronized String responseStrategy(DialogueFocus.Reading reading) {
        responsePlan = null;
        try {
            if (reading == null || reading.task.isEmpty()
                    && reading.kind == DialogueFocus.Kind.NONE) return "";
            SkillLab.Plan plan = laboratory.get().plan(SkillLab.RESPONDER, "tarea");
            responsePlan = plan;
            String instruction;
            switch (plan.variant("enfoque")) {
                case "anunciar_y_hacer":
                    instruction = " | ESTRATEGIA (la estoy probando porque mi forma anterior de responder en tareas fallaba): empieza con "
                            + "media frase que diga qué vas a hacer y hazlo en este mismo mensaje.";
                    break;
                case "un_paso":
                    instruction = " | ESTRATEGIA (la estoy probando porque mi forma anterior de responder en tareas fallaba): haz sólo el "
                            + "siguiente paso concreto de lo que pide, breve y útil, sin introducciones.";
                    break;
                default: instruction = "";
            }
            String confidence = laboratory.get().confidenceNote(plan);
            return instruction + (confidence.isEmpty() ? "" : confidence
                    + " Haz exactamente lo que pide, sin rodeos.");
        } catch (RuntimeException unavailable) { return ""; }
    }

    /**
     * A generated answer is NOT evidence of correctness. Keep its strategy
     * pending, but grant no positive credit without explicit user feedback
     * or an independent verifier. Silence and topic changes remain UNKNOWN.
     */
    public synchronized void responseFinished(boolean generated, String subject) {
        SkillLab.Plan plan = responsePlan;
        responsePlan = null;
        if (plan == null || !generated) return;
        try { laboratory.get().await(plan, null, "pendiente_verificacion", subject); }
        catch (RuntimeException unavailable) { /* No impact on sending the reply. */ }
    }

    /** Consume notes once even when nobody asked for the skill catalogue. */
    public synchronized String describeSkills(String input) {
        String notes = turnNotes.toString();
        turnNotes.setLength(0);
        try {
            if (!SkillLab.asksAbout(input)) return notes;
            String facts = laboratory.get().render(700);
            return notes + " | MIS_HABILIDADES (hechos de mi laboratorio: resultados verificados, experimentos y decisiones)"
                    + (facts.isEmpty() ? ": aún no tengo suficientes resultados verificados; dilo así, con honestidad."
                    : ":" + facts + "\nCuéntalo con tus palabras y criterio propio; no inventes cifras ni habilidades.");
        } catch (RuntimeException unavailable) { return notes; }
    }
}
