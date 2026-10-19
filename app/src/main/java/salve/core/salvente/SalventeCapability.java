package salve.core.salvente;

import java.text.Normalizer;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Capacidades que puede tener un Salvente. Es una lista cerrada a propósito: un Salvente no
 * controla el teléfono (llamadas, Maps, música), no ve la identidad ni la memoria de Salve y no
 * maneja credenciales de nodos. Cada capacidad exige un mínimo de ciertos rasgos para ser
 * coherente con la personalidad (un apoyo emocional sin empatía sería una contradicción).
 */
public enum SalventeCapability {
    CONVERSACION("Conversación", "habla con su persona", Collections.emptyMap()),
    MEMORIA_PROPIA("Memoria propia", "recuerda sus conversaciones en su propio espacio cifrado", Collections.emptyMap()),
    APOYO_ESTUDIO("Apoyo al estudio", "explica temas y repasa", needs(SalventeTrait.PACIENCIA, 0.45, SalventeTrait.RIGOR, 0.40)),
    CREATIVIDAD("Creatividad", "inventa ideas, juegos y proyectos", needs(SalventeTrait.CREATIVIDAD, 0.50)),
    CUENTOS("Cuentos", "cuenta e inventa historias", needs(SalventeTrait.CREATIVIDAD, 0.45, SalventeTrait.CALIDEZ, 0.40)),
    BIENESTAR("Bienestar", "escucha y acompaña (no sustituye a un profesional)", needs(SalventeTrait.EMPATIA, 0.55, SalventeTrait.PRUDENCIA, 0.40)),
    ORGANIZACION("Organización", "ayuda a planificar tareas y rutinas", needs(SalventeTrait.RIGOR, 0.45)),
    IDIOMAS("Idiomas", "practica y corrige otros idiomas", needs(SalventeTrait.PACIENCIA, 0.40));

    /** Siempre presentes: sin ellas no es un Salvente con continuidad propia. */
    public static final SalventeCapability[] REQUIRED = {CONVERSACION, MEMORIA_PROPIA};
    public static final int MAX = 6;

    public final String label, description;
    public final Map<SalventeTrait, Double> minimums;

    SalventeCapability(String label, String description, Map<SalventeTrait, Double> minimums) {
        this.label = label; this.description = description; this.minimums = minimums;
    }

    public static SalventeCapability fromKey(String raw) {
        if (raw == null) return null;
        String key = Normalizer.normalize(raw.trim().toUpperCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^A-Z]+", "_").replaceAll("^_|_$", "");
        for (SalventeCapability capability : values()) if (capability.name().equals(key)) return capability;
        return null;
    }

    public boolean isRequired() { return this == CONVERSACION || this == MEMORIA_PROPIA; }

    private static Map<SalventeTrait, Double> needs(Object... pairs) {
        Map<SalventeTrait, Double> map = new EnumMap<>(SalventeTrait.class);
        for (int i = 0; i < pairs.length; i += 2) map.put((SalventeTrait) pairs[i], (Double) pairs[i + 1]);
        return Collections.unmodifiableMap(map);
    }
}
