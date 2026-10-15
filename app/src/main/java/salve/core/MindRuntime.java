package salve.core;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

import salve.core.memory.ConversationMemoryGrounding;
import salve.core.memory.MemoryProvenance;
import salve.core.memory.MemoryVectorStore;
import salve.core.mind.DreamCycle;
import salve.core.mind.EmbeddingCalibration;
import salve.core.mind.Interlocutor;
import salve.core.mind.Episode;
import salve.core.mind.MindCommand;
import salve.core.mind.MindFileStore;
import salve.core.mind.MindGenerator;
import salve.core.mind.PredictionLoop;
import salve.core.mind.ReflectionCycle;
import salve.core.mind.SelfModelJournal;
import salve.data.db.MemoriaDatabase;
import salve.data.db.RecuerdoDao;
import salve.data.db.RecuerdoEntity;

/**
 * Punto único de la mente persistente de Salve en Android.
 *
 * <p>Une el modelo de sí misma, la memoria por significado, el sueño, la reflexión y la
 * señal de sorpresa. Todo el razonamiento interno usa el modelo local: estos datos son
 * personales y nunca salen hacia un proveedor en la nube.</p>
 */
public final class MindRuntime {
    private static final String TAG = "Salve/Mind";
    private static volatile MindRuntime instance;

    private static final int INDEX_BATCH = 32;

    private final Context app;
    private final RecuerdoDao memories;
    private final SelfModelJournal journal;
    private final File vectorFile;
    private final MemoryVectorStore vectors;
    private final ExecutorService background = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "salve-mind");
        t.setPriority(Thread.MIN_PRIORITY);
        return t;
    });
    private final AtomicBoolean embedderLoading = new AtomicBoolean();
    private volatile EmbeddingsIndex embeddings;
    /** Resultado de la prueba de español del modelo de embeddings; null hasta cargarlo. */
    private volatile EmbeddingCalibration.Result calibration;
    private final PredictionLoop prediction;

    private MindRuntime(Context context) {
        app = context.getApplicationContext();
        memories = MemoriaDatabase.getInstance(app).recuerdoDao();
        File dir = new File(app.getNoBackupFilesDir(), "mind");
        journal = new SelfModelJournal(new MindFileStore(new File(dir, "self_model.json")), System::currentTimeMillis);
        vectorFile = new File(dir, "memory_vectors.bin");
        vectors = MemoryVectorStore.load(vectorFile);
        prediction = new PredictionLoop(journal, local(SalveLLM.Role.CONVERSACIONAL), this::embedOrThrow,
                System::currentTimeMillis, cosine -> {
                    EmbeddingCalibration.Result c = calibration;
                    return c == null ? -1 : c.surprise(cosine);
                });
    }

    /** Solo el modelo local: el contenido de la mente es personal y no se envía a la nube. */
    private MindGenerator local(SalveLLM.Role role) {
        return prompt -> {
            ModelResult result = SalveLLM.getInstance(app).generateResult(prompt, role);
            if (!result.isSuccess()) throw new IllegalStateException("Modelo local no disponible o fallido.");
            return result.getText();
        };
    }

    public static MindRuntime get(Context context) {
        if (instance == null) {
            synchronized (MindRuntime.class) {
                if (instance == null) instance = new MindRuntime(context);
            }
        }
        return instance;
    }

    public SelfModelJournal journal() { return journal; }

    /** Capacidades que existen de verdad en esta app; evita que el modelo describa las de un chatbot genérico. */
    static final String CAPABILITIES = "Capacidades reales: conversar con el modelo local; guardar datos que me cuentas "
            + "y buscarlos en mi memoria; consolidar recuerdos al dormir y pensar entre conversaciones; investigar en "
            + "Wikipedia o en URLs que me des; analizar fotos si el modelo tiene visión; controlar apps del móvil y crear "
            + "rutinas solo con tu aprobación; presupuesto personal; voz. Órdenes: «tu mente», «sueña ahora».";

    /**
     * Texto para el prompt de cada turno: con quién habla, qué puede hacer de verdad y su yo
     * persistente (narrativa, creencias, preguntas y pensamientos).
     */
    public String selfModelForPrompt(int maxChars) {
        try {
            salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(app);
            String head = salve.core.identity.IdentityPrompt.render(identity.session(), peopleGraph(),
                    identity.biometricAvailable(), System.currentTimeMillis()) + "\n" + CAPABILITIES + "\n";
            // Reflexiones y creencias sobre Bryan son su espacio íntimo: solo con acceso privado.
            String self = identity.privateAccess() ? journal.render(Math.max(120, maxChars - head.length()))
                    : "YO_PERSISTENTE: " + journal.narrative() + " (mis recuerdos y reflexiones sobre Bryan quedan reservados "
                    + "hasta que se verifique que es él).";
            return head + self;
        } catch (RuntimeException failure) { return ""; }
    }

    /** Árbol de personas calculado desde el perfil guardado (una sola fuente de verdad). */
    public salve.core.identity.PeopleGraph peopleGraph() {
        List<String[]> family = new ArrayList<>();
        for (String statement : familyFacts())
            for (salve.core.mind.FamilyFacts.Member m : salve.core.mind.FamilyFacts.parse(statement))
                family.add(new String[]{m.relation, m.name});
        return salve.core.identity.PeopleGraph.build(interlocutor().name, family);
    }

    /** Lo que consta del usuario: nombre declarado y si se presentó como creador. */
    public Interlocutor.Known interlocutor() {
        try {
            RecuerdoEntity name = memories.ultimoPorEtiqueta("profile:name");
            RecuerdoEntity relation = memories.ultimoPorEtiqueta("profile:relation");
            boolean creator = (relation != null && Interlocutor.declaresCreator(relation.frase))
                    || (name != null && Interlocutor.declaresCreator(name.frase));
            return new Interlocutor.Known(name == null ? null : Interlocutor.extractName(name.frase),
                    name == null ? null : Episode.memoryId(name.id), creator);
        } catch (RuntimeException unavailable) {
            return new Interlocutor.Known(null, null, false);
        }
    }

    /** Frases de familia guardadas («Mi hermana se llama Ana.»). */
    public List<String> familyFacts() {
        List<String> out = new ArrayList<>();
        try {
            List<RecuerdoEntity> rows = memories.perfiles();
            if (rows != null) for (RecuerdoEntity r : rows)
                if (r.etiquetas != null && r.etiquetas.contains("profile:family_") && r.frase != null) out.add(r.frase.trim());
        } catch (RuntimeException ignored) { }
        return out;
    }

    /** Respuesta directa a «¿quién es mi familia?», «¿cómo se llama mi madre?», o null. */
    public String familyAnswer(String input) {
        if (!salve.core.mind.FamilyFacts.asksAboutFamily(input)) return null;
        salve.core.identity.SessionIdentity session = salve.core.identity.IdentityManager.get(app).session();
        if (!session.isCreatorSide()) {
            String own = peopleGraph().familyFor(session.state(), session.name());
            return own.isEmpty() ? "Eso forma parte de la vida privada de " + creatorFirstName()
                    + "; no lo comparto sin saber que eres él." : own;
        }
        return salve.core.mind.FamilyFacts.answer(input, familyFacts());
    }

    private String creatorFirstName() {
        return peopleGraph().creator().name.split(" ")[0];
    }

    /** Respuesta directa a «¿sabes quién soy?», o null si la entrada no pregunta eso. */
    public String userIdentityAnswer(String input) {
        if (!Interlocutor.asksWhoUserIs(input)) return null;
        salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(app);
        salve.core.identity.SessionIdentity session = identity.session();
        String first = creatorFirstName();
        switch (session.state()) {
            case GUEST:
                return session.name() == null ? "No lo sé: me dijiste que no eres " + first + ". ¿Cómo te llamas?"
                        : "Te presentaste como " + session.name() + ". No te tengo entre las personas que conozco, pero encantada.";
            case FAMILY:
                return "Eres " + session.name() + ", " + salve.core.identity.PeopleGraph.display(session.relation())
                        + " de " + first + ", según te presentaste.";
            case UNCERTAIN:
                return (session.claimedCreator() ? "Dices que eres " + first + ", pero no he podido confirmarlo. "
                        : "No estoy segura de quién eres. ")
                        + (identity.biometricAvailable() ? "Si eres " + first + ", di «verifícame» y confírmalo con tu huella o tu rostro."
                        : "¿Me dices quién eres?");
            default:
                break;
        }
        if (interlocutor().name == null) {
            // Sin nombre guardado, en su propio teléfono: se presume Bryan y se dice por qué falta el dato.
            boolean cloudPending = salve.data.sync.CloudSyncManager.isPCloudConfigured(app)
                    && !salve.data.sync.CloudSyncManager.memoryRestoreComplete(app);
            return "Estás en el teléfono de " + first + ", así que lo más probable es que seas tú, " + first
                    + ", quien me creó. Aun así, en esta instalación no tengo tu nombre guardado"
                    + (cloudPending ? " porque todavía no he recuperado mis recuerdos de pCloud (di «recupera tus recuerdos»)" : "")
                    + ". " + (identity.biometricAvailable() ? "Si quieres que lo confirme, di «verifícame». " : "")
                    + "Si eres tú, dime «sí, soy yo» y lo recordaré.";
        }
        List<salve.core.mind.SelfModel.Belief> aboutUser = new ArrayList<>();
        for (salve.core.mind.SelfModel.Belief b : journal.beliefs())
            if (salve.core.mind.SelfModel.ABOUT_BRYAN.equals(b.about) && b.contradictions.isEmpty()) aboutUser.add(b);
        aboutUser.sort((a, b) -> Double.compare(b.confidence, a.confidence));
        String answer = Interlocutor.answer(interlocutor(), "Bryan", aboutUser);
        if (session.state() == salve.core.identity.SessionIdentity.State.VERIFIED_CREATOR && answer.startsWith("Eres "))
            answer += " Te he verificado con el teléfono.";
        return answer;
    }

    // ── Órdenes de Bryan ────────────────────────────────────────────────────────

    /** Devuelve la respuesta a una orden sobre el estado interno, o null si la entrada no lo es. */
    public String handleCommand(String input) {
        MindCommand.Action action = MindCommand.parse(input);
        if (action == null) return null;
        if (!salve.core.identity.IdentityManager.get(app).privateAccess())
            return "Mi mente guarda reflexiones sobre " + creatorFirstName() + "; solo la abro cuando sé que es él. "
                    + "Di «verifícame» y confírmalo con tu huella o tu rostro.";
        switch (action) {
            case DESCRIBE: {
                EmbeddingCalibration.Result c = calibration;
                return journal.describe() + "\n" + (c == null
                        ? "Memoria por significado: el modelo de embeddings aún no se ha cargado."
                        : c.describe());
            }
            case ACCEPT_NARRATIVE: return journal.acceptNarrative();
            case REJECT_NARRATIVE: return journal.rejectNarrative();
            case PAUSE: return journal.setPaused(true);
            case RESUME: return journal.setPaused(false);
            case EXPORT: return exportForTraining();
            case DREAM_NOW:
                salve.work.DreamWorker.enqueueNow(app);
                return "Programé un sueño: consolidaré mis recuerdos cuando el móvil esté cargando y sin una conversación activa.";
            default: return null;
        }
    }

    /**
     * Escribe el conocimiento consolidado y los episodios citados en la carpeta de la app accesible
     * por USB ({@code Android/data/<paquete>/files/exports}), para entrenar el adaptador en el PC.
     */
    private String exportForTraining() {
        try {
            File dir = app.getExternalFilesDir("exports");
            if (dir == null) return "No hay almacenamiento disponible para exportar.";
            java.util.Set<Integer> cited = new HashSet<>();
            for (String source : journal.referencedSources()) {
                if (!source.startsWith("recuerdos:")) continue;
                try { cited.add(Integer.parseInt(source.substring("recuerdos:".length()))); }
                catch (NumberFormatException ignored) { }
            }
            List<Episode> episodes = new ArrayList<>();
            if (!cited.isEmpty()) for (RecuerdoEntity r : memories.porIds(new ArrayList<>(cited))) addEpisode(episodes, r);
            String knowledge = salve.core.mind.MindDatasetExporter.knowledge(journal);
            java.nio.file.Files.write(new File(dir, "salve-knowledge.jsonl").toPath(),
                    knowledge.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.file.Files.write(new File(dir, "salve-episodes.jsonl").toPath(),
                    salve.core.mind.MindDatasetExporter.episodes(episodes).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return "Exporté " + (knowledge.split("\n").length - 1) + " elementos consolidados y " + episodes.size()
                    + " recuerdos citados en Android/data/" + app.getPackageName() + "/files/exports. "
                    + "Contienen datos personales: cópialos solo a tu PC.";
        } catch (Exception failure) {
            return "No pude exportar: " + failure.getClass().getSimpleName();
        }
    }

    // ── Memoria por significado ─────────────────────────────────────────────────

    /** Para {@link ConversationMemoryGrounding}: nunca bloquea un turno cargando el modelo de embeddings. */
    public ConversationMemoryGrounding.SemanticRecall semanticRecall() {
        return (query, limit) -> {
            EmbeddingsIndex index = embeddings;
            EmbeddingCalibration.Result c = calibration;
            if (index == null || c == null) { warmUpEmbeddings(); return Collections.emptyList(); }
            // Un modelo que no distingue el español solo aportaría ruido con forma de recuerdo.
            if (!c.passed) return Collections.emptyList();
            float[] q;
            synchronized (index) { q = index.embed(query); }
            List<Integer> ids = new ArrayList<>();
            for (MemoryVectorStore.Hit hit : vectors.nearest(q, limit, c.threshold)) ids.add(hit.id);
            return ids;
        };
    }

    /** Indexa en segundo plano los recuerdos nuevos que aún no tienen vector. */
    public void indexNewMemoriesAsync() {
        background.execute(() -> indexMissing(INDEX_BATCH, () -> false));
    }

    public void warmUpEmbeddings() {
        if (embeddings != null || !embedderLoading.compareAndSet(false, true)) return;
        background.execute(() -> {
            try { loadEmbeddings(); }
            finally { embedderLoading.set(false); }
        });
    }

    private EmbeddingsIndex loadEmbeddings() {
        EmbeddingsIndex index = embeddings;
        if (index != null) return index;
        synchronized (this) {
            // Un fallo de carga se registra una vez; no se reintenta en cada consulta.
            if (embeddings == null && calibration == null) {
                try {
                    EmbeddingsIndex created = new EmbeddingsIndex(app);
                    calibration = EmbeddingCalibration.run(text -> { synchronized (created) { return created.embed(text); } });
                    embeddings = created;
                    Log.i(TAG, calibration.describe());
                } catch (Throwable failure) {
                    calibration = EmbeddingCalibration.failed(failure.getClass().getSimpleName());
                    Log.w(TAG, "Embeddings no disponibles: " + failure.getClass().getSimpleName());
                }
            }
            return embeddings;
        }
    }

    private float[] embedOrThrow(String text) throws Exception {
        EmbeddingsIndex index = loadEmbeddings();
        if (index == null) throw new IllegalStateException("Embeddings no disponibles.");
        synchronized (index) { return index.embed(text); }
    }

    /** Recorre la tabla en orden de id y crea los vectores que falten. Devuelve cuántos se crearon. */
    int indexMissing(int max, BooleanSupplier stopped) {
        EmbeddingsIndex index = loadEmbeddings();
        EmbeddingCalibration.Result c = calibration;
        if (index == null || c == null || !c.passed) return 0;
        int created = 0;
        long after = 0;
        try {
            while (created < max && !stopped.getAsBoolean()) {
                List<RecuerdoEntity> page = memories.pageForCloud(after, 200);
                if (page == null || page.isEmpty()) break;
                for (RecuerdoEntity r : page) {
                    after = Math.max(after, r.id);
                    if (created >= max || stopped.getAsBoolean()) break;
                    if (vectors.contains(r.id) || r.frase == null || r.frase.trim().isEmpty()) continue;
                    if ("configuracion_sistema".equals(MemoryProvenance.kind(r))) continue;
                    float[] v;
                    synchronized (index) { v = index.embed(r.frase); }
                    vectors.put(r.id, v);
                    created++;
                }
            }
            if (vectors.isDirty()) vectors.save(vectorFile);
        } catch (Exception failure) {
            Log.w(TAG, "Indexación semántica interrumpida: " + failure.getClass().getSimpleName());
        }
        return created;
    }

    /** Borra vectores de recuerdos eliminados, para que lo olvidado no siga siendo recuperable. */
    public int pruneDeletedMemories() {
        try {
            HashSet<Integer> ids = new HashSet<>(memories.todosLosIds());
            int removed = vectors.retainOnly(ids);
            if (vectors.isDirty()) vectors.save(vectorFile);
            java.util.Set<String> gone = new HashSet<>();
            for (String source : journal.referencedSources()) {
                if (!source.startsWith("recuerdos:")) continue;
                try {
                    if (!ids.contains(Integer.parseInt(source.substring("recuerdos:".length())))) gone.add(source);
                } catch (NumberFormatException ignored) { }
            }
            return removed + journal.forgetSources(gone);
        } catch (Exception failure) {
            return 0;
        }
    }

    /** Tras un borrado: retira vectores y creencias que dependían de lo olvidado. */
    public void pruneDeletedMemoriesAsync() {
        background.execute(this::pruneDeletedMemories);
    }

    /** Llamar cuando se borra un recuerdo concreto. */
    public void forgetMemoryVector(int id) {
        background.execute(() -> {
            try { if (vectors.remove(id)) vectors.save(vectorFile); }
            catch (Exception ignored) { }
        });
    }

    // ── Sueño y reflexión (WorkManager) ─────────────────────────────────────────

    public String dream(BooleanSupplier stopped) {
        BooleanSupplier stop = combined(stopped);
        int pruned = pruneDeletedMemories();
        int indexed = indexMissing(400, stop);
        long cursor = journal.metrics().lastDreamMemoryId;
        List<RecuerdoEntity> page;
        try { page = memories.pageForCloud(Math.max(0, cursor), DreamCycle.MAX_EPISODES); }
        catch (RuntimeException unavailable) { return "No pude leer los recuerdos para soñar."; }
        List<Episode> episodes = new ArrayList<>();
        long maxId = cursor;
        if (page != null) for (RecuerdoEntity r : page) { addEpisode(episodes, r); maxId = Math.max(maxId, r.id); }
        if (episodes.isEmpty() && maxId > cursor) {
            // Solo había registros de configuración: avanzar el cursor para no quedarse atascada.
            journal.apply(new salve.core.mind.MindPatch("sueño").asDream(maxId));
            return "Sin experiencias personales nuevas que consolidar.";
        }
        String result = new DreamCycle(journal, local(SalveLLM.Role.SINTETIZADOR)).run(episodes, stop);
        Log.i(TAG, "Sueño: indexados=" + indexed + " podados=" + pruned);
        return result;
    }

    public String reflectIfDue(BooleanSupplier stopped) {
        BooleanSupplier stop = combined(stopped);
        long latest = -1;
        try {
            RecuerdoEntity last = memories.ultimoRecuerdo();
            if (last != null) latest = last.timestamp;
        } catch (RuntimeException ignored) { }
        ReflectionCycle cycle = new ReflectionCycle(journal, local(SalveLLM.Role.REFLEXION));
        if (!cycle.shouldRun(latest, System.currentTimeMillis())) return "Sin reflexión pendiente.";
        return cycle.run(recentEpisodes(8), stop);
    }

    // ── Predicción y sorpresa ───────────────────────────────────────────────────

    /** Compara la nueva entrada con la predicción anterior. Rápido: dos embeddings. */
    public void observeUserInput(String input) {
        background.execute(() -> {
            try { prediction.observe(input); }
            catch (RuntimeException ignored) { }
        });
    }

    /**
     * Predice la siguiente intervención tras una respuesta. Usa el modelo local, que atiende
     * un turno a la vez: se omite en voz, con poca memoria o si ya empezó otro turno.
     */
    public void predictNextAsync(String input, String reply, boolean voice) {
        if (voice || input == null || reply == null) return;
        EmbeddingCalibration.Result c = calibration;
        // Sin un modelo que mida bien el significado en español, la sorpresa no sería medible:
        // no se gasta una inferencia del modelo local en predecir.
        if (c == null || !c.passed) return;
        background.execute(() -> {
            try {
                if (GoalAutonomyRuntime.get(app).hasActiveConversation()) return;
                ModelMemoryPolicy.Snapshot ram = DeviceModelMemory.read(app);
                if (ram.known() && ram.low) return;
                prediction.predict(input, reply);
            } catch (RuntimeException ignored) { }
        });
    }

    // ── Episodios ───────────────────────────────────────────────────────────────

    private List<Episode> recentEpisodes(int max) {
        List<Episode> out = new ArrayList<>();
        try {
            List<RecuerdoEntity> rows = memories.recientes(max);
            if (rows != null) for (RecuerdoEntity r : rows) addEpisode(out, r);
        } catch (RuntimeException ignored) { }
        return out;
    }

    private static void addEpisode(List<Episode> out, RecuerdoEntity r) {
        if (r == null || r.frase == null || r.frase.trim().isEmpty()) return;
        String kind = MemoryProvenance.kind(r);
        if ("configuracion_sistema".equals(kind)) return;
        out.add(new Episode(Episode.memoryId(r.id), "[" + kind + "] " + r.frase, r.timestamp));
    }

    private BooleanSupplier combined(BooleanSupplier stopped) {
        return () -> (stopped != null && stopped.getAsBoolean()) || GoalAutonomyRuntime.get(app).hasActiveConversation();
    }
}
