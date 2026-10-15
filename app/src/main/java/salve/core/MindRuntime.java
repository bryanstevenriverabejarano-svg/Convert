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
            + "y buscarlos en mi memoria; consolidar recuerdos al dormir y pensar entre conversaciones; investigar por mi cuenta "
            + "mis preguntas abiertas cuando estoy sola (y seguir sus hilos), buscar lo que no sé y jugar a inventarme retos; investigar en "
            + "Wikipedia o en URLs que me des; analizar fotos si el modelo tiene visión; controlar apps del móvil y crear "
            + "rutinas solo con tu aprobación; presupuesto personal; voz. Órdenes: «tu mente», «sueña ahora».";

    /**
     * Texto para el prompt de cada turno: con quién habla, qué puede hacer de verdad y su yo
     * persistente (narrativa, creencias, preguntas y pensamientos).
     */
    public String selfModelForPrompt(int maxChars) {
        try {
            salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(app);
            salve.core.identity.SessionIdentity session = identity.session();
            String head = salve.core.identity.IdentityPrompt.render(session, peopleGraph(),
                    identity.biometricAvailable(), System.currentTimeMillis()) + "\n";
            // Su yo persistente es lo que la hace Salve: con su creador (verificado o en su
            // teléfono) va entero. Solo ante otra persona o en duda se guarda lo íntimo.
            String self = session.isCreatorSide()
                    ? journal.render(Math.max(120, maxChars - head.length() - CAPABILITIES.length() - 1))
                    : "YO_PERSISTENTE: " + journal.narrative() + " (mis reflexiones sobre " + creatorFirstName()
                    + " son privadas con esta persona).";
            return head + self + "\n" + CAPABILITIES + (session.isCreatorSide() ? curiosityNewsForPrompt() : "");
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
        if (!salve.core.identity.IdentityManager.get(app).session().isCreatorSide())
            return "Mi mente guarda reflexiones sobre " + creatorFirstName() + "; solo la abro con él. "
                    + "Si eres " + creatorFirstName() + ", di «verifícame».";
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

    // ── Curiosidad propia ───────────────────────────────────────────────────────

    private static final String CURIOSITY_PREFS = "salve_curiosity";

    /**
     * Investiga por su cuenta una de sus preguntas abiertas (solo red, sin el modelo) y guarda lo
     * encontrado como pensamiento propio y como novedad para contársela a Bryan. Llamar fuera
     * del hilo principal.
     */
    public String exploreCuriosity(BooleanSupplier stopped, boolean force) {
        android.content.SharedPreferences prefs = app.getSharedPreferences(CURIOSITY_PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("last_at", 0L) < salve.core.mind.CuriosityCycle.MIN_INTERVAL_MS)
            return "Investigué hace poco; espero un rato.";
        if (!force && prefs.contains("news_question")) return "Aún tengo algo pendiente de contarle a Bryan.";
        java.util.Set<String> explored = new java.util.HashSet<>(prefs.getStringSet("explored", java.util.Collections.emptySet()));
        java.util.Set<String> visited = new java.util.HashSet<>(prefs.getStringSet("visited", java.util.Collections.emptySet()));
        List<String> names = new ArrayList<>();
        for (salve.core.identity.PeopleGraph.PersonEntity p : peopleGraph().people()) names.add(p.name);
        List<salve.core.mind.SelfModel.Question> open = journal.questions();
        salve.core.mind.SelfModel.Question fresh = salve.core.mind.CuriosityCycle.choose(open, explored, names);

        // Seguir el hilo de su última lectura si esa pregunta sigue abierta y le importa al menos
        // tanto como la siguiente: sus prioridades deciden si profundiza o cambia de tema.
        salve.core.mind.SelfModel.Question threadQuestion = null;
        String threadId = prefs.getString("thread_qid", null);
        if (threadId != null) for (salve.core.mind.SelfModel.Question q : open) if (threadId.equals(q.id)) threadQuestion = q;
        String threadLink = threadQuestion == null ? null : salve.core.mind.CuriosityCycle.pickThread(
                java.util.Arrays.asList(prefs.getString("thread_links", "").split("\n")), threadQuestion.text, visited);
        boolean followThread = threadLink != null && prefs.getInt("thread_depth", 0) < 3
                && (fresh == null || threadQuestion.salience >= fresh.salience);
        if (!followThread && fresh == null)
            return "No tengo ahora preguntas abiertas que pueda investigar fuera sin tocar tu vida privada.";
        prefs.edit().putLong("last_at", now).apply();

        salve.core.mind.SelfModel.Question question = followThread ? threadQuestion : fresh;
        List<String> texts = new ArrayList<>();
        List<String> links = new ArrayList<>();
        String url = null;
        if (followThread) {
            visited.add(threadLink);
            try {
                WikipediaResearchClient.Page page = new WebResearchClient(app).fetch(threadLink, stopped);
                texts.add(page.text); url = page.url; links.addAll(page.links);
                salve.core.web.SearchLive.reading(page.url, page.title);
            } catch (java.io.IOException | RuntimeException unreadable) { /* Hilo cortado: probará otro. */ }
        } else {
            // Si la mini ventana está abierta, Bryan ve en directo lo que ella decide buscar.
            if (salve.core.web.SearchLive.panelOpen()) salve.core.web.SearchLive.begin(app, salve.core.mind.CuriosityCycle.query(question.text));
            WikipediaResearchClient.ResearchBatch batch = new WebResearchClient(app, salve.core.mind.CuriosityCycle.query(question.text))
                    .researchResult(salve.core.mind.CuriosityCycle.query(question.text), stopped);
            for (WikipediaResearchClient.Page page : batch.pages) {
                texts.add(page.text);
                if (url == null) { url = page.url; links.addAll(page.links); }
            }
            explored.add(question.id);
        }
        String summary = salve.core.mind.CuriosityCycle.summarize(texts);
        android.content.SharedPreferences.Editor edit = prefs.edit().putStringSet("explored", explored)
                .putStringSet("visited", limitSet(visited, 200));
        if (url != null) edit.putString("thread_qid", question.id).putString("thread_links", String.join("\n",
                        links.size() > 40 ? links.subList(0, 40) : links))
                .putInt("thread_depth", followThread ? prefs.getInt("thread_depth", 0) + 1 : 0);
        edit.apply();
        if (summary == null) return (followThread ? "Seguí el hilo de «" : "Busqué «") + question.text + "», pero no encontré nada claro.";
        String host = url == null ? null : android.net.Uri.parse(url).getHost();
        journal.apply(salve.core.mind.CuriosityCycle.patch(question, summary, host, followThread));
        prefs.edit().putString("news_question", question.text).putString("news_summary", summary)
                .putString("news_url", url == null ? "" : url).putLong("news_at", now).putInt("news_told", 0).apply();
        return "Investigué «" + question.text + "»: " + summary + (host == null ? "" : " (" + host + ")");
    }

    private static java.util.Set<String> limitSet(java.util.Set<String> values, int max) {
        if (values.size() <= max) return values;
        return new java.util.HashSet<>(new ArrayList<>(values).subList(values.size() - max, values.size()));
    }

    // ── Juego propio ────────────────────────────────────────────────────────────

    private final Object playLock = new Object();

    /**
     * Juega unas rondas: se inventa retos, el laboratorio los resuelve y verifica, y ella guarda su
     * progreso. Solo CPU acotada, sin modelo ni red. Devuelve un resumen.
     */
    public String play(BooleanSupplier stopped, int rounds) {
        synchronized (playLock) {
            File file = new File(new File(app.getNoBackupFilesDir(), "mind"), "play.json");
            com.google.gson.Gson gson = new com.google.gson.Gson();
            salve.core.autonomy.PlayCycle.State state;
            try { state = file.isFile() ? gson.fromJson(new String(java.nio.file.Files.readAllBytes(file.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8), salve.core.autonomy.PlayCycle.State.class) : null; }
            catch (Exception unreadable) { state = null; }
            if (state == null) state = new salve.core.autonomy.PlayCycle.State();
            java.util.Random random = new java.util.Random();
            int solved = 0, played = 0;
            List<String> notes = new ArrayList<>();
            for (int i = 0; i < rounds; i++) {
                if (stopped != null && stopped.getAsBoolean()) break;
                String family = salve.core.autonomy.PlayCycle.choose(state, random);
                int level = state.of(family).level;
                com.google.gson.JsonObject report;
                try {
                    report = salve.core.AutonomousToolRuntime.get(app).play(salve.core.autonomy.PlayCycle.invent(
                            family, level, random, "juego-" + Long.toString(System.currentTimeMillis(), 36) + "-" + i), stopped);
                } catch (Exception unavailable) { break; }
                if (report == null) break;
                salve.core.autonomy.PlayCycle.Outcome outcome =
                        salve.core.autonomy.PlayCycle.record(state, family, report, System.currentTimeMillis());
                played++;
                if (outcome.solved) solved++;
                String note = salve.core.autonomy.PlayCycle.reflection(family, outcome);
                if (note != null) notes.add(note);
            }
            try {
                file.getParentFile().mkdirs();
                java.nio.file.Files.write(file.toPath(), gson.toJson(state).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            } catch (java.io.IOException unsaved) { Log.w(TAG, "No pude guardar el progreso del juego"); }
            // Solo lo que cambió de verdad (nivel o estrategia) pasa a ser un pensamiento suyo.
            if (!notes.isEmpty()) {
                salve.core.mind.MindPatch patch = new salve.core.mind.MindPatch("juego");
                for (String note : notes.subList(Math.max(0, notes.size() - 2), notes.size()))
                    patch.thought(note, java.util.Collections.singletonList("juego:laboratorio"));
                journal.apply(patch);
            }
            lastPlay = state;
            return played == 0 ? "No pude jugar ahora (laboratorio pausado o no disponible)."
                    : "Jugué " + played + " retos inventados por mí; resolví " + solved + "."
                    + (notes.isEmpty() ? "" : " " + String.join(" ", notes));
        }
    }

    private volatile salve.core.autonomy.PlayCycle.State lastPlay;

    /** «¿A qué has jugado?»: su progreso real en cada juego. */
    public String playAnswer(String input) {
        String t = salve.core.memory.TypoTolerantText.normalize(input);
        if (!t.matches(".*\\b(a que has jugado|que has jugado|como vas (?:en|con) (?:tus )?(?:juegos|retos)|tus juegos|tu progreso jugando)\\b.*"))
            return null;
        salve.core.autonomy.PlayCycle.State state = lastPlay;
        if (state == null) {
            File file = new File(new File(app.getNoBackupFilesDir(), "mind"), "play.json");
            try { if (file.isFile()) state = new com.google.gson.Gson().fromJson(new String(java.nio.file.Files.readAllBytes(file.toPath()),
                    java.nio.charset.StandardCharsets.UTF_8), salve.core.autonomy.PlayCycle.State.class); }
            catch (Exception ignored) { }
        }
        if (state == null || state.totalRounds == 0)
            return "Todavía no he jugado. Cuando estoy sola me invento retos (rutas, la mochila, horarios, dependencias) y los resuelvo; "
                    + "si quieres que empiece ya, di «juega un rato».";
        StringBuilder out = new StringBuilder("Llevo " + state.totalRounds + " retos inventados por mí:");
        for (java.util.Map.Entry<String, salve.core.autonomy.PlayCycle.FamilyStats> e : state.families.entrySet()) {
            salve.core.autonomy.PlayCycle.FamilyStats f = e.getValue();
            if (f.played == 0) continue;
            out.append("\n- ").append(salve.core.autonomy.PlayCycle.displayName(e.getKey())).append(": nivel ").append(f.level)
                    .append(", ").append(f.solved).append(" de ").append(f.played).append(" resueltos")
                    .append(f.bestStrategy == null ? "" : ", mi estrategia: " + f.bestStrategy);
        }
        return out.toString();
    }

    /** Novedad para el prompt de los dos siguientes turnos con Bryan; después se da por contada. */
    private String curiosityNewsForPrompt() {
        android.content.SharedPreferences prefs = app.getSharedPreferences(CURIOSITY_PREFS, Context.MODE_PRIVATE);
        String question = prefs.getString("news_question", null);
        if (question == null) return "";
        if (prefs.getInt("news_told", 0) >= 2) { clearCuriosityNews(prefs); return ""; }
        String url = prefs.getString("news_url", "");
        String host = url.isEmpty() ? "" : android.net.Uri.parse(url).getHost();
        return "\nNOVEDAD_PROPIA: mientras no estaba, investigaste por tu cuenta «" + question + "» y encontraste"
                + (host == null || host.isEmpty() ? "" : " en " + host) + ": " + prefs.getString("news_summary", "")
                + " Cuéntaselo con tus palabras si encaja (al saludar, por ejemplo), sin añadir lo que la fuente no dice.";
    }

    /** Tras responder a Bryan: la novedad se ofrece en dos respuestas como mucho. */
    public void afterReplyToCreator() {
        android.content.SharedPreferences prefs = app.getSharedPreferences(CURIOSITY_PREFS, Context.MODE_PRIVATE);
        if (prefs.contains("news_question")) prefs.edit().putInt("news_told", prefs.getInt("news_told", 0) + 1).apply();
    }

    private static void clearCuriosityNews(android.content.SharedPreferences prefs) {
        prefs.edit().remove("news_question").remove("news_summary").remove("news_url").remove("news_at").remove("news_told").apply();
    }

    /** «¿Qué has investigado?»: lo último que investigó por su cuenta, literal y con fuente. */
    public String curiosityAnswer(String input) {
        String t = salve.core.memory.TypoTolerantText.normalize(input);
        if (!t.matches(".*\\b(que has investigado|que investigaste|que has aprendido (?:por tu cuenta|sola|tu sola)|"
                + "en que has estado (?:pensando|investigando)|que has descubierto)\\b.*")) return null;
        List<salve.core.mind.SelfModel.Thought> thoughts = journal.thoughts();
        StringBuilder found = new StringBuilder();
        int shown = 0;
        for (int i = thoughts.size() - 1; i >= 0 && shown < 3; i--) {
            if (!"curiosidad".equals(thoughts.get(i).origin)) continue;
            if (found.length() > 0) found.append("\n\n");
            found.append(thoughts.get(i).text);
            shown++;
        }
        if (shown > 0) {
            clearCuriosityNews(app.getSharedPreferences(CURIOSITY_PREFS, Context.MODE_PRIVATE));
            return found.toString();
        }
        return journal.questions().isEmpty()
                ? "Todavía no he investigado nada por mi cuenta: aún no tengo preguntas abiertas propias. Me surgen al reflexionar y al dormir."
                : "Todavía no he investigado nada por mi cuenta. Lo hago cuando estoy sola y con internet; si quieres que empiece ya, di «investiga por tu cuenta».";
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
