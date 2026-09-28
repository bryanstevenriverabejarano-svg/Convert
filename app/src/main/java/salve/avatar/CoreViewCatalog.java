package salve.avatar;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** Original supplied illustrations; views are discrete references, not a reconstructed 3D mesh. */
public final class CoreViewCatalog {
    public static final class Entry {
        public final String id, label, path, group;
        public final int width, height;
        private Entry(String id, String label, String group, int width, int height) {
            this.id=id; this.label=label; this.group=group; this.width=width; this.height=height;
            this.path="avatar/core/"+id+".png";
        }
    }
    public static final List<Entry> VIEWS=Collections.unmodifiableList(Arrays.asList(
        new Entry("front", "Frontal", "turnaround", 1024, 1536),
        new Entry("front_three_quarter", "Frontal tres cuartos", "turnaround", 1024, 1536),
        new Entry("right", "Lateral derecha", "turnaround", 1024, 1536),
        new Entry("rear_right", "Trasera tres cuartos derecha", "turnaround", 1024, 1536),
        new Entry("rear", "Trasera", "turnaround", 1024, 1536),
        new Entry("rear_three_quarter", "Trasera tres cuartos", "turnaround", 1024, 1536),
        new Entry("left", "Lateral izquierda", "turnaround", 1024, 1536),
        new Entry("front_opposite", "Frontal tres cuartos contrario", "turnaround", 1024, 1536),
        new Entry("front_variant", "Variante frontal derecha", "reference", 1024, 1536),
        new Entry("above", "Vista superior", "reference", 1024, 1536),
        new Entry("below", "Vista inferior", "reference", 1024, 1536),
        new Entry("a_front", "Pose A frontal", "bind", 1024, 1536),
        new Entry("a_open_hair", "Pose A con cabello abierto", "bind", 1024, 1536),
        new Entry("a_rear", "Pose A trasera", "bind", 1024, 1536),
        new Entry("seated", "Sentada", "pose", 1024, 1536),
        new Entry("crouched", "Agachada", "pose", 1024, 1536),
        new Entry("kneeling", "De rodillas", "pose", 1024, 1536),
        new Entry("kneeling_variant", "Rodillas variante derecha", "pose", 1374, 1145),
        new Entry("leaning", "Inclinada", "pose", 1024, 1536),
        new Entry("leaning_variant", "Inclinada variante", "pose", 1024, 1536)));
    private CoreViewCatalog() {}
    public static Entry find(String id) {
        for(Entry e:VIEWS)if(e.id.equals(id))return e;
        throw new IllegalArgumentException("Vista del núcleo desconocida.");
    }
}
