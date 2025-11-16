package salve.core;

/** Representa un problema detectado en un método. */
public class MethodIssue {
    private final String methodName;
    private final int parameterCount;
    private final IssueLevel level;
    private final String suggestion;

    public MethodIssue(String methodName, int parameterCount, IssueLevel level, String suggestion) {
        this.methodName = methodName;
        this.parameterCount = parameterCount;
        this.level = level;
        this.suggestion = suggestion;
    }

    /**
     * Devuelve el nombre del método donde se detectó el problema.
     * @return nombre del método
     */
    public String getMethodName() {
        return methodName;
    }

    /**
     * Devuelve el número de parámetros que tiene el método.
     * @return cantidad de parámetros
     */
    public int getParameterCount() {
        return parameterCount;
    }

    /**
     * Devuelve el nivel de la issue (INFO, WARNING, etc.).
     * @return nivel del issue
     */
    public IssueLevel getLevel() {
        return level;
    }

    /**
     * Devuelve la sugerencia asociada a este issue.
     * @return sugerencia para solucionar el problema
     */
    public String getSuggestion() {
        return suggestion;
    }

    /**
     * Descripción detallada y segura (sin nulls) del issue.
     * Útil como reemplazo directo de getDescription() en el código que lo consuma.
     */
    public String getDescription() {
        StringBuilder sb = new StringBuilder();

        // Sugerencia (si hay)
        if (suggestion != null && !suggestion.trim().isEmpty()) {
            sb.append(suggestion.trim());
        } else {
            sb.append("Sin sugerencia específica");
        }

        // Datos de contexto
        sb.append(" | Método: ")
                .append(methodName != null && !methodName.isEmpty() ? methodName : "?");
        sb.append(" | Parámetros: ").append(parameterCount);
        sb.append(" | Nivel: ").append(level != null ? level : "DESCONOCIDO");

        return sb.toString();
    }

    @Override
    public String toString() {
        return methodName + " (params=" + parameterCount + ") [" + level + "] " + suggestion;
    }
}
