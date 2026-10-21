package salve.core.mind;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

/**
 * «¿Cómo está tu memoria?»: el estado real de su memoria en palabras normales. Cuántos recuerdos
 * tiene, si ya los puede buscar por significado (vectores) o solo por palabras, y quién es más
 * habitual en su vida. Así se sabe si la búsqueda por significado funciona de verdad en el móvil.
 */
public final class MemoryHealth {
    private MemoryHealth() { }

    public enum Semantic { READY, DOWNLOADING, FAILED, LOADING }

    public static boolean isCommand(String input) {
        String t = Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:]+", " ").replaceAll("\\s+", " ").trim().replaceFirst("^salve ", "");
        return t.matches("(?:como (?:esta|va|anda) tu memoria|estado de tu memoria|(?:dime |muestrame )?el estado de tu memoria|"
                + "tu memoria funciona(?: bien)?|funciona tu memoria|como estan tus recuerdos|revisa tu memoria|"
                + "(?:tienes|funciona) (?:la )?(?:memoria|busqueda) por significado|estado de (?:tus|los) vectores)");
    }

    /**
     * @param detail descripción de la descarga o de la prueba de español del modelo
     * @param closest nombres de las personas más habituales (de más a menos), puede estar vacío
     */
    public static String describe(int memories, int withVector, Semantic semantic, String detail, List<String> closest) {
        StringBuilder b = new StringBuilder("Tengo ").append(memories).append(memories == 1 ? " recuerdo guardado. " : " recuerdos guardados. ");
        switch (semantic) {
            case READY:
                b.append("Los busco por palabras y también por significado");
                if (memories > 0) {
                    int pct = (int) Math.round(100.0 * Math.min(withVector, memories) / memories);
                    b.append(": ").append(withVector).append(" de ").append(memories).append(" ya tienen su vector (").append(pct).append(" %)");
                    if (withVector < memories) b.append(" y sigo con los demás en segundo plano");
                }
                b.append(". Así encuentro un recuerdo aunque me lo digas con otras palabras.");
                break;
            case DOWNLOADING:
                b.append("Ahora solo puedo buscarlos por palabras: me falta el modelo que entiende el significado. ")
                        .append("Se descarga solo cuando estés con Wi-Fi (unos 123 MB, una vez). Estado: ").append(clean(detail)).append('.');
                break;
            case LOADING:
                b.append("Estoy preparando la búsqueda por significado; mientras tanto busco por palabras.");
                break;
            default:
                b.append("La búsqueda por significado no está funcionando (").append(clean(detail))
                        .append("), así que de momento busco solo por palabras.");
        }
        if (closest != null && !closest.isEmpty())
            b.append(" Las personas más presentes en mi vida ahora: ").append(String.join(", ", closest))
                    .append(": sus recuerdos me salen antes.");
        return b.toString();
    }

    /**
     * Lo que está pasando ahora: si está creando vectores, el último fallo y cómo va traer los
     * recuerdos de pCloud (tras reinstalar llegan por lotes y al principio hay pocos).
     */
    public static String extra(boolean indexing, String indexError, String cloudStatus, boolean cloudComplete) {
        StringBuilder b = new StringBuilder();
        if (indexing) b.append(" Ahora mismo estoy creando los vectores que faltan; pregúntamelo en un par de minutos.");
        else if (indexError != null && !indexError.isEmpty()) b.append(" La última vez que creé vectores falló (").append(indexError).append(").");
        if (cloudStatus != null && !cloudStatus.trim().isEmpty()) {
            if (cloudComplete) b.append(" Mi copia de pCloud está entera en este móvil.");
            else b.append(" Todavía estoy trayendo recuerdos de pCloud, así que puede que me falten: ").append(clean(cloudStatus)).append('.');
        }
        return b.toString();
    }

    private static String clean(String s) {
        String t = s == null ? "" : s.trim();
        if (t.endsWith(".")) t = t.substring(0, t.length() - 1);
        return t.isEmpty() ? "sin datos" : t;
    }
}
