package salve.core.conversation;

/**
 * Cuánto lee el modelo antes de responder. La espera hasta la primera palabra es, sobre todo, leer el
 * prompt (en el S24 Ultra con Gemma 4 E4B: ~3.000 tokens → 4–8 s). Un saludo o charla corta no
 * necesita recuerdos ni explicaciones largas: con la mitad de prompt contesta en la mitad de tiempo.
 * Lo que sí necesita contexto (memoria, verificación, «¿recuerdas…?», fotos, mensajes largos) sigue
 * con el presupuesto completo.
 */
public final class PromptBudgetPolicy {
    /** Prompt ligero: compacta las reglas, la identidad, el modelo de sí misma y las últimas frases. */
    public static final int LEAN_CHARS = 6000;
    /** Un mensaje más largo suele traer contenido que hay que entender con calma. */
    static final int LEAN_MAX_INPUT = 160;

    private PromptBudgetPolicy() { }

    public static int forTurn(int base, boolean needsMemory, boolean verify, boolean recall,
                              boolean photo, boolean story, int inputLength) {
        if (base <= LEAN_CHARS) return base;
        if (needsMemory || verify || recall || photo || story || inputLength > LEAN_MAX_INPUT) return base;
        return LEAN_CHARS;
    }
}
