package salve.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The bundled download catalog. Metadata is declared information, never proof of runnable inference. */
public final class ModelCatalog {
    public enum Capability { TEXT, VISION }
    private static final Pattern PINNED_URL = Pattern.compile(
            "https://huggingface\\.co/[A-Za-z0-9_.-]+/[A-Za-z0-9_.-]+/resolve/([a-f0-9]{40})/[A-Za-z0-9_.-]+\\.litertlm(?:\\?download=true)?");
    private final List<Entry> entries;

    private ModelCatalog(List<Entry> entries) { this.entries = Collections.unmodifiableList(entries); }
    public List<Entry> getEntries() { return entries; }

    /** Actual successful inference during this process; text success never certifies vision. */
    public static final class RuntimeSnapshot {
        public final String path;
        public final Set<Capability> verifiedCapabilities;
        public RuntimeSnapshot(String path, Set<Capability> capabilities) {
            this.path = path;
            EnumSet<Capability> copy = EnumSet.noneOf(Capability.class);
            copy.addAll(capabilities);
            this.verifiedCapabilities = Collections.unmodifiableSet(copy);
        }
    }

    public Entry findById(String id) {
        for (Entry entry : entries) if (entry.id.equals(id)) return entry;
        return null;
    }

    /** Selection does not download or activate anything. Only verified adapters may be routed a turn. */
    public Entry selectReady(Set<Capability> required, Map<String, Set<Capability>> verifiedCapabilities, String preferredId) {
        return select(required, preferredId, entry -> verifiedCapabilities.containsKey(entry.id)
                && verifiedCapabilities.get(entry.id).containsAll(required));
    }

    /** Select one artifact explicitly; extending the catalog must never trigger a bulk download. */
    public Entry selectDownload(Set<Capability> required, String preferredId) {
        if (preferredId != null && findById(preferredId) == null) {
            throw invalid("Modelo solicitado fuera del catálogo");
        }
        return select(required, preferredId, entry -> preferredId == null || entry.id.equals(preferredId));
    }

    private Entry select(Set<Capability> required, String preferredId, java.util.function.Predicate<Entry> eligible) {
        if (required == null || required.isEmpty()) throw invalid("Indica la capacidad requerida");
        Entry first = null;
        for (Entry entry : entries) {
            if (!entry.declaredCapabilities.containsAll(required) || !eligible.test(entry)) continue;
            if (entry.id.equals(preferredId)) return entry;
            if (first == null) first = entry;
        }
        // No ranking by size or invented quality/latency scores. Preserve catalog order on equal evidence.
        return first;
    }

    public static ModelCatalog read(InputStream stream) {
        try (InputStream input = stream) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] chunk = new byte[4096];
            int count;
            while ((count = input.read(chunk)) != -1) {
                if (count > 1_048_576 - buffer.size()) throw invalid("Catálogo demasiado grande");
                buffer.write(chunk, 0, count);
            }
            byte[] bytes = buffer.toByteArray();
            try (JsonReader reader = new JsonReader(new StringReader(new String(bytes, StandardCharsets.UTF_8)))) {
                reader.setStrictness(Strictness.STRICT);
                JsonElement root = readValue(reader, 0);
                if (reader.peek() != JsonToken.END_DOCUMENT || !root.isJsonObject()) throw invalid("Catálogo inválido");
                JsonElement items = root.getAsJsonObject().get("items");
                if (items == null || !items.isJsonArray() || items.getAsJsonArray().isEmpty()) throw invalid("Catálogo sin modelos");
                List<Entry> entries = new ArrayList<>();
                Set<String> ids = new HashSet<>(), names = new HashSet<>(), filenames = new HashSet<>();
                for (JsonElement value : items.getAsJsonArray()) {
                    if (!value.isJsonObject()) throw invalid("Entrada de modelo inválida");
                    Entry entry = new Entry(value.getAsJsonObject());
                    if (!ids.add(key(entry.id)) || !names.add(key(entry.name)) || !filenames.add(key(entry.filename))) {
                        throw invalid("Identificador, nombre o archivo de modelo duplicado");
                    }
                    entries.add(entry);
                }
                return new ModelCatalog(entries);
            }
        } catch (IOException | IllegalStateException | NumberFormatException error) {
            throw new IllegalArgumentException("Catálogo de modelos inválido", error);
        }
    }

    /** Strict streaming read also rejects duplicate JSON keys, which a tree parser silently overwrites. */
    private static JsonElement readValue(JsonReader reader, int depth) throws IOException {
        if (depth > 8) throw invalid("Catálogo demasiado anidado");
        switch (reader.peek()) {
            case BEGIN_OBJECT:
                JsonObject object = new JsonObject();
                reader.beginObject();
                while (reader.hasNext()) {
                    String name = reader.nextName();
                    if (object.has(name)) throw invalid("Campo de catálogo duplicado: " + name);
                    object.add(name, readValue(reader, depth + 1));
                }
                reader.endObject();
                return object;
            case BEGIN_ARRAY:
                JsonArray array = new JsonArray();
                reader.beginArray();
                while (reader.hasNext()) array.add(readValue(reader, depth + 1));
                reader.endArray();
                return array;
            case STRING: return new JsonPrimitive(reader.nextString());
            case NUMBER: return new JsonPrimitive(new BigDecimal(reader.nextString()));
            case BOOLEAN: return new JsonPrimitive(reader.nextBoolean());
            case NULL: reader.nextNull(); return JsonNull.INSTANCE;
            default: throw invalid("JSON de catálogo inválido");
        }
    }

    public static final class Entry {
        public final String id, name, url, filename, sha256, revision;
        public final long sizeBytes;
        public final boolean supportsVision;
        public final Set<Capability> declaredCapabilities;
        /** Null means the catalog does not establish this fact. No hardware or quality is inferred from size. */
        public final String version, provider, location, license, modelCard, cpu, gpu;
        public final Long contextTokens, ramBytes, vramBytes;
        public final Double tokensPerSecond, costPerMillionTokens;
        public final List<String> specialties, limitations;

        private Entry(JsonObject item) {
            id = string(item, "id", true);
            String displayName = string(item, "name", false);
            name = displayName == null ? id : displayName;
            filename = string(item, "filename", true);
            if (!filename.matches("[A-Za-z0-9_-][A-Za-z0-9._-]{0,140}\\.litertlm")) throw invalid("Nombre o formato de modelo inválido");
            url = string(item, "url", true);
            Matcher pinned = PINNED_URL.matcher(url);
            if (!NetworkResourcePolicy.validateModelUrl(url).allowed || !pinned.matches()) {
                throw invalid("El enlace debe fijar una revisión de Hugging Face y un archivo LiteRT-LM");
            }
            revision = pinned.group(1);
            Long size = integer(item, "sizeBytes", true, false);
            if (size > 16L * 1024 * 1024 * 1024) throw invalid("Tamaño de modelo inválido");
            sizeBytes = size;
            sha256 = string(item, "sha256", true);
            if (!sha256.matches("[a-f0-9]{64}")) throw invalid("SHA-256 de modelo inválido");
            JsonElement vision = item.get("supportsVision");
            if (vision != null && (!vision.isJsonPrimitive() || !vision.getAsJsonPrimitive().isBoolean())) {
                throw invalid("supportsVision debe ser booleano");
            }
            supportsVision = vision != null && vision.getAsBoolean();
            declaredCapabilities = Collections.unmodifiableSet(supportsVision
                    ? EnumSet.of(Capability.TEXT, Capability.VISION) : EnumSet.of(Capability.TEXT));
            version = string(item, "version", false); provider = string(item, "provider", false);
            location = string(item, "location", false); license = string(item, "license", false);
            modelCard = string(item, "modelCard", false); cpu = string(item, "cpu", false); gpu = string(item, "gpu", false);
            if (modelCard != null && !NetworkResourcePolicy.validateKnowledgeUrl(modelCard).allowed) throw invalid("Ficha de modelo inválida");
            contextTokens = integer(item, "contextTokens", false, false);
            ramBytes = integer(item, "ramBytes", false, true); vramBytes = integer(item, "vramBytes", false, true);
            tokensPerSecond = decimal(item, "tokensPerSecond"); costPerMillionTokens = decimal(item, "costPerMillionTokens");
            specialties = strings(item, "specialties"); limitations = strings(item, "limitations");
        }
    }

    private static String string(JsonObject object, String field, boolean required) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            if (required) throw invalid("Falta " + field);
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) throw invalid(field + " debe ser texto");
        String text = value.getAsString();
        if (text.isEmpty() || text.length() > 2048 || !text.equals(text.trim()) || text.chars().anyMatch(Character::isISOControl)) {
            throw invalid("Texto inválido en " + field);
        }
        return text;
    }

    private static Long integer(JsonObject object, String field, boolean required, boolean allowZero) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) {
            if (required) throw invalid("Falta " + field);
            return null;
        }
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid(field + " debe ser un entero");
        try {
            long result = value.getAsBigDecimal().longValueExact();
            if (result < (allowZero ? 0 : 1)) throw invalid("Valor inválido en " + field);
            return result;
        } catch (ArithmeticException error) { throw invalid("Entero inválido en " + field); }
    }

    private static Double decimal(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) return null;
        if (!value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) throw invalid(field + " debe ser numérico");
        double number = value.getAsDouble();
        if (!Double.isFinite(number) || number < 0) throw invalid("Valor inválido en " + field);
        return number;
    }

    private static List<String> strings(JsonObject object, String field) {
        JsonElement value = object.get(field);
        if (value == null || value.isJsonNull()) return Collections.emptyList();
        if (!value.isJsonArray()) throw invalid(field + " debe ser una lista");
        List<String> strings = new ArrayList<>();
        for (JsonElement element : value.getAsJsonArray()) {
            JsonObject wrapper = new JsonObject(); wrapper.add(field, element);
            String text = string(wrapper, field, true);
            if (strings.contains(text)) throw invalid("Valor duplicado en " + field);
            strings.add(text);
        }
        return Collections.unmodifiableList(strings);
    }

    private static String key(String value) { return Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT); }
    private static IllegalArgumentException invalid(String message) { return new IllegalArgumentException(message); }
}
