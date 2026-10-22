package salve.core;

import android.util.Log;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import salve.core.reading.DocumentReading;
import salve.core.reading.StoryShelf;

/**
 * Su mesa de lectura: lee un documento por partes, lo analiza y hace con él lo que pidió Bryan
 * (guardarlo como cuento, aprender de él, o solo opinar sin guardar nada). Trabaja en su propio
 * hilo de baja prioridad, con el modelo local (lo que lee no sale del móvil), para no bloquear la
 * conversación: entre parte y parte, un mensaje de Bryan se atiende primero.
 */
final class ReadingDesk {
    private final MotorConversacional m;
    private final ExecutorService reader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "salve-lectura");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private volatile StoryShelf shelf;

    ReadingDesk(MotorConversacional m) { this.m = m; }

    StoryShelf shelf() {
        if (shelf == null) synchronized (this) {
            if (shelf == null) shelf = new StoryShelf(new salve.core.mind.MindFileStore(
                    new File(new File(m.context.getNoBackupFilesDir(), "mind"), "stories.json")), System::currentTimeMillis);
        }
        return shelf;
    }

    /** Lo que Bryan le da para leer y para qué (la lectura en voz alta, si la pide, la hace la pantalla). */
    void leer(String name, String text, DocumentReading.Mode mode) {
        leer(name, text, mode, mode == DocumentReading.Mode.STORY, mode != DocumentReading.Mode.LEARN, mode != DocumentReading.Mode.OPINION);
    }

    /**
     * @param illustrate ilustrar lo que se pueda dibujar
     * @param opinion dar su opinión sincera
     * @param save guardarlo (cuento: estantería y recuerdos; si no, recuerdos)
     */
    void leer(String name, String text, DocumentReading.Mode mode, boolean illustrate, boolean opinion, boolean save) {
        if (text == null || text.trim().isEmpty()) { m.hablar("No he encontrado texto en ese documento. Si es escaneado, mándamelo como fotos."); return; }
        String title = cleanTitle(name);
        DocumentReading.Plan plan = DocumentReading.plan(text);
        m.hablar("Voy a leer «" + title + "» con calma" + (plan.chunks.size() > 1 ? ", en " + plan.chunks.size() + " partes" : "")
                + ": " + plannedWork(mode, illustrate, opinion, save) + " Puedes seguir hablando conmigo mientras.");
        try { reader.execute(() -> leerAhora(title, text, plan, mode, illustrate, opinion, save)); }
        catch (java.util.concurrent.RejectedExecutionException closed) { /* la app se cierra */ }
    }

    /** «lo entenderé, lo ilustraré, te diré qué me parece y lo guardaré en mis cuentos.» */
    static String plannedWork(DocumentReading.Mode mode, boolean illustrate, boolean opinion, boolean save) {
        List<String> parts = new ArrayList<>();
        parts.add(mode == DocumentReading.Mode.STORY ? "lo entenderé" : mode == DocumentReading.Mode.LEARN ? "sacaré lo importante" : "lo analizaré");
        if (illustrate) parts.add("lo ilustraré");
        if (opinion) parts.add("te diré qué me parece");
        parts.add(save ? (mode == DocumentReading.Mode.STORY ? "lo guardaré en mis cuentos" : "lo guardaré en mis recuerdos") : "no lo guardaré");
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < parts.size(); i++) b.append(i == 0 ? "" : i == parts.size() - 1 ? " y " : ", ").append(parts.get(i));
        return b.append('.').toString();
    }

    private void leerAhora(String title, String text, DocumentReading.Plan plan, DocumentReading.Mode mode,
                           boolean illustrate, boolean opinion, boolean save) {
        try {
            if (m.llm == null) { m.hablar("Ahora no tengo un modelo cargado para leer. Prueba en un momento."); return; }
            List<String> notes = new ArrayList<>();
            for (int i = 0; i < plan.chunks.size(); i++) {
                if (m.closed) return;
                m.estado("Leyendo «" + title + "»: parte " + (i + 1) + " de " + plan.chunks.size() + "…");
                ModelResult r = m.llm.generateResult(DocumentReading.notesPrompt(mode, title, i + 1, plan.chunks.size(), plan.chunks.get(i)),
                        SalveLLM.Role.CONVERSACIONAL);
                if (r.isSuccess() && r.getText() != null && !r.getText().trim().isEmpty()) {
                    String n = r.getText().trim().replaceAll("\\s*\\n\\s*", " · ");
                    notes.add(n.length() > DocumentReading.NOTE_CHARS ? n.substring(0, DocumentReading.NOTE_CHARS - 1) + "…" : n);
                } else notes.add("(no pude tomar notas de esta parte)");
            }
            m.estado("Pensando en lo que he leído…");
            ModelResult fin = m.llm.generateResult(DocumentReading.finalPrompt(mode, title, notes, plan.sampled(), opinion), SalveLLM.Role.CONVERSACIONAL);
            if (!fin.isSuccess()) { m.hablar("He leído «" + title + "», pero no he conseguido terminar de analizarlo. Pídemelo otra vez en un rato."); return; }
            DocumentReading.Analysis a = DocumentReading.parse(mode, fin.getText(), title);
            String date = salve.core.mind.Biography.date(System.currentTimeMillis());
            boolean saved = false;
            if (save && mode == DocumentReading.Mode.STORY) {
                String file = guardarTexto(a.title, text);
                shelf().add(a, file);
                List<String> tags = new ArrayList<>(java.util.Arrays.asList("cuento", "lectura"));
                for (String c : a.characterNames()) if (tags.size() < 8) tags.add(c);
                m.memoria.guardarRecuerdo(a.memoryText(date), "alegria", 6, tags);
                saved = true;
            } else if (save) {
                m.memoria.guardarRecuerdo(a.memoryText(date), "curiosidad", 6, java.util.Arrays.asList("lectura", "documento",
                        mode == DocumentReading.Mode.LEARN ? "aprendizaje" : "opinion"));
                saved = true;
            }
            if (saved) m.mind().indexNewMemoriesAsync();
            // Lo que aprendió de un documento se ancla a sus esquemas cuando sueñe.
            if (saved && mode == DocumentReading.Mode.LEARN) m.mind().concepts().queue(a.title);
            m.registrarAccion(salve.core.mind.SelfActionLog.Kind.READ, "«" + a.title + "»",
                    "lo leí y " + plannedWork(mode, illustrate, opinion, saved).replace("lo entenderé", "lo entendí")
                            .replace("sacaré", "saqué").replace("lo analizaré", "lo analicé").replace("lo ilustraré", "lo ilustré")
                            .replace("te diré", "te dije").replace("lo guardaré", "lo guardé").replace("no lo guardaré", "no lo guardé"));
            String told = a.spoken(saved, plan.sampled(), opinion, illustrate);
            // Ilustrado: cada escena o cosa que reconoce en su repertorio lleva su dibujo.
            if (illustrate) {
                salve.sketch.SketchLibrary library = salve.sketch.Sketches.library(m.context);
                if (library != null) told = salve.sketch.StoryIllustrator.annotate(told, library, false);
            }
            // Si me lo está leyendo en voz alta, espero a que termine: hablar ahora lo cortaría.
            for (int waited = 0; waited < 1800 && m.leyendoEnVozAlta(); waited++) Thread.sleep(1000);
            m.hablar(told);
        } catch (Exception failure) {
            Log.w(MotorConversacional.TAG, "Lectura interrumpida: " + failure.getClass().getSimpleName());
            m.hablar("Me he quedado a medias leyendo «" + title + "». Vuelve a dármelo y lo intento otra vez.");
        }
    }

    /** «¿Qué cuentos tienes?» y «léeme el cuento de …». @return respuesta, o null si no es eso */
    String comandoCuentos(String entrada) {
        if (DocumentReading.asksForStoryList(entrada)) return shelf().describe();
        String wanted = DocumentReading.storyToRetell(entrada);
        if (wanted == null) return null;
        StoryShelf.Story s = shelf().find(wanted);
        if (s == null) return null; // no es uno de los suyos: que siga la conversación (puede inventarlo)
        String text = leerTexto(s.file);
        if (text == null) return "Recuerdo «" + s.title + "»: " + s.summary + " Pero ya no tengo el texto entero para leértelo.";
        String shown = text.length() > 8000 ? text.substring(0, 8000).trim() + "…\n\n(Es largo: te he leído el principio.)" : text.trim();
        salve.sketch.SketchLibrary library = salve.sketch.Sketches.library(m.context);
        return "«" + s.title + "»\n\n" + (library == null ? shown : salve.sketch.StoryIllustrator.annotate(shown, library, false));
    }

    private String guardarTexto(String title, String text) {
        try {
            File dir = new File(m.context.getFilesDir(), "library");
            if (!dir.isDirectory() && !dir.mkdirs()) return null;
            String slug = java.text.Normalizer.normalize(title.toLowerCase(java.util.Locale.ROOT), java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
            if (slug.length() > 40) slug = slug.substring(0, 40);
            File f = new File(dir, (slug.isEmpty() ? "cuento" : slug) + "-" + System.currentTimeMillis() + ".txt");
            java.nio.file.Files.write(f.toPath(), text.getBytes(StandardCharsets.UTF_8));
            return f.getName();
        } catch (Exception failure) { return null; }
    }

    private String leerTexto(String file) {
        if (file == null || file.contains("/") || file.contains("..")) return null;
        try {
            File f = new File(new File(m.context.getFilesDir(), "library"), file);
            if (!f.isFile() || f.length() > 4_000_000) return null;
            return new String(java.nio.file.Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
        } catch (Exception unreadable) { return null; }
    }

    /** «El_dragon-final.pdf» → «El dragon final». */
    static String cleanTitle(String name) {
        String t = name == null ? "" : name.replaceAll("(?i)\\.(pdf|txt|md)$", "").replaceAll("\\s*\\(\\d+\\)$", "")
                .replaceAll("[_]+", " ").replaceAll("\\s+", " ").trim();
        return t.isEmpty() ? "el documento" : t;
    }
}
