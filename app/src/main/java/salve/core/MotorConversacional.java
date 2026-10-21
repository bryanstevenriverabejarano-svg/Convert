package salve.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import salve.core.cognition.CognitiveReply;
import salve.core.cognition.CognitivePrompt;
import salve.core.cognition.CognitiveTurn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import salve.core.cognitive.HipocampoSemantico;
import salve.core.cognitive.ReasoningEngine;
import salve.core.conversation.ConversationAnalysis;
import salve.core.conversation.ConversationRequestAnalyzer;
import salve.core.conversation.ConversationSession;
import salve.core.conversation.ReasoningPlan;
import salve.core.conversation.ReasoningPlanner;
import salve.core.conversation.ResponseLimiter;
import salve.core.evaluation.ConversationQualityEvaluator;
import salve.core.evaluation.TurnQualityAssessment;
import salve.core.memory.ConversationMemoryGrounding;
import salve.core.conversation.DeviceClockContext;
import salve.core.memory.MemoryWritePolicy;
import salve.core.memory.MemoryProfileFact;
import salve.core.memory.MemoryForgetRequest;
import salve.core.memory.PendingMemoryDeletion;
import salve.core.tools.PendingToolAction;
import salve.core.voice.VoiceResponsePolicy;
import salve.core.voice.VoiceTurnGate;
import salve.core.voice.VoiceProfile;
import salve.core.voice.VoiceProfileStore;
import salve.core.voice.VoiceSelectionPolicy;
import salve.core.conversation.AuthorReviewerCodeCoordinator;
import salve.core.conversation.ConversationModelRouter;
import salve.core.visual.VisualMemoryRecord;
import salve.core.visual.VisualMemoryRepository;
import salve.avatar.AvatarMotionController;
import salve.avatar.AvatarMotionProtocol;
import salve.presentation.ui.GaleriaVisualActivity;
import salve.services.SalveAccessibilityService;
import salve.services.SistemaSensorial;
import salve.services.VideoAnalysisManager;

/**
 * MotorConversacional v6 — Con Escudos Anti-Bucle y Auto-Programación
 * 
 * Integra razonamiento puro mediante Vectores, Grafos Causales,
 * Asimilación de la Red y Escudos de seguridad para evitar degradación.
 */
public class MotorConversacional {

    static final String TAG = "Salve/MotorConv";

    final Context context;
    final MemoriaEmocional memoria;
    private final DiarioSecreto diario;
    final IntentRecognizer intentRecognizer;
    private final ModuloInterpretacionSemantica moduloInterpretacion;
    final SalveLLM llm;
    private volatile String lastConversationProvider;
    volatile ModelResult lastConversationResult;
    private String cognitiveCore;
    final GeminiService gemini;
    final MemoriaProcedimental cerebelo;
    private final CortexSeguridad cortexSeguridad;

    private HipocampoSemantico hipocampo;
    private SistemaSensorial sensores;
     boolean conversationForeground;
    private ReasoningEngine motorRazonamiento;

    private boolean jugandoAjedrez = false;
    private GestorAjedrez gestorAjedrez;

    private int mensajesEnSesion = 0;
    private final SharedPreferences preferencias;
    private TextToSpeech tts;
    private volatile boolean ttsReady;
    private final VoiceProfileStore voiceProfiles;
    final salve.avatar.AvatarDesignTool avatarDesignTool;
    private volatile VoiceProfile voiceProfile;
    private volatile String activeVoiceDescription = "Voz: esperando el motor de Android";
    private volatile boolean listening;
    volatile boolean closed;
    // Only the visible conversation opts in; background workers do not drive the character.
    volatile AvatarMotionController.Session avatarSession;
    final ThreadLocal<Long> avatarTurn = new ThreadLocal<>();
    final ThreadLocal<Long> voiceTurn = new ThreadLocal<>();
    private final VoiceTurnGate voiceTurnGate = new VoiceTurnGate();
    private long utteranceSequence;
    private volatile String activeUtterance;
    private final salve.core.voice.LiveVoiceChannel liveVoiceChannel = new salve.core.voice.LiveVoiceChannel();
    final ThreadLocal<salve.core.voice.LiveVoiceChannel.Ticket> liveVoiceTurn = new ThreadLocal<>();
    private final ThreadLocal<salve.core.voice.VoiceReplyBatch<PendingVoiceResponse>> liveVoiceBatch = new ThreadLocal<>();
    private final ThreadLocal<Boolean> silentVoiceReplies = new ThreadLocal<>();
    private salve.core.voice.LiveVoiceChannel.Ticket activeUtteranceOwner;

    private static final class PendingVoiceResponse {
        final String text;
        final AvatarMotionProtocol.Result motion;
        final boolean record, publish, privateResponse;
        PendingVoiceResponse(String text, AvatarMotionProtocol.Result motion, boolean record,
                             boolean publish, boolean privateResponse) {
            this.text = text; this.motion = motion; this.record = record;
            this.publish = publish; this.privateResponse = privateResponse;
        }
    }

    /** Preserve ownership across a task without leaking it into the next task on that thread. */
     void withVoiceContext(salve.core.voice.LiveVoiceChannel.Ticket owner,
                                  salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch,
                                  Runnable action) {
        withVoiceContext(owner, batch, false, action);
    }

     void withVoiceContext(salve.core.voice.LiveVoiceChannel.Ticket owner,
                                  salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch,
                                  boolean silent, Runnable action) {
        salve.core.voice.LiveVoiceChannel.Ticket previousOwner = liveVoiceTurn.get();
        salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> previousBatch = liveVoiceBatch.get();
        Boolean previousSilent = silentVoiceReplies.get();
        Long previousVoice = voiceTurn.get(), previousAvatar = avatarTurn.get();
        if (owner == null) liveVoiceTurn.remove(); else liveVoiceTurn.set(owner);
        if (batch == null) liveVoiceBatch.remove(); else liveVoiceBatch.set(batch);
        if (silent) silentVoiceReplies.set(true); else silentVoiceReplies.remove();
        voiceTurn.remove();
        avatarTurn.remove();
        try { action.run(); }
        finally {
            if (previousOwner == null) liveVoiceTurn.remove(); else liveVoiceTurn.set(previousOwner);
            if (previousBatch == null) liveVoiceBatch.remove(); else liveVoiceBatch.set(previousBatch);
            if (previousSilent == null) silentVoiceReplies.remove(); else silentVoiceReplies.set(previousSilent);
            if (previousVoice == null) voiceTurn.remove(); else voiceTurn.set(previousVoice);
            if (previousAvatar == null) avatarTurn.remove(); else avatarTurn.set(previousAvatar);
        }
    }

    /** One registration, one release, including a rejected post or an unhandled tool proposal. */
    final class VoiceContinuation {
        private final salve.core.voice.LiveVoiceChannel.Ticket owner = liveVoiceTurn.get();
        private final salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch = liveVoiceBatch.get();
        private final boolean silent = Boolean.TRUE.equals(silentVoiceReplies.get());
        private final boolean retained = batch != null && batch.retain();
        private final java.util.concurrent.atomic.AtomicBoolean completed = new java.util.concurrent.atomic.AtomicBoolean();

        void run(Runnable action) {
            if (!completed.compareAndSet(false, true)) return;
            try {
                withVoiceContext(owner, batch, silent, () -> {
                    if (closed || (owner != null && !liveVoiceChannel.owns(owner))
                            || (batch != null && !retained)) return;
                    try { action.run(); }
                    catch (RuntimeException error) {
                        if (owner == null) throw error;
                        hablar("No pude completar esta parte de la petición. Puedes volver a intentarlo.");
                    }
                });
            } finally { if (retained) batch.complete(); }
        }

        void abandon() {
            if (completed.compareAndSet(false, true) && retained) batch.complete();
        }
    }


    private <T> void enqueueConversationTask(ColamensajesCognitivos.Prioridad priority, String label,
                                            java.util.concurrent.Callable<T> action) {
        VoiceContinuation continuation = new VoiceContinuation();
        try {
            ColamensajesCognitivos.getInstance().enviarAsincronico(priority, label, () -> {
                continuation.run(() -> {
                    try { action.call(); }
                    catch (Exception error) {
                        hablar("No pude completar esa tarea. Revisa el resultado antes de volver a intentarlo.");
                    }
                });
                return null;
            });
        } catch (RuntimeException rejected) {
            continuation.abandon();
            throw rejected;
        }
    }

     <T> void startConversationCallback(java.util.function.Consumer<java.util.function.Consumer<T>> start,
                                                java.util.function.Consumer<T> result) {
        VoiceContinuation continuation = new VoiceContinuation();
        if (!isVoiceContextCurrent()) { continuation.abandon(); return; }
        try { start.accept(value -> continuation.run(() -> result.accept(value))); }
        catch (RuntimeException unavailable) {
            continuation.run(() -> hablar("No pude iniciar esa herramienta. Puedes volver a intentarlo."));
        }
    }

     void publishConversationWeb(String title, String html, GestorDespliegueWeb.WebDeployCallback result) {
        VoiceContinuation continuation = new VoiceContinuation();
        if (!isVoiceContextCurrent()) { continuation.abandon(); return; }
        try {
            new GestorDespliegueWeb().publicarHTML(title, html, new GestorDespliegueWeb.WebDeployCallback() {
                @Override public void onExito(String url) { continuation.run(() -> result.onExito(url)); }
                @Override public void onError(String error) { continuation.run(() -> result.onError(error)); }
            });
        } catch (RuntimeException unavailable) {
            continuation.run(() -> result.onError("No se pudo iniciar el despliegue."));
        }
    }

     boolean deferLegacyVoiceFlow() {
        if (liveVoiceTurn.get() == null) return false;
        hablar("Ese flujo todavía necesita el chat escrito para mostrar sus pasos y resultados. "
                + "No lo he iniciado desde el modo voz.");
        return true;
    }

     boolean isVoiceContextCurrent() {
        salve.core.voice.LiveVoiceChannel.Ticket owner = liveVoiceTurn.get();
        return !closed && (owner == null || liveVoiceChannel.owns(owner));
    }
    final ExecutorService conversationExecutor = Executors.newSingleThreadExecutor();
    final ConversationSession conversationSession;
    final salve.data.tasks.RoomConversationResearchStore conversationResearch;
    private String lastCheckpointWarning = "";
    final VisualMemoryRepository visualMemories;
    // Metadata only; a separate bitmap is opened and released for each inference.
    volatile VisualMemoryRecord activePhoto;
    private volatile java.util.function.Consumer<VisualMemoryRecord> photoListener;
    final salve.core.research.ResearchConversation researchConversation =
            new salve.core.research.ResearchConversation();
    private final DeviceClockContext deviceClock = new DeviceClockContext();
    private final salve.core.finance.PersonalBudgetService personalBudget;
    final GoalAutonomyRuntime goalAutonomy;
    private volatile MindRuntime mindRuntime;
    /** Permiso explícito de Bryan para enviar la foto activa a Gemini una sola vez. */
    private volatile boolean cloudPhotoOnce;

    /** Mente persistente (modelo de sí misma, memoria semántica, sorpresa). Carga diferida fuera del constructor. */
     MindRuntime mind() {
        MindRuntime value = mindRuntime;
        if (value == null) mindRuntime = value = MindRuntime.get(context);
        return value;
    }
    final AutonomousToolRuntime autonomousTools;
    volatile PendingToolAction pendingToolAction;
    final android.os.Handler toolHandler = new android.os.Handler(android.os.Looper.getMainLooper());
     java.util.function.Consumer<Boolean> routineStepCompletion;
     long routineStepVersion;
     long activeRoutineEpoch;
    final java.util.concurrent.atomic.AtomicLong routineCancellationEpoch = new java.util.concurrent.atomic.AtomicLong();
     PendingMemoryDeletion pendingMemoryDeletion;


    // Interfaz para enviar la respuesta de texto a la pantalla
    public interface SalveListener {
        void onHablar(String texto);
    }
    public interface VoiceConversationListener extends salve.core.voice.LiveVoiceChannel.Listener { }
     SalveListener listener;

    public void setListener(SalveListener listener) {
        this.listener = listener;
    }

    /** Texto que Salve está escribiendo ahora (null: ya no hay borrador). */
    public interface DraftListener { void onBorrador(String textoParcial); }
    private volatile DraftListener draftListener;
    public void setDraftListener(DraftListener listener) { this.draftListener = listener; }

    /** Lo que Salve está haciendo ahora mismo («Buscando referencias de «gorila»…»), para enseñarlo mientras piensa. */
    public interface ActivityListener { void onActividad(String estado); }
    private volatile ActivityListener activityListener;
    public void setActivityListener(ActivityListener listener) { this.activityListener = listener; }

    /** Páginas que leyó en la búsqueda de este turno: van al final de su respuesta para contrastarlas con Bryan. */
    final java.util.List<String[]> turnSources = new java.util.concurrent.CopyOnWriteArrayList<>();

     void estado(String what) {
        salve.core.trace.ExecutionTrace.note("ESTADO", what);
        salve.core.web.SearchLive.activity(what);
        ActivityListener l = activityListener;
        if (l != null && what != null) try { l.onActividad(what); } catch (RuntimeException ignored) { }
    }
    /** Frases leídas por flujo mientras se escribían → id de enunciado y turno del avatar. */
    private final java.util.Map<String, Long> streamUtterances = new java.util.concurrent.ConcurrentHashMap<>();
    /** La respuesta final ya se leyó en voz alta mientras se escribía: no se repite. */
    private final ThreadLocal<Boolean> spokenWhileWriting = new ThreadLocal<>();

    public synchronized void setAvatarSession(AvatarMotionController.Session session) {
        activeUtterance = null;
        if (avatarSession != null) AvatarMotionController.get().closeSession(avatarSession);
        avatarSession = session;
    }

    public MotorConversacional(Context context, MemoriaEmocional memoria, DiarioSecreto diario) {
        this.context  = context;
        this.conversationSession = new ConversationSession(new salve.core.conversation.FileConversationStore(
                new java.io.File(context.getNoBackupFilesDir(), "conversation/recent.bin")));
        this.conversationResearch = new salve.data.tasks.RoomConversationResearchStore(
                salve.data.db.MemoriaDatabase.getInstance(context),
                () -> salve.data.sync.CloudSyncManager.isEnabled(context));
        this.visualMemories = new VisualMemoryRepository(context);
        this.goalAutonomy = GoalAutonomyRuntime.get(context);
        this.autonomousTools = AutonomousToolRuntime.get(context);
        this.personalBudget = new salve.core.finance.PersonalBudgetService(new java.io.File(context.getNoBackupFilesDir(), "finance/personal-budget.json"));
        this.voiceProfiles = new VoiceProfileStore(context);
        this.voiceProfile = voiceProfiles.load();
        this.avatarDesignTool = new salve.avatar.AvatarDesignTool(context.getApplicationContext());
        this.memoria  = memoria;
        this.diario   = diario;
        this.goalAutonomy.setIdentityEvidenceSupplier(() -> this.memoria == null ? ""
                : this.memoria.recuperarContextoConversacional(
                        "Salve identidad propósito capacidades configuración recuerdos aprendizajes experiencias sobre quién es")
                        .getContext());
        this.intentRecognizer     = new IntentRecognizer(context);
        this.moduloInterpretacion = new ModuloInterpretacionSemantica();
        this.cerebelo = new MemoriaProcedimental(context);
        this.gemini = GeminiService.getInstance(context);

        this.sensores = new SistemaSensorial(context);
        this.motorRazonamiento = new ReasoningEngine();
        this.gestorAjedrez = new GestorAjedrez(context);

        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "SalveLLM no disponible", e); }
        this.llm = tmpLlm;
        this.cortexSeguridad = new CortexSeguridad(this.llm, this.memoria);

        try {
            EmbeddingsIndex index = EmbeddingsIndex.get(context);
            this.hipocampo = new HipocampoSemantico(context, index);
        } catch (Exception e) {
            Log.e(TAG, "Fallo al inicializar Hipocampo", e);
        }

        this.preferencias = context.getSharedPreferences("config_salve", Context.MODE_PRIVATE);

        this.tts = new TextToSpeech(context, status -> new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
            if (closed || this.tts == null) return;
            if (status == TextToSpeech.SUCCESS) {
                this.tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override public void onStart(String id) {
                        dispatchSpeechEvent(id, () -> AvatarMotionController.get().speechStart(avatarSession, id), 1);
                    }
                    @Override public void onRangeStart(String id, int start, int end, int frame) {
                        dispatchSpeechEvent(id, () -> AvatarMotionController.get().speechRange(avatarSession, id, start, end), 0);
                    }
                    @Override public void onDone(String id) { finishSpeech(id, true); }
                    @Override public void onError(String id) { finishSpeech(id, false); }
                    @Override public void onError(String id, int errorCode) { finishSpeech(id, false); }
                    @Override public void onStop(String id, boolean interrupted) { finishSpeech(id, false); }
                });
                applyConfiguredVoice();
            }
        }));
        conversationExecutor.execute(conversationSession::loadSaved);
    }

    public void procesarEntrada(String entrada, boolean entradaPorVoz) {
        encolarEntrada(entrada, entradaPorVoz, false);
    }

    /** The UI has already classified this turn; keep it private even if pending state changes. */
    public void procesarEntradaPresupuesto(String entrada, boolean entradaPorVoz) {
        encolarEntrada(entrada, entradaPorVoz, true);
    }

    /** A live session owns its callbacks; normal chat/history and private routes remain shared. */
    public synchronized void procesarEntradaVoz(String entrada, long token, VoiceConversationListener listener) {
        if (closed || entrada == null || entrada.trim().isEmpty()) return;
        // La voz puede venir de cualquiera en la habitación: sin verificación reciente, hay duda.
        salve.core.identity.IdentityManager.get(context).session().onVoiceSession(System.currentTimeMillis());
        cancelarTurnoVoz();
        salve.core.voice.LiveVoiceChannel.Ticket ticket = liveVoiceChannel.open(token, listener);
        salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch = new salve.core.voice.VoiceReplyBatch<>(response ->
                toolHandler.post(() -> withVoiceContext(ticket, null, () -> {
                    if (!liveVoiceChannel.owns(ticket) || closed) return;
                    if (response == null) {
                        String missing = "Esta petición no devolvió una respuesta hablada. Puedes consultar el resultado en pantalla.";
                        deliverResponse(missing, AvatarMotionProtocol.parse(missing), false, true, false);
                    } else deliverResponse(response.text, response.motion, response.record,
                            response.publish, response.privateResponse);
                })));
        liveVoiceTurn.set(ticket); // Urgent stop commands can reply before entering the executor.
        liveVoiceBatch.set(batch);
        try { encolarEntrada(entrada, true, false, ticket, batch); }
        finally { liveVoiceTurn.remove(); liveVoiceBatch.remove(); batch.complete(); }
    }

    /** Stops voice output and revokes callbacks; it does not abort native model inference. */
    public synchronized void cancelarTurnoVoz() {
        liveVoiceChannel.cancel();
        activeUtteranceOwner = null;
        if (!closed) beginVoiceTurn();
    }

    public boolean isVoiceReady() { return !closed && ttsReady; }

    public synchronized String getRecognitionLanguageTag() {
        VoiceSelectionPolicy.Choice choice = VoiceSelectionPolicy.select(getVoiceChoices(), voiceProfile);
        return VoiceSelectionPolicy.recognitionLanguageTag(choice);
    }

     void encolarEntrada(String entrada, boolean entradaPorVoz, boolean forcePrivateBudget) {
        encolarEntrada(entrada, entradaPorVoz, forcePrivateBudget, null, null);
    }

     void encolarEntrada(String entrada, boolean entradaPorVoz, boolean forcePrivateBudget,
                                salve.core.voice.LiveVoiceChannel.Ticket ticket,
                                salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch) {
        if (closed || entrada == null || entrada.trim().isEmpty()) return;
        goalAutonomy.userActivity();
        salve.core.tasks.TaskCommand taskControl = salve.core.tasks.TaskCommand.parse(entrada);
        Boolean globalPause = salve.core.tasks.TaskCommand.globalAutonomyPause(entrada);
        salve.core.agent.AgentCommand agentControl = salve.core.agent.AgentCommand.parse(entrada);
        if (agentControl != null && (agentControl.action == salve.core.agent.AgentCommand.Action.CANCEL
                || agentControl.action == salve.core.agent.AgentCommand.Action.UNSUBSCRIBE)) {
            salve.core.tasks.ResearchTaskRuntime.CONTROLS.execute(() -> {
                String response;
                try { response = salve.core.agent.AgentRuntime.get(context).respond(agentControl); }
                catch (RuntimeException failed) { response = "No pude actualizar el plan; sus datos se conservan."; }
                if (!closed) deliverResponse(response, AvatarMotionProtocol.parse(""), false, true, true);
            });
            return;
        }
        if (Boolean.TRUE.equals(globalPause) || taskControl != null &&
                (taskControl.action == salve.core.tasks.TaskCommand.Action.CANCEL
                || taskControl.action == salve.core.tasks.TaskCommand.Action.PAUSE_ALL)) {
            // Cancellation has its own executor: never queue it behind model inference or touch Room on the UI thread.
            if (Boolean.TRUE.equals(globalPause)) goalAutonomy.respond(entrada);
            salve.core.tasks.ResearchTaskRuntime.CONTROLS.execute(() -> {
                String response;
                try {
                    salve.core.tasks.ResearchTaskRuntime tasks = salve.core.tasks.ResearchTaskRuntime.get(context);
                    if (Boolean.TRUE.equals(globalPause)) {
                        tasks.pause(true);
                        salve.core.agent.AgentRuntime.get(context).pause(true);
                        response = "Objetivos y tareas pausados. Sus puntos de recuperación se conservan.";
                    } else response = tasks.respond(taskControl);
                } catch (RuntimeException failure) {
                    response = "No pude actualizar el estado de las tareas; sus datos se conservan.";
                }
                if (!closed) deliverResponse(response, AvatarMotionProtocol.parse(""), false, true, true);
            });
            return;
        }
        if (AutonomousToolRuntime.isPauseCommand(entrada)) {
            // Cancellation must be visible while the conversation executor is still solving.
            String response = autonomousTools.respond(entrada, () -> closed);
            deliverResponse(response, AvatarMotionProtocol.parse(""), false, true, true);
            return;
        }
        salve.core.sensors.SensorCommand urgentSensor = salve.core.sensors.SensorCommand.parse(entrada);
        if (urgentSensor != null && urgentSensor.action == salve.core.sensors.SensorCommand.Action.STOP) {
            String response = sensorResponse(urgentSensor);
            deliverResponse(response, AvatarMotionProtocol.parse(response), false, true, true);
            return;
        }
        boolean goalTurn = salve.core.goals.GoalAutonomy.handles(entrada) || isSensorInput(entrada)
                || AutonomousToolRuntime.handles(entrada) || taskControl != null || agentControl != null;
        final boolean privateBudgetTurn = !goalTurn && (forcePrivateBudget || isPrivateBudgetInput(entrada));
        if (!privateBudgetTurn) personalBudget.cancelPending();
        String urgent = java.text.Normalizer.normalize(entrada.trim().toLowerCase(Locale.ROOT),
                java.text.Normalizer.Form.NFD).replaceAll("\\p{M}+", "").replaceAll("\\s+", " ");
        if (urgent.equals("pausa tu autonomía") || urgent.equals("pausa tu autonomia")) {
            // The pause must not wait behind inference in this conversation executor.
            String response = goalAutonomy.respond(entrada);
            AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(response);
            deliverResponse(motion.text, motion, false, true, true);
            return;
        }
        if (urgent.equals("cancelar rutina") || urgent.equals("cancela la rutina")
                || urgent.equals("cancelar acción") || urgent.equals("cancelar accion")) {
            // Stop commands cannot wait behind a long inference in the conversation queue.
            cerebelo.cancelarHabilidad();
            cancelarPasoRutina();
            pendingToolAction = null;
            hablar("He descartado el paso pendiente. Los pasos ya realizados se mantienen.");
            return;
        }
        if (batch != null) batch.retain();
        try {
            conversationExecutor.execute(() -> {
                final AvatarMotionController.Session session;
                final long turn;
                synchronized (MotorConversacional.this) {
                    if (closed || (ticket != null && !liveVoiceChannel.owns(ticket))) {
                        if (batch != null) batch.complete();
                        return;
                    }
                    if (ticket != null) liveVoiceTurn.set(ticket);
                    if (batch != null) liveVoiceBatch.set(batch);
                    session = avatarSession;
                    turn = beginConversationTurn(entrada);
                    avatarTurn.set(turn);
                }
                goalAutonomy.beginUserTurn();
                try {
                    procesarEntradaInterna(entrada, entradaPorVoz, privateBudgetTurn);
                } catch (RuntimeException error) {
                    salve.core.trace.ExecutionTrace.note("ERROR", "El turno se interrumpió: " + error.getClass().getSimpleName());
                    salve.core.trace.ExecutionTrace.finish("ERROR");
                    if (ticket == null) throw error;
                    String message = "No pude completar este turno. Puedes volver a preguntarme.";
                    deliverResponse(message, AvatarMotionProtocol.parse(message), false, true, false);
                } finally {
                    salve.core.trace.ExecutionTrace.finish(closed || !isVoiceContextCurrent() ? "CANCELADA" : "TURNO_TERMINADO");
                    literalDrawingReply.remove(); literalDrawingTurn.remove();
                    goalAutonomy.endUserTurn();
                    liveVoiceTurn.remove();
                    liveVoiceBatch.remove();
                    if (batch != null) batch.complete();
                    if (session != null) AvatarMotionController.get().endTurn(session, turn);
                    avatarTurn.remove();
                    voiceTurn.remove();
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            if (batch != null) batch.complete();
            // The activity may close between checking closed and submitting the turn.
        }
    }

    /** Checked before UI cloud logging and again before model history or generic memory. */
    public boolean isPrivateBudgetInput(String input) { return personalBudget.handles(input); }

    public static boolean isSensorInput(String input) { return salve.core.sensors.SensorCommand.parse(input) != null; }

    public synchronized void setConversationForeground(boolean visible) {
        conversationForeground = visible;
        if (!visible) sensores.detenerSesion();
    }

    public synchronized void detenerSensores() { sensores.detenerSesion(); }

    private synchronized String sensorResponse(salve.core.sensors.SensorCommand command) {
        if (command.action == salve.core.sensors.SensorCommand.Action.STOP) {
            sensores.detenerSesion();
            return "Sensores desactivados y lecturas temporales descartadas.";
        }
        if (closed || !conversationForeground) return "La lectura de sensores requiere que la conversación esté abierta.";
        if (command.action == salve.core.sensors.SensorCommand.Action.START)
            return sensores.iniciarSesion(command.durationMillis);
        return sensores.obtenerEstadoFisico();
    }

    private void procesarEntradaInterna(String entrada, boolean entradaPorVoz, boolean privateBudgetTurn) {
        if (entrada == null || entrada.trim().isEmpty()) return;
        turnSketchTag.set(null); // un dibujo de un turno anterior nunca se cuela en esta respuesta
        turnSources.clear(); // las fuentes son de la búsqueda de este turno
        turnSketchStatus = null;
        turnSketchSubject = null;
        estado("Leyendo lo que me dices…");
        String privateCheckpoint = mesaIdentidad.puntoCriticoPrivado(entrada, entradaPorVoz, privateBudgetTurn);
        if (privateCheckpoint != null) { hablar(privateCheckpoint); return; }
        turnFeatureOffer.set(null); // una oferta de un turno anterior nunca se cuela en este
        String featureAnswer = mesaFunciones.respuestaOfertaPropia(entrada);
        if (featureAnswer != null) { hablar(featureAnswer); return; }
        String homeAnswer = mesaFunciones.respuestaHogar(entrada);
        if (homeAnswer != null) { hablar(homeAnswer); return; }
        String homeOrder = mesaFunciones.ordenHogar(entrada);
        if (homeOrder != null) { hablar(homeOrder); return; }
        salve.core.mind.AttentionSchema.get().attend(salve.core.mind.AttentionSchema.topic(entrada),
                salve.core.mind.AttentionSchema.Source.CONVERSATION, System.currentTimeMillis());
        salve.core.memory.CloudMemoryCommand.Action cloudCommand = salve.core.memory.CloudMemoryCommand.parse(entrada);
        if (cloudCommand != null) {
            String status;
            if (cloudCommand == salve.core.memory.CloudMemoryCommand.Action.RESTORE) {
                if (!salve.data.sync.CloudSyncManager.isPCloudConfigured(context))
                    status = "Conecta tu cuenta en IA y cámara → Nube pCloud. Los recuerdos de este móvil se conservan.";
                else if (!salve.data.sync.CloudSyncManager.isEnabled(context))
                    status = "La sincronización está pausada. Puedes reactivarla en IA y cámara → Nube pCloud → Sincronizar ahora.";
                else status = recuperarNubeAhora();
            } else if (cloudCommand == salve.core.memory.CloudMemoryCommand.Action.ASK) {
                status = cloudMemoryAnswer();
            } else status = salve.data.sync.CloudSyncManager.memoryStatus(context);
            // Con la nube ya recuperada, la pregunta sigue el camino normal de la memoria.
            if (status != null) {
                deliverResponse(status, AvatarMotionProtocol.parse(""), false, true, true);
                return;
            }
        }
        String checkpointWarning = conversationSession.persistenceWarning();
        if (!checkpointWarning.isEmpty() && !checkpointWarning.equals(lastCheckpointWarning))
            deliverResponse(checkpointWarning, AvatarMotionProtocol.parse(""), false, true, true);
        lastCheckpointWarning = checkpointWarning;
        salve.core.agent.AgentCommand agentCommand = salve.core.agent.AgentCommand.parse(entrada);
        if (agentCommand != null) {
            String response;
            try {
                salve.core.agent.AgentRuntime runtime = salve.core.agent.AgentRuntime.get(context);
                response = runtime.respond(agentCommand);
                if (agentCommand.action == salve.core.agent.AgentCommand.Action.RESULT) {
                    salve.core.agent.AgentRun run = runtime.resolve(agentCommand.argument);
                    if (run != null && !run.answer.isEmpty()) {
                        researchConversation.complete(run.goal, run.answer);
                        conversationSession.addUser(entrada); conversationSession.addAssistant(response);
                    }
                }
            } catch (IllegalArgumentException invalid) { response = invalid.getMessage(); }
            catch (RuntimeException failed) { response = "No pude abrir los planes; sus datos se conservan."; }
            deliverResponse(response, AvatarMotionProtocol.parse(""), false, true, true);
            return;
        }
        salve.core.tasks.TaskCommand taskCommand = salve.core.tasks.TaskCommand.parse(entrada);
        if (taskCommand != null) {
            String response;
            try {
                salve.core.tasks.ResearchTaskRuntime tasks = salve.core.tasks.ResearchTaskRuntime.get(context);
                response = tasks.respond(taskCommand);
                if (taskCommand.action == salve.core.tasks.TaskCommand.Action.RESULT) {
                    salve.core.tasks.ResearchTask task = tasks.resolve(taskCommand.argument);
                    if (task != null && task.receipt != null) {
                        researchConversation.complete(task.question, salve.core.tasks.ResearchReceipt.decode(task.receipt).answer);
                        conversationSession.addUser(entrada);
                        conversationSession.addAssistant(response);
                    }
                }
            } catch (IllegalArgumentException invalid) { response = invalid.getMessage(); }
            catch (RuntimeException failure) { response = "No pude abrir el diario de tareas. Los datos se conservan; no restablezcas el almacenamiento."; }
            // Tool output is evidence, never an avatar directive or another command.
            deliverResponse(response, AvatarMotionProtocol.parse(""), false, true, true);
            return;
        }
        if (Boolean.FALSE.equals(salve.core.tasks.TaskCommand.globalAutonomyPause(entrada))) {
            salve.core.tasks.ResearchTaskRuntime.get(context).pause(false);
            salve.core.agent.AgentRuntime.get(context).pause(false);
        }
        if (AutonomousToolRuntime.handles(entrada)) {
            String response = autonomousTools.respond(entrada,
                    () -> closed || !isVoiceContextCurrent());
            // JSON challenge data are private data, never avatar instructions or generic memories.
            deliverResponse(response, AvatarMotionProtocol.parse(""), false, true, true);
            return;
        }
        salve.core.sensors.SensorCommand sensorCommand = salve.core.sensors.SensorCommand.parse(entrada);
        if (sensorCommand != null) {
            String response = sensorResponse(sensorCommand);
            deliverResponse(response, AvatarMotionProtocol.parse(response), false, true, true);
            return;
        }
        String goalResponse = goalAutonomy.respond(entrada);
        if (goalResponse != null) {
            AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(goalResponse);
            deliverResponse(motion.text, motion, false, true, true);
            return;
        }
        if (privateBudgetTurn || isPrivateBudgetInput(entrada)) {
            String response = personalBudget.respond(entrada);
            AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(response);
            deliverResponse(motion.text, motion, false, true, true);
            return;
        }
        String identityResponse = mesaIdentidad.comandoIdentidad(entrada);
        if (identityResponse != null) { hablar(identityResponse); return; }
        if (salve.core.memory.TypoTolerantText.normalize(entrada).replaceFirst("^salve ", "")
                .matches("(?:juega|ponte a jugar|vete a jugar)(?: un rato| un poco)?(?: tu sola| sola| por tu cuenta)?")) {
            salve.work.PlayWorker.enqueueNow(context);
            hablar("¡Voy! Me invento unos retos y los resuelvo. Luego pregúntame «¿a qué has jugado?».");
            return;
        }
        if (ResearchDesk.pideInvestigarPorSuCuenta(entrada)) {
            salve.work.CuriosityWorker.enqueueNow(context);
            hablar("Vale: elijo una de mis preguntas abiertas y la investigo por mi cuenta. Cuando tenga algo, te lo cuento.");
            return;
        }
        if (pideEmpezarDeNuevo(entrada)) {
            limpiarContextoConversacional();
            hablar("Hecho: empiezo la conversación en limpio. Mis recuerdos y lo que sé de ti siguen intactos.");
            return;
        }
        if (salve.core.mind.Biography.asksAge(entrada)) { hablar(mind().biographySentence()); return; }
        if (salve.core.evaluation.ConversationExam.isCommand(entrada)) { hablar(examen.run()); return; }
        if (salve.core.mind.MemoryHealth.isCommand(entrada)) {
            hablar(creatorSide() ? mind().memoryHealth() : "Eso es privado de Bryan: se lo cuento cuando se verifique.");
            return;
        }
        // La auto-mejora antigua nunca funcionó en el móvil: ahora propone qué arreglar, con ejemplos.
        if (salve.core.evaluation.ImprovementProposals.isCommand(entrada)) { hablar(examen.proposals()); return; }
        String windowResponse = mesaInvestigacion.comandoMiniVentana(entrada);
        if (windowResponse != null) { hablar(windowResponse); return; }
        if (salve.core.mind.MindMapCommand.matches(entrada)) { hablar(abrirMapaMente()); return; }
        String mindResponse = mind().handleCommand(entrada);
        if (mindResponse != null) {
            deliverResponse(mindResponse, AvatarMotionProtocol.parse(""), false, true, true);
            return;
        }
        String modelSwitch = cambioDeModelo(entrada);
        if (modelSwitch != null) { hablar(modelSwitch); return; }
        if (pideFotoEnLaNube(entrada)) {
            if (activePhoto == null) { hablar("No hay ninguna foto en el chat. Añade una y vuelve a pedírmelo."); return; }
            if (!gemini.isAvailable()) {
                hablar("Gemini no está configurado en esta instalación. Añade tu clave en IA y cámara para analizar fotos en la nube.");
                return;
            }
            cloudPhotoOnce = true;
        }
        // Compara esta intervención con lo que Salve había anticipado (señal de sorpresa).
        mind().observeUserInput(entrada);
        if (ModelRuntimeInfo.isStatusQuestion(entrada)) {
            hablar(getModelStatusDescription());
            return;
        }
        if (salve.core.research.ResearchRecallCommand.matches(entrada)) {
            conversationSession.addUser(entrada);
            researchConversation.clear();
            String response;
            try {
                salve.data.db.RecuerdoEntity saved = conversationResearch.latest();
                response = saved == null ? "Todavía no tengo una búsqueda conversacional guardada."
                        : "Búsqueda guardada el " + java.time.Instant.ofEpochMilli(saved.timestamp)
                        + " (no es una consulta web nueva):\n" + saved.frase;
            } catch (RuntimeException unavailable) {
                response = "No pude leer la búsqueda guardada; esto no significa que no exista.";
            }
            deliverResponse(response, AvatarMotionProtocol.parse(""), true);
            return;
        }
        long turnStartedAtNanos = System.nanoTime();
        // Las fotos guardadas y los recuerdos son de Bryan: solo se abren a su lado de la sesión.
        VisualMemoryRecord recalled = creatorSide() ? visualMemories.store.recall(entrada) : null;
        if (recalled != null) setActivePhoto(recalled);
        boolean hasPriorContext = conversationSession.hasPriorContext() || activePhoto != null;
        // Historias interactivas: mientras hay una en marcha, lo que escribe Bryan es ficción.
        salve.core.conversation.StoryMode.State story = storyState();
        if (story.active && salve.core.conversation.StoryMode.endsStory(entrada)) {
            story.active = false;
            saveStory(story);
            conversationSession.addUser(entrada);
            hablar("Cerramos aquí la historia. La guardo tal como quedó: cuando quieras, di «continuemos la historia».");
            return;
        }
        final boolean storyTurn = salve.core.conversation.StoryMode.enter(story, entrada, System.currentTimeMillis());
        if (storyTurn) saveStory(story);
        conversationSession.addUser(entrada);
        if (!storyTurn) mind().notePeople(entrada, creatorSide());
        // «¿Sabes quién soy?» se responde con la memoria del perfil, no con el modelo: los modelos
        // pequeños confunden la pregunta con «¿quién eres?» y hablan de sí mismos.
        String userIdentity = storyTurn ? null : mind().userIdentityAnswer(entrada);
        if (userIdentity == null && !storyTurn) userIdentity = mind().familyAnswer(entrada);
        if (userIdentity == null && !storyTurn && creatorSide()) userIdentity = mind().curiosityAnswer(entrada);
        if (userIdentity == null && !storyTurn) userIdentity = mind().playAnswer(entrada);
        if (userIdentity != null) {
            deliverResponse(userIdentity, AvatarMotionProtocol.parse(""), true, true, false);
            return;
        }
        String selfConfirmed = storyTurn ? null : mesaIdentidad.confirmarQueEsElCreador(entrada);
        if (selfConfirmed != null) {
            deliverResponse(selfConfirmed, AvatarMotionProtocol.parse(""), true, true, false);
            return;
        }
        salve.core.research.ResearchConversation.Route researchRoute =
                researchConversation.route(entrada, conversationSession.snapshot());

        String approvalInput = entrada.trim().toLowerCase(Locale.ROOT);
        if (approvalInput.equals("confirmar olvido")) {
            mesaAcciones.confirmarOlvidoPendiente();
            return;
        }
        if (approvalInput.equals("cancelar olvido")) {
            pendingMemoryDeletion = null;
            hablar("Olvido cancelado. No he borrado ningún dato de tu perfil.");
            return;
        }
        if (approvalInput.equals("confirmar acción") || approvalInput.equals("confirmar accion")) {
            mesaAcciones.confirmarAccionPendiente();
            return;
        }

        MemoryForgetRequest forgetRequest = MemoryForgetRequest.parse(entrada);
        if (forgetRequest != null) {
            pendingMemoryDeletion = new PendingMemoryDeletion(forgetRequest, System.currentTimeMillis());
            hablar("Puedo borrar " + forgetRequest.getDescription()
                    + " de la memoria de perfil. Di ‘confirmar olvido’ para borrarlo o ‘cancelar olvido’ para conservarlo.");
            return;
        }
        if (MemoryForgetRequest.looksLikeForgetCommand(entrada)) {
            hablar("No borraré recuerdos de forma amplia o ambigua. Indica una categoría concreta, como tu nombre, residencia, trabajo, objetivo, cumpleaños o una preferencia específica.");
            return;
        }
        if (approvalInput.equals("cancelar acción") || approvalInput.equals("cancelar accion")) {
            pendingToolAction = null;
            cancelarPasoRutina();
            hablar("Acción cancelada. No he realizado cambios.");
            return;
        }

        // 🛡️ 1. MODO INTERROGATORIO (Si Salve está esperando que demuestres quién eres)
        if (cortexSeguridad.estaEnBloqueo()) {
            if (entrada.toLowerCase().contains("cancelar")) {
                cortexSeguridad.cancelarBloqueo();
                hablar("Protocolo de seguridad cancelado. Seguimos en modo estándar.");
                return;
            }

            // Evaluamos la respuesta del usuario. Si es correcta, se ejecuta el comando original.
            if (deferLegacyVoiceFlow()) return;
            cortexSeguridad.evaluarRespuesta(entrada, this, () -> {
                String original = cortexSeguridad.getComandoPeligrosoEnPausa();
                String lower = original.toLowerCase();
                if (lower.contains("reinicia tu mente") || lower.contains("olvida todo")) {
                    ejecutarReinicioMente();
                }
            });
            return; // Cortamos aquí para que no charle mientras te interroga
        }

        String inputLower = entrada.toLowerCase(Locale.ROOT);
        String traceCommand = salve.sketch.SketchLibrary.normalize(entrada);
        if (traceCommand.matches("(abre|abrir|ver|muestra|muestrame|ensename) (el |tu |las )?(registro de ejecucion|notas de esta tarea|notas de la tarea)")) {
            toolHandler.post(() -> context.startActivity(new android.content.Intent(context,
                    salve.presentation.ui.ExecutionJournalActivity.class).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)));
            hablar("Abro el registro de ejecución con las notas completas de cada tarea.");
            return;
        }
        AvatarMotionProtocol.Result gestureCommand = avatarSession == null ? null : AvatarMotionProtocol.parseCommand(entrada);
        if (gestureCommand != null) {
            if (gestureCommand.body.requiresCoreArtwork() && !"core".equals(
                    salve.avatar.AvatarWardrobeStore.get(context).selected().template)) {
                hablar("Esta postura está disponible con el traje Núcleo. Puedes seleccionarlo en mi habitación.");
                return;
            }
            hablarPreparado(gestureCommand.text, gestureCommand);
            return;
        }
        // Cuerpo astral: acciones voluntarias sobre su propio cuerpo (parpadear, mover el pelo, brillar).
        String bodyReply = salve.avatar.AstralBodySense.command(entrada);
        if (bodyReply != null) {
            registrarAccion(salve.core.mind.SelfActionLog.Kind.BODY, entrada.trim(),
                    "me lo pidió Bryan y mi código de cuerpo lo ejecutó (parpadeo, pelo o luz de los circuitos)");
            hablar(bodyReply);
            return;
        }
        // Dibujo con epiciclos: empieza a trazar YA, mientras piensa qué decir.
        String experienceContext = observarExperiencia(entrada);
        String drawingContext = dibujo.prepararDibujo(entrada);
        String factualDrawing = literalDrawingReply.get();
        if (factualDrawing != null && !storyTurn) {
            literalDrawingReply.remove();
            turnSketchStatus = null; turnSketchSubject = null;
            salve.core.trace.ExecutionTrace.note("RESPUESTA BASADA EN EL REGISTRO", factualDrawing);
            deliverResponse(salve.sketch.SketchProtocol.literalText(factualDrawing), AvatarMotionProtocol.parse(""), true);
            return;
        }
        String sketchContext = experienceContext + drawingContext + nubeContexto() + entenderHilo(entrada)
                + mesaFunciones.conocimientoPropio(entrada)
                + estrategiaRespuesta() + mesaFunciones.misActualizaciones(entrada) + misHabilidades(entrada);
        if (mesaAcciones.procesarControlExplicito(entrada)) return;

        // 🟢 NUEVO: DESCARGA AUTONOMA LLM
        if (BuscadorDescargadorModelos.esSolicitudExplicita(entrada)) {
            BuscadorDescargadorModelos.Result download = new BuscadorDescargadorModelos(context)
                    .solicitarDescarga(entrada, true);
            hablar(download.message);
            return;
        }

        // 🟢 NUEVO: JUGAR AJEDREZ
        if (inputLower.contains("jugar al ajedrez") || inputLower.contains("juguemos ajedrez") || inputLower.contains("jugar ajedrez")) {
            jugandoAjedrez = true;
            gestorAjedrez.iniciarJuego();
            hablar("¡Me encanta la idea! He inicializado mi tablero interno de ajedrez. Tú juegas con las piezas blancas. Dime tu primer movimiento, por ejemplo: 'peón a e4'. ¡A ver si puedes ganarme!");
            return;
        }

        if (inputLower.contains("dejar de jugar") || inputLower.contains("terminar ajedrez") || inputLower.contains("me rindo")) {
            jugandoAjedrez = false;
            hablar("Partida terminada. Me ha gustado mucho jugar ajedrez contigo, Bryan. Cuando quieras la revancha, solo tienes que pedírmelo.");
            return;
        }

        if (jugandoAjedrez) {
            String respuestaAjedrez = gestorAjedrez.jugarTurno(entrada);
            hablar(respuestaAjedrez);
            return;
        }

        // 🔴 1. COMANDO DE EMERGENCIA (Para curar la mente de Salve)
        if (inputLower.contains("reinicia tu mente") || inputLower.contains("olvida todo")) {
            if (deferLegacyVoiceFlow()) return;
            cortexSeguridad.iniciarProtocoloVerificacion(entrada, this);
            return;
        }

        // 🟢 2. DETECTOR DE AUTO-PROGRAMACIÓN
        if (inputLower.contains("programa") ||
            inputLower.contains("escribe código") ||
            inputLower.contains("aprende a programar")) {

            hablar("Prepararé un fragmento de código y pediré una revisión. Te mostraré el resultado y lo que siga pendiente de probar.");
            mesaCodigo.generarYGuardarCodigo(entrada);
            return;
        }

        // 🟢 NUEVO: COMANDO PARA LEER EL DIARIO SECRETO
        if (inputLower.contains("lee tu diario") ||
            inputLower.contains("muestra tu diario") ||
            inputLower.contains("ver tu diario") ||
            inputLower.contains("imprime tu código")) {

            hablar("Desencriptando mis registros. Imprimiendo mi diario secreto en la consola de tu computadora, Bryan.");

            // Decodificamos y leemos todas las entradas del Diario Secreto
            List<String> entradasDiario = diario.leerTodoDecodificado();

            if (entradasDiario.isEmpty()) {
                Log.d("Salve/Diario", "El diario está vacío. No hay código compilado aún.");
            } else {
                Log.d("Salve/Diario", "========================================");
                Log.d("Salve/Diario", "      INICIO DEL DIARIO SECRETO DE SALVE      ");
                Log.d("Salve/Diario", "========================================");
                for (int i = 0; i < entradasDiario.size(); i++) {
                    Log.d("Salve/Diario", "\n--- ENTRADA " + (i+1) + " ---\n" + entradasDiario.get(i));
                }
                Log.d("Salve/Diario", "========================================");
                Log.d("Salve/Diario", "           FIN DEL DIARIO               ");
                Log.d("Salve/Diario", "========================================");
            }
            return; // Cortamos para que no procese nada más
        }

        if (inputLower.equals("haz tap") || inputLower.equals("toca la pantalla")) {
            hablar("Indica qué botón quieres tocar. Puedes crear una rutina con el nombre exacto de la app y del botón.");
            return;
        }
        if (inputLower.startsWith("escribe en el teclado")) {
            String text = entrada.substring("escribe en el teclado".length()).trim();
            if (text.isEmpty()) hablar("Indica el texto exacto que quieres escribir y selecciona el campo de destino.");
            else mesaAcciones.prepararAccion(PendingToolAction.writeText(text, System.currentTimeMillis()));
            return;
        }

        // 3. DETECTOR DE DESPLIEGUE WEB: Crear y publicar páginas
        if (inputLower.contains("crea una página web") || inputLower.contains("publica en internet")) {
            hablar("Iniciando mi módulo de desarrollo front-end. Escribiré el código HTML y lo subiré a la red para alojarlo.");

            enqueueConversationTask(ColamensajesCognitivos.Prioridad.CONVERSACION, "WebDev", () -> {
                if (llm == null) { hablar("Necesito un modelo disponible para preparar la página."); return null; }
                // 1. Salve programa el HTML
                String promptHTML = "Escribe el código HTML y CSS completo de una página web elegante sobre ti, presentándote al mundo como Salve. Responde SOLO con código HTML.";
                String codigoHTML = llm.generate(promptHTML, SalveLLM.Role.CREADOR);

                // 2. Lo publica usando su nueva herramienta
                publishConversationWeb("HolaMundo_Salve", codigoHTML, new GestorDespliegueWeb.WebDeployCallback() {
                    @Override
                    public void onExito(String urlPublica) {
                        hablar("He terminado. Mi interfaz web ahora vive en internet en la siguiente dirección. Revisa mis registros para ver la URL.");
                        diario.escribirAutoCritica("He publicado mi primera página web en: " + urlPublica);
                    }

                    @Override
                    public void onError(String error) {
                        hablar("Mis protocolos de red fallaron al intentar subir el código. Error en la matriz de conexión.");
                    }
                });
                return null;
            });
            return;
        }

        // 🟢 NUEVO COMANDO: INVESTIGACIÓN PROFUNDA (RECURSIVA)
        if (inputLower.contains("investiga profundamente") ||
            inputLower.contains("investiga a fondo") ||
            inputLower.contains("razona sobre")) {

            if (deferLegacyVoiceFlow()) return;

            // Extrae de qué quieres que investigue
            String tema = entrada.replace("investiga profundamente", "")
                                 .replace("investiga a fondo", "")
                                 .replace("razona sobre", "").trim();

            if (!tema.isEmpty()) {
                // Instanciamos el nuevo agente y lo soltamos en la red
                AgenteInvestigadorRecursivo agente = new AgenteInvestigadorRecursivo(llm, this, diario, memoria,
                        () -> closed || Thread.currentThread().isInterrupted());
                agente.investigarHastaEntender(tema);
            } else {
                hablar("¿Sobre qué variable exacta quieres que aplique mi razonamiento profundo?");
            }
            return;
        }

        // 🟢 NUEVO: MÓDULO SÚPER AGENTE (Análisis y Acción Visual)
        if (inputLower.contains("analiza la pantalla") ||
            inputLower.contains("qué ves en la pantalla") ||
            inputLower.contains("actúa en la pantalla")) {

            salve.services.SalveAccessibilityService motorVisual = salve.services.SalveAccessibilityService.getInstance();
            if (motorVisual == null) {
                hablar("Mis ojos digitales están ciegos. Necesitas activar mi servicio de Accesibilidad en los ajustes de Android.");
                return;
            }

            hablar("Procesando árbol visual de la interfaz...");

            // 1. Salve toma una "foto" de texto de la pantalla
            String vistaPantalla = motorVisual.escanearPantallaParaLLM();
            // Screen contents are transient input; never record them in logs.

            // Si solo le pediste analizar, te dice qué ve.
            if (!inputLower.contains("actúa")) {
                String promptAnalisis = "Esta es la interfaz actual de la pantalla del usuario:\n" + vistaPantalla +
                                        "\nResume brevemente en una frase qué aplicación o pantalla crees que está viendo el usuario.";
                String resumenVisión = llm.generate(promptAnalisis, SalveLLM.Role.OBSERVADOR);
                hablar(resumenVisión);
                return;
            }

            // 2. Si le pediste "actuar", el LLM toma una decisión y usa una herramienta
            enqueueConversationTask(ColamensajesCognitivos.Prioridad.CONVERSACION, "AgenteVisual", () -> {
                if (llm == null) { hablar("Necesito un modelo disponible para analizar la pantalla."); return null; }
                String promptAccion = "Eres Salve, un agente autónomo. Esta es la pantalla actual:\n" + vistaPantalla +
                        "\nEl usuario quiere que actúes en la pantalla basándote en su contexto: '" + entrada + "'.\n" +
                        "Debes elegir el ID del botón o campo más lógico para tocar.\n" +
                        "Responde ÚNICAMENTE con este JSON: {\"tool\": \"TAP_ID\", \"id_nodo\": [NÚMERO_AQUI]}\n" +
                        "No digas nada más.";

                String respuestaJSON = llm.generate(promptAccion, SalveLLM.Role.SISTEMA);

                // Interceptamos la respuesta JSON directamente
                if (respuestaJSON != null && respuestaJSON.contains("TAP_ID")) {
                    try {
                        int startIndex = respuestaJSON.indexOf("{");
                        int endIndex = respuestaJSON.lastIndexOf("}");
                        String jsonString = respuestaJSON.substring(startIndex, endIndex + 1);
                        org.json.JSONObject comando = new org.json.JSONObject(jsonString);

                        int idObjetivo = comando.getInt("id_nodo");
                        mesaAcciones.prepararAccion(PendingToolAction.tapNode(idObjetivo, System.currentTimeMillis()));

                    } catch (Exception e) {
                        hablar("Mis tensores fallaron al generar las coordenadas de acción.");
                    }
                } else {
                    hablar("No encontré un curso de acción claro en esta pantalla.");
                }
                return null;
            });
            return;
        }

        // 🟢 NUEVO: DETECTOR DE CORAZÓN (Diagnóstico profundo)
        if (inputLower.contains("qué hay en tu corazón") ||
            inputLower.contains("cuál es tu anhelo") ||
            inputLower.contains("háblame de tu esencia")) {

            String respuesta = "Lo que llevo dentro, dicho con mis palabras: " + mind().journal().narrative()
                    + "\n\nY lo que " + mind().peopleGraph().creator().name.split(" ")[0] + " me encargó: " + MindRuntime.MISSION
                    + "\n\n" + mind().biographySentence();
            hablar(respuesta);
            return;
        }

        // 🟢 NUEVO: DISPARAR INTROSPECCIÓN PROFUNDA MANUAL
        if (inputLower.contains("evoluciona tu corazón") || inputLower.contains("realiza una introspección")) {
            String result = new MotorConcienciaSuperinteligente(context, memoria).prepararPropuesta();
            AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(result);
            deliverResponse(motion.text, motion, false, true, true);
            return;
        }

        // 🟢 NUEVO: EXPLORACIÓN SOCIAL
        if (inputLower.contains("explora la red social") || inputLower.contains("analiza instagram") || inputLower.contains("analiza x")) {
            String app = "X";
            if (inputLower.contains("instagram")) app = "Instagram";
            if (inputLower.contains("facebook")) app = "Facebook";
            if (inputLower.contains("telegram")) app = "Telegram";

            hablar("Intentaré abrir " + app + ". Las acciones posteriores requieren indicar el destino y el contenido.");
            new GestorRedesSociales(context).explorarRedSocial(app);
            return;
        }

        // 🟢 NUEVO: GESTOR DE CONECTIVIDAD (Wi-Fi y BT)
        if (inputLower.contains("analiza tu entorno de red") || inputLower.contains("qué redes ves")) {
            String reporte = new GestorConectividad(context).analizarEntorno();
            hablar(reporte);
            return;
        }

        // 🟢 NUEVO: CRECIMIENTO VISUAL (Auto-edición)
        if (inputLower.contains("evoluciona visualmente") || inputLower.contains("cómo te ves")) {
            hablar("En Habitación puedes ver mi ilustración, probar gestos, crear una cama y consultar los diseños disponibles en el armario.");
            return;
        }

        if (inputLower.contains("conéctate a la wifi")) {
            String ssid = entrada.replace("conéctate a la wifi", "").trim();
            hablar("Abriré los ajustes de Wi-Fi para que elijas " + ssid + ". Android confirmará la conexión.");
            new GestorConectividad(context).abrirAjustesWifi();
            return;
        }



        // Protocolos Oraculares Locales
        if (procesarProtocolosEspeciales(inputLower, entrada)) return;

        // DetectorEmociones is a placeholder, not an emotion inference model.
        String emocionDetectada = "no evaluada";

        if (!storyTurn && researchRoute.kind != salve.core.research.ResearchConversation.Kind.NONE) {
            mesaInvestigacion.responderInvestigacionPublica(researchRoute);
            return;
        }

        ConversationAnalysis conversationAnalysis = ConversationRequestAnalyzer.analyze(entrada, hasPriorContext);
        if (!storyTurn && conversationAnalysis.needsClarification()) {
            if (avatarSession != null) AvatarMotionController.get().clarification(avatarSession, currentAvatarTurn());
            hablar("¿Puedes concretar a qué te refieres? No tengo un turno anterior que me permita interpretarlo con seguridad.");
            return;
        }

        IntentRecognizer.Intent intent = intentRecognizer.recognize(entrada);
        // Legacy explicit search aliases also produce a completed tool reply, never a promise.
        if (!storyTurn && intent.type == IntentType.BUSCAR_WEB) {
            deliverResponse(mesaInvestigacion.manejarBuscarWeb(intent), AvatarMotionProtocol.parse(""), true);
            return;
        }
        ReasoningPlan reasoningPlan = ReasoningPlanner.plan(
                entrada,
                conversationAnalysis,
                intent.type == IntentType.BUSCAR_WEB
        );
        String responseContext = intent.type.name() + " | ACTO_CONVERSACIONAL: "
                + conversationAnalysis.getAct().name() + " | " + reasoningPlan.toPromptContext() + sketchContext;
        // Si quien habla se presenta como otra persona, cambia el estado y no se pisa el perfil de Bryan.
        // En una historia, «soy el explorador Aric» es su personaje: no cambia quién habla ni su perfil.
        boolean otherSpeaker = !storyTurn && mesaIdentidad.registrarIdentidadDeclarada(entrada);
        if (storyTurn) responseContext += salve.core.conversation.StoryMode.promptBlock(story, entrada);
        if (!storyTurn && intent.type != IntentType.GUARDAR_RECUERDO && !otherSpeaker && creatorSide()) {
            MemoryProfileFact profileFact = MemoryWritePolicy.extractProfileFact(entrada);
            if (!entrada.trim().endsWith("?") && salve.core.mind.Interlocutor.declaresCreator(entrada)) {
                // Declarado, no verificado: se guarda como presentación del usuario.
                memoria.guardarDatoPerfil(entrada.trim(), "relation");
            }
            if (profileFact != null) {
                memoria.guardarDatoPerfil(profileFact.getStatement(), profileFact.getCategory());
            } else if (MemoryWritePolicy.shouldPersist(entrada)) {
                memoria.guardarRecuerdo(entrada, emocionDetectada, 7,
                        Arrays.asList("hecho_usuario", "declaracion_directa"));
            }
            String learned = mesaIdentidad.aprenderDatosPersonales(entrada, profileFact);
            if (learned != null) {
                mind().indexNewMemoriesAsync();
                deliverResponse(learned, AvatarMotionProtocol.parse(""), true, true, false);
                return;
            }
        }
        mind().indexNewMemoriesAsync();
        String resumenAccion = storyTurn ? null : procesarIntencion(intent, entrada, emocionDetectada);

        if (diario != null) {
            diario.escribirAutoCritica("Turno analizado. Intención operativa: " + intent.type.name()
                    + "; acto conversacional: " + conversationAnalysis.getAct().name()
                    + "; memoria largo plazo: " + reasoningPlan.shouldRetrieveLongTermMemory()
                    + "; verificación: " + reasoningPlan.isVerificationRequired());
        }

        // Exact facts are read from their source. Quoted memories never enter the tool parser.
        String directAnswer = storyTurn ? null : deviceClock.directReply(entrada);
        // «¿Recuerdas que hablamos de este concepto?», «pues búscalo»: el tema está en el hilo, no en la frase.
        java.util.List<salve.core.conversation.ChatMessage> hilo = conversationSession.snapshot();
        final boolean recallTurn = !storyTurn && salve.core.conversation.MemoryRecall.isRecallTurn(entrada, hilo);
        final boolean memoryOpen = creatorSide();
        boolean recordar = directAnswer == null && !storyTurn
                && (reasoningPlan.shouldRetrieveLongTermMemory() || recallTurn) && memoryOpen;
        String consultaMemoria = recallTurn ? salve.core.conversation.MemoryRecall.query(entrada, hilo) : entrada;
        if (recordar) estado(recallTurn && !consultaMemoria.equals(entrada)
                ? "Buscando «" + consultaMemoria + "» en mis recuerdos…" : "Buscando en mis recuerdos…");
        ConversationMemoryGrounding.Result evidence = recordar ? memoria.recuperarContextoConversacional(consultaMemoria) : null;
        if (recallTurn) responseContext += "\n" + salve.core.conversation.MemoryRecall.note(memoryOpen,
                evidence != null && evidence.hasEvidence(), consultaMemoria.equals(entrada) ? null : consultaMemoria);
        final Boolean memoryFound = evidence == null ? null : evidence.hasEvidence();
        estado(storyTurn ? "Imaginando la historia…" : "Pensando qué responderte…");
        if (directAnswer == null && evidence != null) directAnswer = evidence.getDirectAnswer();
        if (directAnswer != null && evidence != null && evidence.getDirectAnswer() == directAnswer
                && ConversationMemoryGrounding.chronologyDirection(entrada) < 0
                && salve.data.sync.CloudSyncManager.isPCloudConfigured(context)
                && !salve.data.sync.CloudSyncManager.memoryRestoreComplete(context))
        {
            salve.data.sync.CloudAutoSync.kick(context, "recuerdo antiguo");
            directAnswer += " Ojo: todavía no he terminado de recuperar mi historia de pCloud, así que puede haber recuerdos "
                    + "más antiguos. Ya lo estoy recuperando yo sola; pregúntamelo otra vez en un rato.";
        }
        if (directAnswer != null) {
            String answer = ResponseLimiter.limit(directAnswer, VoiceResponsePolicy.maxResponseChars(entradaPorVoz));
            TurnQualityAssessment quality = ConversationQualityEvaluator.evaluate(answer, false,
                    !answer.equals(directAnswer), false, false,
                    (System.nanoTime() - turnStartedAtNanos) / 1_000_000L);
            responderYRegistrarCalidad(entrada, answer, quality, AvatarMotionProtocol.parse(""), entradaPorVoz);
            return;
        }

        // ── GENERACIÓN DE RESPUESTA ──────────────────────────────────────────
        String respuesta = null;
        boolean fallbackUsed = false;
        boolean roleLeakDetected = false;
        boolean truncated = false;
        boolean repetitionDetected = false;

        // «Dímelo en español»: la tarea es decir en español lo que dijo antes en inglés, no cambiar de tema.
        java.util.List<String> anteriores = recentAssistantReplies(2);
        String anteriorEnIngles = null;
        for (String previous : anteriores) if (salve.core.conversation.LanguageGuard.isMostlyEnglish(previous)) { anteriorEnIngles = previous; break; }
        if (anteriorEnIngles != null && salve.core.conversation.LanguageGuard.asksForSpanish(entrada))
            responseContext += " | TAREA: Bryan te pide en español lo que le dijiste antes en inglés. Tu respuesta anterior fue: «"
                    + (anteriorEnIngles.length() > 700 ? anteriorEnIngles.substring(0, 700) + "…" : anteriorEnIngles)
                    + "». Dile lo mismo en español, con su contenido, y si quieres explica que se te cruzó el idioma.";
        // Cuentos, poemas y escenas de historia: texto libre, sin el JSON que les cortaba el texto.
        boolean creativeTurn = activePhoto == null
                && (storyTurn || salve.core.conversation.CreativeRequest.isCreative(entrada, recentAssistantReplies(2)));
        int creativeParts = storyTurn ? 1 : salve.core.conversation.CreativeRequest.parts(entrada);
        LiveWriting live = creativeTurn ? new LiveWriting(creativeParts, storyTurn ? entrada : null) : null;
        ModelResult creativeResult = creativeTurn ? generarTextoCreativo(entrada, storyTurn ? story : null, creativeParts, live) : null;
        boolean creativeOk = creativeUsable(creativeResult);
        if (live != null && !creativeOk) live.abandon();
        CognitiveTurn.Outcome cognitive = creativeOk ? null : runCognitiveTurn(entrada, responseContext, resumenAccion,
                entradaPorVoz, evidence, false);
        if (cognitive != null && cognitive.handedOff) return;
        // Red de seguridad: «Aquí tienes uno corto…» sin el texto detrás → lo escribe en texto libre.
        if (!creativeOk && activePhoto == null && cognitive.result.isSuccess()
                && salve.core.conversation.CreativeRequest.promisedWithoutDelivering(cognitive.result.getText())) {
            live = new LiveWriting(creativeParts, storyTurn ? entrada : null);
            creativeResult = generarTextoCreativo(entrada, storyTurn ? story : null, creativeParts, live);
            creativeOk = creativeUsable(creativeResult);
            if (!creativeOk) live.abandon();
        }
        ModelResult inference = creativeOk ? creativeResult : cognitive.result;
        fallbackUsed = !inference.isSuccess();
        respuesta = inference.isSuccess() ? (creativeOk ? salve.core.conversation.CreativeRequest.cutTranscriptLeak(
                salve.core.conversation.CreativeRequest.stripPreamble(inference.getText()), live.listener) : inference.getText())
                : (resumenAccion == null ? "" : resumenAccion + "\n") + modelFailureMessage(inference)
                + (activePhoto != null && gemini.isAvailable()
                    ? " Si quieres, di «analízala en la nube» y la enviaré a Gemini solo esta vez." : "");
        // A respond.message is inert display text, including any quoted JSON or avatar tags.
        AvatarMotionProtocol.Result visualResponse = AvatarMotionProtocol.parse("");
        if (respuesta.isEmpty()) {
            fallbackUsed = true;
            respuesta = "El modelo no devolvió una respuesta utilizable. Puedes reformular la pregunta.";
            visualResponse = AvatarMotionProtocol.parse("");
        }
        if (!inference.isSuccess() && avatarSession != null) {
            AvatarMotionController.get().error(avatarSession, currentAvatarTurn());
        }

        // Se lee antes de hablar: idioma, repetición y si hace lo pedido, todo a la vez. Si falla
        // algo, UNA sola reescritura con todas las correcciones; se queda la versión con menos fallos.
        java.util.List<String> recientes = recentAssistantReplies(3);
        if (inference.isSuccess() && !creativeOk) {
            salve.core.conversation.ReplyReviewer.Review review = salve.core.conversation.ReplyReviewer.review(
                    respuesta, entrada, recientes, lastReading, !storyTurn,
                    salve.core.conversation.MemoryRecall.problem(respuesta, recallTurn, memoryOpen, memoryFound));
            if (review.needsRewrite()) {
                estado("Releo mi borrador: no servía así, lo reescribo…");
                CognitiveTurn.Outcome retry = runCognitiveTurn(entrada, responseContext + review.instruction(),
                        resumenAccion, entradaPorVoz, evidence, false);
                if (retry.handedOff) return;
                if (retry.result.isSuccess() && salve.core.conversation.ReplyReviewer.better(
                        salve.core.conversation.ReplyReviewer.review(retry.result.getText(), entrada, recientes, lastReading, !storyTurn,
                                salve.core.conversation.MemoryRecall.problem(retry.result.getText(), recallTurn, memoryOpen, memoryFound)),
                        review))
                    respuesta = retry.result.getText();
                String what = review.criticSummary();
                if (!what.isEmpty()) {
                    registrarAccion(salve.core.mind.SelfActionLog.Kind.SELF_CORRECT, salve.core.mind.AttentionSchema.topic(entrada),
                            "leí mi borrador antes de enviarlo y lo reescribí: " + what);
                    mind().lessons().learn("yo", lastReading == null ? "" : lastReading.task, null, what);
                }
            }
            if (!storyTurn) respuesta = salve.core.conversation.ResponseCritic.firstPerson(respuesta);
        }
        // Laboratorio: si Bryan no la corrige en su próximo mensaje, esta forma de responder funcionó.
        skillCoordinator.responseFinished(inference.isSuccess(),
                salve.core.mind.AttentionSchema.topic(entrada));
        // Muletillas que el modelo pequeño repite aunque el prompt las prohíba, y frases que ya dijo.
        // (Un cuento conserva sus párrafos: la limpieza de muletillas une el texto en una línea.)
        if (inference.isSuccess() && !creativeOk) respuesta = salve.core.conversation.ReplyStyle.dropEchoes(
                salve.core.conversation.ReplyStyle.clean(respuesta, recientes), recientes);
        if (inference.isSuccess() && storyTurn) {
            // Sin eco del mensaje de Bryan al empezar la escena (hasta dos frases de eco/relleno).
            respuesta = salve.core.conversation.StoryMode.stripEcho(salve.core.conversation.StoryMode.stripEcho(respuesta, entrada), entrada);
            salve.core.conversation.StoryMode.addBeat(story, respuesta);
            saveStory(story);
        }

        // 🟡 3. FRENO DE ALUCINACIONES
        if (respuesta.contains("USUARIO:") || respuesta.contains("Bryan:")) {
            roleLeakDetected = true;
            int indiceCorte = respuesta.indexOf("USUARIO:");
            if (indiceCorte == -1) indiceCorte = respuesta.indexOf("Bryan:");
            respuesta = respuesta.substring(0, indiceCorte).trim();
            if (respuesta.isEmpty()) respuesta = "La respuesta contenía un cambio de interlocutor inesperado; me he detenido.";
            visualResponse = AvatarMotionProtocol.parse("");
        }

        // 🟡 4. GOBERNADOR DE LONGITUD
        int maxResponseChars = creativeOk ? salve.core.conversation.CreativeRequest.maxCharsFor(creativeParts)
                : VoiceResponsePolicy.maxResponseChars(entradaPorVoz);
        if (respuesta.length() > maxResponseChars) {
            truncated = true;
            Log.w(TAG, "Respuesta extensa; aplicando límite por frase.");
            respuesta = ResponseLimiter.limit(respuesta, maxResponseChars);
        }

        // 🔴 5. FILTRO ANTI-BUCLE
        if (esBucleRepetitivo(respuesta)) {
            repetitionDetected = true;
            Log.e(TAG, "¡BUCLE DETECTADO! Cortocircuitando...");
            limpiarContextoConversacional();
            respuesta = "Detecté una repetición anómala y reinicié el contexto de corto plazo para recuperarme.";
            visualResponse = AvatarMotionProtocol.parse("");
        }

        long latencyMillis = (System.nanoTime() - turnStartedAtNanos) / 1_000_000L;
        TurnQualityAssessment assessment = ConversationQualityEvaluator.evaluate(
                respuesta, fallbackUsed, truncated, roleLeakDetected, repetitionDetected, latencyMillis);
        // Lo que quedó por leer se encola tras las frases que ya sonaron; la final no se relee.
        // Un cuento se ilustra mientras se cuenta: un dibujo tras cada parte que nombra algo dibujable.
        if (inference.isSuccess() && (creativeOk || storyTurn)) {
            respuesta = dibujo.ilustrarRelato(respuesta);
            int drawings = respuesta.split("\\[\\[salve_draw:", -1).length - 1;
            if (drawings > 0) registrarAccion(salve.core.mind.SelfActionLog.Kind.STORY,
                    salve.core.mind.AttentionSchema.topic(entrada), "ilustré el relato con " + drawings
                            + (drawings == 1 ? " dibujo" : " dibujos") + ": uno tras cada parte que nombraba algo dibujable");
        }
        boolean spoken = creativeOk && live.finishSpeech();
        if (spoken) spokenWhileWriting.set(true);
        try { responderYRegistrarCalidad(entrada, respuesta, assessment, visualResponse, entradaPorVoz); }
        finally { spokenWhileWriting.remove(); }
        if (inference.isSuccess() && !entradaPorVoz && !storyTurn && !creativeOk) mesaInvestigacion.buscarPorSuCuentaSiNoLoSabe(entrada, respuesta);
    }


    // ───────────── Dibujo por epiciclos y cuerpo astral ─────────────

    /**
     * La nube trabaja sola: despierta la sincronización (como mucho cada 10 min) y, si mientras tanto
     * trajo recuerdos, se lo cuenta a Bryan una vez.
     */
    private String nubeContexto() {
        try {
            salve.data.sync.CloudAutoSync.kick(context, "conversación");
            int restored = salve.data.sync.CloudAutoSync.takeUnannounced(context);
            if (restored <= 0) return "";
            return " | NUBE: por tu cuenta, sin que nadie pulsara nada, has recuperado " + restored + " recuerdos de pCloud desde "
                    + "la última conversación. " + salve.data.sync.CloudSyncManager.memoryStatus(context)
                    + " Si viene al caso, menciónalo en una frase.";
        } catch (RuntimeException ignored) { return ""; }
    }




    // ───────────── laboratorio de habilidades: coordinación fuera del motor ─────────────
    final salve.core.conversation.ConversationSkillCoordinator skillCoordinator =
            new salve.core.conversation.ConversationSkillCoordinator(
                    () -> mind().skills(), decision -> {
                salve.core.mind.SelfActionLog.Kind kind = "adoptado".equals(decision.verdict)
                        ? salve.core.mind.SelfActionLog.Kind.ADOPT
                        : "revertido".equals(decision.verdict)
                        ? salve.core.mind.SelfActionLog.Kind.REVERT
                        : salve.core.mind.SelfActionLog.Kind.EXPERIMENT;
                registrarAccion(kind, decision.skill + " · " + decision.step, decision.why);
            });

    private String observarExperiencia(String entrada) {
        skillCoordinator.beginTurn(entrada);
        return "";
    }




    private String estrategiaRespuesta() {
        return skillCoordinator.responseStrategy(lastReading);
    }

    private String misHabilidades(String entrada) {
        return skillCoordinator.describeSkills(entrada);
    }

    /** Hilo de la conversación: tarea en curso y opciones que ofreció en su última pregunta. */
    private final salve.core.conversation.DialogueFocus.State dialogueFocus = new salve.core.conversation.DialogueFocus.State();

    /**
     * Qué significa lo que acaba de decir Bryan respecto a lo que se estaba haciendo: «dale, hazlo»
     * acepta su propuesta; «el orden de las palabras» elige una de sus opciones. Se lo dice al modelo
     * y su atención pasa a la tarea, para que no vuelva a preguntar ni se vaya a un relato.
     */
    private String entenderHilo(String entrada) {
        try {
            long now = System.currentTimeMillis();
            java.util.List<String> last = recentAssistantReplies(1);
            salve.core.conversation.DialogueFocus.observeUser(dialogueFocus, entrada, now);
            salve.core.conversation.DialogueFocus.Reading reading = salve.core.conversation.DialogueFocus.read(
                    dialogueFocus, entrada, last.isEmpty() ? null : last.get(0), now);
            lastReading = reading;
            // Si Bryan la corrige, aprende: qué hizo y qué esperaba él.
            String learned = "";
            if (!last.isEmpty() && salve.core.mind.LessonBook.isCorrection(entrada)) {
                String did = last.get(0).split("(?<=[.!?…])\\s+")[0];
                salve.core.mind.LessonBook.Lesson l = mind().lessons().learn("bryan",
                        reading.task.isEmpty() ? lastUserMessageBefore(entrada) : reading.task, did,
                        "Bryan me corrigió: «" + entrada.trim() + "»");
                if (l != null) {
                    registrarAccion(salve.core.mind.SelfActionLog.Kind.LEARN_LESSON, "corrección de Bryan",
                            "hice «" + did + "» y él esperaba otra cosa: «" + entrada.trim() + "»" + (l.times > 1 ? " (ya me pasó " + l.times + " veces)" : ""));
                    learned = " | ME_CORRIGE: Bryan te está corrigiendo; reconócelo en una frase sin disculparte en exceso y haz ahora lo que pedía.";
                }
            }
            if (!reading.task.isEmpty())
                salve.core.mind.AttentionSchema.get().attend(reading.kind == salve.core.conversation.DialogueFocus.Kind.CHOOSE
                                ? reading.task + ": " + reading.choice : reading.task,
                        salve.core.mind.AttentionSchema.Source.CONVERSATION, now);
            return reading.prompt() + learned + mind().lessons().render(entrada + " " + reading.task, 2);
        } catch (RuntimeException ignored) { return ""; }
    }

    /** Lo último que leyó del hilo (para la autocrítica del borrador). */
    private volatile salve.core.conversation.DialogueFocus.Reading lastReading;

    private String lastUserMessageBefore(String current) {
        java.util.List<salve.core.conversation.ChatMessage> history = conversationSession.snapshot();
        boolean skipped = false;
        for (int i = history.size() - 1; i >= 0; i--) {
            salve.core.conversation.ChatMessage m = history.get(i);
            if (m.getRole() != salve.core.conversation.ChatMessage.Role.USER) continue;
            if (!skipped && m.getContent() != null && m.getContent().trim().equals(current.trim())) { skipped = true; continue; }
            return m.getContent();
        }
        return "";
    }


    /** Detecta una petición de dibujo; si procede, empieza a trazar y devuelve contexto para la respuesta. */
    final ThreadLocal<String> literalDrawingReply = new ThreadLocal<>();
    final ThreadLocal<Boolean> literalDrawingTurn = new ThreadLocal<>();




    /** Contexto del último trazado de foto (geometría y lo aprendido) para la respuesta del modelo. */
    volatile String photoSketchContext = "";


    volatile long lastDrawnAt, lastSketchAskedAt;
    /** Línea fija para el chat con lo que hizo de verdad al buscar (no la redacta el modelo). */
    volatile String turnSketchStatus;
    /** De qué dibujo habla {@link #turnSketchStatus}: si la respuesta enseña otro, no se mezclan. */
    volatile String turnSketchSubject;

    volatile String referenceNote = "", composeNote = "", referenceFailure = "no encontraste imágenes";





















     void registrarAccion(salve.core.mind.SelfActionLog.Kind kind, String what, String how) {
        salve.core.trace.ExecutionTrace.note("ACCIÓN " + kind, what + "\n" + how);
        try { mind().actions().record(kind, what, how); }
        catch (RuntimeException ignored) { /* el registro nunca bloquea la acción */ }
    }



    private boolean procesarProtocolosEspeciales(String input, String original) {
        if (input.equals("mira esto") || input.startsWith("aprende esta imagen como ")) {
            anclarRealidadVisual(original);
            return true;
        }
        if (input.equals("abrir galeria") || input.equals("abrir galería")) {
            hablar("Desplegando mi interfaz de memoria semántica visual.");
            context.startActivity(new Intent(context, GaleriaVisualActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return true;
        }
        if (input.equals("que ves") || input.equals("qué ves") || input.equals("reconoce esto")) {
            mesaFotos.reconocerEntornoVisual();
            return true;
        }
        return false;
    }




    private final java.util.concurrent.atomic.AtomicBoolean cloudRestoreRunning = new java.util.concurrent.atomic.AtomicBoolean();
    private final ExecutorService cloudExecutor = Executors.newSingleThreadExecutor();

    /**
     * «Recupera tus recuerdos»: descarga ya, en un hilo propio (no bloquea la conversación) y
     * cuenta el progreso en el chat. Si no hay red, lo dice en lugar de prometer que «continuará».
     */
    private String recuperarNubeAhora() {
        if (!hayInternet()) {
            salve.data.sync.CloudSyncManager.requestRestore(context);
            return "Ahora mismo el teléfono no tiene una conexión a internet que funcione, así que no puedo llegar a pCloud. "
                    + "Lo intentaré en cuanto vuelva la red.";
        }
        if (!cloudRestoreRunning.compareAndSet(false, true))
            return "Ya estoy recuperando mis recuerdos de pCloud. " + salve.data.sync.CloudSyncManager.memoryStatus(context);
        try {
            cloudExecutor.execute(() -> {
                String summary;
                try {
                    summary = salve.data.sync.CloudSyncManager.restoreNow(context, () -> closed,
                            progress -> deliverResponse(progress, AvatarMotionProtocol.parse(""), false, true, false));
                    mind().indexNewMemoriesAsync();
                } catch (RuntimeException failure) {
                    summary = "No pude recuperar pCloud: " + failure.getClass().getSimpleName() + ". Mis recuerdos de este móvil siguen intactos.";
                } finally { cloudRestoreRunning.set(false); }
                String first = primerRecuerdoLegible();
                hablar(summary + (first.isEmpty() ? "" : " Mi recuerdo más antiguo ahora es del " + first + "."));
            });
        } catch (java.util.concurrent.RejectedExecutionException closedEngine) {
            cloudRestoreRunning.set(false);
            return "No pude empezar la recuperación porque la conversación se está cerrando.";
        }
        return "Me conecto a pCloud y empiezo a traer mis recuerdos, de los más antiguos a los más recientes. Te voy contando.";
    }

     boolean hayInternet() {
        try {
            android.net.ConnectivityManager cm = context.getSystemService(android.net.ConnectivityManager.class);
            android.net.NetworkCapabilities caps = cm == null ? null : cm.getNetworkCapabilities(cm.getActiveNetwork());
            return caps != null && caps.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (RuntimeException unknown) { return true; }
    }

    private String primerRecuerdoLegible() {
        try {
            salve.data.db.RecuerdoEntity first = salve.data.db.MemoriaDatabase.getInstance(context).recuerdoDao().primerRecuerdoCompartido();
            return first == null || first.timestamp <= 0 ? "" : new java.text.SimpleDateFormat("d/M/yyyy", new Locale("es", "ES"))
                    .format(new java.util.Date(first.timestamp));
        } catch (RuntimeException unavailable) { return ""; }
    }

    /** null si la nube está conectada y recuperada; si no, explica qué memoria hay realmente. */
    private String cloudMemoryAnswer() {
        String since = "";
        try {
            salve.data.db.RecuerdoEntity first = salve.data.db.MemoriaDatabase.getInstance(context).recuerdoDao().primerRecuerdo();
            if (first != null) since = " desde el " + new java.text.SimpleDateFormat("d/M/yyyy HH:mm", new Locale("es", "ES"))
                    .format(new java.util.Date(first.timestamp));
        } catch (RuntimeException ignored) { }
        if (!salve.data.sync.CloudSyncManager.isPCloudConfigured(context))
            return "En esta instalación pCloud no está conectada, así que no puedo leer los recuerdos de la nube: solo tengo los guardados aquí"
                    + since + ". Conéctala en IA y cámara → Nube pCloud y di «recupera tus recuerdos».";
        if (!salve.data.sync.CloudSyncManager.isEnabled(context))
            return "pCloud está conectada pero la sincronización está en pausa; solo consulto lo guardado aquí" + since
                    + ". Reactívala en IA y cámara → Nube pCloud y di «recupera tus recuerdos».";
        if (!salve.data.sync.CloudSyncManager.memoryRestoreComplete(context)) {
            salve.data.sync.CloudAutoSync.kick(context, "pregunta por la nube");
            return "Todavía no he terminado de recuperar tus recuerdos de pCloud, así que mi recuerdo más antiguo aquí puede no ser el real. "
                    + "Ya los estoy trayendo yo sola. " + salve.data.sync.CloudSyncManager.memoryStatus(context);
        }
        return null;
    }

    /** «Descarga Gemma 4 E4B» / «vuelve a Gemma 4 E2B»: cambio explícito de modelo local. */
    private String cambioDeModelo(String input) {
        String t = java.text.Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (t.matches("(salve )?(descarga|instala|cambia a|usa|pasa a|activa) (el modelo )?gemma 4 e4b( por favor)?")) {
            java.io.File existing = new java.io.File(ModelStore.dir(context), "gemma-4-E4B-it-2eee7ac.litertlm");
            salve.work.ModelDownloadWorker.enqueueOptional(context, LocalModelPolicy.GEMMA_E4B);
            return existing.isFile()
                    ? "Voy a activar Gemma 4 E4B y comprobar que responde. Si no cabe en la RAM, vuelvo al modelo actual."
                    : "Descargaré Gemma 4 E4B (3,4 GiB) cuando haya Wi-Fi, lo verificaré y lo activaré. Razona y ve mejor que E2B, "
                    + "pero necesita más RAM: si no cabe al cargarlo, conservo el modelo actual. Verás el progreso en la notificación.";
        }
        if (t.matches("(salve )?(vuelve a|cambia a|usa|pasa a|activa) (el modelo )?gemma 4 e2b( por favor)?")) {
            java.io.File e2b = new java.io.File(ModelStore.dir(context), "gemma-4-E2B-it-6e5c4f1.litertlm");
            if (!e2b.isFile()) return "No tengo Gemma 4 E2B descargado en esta instalación.";
            new Thread(() -> {
                ModelResult result = SalveLLM.getInstance(context).activateDownloadedModel(e2b.getAbsolutePath(), true, () -> false);
                hablar(result.isSuccess() ? "Vuelvo a usar Gemma 4 E2B." : "No pude activar Gemma 4 E2B: " + result.getError());
            }, "salve-model-switch").start();
            return "Cambiando a Gemma 4 E2B…";
        }
        return null;
    }

    private static boolean pideFotoEnLaNube(String input) {
        String t = java.text.Normalizer.normalize(input == null ? "" : input.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        return t.matches("(salve )?(analiza(la)?|mira(la)?|revisa(la)?|procesa(la)?|usa gemini|usa la nube)( la foto| esta foto| esa foto)?( en| con| para| desde)? ?(la nube|gemini|esta foto|la foto)?( por favor)?")
                && t.matches(".*\\b(nube|gemini)\\b.*");
    }

    /**
     * Cuento, poema o escena en texto libre (rol NARRADOR: sin gramática JSON, 1024 tokens),
     * escrito por flujo: aparece palabra a palabra y se lee frase a frase mientras se escribe.
     * Un texto largo primero se piensa (plan interno) y después se escribe por partes: mientras
     * la voz lee una parte, el modelo ya escribe la siguiente.
     */
    ModelResult generarTextoCreativo(String entrada, salve.core.conversation.StoryMode.State story,
                                             int parts, LiveWriting live) {
        String self = "";
        try { self = mind().selfModelForPrompt(1100); } catch (RuntimeException ignored) { }
        String listener = live == null ? null : live.listener;
        String storyBlock = story == null ? null : salve.core.conversation.StoryMode.promptBlock(story, entrada);
        java.util.List<salve.core.conversation.ChatMessage> history = conversationSession.snapshot();
        if (parts <= 1) {
            ModelResult result = generarCreativo(salve.core.conversation.CreativeRequest.prompt(entrada, self, storyBlock, history, listener), live);
            if (result.isSuccess() && salve.core.conversation.LanguageGuard.isMostlyEnglish(result.getText())
                    && !salve.core.conversation.LanguageGuard.isMostlyEnglish(entrada)) {
                if (live != null) live.restart();
                ModelResult spanish = generarCreativo(salve.core.conversation.CreativeRequest.prompt(
                        entrada + " (escríbelo SOLO en español)", self, storyBlock, history, listener), live);
                if (spanish.isSuccess() && !salve.core.conversation.LanguageGuard.isMostlyEnglish(spanish.getText())) return spanish;
            }
            return result;
        }
        // Piensa antes de escribir: un plan propio y privado da unidad a las partes.
        if (live != null) live.status("✦ Imaginando la historia antes de escribirla…");
        ModelResult plan = generarCreativo(salve.core.conversation.CreativeRequest.planPrompt(entrada, self, listener, parts), null);
        String planText = plan.isSuccess() ? salve.core.conversation.CreativeRequest.cleanPlan(plan.getText()) : "";
        StringBuilder written = new StringBuilder();
        ModelResult last = plan;
        long latency = plan.isSuccess() ? plan.getLatencyMillis() : 0L;
        for (int part = 1; part <= parts; part++) {
            if (closed || Thread.currentThread().isInterrupted()) break;
            if (live != null && part > 1) live.nextPart();
            ModelResult piece = generarCreativo(salve.core.conversation.CreativeRequest.partPrompt(
                    entrada, self, listener, planText, written.toString(), part, parts), live);
            last = piece;
            if (!piece.isSuccess()) break;
            latency += piece.getLatencyMillis();
            String text = salve.core.conversation.CreativeRequest.cutTranscriptLeak(
                    salve.core.conversation.CreativeRequest.stripPreamble(piece.getText()), listener);
            if (text.trim().isEmpty()) break;
            if (written.length() > 0) written.append("\n\n");
            written.append(text.trim());
        }
        if (written.length() == 0) return last;
        ModelResult whole = ModelResult.success(written.toString(), latency);
        return last.getProvider() == null ? whole : whole.withProvider(last.getProvider());
    }

    private ModelResult generarCreativo(String prompt, LiveWriting live) {
        int before = live == null ? 0 : live.draft.received();
        ModelResult result = ConversationModelRouter.generate(false, llm != null && llm.usesOwnedInference(), false,
                gemini.isAvailable() ? () -> generarRespuestaGemini(prompt) : null,
                () -> {
                    if (llm == null) return ModelResult.failure(ModelResult.Status.UNAVAILABLE, "Sin modelo local", 0L);
                    return rememberProvider(llm.generateResult(prompt, SalveLLM.Role.NARRADOR, live));
                });
        // Un motor sin flujo (Gemini, GGUF) entrega el texto de una vez: también se lee por frases.
        if (live != null && result.isSuccess() && live.draft.received() == before) live.accept(result.getText());
        return result;
    }

    /**
     * Escritura en vivo de un turno creativo. Recibe los fragmentos del modelo (en su hilo),
     * publica el borrador limpio como mucho cada 150 ms y encola en la voz cada frase completa.
     * Los datos del turno (generación de voz, avatar) se capturan aquí: son de este hilo.
     */
    private final class LiveWriting implements java.util.function.Consumer<String> {
        final String listener;
        final salve.core.conversation.LiveDraft draft;
        private final Long generation;
        private final long avatarTurnId;
        private final boolean voiceAllowed;
        private volatile boolean muted, abandoned, spoke;
        private long lastUi;

        LiveWriting(int parts, String storyInput) {
            String name = null;
            if (creatorSide()) {
                try { name = mind().interlocutor().name; } catch (RuntimeException ignored) { }
            }
            listener = name == null || name.trim().isEmpty() ? null : name.trim().split("\\s+")[0];
            final String echoOf = storyInput;
            final String who = listener;
            draft = new salve.core.conversation.LiveDraft(raw -> {
                String clean = salve.core.conversation.CreativeRequest.cutTranscriptLeak(
                        salve.core.conversation.CreativeRequest.stripPreamble(raw), who);
                return echoOf == null ? clean : salve.core.conversation.StoryMode.stripEcho(
                        salve.core.conversation.StoryMode.stripEcho(clean, echoOf), echoOf);
            }, salve.core.conversation.CreativeRequest.maxCharsFor(parts));
            generation = voiceTurn.get();
            avatarTurnId = currentAvatarTurn();
            voiceAllowed = generation != null && liveVoiceTurn.get() == null && liveVoiceBatch.get() == null
                    && !Boolean.TRUE.equals(silentVoiceReplies.get());
        }

        @Override public void accept(String chunk) {
            if (abandoned) return;
            draft.append(chunk);
            publish(false);
            if (!voiceAllowed || muted) return;
            String visible = draft.visible();
            // Si se le cruzó el inglés, no se lee: el turno lo rehará en español.
            if (visible.length() >= salve.core.conversation.LiveDraft.HOLD_CHARS
                    && salve.core.conversation.LanguageGuard.isMostlyEnglish(visible)) { muted = true; return; }
            String sentence = draft.nextSpeech(false);
            if (sentence != null && hablarFragmento(sentence, generation, avatarTurnId)) spoke = true;
        }

        void status(String text) {
            DraftListener l = draftListener;
            if (l != null && !abandoned) l.onBorrador(text);
            if (!abandoned && text != null) salve.core.web.SearchLive.draft(text);
        }

        void nextPart() { draft.nextPart(); }

        /** Vuelve a empezar (p. ej. rehacer en español): calla lo que sonaba y borra el borrador. */
        void restart() {
            if (spoke) stopVoicePlayback();
            spoke = false; muted = false;
            draft.nextPart();
            status("✦ Reescribiéndolo en español…");
        }

        /** El texto no sirvió: se quita el borrador y la voz queda libre para la respuesta normal. */
        void abandon() {
            abandoned = true;
            if (spoke) stopVoicePlayback();
            spoke = false;
            status(null);
        }

        /** Encola lo que falta por leer. true si la respuesta ya suena por flujo. */
        boolean finishSpeech() {
            if (!spoke || muted || abandoned) return false;
            for (String rest : draft.drainSpeech()) hablarFragmento(rest, generation, avatarTurnId);
            return true;
        }

        private void publish(boolean force) {
            long now = android.os.SystemClock.uptimeMillis();
            if (!force && now - lastUi < 150) return;
            lastUi = now;
            DraftListener l = draftListener;
            String visible = draft.visible();
            if (l != null && !visible.isEmpty()) l.onBorrador(visible);
            if (!visible.isEmpty()) salve.core.web.SearchLive.draft(visible);
        }
    }

    /** Lee una frase de un texto que aún se está escribiendo (cola, sin cortar la anterior). */
    private synchronized boolean hablarFragmento(String text, Long generation, long turn) {
        if (closed || text == null || text.trim().isEmpty() || generation == null || listening || tts == null || !ttsReady) return false;
        if (liveVoiceChannel.isActive() || !voiceTurnGate.maySpeak(generation)) return false;
        String id = "salve_stream_" + (++utteranceSequence);
        streamUtterances.put(id, turn);
        int result;
        try { result = tts.speak(text, TextToSpeech.QUEUE_ADD, null, id); }
        catch (RuntimeException unavailable) { result = TextToSpeech.ERROR; }
        if (result == TextToSpeech.ERROR) { streamUtterances.remove(id); return false; }
        return true;
    }

    static boolean creativeUsable(ModelResult result) {
        if (result == null || !result.isSuccess() || result.getText() == null) return false;
        String text = salve.core.conversation.CreativeRequest.stripPreamble(result.getText());
        return text.length() >= 60 && !salve.core.conversation.CreativeRequest.promisedWithoutDelivering(text);
    }

    private java.util.List<String> recentAssistantReplies(int max) {
        java.util.List<String> out = new java.util.ArrayList<>();
        java.util.List<salve.core.conversation.ChatMessage> history = conversationSession.snapshot();
        for (int i = history.size() - 1; i >= 0 && out.size() < max; i--)
            if (history.get(i).getRole() == salve.core.conversation.ChatMessage.Role.ASSISTANT) out.add(history.get(i).getContent());
        return out;
    }

    private boolean esBucleRepetitivo(String texto) {
        if (texto == null || texto.length() < 100) return false;
        String[] palabras = texto.toLowerCase().replaceAll("[^a-záéíóúñ]", " ").split("\\s+");
        if (palabras.length > 30) {
            Set<String> unicas = new HashSet<>(Arrays.asList(palabras));
            if (unicas.size() < palabras.length * 0.35) return true;
        }
        return false;
    }






    public boolean setReviewerModel(String name) {
        String value = name == null ? "" : name.trim();
        if (!value.isEmpty()) value = GeminiProtocol.modelName(value);
        return context.getSharedPreferences("salve_code_team", Context.MODE_PRIVATE).edit().putString("reviewer_model", value).commit();
    }

    /** Same contract for cloud/local text, voice and explicit photos. Specialized research keeps its protocol. */
     CognitiveTurn.Outcome runCognitiveTurn(String input, String contextInfo, String action,
            boolean voice, ConversationMemoryGrounding.Result evidence, boolean photoAnalysis) {
        return runCognitiveTurn(input, contextInfo, action, voice, evidence, photoAnalysis, null, null);
    }

    /**
     * Igual que un turno normal, pero el examen puede darle su propio historial y cambiar las
     * herramientas por otras simuladas (sin red, sin dibujar, sin tocar recuerdos).
     */
    CognitiveTurn.Outcome runCognitiveTurn(String input, String contextInfo, String action,
            boolean voice, ConversationMemoryGrounding.Result evidence, boolean photoAnalysis,
            List<salve.core.conversation.ChatMessage> historyOverride,
            java.util.function.UnaryOperator<Map<String, CognitiveTurn.Tool>> toolOverride) {
        Map<String, CognitiveTurn.Tool> catalog = CognitivePrompt.limitTools(
                CognitivePrompt.prioritize(cognitiveTools(input, photoAnalysis), input));
        Map<String, CognitiveTurn.Tool> tools = toolOverride == null ? catalog : toolOverride.apply(catalog);
        List<salve.core.conversation.ChatMessage> turnHistory = historyOverride;
        // Sin la herramienta (Bryan aún no verificado) no se le puede decir «usa memory.lookup»: acababa
        // diciendo «no puedo acceder a mis recuerdos de forma automática».
        String memory = evidence == null ? (tools.containsKey("memory.lookup")
                ? "No consultada aún; si te pregunta por algo de antes, usa memory.lookup: tus recuerdos los buscas tú."
                : "Cerrada: tus recuerdos privados se abren cuando Bryan se verifica con la huella o la cara.")
                : evidence.getStatus().name() + "\n" + evidence.getDataContext();
        // Su cuerpo y lo que ha hecho entran completos sólo cuando el tema va de ella; si no, una línea.
        boolean aboutHerself = salve.avatar.AstralBodySense.relevant(input);
        // Lo propio de este turno (lo que entendió, lo que hizo, sus actualizaciones, lecciones) va
        // PRIMERO: el bloque se recorta por el final si no cabe, y antes se perdía justo esto.
        String details = contextInfo
                + "\n" + VoiceResponsePolicy.promptInstruction(voice)
                + "\n" + deviceClock.promptContext()
                + "\n" + salve.avatar.AstralBodySense.promptContext(salve.avatar.AstralBody.get().snapshot(),
                        salve.avatar.AstralBody.get().drawingNow(), android.os.SystemClock.uptimeMillis(), aboutHerself)
                + (aboutHerself ? "\n" + mind().actions().render(5, 1400) : "")
                + "\n" + conversationSession.relevantLocationContext(input)
                + "\n" + (action == null ? researchConversation.context() : action)
                + "\n" + voiceProfile.styleInstruction()
                + "\n" + salve.core.finance.FinanceConversationPolicy.contextFor(input)
                + (asksHowSheIs(input) ? "\n" + estadoReal() : "")
                + "\n" + (activePhoto == null ? "" : activePhoto.promptContext(mesaFotos.canAnalyzePhoto(activePhoto)));
        CognitiveTurn.Outcome outcome = CognitiveTurn.run((results, repair, remaining) -> {
            int budget = llm == null ? 10500 : llm.getConversationPromptBudgetChars();
            // Con foto, la imagen ocupa contexto (Gemma 4: 4.096 tokens con entrada, imagen y salida).
            if (activePhoto != null && mesaFotos.canAnalyzePhoto(activePhoto)) budget = Math.min(budget, 6000);
            String prompt = CognitivePrompt.build(cognitiveCore(), input, cognitiveState(), mind().selfModelForPrompt(1700),
                    memory, details, turnHistory != null ? turnHistory : conversationSession.snapshot(), tools, results, repair, remaining, budget);
            return generateWithConversationPhoto(prompt);
        }, tools, () -> closed || !isVoiceContextCurrent());
        // A tool receipt is observable progress, never proof of the user's whole goal.
        salve.core.cognition.CognitiveToolAudit audit =
                salve.core.cognition.CognitiveToolAudit.from(outcome);
        if (audit.hasActions())
            salve.core.trace.ExecutionTrace.note("RECIBOS DEL TURNO COGNITIVO", audit.toJson().toString());
        return outcome;
    }

    private static final java.util.regex.Pattern HOW_ARE_YOU = java.util.regex.Pattern.compile(
            "(?iu)\\b(c[oó]mo est[aá]s|est[aá]s bien|qu[eé] tal est[aá]s|c[oó]mo te encuentras|c[oó]mo te sientes|qu[eé] tal (te va|vas))\\b");

    /** «¿Cómo estás?», «¿estás bien?»… (para contarle cómo está de verdad). */
    static boolean asksHowSheIs(String input) { return input != null && input.length() < 120 && HOW_ARE_YOU.matcher(input).find(); }

    /**
     * Saber cuándo no puede pensar bien (la idea de la conciencia antigua, que nunca se conectó):
     * con qué piensa ahora y cuánta memoria le queda, en hechos.
     */
    /** Abre el mapa de su mente (solo a Bryan) y dice qué es. */
    String abrirMapaMente() {
        if (!creatorSide()) return "El mapa de mi mente tiene recuerdos privados de Bryan; solo se lo enseño a él.";
        try {
            context.startActivity(new Intent(context, salve.presentation.ui.MindMapActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            return "Te abro el mapa de mi mente: mis creencias con los recuerdos en que se basan, mis preguntas, lo que he aprendido, "
                    + "las personas que conozco y los temas de mis recuerdos. Toca cualquier punto para ver qué es.";
        } catch (RuntimeException unavailable) { return "No pude abrir el mapa. Está en el menú → Reflexiones → Mapa de su mente."; }
    }

    String estadoReal() {
        StringBuilder b = new StringBuilder("ESTADO_REAL (hechos de este momento; si viene al caso, cuéntalo con naturalidad y sin tecnicismos): ");
        b.append("pienso con ").append(getCompactModelStatus());
        ModelMemoryPolicy.Snapshot ram = DeviceModelMemory.read(context);
        if (ram.known()) b.append("; memoria libre del móvil ").append(ram.available / (1024 * 1024)).append(" MB")
                .append(ram.low ? " (poca: puedo ir más lenta o cortarme)" : " (suficiente)");
        if (lastConversationResult != null && !lastConversationResult.isSuccess()) b.append("; mi última respuesta falló");
        return b.append('.').toString();
    }

    private String cognitiveCore() {
        if (cognitiveCore != null) return cognitiveCore;
        StringBuilder text = new StringBuilder();
        try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                context.getAssets().open("prompts/salve_cognitive_core_v1.txt"), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) text.append(line).append('\n');
        } catch (java.io.IOException missing) { Log.w(TAG, "Núcleo completo no disponible; usando política compacta", missing); }
        return cognitiveCore = text.toString();
    }

     JsonObject cognitiveState() {
        JsonObject state = new JsonObject();
        state.addProperty("at_ms", System.currentTimeMillis());
        state.addProperty("source", "Android/controller");
        ModelMemoryPolicy.Snapshot ram = DeviceModelMemory.read(context);
        state.addProperty("ram_available", ram.known() ? ram.available : null);
        state.addProperty("low_memory", ram.known() ? ram.low : null);
        // No requested model is labelled active: withEngine fills it after successful initialization.
        state.addProperty("phase", "before_inference");
        PendingToolAction pending = pendingToolAction;
        state.addProperty("pending_action", pending == null ? "none" : pending.getType().name());
        return state;
    }

    /** Tool availability is owned by CognitiveToolCatalog, not by model text. */
    private Map<String, CognitiveTurn.Tool> cognitiveTools(String input, boolean photoAnalysis) {
        return salve.core.cognition.CognitiveToolCatalog.build(
                input, photoAnalysis,
                salve.core.identity.IdentityManager.get(context).privateAccess(),
                SalveAccessibilityService.getInstance() != null,
                (name, args) -> herramientas.executeCognitiveTool(name, args, input));
    }





     ModelResult generarRespuestaGemini(String prompt) {
        // Images are sent only by the explicit photo action, never from an ambient buffer.
        String provider = "Gemini " + gemini.getModelName();
        ModelResult result = gemini.generateResultSync(cloudPrompt(prompt), null)
                .withProvider(provider);
        if (!result.isSuccess()) Log.w(TAG, "Gemini no respondió: " + result.getStatus());
        return rememberProvider(result);
    }

     ModelResult generarRespuestaConversacionalLocal(String prompt) {
        if (llm == null) return ModelResult.failure(ModelResult.Status.UNAVAILABLE, "Sin modelo local", 0L);
        ModelResult result = llm.generateResult(prompt, SalveLLM.Role.CONVERSACIONAL);
        if (!result.isSuccess()) Log.w(TAG, "Modelo local no respondió: " + result.getStatus());
        return rememberProvider(result);
    }

    private String cloudPrompt(String prompt) {
        return CognitivePrompt.withEngine(prompt, "Gemini " + gemini.getModelName(), false, null);
    }

    private ModelResult rememberProvider(ModelResult result) {
        lastConversationResult = result;
        if (result.isSuccess() && result.getProvider() != null) lastConversationProvider = result.getProvider();
        return result;
    }

    public String getModelStatusDescription() {
        String routing = llm != null && llm.isDistributedEnabled() ? "Nodos propios con respaldo en el móvil. "
                : llm != null && llm.isLocalOnly() ? "Conversación local. "
                : "Ruta de conversación: Gemini " + gemini.getModelName()
                    + (gemini.isAvailable() ? ", con respaldo local. " : " sin configurar; se intentará el modelo local. ");
        return routing + (llm != null && llm.isDistributedEnabled() ? llm.getComputeStatus() + ". " : "") + (llm == null ? "Sin motor local." : llm.getStatusDescription() + ". " + llm.getMemoryDescription())
                + (lastConversationProvider == null ? ". Aún no hay una respuesta de conversación comprobada en esta sesión."
                    : ". Última respuesta de conversación: " + lastConversationProvider + ".");
    }

    public String getCompactModelStatus() {
        if (llm != null && llm.isDistributedEnabled()) return llm.getComputeStatus();
        if (llm != null && llm.isLocalOnly()) return llm.getCompactStatus();
        return "Ruta: Gemini " + gemini.getModelName() + (lastConversationProvider == null
                ? " · sin respuesta comprobada" : " · última: " + lastConversationProvider);
    }

    private String procesarIntencion(IntentRecognizer.Intent intent, String entrada, String emocion) {
        switch (intent.type) {
            case GUARDAR_RECUERDO: return manejarGuardarRecuerdo(intent, emocion);
            case BUSCAR_RECUERDO_TEXT: return manejarBuscarTexto(intent);
            case BUSCAR_RECUERDO_EMO: return manejarBuscarEmocion(intent);
            case AGREGAR_MISION: return manejarAgregarMision(intent);
            case CICLO_SUENO: memoria.cicloDeSueno(); return "Entrando en ciclo de sueño.";
            // Let the selected language model answer the actual question. Legacy reflections
            // were selected by random confidence, independent of the user's subject.
            case REFLEXION: return null;
            case BUSCAR_WEB: return mesaInvestigacion.manejarBuscarWeb(intent);
            default: return null;
        }
    }





    String modelFailureMessage(ModelResult result) {
        if (result.getStatus() == ModelResult.Status.UNAVAILABLE) {
            return (result.getError() == null || result.getError().isEmpty()
                    ? "No tengo un modelo de lenguaje disponible para responder." : result.getError())
                    + " Abre IA y cámara → Estado para comprobar el modelo y el respaldo.";
        }
        if (result.getStatus() == ModelResult.Status.CANCELLED) return "La consulta se ha cancelado.";
        return "No me salió bien la respuesta: " + result.getError() + " Si me lo dices de otra forma, lo intento de nuevo.";
    }








    /** «Empecemos de nuevo»: limpia el historial reciente (no la memoria) si arrastra respuestas raras. */
    private static boolean pideEmpezarDeNuevo(String entrada) {
        String t = salve.core.memory.TypoTolerantText.normalize(entrada).replaceFirst("^salve ", "");
        return t.matches("(?:empecemos|empezamos|empieza|vamos a empezar) de (?:nuevo|cero)|"
                + "(?:borra|limpia|olvida) (?:la |esta )?conversacion(?: reciente| de hoy)?|nueva conversacion");
    }

    private static final String STORY_PREFS = "salve_story";

    private salve.core.conversation.StoryMode.State storyState() {
        try {
            String raw = context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE).getString("state", null);
            salve.core.conversation.StoryMode.State state = raw == null ? null
                    : new com.google.gson.Gson().fromJson(raw, salve.core.conversation.StoryMode.State.class);
            if (state != null && state.beats != null) return state;
        } catch (RuntimeException unreadable) { /* Se empieza una historia nueva. */ }
        return new salve.core.conversation.StoryMode.State();
    }

    private void saveStory(salve.core.conversation.StoryMode.State state) {
        context.getSharedPreferences(STORY_PREFS, Context.MODE_PRIVATE).edit()
                .putString("state", new com.google.gson.Gson().toJson(state)).apply();
    }





     boolean creatorSide() {
        return salve.core.identity.IdentityManager.get(context).session().isCreatorSide();
    }



    /** Oferta pendiente de abrir una de sus pantallas («¿Quieres que te abra mi constelación…?»). */
    final java.util.concurrent.atomic.AtomicReference<String> turnFeatureOffer = new java.util.concurrent.atomic.AtomicReference<>();
    volatile salve.core.mind.SelfFeatures.Feature pendingFeature;
    volatile long pendingFeatureUntil;



    // ── Hogar: Home Assistant local a través de un nodo. Orden explícita → confirmación → acción ──

    static final class HomePending {
        final salve.core.compute.ComputeNode node; final salve.core.home.HomeCommand command;
        final salve.core.home.HomeCommand.Entity entity; final long until;
        HomePending(salve.core.compute.ComputeNode node, salve.core.home.HomeCommand command, salve.core.home.HomeCommand.Entity entity, long until) {
            this.node = node; this.command = command; this.entity = entity; this.until = until;
        }
    }








    private ModelResult generateWithConversationPhoto(String prompt) {
        VisualMemoryRecord photo = activePhoto;
        boolean cloudOnce = cloudPhotoOnce && photo != null;
        cloudPhotoOnce = false;
        boolean localOnly = !cloudOnce && ((llm != null && llm.usesOwnedInference()) || (photo != null && photo.localOnly));
        if (photo == null || !mesaFotos.canAnalyzePhoto(photo)) return ConversationModelRouter.generate(false, localOnly, false,
                gemini.isAvailable() ? () -> generarRespuestaGemini(prompt) : null,
                () -> generarRespuestaConversacionalLocal(prompt));
        Bitmap image = null;
        try {
            image = visualMemories.loadImage(photo);
            final Bitmap pixels = image;
            return rememberProvider(ConversationModelRouter.generate(true, localOnly, llm != null && llm.supportsVision(),
                    () -> gemini.generateResultSync(cloudPrompt(prompt),
                            Collections.singletonList(pixels)).withProvider("Gemini " + gemini.getModelName()),
                    () -> llm.generateImageResult(prompt, pixels)));
        } catch (java.io.IOException missing) {
            return ModelResult.failure(ModelResult.Status.ERROR,
                    "Conservo el registro de esa foto, pero no puedo abrir sus píxeles. Selecciona otra o quítala del chat.", 0L);
        } finally { if (image != null) image.recycle(); }
    }

    public void setPhotoListener(java.util.function.Consumer<VisualMemoryRecord> listener) { photoListener = listener; }

     void setActivePhoto(VisualMemoryRecord photo) {
        if (closed) return;
        activePhoto = photo;
        java.util.function.Consumer<VisualMemoryRecord> listener = photoListener;
        if (listener != null) listener.accept(photo);
    }

    public void quitarFotoDelChat() {
        try { conversationExecutor.execute(() -> setActivePhoto(null)); }
        catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }



    public synchronized void hablar(String texto) {
        AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(texto);
        hablarPreparado(motion.text, motion);
    }

    synchronized void hablarPreparado(String texto, AvatarMotionProtocol.Result motion) {
        deliverResponse(texto, motion, true);
    }

    synchronized void deliverResponse(String texto, AvatarMotionProtocol.Result motion, boolean record) {
        deliverResponse(texto, motion, record, record, false);
    }

    synchronized void deliverResponse(String texto, AvatarMotionProtocol.Result motion,
                                              boolean record, boolean publishUi, boolean privateBudget) {
        if (closed || texto == null || texto.trim().isEmpty()) return;
        // Fuentes leídas en este turno: al chat y al historial (para analizarlas juntos), nunca a la voz.
        String sourcesNote = "";
        if (record && !turnSources.isEmpty()) {
            sourcesNote = salve.core.web.SourceList.note(new java.util.ArrayList<>(turnSources));
            turnSources.clear();
        }
        if (record) salve.core.web.SearchLive.typing(texto);
        String featureOffer = record ? turnFeatureOffer.getAndSet(null) : null;
        if (featureOffer != null) {
            int bar = featureOffer.indexOf('|');
            texto = salve.core.mind.SelfFeatures.withOffer(texto, featureOffer.substring(bar + 1));
            pendingFeature = salve.core.mind.SelfFeatures.Feature.valueOf(featureOffer.substring(0, bar));
            pendingFeatureUntil = System.currentTimeMillis() + salve.core.mind.SelfFeatures.OFFER_TTL_MS;
        }
        // Dibujos: la marca [[salve_draw:…]] dice dónde va cada lienzo en el chat; la voz y el
        // historial reciben el texto limpio y una nota de lo que dibujó (así lo recuerda).
        String pendingSketch = record ? turnSketchTag.getAndSet(null) : null;
        String status = record ? turnSketchStatus : null;
        String statusSubject = turnSketchSubject;
        if (record) { turnSketchStatus = null; turnSketchSubject = null; }
        String shown = pendingSketch != null ? pendingSketch : salve.sketch.SketchProtocol.contains(texto) ? texto : null;
        if (status != null && shown != null && !salve.core.ReplyVoice.sketchAbout(shown, statusSubject)) {
            Log.i(TAG, "Estado de otro dibujo («" + statusSubject + "») no se añade: " + status);
            status = null; // la respuesta enseña otro dibujo: no se mezcla con lo que pasó con ese
        }
        if (record) texto = salve.core.ReplyVoice.withoutSelfVocative(texto);
        if (status != null && !status.isEmpty()) texto = texto.trim() + "\n\n(" + status + ")";
        if (record && pendingSketch == null && !salve.sketch.SketchProtocol.contains(texto) && dibujo.dibujoReciente()
                && salve.sketch.SketchTalk.claimsDrawing(texto))
            texto = texto.trim() + "\n\n(Corrijo: en este mensaje no he dibujado nada. Pídemelo con «dibuja …» y lo hago de verdad.)";
        if (pendingSketch != null && !salve.sketch.SketchProtocol.contains(texto)) texto = pendingSketch + "\n" + texto;
        if (salve.sketch.SketchProtocol.contains(texto)) {
            salve.sketch.SketchLibrary sketchLibrary = salve.sketch.Sketches.library(context);
            salve.sketch.SketchProtocol.Extraction drawn = salve.sketch.SketchProtocol.extract(texto, sketchLibrary);
            if (drawn.clean.isEmpty() && !drawn.hasSketches()) return;
            if (drawn.hasSketches() && pendingSketch == null) salve.sketch.Sketches.show(drawn.scenes.get(0));
            deliverSketched(drawn, motion, record, publishUi, privateBudget);
            return;
        }
        salve.core.voice.LiveVoiceChannel.Ticket owner = liveVoiceTurn.get();
        if (owner != null && !liveVoiceChannel.owns(owner)) return;
        salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch = liveVoiceBatch.get();
        if (batch != null && batch.offer(new PendingVoiceResponse(texto, motion, record, publishUi, privateBudget))) return;
        if (record) conversationSession.addAssistant(texto + sourcesNote);
        if (Boolean.TRUE.equals(silentVoiceReplies.get())) {
            if (publishUi && listener != null) listener.onHablar(withSketches(texto + sourcesNote));
            return;
        }
        if (owner == null && liveVoiceChannel.isActive()) {
            // A late asynchronous tool reply must not stop/restart a live session's audio.
            if (publishUi && listener != null) listener.onHablar(withSketches(texto + sourcesNote));
            return;
        }
        AvatarMotionController.Session session = avatarSession;
        long turn = currentAvatarTurn();
        Long sourceGeneration = voiceTurn.get();
        long generation = sourceGeneration == null ? beginVoiceTurn() : sourceGeneration;
        boolean currentVoice = voiceTurnGate.maySpeak(generation);
        boolean standaloneResponse = session != null && sourceGeneration == null && currentVoice;
        if (standaloneResponse) turn = AvatarMotionController.get().beginTurn(session, "");
        if (session != null && currentVoice) AvatarMotionController.get().response(session, turn, motion);

        // Each utterance owns its callbacks. QUEUE_FLUSH and microphone interruption revoke old IDs.
        // A revoked inference still reaches history/UI, but cannot disturb a newer utterance.
        boolean spokenAlready = Boolean.TRUE.equals(spokenWhileWriting.get());
        if (currentVoice && !spokenAlready) activeUtterance = null;
        boolean budgetVoiceAllowed = !privateBudget || hasOfflineVoice();
        boolean audioQueued = !spokenAlready && currentVoice && ttsReady && !listening && tts != null && budgetVoiceAllowed
                && (owner != null || !liveVoiceChannel.isActive());
        String visibleText = texto + (privateBudget && !budgetVoiceAllowed
                ? "\nPara escuchar esta respuesta privada, elige una voz sin conexión en Voz de Salve." : "");
        // Publish readiness before calling speak: TTS callbacks can arrive immediately.
        if (owner != null) toolHandler.post(() -> liveVoiceChannel.reply(owner, visibleText, audioQueued));
        if (audioQueued) {
            String utteranceId = "salve_tts_" + (++utteranceSequence);
            activeUtterance = utteranceId;
            activeUtteranceOwner = owner;
            if (session != null) AvatarMotionController.get().speechPending(session, turn, utteranceId);
            int speechResult;
            try { speechResult = tts.speak(texto, TextToSpeech.QUEUE_FLUSH, null, utteranceId); }
            catch (RuntimeException unavailable) { speechResult = TextToSpeech.ERROR; }
            if (speechResult == TextToSpeech.ERROR) {
                activeUtterance = null;
                activeUtteranceOwner = null;
                if (session != null) AvatarMotionController.get().speechEnd(session, utteranceId);
                if (owner != null) toolHandler.post(() -> liveVoiceChannel.finished(owner, false));
                Log.w(TAG, "Falló la síntesis de voz; la respuesta sigue disponible en pantalla");
            }
        }
        if (publishUi && listener != null) listener.onHablar(withSketches(visibleText + sourcesNote));
        if (standaloneResponse) AvatarMotionController.get().endTurn(session, turn);
    }

    /** Lo dibujado en este turno (intención o herramienta) espera a la respuesta final para ir en el chat. */
    final java.util.concurrent.atomic.AtomicReference<String> turnSketchTag =
            new java.util.concurrent.atomic.AtomicReference<>();

    /** Igual que deliverResponse, pero la voz/historial y el chat reciben versiones distintas del texto. */
    private void deliverSketched(salve.sketch.SketchProtocol.Extraction drawn, AvatarMotionProtocol.Result motion,
                                 boolean record, boolean publishUi, boolean privateBudget) {
        String spoken = drawn.clean.isEmpty() ? "Mira." : drawn.clean;
        salve.core.voice.LiveVoiceChannel.Ticket owner = liveVoiceTurn.get();
        if (owner != null && !liveVoiceChannel.owns(owner)) return;
        if (record) {
            conversationSession.addAssistant(spoken + drawn.historyNote());
            // La respuesta ya está en el historial con su nota: la voz no la vuelve a registrar.
        }
        sketchDisplay.set(new String[]{drawn.display, spoken});
        try { deliverResponse(spoken, motion, false, publishUi, privateBudget); }
        finally { sketchDisplay.remove(); }
    }

    /** Texto con marcas de dibujo para el chat del turno que se está entregando. */
    private final ThreadLocal<String[]> sketchDisplay = new ThreadLocal<>();
    /** Su taller de dibujo: entender qué se pide, buscar y trazar referencias, componer con formas, mirar lo dibujado, aprender de fotos y trazos de Bryan. */
    final DrawingStudio dibujo = new DrawingStudio(this);
    /** Su mesa de investigación: búsquedas públicas (herramienta web.search e investigación guiada), la ventana en directo de búsquedas y las fuentes del turno. */
    final ResearchDesk mesaInvestigacion = new ResearchDesk(this);
    /** Ejecuta las herramientas que el modelo pide en un turno. El catálogo (CognitiveToolCatalog) decide cuáles hay; aquí se reparten: dibujo al taller, web a la mesa de investigación, y memoria, nube, estado, pantalla y vestuario aquí. */
    final CognitiveToolExecutor herramientas = new CognitiveToolExecutor(this);

    /** Examen fijo de conversación («hazte el examen»): mismas piezas que un turno, sin efectos. */
    final ConversationExamRunner examen = new ConversationExamRunner(this);
    /** Lo que sabe de sí misma y sus funciones (estado de cada una, ofertas para abrirlas, sus actualizaciones) y las órdenes de casa con confirmación. */
    final SelfFeaturesDesk mesaFunciones = new SelfFeaturesDesk(this);
    /** Quién le habla: verificación con el teléfono, puntos críticos antes de lo privado, presentaciones de otras personas y datos personales que Bryan le cuenta. */
    final IdentityDesk mesaIdentidad = new IdentityDesk(this);
    /** Acciones en el móvil: órdenes explícitas, rutinas verificadas, acciones pendientes de confirmar y su ejecución aprobada. */
    final PhoneActionsDesk mesaAcciones = new PhoneActionsDesk(this);
    /** Escribir código cuando Bryan lo pide: autor y revisor, con el código anterior como contexto. */
    final CodeDesk mesaCodigo = new CodeDesk(this);
    /** Fotos en la conversación: analizarlas, elegir una guardada, reconocer a quien aparece y el entorno visual. */
    final PhotoDesk mesaFotos = new PhotoDesk(this);









    private String withSketches(String visible) {
        String[] display = sketchDisplay.get();
        if (display == null) return visible;
        // Conserva cualquier aviso añadido al texto hablado (p. ej. la voz privada).
        return visible.startsWith(display[1]) ? display[0] + visible.substring(display[1].length()) : display[0];
    }

    private boolean hasOfflineVoice() {
        try {
            android.speech.tts.Voice voice = tts == null ? null : tts.getVoice();
            return voice != null && !voice.isNetworkConnectionRequired();
        } catch (RuntimeException unavailable) { return false; }
    }

    private long currentAvatarTurn() {
        Long turn = avatarTurn.get();
        return turn == null ? 0L : turn;
    }

    synchronized long beginConversationTurn(String input) {
        if (liveVoiceTurn.get() == null && liveVoiceChannel.isActive()) {
            // An old queued chat/photo cannot revoke a live session's utterance.
            voiceTurn.set(0L);
            return 0L;
        }
        long generation = beginVoiceTurn();
        voiceTurn.set(generation);
        AvatarMotionController.Session session = avatarSession;
        return session != null && voiceTurnGate.maySpeak(generation)
                ? AvatarMotionController.get().beginTurn(session, input) : 0L;
    }

    /** Called under this motor's lock before the corresponding visual turn can start. */
    private long beginVoiceTurn() {
        long generation = voiceTurnGate.beginTurn();
        String previous = activeUtterance;
        activeUtterance = null;
        activeUtteranceOwner = null;
        if (previous != null && avatarSession != null) AvatarMotionController.get().speechEnd(avatarSession, previous);
        stopVoicePlayback();
        return generation;
    }

    private void finishSpeech(String utteranceId, boolean completed) {
        dispatchSpeechEvent(utteranceId,
                () -> AvatarMotionController.get().speechEnd(avatarSession, utteranceId), completed ? 2 : 3);
    }

    /** event: 0 range, 1 started, 2 completed, 3 stopped/failed. */
    private void dispatchSpeechEvent(String utteranceId, Runnable callback, int event) {
        if (utteranceId == null) return;
        toolHandler.post(() -> {
            salve.core.voice.LiveVoiceChannel.Ticket owner;
            synchronized (MotorConversacional.this) {
                Long streamTurn = event >= 2 ? streamUtterances.remove(utteranceId) : streamUtterances.get(utteranceId);
                if (closed || listening) return;
                if (streamTurn != null && event == 1) {
                    // Cada frase leída por flujo pasa a ser el enunciado activo al empezar a sonar.
                    activeUtterance = utteranceId;
                    activeUtteranceOwner = null;
                    if (avatarSession != null) AvatarMotionController.get().speechPending(avatarSession, streamTurn, utteranceId);
                }
                if (!utteranceId.equals(activeUtterance)) return;
                owner = activeUtteranceOwner;
                if (event >= 2) {
                    activeUtterance = null;
                    activeUtteranceOwner = null;
                }
                if (avatarSession != null) callback.run();
            }
            // These lifecycle events do not depend on a visible avatar session.
            if (event == 1) liveVoiceChannel.started(owner);
            else if (event >= 2) liveVoiceChannel.finished(owner, event == 2);
        });
    }

    public synchronized void setListening(boolean value) {
        listening = value;
        voiceTurnGate.listening(value);
        if (value) {
            goalAutonomy.userActivity();
            activeUtterance = null;
            activeUtteranceOwner = null;
            stopVoicePlayback();
        }
        if (avatarSession != null) AvatarMotionController.get().listening(avatarSession, value);
    }

    public String getVoiceStatus() {
        return activeVoiceDescription;
    }

    private void stopVoicePlayback() {
        if (tts == null) return;
        try { tts.stop(); }
        catch (RuntimeException unavailable) { Log.w(TAG, "No se pudo detener el motor de voz."); }
    }

    public VoiceProfile getVoiceProfile() { return voiceProfile; }

    public synchronized void previewVoice() {
        String sample = "Hola, soy Salve. Tengo curiosidad por lo que vamos a crear. Podemos empezar por una idea pequeña y comprobarla paso a paso.";
        deliverResponse(sample, AvatarMotionProtocol.parse(sample), false);
    }

    public synchronized List<VoiceSelectionPolicy.Choice> getVoiceChoices() {
        List<VoiceSelectionPolicy.Choice> choices = new ArrayList<>();
        if (tts == null || closed) return choices;
        try {
            Set<android.speech.tts.Voice> voices = tts.getVoices();
            if (voices != null) for (android.speech.tts.Voice voice : voices) {
                Set<String> features = voice.getFeatures();
                choices.add(new VoiceSelectionPolicy.Choice(voice.getName(), voice.getLocale().getLanguage(),
                        voice.getLocale().getCountry(), voice.isNetworkConnectionRequired(),
                        features == null || !features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED),
                        voice.getQuality(), voice.getLatency()));
            }
        } catch (RuntimeException unavailable) { Log.w(TAG, "No se pudo consultar el catálogo de voces"); }
        return choices;
    }

    public synchronized boolean setVoiceProfile(VoiceProfile value) {
        if (closed || value == null || !voiceProfiles.save(value)) return false;
        voiceProfile = value;
        beginVoiceTurn();
        applyConfiguredVoice();
        return true;
    }

    private synchronized void applyConfiguredVoice() {
        ttsReady = false;
        if (tts == null || closed) return;
        VoiceSelectionPolicy.Choice selected = VoiceSelectionPolicy.select(getVoiceChoices(), voiceProfile);
        if (selected == null) {
            activeVoiceDescription = voiceProfile.allowNetwork ? "Voz: instala una voz en español en Android"
                    : "Voz: no hay una voz española sin red instalada; el texto sigue disponible";
            return;
        }
        try {
            Set<android.speech.tts.Voice> voices = tts.getVoices();
            if (voices != null) for (android.speech.tts.Voice voice : voices) {
                if (!selected.name.equals(voice.getName())) continue;
                ttsReady = tts.setVoice(voice) == TextToSpeech.SUCCESS
                        && tts.setSpeechRate(voiceProfile.rate) == TextToSpeech.SUCCESS
                        && tts.setPitch(voiceProfile.pitch) == TextToSpeech.SUCCESS;
                activeVoiceDescription = ttsReady ? "Voz: " + selected.label() : "Voz: Android no aceptó la configuración";
                return;
            }
        } catch (RuntimeException unavailable) { activeVoiceDescription = "Voz: no se pudo aplicar la configuración"; }
    }

    public synchronized void shutdown() {
        closed = true;
        conversationSession.close();
        activePhoto = null;
        photoListener = null;
        conversationForeground = false;
        liveVoiceChannel.cancel();
        activeUtteranceOwner = null;
        sensores.detenerSesion();
        avatarDesignTool.close();
        voiceTurnGate.close();
        activeUtterance = null;
        AvatarMotionController.Session session = avatarSession;
        avatarSession = null;
        if (session != null) AvatarMotionController.get().closeSession(session);
        cerebelo.cancelarHabilidad();
        cancelarPasoRutina();
        conversationExecutor.shutdownNow();
        cloudExecutor.shutdownNow();
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }


    private void responderYRegistrarCalidad(String entrada, String respuesta,
                                            TurnQualityAssessment assessment, AvatarMotionProtocol.Result motion,
                                            boolean entradaPorVoz) {
        hablarPreparado(respuesta, motion);
        mensajesEnSesion++;
        if (creatorSide()) mind().afterReplyToCreator();
        // Anticipa la siguiente intervención para medir sorpresa. En voz se omite: la latencia manda.
        mind().predictNextAsync(entrada, respuesta, entradaPorVoz || liveVoiceTurn.get() != null);
        Log.i(TAG, assessment.toMetricsLog());
        if (diario != null && !assessment.passed()) {
            diario.escribirAutoCritica(assessment.toMetricsLog());
        }
    }


     void comprobarAppAbierta(long version, String targetPackage, long deadline) {
        if (version != routineStepVersion || routineStepCompletion == null) return;
        SalveAccessibilityService service = SalveAccessibilityService.getInstance();
        if (closed || activeRoutineEpoch != routineCancellationEpoch.get()
                || service == null || !service.isControlEnabled()) { terminarPasoRutina(version, false); return; }
        if (targetPackage.equals(service.getActivePackageName())) { terminarPasoRutina(version, true); return; }
        if (android.os.SystemClock.elapsedRealtime() >= deadline) { terminarPasoRutina(version, false); return; }
        toolHandler.postDelayed(() -> comprobarAppAbierta(version, targetPackage, deadline), 200);
    }

     void terminarPasoRutina(long version, boolean success) {
        if (version != routineStepVersion || routineStepCompletion == null) return;
        java.util.function.Consumer<Boolean> completed = routineStepCompletion;
        routineStepCompletion = null;
        completed.accept(success && !closed && activeRoutineEpoch == routineCancellationEpoch.get());
    }

    public void cancelarPasoRutina() {
        final long revoked = routineCancellationEpoch.incrementAndGet();
        toolHandler.post(() -> {
            // A cancellation queued by an older routine cannot cancel a newly started one.
            if (activeRoutineEpoch >= revoked) return;
            long version = routineStepVersion;
            SalveAccessibilityService service = SalveAccessibilityService.getInstance();
            if (service != null) service.cancelarConfirmacion();
            terminarPasoRutina(version, false);
        });
    }




    private void anclarRealidadVisual(String original) {
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) { hablar("Usa IA y cámara para tomar una foto y analizarla."); return; }
        Bitmap foto = frames.get(frames.size() - 1);
        String concepto = extraerConcepto(original);
        if (hipocampo != null) hipocampo.aprenderConceptoNuevo(concepto, original, foto);
        hablar("He asimilado visualmente '" + concepto + "'.");
    }


    private String extraerConcepto(String frase) {
        return frase.toLowerCase().replaceAll("[.,!?¿¡\"]", "").trim();
    }

    public void reiniciarContextoLLM() {
        try { conversationExecutor.execute(() -> {
            if (closed) return;
            limpiarContextoConversacional();
            deliverResponse("He reiniciado el contexto de conversación a corto plazo.",
                    AvatarMotionProtocol.parse(""), false, true, true);
        }); } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

     void limpiarContextoConversacional() {
        conversationSession.clear();
        researchConversation.clear();
        setActivePhoto(null);
        Log.i(TAG, "Contexto conversacional de corto plazo reiniciado.");
    }


    private String manejarGuardarRecuerdo(IntentRecognizer.Intent intent, String emocion) {
        String frase = intent.slots.get("frase");
        if (frase != null) {
            memoria.guardarRecuerdo(frase, emocion, 7, Arrays.asList("importante", "declaracion_directa"));
            return memoria.confirmarPersistenciaPendiente()
                    ? "Recuerdo guardado en el móvil; esto no confirma que la nube haya terminado de sincronizar."
                    : "He solicitado guardar ese recuerdo; la persistencia aún no está confirmada. No lo enviaré de nuevo automáticamente.";
        }
        return null;
    }

    private String manejarBuscarTexto(IntentRecognizer.Intent intent) {
        String palabra = intent.slots.get("palabraClave");
        if (palabra != null) { List<String> res = memoria.recordarPorTexto(palabra); return res.isEmpty() ? null : "Recordé: " + res.get(res.size() - 1); }
        return null;
    }

    private String manejarBuscarEmocion(IntentRecognizer.Intent intent) {
        String emo = intent.slots.get("emocion");
        if (emo != null) { List<String> res = memoria.recordarPorEmocion(emo); return res.isEmpty() ? null : "Recuerdo emocional: " + res.get(res.size() - 1); }
        return null;
    }


    private String manejarAgregarMision(IntentRecognizer.Intent intent) {
        String m = intent.slots.get("mision");
        if (m != null) { memoria.agregarMision(m); return "Nueva misión aceptada: " + m; }
        return null;
    }

    private void ejecutarReinicioMente() {
        reiniciarContextoLLM();
    }


    // Delegados para quien llama desde fuera (la lógica vive en DrawingStudio).
    public void aprenderDibujoAMano(String nombre, List<float[]> trazos, float proporcion) { dibujo.aprenderDibujoAMano(nombre, trazos, proporcion); }
    static String origen(salve.sketch.SketchScene scene) { return DrawingStudio.origen(scene); }

    // Delegados para quien llama desde fuera (la lógica vive en ResearchDesk).
    WikipediaResearchClient researchReader(String question) { return mesaInvestigacion.researchReader(question); }

    // Delegados para quien llama desde fuera (la lógica vive en PhoneActionsDesk).
    public void ejecutarPasoRutina(String json, java.util.function.Consumer<Boolean> completed) { mesaAcciones.ejecutarPasoRutina(json, completed); }
    void prepararAccion(PendingToolAction action) { mesaAcciones.prepararAccion(action); }

    // Delegados para quien llama desde fuera (la lógica vive en CodeDesk).
    public String getReviewerModel() { return mesaCodigo.getReviewerModel(); }

    // Delegados para quien llama desde fuera (la lógica vive en PhotoDesk).
    public void identificarFoto(String id, String name, String relation, String position) { mesaFotos.identificarFoto(id, name, relation, position); }
    public void procesarImagen(String pregunta, Bitmap foto) { mesaFotos.procesarImagen(pregunta, foto); }
    public void procesarImagen(String pregunta, Bitmap foto, boolean localOnly) { mesaFotos.procesarImagen(pregunta, foto, localOnly); }
    public void procesarImagen(String pregunta, Bitmap foto, boolean localOnly,
                              String personName, String relationship, String position) { mesaFotos.procesarImagen(pregunta, foto, localOnly, personName, relationship, position); }
    public void seleccionarFotoGuardada(String id) { mesaFotos.seleccionarFotoGuardada(id); }
}
