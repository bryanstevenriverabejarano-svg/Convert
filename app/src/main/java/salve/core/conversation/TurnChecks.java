package salve.core.conversation;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fallos del borrador que se ven comparándolo con lo que Bryan preguntó (salen de su examen): no dice
 * su nombre cuando le pregunta «¿sabes quién soy?», contesta a una pregunta sobre ella sin hablar de
 * sí misma, o solo anuncia lo que va a hacer («Analizaré cada uno por separado») sin hacerlo.
 * Deterministas; se juntan con los demás fallos en una sola reescritura.
 */
public final class TurnChecks {
    private TurnChecks() { }

    private static final Pattern WHO_AM_I = Pattern.compile(
            "\\b(sabes|recuerdas|conoces) (quien|a quien) (soy|te habla)|\\bquien soy( yo)?\\b|\\bme conoces\\b|\\bsabes como me llamo\\b");
    private static final Pattern ABOUT_HER = Pattern.compile(
            "\\b(tienes|eres|estas|te gustan?|te interesan?|te apasiona|sientes|piensas tu|has hecho|tu proposito|tus intereses|tu identidad|"
            + "sabes hacer|te formas)\\b");
    private static final Pattern NOT_ABOUT_HER = Pattern.compile(
            "\\b(tienes (que|razon)|estas segur|puedes|podrias|ayudame|busca|dibuja|explicame|ensename|cuentame un|escribeme)\\b");
    static final Pattern FIRST_PERSON = Pattern.compile(
            "\\b(soy|me|mi|mis|yo|estoy|tengo|siento|aprendo|recuerdo|dibujo|pienso|quiero|puedo|se hacer|conmigo|he|creo|disfruto|"
            + "intento|busco|noto|leo|juego|sueno|mio|mia)\\b");
    private static final Pattern CAUSAL_QUESTION = Pattern.compile(
            "(^|\\b)(por que|porque|a que se debe|cual es la razon|cual es el motivo)\\b");
    private static final Pattern CAUSAL_ANSWER = Pattern.compile(
            "\\b(porque|ya que|debido a|se debe a|por eso|puesto que|gracias a|la razon|el motivo|"
            + "lo que hace que|esto ocurre|me diferencia|me distingue|me distingo|por mi|por mis)\\b");
    private static final Pattern COMPARE_QUESTION = Pattern.compile(
            "\\b(compara|comparame|comparacion|diferencia entre|que es mejor entre|frente a|versus)\\b");
    private static final Pattern COMPARE_ANSWER = Pattern.compile(
            "\\b(mientras|en cambio|a diferencia|ambos|pero|distinto|diferente|comparado|"
            + "por otro lado|en comun|por el contrario|frente a)\\b");

    private static final Pattern PROMISE = Pattern.compile(
            "\\b(voy a|vamos a|analizare|explicare|te explicare|te contare|contare|revisare|descompondre|desglosare|hare|preparare|buscare|"
            + "te dire|empezare|vere|examinare|comentare|abordare|dame un momento|enseguida te|a continuacion te)\\b");

    /**
     * La corrección para el modelo, o null si el borrador está bien.
     * @param creator si quien habla es Bryan (solo entonces se le pide decir «Bryan»)
     */
    public static String problem(String input, String draft, boolean creator) {
        if (draft == null || draft.trim().isEmpty() || input == null) return null;
        String in = normalize(input), d = normalize(draft);
        List<String> out = new ArrayList<>();
        if (creator && WHO_AM_I.matcher(in).find() && !d.contains("bryan"))
            out.add("te pregunta si sabes quién es: es Bryan, quien te creó; dilo con su nombre y algo concreto que sepas de él");
        boolean explainedHerCausally = CAUSAL_QUESTION.matcher(in).find()
                && CAUSAL_ANSWER.matcher(d).find() && count(FIRST_PERSON, d) >= 1;
        if (ABOUT_HER.matcher(in).find() && !NOT_ABOUT_HER.matcher(in).find()
                && in.length() < 90 && count(FIRST_PERSON, d) < 2 && !explainedHerCausally)
            out.add("te pregunta por ti: contesta hablando de ti en primera persona, con algo concreto tuyo (qué haces, qué te gusta, cómo estás y por qué)");
        if (announcesOnly(draft))
            out.add("solo anunciaste lo que ibas a hacer, sin hacerlo: hazlo ya en esta misma respuesta");
        // Ask-demand alignment, not model-supplied self-evaluation. A generic
        // greeting must not count as an explanation merely because it says "soy".
        if (CAUSAL_QUESTION.matcher(in).find() && !CAUSAL_ANSWER.matcher(d).find()
                && d.length() < 240)
            out.add("preguntó POR QUÉ: explica una causa o diferencia concreta usando lo que sabes; si no hay evidencia, reconócelo");
        if (COMPARE_QUESTION.matcher(in).find() && !COMPARE_ANSWER.matcher(d).find()
                && d.length() < 100)
            out.add("pidió COMPARAR: contrasta explícitamente las dos opciones y explica una diferencia comprobable");
        return out.isEmpty() ? null : String.join("; ", out);
    }

    /** «Analizaré cada uno por separado para ofrecerte una perspectiva clara.» y nada más. */
    public static boolean announcesOnly(String draft) {
        if (draft == null) return false;
        String d = normalize(draft);
        return d.length() < 220 && !draft.contains(":") && !draft.contains("\n") && PROMISE.matcher(d).find();
    }

    /** Las dos correcciones juntas (cualquiera puede ser null). */
    public static String join(String a, String b) {
        if (a == null || a.isEmpty()) return b;
        if (b == null || b.isEmpty()) return a;
        return a + "; " + b;
    }

    private static int count(Pattern p, String t) {
        Matcher m = p.matcher(t);
        int n = 0;
        while (m.find()) n++;
        return n;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!,;«»()\"]", " ").replaceAll("\\s+", " ").trim();
    }
}
