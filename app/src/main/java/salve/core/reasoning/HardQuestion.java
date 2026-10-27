package salve.core.reasoning;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ¿Merece la pena que Salve «piense» antes de responder? El cómputo en tiempo de prueba (razonar
 * antes de contestar) mejora mucho los problemas de varios pasos, pero cuesta segundos: solo se usa
 * cuando la pregunta lo pide (acertijos, problemas con varios datos, «razona», «paso a paso»,
 * planes, demostraciones). La charla normal sigue siendo inmediata.
 */
public final class HardQuestion {
    private HardQuestion() { }

    /** Tokens máximos de pensamiento: acota la espera (~10–20 s en el S24 Ultra con MTP). */
    public static final int THINKING_BUDGET_TOKENS = 768;

    private static final Pattern EXPLICIT = Pattern.compile(
            "\\b(?:razona(?:lo)?|piensa(?:lo)? bien|piensalo con calma|paso a paso|demuestra(?:lo)?|deduce|justifica"
                    + "|acertijo|adivinanza|enigma|problema de logica|logica|resuelve (?:este|el) problema"
                    + "|plan(?:ifica)? (?:para|de)|estrategia para|que harias (?:si|para)|como (?:organizo|planifico|optimizo)"
                    + "|que es mejor|que me conviene|ventajas y desventajas|pros y contras)\\b");
    private static final Pattern NUMBER = Pattern.compile("\\d+(?:[.,]\\d+)?");
    private static final Pattern QUANTITY_QUESTION = Pattern.compile(
            "\\b(?:cuant[oa]s?|cuanto tiempo|cuanto tarda|que edad|a que hora|cuantos dias|que porcentaje|cuanto (?:me|le|nos) (?:queda|sobra|falta|cuesta))\\b");

    public static boolean needsThinking(String input) {
        if (input == null) return false;
        String t = Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("\\s+", " ").trim();
        if (t.length() < 12 || t.length() > 1500) return false;
        // Una cuenta directa la resuelve la calculadora exacta, no hace falta pensar.
        if (MathGrounding.requested(input) != null) return false;
        if (EXPLICIT.matcher(t).find()) return true;
        // Problema con enunciado: pregunta de cantidad con al menos dos datos numéricos.
        int numbers = 0;
        Matcher m = NUMBER.matcher(t);
        while (m.find()) numbers++;
        return numbers >= 2 && QUANTITY_QUESTION.matcher(t).find();
    }
}
