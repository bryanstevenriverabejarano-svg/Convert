package salve.core;

import java.util.ArrayList;
import java.util.List;

/** Reporte de análisis para una clase core. */
public class AnalysisReport {
    private final String className;
    private final List<MethodIssue> issues = new ArrayList<>();

    public AnalysisReport(String className) {
        this.className = className;
    }

    public void addIssue(MethodIssue issue) {
        issues.add(issue);
    }

    /**
     * Devuelve el nombre de la clase asociada a este reporte.
     * @return nombre de la clase analizada
     */
    public String getClassName() {
        return className;
    }

    /**
     * Devuelve la lista de issues detectados en este reporte.
     * El contenido de la lista no debe ser modificado por el llamador.
     * @return lista de objetos MethodIssue
     */
    public List<MethodIssue> getIssues() {
        return issues;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("Reporte de " + className + ":\n");
        for (MethodIssue mi : issues) {
            sb.append("  - ").append(mi.toString()).append("\n");
        }
        return sb.toString();
    }
}