package salve.sketch;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class IconLibraryTest {
    static final class SketchTestAssetsHasBrand {
        static boolean any(IconLibrary icons) {
            for (String w : new String[]{"brand", "apple", "google", "facebook"}) {
                IconLibrary.Icon i = icons.find("brand " + w);
                if (i != null && i.name.startsWith("brand-")) return true;
            }
            return false;
        }
    }
    static IconLibrary icons;

    @BeforeClass public static void load() throws Exception { icons = IconLibrary.parse(SketchTestAssets.read("sketch/icons.json")); }

    static String found(String english) {
        IconLibrary.Icon i = icons.find(english);
        return i == null ? null : i.name;
    }

    @Test public void hasManyDrawableIconsAndNoBrands() {
        assertTrue(icons.size() > 1000);
        assertEquals("«apple» es la manzana, no la marca", "apple", found("apple"));
        assertFalse("sin logotipos de marcas", SketchTestAssetsHasBrand.any(icons));
    }

    @Test public void findsWhatHerConceptNames() {
        assertEquals("building-church", found("church"));
        assertEquals("building-church", found("Churches"));
        assertEquals("cat", found("cat"));
        assertEquals("dog", found("dog"));
        assertEquals("car", found("car"));
        assertEquals("door", found("door"));
        assertEquals("bell", found("bell"));
    }

    @Test public void doesNotInventAMatch() {
        assertNull("no hay iguana: no vale un icono cualquiera", found("iguana"));
        assertNull(found(""));
        assertNull(found("the"));
    }

    @Test public void iconStrokesAreReadyToDraw() {
        IconLibrary.Icon church = icons.find("church");
        assertFalse(church.strokes().isEmpty());
        float[] b = PartPlan.bounds(church.strokes());
        assertTrue(b[0] >= -1 && b[1] >= -1 && b[2] <= 1001 && b[3] <= 1001);
        church.strokes().get(0)[0] = -999;
        assertNotEquals("devuelve copias", -999f, church.strokes().get(0)[0], 0f);
    }

    @Test public void partsHaveAnEnglishNameForTheirIcon() {
        assertEquals("door", PartPlan.english("la puerta"));
        assertEquals("bell", PartPlan.english("campanario"));
        assertEquals("", PartPlan.english("dragón"));
    }
}
