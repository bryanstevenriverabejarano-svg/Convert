package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Inventory of executable local capabilities. An absent tool is never advertised as available. */
public final class ToolRegistry {
    private static final List<String> FAMILIES = Collections.unmodifiableList(
            Arrays.asList("route", "knapsack", "schedule", "dependencies"));

    private ToolRegistry() { }

    public static List<String> families() { return FAMILIES; }

    /** A fresh metadata object: a caller or a model cannot mutate the executable registry. */
    public static JsonObject describe(String family) {
        if (!FAMILIES.contains(family)) throw new IllegalArgumentException("Capacidad no registrada");
        JsonObject value = new JsonObject();
        value.addProperty("name", family);
        value.addProperty("invocation", "SOLVE_CHALLENGE");
        value.addProperty("available", true);
        value.addProperty("description", description(family));
        value.addProperty("inputTemplate", inputTemplate(family));
        value.addProperty("outputContract", outputContract(family));
        JsonArray strategies = new JsonArray();
        for (String strategy : ToolKernels.strategies(family)) strategies.add(strategy);
        value.add("strategies", strategies);
        value.add("externalPermissions", new JsonArray());
        value.addProperty("sideEffects", "Sólo registro local versionado después de validar; sin red ni otras apps");
        value.addProperty("limits", "Hasta 14 elementos, 196 aristas, capacidad 100; enteros acotados; 4 intentos de 5 millones de operaciones");
        value.addProperty("estimatedProviderCost", 0);
        value.add("estimatedLatencyMs", JsonNull.INSTANCE);
        value.addProperty("latencyStatus", "Medir en el dispositivo; el límite de operaciones no garantiza milisegundos");
        value.addProperty("risks", "Interpretación errónea de datos, entrada incompatible, agotamiento de presupuesto o fallo de persistencia");
        value.addProperty("knownFailureModes", "Precondiciones del algoritmo, resultado subóptimo del candidato, regresiones incompatibles");
        value.addProperty("appropriateContext", "Problema discreto de esta familia con todos sus datos explícitos; no sustituye una decisión del usuario");
        value.addProperty("verification", "Resultado actual y hasta tres entradas conservadas; no certifica todos los casos futuros");
        return value;
    }

    public static String inputTemplate(String family) {
        switch (family) {
            case "route": return "{\"nodes\":N,\"edges\":[[origen,destino,peso]],\"source\":S,\"target\":T}";
            case "knapsack": return "{\"capacity\":C,\"items\":[{\"weight\":P,\"value\":V}]}";
            case "schedule": return "{\"jobs\":[{\"start\":I,\"end\":F,\"value\":V}]}";
            case "dependencies": return "{\"nodes\":N,\"edges\":[[origen,destino]]}";
            default: throw new IllegalArgumentException("Capacidad no registrada");
        }
    }

    public static String summary() {
        StringBuilder text = new StringBuilder("Herramientas locales registradas:\n");
        for (String family : FAMILIES) text.append(family).append(": ").append(description(family)).append('\n');
        return text.append("Todas comprueban cada resultado. No requieren red. Su latencia depende del reto y del dispositivo.").toString();
    }

    private static String description(String family) {
        switch (family) {
            case "route": return "Encontrar el camino de menor coste en un grafo dirigido";
            case "knapsack": return "Elegir elementos para maximizar valor respetando capacidad";
            case "schedule": return "Elegir intervalos compatibles de máximo valor";
            case "dependencies": return "Ordenar dependencias en capas o identificar un ciclo";
            default: throw new IllegalArgumentException("Capacidad no registrada");
        }
    }

    private static String outputContract(String family) {
        switch (family) {
            case "route": return "status, path y cost si existe ruta; unreachable si no existe";
            case "knapsack": return "status, selected, weight y value";
            case "schedule": return "status, selected y value";
            case "dependencies": return "status; order/layers si acíclico o cycle si cíclico";
            default: throw new IllegalArgumentException("Capacidad no registrada");
        }
    }
}
