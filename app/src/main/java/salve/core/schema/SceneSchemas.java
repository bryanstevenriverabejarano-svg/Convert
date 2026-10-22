package salve.core.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import salve.core.visual.SceneGraph;

/**
 * Los esquemas que se ven en una foto. Cada relación medida en una escena (SceneGraph) es la forma
 * física de un esquema: «encima de» es SOPORTE, «dentro de» es CONTENEDOR, «delante de» es BLOQUEO
 * (tapa la vista), «al lado» y «cerca» son CERCANÍA. Con eso deduce cosas que no se ven pero se
 * siguen de la geometría: qué caería si quitas algo, qué saldría si abres algo.
 */
public final class SceneSchemas {
    private SceneSchemas() { }

    /** Un esquema con sus papeles rellenos («la mesa» sostiene «la taza»). */
    public static final class Instance {
        public final ImageSchemas.Schema schema;
        public final String a, b;
        public Instance(ImageSchemas.Schema schema, String a, String b) { this.schema = schema; this.a = a; this.b = b; }
        @Override public String toString() { return schema.id() + "(" + schema.roleA + "=" + a + ", " + schema.roleB + "=" + b + ")"; }
    }

    public static List<Instance> instances(SceneGraph scene) {
        List<Instance> out = new ArrayList<>();
        if (scene == null) return out;
        for (SceneGraph.Relation r : scene.relations) {
            switch (r.kind) {
                case "encima de": out.add(new Instance(ImageSchemas.Schema.SOPORTE, r.a.name, r.b.name)); break;
                case "dentro de": out.add(new Instance(ImageSchemas.Schema.CONTENEDOR, r.a.name, r.b.name)); break;
                case "delante de": out.add(new Instance(ImageSchemas.Schema.BLOQUEO, r.a.name, r.b.name)); break;
                default: out.add(new Instance(ImageSchemas.Schema.CERCANIA, r.a.name, r.b.name));
            }
        }
        return out;
    }

    /**
     * Lo que se deduce siguiendo las cadenas: si A está encima de B y B encima de C, quitar C hace
     * caer a B y a A. Igual con lo que está dentro de algo. Frases cortas, como mucho 4.
     */
    public static List<String> deductions(SceneGraph scene) {
        List<String> out = new ArrayList<>();
        if (scene == null) return out;
        Map<String, Set<String>> supports = new LinkedHashMap<>(), contains = new LinkedHashMap<>();
        for (Instance i : instances(scene)) {
            if (i.schema == ImageSchemas.Schema.SOPORTE) supports.computeIfAbsent(i.b, k -> new LinkedHashSet<>()).add(i.a);
            if (i.schema == ImageSchemas.Schema.CONTENEDOR) contains.computeIfAbsent(i.b, k -> new LinkedHashSet<>()).add(i.a);
        }
        for (String base : supports.keySet()) {
            Set<String> fall = closure(supports, base);
            if (!fall.isEmpty() && out.size() < 4) out.add("si quitas " + base + ", caería" + (fall.size() > 1 ? "n " : " ") + join(fall));
        }
        for (String box : contains.keySet()) {
            Set<String> inside = closure(contains, box);
            if (!inside.isEmpty() && out.size() < 4) out.add("si abres " + box + ", podría" + (inside.size() > 1 ? "n" : "") + " salir " + join(inside));
        }
        return out;
    }

    /** «Esquemas: soporte (mesa sostiene taza); … Deduzco: si quitas la mesa, caería la taza.» Vacío si no hay. */
    public static String describe(SceneGraph scene) {
        List<Instance> list = instances(scene);
        StringBuilder b = new StringBuilder();
        int shown = 0;
        for (Instance i : list) {
            if (i.schema == ImageSchemas.Schema.CERCANIA || shown >= 6) continue;
            b.append(shown++ == 0 ? "Esquemas: " : "; ").append(i.schema.label).append(" (").append(phrase(i)).append(')');
        }
        if (shown > 0) b.append('.');
        List<String> d = deductions(scene);
        if (!d.isEmpty()) b.append(b.length() == 0 ? "" : " ").append("Deduzco: ").append(String.join("; ", d)).append('.');
        return b.toString();
    }

    static String phrase(Instance i) {
        switch (i.schema) {
            case SOPORTE: return i.b + " sostiene a " + i.a;
            case CONTENEDOR: return i.b + " contiene a " + i.a;
            case BLOQUEO: return i.a + " tapa a " + i.b;
            default: return i.a + " y " + i.b;
        }
    }

    private static Set<String> closure(Map<String, Set<String>> edges, String from) {
        Set<String> seen = new LinkedHashSet<>();
        List<String> todo = new ArrayList<>(edges.getOrDefault(from, new LinkedHashSet<>()));
        while (!todo.isEmpty()) {
            String n = todo.remove(0);
            if (n.equals(from) || !seen.add(n)) continue;
            todo.addAll(edges.getOrDefault(n, new LinkedHashSet<>()));
        }
        return seen;
    }

    private static String join(Set<String> items) {
        List<String> l = new ArrayList<>(items);
        if (l.size() == 1) return l.get(0);
        return String.join(", ", l.subList(0, l.size() - 1)) + " y " + l.get(l.size() - 1);
    }
}
