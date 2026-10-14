package salve.core.memory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fusión por rango recíproco (RRF) de varias listas ordenadas de identificadores.
 *
 * <p>Combina la búsqueda por palabras (exacta, buena con nombres y cifras) y la búsqueda por
 * significado (paráfrasis, sinónimos) sin tener que calibrar sus puntuaciones entre sí.</p>
 */
public final class HybridFusion {
    public static final int K = 60;
    private HybridFusion() {}

    @SafeVarargs
    public static List<Integer> rrf(int limit, List<Integer>... rankings) {
        Map<Integer, Double> scores = new LinkedHashMap<>();
        for (List<Integer> ranking : rankings) {
            if (ranking == null) continue;
            for (int rank = 0; rank < ranking.size(); rank++) {
                Integer id = ranking.get(rank);
                if (id == null) continue;
                scores.merge(id, 1.0 / (K + rank + 1), Double::sum);
            }
        }
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(scores.entrySet());
        // Estable: ante empate conserva el orden de aparición (primero la lista léxica).
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        List<Integer> out = new ArrayList<>();
        for (Map.Entry<Integer, Double> e : sorted) {
            if (out.size() >= limit) break;
            out.add(e.getKey());
        }
        return out;
    }
}
