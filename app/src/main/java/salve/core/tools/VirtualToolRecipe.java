package salve.core.tools;

import com.google.gson.Gson;
import com.google.gson.Strictness;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

import java.io.IOException;
import java.io.StringReader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

/** Bounded, declarative recipes. A recipe never loads or executes generated program code. */
public final class VirtualToolRecipe {
    public static final int MAX_STEPS = 8;
    private static final int MAX_JSON_CHARS = 24_000;
    private final List<String> steps;

    private VirtualToolRecipe(List<String> steps) {
        this.steps = Collections.unmodifiableList(new ArrayList<>(steps));
    }

    public static String normalizeName(String name) {
        if (name == null) throw new IllegalArgumentException("Falta el nombre de la rutina.");
        String value = Normalizer.normalize(name.trim(), Normalizer.Form.NFC)
                .toLowerCase(Locale.ROOT);
        if (!value.matches("[\\p{L}\\p{N}][\\p{L}\\p{N} _-]{0,63}")) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y 64 letras, números o espacios.");
        }
        return value;
    }

    public static VirtualToolRecipe parse(String json) {
        if (json == null || json.length() > MAX_JSON_CHARS) {
            throw new IllegalArgumentException("La receta está vacía o supera el tamaño permitido.");
        }
        List<String> result = new ArrayList<>();
        Gson gson = new Gson();
        try (JsonReader reader = new JsonReader(new StringReader(json))) {
            reader.setStrictness(Strictness.STRICT);
            reader.beginArray();
            while (reader.hasNext()) {
                if (result.size() == MAX_STEPS) throw new IllegalArgumentException("Máximo 8 pasos por rutina.");
                reader.beginObject();
                Map<String, String> fields = new LinkedHashMap<>();
                while (reader.hasNext()) {
                    String key = reader.nextName();
                    if (fields.containsKey(key) || reader.peek() != JsonToken.STRING) {
                        throw new IllegalArgumentException("Parámetro duplicado o de tipo incorrecto.");
                    }
                    fields.put(key, reader.nextString());
                }
                reader.endObject();
                validateStep(fields);
                result.add(gson.toJson(fields));
            }
            reader.endArray();
            if (result.isEmpty() || reader.peek() != JsonToken.END_DOCUMENT) {
                throw new IllegalArgumentException("La receta debe contener de 1 a 8 pasos y ningún texto adicional.");
            }
        } catch (IOException | IllegalStateException e) {
            throw new IllegalArgumentException("La receta no es un JSON válido.", e);
        }
        return new VirtualToolRecipe(result);
    }

    private static void validateStep(Map<String, String> fields) {
        String tool = fields.get("tool");
        String packageName = fields.get("paquete");
        if (packageName == null || packageName.length() > 255
                || !packageName.matches("[A-Za-z_][A-Za-z0-9_]*(\\.[A-Za-z_][A-Za-z0-9_]*)+")) {
            throw new IllegalArgumentException("Cada paso necesita el identificador completo de la aplicación en paquete.");
        }
        if ("ABRIR_APP".equals(tool)) {
            if (fields.size() != 2) {
                throw new IllegalArgumentException("ABRIR_APP necesita solamente tool y paquete.");
            }
        } else if ("ESCRIBIR".equals(tool) || "TAP_ID".equals(tool)) {
            if (fields.size() != 3 || !fields.containsKey("texto")) {
                throw new IllegalArgumentException(tool + " necesita solamente tool, paquete y texto.");
            }
            int maximum = "ESCRIBIR".equals(tool) ? 2_000 : 160;
            String text = fields.get("texto");
            if (text.trim().isEmpty() || text.length() > maximum || text.indexOf('\0') >= 0) {
                throw new IllegalArgumentException("El texto debe contener de 1 a " + maximum + " caracteres.");
            }
        } else {
            throw new IllegalArgumentException("Herramienta no permitida en una rutina: " + tool);
        }
    }

    public List<String> getSteps() { return steps; }

    public String toJson() { return "[" + String.join(",", steps) + "]"; }

    public enum Outcome { SUCCESS, FAILURE, CANCELLED }

    public interface StepExecutor {
        /** Complete only when the approved action has actually returned its result. */
        void execute(String stepJson, Consumer<Boolean> completed);
        /** Discard the pending proposal when execution is cancelled. */
        void cancel();
    }

    /** Only one step can await approval or execution. Late and duplicate results are ignored. */
    public static final class Execution {
        private final VirtualToolRecipe recipe;
        private final StepExecutor executor;
        private final Consumer<Outcome> completed;
        private boolean started;
        private boolean finished;
        private int nextStep;

        public Execution(VirtualToolRecipe recipe, StepExecutor executor, Consumer<Outcome> completed) {
            if (recipe == null || executor == null || completed == null) throw new IllegalArgumentException();
            this.recipe = recipe;
            this.executor = executor;
            this.completed = completed;
        }

        public synchronized void start() {
            if (started || finished) return;
            started = true;
            dispatchNext();
        }

        public synchronized boolean isRunning() { return started && !finished; }

        public synchronized void cancel() {
            if (finished) return;
            finished = true;
            try {
                executor.cancel();
            } finally {
                completed.accept(Outcome.CANCELLED);
            }
        }

        private void dispatchNext() {
            if (finished) return;
            if (nextStep == recipe.steps.size()) {
                finish(Outcome.SUCCESS);
                return;
            }
            int currentStep = nextStep;
            try {
                executor.execute(recipe.steps.get(currentStep), success -> acceptResult(currentStep, success));
            } catch (RuntimeException e) {
                // If a synchronous callback already advanced the recipe, this is a stale failure.
                if (!finished && nextStep == currentStep) finish(Outcome.FAILURE);
            }
        }

        private synchronized void acceptResult(int step, Boolean success) {
            if (finished || nextStep != step) return;
            if (!Boolean.TRUE.equals(success)) {
                finish(Outcome.FAILURE);
                return;
            }
            nextStep++;
            dispatchNext();
        }

        private void finish(Outcome outcome) {
            if (finished) return;
            finished = true;
            try {
                if (outcome == Outcome.FAILURE) executor.cancel();
            } finally {
                completed.accept(outcome);
            }
        }
    }
}
