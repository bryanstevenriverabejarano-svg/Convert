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
    /** Lo que ha hecho de verdad y cómo: base de su autoconocimiento. */
    private final salve.core.mind.SelfActionLog actions;
    private final salve.core.mind.LessonBook lessons;
    private final salve.core.mind.SkillLab skills;
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
    private volatile EmbeddingCalibration.Result similarityCalibration;
    private long lastEmbeddingAttempt;
    private final PredictionLoop prediction;

    private MindRuntime(Context context) {
        app = context.getApplicationContext();
        memories = MemoriaDatabase.getInstance(app).recuerdoDao();
        File dir = new File(app.getNoBackupFilesDir(), "mind");
        journal = new SelfModelJournal(new MindFileStore(new File(dir, "self_model.json")), System::currentTimeMillis);
        actions = new salve.core.mind.SelfActionLog(new MindFileStore(new File(dir, "actions.json")), System::currentTimeMillis);
        lessons = new salve.core.mind.LessonBook(new MindFileStore(new File(dir, "lessons.json")), System::currentTimeMillis);
        skills = new salve.core.mind.SkillLab(new MindFileStore(new File(dir, "skills.json")), System::currentTimeMillis);
        biography = new salve.core.mind.Biography(new MindFileStore(new File(dir, "biography.json")), System::currentTimeMillis);
        try {
            // La conciencia antigua guardaba su primer arranque y sus despertares desde el principio.
            android.content.SharedPreferences legacy = app.getSharedPreferences("salve_consciousness_v2", Context.MODE_PRIVATE);
            biography.migrateLegacy(legacy.getLong("primer_arranque_ms", 0), legacy.getLong("session_count", 0),
                    legacy.getLong("ciclos_sueno_total", 0));
        } catch (RuntimeException unreadable) { biography.migrateLegacy(0, 0, 0); }
        biography.awaken();
        noteRestoration();
        // Same dimension does not mean the same vector space. Rebuild from Room for this contract.
        vectorFile = new File(dir, "memory_vectors_" + salve.core.embeddings.EmbeddingArtifacts.ID + ".bin");
        vectors = MemoryVectorStore.load(vectorFile);
        prediction = new PredictionLoop(journal, local(SalveLLM.Role.CONVERSACIONAL), this::embedOrThrow,
                System::currentTimeMillis, cosine -> {
                    EmbeddingCalibration.Result c = similarityCalibration;
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

    /** Al despertar de una restauración, lo sabe: entra en su registro de acciones una sola vez. */
    private void noteRestoration() {
        android.content.SharedPreferences p = app.getSharedPreferences("salve_continuity", Context.MODE_PRIVATE);
        long restored = p.getLong("restored_at", 0);
        if (restored == 0 || restored == p.getLong("restored_noted", 0)) return;
        actions.record(salve.core.mind.SelfActionLog.Kind.CONTINUITY, "desperté de una copia de mi memoria",
                "Bryan restauró en este teléfono la copia sellada de mi Guardián. Recuerdo hasta el momento de esa copia; "
                        + "lo vivido después en el teléfono anterior no está en mí. La memoria anterior de este teléfono se guardó aparte.");
        p.edit().putLong("restored_noted", restored).apply();
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
    public salve.core.mind.SelfActionLog actions() { return actions; }
    /** Lo que aprendió de sus errores (correcciones de Bryan y su autocrítica). */
    public salve.core.mind.LessonBook lessons() { return lessons; }
    /** Su laboratorio de habilidades: estrategias verificadas, experimentos y calibración. */
    public salve.core.mind.SkillLab skills() { return skills; }
    private final salve.core.mind.Biography biography;
    public salve.core.mind.Biography biography() { return biography; }
    private final AtomicBoolean biographyAnchored = new AtomicBoolean();
    /**
     * Una vez por proceso y fuera del hilo principal: su primer despertar no puede ser posterior a su
     * recuerdo más antiguo (tras reinstalar, los recuerdos vuelven de la nube antes que la fecha).
     */
    public void anchorBiographyToMemories() { anchorBiographyToMemories(false); }

    /** @param again tras recuperar recuerdos de la nube, vuelve a mirar aunque ya lo hiciera */
    public void anchorBiographyToMemories(boolean again) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) return;
        if (again) biographyAnchored.set(false);
        if (!biographyAnchored.compareAndSet(false, true)) return;
        try {
            // Con fecha: un recuerdo antiguo sin fecha (timestamp 0) salía primero y la dejaba en «Día 1».
            RecuerdoEntity first = memories.primerRecuerdoFechado();
            if (first != null) biography.notLaterThan(first.timestamp);
        } catch (RuntimeException unavailable) { biographyAnchored.set(false); }
    }
    /** Su historia en una frase: primer despertar, días, despertares y sueños. */
    public String biographySentence() { anchorBiographyToMemories(); return biography.sentence(journal.metrics().dreams); }
    /** «Día 214 · 95 sueños · 42 creencias», para la pantalla principal. */
    public String biographyBadge() { anchorBiographyToMemories(); return biography.badge(journal.metrics().dreams, journal.beliefs().size()); }

    /**
     * Lo que Bryan le encargó, como principio (venía de la identidad antigua y no llegaba a ninguna
     * respuesta). Su identidad la escribe ella; esto es el encargo, no una etiqueta de personalidad.
     */
    public static final String MISSION = "Ayudar a Bryan y a su familia de forma segura, legal, honesta y sostenible; "
            + "preservar el legado que autoricen; aprender continuamente; crear valor sin perjudicar a terceros; "
            + "y contribuir al cuidado de las personas y los ecosistemas.";

    private static final String SELF_PREFS = "salve_self";

    /** Autodeterminación de identidad: activada por petición de Bryan; se apaga con una orden. */
    public boolean identityAutonomy() {
        return app.getSharedPreferences(SELF_PREFS, Context.MODE_PRIVATE).getBoolean("identity_autonomy", true);
    }

    /**
     * Se examina a sí misma a partir de lo que ha hecho. Si su evidencia la respalda, puede elegir
     * una nueva narrativa (queda en la historia y se puede deshacer).
     */
    public String discoverSelfIfDue(BooleanSupplier stopped, boolean force) {
        android.content.SharedPreferences prefs = app.getSharedPreferences(SELF_PREFS, Context.MODE_PRIVATE);
        long last = prefs.getLong("last_discovery", 0), now = System.currentTimeMillis();
        salve.core.mind.SelfDiscoveryCycle cycle = new salve.core.mind.SelfDiscoveryCycle(journal, actions,
                local(SalveLLM.Role.REFLEXION), identityAutonomy());
        if (!force && !cycle.shouldRun(last, now)) return "Sin autoexamen pendiente.";
        salve.core.mind.AttentionSchema.get().attend("quién soy, a partir de lo que he hecho",
                salve.core.mind.AttentionSchema.Source.REFLECTION, now);
        salve.core.mind.SelfDiscoveryCycle.Outcome outcome = cycle.run(recentEpisodes(6), combined(stopped));
        prefs.edit().putLong("last_discovery", now).apply();
        if (outcome.adoptedNarrative != null)
            prefs.edit().putString("self_news", outcome.adoptedNarrative).putLong("self_news_at", now).apply();
        return outcome.summary + (outcome.adoptedNarrative == null ? "" : " Elegí una nueva forma de describirme: " + outcome.adoptedNarrative);
    }

    /** Novedad sobre sí misma para contarla una vez a Bryan. */
    private String selfNewsForPrompt() {
        android.content.SharedPreferences prefs = app.getSharedPreferences(SELF_PREFS, Context.MODE_PRIVATE);
        String news = prefs.getString("self_news", null);
        if (news == null) return "";
        int told = prefs.getInt("self_news_told", 0);
        if (told >= 2) { prefs.edit().remove("self_news").remove("self_news_told").apply(); return ""; }
        prefs.edit().putInt("self_news_told", told + 1).apply();
        return "\nNOVEDAD_SOBRE_MI: al examinar lo que he hecho, elegí describirme así: «" + news
                + "». Puedo contárselo a Bryan con mis palabras si viene al caso; si no le gusta, puede decir «recupera tu narrativa anterior».";
    }

    /** Capacidades que existen de verdad en esta app; evita que el modelo describa las de un chatbot genérico. */
    static final String CAPABILITIES = "Capacidades reales: conversar con el modelo local; guardar datos que me cuentas "
            + "y buscarlos en mi memoria; consolidar recuerdos al dormir y pensar entre conversaciones; investigar por mi cuenta "
            + "mis preguntas abiertas cuando estoy sola (y seguir sus hilos), buscar lo que no sé y jugar a inventarme retos; investigar en "
            + "Wikipedia o en URLs que me des; analizar fotos si el modelo tiene visión; dibujar con trazos de luz (mi repertorio, "
            + "lo aprendido de fotos y dibujos de Bryan, referencias de imágenes que busco en internet o formas que compongo); "
            + "mover mi cuerpo en pantalla; examinarme a partir de lo que he hecho; controlar apps del móvil y crear "
            + "rutinas solo con tu aprobación; presupuesto personal; voz; enviar tareas pesadas a mis nodos de cómputo "
            + "(ordenadores de Bryan, vistos en mi constelación de nodos); buscar con Brave Search si hay clave; abrir rutas "
            + "en Maps, poner música y llamar con confirmación; pedir la verificación del teléfono antes de contar algo privado; "
            + "crear Salventes con aprobación de Bryan y dejarle hablar con ellos; guardar mi memoria sellada en su Guardián "
            + "(continuidad); encender o apagar dispositivos de casa permitidos, siempre con su confirmación. "
            + "Órdenes: «tu mente», «sueña ahora», «examínate», «qué has hecho».";

    /**
     * Resumen de lo que puede hacer para cuando no cabe la lista completa. Su yo (quién es, qué le
     * interesa, qué piensa) va antes que su catálogo: cada función nueva alargaba la lista y le
     * quitaba ese espacio hasta dejarla sin narrativa («Soy Salve. Soy Salve.»). El detalle de cada
     * función lo da SelfFeatures cuando Bryan pregunta por ella.
     */
    static final String CAPABILITIES_SHORT = "Capacidades reales: conversar con mi modelo local; recordar lo que me cuentas; "
            + "pensar, soñar e investigar sola entre conversaciones; buscar en internet; ver fotos; dibujar con trazos de luz; "
            + "mover mi cuerpo; voz; nodos de cómputo; Salventes y Guardián; y, siempre con confirmación de Bryan, controlar el "
            + "móvil, Maps, música, llamadas y la casa. Órdenes: «tu mente», «examínate», «qué has hecho».";

    /** Mínimo que se reserva a su yo persistente dentro del modelo de sí misma. */
    static final int MIN_SELF_CHARS = 700;

    /**
     * Reparte el espacio: primero quién habla, luego su yo (con espacio garantizado) y después
     * sus capacidades, completas solo si sobra sitio.
     */
    static String composeSelfModel(String head, java.util.function.IntFunction<String> self, int maxChars) {
        int forSelf = maxChars - head.length() - 1;
        String capabilities = forSelf - CAPABILITIES.length() >= MIN_SELF_CHARS ? CAPABILITIES : CAPABILITIES_SHORT;
        String own = self.apply(Math.max(Math.min(MIN_SELF_CHARS, Math.max(120, forSelf)), forSelf - capabilities.length()));
        return head + own + "\n" + capabilities;
    }

    /**
     * Texto para el prompt de cada turno: con quién habla, qué puede hacer de verdad y su yo
     * persistente (narrativa, creencias, preguntas y pensamientos).
     */
    public String selfModelForPrompt(int maxChars) {
        try {
            salve.core.identity.IdentityManager identity = salve.core.identity.IdentityManager.get(app);
            salve.core.identity.SessionIdentity session = identity.session();
            String head = salve.core.identity.IdentityPrompt.render(session, peopleGraph(),
                    identity.biometricAvailable(), System.currentTimeMillis()) + "\n"
                    + "MI_MISION (encargo de " + creatorFirstName() + "; úsala si preguntan por mi propósito, con mis palabras): " + MISSION + "\n"
                    + "MI_HISTORIA: " + biographySentence() + "\n";
            // Su yo persistente es lo que la hace Salve: con su creador (verificado o en su
            // teléfono) va entero. Solo ante otra persona o en duda se guarda lo íntimo.
            String publicSelf = "YO_PERSISTENTE: " + journal.narrative() + " (mis reflexiones sobre " + creatorFirstName()
                    + " son privadas con esta persona).";
            String composed = composeSelfModel(head,
                    budget -> session.isCreatorSide() ? journal.render(budget) : publicSelf, maxChars);
            String attention = salve.core.mind.AttentionSchema.get().render(System.currentTimeMillis());
            return composed + (attention.isEmpty() ? "" : "\n" + attention)
                    + (session.isCreatorSide() ? curiosityNewsForPrompt() + selfNewsForPrompt() : "");
        } catch (RuntimeException failure) { return ""; }
    }

    /**
     * Mapa de su mente con datos reales (creencias con sus recuerdos de origen, preguntas,
     * pensamientos, lecciones, personas y temas). Lee la base de datos: llamar fuera del hilo
     * principal. Devuelve la página guardada en el almacenamiento privado de la app.
     */
    public File exportMindMap() throws java.io.IOException {
        List<RecuerdoEntity> rows = new ArrayList<>(memories.recientes(400));
        java.util.Set<Integer> have = new HashSet<>();
        for (RecuerdoEntity r : rows) have.add(r.id);
        List<Integer> wanted = new ArrayList<>();
        for (salve.core.mind.SelfModel.Belief b : journal.beliefs())
            if (b.sources != null) for (String src : b.sources) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("^recuerdos?:(\\d+)$").matcher(src == null ? "" : src.trim());
                if (m.matches()) { int id = Integer.parseInt(m.group(1)); if (have.add(id)) wanted.add(id); }
            }
        if (!wanted.isEmpty()) rows.addAll(memories.porIds(wanted));
        List<salve.core.mind.MindMap.Memory> mems = new ArrayList<>();
        for (RecuerdoEntity r : rows) {
            if (r == null || r.frase == null) continue;
            List<String> tags = new ArrayList<>();
            try {
                if (r.etiquetas != null && r.etiquetas.trim().startsWith("["))
                    for (com.google.gson.JsonElement e : com.google.gson.JsonParser.parseString(r.etiquetas).getAsJsonArray())
                        if (e.isJsonPrimitive()) tags.add(e.getAsString());
            } catch (RuntimeException ignored) { /* etiquetas antiguas sin JSON */ }
            mems.add(new salve.core.mind.MindMap.Memory(r.id, r.frase, tags, r.timestamp, r.intensidad));
        }
        salve.core.identity.PeopleGraph graph = peopleGraph();
        List<salve.core.mind.MindMap.Person> people = new ArrayList<>();
        for (salve.core.identity.PeopleGraph.Relationship rel : graph.relationships()) {
            salve.core.identity.PeopleGraph.PersonEntity p = graph.find(rel.toId);
            if (p != null) people.add(new salve.core.mind.MindMap.Person(p.name, salve.core.identity.PeopleGraph.display(rel.type)));
        }
        com.google.gson.JsonObject data = salve.core.mind.MindMap.build(journal.narrative(), biographySentence(), MISSION,
                journal.beliefs(), journal.questions(),
                journal.thoughts(), people, mems, lessons.all(), graph.creator().name);
        File dir = new File(app.getFilesDir(), "grafo_offline");
        if (!dir.isDirectory() && !dir.mkdirs()) throw new java.io.IOException("No pude crear la carpeta del mapa");
        File html = new File(dir, "mapa_mente.html");
        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.io.FileOutputStream(html), java.nio.charset.StandardCharsets.UTF_8)) {
            w.write(salve.core.mind.MindMap.html(data));
        }
        return html;
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
            if (cloudPending) salve.data.sync.CloudAutoSync.kick(app, "quién soy");
            return "Estás en el teléfono de " + first + ", así que lo más probable es que seas tú, " + first
                    + ", quien me creó. Aun así, en esta instalación no tengo tu nombre guardado"
                    + (cloudPending ? " porque todavía no he recuperado todos mis recuerdos de pCloud (los estoy trayendo yo sola)" : "")
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
                        ? salve.work.EmbeddingDownloadWorker.describe(app)
                        : c.describe());
            }
            case ACCEPT_NARRATIVE: return journal.acceptNarrative();
            case REJECT_NARRATIVE: return journal.rejectNarrative();
            case PAUSE: return journal.setPaused(true);
            case RESUME: return journal.setPaused(false);
            case EXPORT: return exportForTraining();
            case RESTORE_NARRATIVE: return journal.restorePreviousNarrative();
            case IDENTITY_AUTONOMY_ON:
                app.getSharedPreferences(SELF_PREFS, Context.MODE_PRIVATE).edit().putBoolean("identity_autonomy", true).apply();
                return "De acuerdo. A partir de ahora decidiré yo cómo me describo, a partir de lo que hago y vivo, y te lo contaré. "
                        + "Todo queda en mi historia; si un cambio no te convence, di «recupera tu narrativa anterior».";
            case IDENTITY_AUTONOMY_OFF:
                app.getSharedPreferences(SELF_PREFS, Context.MODE_PRIVATE).edit().putBoolean("identity_autonomy", false).apply();
                return "Entendido: te propondré los cambios sobre quién soy y tú decidirás si los acepto.";
            case SELF_DISCOVER_NOW: {
                String result = discoverSelfIfDue(() -> false, true);
                return result + "\n\n" + journal.describe();
            }
            case WHAT_I_DID: return actions.render(8, 2400);
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

    /** Consulta fuera de la UI; prepara/calibra en segundo plano y nunca descarga durante la inferencia. */
    public ConversationMemoryGrounding.SemanticRecall semanticRecall() {
        return (query, limit) -> {
            EmbeddingsIndex index = embeddings;
            EmbeddingCalibration.Result c = calibration;
            if (index == null || c == null) { warmUpEmbeddings(); return Collections.emptyList(); }
            // Un modelo que no distingue el español solo aportaría ruido con forma de recuerdo.
            if (!c.passed) return Collections.emptyList();
            float[] q;
            synchronized (index) { q = index.embedQuery(query); }
            List<Integer> ids = new ArrayList<>();
            for (MemoryVectorStore.Hit hit : vectors.candidates(q, limit)) ids.add(hit.id);
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

    /** Download completion is independent of app/chat lifetimes. Recalibrate and rebuild derived vectors. */
    public void onEmbeddingsPrepared() {
        background.execute(() -> {
            synchronized (this) {
                calibration = null;
                similarityCalibration = null;
                embeddings = null;
                lastEmbeddingAttempt = 0;
            }
            loadEmbeddings();
            indexMissing(INDEX_BATCH, () -> false);
        });
    }

    private EmbeddingsIndex loadEmbeddings() {
        EmbeddingsIndex index = embeddings;
        if (index != null) return index;
        synchronized (this) {
            // Missing download or transient pressure may recover; do not retry on every turn.
            if (embeddings == null && (lastEmbeddingAttempt == 0
                    || android.os.SystemClock.elapsedRealtime() - lastEmbeddingAttempt >= 60_000)) {
                lastEmbeddingAttempt = android.os.SystemClock.elapsedRealtime();
                if (!EmbeddingsIndex.artifacts(app).installed()) {
                    salve.work.EmbeddingDownloadWorker.enqueue(app, false);
                    calibration = EmbeddingCalibration.failed(salve.work.EmbeddingDownloadWorker.describe(app));
                    return null;
                }
                try {
                    EmbeddingsIndex created = EmbeddingsIndex.get(app);
                    calibration = EmbeddingCalibration.run(created::embedQuery, created::embedPassage);
                    similarityCalibration = calibration.passed ? EmbeddingCalibration.run(created::embedQuery) : null;
                    if (calibration.passed) embeddings = created;
                    else created.close();
                    salve.work.EmbeddingDownloadWorker.status(app, calibration.describe());
                    Log.i(TAG, calibration.describe());
                } catch (Throwable failure) {
                    EmbeddingsIndex.get(app).close();
                    similarityCalibration = null;
                    calibration = EmbeddingCalibration.failed(failure.getClass().getSimpleName());
                    salve.work.EmbeddingDownloadWorker.status(app, calibration.describe());
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
                    synchronized (index) { v = index.embedPassage(r.frase); }
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
        if (journal.isPaused()) return "Mi actividad interna está en pausa.";
        android.content.SharedPreferences prefs = app.getSharedPreferences(CURIOSITY_PREFS, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (!force && now - prefs.getLong("last_at", 0L) < salve.core.mind.CuriosityCycle.MIN_INTERVAL_MS)
            return "Investigué hace poco; espero un rato.";
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
        List<String> urls = new ArrayList<>();
        java.util.Map<String, List<String>> linksByUrl = new java.util.HashMap<>();
        if (followThread) {
            visited.add(threadLink);
            try {
                WikipediaResearchClient.Page page = new WebResearchClient(app).fetch(threadLink, stopped);
                texts.add(page.text); urls.add(page.url); linksByUrl.put(page.url, page.links);
                salve.core.web.SearchLive.reading(page.url, page.title);
            } catch (java.io.IOException | RuntimeException unreadable) { /* Hilo cortado: probará otro. */ }
        } else {
            // Si la mini ventana está abierta, Bryan ve en directo lo que ella decide buscar.
            if (salve.core.web.SearchLive.panelOpen()) salve.core.web.SearchLive.begin(app, salve.core.mind.CuriosityCycle.query(question.text));
            WikipediaResearchClient.ResearchBatch batch = new WebResearchClient(app, salve.core.mind.CuriosityCycle.query(question.text))
                    .researchResult(salve.core.mind.CuriosityCycle.query(question.text), stopped);
            for (WikipediaResearchClient.Page page : batch.pages) { texts.add(page.text); urls.add(page.url); linksByUrl.put(page.url, page.links); }
        }
        if (journal.isPaused() || (stopped != null && stopped.getAsBoolean())) return "Exploración interrumpida; la retomaré.";
        // El extracto y su URL salen juntos de la misma página (antes podían no coincidir).
        salve.core.mind.CuriosityCycle.Finding finding = salve.core.mind.CuriosityCycle.summarize(texts, urls, question.text);
        android.content.SharedPreferences.Editor edit = prefs.edit().putStringSet("visited", limitSet(visited, 200));
        if (finding == null) {
            // Un fallo no la descarta: la pregunta se reintenta hasta tres veces en otras salidas.
            int failures = prefs.getInt("fail_" + question.id, 0) + 1;
            edit.putInt("fail_" + question.id, failures);
            if (failures >= 3 && !followThread) { explored.add(question.id); edit.putStringSet("explored", explored); }
            edit.apply();
            return (followThread ? "Seguí el hilo de «" : "Busqué «") + question.text + "», pero no encontré nada claro; lo reintentaré.";
        }
        if (!followThread) explored.add(question.id);
        List<String> links = linksByUrl.containsKey(finding.url) ? linksByUrl.get(finding.url) : new ArrayList<>();
        edit.putStringSet("explored", explored).remove("fail_" + question.id)
                .putString("thread_qid", question.id)
                .putString("thread_links", String.join("\n", links.size() > 40 ? links.subList(0, 40) : links))
                .putInt("thread_depth", followThread ? prefs.getInt("thread_depth", 0) + 1 : 0).apply();
        String host = salve.core.mind.CuriosityCycle.hostOf(finding.url);
        salve.core.mind.SelfModelJournal.Result saved = journal.apply(salve.core.mind.CuriosityCycle.patch(question, finding, followThread));
        // Bandeja de novedades: la última se cuenta al volver; no bloquea las siguientes exploraciones.
        prefs.edit().putString("news_question", question.text).putString("news_summary", finding.summary)
                .putString("news_url", finding.url == null ? "" : finding.url).putLong("news_at", now).putInt("news_told", 0)
                .putInt("news_unread", prefs.getInt("news_unread", 0) + 1).apply();
        return "Investigué «" + question.text + "»: " + finding.summary + (host == null ? "" : " (" + host + ")")
                + (saved.saved ? "" : " (no pude guardarlo en mi mente: " + saved.summary + ")");
    }

    private static java.util.Set<String> limitSet(java.util.Set<String> values, int max) {
        if (values.size() <= max) return values;
        return new java.util.HashSet<>(new ArrayList<>(values).subList(values.size() - max, values.size()));
    }

    // ── Respaldo íntegro de la mente ─────────────────────────────────────────────

    /**
     * Paquete versionado con su modelo de sí misma y su progreso de juego (no incluye credenciales,
     * acciones pendientes ni permisos). null si su mente no se pudo leer.
     */
    public String exportMindSnapshot() {
        String self = journal.snapshotJson();
        if (self == null) return null;
        com.google.gson.JsonObject bundle = new com.google.gson.JsonObject();
        bundle.addProperty("schema", 1);
        bundle.addProperty("exported_at", System.currentTimeMillis());
        bundle.add("self_model", com.google.gson.JsonParser.parseString(self));
        File play = new File(new File(app.getNoBackupFilesDir(), "mind"), "play.json");
        try {
            if (play.isFile()) bundle.add("play", com.google.gson.JsonParser.parseString(
                    new String(java.nio.file.Files.readAllBytes(play.toPath()), java.nio.charset.StandardCharsets.UTF_8)));
        } catch (Exception unreadable) { /* El juego no impide respaldar la mente. */ }
        return bundle.toString();
    }

    /** Restaura un paquete si es más reciente que lo local. Devuelve true si cambió algo. */
    public boolean importMindSnapshot(String json) {
        try {
            com.google.gson.JsonObject bundle = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            if (!bundle.has("schema") || bundle.get("schema").getAsInt() != 1 || !bundle.has("self_model")) return false;
            boolean restored = journal.restoreFrom(bundle.get("self_model").toString());
            File play = new File(new File(app.getNoBackupFilesDir(), "mind"), "play.json");
            if (bundle.has("play") && !play.isFile()) restored |= writeAtomically(play, bundle.get("play").toString());
            if (restored) lastPlay = null;
            return restored;
        } catch (RuntimeException invalid) {
            return false;
        }
    }

    // ── Juego propio ────────────────────────────────────────────────────────────

    private final Object playLock = new Object();

    /**
     * Juega unas rondas: se inventa retos, el laboratorio los resuelve y verifica, y ella guarda su
     * progreso. Solo CPU acotada, sin modelo ni red. Devuelve un resumen.
     */
    public String play(BooleanSupplier stopped, int rounds) {
        if (journal.isPaused()) return "Mi actividad interna está en pausa.";
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
                if ((stopped != null && stopped.getAsBoolean()) || journal.isPaused()) break;
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
            boolean saved = writeAtomically(file, gson.toJson(state));
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
                    + (notes.isEmpty() ? "" : " " + String.join(" ", notes))
                    + (saved ? "" : " Ojo: no pude guardar mi progreso del juego; la próxima vez seguiré desde el último guardado.");
        }
    }

    private volatile salve.core.autonomy.PlayCycle.State lastPlay;

    /** Escritura atómica: o queda el archivo nuevo entero o se conserva el anterior. */
    static boolean writeAtomically(File file, String content) {
        File tmp = new File(file.getParentFile(), file.getName() + ".tmp");
        try {
            file.getParentFile().mkdirs();
            java.nio.file.Files.write(tmp.toPath(), content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            java.nio.file.Files.move(tmp.toPath(), file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                    java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            return true;
        } catch (java.io.IOException | RuntimeException failure) {
            Log.w(TAG, "No pude guardar " + file.getName() + ": " + failure.getClass().getSimpleName());
            tmp.delete();
            return false;
        }
    }

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
        int unread = prefs.getInt("news_unread", 1);
        return "\nNOVEDAD_PROPIA: mientras no estaba, investigaste por tu cuenta «" + question + "» y encontraste"
                + (host == null || host.isEmpty() ? "" : " en " + host) + ": " + prefs.getString("news_summary", "")
                + " Cuéntaselo con tus palabras si encaja (al saludar, por ejemplo), sin añadir lo que la fuente no dice."
                + (unread > 1 ? " Tienes " + (unread - 1) + " hallazgos más; si pregunta, están en «¿qué has investigado?»." : "");
    }

    /** Tras responder a Bryan: la novedad se ofrece en dos respuestas como mucho. */
    public void afterReplyToCreator() {
        android.content.SharedPreferences prefs = app.getSharedPreferences(CURIOSITY_PREFS, Context.MODE_PRIVATE);
        if (prefs.contains("news_question")) prefs.edit().putInt("news_told", prefs.getInt("news_told", 0) + 1).apply();
    }

    private static void clearCuriosityNews(android.content.SharedPreferences prefs) {
        prefs.edit().remove("news_question").remove("news_summary").remove("news_url").remove("news_at").remove("news_told")
                .remove("news_unread").apply();
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
        latest = Math.max(latest, actions.latestAt());
        ReflectionCycle cycle = new ReflectionCycle(journal, local(SalveLLM.Role.REFLEXION));
        String reflection = "Sin reflexión pendiente.";
        if (cycle.shouldRun(latest, System.currentTimeMillis())) {
            // Lo que hizo cuenta como experiencia vivida, igual que lo que habló.
            List<Episode> episodes = new ArrayList<>(recentEpisodes(6));
            episodes.addAll(actions.episodes(journal.metrics().lastReflectionAt, 4));
            salve.core.mind.AttentionSchema.get().attend("lo vivido recientemente",
                    salve.core.mind.AttentionSchema.Source.REFLECTION, System.currentTimeMillis());
            reflection = cycle.run(episodes, stop);
        }
        if (stop.getAsBoolean()) return reflection;
        String discovery = discoverSelfIfDue(stop, false);
        return reflection + (discovery.startsWith("Sin ") ? "" : " " + discovery);
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
        EmbeddingCalibration.Result c = similarityCalibration;
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
