package salve.core.finance;

import java.math.BigDecimal;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Small, deterministic Spanish budget dialogue. It never calls a model or persists a value.
 * Only explicit monthly declarations change a budget; callers must persist before replying.
 * Pending details are transient and never appear in conversation history or long-term memory.
 */
public final class PersonalBudgetConversation {
    private static final String AMOUNT = "([0-9]{1,13}(?:[.,][0-9]{1,2})?)(?![0-9.,])";
    private static final Pattern INCOME = Pattern.compile("^(?:mi (?:sueldo|salario|ingreso)(?: mensual)?(?: neto)? (?:es|son)|(?:ahora )?(?:cobro|gano|recibo))\\s+" + AMOUNT + "\\s*(.*)$");
    private static final Pattern EXPENSE = Pattern.compile("^(?:pago|gasto)\\s+" + AMOUNT + "\\s*(.*)$");
    private static final Pattern NAMED_EXPENSE = Pattern.compile("^mi (alquiler|hipoteca|seguro|internet|transporte) (?:es|son|cuesta)\\s+" + AMOUNT + "\\s*(.*)$");
    private static final Pattern TARGET = Pattern.compile("^(?:quiero|me gustaria|voy a) ahorrar (?:el )?" + AMOUNT + "\\s*%$");
    private static final Pattern CURRENCY = Pattern.compile("(?<![a-z])(?:pesos colombianos|dolares estadounidenses|euros?|eur|usd|cop|€)(?![a-z])");
    private static final Pattern MONTH = Pattern.compile("\\b(?:al mes|cada mes|por mes|mensual(?:es)?)\\b");
    private static final Pattern OTHER_PERIOD = Pattern.compile("\\b(?:semanal(?:es)?|quincenal(?:es)?|anual(?:es)?|diari[oa]s?|semanas?|quincenas?|anos?|dias?|ayer|hoy|manana|este mes|una vez)\\b");
    private static final Pattern UNSAFE_DECLARATION = Pattern.compile("\\b(?:no|nunca|tampoco|quizas|supongamos|imagina|imaginemos|ejemplo|hipotetic[oa]|si|fuera|seria|podria|empresa|negocio|cliente|emplead[oa]|brutos?|brutas?)\\b");
    private static final Pattern NEED = Pattern.compile("\\b(?:alquiler|hipoteca|comida|alimentacion|supermercado|electricidad|luz|agua|gas|internet|transporte|medicinas|medicamentos|seguro)\\b");
    private static final Pattern WANT = Pattern.compile("\\b(?:ocio|restaurantes?|cine|videojuegos?|streaming|netflix|spotify|vacaciones|caprichos?)\\b");
    private static final String HELP = "Puedo llevar tu presupuesto personal mensual aquí. Por ejemplo: «mi sueldo es 1800 EUR netos al mes» y «pago 650 EUR de alquiler al mes». Usa importes sin separadores de miles. También puedes preguntarme cómo va tu presupuesto, corregir un gasto o borrarlo. Los datos de tu empresa se tratan por separado.";

    private String pending;

    public static final class Response {
        public final String reply;
        /** Immutable proposal; null when no replacement should be persisted. */
        public final PersonalBudget nextBudget;
        public final boolean clearAll;
        public final boolean changed;

        private Response(String reply, PersonalBudget nextBudget, boolean clearAll) {
            this.reply = reply;
            this.nextBudget = nextBudget;
            this.clearAll = clearAll;
            this.changed = nextBudget != null || clearAll;
        }
        public String reply() { return reply; }
        public PersonalBudget nextBudget() { return nextBudget; }
        public boolean clearAll() { return clearAll; }
        public boolean changed() { return changed; }
    }

    public synchronized boolean handles(String input) {
        boolean clarification = pending != null && isClarification(normalize(input));
        return FinanceConversationPolicy.isPrivateBudgetInput(input) || clarification;
    }
    public synchronized boolean hasPendingClarification() { return pending != null; }
    public synchronized void resetPending() { pending = null; }

    public synchronized Response handle(String input, PersonalBudget current) {
        if (current == null) throw new IllegalArgumentException("Falta el presupuesto actual.");
        if (input == null || input.length() > 1000) { pending = null; return unchanged(HELP); }
        String text = normalize(input);
        if (text.equals("cancela") || text.equals("cancelar")) {
            pending = null;
            return unchanged("Cancelado; no he guardado esa anotación.");
        }
        // A clarification can only complete the declaration, never execute another instruction.
        if (pending != null && isClarification(text)) {
            text = clarificationDetails(text);
            if (text.equals("necesidad") || text.equals("necesidades") || text.equals("ocio"))
                text = "categoria " + text;
            text = pending + " " + text;
        }
        pending = null;
        if (text.matches("[¿?]*(?:que gastos tengo|cuales son mis gastos|muestra mis gastos|mis gastos)[¿?]*"))
            return unchanged(listExpenses(current));
        if (isSummaryQuestion(text)) return unchanged(describe(current));
        if (input.matches("(?s).*[\\\"'«»“”‘’¿?].*") || UNSAFE_DECLARATION.matcher(text).find())
            return unchanged("No he cambiado tu presupuesto. Para guardar datos necesito una declaración personal explícita, con importe, divisa y frecuencia mensual; los ingresos deben ser netos.");
        text = text.replaceFirst("^(?:salve[, ]+)?(?:recuerda que |guarda que |anota que )?", "");
        if (text.matches("(?:borra|olvida|elimina) mi presupuesto"))
            return new Response("He borrado tu presupuesto personal guardado en este dispositivo.", null, true);
        if (text.matches("(?:borra|olvida|elimina) mi (?:sueldo|salario|ingreso(?: mensual)?)"))
            return update(current, current.withoutIncome(), "He eliminado el ingreso guardado; conservé tus gastos.");
        if (text.matches("(?:borra|olvida|elimina) (?:todos )?mis gastos"))
            return update(current, current.withoutAllExpenses(), "He eliminado los gastos guardados. El presupuesto vuelve a estar incompleto.");
        Matcher deletion = Pattern.compile("^(?:borra|olvida|elimina) el gasto (?:de )?([a-z0-9 ]{1,60})$").matcher(text);
        if (deletion.matches()) {
            PersonalBudget next = current.withoutExpense(deletion.group(1));
            return current.toJson().equals(next.toJson()) ? unchanged("No encontré ese gasto guardado.")
                    : update(current, next, "He eliminado ese gasto. Revisa si falta algún otro gasto mensual.");
        }
        if (text.matches("(?:esos|estos) son todos mis gastos(?: mensuales)?|ya (?:anote|registre) todos mis gastos(?: mensuales)?"))
            return update(current, current.withExpensesComplete(true), "He marcado los gastos como completos según tu confirmación. " + describe(current.withExpensesComplete(true)));
        Matcher target = TARGET.matcher(text);
        if (target.matches()) {
            try {
                PersonalBudget next = current.withSavingsTargetPercent(amount(target.group(1)));
                return update(current, next, "Objetivo de ahorro actualizado al " + number(next.savingsTargetPercent)
                        + "% del ingreso neto mensual. Es una meta editable; el ahorro posible depende de tus gastos.");
            } catch (IllegalArgumentException e) {
                return unchanged("El objetivo debe ser un porcentaje entre 0 y 100, con hasta dos decimales.");
            }
        }
        Matcher income = INCOME.matcher(text);
        if (income.matches()) return income(text, income.group(1), income.group(2), current);
        Matcher expense = EXPENSE.matcher(text);
        if (expense.matches()) return expense(text, expense.group(1), expense.group(2), null, current);
        Matcher named = NAMED_EXPENSE.matcher(text);
        if (named.matches()) return expense(text, named.group(2), named.group(3), named.group(1), current);
        return unchanged(HELP);
    }

    private Response income(String text, String rawAmount, String details, PersonalBudget current) {
        if (OTHER_PERIOD.matcher(details).find()) return unchanged("No he guardado el ingreso. Indica el importe neto mensual; no convierto automáticamente importes anuales, semanales o puntuales.");
        String currency = currencyOrExisting(details, current);
        boolean monthly = MONTH.matcher(text).find();
        boolean net = Pattern.compile("\\bnet[oa]s?\\b").matcher(text).find();
        String rest = strippedDetails(details).replaceAll("\\bnet[oa]s?\\b", "").trim();
        if (currency == null || !monthly || !net) {
            if (!rest.isEmpty() && !rest.matches("(?:pesos|dolares)?")) return unchanged(HELP);
            if (!rest.isEmpty()) return unchanged("No he guardado el ingreso. Especifica la divisa (por ejemplo EUR, USD o COP) y vuelve a indicar el ingreso neto mensual completo.");
            pending = text;
            return unchanged("Antes de guardarlo, confirma " + missing(currency == null, !monthly, !net) + ". Puedes responder, por ejemplo, «EUR netos al mes». Pesos o dólares necesitan el país o el código de divisa.");
        }
        if (!rest.isEmpty()) return unchanged(HELP);
        try {
            PersonalBudget next = current.withMonthlyIncome(amount(rawAmount), currency);
            return update(current, next, "Ingreso neto mensual guardado: " + money(next.monthlyIncome, next.currency)
                    + ". " + (next.expensesComplete ? "Puedes pedirme el resumen del presupuesto."
                    : "Dime tus gastos mensuales y avísame cuando estén todos; así podremos estimar cuánto reservar y repartir."));
        } catch (IllegalArgumentException e) { return invalidAmountOrCurrency(); }
    }

    private Response expense(String text, String rawAmount, String details, String fixedName, PersonalBudget current) {
        if (OTHER_PERIOD.matcher(details).find()) return unchanged("No he guardado ese gasto. Este presupuesto registra gastos mensuales recurrentes; indica su equivalente mensual y distingue los pagos puntuales.");
        String currency = currencyOrExisting(details, current);
        boolean monthly = MONTH.matcher(details).find();
        String rest = strippedDetails(details);
        PersonalBudget.Category category = null;
        Matcher explicitCategory = Pattern.compile("\\bcategoria (necesidad(?:es)?|ocio)\\b").matcher(rest);
        if (explicitCategory.find()) {
            category = explicitCategory.group(1).startsWith("necesidad") ? PersonalBudget.Category.NEED : PersonalBudget.Category.WANT;
            rest = explicitCategory.replaceAll("");
        }
        rest = rest.replaceFirst("^(?:de|en|por|para)\\s+", "").replaceFirst("^(?:el|la|los|las)\\s+", "").trim();
        String name = fixedName == null ? rest : fixedName;
        if (fixedName != null && !rest.isEmpty()) return unchanged(HELP);
        if (name.isEmpty() || !name.matches("[a-z][a-z0-9 ]{0,59}") || name.matches(".*\\b(?:y|pero|que|mi|su)\\b.*"))
            return unchanged("No he guardado el gasto. Indica uno por mensaje, por ejemplo: «pago 650 EUR de alquiler al mes».");
        if (category == null) {
            boolean need = NEED.matcher(name).find(), want = WANT.matcher(name).find();
            if (need != want) category = need ? PersonalBudget.Category.NEED : PersonalBudget.Category.WANT;
        }
        if (currency == null || !monthly || category == null) {
            // Ambiguous currency words must not accidentally become an expense's identity.
            if (name.matches(".*\\b(?:pesos|dolares)\\b.*")) return unchanged("No he guardado el gasto. Especifica la divisa: por ejemplo EUR, USD o COP; vuelve a indicar el gasto completo.");
            pending = text;
            String need = currency == null || !monthly ? missing(currency == null, !monthly, false) : "";
            if (category == null) need += (need.isEmpty() ? "" : " y ") + "si es una necesidad u ocio (responde «necesidad» u «ocio»)";
            return unchanged("Antes de guardarlo, confirma " + need + ".");
        }
        try {
            PersonalBudget next = current.withExpense(name, amount(rawAmount), category, currency);
            return update(current, next, "Gasto mensual guardado: " + name + ", " + money(amount(rawAmount), currency)
                    + " (" + (category == PersonalBudget.Category.NEED ? "necesidad" : "ocio") + "). "
                    + (next.expensesComplete ? "El presupuesto sigue marcado como completo."
                    : "Cuando estén todos, di «esos son todos mis gastos»."));
        } catch (IllegalArgumentException e) { return invalidAmountOrCurrency(); }
    }

    private static String describe(PersonalBudget budget) {
        PersonalBudget.Summary summary = budget.summary();
        String currency = budget.currency;
        if (budget.monthlyIncome == null)
            return "Gastos mensuales registrados: " + money(summary.totalExpenses, currency)
                    + ". Falta tu ingreso neto mensual y su divisa para calcular ahorro y distribución. "
                    + "El objetivo de ahorro del " + number(budget.savingsTargetPercent) + "% es editable, no una obligación.";
        String result = "Proyección mensual: ingreso neto " + money(budget.monthlyIncome, currency)
                + "; gastos registrados " + money(summary.totalExpenses, currency)
                + " (necesidades " + money(summary.needs, currency) + ", ocio " + money(summary.wants, currency) + "). ";
        if (summary.deficit.signum() > 0) result += "Los gastos superan el ingreso en " + money(summary.deficit, currency) + "; no hay margen estimado de ahorro. ";
        else result += "Diferencia tras esos gastos: " + money(summary.remainder, currency)
                + ". Objetivo editable de ahorro " + number(budget.savingsTargetPercent) + "%: " + money(summary.targetSavings, currency)
                + "; reserva posible con estos datos " + money(summary.feasibleSavings, currency)
                + "; después de esa reserva quedarían " + money(summary.remainingAfterSavings, currency) + ". ";
        result += summary.provisional
                ? "Faltan gastos por confirmar: las cifras son provisionales y no indican cuánto puedes gastar con seguridad. Cuando estén todos, di «esos son todos mis gastos». "
                : "Has confirmado que están todos los gastos. Sigue siendo una proyección mensual, no el saldo disponible de hoy. ";
        return result + "Referencia flexible 50/30/20: necesidades " + money(summary.referenceNeeds50, currency)
                + ", ocio " + money(summary.referenceWants30, currency) + " y ahorro " + money(summary.referenceSavings20, currency)
                + ". No sustituye tus gastos reales ni tu objetivo de ahorro elegido.";
    }

    private static String listExpenses(PersonalBudget budget) {
        if (budget.expenses.isEmpty()) return "No tienes gastos mensuales registrados en este presupuesto.";
        StringBuilder result = new StringBuilder("Gastos mensuales registrados: ");
        int count = Math.min(10, budget.expenses.size());
        for (int i = 0; i < count; i++) {
            PersonalBudget.Expense expense = budget.expenses.get(i);
            if (i > 0) result.append("; ");
            result.append(expense.name).append(": ").append(money(expense.amount, budget.currency));
        }
        result.append('.');
        if (budget.expenses.size() > count) result.append(" Hay ").append(budget.expenses.size() - count)
                .append(" gastos más; puedes corregir o borrar uno por su nombre.");
        result.append(" Total registrado: ").append(money(budget.summary().totalExpenses, budget.currency)).append('.');
        if (!budget.expensesComplete) result.append(" La lista aún no está confirmada como completa.");
        return result.toString();
    }

    private static Response update(PersonalBudget before, PersonalBudget after, String reply) {
        return new Response(reply, before.toJson().equals(after.toJson()) ? null : after, false);
    }
    private static Response unchanged(String text) { return new Response(text, null, false); }
    private static Response invalidAmountOrCurrency() {
        return unchanged("No he guardado cambios. Revisa el importe (sin separadores de miles, hasta dos decimales) y usa la misma divisa que el presupuesto. No convierto monedas automáticamente.");
    }
    private static BigDecimal amount(String raw) { return new BigDecimal(raw.replace(',', '.')); }
    private static String number(BigDecimal value) { return value.stripTrailingZeros().toPlainString().replace('.', ','); }
    private static String money(BigDecimal value, String currency) { return number(value) + (currency == null || currency.isEmpty() ? "" : " " + currency); }
    private static String missing(boolean currency, boolean monthly, boolean net) {
        String result = currency ? "la divisa" : "";
        if (monthly) result += (result.isEmpty() ? "" : ", ") + "que es un importe mensual";
        if (net) result += (result.isEmpty() ? "" : " y ") + "que el ingreso es neto";
        return result;
    }
    private static String currency(String details) {
        Matcher matcher = CURRENCY.matcher(details);
        String result = null;
        while (matcher.find()) {
            String token = matcher.group();
            String found = token.startsWith("eur") || token.equals("€") ? "EUR"
                    : token.equals("cop") || token.startsWith("pesos") ? "COP" : "USD";
            if (result != null && !result.equals(found)) return null;
            result = found;
        }
        return result;
    }
    private static String currencyOrExisting(String details, PersonalBudget current) {
        String explicit = currency(details);
        if (explicit != null) return explicit;
        // Only inherit an established currency when none was mentioned in the new statement.
        if (CURRENCY.matcher(details).find() || details.matches(".*\\b(?:pesos|dolares)\\b.*")) return null;
        return current.currency;
    }
    private static String strippedDetails(String details) {
        return MONTH.matcher(CURRENCY.matcher(details).replaceAll(" ")).replaceAll(" ").replaceAll("\\s+", " ").trim();
    }
    private static boolean isSummaryQuestion(String text) {
        return text.matches("[¿?]*(?:reparte mi dinero|distribuye mi dinero|como va mi presupuesto|cual es mi presupuesto|mi presupuesto|resume mi presupuesto|cuanto puedo (?:gastar|ahorrar)|cuanto me queda|como (?:reparto|distribuyo) mi (?:sueldo|salario))[¿?]*");
    }
    private static boolean isClarification(String text) {
        text = clarificationDetails(text);
        if (text.equals("cancela") || text.equals("cancelar") || text.matches("necesidad(?:es)?|ocio")) return true;
        String rest = strippedDetails(text).replaceAll("\\bnet[oa]s?\\b", "").trim();
        return !text.isEmpty() && rest.isEmpty();
    }
    private static String clarificationDetails(String text) {
        return text.replaceFirst("^(?:si|correcto|exacto)[, ]+", "");
    }
    private static String normalize(String raw) {
        if (raw == null) return "";
        return FinanceConversationPolicy.normalize(raw).replaceFirst("^salve[, ]+", "").replaceFirst("[.!]+$", "").trim();
    }
}
