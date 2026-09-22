package salve.core.autonomy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Bounded, deterministic algorithms used by generated declarative tools.
 * These kernels do not invoke a language model, load code, or access device capabilities.
 * Heuristics deliberately promise feasibility only; a separate verifier checks optimality.
 */
public final class ToolKernels {
    private static final int MAX_NODES = 14;
    private static final int MAX_AMOUNT = 1_000_000;
    private static final long INF = Long.MAX_VALUE / 4;

    private ToolKernels() { }

    public static List<String> strategies(String family) {
        if (family == null) throw invalid("Missing family");
        switch (family) {
            case "route": return Collections.unmodifiableList(Arrays.asList(
                    "FEWEST_EDGES", "DIJKSTRA", "BELLMAN_FORD", "DAG_SHORTEST"));
            case "knapsack": return Collections.unmodifiableList(Arrays.asList(
                    "RATIO_GREEDY", "VALUE_GREEDY", "DYNAMIC_PROGRAMMING"));
            case "schedule": return Collections.unmodifiableList(Arrays.asList(
                    "EARLIEST_FINISH", "GREATEST_VALUE", "WEIGHTED_DP"));
            case "dependencies": return Collections.unmodifiableList(Arrays.asList(
                    "INPUT_ORDER", "KAHN_LAYERS"));
            default: throw invalid("Unsupported family");
        }
    }

    public static JsonObject execute(String family, String strategy, JsonObject input,
                                     OperationBudget budget) {
        if (budget == null || input == null || !strategies(family).contains(strategy))
            throw invalid("Missing input, budget, or unsupported strategy");
        budget.tick();
        switch (family) {
            case "route": return route(strategy, input, budget);
            case "knapsack": return knapsack(strategy, input, budget);
            case "schedule": return schedule(strategy, input, budget);
            case "dependencies": return dependencies(strategy, input, budget);
            default: throw invalid("Unsupported family");
        }
    }

    private static JsonObject route(String strategy, JsonObject input, OperationBudget budget) {
        fields(input, "nodes", "edges", "source", "target");
        int n = integer(input.get("nodes"), 1, MAX_NODES);
        int source = integer(input.get("source"), 0, n - 1);
        int target = integer(input.get("target"), 0, n - 1);
        JsonArray edges = array(input.get("edges"), MAX_NODES * MAX_NODES);
        long[][] weights = new long[n][n];
        for (long[] row : weights) { budget.tick(); Arrays.fill(row, INF); }
        for (JsonElement element : edges) {
            budget.tick();
            JsonArray edge = array(element, 3);
            if (edge.size() != 3) throw invalid("Route edges require three integers");
            int from = integer(edge.get(0), 0, n - 1);
            int to = integer(edge.get(1), 0, n - 1);
            int weight = integer(edge.get(2), -MAX_AMOUNT, MAX_AMOUNT);
            weights[from][to] = Math.min(weights[from][to], weight);
        }
        rejectNegativeCycle(weights, budget);
        long[] distance = new long[n];
        int[] previous = new int[n];
        Arrays.fill(distance, INF);
        Arrays.fill(previous, -1);
        distance[source] = 0;
        switch (strategy) {
            case "FEWEST_EDGES": {
                int[] queue = new int[n];
                boolean[] seen = new boolean[n];
                int head = 0, tail = 0;
                queue[tail++] = source;
                seen[source] = true;
                while (head < tail) {
                    budget.tick();
                    int from = queue[head++];
                    for (int to = 0; to < n; to++) {
                        budget.tick();
                        if (weights[from][to] == INF || seen[to]) continue;
                        seen[to] = true;
                        previous[to] = from;
                        distance[to] = distance[from] + weights[from][to];
                        queue[tail++] = to;
                    }
                }
                break;
            }
            case "DIJKSTRA": {
                for (long[] row : weights) for (long value : row) {
                    budget.tick();
                    if (value < 0) throw invalid("Dijkstra requires nonnegative weights");
                }
                boolean[] settled = new boolean[n];
                for (int step = 0; step < n; step++) {
                    budget.tick();
                    int from = -1;
                    for (int i = 0; i < n; i++) {
                        budget.tick();
                        if (!settled[i] && distance[i] != INF
                                && (from < 0 || distance[i] < distance[from])) from = i;
                    }
                    if (from < 0) break;
                    settled[from] = true;
                    for (int to = 0; to < n; to++) {
                        budget.tick();
                        if (!settled[to]) relax(weights, distance, previous, from, to);
                    }
                }
                break;
            }
            case "BELLMAN_FORD": {
                for (int step = 1; step < n; step++) {
                    budget.tick();
                    boolean changed = false;
                    for (int from = 0; from < n; from++) for (int to = 0; to < n; to++) {
                        budget.tick();
                        changed |= relax(weights, distance, previous, from, to);
                    }
                    if (!changed) break;
                }
                break;
            }
            case "DAG_SHORTEST": {
                boolean[][] graph = new boolean[n][n];
                for (int from = 0; from < n; from++) for (int to = 0; to < n; to++) {
                    budget.tick();
                    graph[from][to] = weights[from][to] != INF;
                }
                List<List<Integer>> layers = topologicalLayers(graph, budget);
                if (layers == null) throw invalid("DAG shortest path requires an acyclic graph");
                for (List<Integer> layer : layers) for (int from : layer) {
                    budget.tick();
                    for (int to = 0; to < n; to++) {
                        budget.tick();
                        relax(weights, distance, previous, from, to);
                    }
                }
                break;
            }
            default: throw invalid("Unsupported route strategy");
        }
        JsonObject result = status(distance[target] == INF ? "unreachable" : "ok");
        JsonArray path = new JsonArray();
        if (distance[target] != INF) {
            List<Integer> reverse = new ArrayList<>();
            int current = target;
            while (current != source) {
                budget.tick();
                if (current < 0 || reverse.size() >= n) throw invalid("Invalid predecessor chain");
                reverse.add(current);
                current = previous[current];
            }
            reverse.add(source);
            Collections.reverse(reverse);
            for (int vertex : reverse) { budget.tick(); path.add(vertex); }
            result.addProperty("cost", Math.toIntExact(distance[target]));
        }
        result.add("path", path);
        return result;
    }

    private static boolean relax(long[][] weights, long[] distance, int[] previous, int from, int to) {
        if (distance[from] == INF || weights[from][to] == INF) return false;
        long candidate = distance[from] + weights[from][to];
        if (candidate >= distance[to]) return false;
        distance[to] = candidate;
        previous[to] = from;
        return true;
    }

    /** A virtual zero-cost source reaches every vertex, including disconnected cycles. */
    private static void rejectNegativeCycle(long[][] weights, OperationBudget budget) {
        int n = weights.length;
        long[] distance = new long[n];
        for (int step = 0; step < n; step++) {
            budget.tick();
            boolean changed = false;
            for (int from = 0; from < n; from++) for (int to = 0; to < n; to++) {
                budget.tick();
                if (weights[from][to] != INF && distance[from] + weights[from][to] < distance[to]) {
                    if (step == n - 1) throw invalid("Negative cycles are unsupported");
                    distance[to] = distance[from] + weights[from][to];
                    changed = true;
                }
            }
            if (!changed) return;
        }
    }

    private static JsonObject knapsack(String strategy, JsonObject input, OperationBudget budget) {
        fields(input, "capacity", "items");
        int capacity = integer(input.get("capacity"), 0, 100);
        JsonArray items = array(input.get("items"), MAX_NODES);
        int n = items.size();
        int[] weights = new int[n], values = new int[n];
        for (int i = 0; i < n; i++) {
            budget.tick();
            JsonObject item = object(items.get(i));
            fields(item, "weight", "value");
            weights[i] = integer(item.get("weight"), 1, MAX_AMOUNT);
            values[i] = integer(item.get("value"), 0, MAX_AMOUNT);
        }
        int mask = 0;
        if (strategy.equals("DYNAMIC_PROGRAMMING")) {
            int[] best = new int[capacity + 1], masks = new int[capacity + 1];
            Arrays.fill(best, -1);
            best[0] = 0;
            for (int i = 0; i < n; i++) {
                budget.tick();
                for (int weight = capacity; weight >= weights[i]; weight--) {
                    budget.tick();
                    if (best[weight - weights[i]] < 0) continue;
                    int value = best[weight - weights[i]] + values[i];
                    int candidateMask = masks[weight - weights[i]] | (1 << i);
                    if (value > best[weight]
                            || (value == best[weight] && candidateMask < masks[weight])) {
                        best[weight] = value;
                        masks[weight] = candidateMask;
                    }
                }
            }
            int bestValue = 0;
            for (int weight = 0; weight <= capacity; weight++) {
                budget.tick();
                if (best[weight] > bestValue) { bestValue = best[weight]; mask = masks[weight]; }
            }
        } else {
            List<Integer> ordered = indexes(n, budget);
            ordered.sort((a, b) -> {
                budget.tick();
                int comparison = strategy.equals("RATIO_GREEDY")
                        ? Long.compare((long) values[b] * weights[a], (long) values[a] * weights[b])
                        : Integer.compare(values[b], values[a]);
                return comparison == 0 ? Integer.compare(a, b) : comparison;
            });
            int weight = 0;
            for (int index : ordered) {
                budget.tick();
                if (values[index] > 0 && weights[index] <= capacity - weight) {
                    weight += weights[index];
                    mask |= 1 << index;
                }
            }
        }
        int value = 0, weight = 0;
        JsonArray selected = new JsonArray();
        for (int i = 0; i < n; i++) {
            budget.tick();
            if ((mask & (1 << i)) != 0) {
                selected.add(i);
                value += values[i];
                weight += weights[i];
            }
        }
        JsonObject result = status("ok");
        result.addProperty("value", value);
        result.addProperty("weight", weight);
        result.add("selected", selected);
        return result;
    }

    private static JsonObject schedule(String strategy, JsonObject input, OperationBudget budget) {
        fields(input, "jobs");
        JsonArray jobs = array(input.get("jobs"), MAX_NODES);
        int n = jobs.size();
        int[] start = new int[n], end = new int[n], value = new int[n];
        for (int i = 0; i < n; i++) {
            budget.tick();
            JsonObject job = object(jobs.get(i));
            fields(job, "start", "end", "value");
            start[i] = integer(job.get("start"), Integer.MIN_VALUE, Integer.MAX_VALUE);
            end[i] = integer(job.get("end"), Integer.MIN_VALUE, Integer.MAX_VALUE);
            value[i] = integer(job.get("value"), 0, MAX_AMOUNT);
            if (start[i] >= end[i]) throw invalid("Job start must precede end");
        }
        Comparator<Integer> chronological = (a, b) -> {
            budget.tick();
            int comparison = Integer.compare(end[a], end[b]);
            if (comparison == 0) comparison = Integer.compare(start[a], start[b]);
            return comparison == 0 ? Integer.compare(a, b) : comparison;
        };
        List<Integer> ordered = indexes(n, budget);
        ordered.sort(chronological);
        int mask = 0;
        switch (strategy) {
            case "WEIGHTED_DP": {
                int[] best = new int[n + 1], masks = new int[n + 1];
                for (int i = 1; i <= n; i++) {
                    budget.tick();
                    int job = ordered.get(i - 1), predecessor = 0;
                    for (int j = 0; j < i - 1; j++) {
                        budget.tick();
                        if (end[ordered.get(j)] <= start[job]) predecessor = j + 1;
                    }
                    int include = value[job] + best[predecessor];
                    int includeMask = masks[predecessor] | (1 << job);
                    if (include > best[i - 1]
                            || (include == best[i - 1] && includeMask < masks[i - 1])) {
                        best[i] = include;
                        masks[i] = includeMask;
                    } else {
                        best[i] = best[i - 1];
                        masks[i] = masks[i - 1];
                    }
                }
                mask = masks[n];
                break;
            }
            case "EARLIEST_FINISH": {
                long previousEnd = Long.MIN_VALUE;
                for (int job : ordered) {
                    budget.tick();
                    if (value[job] > 0 && start[job] >= previousEnd) {
                        mask |= 1 << job;
                        previousEnd = end[job];
                    }
                }
                break;
            }
            case "GREATEST_VALUE": {
                List<Integer> byValue = new ArrayList<>(ordered);
                byValue.sort((a, b) -> {
                    budget.tick();
                    int comparison = Integer.compare(value[b], value[a]);
                    return comparison == 0 ? chronological.compare(a, b) : comparison;
                });
                for (int job : byValue) {
                    budget.tick();
                    boolean compatible = value[job] > 0;
                    for (int other = 0; other < n; other++) {
                        budget.tick();
                        if ((mask & (1 << other)) != 0 && start[job] < end[other]
                                && start[other] < end[job]) compatible = false;
                    }
                    if (compatible) mask |= 1 << job;
                }
                break;
            }
            default: throw invalid("Unsupported scheduling strategy");
        }
        int total = 0;
        JsonArray selected = new JsonArray();
        for (int job : ordered) {
            budget.tick();
            if ((mask & (1 << job)) != 0) { selected.add(job); total += value[job]; }
        }
        JsonObject result = status("ok");
        result.addProperty("value", total);
        result.add("selected", selected);
        return result;
    }

    private static JsonObject dependencies(String strategy, JsonObject input, OperationBudget budget) {
        fields(input, "nodes", "edges");
        int n = integer(input.get("nodes"), 1, MAX_NODES);
        JsonArray edges = array(input.get("edges"), MAX_NODES * MAX_NODES);
        boolean[][] graph = new boolean[n][n];
        for (JsonElement element : edges) {
            budget.tick();
            JsonArray edge = array(element, 2);
            if (edge.size() != 2) throw invalid("Dependency edges require two integers");
            graph[integer(edge.get(0), 0, n - 1)][integer(edge.get(1), 0, n - 1)] = true;
        }
        List<List<Integer>> layers = topologicalLayers(graph, budget);
        if (layers == null) {
            JsonObject result = status("cycle");
            result.add("cycle", findCycle(graph, budget));
            return result;
        }
        if (strategy.equals("INPUT_ORDER")) {
            // A deliberately naive candidate: the verifier must reject invalid dependency order.
            layers = new ArrayList<>();
            for (int i = 0; i < n; i++) { budget.tick(); layers.add(Collections.singletonList(i)); }
        }
        JsonArray order = new JsonArray(), jsonLayers = new JsonArray();
        for (List<Integer> layer : layers) {
            budget.tick();
            JsonArray jsonLayer = new JsonArray();
            for (int vertex : layer) { budget.tick(); order.add(vertex); jsonLayer.add(vertex); }
            jsonLayers.add(jsonLayer);
        }
        JsonObject result = status("ok");
        result.add("order", order);
        result.add("layers", jsonLayers);
        return result;
    }

    /** Null means a directed cycle; every returned layer is ready before that layer executes. */
    private static List<List<Integer>> topologicalLayers(boolean[][] graph, OperationBudget budget) {
        int n = graph.length;
        int[] indegree = new int[n];
        boolean[] removed = new boolean[n];
        for (int from = 0; from < n; from++) for (int to = 0; to < n; to++) {
            budget.tick();
            if (graph[from][to]) indegree[to]++;
        }
        List<List<Integer>> layers = new ArrayList<>();
        int count = 0;
        while (count < n) {
            budget.tick();
            List<Integer> layer = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                budget.tick();
                if (!removed[i] && indegree[i] == 0) layer.add(i);
            }
            if (layer.isEmpty()) return null;
            for (int from : layer) {
                budget.tick();
                removed[from] = true;
                count++;
                for (int to = 0; to < n; to++) {
                    budget.tick();
                    if (graph[from][to]) indegree[to]--;
                }
            }
            layers.add(layer);
        }
        return layers;
    }

    private static JsonArray findCycle(boolean[][] graph, OperationBudget budget) {
        int[] color = new int[graph.length];
        List<Integer> stack = new ArrayList<>();
        for (int vertex = 0; vertex < graph.length; vertex++) {
            budget.tick();
            if (color[vertex] == 0) {
                JsonArray cycle = visitCycle(vertex, graph, color, stack, budget);
                if (cycle != null) return cycle;
            }
        }
        throw invalid("Cycle witness not found");
    }

    private static JsonArray visitCycle(int vertex, boolean[][] graph, int[] color,
                                        List<Integer> stack, OperationBudget budget) {
        budget.tick();
        color[vertex] = 1;
        stack.add(vertex);
        for (int to = 0; to < graph.length; to++) {
            budget.tick();
            if (!graph[vertex][to]) continue;
            if (color[to] == 1) {
                JsonArray cycle = new JsonArray();
                int beginning = stack.indexOf(to);
                for (int i = beginning; i < stack.size(); i++) { budget.tick(); cycle.add(stack.get(i)); }
                cycle.add(to);
                return cycle;
            }
            if (color[to] == 0) {
                JsonArray cycle = visitCycle(to, graph, color, stack, budget);
                if (cycle != null) return cycle;
            }
        }
        stack.remove(stack.size() - 1);
        color[vertex] = 2;
        return null;
    }

    private static List<Integer> indexes(int n, OperationBudget budget) {
        List<Integer> result = new ArrayList<>();
        for (int i = 0; i < n; i++) { budget.tick(); result.add(i); }
        return result;
    }

    private static JsonObject status(String value) {
        JsonObject result = new JsonObject();
        result.addProperty("status", value);
        return result;
    }

    private static JsonObject object(JsonElement element) {
        if (element == null || !element.isJsonObject()) throw invalid("Expected an object");
        return element.getAsJsonObject();
    }

    private static JsonArray array(JsonElement element, int maximum) {
        if (element == null || !element.isJsonArray()) throw invalid("Expected an array");
        JsonArray result = element.getAsJsonArray();
        if (result.size() > maximum) throw invalid("Array exceeds domain limit");
        return result;
    }

    private static int integer(JsonElement element, int minimum, int maximum) {
        if (element == null || !element.isJsonPrimitive()) throw invalid("Expected an integer");
        JsonPrimitive value = element.getAsJsonPrimitive();
        String raw = value.getAsString();
        if (!value.isNumber() || raw.length() > 11 || !raw.matches("-?(0|[1-9][0-9]*)"))
            throw invalid("Expected an integer JSON number");
        try {
            int parsed = Integer.parseInt(raw);
            if (parsed < minimum || parsed > maximum) throw invalid("Integer outside domain");
            return parsed;
        } catch (NumberFormatException error) {
            throw invalid("Integer outside domain");
        }
    }

    private static void fields(JsonObject input, String... names) {
        if (input.size() != names.length || !input.keySet().containsAll(Arrays.asList(names)))
            throw invalid("Missing or unknown input fields");
    }

    private static IllegalArgumentException invalid(String reason) { return new IllegalArgumentException(reason); }
}
