package salve.core.identity;

import java.text.Normalizer;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Una sola política para todo lo que Salve envía a internet por iniciativa propia (curiosidad,
 * búsqueda autónoma, herramienta web.search del modelo). Una consulta sale solo si no contiene
 * nombres de personas conocidas, vida personal (familia, salud, dinero, credenciales) ni datos
 * identificativos. Las búsquedas que Bryan pide expresamente siguen siendo decisión suya.
 */
public final class NetworkPrivacy {
    private static final String PERSONAL = "(?:mi|mis|tu|tus|su|sus|nuestro|nuestra|nuestros|nuestras|de bryan|del creador)";
    private static final Pattern PERSONAL_SUBJECT = Pattern.compile(".*\\b" + PERSONAL + " (?:propio |propia )?"
            + "(?:herman[oa]s?|madre|padre|mama|papa|hij[oa]s?|parej[ao]|novi[oa]|espos[oa]|marido|mujer|abuel[oa]s?|ti[oa]s?|prim[oa]s?|"
            + "sobrin[oa]s?|familia|familiares|amig[oa]s?|jefe|jefa|vecin[oa]s?|medic[oa]|doctor[a]?|psicolog[oa]|terapeuta|"
            + "diagnostico|tratamiento|enfermedad|salud|medicacion|medicamentos?|terapia|sintomas?|embarazo|analisis|"
            + "sueldo|salario|nomina|deudas?|hipoteca|cuenta|banco|tarjeta|contrasena|clave|pin|direccion|casa|telefono|movil|"
            + "dni|pasaporte|correo|email|ubicacion)\\b.*");
    private static final Pattern PRIVATE_TOPIC = Pattern.compile(".*\\b(bryan|mi creador|tu creador|siente por mi|me dijo|le dije|"
            + "nuestra conversacion|lo que hablamos|mis recuerdos|sus recuerdos)\\b.*");
    private static final Pattern IDENTIFIERS = Pattern.compile(".*(\\b\\d{7,}\\b|\\b[\\w.+-]+@[\\w-]+\\.[\\w.]+\\b|"
            + "\\b\\d{8}[a-z]\\b|\\b[a-z]{2}\\d{2}(?: ?\\d{4}){4,5}\\b).*");

    private NetworkPrivacy() { }

    /** true si la consulta puede salir a un buscador sin exponer a Bryan, su familia o terceros. */
    public static boolean safeForNetwork(String query, Collection<String> knownNames) {
        String t = normalize(query);
        if (t.isEmpty()) return false;
        if (PERSONAL_SUBJECT.matcher(t).matches() || PRIVATE_TOPIC.matcher(t).matches() || IDENTIFIERS.matcher(t).matches())
            return false;
        if (knownNames != null) for (String name : knownNames) {
            if (name == null) continue;
            for (String part : normalize(name).split(" "))
                if (part.length() >= 3 && Pattern.compile("\\b" + Pattern.quote(part) + "\\b").matcher(t).find()) return false;
        }
        return true;
    }

    static String normalize(String value) {
        return Normalizer.normalize(value == null ? "" : value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^\\p{L}\\p{N}@.+ -]+", " ").replaceAll("\\s+", " ").trim();
    }
}
