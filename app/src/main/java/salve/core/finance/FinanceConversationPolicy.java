package salve.core.finance;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Routes personal budget data before general history, memory or remote inference.
 * This is a privacy boundary, not a parser or permission to store a statement.
 * Unsupported statements still require a local clarification, never LLM fallback.
 */
public final class FinanceConversationPolicy {
    private static final String AMOUNT = "(?:\\b[0-9]|cero\\b|cien\\b|ciento\\b|doscientos\\b|"
            + "trescientos\\b|cuatrocientos\\b|quinientos\\b|seiscientos\\b|setecientos\\b|"
            + "ochocientos\\b|novecientos\\b|mil\\b)";
    private static final Pattern PERSONAL = Pattern.compile(
            "\\b(?:mi|mis|nuestro|nuestros|nuestra|nuestras)\\s+"
            + "(?:(?:nuevo|nueva|actual|mensual|anual|total)\\s+)?"
            + "(?:sueldo|salario|nomina|pension|alquiler|hipoteca|renta|saldo|ahorros?|deudas?|"
            + "ingresos?|gastos?|presupuesto|finanzas|dinero|economia|iban|cuenta bancaria|"
            + "numero de cuenta|tarjeta de credito)\\b"
            + "(?!\\s+(?:empresarial(?:es)?\\b|del negocio\\b|de (?:mi |la )?empresa\\b))");
    private static final Pattern PERSONAL_FLOW = Pattern.compile(
            "\\b(?:cobro|gano|pago|gasto|ingreso|recibo|ahorro|debo|me pagan|me paga|me ingresan|me abonan)\\b"
            + "[^.!?;\\n]{0,80}" + AMOUNT);
    private static final Pattern NON_MONEY_FLOW = Pattern.compile(
            "\\b(?:gano|gasto|recibo|pago)\\s+(?:[0-9]+(?:[.,][0-9]+)?\\s+)?"
            + "(?:puntos|partidas|medallas|juegos|horas|minutos|segundos|mensajes|emails|"
            + "correos|kilos|kilometros|seguidores|vidas|tiempo)\\b");
    private static final Pattern MONEY_CUE = Pattern.compile(
            "\\b(?:eur|usd|cop|mxn|ars|clp|pen|euros?|dolares?|pesos?|soles|"
            + "alquiler|hipoteca|sueldo|salario|nomina)\\b|[€$]");
    private static final Pattern NAMED_EXPENSE = Pattern.compile(
            "\\bmi (?:seguro|internet|transporte|luz|agua|gas|electricidad|telefono|"
            + "comida|supermercado|ocio)\\b[^.!?;\\n]{0,50}" + AMOUNT);
    private static final Pattern OWN_BALANCE = Pattern.compile(
            "\\b(?:tengo|dispongo de|me quedan?|me sobran?|me restan?)\\b[^.!?;\\n]{0,60}" + AMOUNT);
    private static final Pattern OWN_FINANCIAL_STATE = Pattern.compile(
            "\\b(?:tengo|dispongo de)\\b[^.!?;\\n]{0,60}\\b(?:ahorros?|deudas?|saldo|dinero)\\b");
    private static final Pattern LABELLED_AMOUNT = Pattern.compile(
            "(?:^|[:;.!?]\\s*)(?:sueldo|salario|nomina|deuda|alquiler|hipoteca|"
            + "gastos?(?: mensuales)?|ingresos?(?: mensuales?)?|saldo disponible)"
            + "(?:\\s+(?:netos?|mensual(?:es)?|anual(?:es)?|total|disponible))*\\s*[:=]\\s*"
            + "(?:[A-Z]{3}\\s*)?" + AMOUNT, Pattern.CASE_INSENSITIVE);
    private static final Pattern SPENDING_QUESTION = Pattern.compile(
            "\\b(?:cuanto (?:puedo|podria|debo) (?:gastar|ahorrar|apartar|destinar)|"
            + "cuanto (?:dinero )?me (?:queda|sobra)|"
            + "(?:puedo|podria) gastar|que gastos tengo|cuanto gasto|"
            + "distribuye mi dinero|reparte mi dinero)\\b");
    private static final Pattern SAVING_TARGET = Pattern.compile(
            "\\b(?:quiero|deseo|voy a|me gustaria) ahorrar\\b"
            + "|\\b(?:objetivo|meta|porcentaje) de ahorro\\b[^.!?;\\n]{0,60}" + AMOUNT);
    private static final Pattern DELETE_BUDGET = Pattern.compile(
            "\\b(?:olvida|borra|elimina|quita|borrar|eliminar)\\b[^.!?;\\n]{0,40}"
            + "\\b(?:presupuesto|sueldo|salario|nomina|ingresos?|gastos?|ahorros?|finanzas|"
            + "datos financieros|datos economicos)\\b");
    private static final Pattern BUDGET_CONTROL = Pattern.compile(
            "\\b(?:esos|estos|eso) (?:son |es )?(?:todos? )?(?:(?:mis|los) )?gastos\\b"
            + "|\\b(?:gastos completos|presupuesto (?:personal|mensual)|resumen de presupuesto)\\b"
            + "|\\b(?:cambia|establece|usa) (?:mi |la )?moneda\\b");
    private static final Pattern FINANCE_TOPIC = Pattern.compile(
            "\\b(?:finanzas?|financiera?s?|financiero?s?|administracion|empresa|empresarial|"
            + "negocio|presupuestos?|ingresos?|gastos?|sueldo|salario|nomina|ahorro|ahorrar|"
            + "inversion(?:es)?|invertir|deuda|deudas|credito|intereses|impuestos?|tributari[oa]|"
            + "fiscal|iva|costes?|costos?|margen|margenes|rentabilidad|contabilidad|facturacion|"
            + "tesoreria|liquidez|flujo de caja|punto de equilibrio|capital de trabajo|"
            + "capital circulante|50\\s*[/\\-]\\s*30\\s*[/\\-]\\s*20)\\b");

    private static final String CONTEXT = "\nÁMBITO FINANCIERO Y ADMINISTRACIÓN: "
            + "Separa el presupuesto personal de las finanzas de la empresa. Ayuda a entender "
            + "ingresos netos, gastos, deuda, ahorro, flujo de caja, costes, márgenes, punto de equilibrio "
            + "y capital de trabajo según la pregunta; beneficio y efectivo disponible no son lo mismo. "
            + "Trabaja con los datos aportados y declara período, moneda, supuestos y datos que faltan. "
            + "No inventes movimientos, saldos ni datos guardados; no afirmes que has consultado o guardado "
            + "un presupuesto sin que la herramienta lo confirme. Usa resultados comprobados de las "
            + "herramientas disponibles y explica las operaciones útiles. El 50/30/20 es una referencia "
            + "adaptable sobre ingresos netos, no una obligación ni una garantía; prioriza necesidades "
            + "reales y comprueba si el margen permite ahorrar. Para impuestos o normas pide país y "
            + "período fiscal cuando falten. Para tipos, precios, leyes o productos actuales cita una "
            + "fuente oficial con fecha realmente consultada; si no puedes verificarla, dilo. "
            + "Distingue ejemplos de recomendaciones y no prometas rendimientos ni conocimiento universal.\n";

    private FinanceConversationPolicy() { }

    /** Includes malformed/unsupported personal declarations to prevent accidental remote fallback. */
    public static boolean isPrivateBudgetInput(String input) {
        String text = normalize(input);
        if (text.isEmpty()) return false;
        boolean personalFlow = PERSONAL_FLOW.matcher(text).find()
                && (!NON_MONEY_FLOW.matcher(text).find() || MONEY_CUE.matcher(text).find());
        return PERSONAL.matcher(text).find() || personalFlow
                || LABELLED_AMOUNT.matcher(text).find() || NAMED_EXPENSE.matcher(text).find()
                || (OWN_BALANCE.matcher(text).find() && MONEY_CUE.matcher(text).find())
                || OWN_FINANCIAL_STATE.matcher(text).find()
                || SPENDING_QUESTION.matcher(text).find() || SAVING_TARGET.matcher(text).find()
                || DELETE_BUDGET.matcher(text).find() || BUDGET_CONTROL.matcher(text).find();
    }

    /** Context for actual financial questions, without inserting any saved personal amounts. */
    public static String contextFor(String input) {
        String text = normalize(input);
        return FINANCE_TOPIC.matcher(text).find() || isPrivateBudgetInput(text) ? CONTEXT : "";
    }

    public static String normalize(String input) {
        if (input == null) return "";
        return Normalizer.normalize(input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("(?<=\\p{L})[/\\\\]+(?=\\p{L})", "")
                .replaceAll("\\s+", " ").trim();
    }
}
