package salve.sketch;

import com.google.gson.Gson;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diario de dibujo de Salve: para cada cosa que dibuja guarda cómo la entiende (su {@link VisualConcept}),
 * de dónde sacó las líneas (referencia, foto, dibujo de Bryan o formas propias), qué vio al mirar su
 * propio dibujo y qué le dijo Bryan. Con esto puede contestar «¿por qué lo dibujaste así?» con hechos
 * y, al mejorar un dibujo, no repite la misma referencia.
 *
 * Lo escribe sólo el código que dibuja; el modelo no puede añadir entradas. files/sketch/journal.json.
 */
public final class SketchJournal {
    public static final class Attempt {
        public long at;
        /** "web", "model", "photo", "hand" o "builtin". */
        public String source;
        /** Cómo lo hizo, en primera persona y con datos. */
        public String how;
        /** Binds factual notes to the geometry actually displayed; old entries remain unlinked. */
        public String drawingFingerprint;
        public String reference, referenceUrl;
        /** Lo que vio al mirar su propio dibujo (si el modelo tiene visión). */
        public String selfCheck;
        /** Lo que Bryan pidió («con más detalle, otra pose»). */
        public String wish;
    }

    static final class Subject {
        String name;
        String english, kind, pose;
        List<String[]> parts = new ArrayList<>();
        List<Attempt> attempts = new ArrayList<>();
        List<String> feedback = new ArrayList<>();
        List<String> usedReferences = new ArrayList<>();
    }

    static final class State {
        int version = 1;
        Map<String, Subject> subjects = new LinkedHashMap<>();
    }

    private static final Gson GSON = new Gson();
    public static final int MAX_SUBJECTS = 120, MAX_ATTEMPTS = 6, MAX_FEEDBACK = 4, MAX_REFS = 16;
    private final File file;
    private State state;

    public SketchJournal(File directory) { this.file = new File(directory, "journal.json"); }

    public static String key(String name) {
        return LearnedSketches.slug(SketchLibrary.normalize(LearnedSketches.stripArticle(LearnedSketches.displayName(name))));
    }

    private State state() {
        if (state != null) return state;
        try {
            if (file.isFile()) state = GSON.fromJson(new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8), State.class);
        } catch (IOException | RuntimeException damaged) { state = null; }
        if (state == null || state.subjects == null) state = new State();
        return state;
    }

    private Subject subject(String name, boolean create) {
        String k = key(name);
        Subject s = state().subjects.get(k);
        if (s == null && create) {
            s = new Subject();
            s.name = LearnedSketches.displayName(name);
            state().subjects.put(k, s);
            while (state().subjects.size() > MAX_SUBJECTS) state().subjects.remove(state().subjects.keySet().iterator().next());
        }
        if (s != null) {
            if (s.parts == null) s.parts = new ArrayList<>();
            if (s.attempts == null) s.attempts = new ArrayList<>();
            if (s.feedback == null) s.feedback = new ArrayList<>();
            if (s.usedReferences == null) s.usedReferences = new ArrayList<>();
        }
        return s;
    }

    public synchronized VisualConcept concept(String name) {
        Subject s = subject(name, false);
        if (s == null || s.parts.isEmpty()) return null;
        List<VisualConcept.Part> parts = new ArrayList<>();
        for (String[] p : s.parts) if (p != null && p.length == 2) parts.add(new VisualConcept.Part(p[0], p[1]));
        return new VisualConcept(s.name, s.english, s.kind, s.pose, parts);
    }

    public synchronized void saveConcept(VisualConcept c) {
        if (c == null || !c.isUsable()) return;
        Subject s = subject(c.subject, true);
        s.english = c.english; s.kind = c.kind; s.pose = c.pose;
        s.parts.clear();
        for (VisualConcept.Part p : c.parts) s.parts.add(new String[]{p.name, p.shape});
        save();
    }

    public synchronized void record(String name, Attempt a) {
        if (a == null) return;
        Subject s = subject(name, true);
        if (a.at == 0) a.at = System.currentTimeMillis();
        s.attempts.add(a);
        while (s.attempts.size() > MAX_ATTEMPTS) s.attempts.remove(0);
        if (a.referenceUrl != null && !a.referenceUrl.isEmpty() && !s.usedReferences.contains(a.referenceUrl)) {
            s.usedReferences.add(a.referenceUrl);
            while (s.usedReferences.size() > MAX_REFS) s.usedReferences.remove(0);
        }
        save();
    }

    /** Lo que Bryan opinó de su dibujo («no se parece a un gorila»). */
    public synchronized void feedback(String name, String said) {
        String t = VisualConcept.clip(said, 160);
        if (t.isEmpty()) return;
        Subject s = subject(name, true);
        s.feedback.add(t);
        while (s.feedback.size() > MAX_FEEDBACK) s.feedback.remove(0);
        save();
    }

    public synchronized List<String> usedReferences(String name) {
        Subject s = subject(name, false);
        return s == null ? new ArrayList<>() : new ArrayList<>(s.usedReferences);
    }

    public synchronized Attempt latest(String name) {
        Subject s = subject(name, false);
        return s == null || s.attempts.isEmpty() ? null : s.attempts.get(s.attempts.size() - 1);
    }

    /**
     * Por qué su dibujo es como es: datos de su diario, en primera persona. Vacío si no sabe nada de él.
     */
    public synchronized String explain(String name) {
        Subject s = subject(name, false);
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        Attempt last = s.attempts.isEmpty() ? null : s.attempts.get(s.attempts.size() - 1);
        if (last != null) {
            b.append("Cómo hice el último «").append(s.name).append("»: ").append(last.how == null ? "" : last.how);
            if (last.reference != null && !last.reference.isEmpty()) b.append(" Referencia: «").append(last.reference).append("».");
            if (last.wish != null && !last.wish.isEmpty()) b.append(" Bryan pidió: ").append(last.wish).append('.');
            if (last.selfCheck != null && !last.selfCheck.isEmpty()) b.append(" Al mirarlo: ").append(last.selfCheck);
        }
        VisualConcept c = concept(name);
        if (c != null) b.append(" Cómo lo entiendo: ").append(c.summary()).append('.');
        if (s.attempts.size() > 1) {
            b.append(" Intentos anteriores: ");
            for (int i = s.attempts.size() - 2, n = 0; i >= 0 && n < 2; i--, n++)
                b.append(n == 0 ? "" : "; ").append(sourceLabel(s.attempts.get(i).source));
            b.append('.');
        }
        if (!s.feedback.isEmpty()) b.append(" Bryan me dijo: «").append(String.join("», «", s.feedback)).append("».");
        return b.toString().trim();
    }

    public static String sourceLabel(String source) {
        if (source == null) return "de mi repertorio";
        switch (source) {
            case "web": return "a partir de una imagen de referencia de internet";
            case "model": return "compuesto por mí con formas geométricas";
            case "photo": return "trazado de una foto";
            case "hand": return "copiado del dibujo a mano de Bryan";
            default: return "de mi repertorio de serie";
        }
    }

    private void save() {
        try {
            File dir = file.getParentFile();
            if (dir != null && !dir.isDirectory() && !dir.mkdirs()) return;
            File tmp = new File(dir, file.getName() + ".tmp");
            Files.write(tmp.toPath(), GSON.toJson(state()).getBytes(StandardCharsets.UTF_8));
            Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException | RuntimeException ignored) { /* el diario es un apoyo: nunca bloquea un dibujo */ }
    }
}
