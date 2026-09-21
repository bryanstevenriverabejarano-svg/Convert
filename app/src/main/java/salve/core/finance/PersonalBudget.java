package salve.core.finance;

import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** An explicitly recorded monthly projection, never a bank balance or a transaction ledger. */
public final class PersonalBudget {
    private static final BigDecimal MAX_AMOUNT = new BigDecimal("1000000000000");
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final int MAX_EXPENSES = 200;
    private static final int MAX_JSON_LENGTH = 65536;

    public enum Category { NEED, WANT }

    public static final class Expense {
        public final String name;
        public final BigDecimal amount;
        public final Category category;
        private Expense(String name, BigDecimal amount, Category category) {
            this.name = cleanName(name);
            this.amount = amount;
            if (category == null) throw new IllegalArgumentException("Clasifica el gasto como necesidad o deseo.");
            this.category = category;
        }
    }

    /** Null until an amount with an explicit ISO currency has been recorded. */
    public final String currency;
    /** Net recurring monthly income; null is unknown and zero is an explicitly recorded zero. */
    public final BigDecimal monthlyIncome;
    public final BigDecimal savingsTargetPercent;
    /** Only true after the user explicitly declares the list complete. */
    public final boolean expensesComplete;
    public final List<Expense> expenses;

    public PersonalBudget() {
        this(null, null, new BigDecimal("20"), false, Collections.emptyList());
    }

    private PersonalBudget(String currency, BigDecimal income, BigDecimal savings, boolean complete,
            List<Expense> expenses) {
        if (currency == null && (income != null || !expenses.isEmpty()))
            throw new IllegalArgumentException("Indica la moneda de los importes.");
        this.currency = currency == null ? null : checkedCurrency(currency);
        this.monthlyIncome = income == null ? null : money(income, this.currency);
        this.savingsTargetPercent = checkedPercent(savings);
        if (expenses.size() > MAX_EXPENSES) throw new IllegalArgumentException("Máximo 200 gastos mensuales.");
        List<Expense> copy = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (Expense expense : expenses) {
            Expense checked = new Expense(expense.name, money(expense.amount, this.currency), expense.category);
            if (!names.add(key(checked.name))) throw new IllegalArgumentException("Gasto duplicado.");
            copy.add(checked);
        }
        this.expenses = Collections.unmodifiableList(copy);
        this.expensesComplete = complete;
    }

    public PersonalBudget withMonthlyIncome(BigDecimal amount, String isoCurrency) {
        String selected = selectCurrency(isoCurrency);
        return new PersonalBudget(selected, money(amount, selected), savingsTargetPercent, expensesComplete, expenses);
    }

    public PersonalBudget withoutIncome() {
        return new PersonalBudget(currency, null, savingsTargetPercent, expensesComplete, expenses);
    }

    /** Replaces the same named recurring expense; recording it again never adds a second copy. */
    public PersonalBudget withExpense(String name, BigDecimal amount, Category category, String isoCurrency) {
        String selected = selectCurrency(isoCurrency);
        Expense replacement = new Expense(name, money(amount, selected), category);
        List<Expense> updated = new ArrayList<>(expenses);
        boolean found = false;
        for (int i = 0; i < updated.size(); i++) {
            if (key(updated.get(i).name).equals(key(replacement.name))) {
                updated.set(i, replacement);
                found = true;
                break;
            }
        }
        if (!found) updated.add(replacement);
        return new PersonalBudget(selected, monthlyIncome, savingsTargetPercent, expensesComplete && found, updated);
    }

    public PersonalBudget withoutExpense(String name) {
        String selected = key(cleanName(name));
        List<Expense> updated = new ArrayList<>();
        for (Expense expense : expenses) if (!key(expense.name).equals(selected)) updated.add(expense);
        if (updated.size() == expenses.size()) return this;
        return new PersonalBudget(currency, monthlyIncome, savingsTargetPercent, false, updated);
    }

    public PersonalBudget withoutAllExpenses() {
        return new PersonalBudget(currency, monthlyIncome, savingsTargetPercent, false, Collections.emptyList());
    }

    public PersonalBudget withSavingsTargetPercent(BigDecimal percent) {
        return new PersonalBudget(currency, monthlyIncome, percent, expensesComplete, expenses);
    }

    public PersonalBudget withExpensesComplete(boolean complete) {
        return new PersonalBudget(currency, monthlyIncome, savingsTargetPercent, complete, expenses);
    }

    public Summary summary() { return new Summary(this); }

    public static final class Summary {
        public final BigDecimal needs, wants, totalExpenses;
        /** All following amounts are null until the monthly income is known. */
        public final BigDecimal remainder, deficit, targetSavings, feasibleSavings, remainingAfterSavings;
        /** 50/30/20 is a comparison only; it does not change recorded expenses or the savings target. */
        public final BigDecimal referenceNeeds50, referenceWants30, referenceSavings20;
        /** Even a complete monthly projection does not imply funds available in a bank account. */
        public final boolean provisional;
        private Summary(PersonalBudget budget) {
            BigDecimal necessary = BigDecimal.ZERO, optional = BigDecimal.ZERO;
            for (Expense expense : budget.expenses) {
                if (expense.category == Category.NEED) necessary = necessary.add(expense.amount);
                else optional = optional.add(expense.amount);
            }
            needs = necessary;
            wants = optional;
            totalExpenses = needs.add(wants);
            provisional = !budget.expensesComplete;
            if (budget.monthlyIncome == null) {
                remainder = deficit = targetSavings = feasibleSavings = remainingAfterSavings = null;
                referenceNeeds50 = referenceWants30 = referenceSavings20 = null;
                return;
            }
            remainder = budget.monthlyIncome.subtract(totalExpenses);
            deficit = remainder.negate().max(BigDecimal.ZERO);
            targetSavings = budget.percentOfIncome(budget.savingsTargetPercent);
            feasibleSavings = targetSavings.min(remainder.max(BigDecimal.ZERO));
            remainingAfterSavings = remainder.subtract(feasibleSavings);
            referenceNeeds50 = budget.percentOfIncome(new BigDecimal("50"));
            referenceWants30 = budget.percentOfIncome(new BigDecimal("30"));
            referenceSavings20 = budget.percentOfIncome(new BigDecimal("20"));
        }
    }

    public String toJson() {
        StringWriter output = new StringWriter();
        try (JsonWriter writer = new JsonWriter(output)) {
            writer.beginObject().name("version").value(1).name("currency").value(currency)
                    .name("monthlyIncome").value(monthlyIncome)
                    .name("savingsTargetPercent").value(savingsTargetPercent)
                    .name("expensesComplete").value(expensesComplete).name("expenses").beginArray();
            for (Expense expense : expenses) {
                writer.beginObject().name("name").value(expense.name).name("amount").value(expense.amount)
                        .name("category").value(expense.category.name()).endObject();
            }
            writer.endArray().endObject();
        } catch (IOException impossible) {
            throw new IllegalStateException("No se pudo serializar el presupuesto.", impossible);
        }
        return output.toString();
    }

    /** Bounded versioned schema; rejects missing, duplicated, unknown, malformed and trailing fields. */
    public static PersonalBudget fromJson(String json) {
        if (json == null || json.length() > MAX_JSON_LENGTH) throw new IllegalArgumentException("Presupuesto inválido.");
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            reader.beginObject();
            Set<String> fields = new HashSet<>();
            String currency = null;
            BigDecimal income = null, savings = null;
            boolean complete = false;
            List<Expense> expenses = new ArrayList<>();
            while (reader.hasNext()) {
                String field = reader.nextName();
                if (!fields.add(field)) throw new IllegalArgumentException("Campo duplicado.");
                switch (field) {
                    case "version":
                        require(reader, JsonToken.NUMBER);
                        if (!"1".equals(reader.nextString())) throw new IllegalArgumentException("Versión no compatible.");
                        break;
                    case "currency":
                        if (reader.peek() == JsonToken.NULL) reader.nextNull();
                        else { require(reader, JsonToken.STRING); currency = reader.nextString(); }
                        break;
                    case "monthlyIncome":
                        if (reader.peek() == JsonToken.NULL) reader.nextNull();
                        else income = readDecimal(reader);
                        break;
                    case "savingsTargetPercent": savings = readDecimal(reader); break;
                    case "expensesComplete": require(reader, JsonToken.BOOLEAN); complete = reader.nextBoolean(); break;
                    case "expenses":
                        reader.beginArray();
                        while (reader.hasNext()) {
                            if (expenses.size() == MAX_EXPENSES) throw new IllegalArgumentException("Demasiados gastos.");
                            expenses.add(readExpense(reader));
                        }
                        reader.endArray();
                        break;
                    default: throw new IllegalArgumentException("Campo desconocido.");
                }
            }
            reader.endObject();
            if (fields.size() != 6 || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IllegalArgumentException("Faltan campos o sobran datos.");
            return new PersonalBudget(currency, income, savings, complete, expenses);
        } catch (IOException | IllegalStateException error) {
            throw new IllegalArgumentException("Presupuesto inválido.", error);
        }
    }

    private static Expense readExpense(JsonReader reader) throws IOException {
        reader.beginObject();
        Set<String> fields = new HashSet<>();
        String name = null;
        BigDecimal amount = null;
        Category category = null;
        while (reader.hasNext()) {
            String field = reader.nextName();
            if (!fields.add(field)) throw new IllegalArgumentException("Campo de gasto duplicado.");
            switch (field) {
                case "name": require(reader, JsonToken.STRING); name = reader.nextString(); break;
                case "amount": amount = readDecimal(reader); break;
                case "category": require(reader, JsonToken.STRING); category = Category.valueOf(reader.nextString()); break;
                default: throw new IllegalArgumentException("Campo de gasto desconocido.");
            }
        }
        reader.endObject();
        if (fields.size() != 3) throw new IllegalArgumentException("Gasto incompleto.");
        return new Expense(name, amount, category);
    }

    private BigDecimal percentOfIncome(BigDecimal percent) {
        return monthlyIncome.multiply(percent).divide(HUNDRED, fractionDigits(currency), RoundingMode.DOWN);
    }

    private String selectCurrency(String requested) {
        String selected = checkedCurrency(requested);
        if (currency != null && !currency.equals(selected))
            throw new IllegalArgumentException("El presupuesto usa " + currency + "; no se mezclan monedas ni se convierten importes automáticamente.");
        return selected;
    }

    private static String checkedCurrency(String raw) {
        if (raw == null) throw new IllegalArgumentException("Indica la moneda ISO, por ejemplo EUR o USD.");
        String code = raw.trim().toUpperCase(Locale.ROOT);
        if (!code.matches("[A-Z]{3}")) throw new IllegalArgumentException("Moneda ISO inválida.");
        int digits = Currency.getInstance(code).getDefaultFractionDigits();
        if (digits < 0 || digits > 4) throw new IllegalArgumentException("Moneda no compatible.");
        return code;
    }

    private static int fractionDigits(String currency) { return Currency.getInstance(currency).getDefaultFractionDigits(); }

    private static BigDecimal money(BigDecimal value, String currency) {
        if (value == null || value.signum() < 0 || value.compareTo(MAX_AMOUNT) > 0
                || value.scale() > 12 || value.scale() < -12
                || value.stripTrailingZeros().scale() > fractionDigits(currency))
            throw new IllegalArgumentException("Importe fuera de límites o con demasiados decimales para su moneda.");
        return value.setScale(fractionDigits(currency), RoundingMode.UNNECESSARY);
    }

    private static BigDecimal checkedPercent(BigDecimal value) {
        if (value == null || value.signum() < 0 || value.compareTo(HUNDRED) > 0
                || value.scale() > 12 || value.scale() < -12 || value.stripTrailingZeros().scale() > 2)
            throw new IllegalArgumentException("El ahorro debe estar entre 0 y 100 %, con hasta dos decimales.");
        return value.setScale(2, RoundingMode.UNNECESSARY);
    }

    private static String cleanName(String raw) {
        if (raw == null || raw.length() > 160) throw new IllegalArgumentException("Indica un nombre de gasto de hasta 80 caracteres.");
        for (int i = 0; i < raw.length(); i++) if (Character.isISOControl(raw.charAt(i)))
            throw new IllegalArgumentException("Nombre de gasto inválido.");
        String name = Normalizer.normalize(raw, Normalizer.Form.NFKC).replaceAll("\\s+", " ").trim();
        if (name.isEmpty() || name.length() > 80) throw new IllegalArgumentException("Indica un nombre de gasto de hasta 80 caracteres.");
        return name;
    }

    private static String key(String name) { return name.toLowerCase(Locale.ROOT); }

    private static BigDecimal readDecimal(JsonReader reader) throws IOException {
        require(reader, JsonToken.NUMBER);
        String raw = reader.nextString();
        if (raw.length() > 32 || !raw.matches("[0-9]+(?:\\.[0-9]+)?"))
            throw new IllegalArgumentException("Importe decimal inválido.");
        return new BigDecimal(raw);
    }

    private static void require(JsonReader reader, JsonToken token) throws IOException {
        if (reader.peek() != token) throw new IllegalArgumentException("Tipo de campo inválido.");
    }
}
