package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Independent, bounded verification of a tool's witness and objective.
 * No kernel is invoked here. Feedback deliberately contains no reference answer.
 */
public final class ToolVerifier {
    private static final int MAX_NODES = 14;
    private static final int MAX_VALUE = 1_000_000;
    private static final long INFINITY = Long.MAX_VALUE / 4;

    private ToolVerifier() { }

    public static final class Verification {
        public final boolean passed;
        public final String summary;

        private Verification(boolean passed, String summary) {
            this.passed = passed;
            this.summary = summary;
        }
    }

    public static Verification verify(String family, JsonObject input, JsonObject output,
                                      OperationBudget budget) {
        if (budget == null) throw new IllegalArgumentException("Falta el presupuesto de verificación");
        budget.tick();
        try {
            require(input != null && output != null);
            boolean passed;
            if ("route".equals(family)) passed = route(input, output, budget);
            else if ("knapsack".equals(family)) passed = knapsack(input, output, budget);
            else if ("schedule".equals(family)) passed = schedule(input, output, budget);
            else if ("dependencies".equals(family)) passed = dependencies(input, output, budget);
            else throw new Invalid();
            return new Verification(passed, passed
                    ? "Testigo y objetivo comprobados por un verificador independiente."
                    : "El testigo o su objetivo no supera la verificación independiente.");
        } catch (Invalid invalid) {
            return new Verification(false, "Entrada o salida fuera del contrato verificable.");
        }
    }

    private static boolean route(JsonObject input, JsonObject output, OperationBudget budget) {
        keys(input, budget, "nodes", "edges", "source", "target");
        int count = integer(input.get("nodes"), 1, MAX_NODES, budget);
        int source = integer(input.get("source"), 0, count - 1, budget);
        int target = integer(input.get("target"), 0, count - 1, budget);
        JsonArray edges = array(input.get("edges"), 0, MAX_NODES * MAX_NODES, budget);
        long[][] direct = new long[count][count];
        long[][] distance = new long[count][count];
        for (int i = 0; i < count; i++) {
            budget.tick();
            Arrays.fill(direct[i], INFINITY);
            Arrays.fill(distance[i], INFINITY);
            distance[i][i] = 0;
        }
        for (JsonElement element : edges) {
            JsonArray edge = array(element, 3, 3, budget);
            int from = integer(edge.get(0), 0, count - 1, budget);
            int to = integer(edge.get(1), 0, count - 1, budget);
            int weight = integer(edge.get(2), -MAX_VALUE, MAX_VALUE, budget);
            direct[from][to] = Math.min(direct[from][to], weight);
            distance[from][to] = Math.min(distance[from][to], weight);
        }
        // Floyd-Warshall differs from the route kernels and handles parallel edges.
        for (int via = 0; via < count; via++) {
            for (int from = 0; from < count; from++) {
                for (int to = 0; to < count; to++) {
                    budget.tick();
                    if (distance[from][via] != INFINITY && distance[via][to] != INFINITY) {
                        distance[from][to] = Math.min(distance[from][to],
                                distance[from][via] + distance[via][to]);
                    }
                }
            }
        }
        for (int node = 0; node < count; node++) {
            budget.tick();
            require(distance[node][node] >= 0); // Any negative cycle is outside this contract.
        }
        String status = text(output.get("status"), budget);
        if ("unreachable".equals(status)) {
            keys(output, budget, "status", "path");
            array(output.get("path"), 0, 0, budget);
            return distance[source][target] == INFINITY;
        }
        require("ok".equals(status));
        keys(output, budget, "status", "cost", "path");
        long claimedCost = integer(output.get("cost"), -MAX_NODES * MAX_VALUE,
                MAX_NODES * MAX_VALUE, budget);
        JsonArray path = array(output.get("path"), 1, count, budget);
        boolean[] seen = new boolean[count];
        int previous = -1;
        long actualCost = 0;
        for (int i = 0; i < path.size(); i++) {
            int node = integer(path.get(i), 0, count - 1, budget);
            if (seen[node] || (i == 0 && node != source)) return false;
            seen[node] = true;
            if (previous >= 0) {
                if (direct[previous][node] == INFINITY) return false;
                actualCost += direct[previous][node];
            }
            previous = node;
        }
        return previous == target && actualCost == claimedCost
                && distance[source][target] != INFINITY
                && claimedCost == distance[source][target];
    }

    private static boolean knapsack(JsonObject input, JsonObject output, OperationBudget budget) {
        keys(input, budget, "capacity", "items");
        int capacity = integer(input.get("capacity"), 0, 100, budget);
        JsonArray items = array(input.get("items"), 0, MAX_NODES, budget);
        int[] weights = new int[items.size()];
        int[] values = new int[items.size()];
        for (int i = 0; i < items.size(); i++) {
            JsonObject item = object(items.get(i), budget);
            keys(item, budget, "weight", "value");
            weights[i] = integer(item.get("weight"), 1, MAX_VALUE, budget);
            values[i] = integer(item.get("value"), 0, MAX_VALUE, budget);
        }
        keys(output, budget, "status", "value", "weight", "selected");
        require("ok".equals(text(output.get("status"), budget)));
        int selected = selectedMask(output.get("selected"), items.size(), budget);
        int claimedWeight = integer(output.get("weight"), 0, MAX_NODES * MAX_VALUE, budget);
        int claimedValue = integer(output.get("value"), 0, MAX_NODES * MAX_VALUE, budget);
        // Exhaustive subset enumeration: independent of both greedy and capacity DP kernels.
        int[] subsetWeight = new int[1 << items.size()];
        int[] subsetValue = new int[subsetWeight.length];
        int optimum = 0;
        for (int mask = 1; mask < subsetWeight.length; mask++) {
            budget.tick();
            int item = Integer.numberOfTrailingZeros(mask);
            int previous = mask & (mask - 1);
            subsetWeight[mask] = subsetWeight[previous] + weights[item];
            subsetValue[mask] = subsetValue[previous] + values[item];
            if (subsetWeight[mask] <= capacity) optimum = Math.max(optimum, subsetValue[mask]);
        }
        return subsetWeight[selected] <= capacity && subsetWeight[selected] == claimedWeight
                && subsetValue[selected] == claimedValue && claimedValue == optimum;
    }

    private static boolean schedule(JsonObject input, JsonObject output, OperationBudget budget) {
        keys(input, budget, "jobs");
        JsonArray jobs = array(input.get("jobs"), 0, MAX_NODES, budget);
        int[] starts = new int[jobs.size()];
        int[] ends = new int[jobs.size()];
        int[] values = new int[jobs.size()];
        for (int i = 0; i < jobs.size(); i++) {
            JsonObject job = object(jobs.get(i), budget);
            keys(job, budget, "start", "end", "value");
            starts[i] = integer(job.get("start"), Integer.MIN_VALUE, Integer.MAX_VALUE, budget);
            ends[i] = integer(job.get("end"), Integer.MIN_VALUE, Integer.MAX_VALUE, budget);
            values[i] = integer(job.get("value"), 0, MAX_VALUE, budget);
            require(starts[i] < ends[i]);
        }
        keys(output, budget, "status", "value", "selected");
        require("ok".equals(text(output.get("status"), budget)));
        int selected = selectedMask(output.get("selected"), jobs.size(), budget);
        int claimedValue = integer(output.get("value"), 0, MAX_NODES * MAX_VALUE, budget);
        int[] conflicts = new int[jobs.size()];
        for (int i = 0; i < jobs.size(); i++) {
            for (int j = 0; j < jobs.size(); j++) {
                budget.tick();
                if (i != j && starts[i] < ends[j] && starts[j] < ends[i]) conflicts[i] |= 1 << j;
            }
        }
        // Enumerate every subset, not a second weighted-interval scheduling implementation.
        boolean[] feasible = new boolean[1 << jobs.size()];
        int[] subsetValue = new int[feasible.length];
        feasible[0] = true;
        int optimum = 0;
        for (int mask = 1; mask < feasible.length; mask++) {
            budget.tick();
            int job = Integer.numberOfTrailingZeros(mask);
            int previous = mask & (mask - 1);
            feasible[mask] = feasible[previous] && (previous & conflicts[job]) == 0;
            subsetValue[mask] = subsetValue[previous] + values[job];
            if (feasible[mask]) optimum = Math.max(optimum, subsetValue[mask]);
        }
        return feasible[selected] && subsetValue[selected] == claimedValue && claimedValue == optimum;
    }

    private static boolean dependencies(JsonObject input, JsonObject output, OperationBudget budget) {
        keys(input, budget, "nodes", "edges");
        int count = integer(input.get("nodes"), 1, MAX_NODES, budget);
        JsonArray edges = array(input.get("edges"), 0, MAX_NODES * MAX_NODES, budget);
        boolean[][] direct = new boolean[count][count];
        boolean[][] reachable = new boolean[count][count];
        for (JsonElement element : edges) {
            JsonArray edge = array(element, 2, 2, budget);
            int from = integer(edge.get(0), 0, count - 1, budget);
            int to = integer(edge.get(1), 0, count - 1, budget);
            direct[from][to] = true;
            reachable[from][to] = true;
        }
        for (int via = 0; via < count; via++) {
            for (int from = 0; from < count; from++) {
                for (int to = 0; to < count; to++) {
                    budget.tick();
                    reachable[from][to] |= reachable[from][via] && reachable[via][to];
                }
            }
        }
        boolean hasCycle = false;
        for (int node = 0; node < count; node++) {
            budget.tick();
            hasCycle |= reachable[node][node];
        }
        String status = text(output.get("status"), budget);
        if ("cycle".equals(status)) {
            keys(output, budget, "status", "cycle");
            JsonArray cycle = array(output.get("cycle"), 2, count + 1, budget);
            int first = integer(cycle.get(0), 0, count - 1, budget);
            int previous = first;
            boolean[] seen = new boolean[count];
            seen[first] = true;
            for (int i = 1; i < cycle.size(); i++) {
                int node = integer(cycle.get(i), 0, count - 1, budget);
                if (!direct[previous][node]) return false;
                if (i == cycle.size() - 1) {
                    if (node != first) return false;
                } else {
                    if (seen[node]) return false;
                    seen[node] = true;
                }
                previous = node;
            }
            return hasCycle;
        }
        require("ok".equals(status));
        keys(output, budget, "status", "order", "layers");
        if (hasCycle) return false;
        JsonArray order = array(output.get("order"), count, count, budget);
        int[] position = new int[count];
        int[] layerIndex = new int[count];
        Arrays.fill(position, -1);
        Arrays.fill(layerIndex, -1);
        for (int i = 0; i < count; i++) {
            int node = integer(order.get(i), 0, count - 1, budget);
            if (position[node] != -1) return false;
            position[node] = i;
        }
        JsonArray layers = array(output.get("layers"), 1, count, budget);
        for (int i = 0; i < layers.size(); i++) {
            JsonArray layer = array(layers.get(i), 1, count, budget);
            for (JsonElement element : layer) {
                int node = integer(element, 0, count - 1, budget);
                if (layerIndex[node] != -1) return false;
                layerIndex[node] = i;
            }
        }
        for (int from = 0; from < count; from++) {
            if (layerIndex[from] == -1) return false;
            for (int to = 0; to < count; to++) {
                budget.tick();
                if (direct[from][to]
                        && (position[from] >= position[to] || layerIndex[from] >= layerIndex[to])) return false;
            }
        }
        return true;
    }

    private static int selectedMask(JsonElement element, int count, OperationBudget budget) {
        JsonArray selected = array(element, 0, count, budget);
        int mask = 0;
        for (JsonElement entry : selected) {
            int index = integer(entry, 0, count - 1, budget);
            require((mask & (1 << index)) == 0);
            mask |= 1 << index;
        }
        return mask;
    }

    private static JsonObject object(JsonElement element, OperationBudget budget) {
        budget.tick();
        require(element != null && element.isJsonObject());
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonElement element, int minimum, int maximum, OperationBudget budget) {
        budget.tick();
        require(element != null && element.isJsonArray());
        JsonArray array = element.getAsJsonArray();
        require(array.size() >= minimum && array.size() <= maximum);
        return array;
    }

    private static int integer(JsonElement element, int minimum, int maximum, OperationBudget budget) {
        budget.tick();
        require(element != null && element.isJsonPrimitive());
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        require(primitive.isNumber());
        String raw = primitive.getAsString();
        require(raw.length() <= 11 && raw.matches("-?(0|[1-9][0-9]*)"));
        try {
            long number = Long.parseLong(raw);
            require(number >= minimum && number <= maximum);
            return (int) number;
        } catch (NumberFormatException invalid) {
            throw new Invalid();
        }
    }

    private static String text(JsonElement element, OperationBudget budget) {
        budget.tick();
        require(element != null && element.isJsonPrimitive() && element.getAsJsonPrimitive().isString());
        String text = element.getAsString();
        require(text.length() <= 32);
        return text;
    }

    private static void keys(JsonObject object, OperationBudget budget, String... names) {
        budget.tick();
        require(object.size() == names.length);
        Set<String> allowed = new HashSet<>(Arrays.asList(names));
        for (String name : object.keySet()) {
            budget.tick();
            require(allowed.contains(name));
        }
    }

    private static void require(boolean valid) {
        if (!valid) throw new Invalid();
    }

    private static final class Invalid extends RuntimeException { }
}
