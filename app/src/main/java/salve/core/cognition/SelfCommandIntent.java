package salve.core.cognition;

import java.text.Normalizer;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Salve's own functions (memory health, ears, voice, mind map, wake word...)
 * used to be literal commands that answered BEFORE the model saw the turn.
 * This gate reuses those same recognizers only to decide which of her own
 * functions the LLM may choose in this turn. It does not execute anything and
 * the model never supplies the parameters: ON/OFF, which voice, which tab,
 * which body movement are re-derived from the real user input by the executor.
 *
 * Read-only functions return their data to the model as a receipt, so the LLM
 * explains it in its own words. Functions with an effect are a handoff.
 */
public final class SelfCommandIntent {
    public static final String TOOL = "self.command";

    public enum Command {
        MEMORY_HEALTH(true, "estado real de tu memoria"),
        SYNERGY_STATUS(true, "estado de tus núcleos/sinergia"),
        MODEL_STATUS(true, "qué modelo usas y cómo va"),
        DOWNLOAD_STATUS(true, "cómo va la descarga del modelo"),
        SELF_PR_STATUS(true, "estado de tus PR en GitHub"),
        IMPROVEMENT_PROPOSALS(true, "qué mejorarías de ti misma, con ejemplos"),
        CAUSAL_MEMORY(true, "las causas que te ha contado Bryan («X porque Y»)"),
        EVOLUTION_STATUS(true, "en qué fase de tu evolución estás y por qué"),
        EAR_MODE(false, "activar/desactivar modo oídos según lo que dijo"),
        VOICE_ENGINE(false, "cambiar entre tu voz nueva y la de Android según lo pedido"),
        LISTEN(false, "escuchar con el micrófono lo que pidió"),
        MIND_MAP(false, "abrir el mapa de tu mente"),
        OPEN_JOURNAL(false, "abrir tu registro de ejecución o tu biblioteca de informes"),
        BODY_ACTION(false, "mover tu cuerpo: parpadear, pelo o brillar"),
        WAKE_WORD_ON(false, "activar que te despierten diciendo tu nombre"),
        WAKE_WORD_OFF(false, "desactivar la palabra de activación"),
        DEFAULT_ASSISTANT(false, "abrir ajustes para ser su asistente predeterminado"),
        CHESS_START(false, "empezar una partida de ajedrez"),
        STORY_SHELF(false, "abrir tu repertorio de cuentos guardados"),
        SELF_PR_OPEN(false, "escribir tu propio parche y abrir un PR");

        /** Read-only: the receipt goes back to the model instead of ending the turn. */
        public final boolean readOnly;
        public final String label;
        Command(boolean readOnly, String label) { this.readOnly = readOnly; this.label = label; }
    }

    // An explicit prohibition is never offered to the model. It falls back to
    // the legacy parsers, which already understand their own "desactiva" forms.
    private static final Pattern NEGATED = ActionMeaning.NEGATED;

    private SelfCommandIntent() { }

    /** All own functions the CURRENT user turn asked for (normally one). */
    public static Set<Command> detected(String input) {
        if (input == null) return Collections.emptySet();
        String s = normalize(input);
        if (s.isEmpty() || s.length() > 300 || NEGATED.matcher(s).find())
            return Collections.emptySet();
        EnumSet<Command> found = EnumSet.noneOf(Command.class);
        if (salve.core.mind.MemoryHealth.isCommand(input)) found.add(Command.MEMORY_HEALTH);
        if (salve.core.synergy.SynergyRequests.isStatus(input)) found.add(Command.SYNERGY_STATUS);
        if (salve.core.ModelRuntimeInfo.isStatusQuestion(input)) found.add(Command.MODEL_STATUS);
        if (salve.core.voice.ModelSwitchCommand.asksDownloadStatus(input)) found.add(Command.DOWNLOAD_STATUS);
        if (salve.core.selfpr.SelfPrCommands.isStatusCommand(input)) found.add(Command.SELF_PR_STATUS);
        else if (salve.core.selfpr.SelfPrCommands.isCommand(input)) found.add(Command.SELF_PR_OPEN);
        if (salve.core.evaluation.ImprovementProposals.isCommand(input)) found.add(Command.IMPROVEMENT_PROPOSALS);
        if (salve.core.voice.EarMode.parse(input) != null) found.add(Command.EAR_MODE);
        if (salve.core.voice.VoiceEngineCommand.parse(input) != null) found.add(Command.VOICE_ENGINE);
        if (salve.core.voice.ListenCommand.parse(input) != null) found.add(Command.LISTEN);
        if (salve.core.mind.MindMapCommand.matches(input)) found.add(Command.MIND_MAP);
        if (salve.core.reports.ReportLibrary.openRequest(input) != null) found.add(Command.OPEN_JOURNAL);
        if (salve.avatar.AstralBodySense.isCommand(input)) found.add(Command.BODY_ACTION);
        if (salve.core.voice.WakeWord.asksToEnable(input)) found.add(Command.WAKE_WORD_ON);
        else if (salve.core.voice.WakeWord.asksToDisable(input)) found.add(Command.WAKE_WORD_OFF);
        if (salve.core.voice.AssistantLaunch.asksToBecomeAssistant(input)) found.add(Command.DEFAULT_ASSISTANT);
        if (CHESS.matcher(s).find()) found.add(Command.CHESS_START);
        if (salve.core.reading.DocumentReading.asksForStoryList(input)) found.add(Command.STORY_SHELF);
        return Collections.unmodifiableSet(found);
    }

    private static final Pattern CHESS = Pattern.compile(
            "\\b(?:jugar al ajedrez|juguemos (?:al )?ajedrez|jugar ajedrez|echamos una partida de ajedrez)\\b");

    /** Lo pedido por frase conocida o, si su modelo de significado lo entendió en este turno, por significado. */
    public static Set<Command> understood(String input) {
        Set<Command> exact = detected(input);
        ActionMeaning.Match meaning = ActionMeaning.forTurn(input);
        // Lo entendido puede ser una herramienta del móvil (su id lleva punto): eso no es función propia.
        Command command = meaning == null || meaning.command.indexOf('.') >= 0 ? null : Command.valueOf(meaning.command);
        if (command == null || exact.contains(command)) return exact;
        EnumSet<Command> out = EnumSet.noneOf(Command.class);
        out.addAll(exact);
        out.add(command);
        return Collections.unmodifiableSet(out);
    }

    public static boolean offered(String input) { return !understood(input).isEmpty(); }

    /**
     * Las palabras de las que salen los detalles: las de Bryan si su reconocedor las entiende; si solo se
     * entendieron por significado, la frase canónica emparejada por el controlador (nunca texto del modelo).
     */
    public static String wordsFor(String input, Command command) {
        if (detected(input).contains(command)) return input;
        ActionMeaning.Match meaning = ActionMeaning.forTurn(input);
        return meaning != null && command.name().equals(meaning.command) ? meaning.canonical : input;
    }

    /**
     * Lo que el modelo puede elegir este turno: las funciones con efecto, solo si las palabras de Bryan las
     * piden (de ellas salen los detalles); las de solo lectura, siempre. Así entiende con su LLM qué le
     * preguntan («¿cómo vas evolucionando?», «¿qué te falla?») sin depender de una frase exacta.
     */
    public static Set<Command> available(String input) {
        EnumSet<Command> out = EnumSet.noneOf(Command.class);
        out.addAll(understood(input));
        if (input == null || input.trim().isEmpty()) return Collections.unmodifiableSet(out);
        for (Command c : Command.values()) if (c.readOnly) out.add(c);
        return Collections.unmodifiableSet(out);
    }

    /**
     * The model selects an id. A read-only function can be chosen whenever the model understands it is asked
     * for (it only reads her own state); a function with an effect only if the user's own words asked for it,
     * by a known phrase or by meaning (matched by the controller for this exact input).
     */
    public static Command verified(String input, String proposed) {
        if (proposed == null) throw new IllegalArgumentException("Falta la función.");
        Command chosen;
        try {
            chosen = Command.valueOf(proposed.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            throw new IllegalArgumentException("Función propia desconocida.");
        }
        if (!chosen.readOnly && !understood(input).contains(chosen))
            throw new IllegalArgumentException("El usuario no pidió esa función en este turno.");
        return chosen;
    }

    /** Short catalog line: only the ids offered this turn, to fit small prompts. */
    public static String description(Set<Command> offered) {
        StringBuilder text = new StringBuilder("{id:");
        boolean first = true;
        for (Command c : offered) {
            text.append(first ? "'" : "|'").append(c.name()).append('\'');
            first = false;
        }
        text.append("}: usa una función tuya pedida por el usuario (");
        first = true;
        for (Command c : offered) {
            if (!first) text.append("; ");
            text.append(c.name()).append('=').append(c.label);
            first = false;
        }
        return text.append("). Úsala solo si de verdad te preguntan por eso; los detalles (encender/apagar, cuál) salen de lo que dijo, no de ti.")
                .toString();
    }

    static String normalize(String raw) {
        return Normalizer.normalize(raw.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]+", " ")
                .replaceAll("\\s+", " ").trim();
    }
}
