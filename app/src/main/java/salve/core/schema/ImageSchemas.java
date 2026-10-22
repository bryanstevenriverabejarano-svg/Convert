package salve.core.schema;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Esquemas de imagen (Lakoff y Johnson): unas pocas estructuras de la experiencia física con las
 * que se entienden también las cosas abstractas. «La economía se cae» usa SOPORTE; «marcar
 * fronteras emocionales» usa FRONTERA; «estar en un callejón sin salida» usa CAMINO y BLOQUEO.
 *
 * <p>El catálogo es fijo y está escrito a mano, con sus papeles y sus inferencias: no se le pide al
 * modelo que invente axiomas ni números (un modelo pequeño los inventaría distintos cada vez). El
 * modelo solo elige esquemas de esta lista y dice qué hace cada papel; las consecuencias salen de
 * aquí, siempre las mismas y comprobables.</p>
 */
public final class ImageSchemas {
    private ImageSchemas() { }

    public enum Schema {
        CONTENEDOR("contenedor", "contenido", "contenedor", "algo dentro de algo que lo rodea",
                "dentro|fuera de|lleno|vacio|meter|metido|encerrad\\w*|atrapad\\w*|contiene|incluye|salir de|escapar",
                "{0} está separado de lo que queda fuera de {1}",
                "si {1} se abre o se rompe, {0} puede salir",
                "lo que esté dentro de {0} también está dentro de {1}"),
        SOPORTE("soporte", "sostenido", "soporte", "algo que se mantiene gracias a lo que lo sostiene",
                "sostiene|sostener|apoya|apoyo|se cae|caer|cayo|derrumb\\w*|cimiento\\w*|pilar\\w*|sustento|aguanta|base de|"
                        + "colapsa\\w*|hundi\\w*|tambalea\\w*|desplom\\w*",
                "{0} se mantiene gracias a {1}",
                "si falta {1}, {0} cae",
                "si {1} se debilita, {0} se tambalea antes de caer"),
        CAMINO("camino", "viajero", "meta", "ir de un origen a una meta pasando por un recorrido",
                "camino|meta|objetivo|llegar a|avanzar|avanza|retroceder|rumbo|trayecto|paso a paso|destino|desviar\\w*|callejon",
                "{0} sale de un origen y avanza hacia {1}",
                "cada paso acerca {0} a {1}",
                "un obstáculo obliga a {0} a parar, rodearlo o cambiar de rumbo"),
        FUERZA("fuerza", "agente", "afectado", "algo que empuja o mueve a otra cosa",
                "empuja\\w*|presion\\w*|fuerza|obliga\\w*|impulsa\\w*|arrastra\\w*|motiva\\w*|presiona\\w*",
                "{0} cambia el estado de {1}",
                "cuanta más fuerza ejerce {0}, más cambia {1}",
                "si {0} deja de actuar, {1} tiende a quedarse como está"),
        BLOQUEO("bloqueo", "obstaculo", "bloqueado", "algo que impide avanzar a otra cosa",
                "bloquea\\w*|impide\\w*|obstaculo\\w*|barrera\\w*|atasc\\w*|frena\\w*|traba\\w*|no puede avanzar|sin salida|estancad\\w*",
                "{0} impide que {1} avance",
                "para seguir, {1} tiene que quitar, rodear o superar {0}",
                "si {0} desaparece, {1} vuelve a poder moverse"),
        EQUILIBRIO("equilibrio", "un_lado", "otro_lado", "dos lados que se compensan",
                "equilibri\\w*|balance|estable|inestable|estabilidad|compensa\\w*|desequilibr\\w*|contrapes\\w*",
                "todo se mantiene estable mientras {0} y {1} se compensan",
                "si {0} crece sin que {1} lo compense, todo se inclina",
                "algo inestable necesita muy poco para caer"),
        FRONTERA("frontera", "un_lado", "otro_lado", "un límite que separa dos zonas",
                "frontera\\w*|limite\\w*|borde|linea roja|cruzar|cruzo|separa\\w*|muro|espacio personal|traspasa\\w*",
                "{0} y {1} quedan separados por un límite",
                "pasar de {0} a {1} cuesta o necesita permiso",
                "respetar el límite deja que cada lado siga siendo como es"),
        VERTICALIDAD("verticalidad", "lo_alto", "lo_bajo", "arriba y abajo",
                "subir|sube|bajar|baja|alto|bajo|arriba|abajo|elevar|hundir|por los suelos|cima|fondo|ascen\\w*|descen\\w*",
                "{0} está por encima de {1}",
                "en lo humano, arriba suele ser más (mejor, más poder, más ánimo) y abajo menos",
                "pasar de {0} a {1} es descender, y volver cuesta esfuerzo"),
        CERCANIA("cercanía", "uno", "otro", "estar cerca o lejos",
                "cerca|lejos|distancia|proxim\\w*|alejar\\w*|acercar\\w*|intim\\w*|distante",
                "cuanto más cerca están {0} y {1}, más se influyen",
                "alejarse reduce lo que {0} y {1} se afectan"),
        PARTE_TODO("parte y todo", "parte", "todo", "una pieza de algo mayor",
                "parte de|pieza\\w*|componente\\w*|formad[oa] por|miembro\\w*|conjunto|fragment\\w*|entero|completo",
                "{0} es una parte de {1}",
                "si falta {0}, {1} queda incompleto",
                "lo que le pasa a {1} también afecta a {0}"),
        ENLACE("enlace", "uno", "otro", "dos cosas unidas",
                "unid[oa]s?|conectad\\w*|vinculo\\w*|lazo\\w*|atad[oa]|enlace|depende de|romper con|union",
                "{0} y {1} están unidos: lo que mueve a uno tira del otro",
                "si el enlace se rompe, {0} y {1} se separan"),
        CENTRO_PERIFERIA("centro y periferia", "centro", "periferia", "lo esencial y lo que lo rodea",
                "centro|nucleo|corazon de|esencial|periferia|marginal|secundari\\w*|al margen|lo principal",
                "{0} es lo esencial y {1} lo que está alrededor",
                "cambiar {0} lo cambia todo; cambiar {1}, poco"),
        CICLO("ciclo", "fase", "ciclo", "algo que vuelve a empezar",
                "ciclo\\w*|se repite|otra vez|rutina|estacion\\w*|renac\\w*|cada dia|una y otra vez|vuelve a empezar",
                "{0} vuelve una y otra vez dentro de {1}",
                "después del final, {1} vuelve a empezar"),
        ESCALA("escala", "cosa", "cantidad", "más o menos de algo",
                "escala|grado|intensidad|aumenta\\w*|disminuye\\w*|crece\\w*|se multiplica|se reduce",
                "{0} puede tener más o menos {1}",
                "a más {1}, más efecto tiene {0}"),
        ATRACCION("atracción", "atrae", "atraido", "algo que tira de otra cosa hacia sí",
                "atrae\\w*|atraccion|iman|tentacion|repele\\w*|rechaza\\w*|gravedad|magnetic\\w*|irresistible",
                "{1} tiende a moverse hacia {0}",
                "cuanto más cerca está {1} de {0}, más fuerte es la atracción"),
        CAMBIO_ESTADO("cambio de estado", "antes", "despues", "pasar de un estado a otro",
                "se convierte|convertirse|transforma\\w*|pasa a ser|derrite|madura\\w*|evoluciona\\w*|despierta\\w*|cambia de",
                "lo que era {0} pasa a ser {1}",
                "hace falta una causa (energía, tiempo, un empujón) para pasar de {0} a {1}");

        public final String label, roleA, roleB, gloss;
        final Pattern cues;
        final String[] inferences;

        Schema(String label, String roleA, String roleB, String gloss, String cues, String... inferences) {
            this.label = label; this.roleA = roleA; this.roleB = roleB; this.gloss = gloss;
            this.cues = Pattern.compile("\\b(?:" + cues + ")\\b");
            this.inferences = inferences;
        }

        public String id() { return name().toLowerCase(Locale.ROOT); }

        /** Sus consecuencias con los papeles rellenos (o el nombre del papel si falta). */
        public List<String> infer(String a, String b) {
            String x = a == null || a.trim().isEmpty() ? "«" + roleA.replace('_', ' ') + "»" : a.trim();
            String y = b == null || b.trim().isEmpty() ? "«" + roleB.replace('_', ' ') + "»" : b.trim();
            List<String> out = new ArrayList<>();
            for (String t : inferences) out.add(t.replace("{0}", x).replace("{1}", y));
            return out;
        }
    }

    /** El esquema por su nombre, tolerando tildes, espacios y mayúsculas («Parte y todo», «parte_todo»). */
    public static Schema byName(String raw) {
        String t = normalize(raw).replace(' ', '_').replace('-', '_');
        for (Schema s : Schema.values()) {
            if (s.id().equals(t) || normalize(s.label).replace(' ', '_').equals(t)) return s;
        }
        if (t.startsWith("parte")) return Schema.PARTE_TODO;
        if (t.startsWith("centro")) return Schema.CENTRO_PERIFERIA;
        if (t.startsWith("cambio")) return Schema.CAMBIO_ESTADO;
        if (t.startsWith("cercan") || t.startsWith("distancia")) return Schema.CERCANIA;
        return null;
    }

    /** Esquemas que se notan en un texto por sus palabras, de más a menos pistas (como mucho 3). */
    public static List<Schema> detect(String text) {
        String t = normalize(text);
        List<Schema> found = new ArrayList<>();
        List<Integer> hits = new ArrayList<>();
        for (Schema s : Schema.values()) {
            Matcher m = s.cues.matcher(t);
            int n = 0;
            while (m.find()) n++;
            if (n > 0) { found.add(s); hits.add(n); }
        }
        List<Schema> sorted = new ArrayList<>(found);
        sorted.sort((a, b) -> hits.get(found.indexOf(b)) - hits.get(found.indexOf(a)));
        return sorted.subList(0, Math.min(3, sorted.size()));
    }

    /** La lista para el modelo: «contenedor (contenido, contenedor): algo dentro de algo…; …». Corta. */
    public static String catalog() {
        StringBuilder b = new StringBuilder();
        for (Schema s : Schema.values())
            b.append(b.length() == 0 ? "" : "; ").append(s.id()).append(" (").append(s.roleA).append(", ").append(s.roleB).append("): ").append(s.gloss);
        return b.toString();
    }

    static String normalize(String s) {
        return Normalizer.normalize(s == null ? "" : s.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[¿?¡!.,;:«»\"()]+", " ").replaceAll("\\s+", " ").trim();
    }
}
