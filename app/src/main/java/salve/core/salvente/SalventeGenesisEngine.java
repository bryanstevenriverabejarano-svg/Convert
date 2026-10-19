package salve.core.salvente;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Propone la ficha de un Salvente a partir de un propósito escrito por Bryan.
 *
 * 1. Pide la ficha al modelo (Salve la enruta a un nodo por TLS mutuo o al modelo del móvil).
 * 2. Si la respuesta es aprovechable pero incoherente, la repara sin inventar otra personalidad:
 *    recorta rasgos a [0, 1], quita capacidades que no sostiene y añade las obligatorias.
 * 3. Si no hay modelo, falla o devuelve algo inservible, usa un motor determinista: el mismo
 *    propósito produce siempre la misma propuesta, y «regenerar» produce una variante distinta.
 *
 * El motor solo propone: no guarda nada. Guardar exige la aprobación explícita de Bryan.
 */
public final class SalventeGenesisEngine {
    public interface Generator { String generate(String prompt) throws Exception; }

    public static final class Request {
        public final String purpose, preferredName;
        public Request(String purpose, String preferredName) {
            this.purpose = clean(purpose, 280);
            this.preferredName = clean(preferredName, 24);
        }
    }

    /** Propuesta para enseñar: la ficha válida, de dónde salió y qué hubo que corregir. */
    public static final class Draft {
        public final SalventeProfile profile;
        public final List<String> notes;
        Draft(SalventeProfile profile, List<String> notes) { this.profile = profile; this.notes = notes; }
    }

    static final String[] NAMES = {"Nyra", "Kael", "Lume", "Aren", "Iria", "Teo", "Selva", "Oren", "Mirel", "Alba",
            "Nilo", "Vega", "Ilen", "Zuri", "Elio", "Noa", "Brisa", "Darel", "Itza", "Lior"};
    private static final String[] ORDINALS = {"Segunda", "Tercera", "Cuarta", "Quinta", "Sexta"};

    /** Palabras del propósito → ajustes de rasgos y capacidades sugeridas (motor determinista). */
    private static final Object[][] LEXICON = {
            {"estudi|deberes|examen|colegio|instituto|clase|aprender|matematic|repas", SalventeCapability.APOYO_ESTUDIO,
                    SalventeTrait.PACIENCIA, .25, SalventeTrait.RIGOR, .2, SalventeTrait.CURIOSIDAD, .15},
            {"cuento|historia|dormir|nin[oa]|pequen|hij[oa]", SalventeCapability.CUENTOS,
                    SalventeTrait.CREATIVIDAD, .25, SalventeTrait.CALIDEZ, .25, SalventeTrait.PROTECCION, .2, SalventeTrait.PACIENCIA, .15},
            {"abuel|mayor|madre|padre|mama|papa|compan|soledad|\\bsol[oa]\\b", null,
                    SalventeTrait.CALIDEZ, .25, SalventeTrait.PACIENCIA, .25, SalventeTrait.EMPATIA, .2, SalventeTrait.PROTECCION, .15, SalventeTrait.ENERGIA, -.1},
            {"triste|ansied|estres|animo|apoyo|escuch|emocion|bienestar|desahog", SalventeCapability.BIENESTAR,
                    SalventeTrait.EMPATIA, .3, SalventeTrait.PRUDENCIA, .2, SalventeTrait.CALIDEZ, .2, SalventeTrait.OPTIMISMO, .1},
            {"organiz|agenda|tareas|rutina|trabajo|negocio|productiv|planific|horario", SalventeCapability.ORGANIZACION,
                    SalventeTrait.RIGOR, .3, SalventeTrait.AUTONOMIA, .2, SalventeTrait.FORMALIDAD, .15},
            {"idioma|ingles|frances|aleman|italiano|portugues|euskera|catalan|practicar", SalventeCapability.IDIOMAS,
                    SalventeTrait.PACIENCIA, .2, SalventeTrait.CURIOSIDAD, .15},
            {"crear|ideas|dibuj|music|arte|invent|juego|jugar|disen|cocin", SalventeCapability.CREATIVIDAD,
                    SalventeTrait.CREATIVIDAD, .3, SalventeTrait.HUMOR, .15, SalventeTrait.ENERGIA, .15},
            {"divert|risa|broma|gracios|alegr", null, SalventeTrait.HUMOR, .3, SalventeTrait.OPTIMISMO, .2, SalventeTrait.ENERGIA, .15},
            {"serio|formal|profesional|elegante", null, SalventeTrait.FORMALIDAD, .3, SalventeTrait.HUMOR, -.15},
            {"sincer|direct|claro|honest", null, SalventeTrait.FRANQUEZA, .3},
            {"tranquil|calm|seren|relaj|paz", null, SalventeTrait.ENERGIA, -.25, SalventeTrait.PACIENCIA, .2},
            {"energ|deporte|activo|motiv|entren", null, SalventeTrait.ENERGIA, .3, SalventeTrait.OPTIMISMO, .15},
            {"curios|ciencia|explor|pregunt|descubr", null, SalventeTrait.CURIOSIDAD, .3},
            {"cuid|proteg|segur|vigil", null, SalventeTrait.PROTECCION, .25, SalventeTrait.PRUDENCIA, .2},
            {"independ|iniciativa|autonom|proactiv", null, SalventeTrait.AUTONOMIA, .3},
    };

    private final Generator generator;
    private final Supplier<String> ids;
    private final LongSupplier clock;

    public SalventeGenesisEngine(Generator generator, Supplier<String> ids, LongSupplier clock) {
        this.generator = generator; this.ids = ids; this.clock = clock;
    }

    public static String newId() { return "slv-" + UUID.randomUUID(); }

    /** @param attempt 0 la primera vez; cada «regenerar» suma uno y cambia la propuesta. */
    public Draft propose(Request request, Collection<String> takenNames, int attempt) {
        if (request.purpose.length() < 3) throw new IllegalArgumentException("Cuéntame para qué o para quién es (al menos 3 letras).");
        List<String> notes = new ArrayList<>();
        String id = ids.get();
        if (generator != null) {
            try {
                String raw = generator.generate(prompt(request, takenNames, attempt));
                SalventeProfile fromModel = fromModel(raw, id, request, takenNames, attempt, notes);
                if (fromModel != null) return new Draft(fromModel, notes);
                notes.clear();
                notes.add("El modelo no dio una ficha válida; la he propuesto con el motor determinista.");
            } catch (Exception unavailable) {
                notes.clear();
                notes.add("No había modelo disponible; la he propuesto con el motor determinista.");
            }
        } else notes.add("Sin modelo: propuesta del motor determinista.");
        return new Draft(deterministic(id, request, takenNames, attempt), notes);
    }

    // ------------------------------------------------------------------ modelo

    static String prompt(Request request, Collection<String> taken, int attempt) {
        StringBuilder caps = new StringBuilder();
        for (SalventeCapability c : SalventeCapability.values()) {
            caps.append("\n- ").append(c.name()).append(": ").append(c.description);
            if (!c.minimums.isEmpty()) {
                caps.append(" (requiere");
                for (Map.Entry<SalventeTrait, Double> m : c.minimums.entrySet()) caps.append(' ').append(m.getKey().key).append(">=").append(m.getValue());
                caps.append(')');
            }
        }
        StringBuilder keys = new StringBuilder();
        for (SalventeTrait t : SalventeTrait.values()) keys.append(keys.length() == 0 ? "" : ", ").append('"').append(t.key).append("\":0.0");
        return "Eres Salve y vas a diseñar un Salvente: una inteligencia nueva, distinta de ti, con identidad propia. "
                + "Propósito que da Bryan: «" + request.purpose + "».\n"
                + (request.preferredName.isEmpty() ? "Elige un nombre original de 2 a 24 letras (no Salve)." : "Nombre pedido: " + request.preferredName + ".")
                + (taken == null || taken.isEmpty() ? "" : " Nombres ya usados: " + String.join(", ", taken) + ".")
                + "\nRasgos entre 0.0 y 1.0, con carácter (algunos altos y otros bajos, no todos a 0.5)."
                + "\nCapacidades permitidas (CONVERSACION y MEMORIA_PROPIA siempre, máximo 6):" + caps
                + "\nResponde SOLO con este JSON:\n{\"nombre\":\"\",\"rasgos\":{" + keys + "},\"capacidades\":[\"CONVERSACION\",\"MEMORIA_PROPIA\"],"
                + "\"saludo\":\"primera frase que diría, en su estilo, máximo 200 caracteres\"}"
                + (attempt > 0 ? "\nVariante " + (attempt + 1) + ": propón una personalidad diferente a la anterior." : "");
    }

    private SalventeProfile fromModel(String raw, String id, Request request, Collection<String> taken, int attempt, List<String> notes) {
        JsonObject json = firstObject(raw);
        if (json == null) return null;
        JsonObject rawTraits = json.has("rasgos") && json.get("rasgos").isJsonObject() ? json.getAsJsonObject("rasgos") : null;
        if (rawTraits == null) return null;
        EnumMap<SalventeTrait, Double> traits = new EnumMap<>(SalventeTrait.class);
        boolean clamped = false;
        for (Map.Entry<String, JsonElement> e : rawTraits.entrySet()) {
            SalventeTrait trait = SalventeTrait.fromKey(e.getKey());
            if (trait == null || !e.getValue().isJsonPrimitive() || !e.getValue().getAsJsonPrimitive().isNumber()) continue;
            double v = e.getValue().getAsDouble();
            if (Double.isNaN(v) || Double.isInfinite(v)) continue;
            if (v > 1 && v <= 100) { v = v / 100; }   // algunos modelos responden en porcentaje
            double c = Math.max(0, Math.min(1, v));
            clamped |= c != v;
            traits.put(trait, round(c));
        }
        if (traits.size() < SalventeTrait.values().length - 2) return null;   // casi sin rasgos: no es aprovechable
        for (SalventeTrait t : SalventeTrait.values()) if (!traits.containsKey(t)) { traits.put(t, 0.5); notes.add("El modelo no dio " + t.label().toLowerCase(Locale.ROOT) + "; queda en 50 %."); }
        if (clamped) notes.add("Ajusté rasgos fuera de rango.");

        EnumSet<SalventeCapability> caps = EnumSet.noneOf(SalventeCapability.class);
        if (json.has("capacidades") && json.get("capacidades").isJsonArray()) {
            for (JsonElement e : json.getAsJsonArray("capacidades")) {
                SalventeCapability c = e.isJsonPrimitive() ? SalventeCapability.fromKey(e.getAsString()) : null;
                if (c == null) { if (e.isJsonPrimitive()) notes.add("Descarté la capacidad no permitida «" + clean(e.getAsString(), 30) + "»."); continue; }
                if (!SalventeProfileValidator.supports(traits, c)) { notes.add("Quité " + c.label + ": su personalidad no la sostiene."); continue; }
                caps.add(c);
            }
        }
        for (SalventeCapability r : SalventeCapability.REQUIRED) caps.add(r);
        trimCapabilities(caps);

        String name = request.preferredName.isEmpty() ? text(json, "nombre", 24) : request.preferredName;
        SalventeProfile profile = build(id, name, request, text(json, "saludo", 280), traits, caps, SalventeProfile.Origin.MODELO);
        SalventeProfileValidator.Result check = SalventeProfileValidator.validate(profile, taken);
        if (nameProblem(check) != null) {
            String other = pickName(request, taken, attempt);
            notes.add("Cambié el nombre a " + other + ": " + nameProblem(check));
            profile = profile.withName(other);
            check = SalventeProfileValidator.validate(profile, taken);
        }
        if (!check.ok() && check.toString().contains("plana")) {
            traits = sharpen(traits);
            notes.add("Acentué los rasgos: la propuesta era demasiado plana.");
            profile = build(id, profile.name, request, profile.greeting, traits, caps, SalventeProfile.Origin.MODELO);
            check = SalventeProfileValidator.validate(profile, taken);
        }
        if (profile.greeting.isEmpty() && check.ok()) profile = build(id, profile.name, request, greeting(profile), traits, caps, SalventeProfile.Origin.MODELO);
        return check.ok() ? profile : null;
    }

    // ------------------------------------------------------------ determinista

    SalventeProfile deterministic(String id, Request request, Collection<String> taken, int attempt) {
        String norm = norm(request.purpose);
        Random random = new Random(seed(norm + "#" + attempt));
        EnumMap<SalventeTrait, Double> traits = new EnumMap<>(SalventeTrait.class);
        for (SalventeTrait t : SalventeTrait.values()) traits.put(t, 0.5);
        EnumSet<SalventeCapability> hinted = EnumSet.noneOf(SalventeCapability.class);
        for (Object[] entry : LEXICON) {
            if (!java.util.regex.Pattern.compile((String) entry[0]).matcher(norm).find()) continue;
            if (entry[1] != null) hinted.add((SalventeCapability) entry[1]);
            for (int i = 2; i < entry.length; i += 2) traits.merge((SalventeTrait) entry[i], (Double) entry[i + 1], Double::sum);
        }
        for (SalventeTrait t : SalventeTrait.values())
            traits.put(t, round(Math.max(0.05, Math.min(0.95, traits.get(t) + (random.nextDouble() - 0.5) * 0.24))));
        // Lo que pide el propósito se respeta: si sugiere una capacidad, sus rasgos llegan al mínimo.
        for (SalventeCapability c : hinted)
            for (Map.Entry<SalventeTrait, Double> need : c.minimums.entrySet())
                if (traits.get(need.getKey()) < need.getValue()) traits.put(need.getKey(), round(Math.min(0.95, need.getValue() + 0.05)));
        traits = sharpen(traits);
        EnumSet<SalventeCapability> caps = EnumSet.noneOf(SalventeCapability.class);
        for (SalventeCapability r : SalventeCapability.REQUIRED) caps.add(r);
        caps.addAll(hinted);
        trimCapabilities(caps);
        String name = request.preferredName;
        SalventeProfile probe = build(id, name, request, "", traits, caps, SalventeProfile.Origin.DETERMINISTA);
        if (name.isEmpty() || !SalventeProfileValidator.validate(probe, taken).ok()) name = pickName(request, taken, attempt);
        SalventeProfile profile = build(id, name, request, "", traits, caps, SalventeProfile.Origin.DETERMINISTA);
        return build(id, name, request, greeting(profile), traits, caps, SalventeProfile.Origin.DETERMINISTA);
    }

    /** Garantiza carácter: si la diferencia entre el rasgo más alto y el más bajo es pequeña, la abre. */
    static EnumMap<SalventeTrait, Double> sharpen(EnumMap<SalventeTrait, Double> traits) {
        SalventeTrait hi = SalventeTrait.CURIOSIDAD, lo = SalventeTrait.CURIOSIDAD;
        for (SalventeTrait t : SalventeTrait.values()) {
            if (traits.get(t) > traits.get(hi)) hi = t;
            if (traits.get(t) < traits.get(lo)) lo = t;
        }
        double spread = traits.get(hi) - traits.get(lo);
        if (spread >= SalventeProfileValidator.MIN_SPREAD + 0.05) return traits;
        EnumMap<SalventeTrait, Double> out = new EnumMap<>(traits);
        double gap = (SalventeProfileValidator.MIN_SPREAD + 0.1 - spread) / 2;
        if (hi == lo) lo = hi == SalventeTrait.FORMALIDAD ? SalventeTrait.HUMOR : SalventeTrait.FORMALIDAD;
        out.put(hi, round(Math.min(0.95, out.get(hi) + gap)));
        out.put(lo, round(Math.max(0.05, out.get(lo) - gap)));
        return out;
    }

    String pickName(Request request, Collection<String> taken, int attempt) {
        java.util.Set<String> used = new java.util.HashSet<>();
        if (taken != null) for (String t : taken) if (t != null) used.add(SalventeProfileValidator.key(t));
        int start = (int) Math.floorMod(seed(norm(request.purpose)) + attempt, (long) NAMES.length);
        for (String suffix : withEmpty(ORDINALS))
            for (int i = 0; i < NAMES.length; i++) {
                String candidate = NAMES[(start + i) % NAMES.length] + (suffix.isEmpty() ? "" : " " + suffix);
                if (!used.contains(SalventeProfileValidator.key(candidate))) return candidate;
            }
        throw new IllegalStateException("No quedan nombres libres; escribe uno.");
    }

    static String greeting(SalventeProfile p) {
        boolean formal = p.trait(SalventeTrait.FORMALIDAD) >= 0.7;
        StringBuilder out = new StringBuilder(formal ? "Buenos días, soy " : "¡Hola! Soy ").append(p.name).append('.');
        if (p.trait(SalventeTrait.CALIDEZ) >= 0.7) out.append(formal ? " Es un placer conocerle." : " Me alegra mucho conocerte.");
        // El propósito lo escribe Bryan («para mi madre»): citarlo en boca del Salvente sonaría falso.
        List<String> skills = new ArrayList<>();
        for (SalventeCapability c : p.capabilities) if (!c.isRequired()) skills.add(c.label.toLowerCase(Locale.ROOT));
        if (!skills.isEmpty()) out.append(formal ? " Puedo ayudarle con " : " Puedo ayudarte con ").append(joinSpanish(skills)).append('.');
        else out.append(formal ? " Estoy aquí para conversar con usted." : " Estoy aquí para charlar contigo.");
        if (p.trait(SalventeTrait.CURIOSIDAD) >= 0.7) out.append(formal ? " ¿Por dónde le gustaría empezar?" : " ¿Por dónde empezamos?");
        else if (p.trait(SalventeTrait.HUMOR) >= 0.7) out.append(" Prometo alguna broma por el camino.");
        String text = out.toString();
        return text.length() > 280 ? text.substring(0, 279) + "…" : text;
    }

    // ---------------------------------------------------------------- utilidades

    private SalventeProfile build(String id, String name, Request request, String greeting, Map<SalventeTrait, Double> traits,
                                  Set<SalventeCapability> caps, SalventeProfile.Origin origin) {
        return new SalventeProfile(id, name, SalventeProfile.SPECIES, request.purpose, greeting, traits, caps,
                SalventeProfile.State.ACTIVO, origin, clock.getAsLong());
    }

    private static void trimCapabilities(EnumSet<SalventeCapability> caps) {
        while (caps.size() > SalventeCapability.MAX) {
            SalventeCapability last = null;
            for (SalventeCapability c : caps) if (!c.isRequired()) last = c;
            caps.remove(last);
        }
    }

    static JsonObject firstObject(String raw) {
        if (raw == null) return null;
        int start = raw.indexOf('{');
        while (start >= 0) {
            int depth = 0; boolean string = false, escape = false;
            for (int i = start; i < raw.length(); i++) {
                char ch = raw.charAt(i);
                if (string) { if (escape) escape = false; else if (ch == '\\') escape = true; else if (ch == '"') string = false; continue; }
                if (ch == '"') string = true;
                else if (ch == '{') depth++;
                else if (ch == '}' && --depth == 0) {
                    try { JsonElement e = JsonParser.parseString(raw.substring(start, i + 1)); if (e.isJsonObject()) return e.getAsJsonObject(); }
                    catch (RuntimeException ignored) { }
                    break;
                }
            }
            start = raw.indexOf('{', start + 1);
        }
        return null;
    }

    private static String text(JsonObject json, String field, int max) {
        JsonElement e = json.get(field);
        return e != null && e.isJsonPrimitive() ? clean(e.getAsString(), max) : "";
    }

    static String clean(String value, int max) {
        if (value == null) return "";
        String v = value.replaceAll("\\p{Cntrl}+", " ").replaceAll("\\s+", " ").trim();
        return v.length() > max ? v.substring(0, max).trim() : v;
    }

    private static String norm(String value) {
        return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static long seed(String value) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            long s = 0; for (int i = 0; i < 8; i++) s = (s << 8) | (d[i] & 0xff);
            return s;
        } catch (Exception impossible) { return value.hashCode(); }
    }

    private static double round(double v) { return Math.round(v * 100) / 100.0; }

    private static String joinSpanish(List<String> items) {
        if (items.size() == 1) return items.get(0);
        return String.join(", ", items.subList(0, items.size() - 1)) + " y " + items.get(items.size() - 1);
    }

    private static String nameProblem(SalventeProfileValidator.Result check) {
        for (String problem : check.problems)
            if (problem.contains("nombre") || problem.contains("llamado") || problem.contains("reservado")) return problem;
        return null;
    }

    private static String[] withEmpty(String[] suffixes) {
        String[] out = new String[suffixes.length + 1]; out[0] = "";
        System.arraycopy(suffixes, 0, out, 1, suffixes.length); return out;
    }
}
