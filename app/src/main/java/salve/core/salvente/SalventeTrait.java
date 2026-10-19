package salve.core.salvente;

import java.text.Normalizer;
import java.util.Locale;

/**
 * Los 14 rasgos de personalidad de un Salvente, normalizados en [0.0, 1.0]. Cada rasgo sabe
 * describirse en sus dos extremos, de modo que el perfil numérico se traduce a instrucciones de
 * estilo concretas (no a adjetivos sueltos que el modelo ignore).
 */
public enum SalventeTrait {
    CURIOSIDAD("curiosidad", "pregunta poco y va al grano", "pregunta, explora y se interesa por los detalles"),
    EMPATIA("empatia", "es objetivo y poco emotivo", "nota cómo se siente la otra persona y lo tiene en cuenta"),
    CALIDEZ("calidez", "es sobrio y distante", "es cercano y afectuoso"),
    HUMOR("humor", "es serio", "usa humor suave con frecuencia"),
    CREATIVIDAD("creatividad", "prefiere lo práctico y conocido", "propone ideas originales e imaginativas"),
    RIGOR("rigor", "es flexible con los detalles", "es preciso, ordenado y comprueba lo que dice"),
    PRUDENCIA("prudencia", "se lanza con decisión", "es cauto y avisa de los riesgos"),
    AUTONOMIA("autonomia", "espera indicaciones antes de proponer", "toma la iniciativa y propone sin que se lo pidan"),
    PACIENCIA("paciencia", "es ágil y directo", "explica con calma y repite sin prisa"),
    FRANQUEZA("franqueza", "es diplomático y suaviza", "dice las cosas claras aunque no gusten"),
    ENERGIA("energia", "es tranquilo y pausado", "es enérgico y entusiasta"),
    FORMALIDAD("formalidad", "habla de forma coloquial", "habla con formalidad y cortesía"),
    PROTECCION("proteccion", "respeta la independencia sin vigilar", "cuida y protege a su persona"),
    OPTIMISMO("optimismo", "es realista y escéptico", "ve el lado bueno y anima");

    public final String key;
    public final String low, high;

    SalventeTrait(String key, String low, String high) { this.key = key; this.low = low; this.high = high; }

    /** Clave tolerante a mayúsculas y tildes («Empatía» → EMPATIA); null si no es un rasgo. */
    public static SalventeTrait fromKey(String raw) {
        if (raw == null) return null;
        String key = Normalizer.normalize(raw.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        for (SalventeTrait trait : values()) if (trait.key.equals(key)) return trait;
        return null;
    }

    /** Etiqueta legible con tildes. */
    public String label() {
        switch (this) {
            case EMPATIA: return "Empatía";
            case ENERGIA: return "Energía";
            case AUTONOMIA: return "Autonomía";
            case PROTECCION: return "Protección";
            default: return key.substring(0, 1).toUpperCase(Locale.ROOT) + key.substring(1);
        }
    }
}
