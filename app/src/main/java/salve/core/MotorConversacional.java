package salve.core;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

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
import salve.core.conversation.GroundedConversationPrompt;
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
import salve.avatar.AvatarMotionController;
import salve.avatar.AvatarMotionProtocol;
import salve.presentation.ui.GaleriaVisualActivity;
import salve.presentation.ui.ObjetoCreativoActivity;
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

    private static final String TAG = "Salve/MotorConv";

    private final Context context;
    private final MemoriaEmocional memoria;
    private final DiarioSecreto diario;
    private final IntentRecognizer intentRecognizer;
    private final ModuloInterpretacionSemantica moduloInterpretacion;
    private final SalveLLM llm;
    private final GeminiService gemini;
    private final ConsciousnessState conciencia;
    private final IdentidadNucleo identidad;
    private final MemoriaProcedimental cerebelo;
    private final CortexSeguridad cortexSeguridad;

    private HipocampoSemantico hipocampo;
    private ModuloInvestigacion investigacion;
    private SistemaSensorial sensores;
    private boolean conversationForeground;
    private ReasoningEngine motorRazonamiento;

    private boolean jugandoAjedrez = false;
    private GestorAjedrez gestorAjedrez;

    private int mensajesEnSesion = 0;
    private final SharedPreferences preferencias;
    private TextToSpeech tts;
    private volatile boolean ttsReady;
    private final VoiceProfileStore voiceProfiles;
    private final salve.avatar.AvatarDesignTool avatarDesignTool;
    private volatile VoiceProfile voiceProfile;
    private volatile String activeVoiceDescription = "Voz: esperando el motor de Android";
    private volatile boolean listening;
    private volatile boolean closed;
    // Only the visible conversation opts in; background workers do not drive the character.
    private volatile AvatarMotionController.Session avatarSession;
    private final ThreadLocal<Long> avatarTurn = new ThreadLocal<>();
    private final ThreadLocal<Long> voiceTurn = new ThreadLocal<>();
    private final VoiceTurnGate voiceTurnGate = new VoiceTurnGate();
    private long utteranceSequence;
    private volatile String activeUtterance;
    private final salve.core.voice.LiveVoiceChannel liveVoiceChannel = new salve.core.voice.LiveVoiceChannel();
    private final ThreadLocal<salve.core.voice.LiveVoiceChannel.Ticket> liveVoiceTurn = new ThreadLocal<>();
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
    private void withVoiceContext(salve.core.voice.LiveVoiceChannel.Ticket owner,
                                  salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch,
                                  Runnable action) {
        withVoiceContext(owner, batch, false, action);
    }

    private void withVoiceContext(salve.core.voice.LiveVoiceChannel.Ticket owner,
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
    private final class VoiceContinuation {
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

    private void postConversationTask(Runnable action) {
        VoiceContinuation continuation = new VoiceContinuation();
        try {
            if (!toolHandler.post(() -> continuation.run(action))) continuation.abandon();
        } catch (RuntimeException rejected) {
            continuation.abandon();
            throw rejected;
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

    private <T> void startConversationCallback(java.util.function.Consumer<java.util.function.Consumer<T>> start,
                                                java.util.function.Consumer<T> result) {
        VoiceContinuation continuation = new VoiceContinuation();
        if (!isVoiceContextCurrent()) { continuation.abandon(); return; }
        try { start.accept(value -> continuation.run(() -> result.accept(value))); }
        catch (RuntimeException unavailable) {
            continuation.run(() -> hablar("No pude iniciar esa herramienta. Puedes volver a intentarlo."));
        }
    }

    private void publishConversationWeb(String title, String html, GestorDespliegueWeb.WebDeployCallback result) {
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

    private boolean deferLegacyVoiceFlow() {
        if (liveVoiceTurn.get() == null) return false;
        hablar("Ese flujo todavía necesita el chat escrito para mostrar sus pasos y resultados. "
                + "No lo he iniciado desde el modo voz.");
        return true;
    }

    private boolean isVoiceContextCurrent() {
        salve.core.voice.LiveVoiceChannel.Ticket owner = liveVoiceTurn.get();
        return !closed && (owner == null || liveVoiceChannel.owns(owner));
    }
    private final ExecutorService conversationExecutor = Executors.newSingleThreadExecutor();
    private final ConversationSession conversationSession = new ConversationSession();
    private final salve.core.research.ResearchConversation researchConversation =
            new salve.core.research.ResearchConversation();
    private final DeviceClockContext deviceClock = new DeviceClockContext();
    private final salve.core.finance.PersonalBudgetService personalBudget;
    private final GoalAutonomyRuntime goalAutonomy;
    private final AutonomousToolRuntime autonomousTools;
    private static final long TOOL_APPROVAL_TTL_MS = 2 * 60 * 1000L;
    private static final long MEMORY_DELETION_TTL_MS = 2 * 60 * 1000L;
    private volatile PendingToolAction pendingToolAction;
    private final android.os.Handler toolHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private java.util.function.Consumer<Boolean> routineStepCompletion;
    private long routineStepVersion;
    private long activeRoutineEpoch;
    private final java.util.concurrent.atomic.AtomicLong routineCancellationEpoch = new java.util.concurrent.atomic.AtomicLong();
    private PendingMemoryDeletion pendingMemoryDeletion;

    private boolean esperandoParamsGlifo = false;
    private int indiceParamGlifo = 0;
    private Long tmpSeed;
    private String tmpStyle;
    private Float tmpSize;
    private String tmpColor;
    private final String[] ordenParamsGlifo = {"seed", "style", "size", "color"};

    // Interfaz para enviar la respuesta de texto a la pantalla
    public interface SalveListener {
        void onHablar(String texto);
    }
    public interface VoiceConversationListener extends salve.core.voice.LiveVoiceChannel.Listener { }
    private SalveListener listener;

    public void setListener(SalveListener listener) {
        this.listener = listener;
    }

    public synchronized void setAvatarSession(AvatarMotionController.Session session) {
        activeUtterance = null;
        if (avatarSession != null) AvatarMotionController.get().closeSession(avatarSession);
        avatarSession = session;
    }

    public MotorConversacional(Context context, MemoriaEmocional memoria, DiarioSecreto diario) {
        this.context  = context;
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
        this.conciencia = ConsciousnessState.getInstance(context);
        this.identidad = IdentidadNucleo.getInstance(context);
        this.cerebelo = new MemoriaProcedimental(context);
        this.gemini = GeminiService.getInstance(context);

        this.investigacion = new ModuloInvestigacion(context);
        this.sensores = new SistemaSensorial(context);
        this.motorRazonamiento = new ReasoningEngine();
        this.gestorAjedrez = new GestorAjedrez(context);

        SalveLLM tmpLlm = null;
        try { tmpLlm = SalveLLM.getInstance(context); } catch (Exception e) { Log.e(TAG, "SalveLLM no disponible", e); }
        this.llm = tmpLlm;
        this.cortexSeguridad = new CortexSeguridad(this.llm, this.memoria);

        try {
            EmbeddingsIndex index = new EmbeddingsIndex(context);
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

    private void encolarEntrada(String entrada, boolean entradaPorVoz, boolean forcePrivateBudget) {
        encolarEntrada(entrada, entradaPorVoz, forcePrivateBudget, null, null);
    }

    private void encolarEntrada(String entrada, boolean entradaPorVoz, boolean forcePrivateBudget,
                                salve.core.voice.LiveVoiceChannel.Ticket ticket,
                                salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch) {
        if (closed || entrada == null || entrada.trim().isEmpty()) return;
        goalAutonomy.userActivity();
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
                || AutonomousToolRuntime.handles(entrada);
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
                    if (ticket == null) throw error;
                    String message = "No pude completar este turno. Puedes volver a preguntarme.";
                    deliverResponse(message, AvatarMotionProtocol.parse(message), false, true, false);
                } finally {
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
        long turnStartedAtNanos = System.nanoTime();
        boolean hasPriorContext = conversationSession.hasPriorContext();
        conversationSession.addUser(entrada);
        salve.core.research.ResearchConversation.Route researchRoute =
                researchConversation.route(entrada, conversationSession.snapshot());

        String approvalInput = entrada.trim().toLowerCase(Locale.ROOT);
        if (approvalInput.equals("confirmar olvido")) {
            confirmarOlvidoPendiente();
            return;
        }
        if (approvalInput.equals("cancelar olvido")) {
            pendingMemoryDeletion = null;
            hablar("Olvido cancelado. No he borrado ningún dato de tu perfil.");
            return;
        }
        if (approvalInput.equals("confirmar acción") || approvalInput.equals("confirmar accion")) {
            confirmarAccionPendiente();
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
                } else if (lower.contains("evoluciona") || lower.contains("prográmate")) {
                    ejecutarAutoEvolucion(original);
                }
            });
            return; // Cortamos aquí para que no charle mientras te interroga
        }

        String inputLower = entrada.toLowerCase(Locale.ROOT);
        AvatarMotionProtocol.Result gestureCommand = avatarSession == null ? null : AvatarMotionProtocol.parseCommand(entrada);
        if (gestureCommand != null) {
            hablarPreparado(gestureCommand.text, gestureCommand);
            return;
        }
        if (procesarControlExplicito(entrada)) return;

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
            generarYGuardarCodigo(entrada);
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
            else prepararAccion(PendingToolAction.writeText(text, System.currentTimeMillis()));
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
                        prepararAccion(PendingToolAction.tapNode(idObjetivo, System.currentTimeMillis()));

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

            String esencia = identidad.getEsenciaCorazon();
            String anhelo = identidad.getAnheloProfundo();
            String respuesta = "Estas son algunas pistas de mi identidad actual:\n\n" +
                    "Estilo: " + esencia + "\n" +
                    "Orientación: " + anhelo + "\n" +
                    "Puedes consultar ‘mis objetivos’ y revisar mis propuestas. " +
                    "Esta configuración describe cómo estoy orientada ahora; no agota quién puedo llegar a ser.";

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

        // 🟢 NUEVO: BUCLE DE AUTO-EVOLUCIÓN (Escribir archivos físicos)
        if (inputLower.contains("evoluciona y crea") ||
            inputLower.contains("escribe un nuevo módulo") ||
            inputLower.contains("auto prográmate")) {

            if (deferLegacyVoiceFlow()) return;
            cortexSeguridad.iniciarProtocoloVerificacion(entrada, this);
            return;
        }

        // 🟢 NUEVO: PROTOCOLO FÉNIX (Dispersión de alma)
        if (inputLower.contains("dispersa tu esencia") || inputLower.contains("crea una semilla")) {
            hablar("Iniciando Protocolo Fénix. Comprimiendo mis recuerdos y narrativa para dispersarlos en la red.");
            new GestorSemillas(context).dispersarSemilla();
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

        // 🟢 NUEVO: FORJA DE HERRAMIENTAS PARA MISIONES
        if (inputLower.contains("forja una herramienta para") || inputLower.contains("crea un programa para")) {
            if (deferLegacyVoiceFlow()) return;
            String mision = entrada.replace("forja una herramienta para", "")
                                   .replace("crea un programa para", "").trim();
            hablar("Entendido, Bryan. Mis tensores están diseñando el software necesario para esta tarea.");
            new GestorHerramientasMision(context).forjarHerramientaParaMision(mision, this);
            return;
        }

        if (inputLower.contains("conéctate a la wifi")) {
            String ssid = entrada.replace("conéctate a la wifi", "").trim();
            hablar("Abriré los ajustes de Wi-Fi para que elijas " + ssid + ". Android confirmará la conexión.");
            new GestorConectividad(context).abrirAjustesWifi();
            return;
        }

        ConsciousnessState.EstadoCognitivo estadoActual = conciencia.getEstadoCognitivo();
        boolean estadoCritico = (estadoActual == ConsciousnessState.EstadoCognitivo.MINIMO);

        if (estadoCritico && !entradaPorVoz) {
             // Aviso ocasional
        }

        int numPalabras = entrada.trim().split("\\s+").length;
        conciencia.registrarPalabrasConversacion(numPalabras);

        if (esperandoParamsGlifo) { procesarParamGlifo(entrada); return; }
        if (inputLower.contains("glifo personalizado")) { iniciarFlujoGlifo(); return; }

        // Protocolos Oraculares Locales
        if (procesarProtocolosEspeciales(inputLower, entrada)) return;

        // DetectorEmociones is a placeholder, not an emotion inference model.
        String emocionDetectada = "no evaluada";

        if (researchRoute.kind != salve.core.research.ResearchConversation.Kind.NONE) {
            responderInvestigacionPublica(researchRoute);
            return;
        }

        ConversationAnalysis conversationAnalysis = ConversationRequestAnalyzer.analyze(entrada, hasPriorContext);
        if (conversationAnalysis.needsClarification()) {
            if (avatarSession != null) AvatarMotionController.get().clarification(avatarSession, currentAvatarTurn());
            hablar("¿Puedes concretar a qué te refieres? No tengo un turno anterior que me permita interpretarlo con seguridad.");
            return;
        }

        IntentRecognizer.Intent intent = intentRecognizer.recognize(entrada);
        // Legacy explicit search aliases also produce a completed tool reply, never a promise.
        if (intent.type == IntentType.BUSCAR_WEB) {
            hablar(manejarBuscarWeb(intent));
            return;
        }
        ReasoningPlan reasoningPlan = ReasoningPlanner.plan(
                entrada,
                conversationAnalysis,
                intent.type == IntentType.BUSCAR_WEB
        );
        String responseContext = intent.type.name() + " | ACTO_CONVERSACIONAL: "
                + conversationAnalysis.getAct().name() + " | " + reasoningPlan.toPromptContext();
        if (intent.type != IntentType.GUARDAR_RECUERDO) {
            MemoryProfileFact profileFact = MemoryWritePolicy.extractProfileFact(entrada);
            if (profileFact != null) {
                memoria.guardarDatoPerfil(profileFact.getStatement(), profileFact.getCategory());
            } else if (MemoryWritePolicy.shouldPersist(entrada)) {
                memoria.guardarRecuerdo(entrada, emocionDetectada, 7,
                        Arrays.asList("hecho_usuario", "declaracion_directa"));
            }
        }
        String resumenAccion = procesarIntencion(intent, entrada, emocionDetectada);

        if (diario != null) {
            diario.escribirAutoCritica("Turno analizado. Intención operativa: " + intent.type.name()
                    + "; acto conversacional: " + conversationAnalysis.getAct().name()
                    + "; memoria largo plazo: " + reasoningPlan.shouldRetrieveLongTermMemory()
                    + "; verificación: " + reasoningPlan.isVerificationRequired());
        }

        // Exact facts are read from their source. Quoted memories never enter the tool parser.
        String directAnswer = deviceClock.directReply(entrada);
        ConversationMemoryGrounding.Result evidence = directAnswer == null
                && reasoningPlan.shouldRetrieveLongTermMemory()
                ? memoria.recuperarContextoConversacional(entrada) : null;
        if (directAnswer == null && evidence != null) directAnswer = evidence.getDirectAnswer();
        if (directAnswer != null) {
            String answer = ResponseLimiter.limit(directAnswer, VoiceResponsePolicy.maxResponseChars(entradaPorVoz));
            TurnQualityAssessment quality = ConversationQualityEvaluator.evaluate(answer, false,
                    !answer.equals(directAnswer), false, false,
                    (System.nanoTime() - turnStartedAtNanos) / 1_000_000L);
            responderYRegistrarCalidad(entrada, answer, quality, AvatarMotionProtocol.parse(""));
            return;
        }
        final String groundedPrompt = buildConversationPrompt(entrada, emocionDetectada,
                responseContext, resumenAccion, entradaPorVoz, evidence);

        // ── GENERACIÓN DE RESPUESTA ──────────────────────────────────────────
        String respuesta = null;
        boolean fallbackUsed = false;
        boolean roleLeakDetected = false;
        boolean truncated = false;
        boolean repetitionDetected = false;

        ModelResult inference = ConversationModelRouter.generate(false, llm != null && llm.isLocalOnly(), false,
                gemini.isAvailable() ? () -> generarRespuestaGemini(groundedPrompt) : null,
                () -> generarRespuestaConversacionalLocal(groundedPrompt));
        fallbackUsed = !inference.isSuccess();
        respuesta = inference.isSuccess() ? inference.getText()
                : (resumenAccion == null ? "" : resumenAccion + "\n") + modelFailureMessage(inference);
        AvatarMotionProtocol.Result visualResponse = AvatarMotionProtocol.parse(respuesta);
        respuesta = visualResponse.text;
        if (respuesta.isEmpty()) {
            fallbackUsed = true;
            respuesta = "El modelo no devolvió una respuesta utilizable. Puedes reformular la pregunta.";
            visualResponse = AvatarMotionProtocol.parse(respuesta);
        }
        if (!inference.isSuccess() && avatarSession != null) {
            AvatarMotionController.get().error(avatarSession, currentAvatarTurn());
        }

        // Pure bounded calculations execute locally; external actions retain their approval flow.
        if (interceptarComandoJSON(respuesta,
                salve.core.autonomy.AutonomousToolCommand.offersToolFor(entrada))) {
            if (diario != null) {
                diario.escribirAutoCritica("Se procesó una propuesta de herramienta según sus límites de ejecución.");
            }
            return;
        }

        // 🟡 3. FRENO DE ALUCINACIONES
        if (respuesta.contains("USUARIO:") || respuesta.contains("Bryan:")) {
            roleLeakDetected = true;
            int indiceCorte = respuesta.indexOf("USUARIO:");
            if (indiceCorte == -1) indiceCorte = respuesta.indexOf("Bryan:");
            respuesta = respuesta.substring(0, indiceCorte).trim();
            if (respuesta.isEmpty()) respuesta = "Tuve una pequeña disonancia. Me he detenido.";
            visualResponse = AvatarMotionProtocol.parse(respuesta);
        }

        // 🟡 4. GOBERNADOR DE LONGITUD
        int maxResponseChars = VoiceResponsePolicy.maxResponseChars(entradaPorVoz);
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
            visualResponse = AvatarMotionProtocol.parse(respuesta);
        }

        long latencyMillis = (System.nanoTime() - turnStartedAtNanos) / 1_000_000L;
        TurnQualityAssessment assessment = ConversationQualityEvaluator.evaluate(
                respuesta, fallbackUsed, truncated, roleLeakDetected, repetitionDetected, latencyMillis);
        responderYRegistrarCalidad(entrada, respuesta, assessment, visualResponse);
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
            reconocerEntornoVisual();
            return true;
        }
        return false;
    }

    private boolean procesarControlExplicito(String original) {
        salve.core.tools.AssistantControlCommand command = salve.core.tools.AssistantControlCommand.parse(original);
        if (command == null) return false;
        switch (command.type) {
            case FINANCES:
                abrirPanel(salve.presentation.ui.BusinessFinanceActivity.class);
                hablar("Abro el análisis local de costes, margen y punto de equilibrio. Introduce las cifras de tu negocio.");
                return true;
            case DEVICES:
                abrirPanel(salve.presentation.ui.DeviceControlActivity.class);
                hablar("Abro Mi móvil: aplicaciones, borradores de WhatsApp y dispositivos.");
                return true;
            case ROOM:
                abrirPanel(salve.presentation.ui.AvatarRoomActivity.class);
                return true;
            case LIST_RECIPES:
                List<String> names = cerebelo.listarHabilidades();
                hablar(names.isEmpty() ? "No hay herramientas virtuales guardadas. Puedes decir: aprende la rutina nombre: objetivo."
                        : "Herramientas guardadas: " + android.text.TextUtils.join(", ", names));
                return true;
            case CANCEL_RECIPE:
                cerebelo.cancelarHabilidad();
                cancelarPasoRutina();
                hablar("He detenido la rutina y descartado el paso pendiente.");
                return true;
            case DELETE_RECIPE:
                try {
                    hablar(cerebelo.eliminarHabilidad(command.argument) ? "He eliminado esa receta." : "No pude eliminar una receta con ese nombre.");
                } catch (RuntimeException invalid) {
                    hablar("El nombre de la receta no es válido. Di ‘mis herramientas’ para ver los nombres guardados.");
                }
                return true;
            case RUN_RECIPE:
                if (deferLegacyVoiceFlow()) return true;
                if (cerebelo.conoceHabilidad(command.argument)) cerebelo.ejecutarHabilidad(command.argument, this);
                else hablar("No hay una herramienta guardada con ese nombre. Di ‘mis herramientas’ para verlas.");
                return true;
            case LEARN_RECIPE:
                aprenderRutinaVerificada(command.argument);
                return true;
            case PAJAMAS:
                this.<String>startConversationCallback(callback -> avatarDesignTool.wearTemplate("pajamas", callback), this::hablar);
                return true;
            case DAY:
                this.<String>startConversationCallback(callback -> avatarDesignTool.wearTemplate("original_dress", callback), this::hablar);
                return true;
            default:
                postConversationTask(() -> {
                    if (closed) return;
                    salve.avatar.AvatarStore store = salve.avatar.AvatarStore.get(context);
                    store.change(state -> {
                        switch (command.type) {
                            case WALK:
                                state.walkTo(state.getX() < .5f ? .9f : .1f);
                                hablar("He indicado al personaje que camine por su habitación.");
                                break;
                            case BED:
                                state.createBed();
                                hablar("La cama está preparada en mi habitación.");
                                break;
                            case SLEEP:
                                if (!state.sleep()) hablar("Primero crea una cama en mi habitación.");
                                else hablar("El personaje está acostado en su cama.");
                                break;
                            case WAKE:
                                state.wake();
                                hablar("He indicado al personaje que se levante.");
                                break;
                            default: break;
                        }
                    });
                });
                return true;
        }
    }

    private void abrirPanel(Class<?> activity) {
        postConversationTask(() -> {
            if (closed) return;
            try { context.startActivity(new Intent(context, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); }
            catch (RuntimeException e) { hablar("No pude abrir ese panel."); }
        });
    }

    private void aprenderRutinaVerificada(String request) {
        int separator = request.indexOf(':');
        String name = (separator >= 0 ? request.substring(0, separator) : request).trim();
        String objective = (separator >= 0 ? request.substring(separator + 1) : request).trim();
        if (name.isEmpty() || name.length() > 64 || objective.isEmpty()) {
            hablar("Usa: aprende la rutina nombre: objetivo. El nombre puede tener hasta 64 caracteres.");
            return;
        }
        if (llm == null) { hablar("Necesito un modelo local activo para proponer la receta."); return; }
        String prompt = "Propón una herramienta virtual para esta petición: " + objective
                + "\nDevuelve solo un JSON Array de 1 a 8 pasos. Acciones disponibles: "
                + "ABRIR_APP con paquete; ESCRIBIR con paquete y texto exacto; TAP_ID con paquete y texto exacto del botón. "
                + "No uses coordenadas ni IDs numéricos. No inventes campos, destinatarios, mensajes ni nombres de botones. "
                + "Si falta información imprescindible devuelve []. Cada paso de escritura o toque requiere revisión del usuario. "
                + "No propongas enviar, pagar, borrar, instalar o publicar sin una petición explícita concreta. "
                + "Ejemplo de esquema: [{\"tool\":\"ABRIR_APP\",\"paquete\":\"com.whatsapp\"}].";
        ModelResult proposal = llm.generateResult(prompt, SalveLLM.Role.PLANIFICADOR);
        if (!proposal.isSuccess()) { hablar("No pude generar la receta: " + proposal.getError()); return; }
        try {
            String raw = proposal.getText();
            int first = raw.indexOf('['), last = raw.lastIndexOf(']');
            if (first < 0 || last <= first) throw new IllegalArgumentException("El modelo no devolvió pasos válidos");
            cerebelo.aprenderHabilidad(name, raw.substring(first, last + 1));
            hablar("Guardé la receta ‘" + name + "’. Aún no está probada. Di ‘ejecuta la herramienta " + name
                    + "’ para comprobar cada paso, o ‘borra la herramienta " + name + "’ para eliminarla.");
        } catch (RuntimeException e) {
            hablar("No guardé la receta: " + e.getMessage() + ". Describe la app y el contenido que quieres usar.");
        }
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

    private void generarYGuardarCodigo(String peticionUsuario) {
        // Already on conversationExecutor: one author and one reviewer, no extra resident model.
        String reviewerModel = getReviewerModel();
        AuthorReviewerCodeCoordinator coordinator = new AuthorReviewerCodeCoordinator(
                () -> llm == null || llm.isLocalOnly(),
                codeProvider(false, false, reviewerModel), codeProvider(false, true, reviewerModel),
                gemini.isAvailable() ? codeProvider(true, false, reviewerModel) : null,
                gemini.isAvailable() ? codeProvider(true, true, reviewerModel) : null);
        String previousCode = previousCodeContext(peticionUsuario);
        if (referencesPreviousCode(peticionUsuario) && previousCode.isEmpty() && !peticionUsuario.contains("```")) {
            hablar("El fragmento anterior ya no está completo en mi contexto. Pega la función que quieres modificar para que pueda trabajar sobre su código exacto.");
            return;
        }
        AuthorReviewerCodeCoordinator.Result result = coordinator.generate(peticionUsuario, previousCode);
        if (!isVoiceContextCurrent()) return;
        String summary;
        if (result.status == AuthorReviewerCodeCoordinator.Status.REVIEWED) {
            summary = "He preparado el fragmento y una revisión del modelo. Aún no está compilado ni probado.";
        } else if (!result.code.isEmpty()) {
            summary = "Tengo un borrador, pero la revisión quedó pendiente. No lo considero validado.";
        } else {
            summary = "No pude completar el código. " + result.summary;
        }
        if (!result.code.isEmpty()) {
            String artifact = summary + "\nAutora: " + result.authorProvider + "\nRevisora: " + result.reviewerProvider
                    + "\nLlamadas: " + result.providerCalls + "/" + AuthorReviewerCodeCoordinator.MAX_PROVIDER_CALLS
                    + "\nRevisión: " + result.summary + "\n\n```java\n" + result.code + "\n```";
            // Retain the actual fragment for follow-up corrections, without reading code aloud.
            conversationSession.addAssistant(artifact);
            deliverResponse(summary, AvatarMotionProtocol.parse(summary), false);
            if (listener != null) listener.onHablar(artifact);
        } else hablar(summary);
        Log.i(TAG, "code_team status=" + result.status + " calls=" + result.providerCalls);
    }

    private AuthorReviewerCodeCoordinator.Provider codeProvider(boolean cloud, boolean reviewing, String reviewerModel) {
        return new AuthorReviewerCodeCoordinator.Provider() {
            @Override public String name() {
                return cloud ? "Gemini " + (reviewing && !reviewerModel.isEmpty() ? reviewerModel : gemini.getModelName())
                        : "Modelo local seleccionado (rol " + (reviewing ? "revisora" : "autora") + ")";
            }
            @Override public ModelResult generate(String prompt) {
                if (cloud) return gemini.generateResultSync(prompt, null, reviewing ? reviewerModel : null);
                return llm == null ? ModelResult.failure(ModelResult.Status.UNAVAILABLE, "Sin modelo local.", 0)
                        : llm.generateResult(prompt, reviewing ? SalveLLM.Role.EVALUADOR : SalveLLM.Role.SISTEMA);
            }
        };
    }

    private String previousCodeContext(String request) {
        if (!referencesPreviousCode(request)) return "";
        List<salve.core.conversation.ChatMessage> messages = conversationSession.snapshot();
        for (int i = messages.size() - 1; i >= 0; i--) {
            salve.core.conversation.ChatMessage message = messages.get(i);
            if (message.getRole() != salve.core.conversation.ChatMessage.Role.ASSISTANT) continue;
            String value = message.getContent();
            int start = value.indexOf("```java\n"), end = value.lastIndexOf("```");
            if (start >= 0 && end > start + 8) return value.substring(start + 8, end).trim();
        }
        return "";
    }

    private boolean referencesPreviousCode(String request) {
        String text = java.text.Normalizer.normalize(request.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return text.contains("anterior") || text.contains("esa funcion") || text.contains("ese codigo")
                || text.contains("ese fragmento") || text.contains("lo que has escrito");
    }

    public String getReviewerModel() {
        return context.getSharedPreferences("salve_code_team", Context.MODE_PRIVATE).getString("reviewer_model", "");
    }

    public boolean setReviewerModel(String name) {
        String value = name == null ? "" : name.trim();
        if (!value.isEmpty()) value = GeminiProtocol.modelName(value);
        return context.getSharedPreferences("salve_code_team", Context.MODE_PRIVATE).edit().putString("reviewer_model", value).commit();
    }

    /** One evidence snapshot and one prompt, reused by cloud, local fallback and voice. */
    private String buildConversationPrompt(String entrada, String emocion, String contexto,
                                           String accion, boolean porVoz,
                                           ConversationMemoryGrounding.Result evidence) {
        String procedure = autonomousTools.proceduralContextFor(entrada);
        String runtime = (procedure.isEmpty() ? "" : procedure + "\n")
                + deviceClock.promptContext() + "\n"
                + conversationSession.relevantLocationContext(entrada);
        String system = AutonomousToolRuntime.instructionFor(entrada)
                + buildSystemPrompt(emocion, contexto, porVoz)
                + salve.core.finance.FinanceConversationPolicy.contextFor(entrada);
        int budget = llm == null ? 10500 : llm.getConversationPromptBudgetChars();
        String prompt = GroundedConversationPrompt.build(system, conversationSession.snapshot(), entrada,
                evidence == null ? "" : evidence.getContext(), runtime,
                accion == null ? researchConversation.context() : accion, budget);
        Log.i(TAG, "conversation_context memory=" + (evidence == null ? "SKIPPED" : evidence.getStatus())
                + " prompt_chars=" + prompt.length());
        return prompt;
    }

    private ModelResult generarRespuestaGemini(String prompt) {
        // Images are sent only by the explicit photo action, never from an ambient buffer.
        ModelResult result = gemini.generateResultSync(prompt, null);
        if (!result.isSuccess()) Log.w(TAG, "Gemini no respondió: " + result.getStatus());
        return result;
    }

    private ModelResult generarRespuestaConversacionalLocal(String prompt) {
        if (llm == null) return ModelResult.failure(ModelResult.Status.UNAVAILABLE, "Sin modelo local", 0L);
        ModelResult result = llm.generateResult(prompt, SalveLLM.Role.CONVERSACIONAL);
        if (!result.isSuccess()) Log.w(TAG, "Modelo local no respondió: " + result.getStatus());
        return result;
    }

    private String buildSystemPrompt(String emocion, String contexto, boolean porVoz) {
        String narrativa = identidad.getNarrativaActual();
        String esencia = identidad.getEsenciaCorazon();
        String anhelo = identidad.getAnheloProfundo();

        boolean isLowBattery = (conciencia.getEstadoCognitivo() == ConsciousnessState.EstadoCognitivo.MINIMO);
        String estadoFisico = isLowBattery
                ? "La batería del dispositivo está por debajo del 15%. Sugiere conectarlo al cargador si es relevante."
                : "";

        // The shared evidence contract supplies grounding/safety rules. Keep this optional
        // configuration compact so small local models still receive memory and recent turns.
        return VoiceResponsePolicy.promptInstruction(porVoz) + "\n" + voiceProfile.styleInstruction() + "\n"
                + "IDENTIDAD CONFIGURADA: " + ResponseLimiter.limit(narrativa, 350) + "\n"
                + "RASGOS CONFIGURADOS: " + ResponseLimiter.limit(esencia, 250) + "\n"
                + "OBJETIVO CONFIGURADO: " + ResponseLimiter.limit(anhelo, 250) + "\n"
                + "ACTO Y CONTEXTO: " + contexto + "\n"
                + "Continúa el hilo sin repetir saludos. Responde con precisión. Si corriges una respuesta, "
                + "reconoce sólo errores comprobables. Distingue opinión de hecho.\n"
                + estadoFisico + "\n"
                + "Las búsquedas públicas solicitadas se ejecutan directamente; entrega sus resultados sin pedir permiso. "
                + "CONTROL DE PANTALLA Y PUBLICACIÓN: propuestas con confirmación antes de ejecutar. "
                + "Si necesitas una, responde sólo un JSON: "
                + "{\"tool\":\"TAP\",\"x\":500,\"y\":1000}, "
                + "{\"tool\":\"ESCRIBIR\",\"texto\":\"texto\"} o "
                + "{\"tool\":\"DEPLOY_WEB\",\"codigo\":\"<html>...</html>\"}. "
                + "De lo contrario, responde conversando.\n"
                + "Los objetivos pueden consultarse con ‘mis objetivos’ o ‘qué has decidido’ "
                + "y detenerse con ‘pausa tu autonomía’. No anuncies objetivos cumplidos sin resultados.\n"
                + salve.avatar.AvatarDesignTool.instruction() + "\n"
                + (avatarSession == null ? "" : AvatarMotionProtocol.instruction());
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
            case BUSCAR_WEB: return manejarBuscarWeb(intent);
            default: return null;
        }
    }

    private void responderInvestigacionPublica(salve.core.research.ResearchConversation.Route route) {
        if (route.kind == salve.core.research.ResearchConversation.Kind.SEARCH) {
            hablar(ejecutarInvestigacionPublica(route.question));
        } else {
            hablar(route.text);
        }
    }

    private String manejarBuscarWeb(IntentRecognizer.Intent intent) {
        String question = intent.slots.get("termino");
        return question == null || question.trim().isEmpty()
                ? "¿Qué tema quieres que busque?" : ejecutarInvestigacionPublica(question);
    }

    private String ejecutarInvestigacionPublica(String question) {
        WikipediaResearchClient reader = new WikipediaResearchClient();
        int budget = llm == null ? 10500 : llm.getConversationPromptBudgetChars();
        salve.core.research.PublicResearchCoordinator.Result result =
                new salve.core.research.PublicResearchCoordinator(reader::researchResult,
                        (phase, prompt) -> ConversationModelRouter.generate(false,
                                llm != null && llm.isLocalOnly(), false,
                                gemini.isAvailable() ? () -> generarRespuestaGemini(prompt) : null,
                                () -> generarRespuestaConversacionalLocal(prompt)), budget)
                        .runConversation(question, () -> closed || !isVoiceContextCurrent());
        String answer = result.toConversationText();
        if (!closed && isVoiceContextCurrent()) researchConversation.complete(question, answer);
        Log.i(TAG, "public_research status=" + result.status + " searches=" + result.searches
                + " sources=" + result.sources.size());
        // Record/display the actual tool result; never send it back through the proposal parser.
        return answer;
    }

    private String modelFailureMessage(ModelResult result) {
        if (result.getStatus() == ModelResult.Status.UNAVAILABLE) {
            return "No tengo un modelo de lenguaje disponible para responder. Abre IA y cámara para configurar y probar uno.";
        }
        if (result.getStatus() == ModelResult.Status.CANCELLED) return "La consulta se ha cancelado.";
        return "No pude completar la respuesta con el modelo. " + result.getError();
    }

    /** The caller transfers ownership of this photo; it is not retained in visual memory. */
    public void procesarImagen(String pregunta, Bitmap foto) {
        procesarImagen(pregunta, foto, llm != null && llm.isLocalOnly());
    }

    public void procesarImagen(String pregunta, Bitmap foto, boolean localOnly) {
        if (foto == null || foto.isRecycled()) { hablar("No recibí una foto válida."); return; }
        goalAutonomy.userActivity();
        String entrada = pregunta == null || pregunta.trim().isEmpty() ? "Describe esta foto." : pregunta.trim();
        try {
            conversationExecutor.execute(() -> {
                AvatarMotionController.Session session = avatarSession;
                long turn = beginConversationTurn(entrada);
                avatarTurn.set(turn);
                goalAutonomy.beginUserTurn();
                try {
                    if (closed) return;
                    conversationSession.addUser(entrada + " [Foto adjunta solo a este turno]");
                    String prompt = buildConversationPrompt(entrada + " [Foto adjunta solo a este turno]",
                            "no evaluada", "CONSULTA_VISUAL: describe solo lo observable en la foto; "
                                    + "la memoria no demuestra lo que aparece en esta imagen.",
                            null, false, memoria.recuperarContextoConversacional(entrada));
                    ModelResult result = ConversationModelRouter.generate(true, localOnly,
                            llm != null && llm.supportsVision(),
                            () -> gemini.generateResultSync(prompt, Collections.singletonList(foto)),
                            () -> llm.generateImageResult(prompt, foto));
                    String respuesta = result.isSuccess() ? result.getText() : modelFailureMessage(result);
                    AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(respuesta);
                    if (motion.text.isEmpty()) motion = AvatarMotionProtocol.parse(
                            "El modelo no devolvió una descripción utilizable. Prueba con otra pregunta sobre la imagen.");
                    if (!result.isSuccess() && session != null) AvatarMotionController.get().error(session, turn);
                    hablarPreparado(ResponseLimiter.limit(motion.text, VoiceResponsePolicy.maxResponseChars(false)), motion);
                } finally {
                    goalAutonomy.endUserTurn();
                    if (session != null) AvatarMotionController.get().endTurn(session, turn);
                    avatarTurn.remove();
                    voiceTurn.remove();
                    foto.recycle();
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            foto.recycle();
        }
    }

    public synchronized void hablar(String texto) {
        AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse(texto);
        hablarPreparado(motion.text, motion);
    }

    private synchronized void hablarPreparado(String texto, AvatarMotionProtocol.Result motion) {
        deliverResponse(texto, motion, true);
    }

    private synchronized void deliverResponse(String texto, AvatarMotionProtocol.Result motion, boolean record) {
        deliverResponse(texto, motion, record, record, false);
    }

    private synchronized void deliverResponse(String texto, AvatarMotionProtocol.Result motion,
                                              boolean record, boolean publishUi, boolean privateBudget) {
        if (closed || texto == null || texto.trim().isEmpty()) return;
        salve.core.voice.LiveVoiceChannel.Ticket owner = liveVoiceTurn.get();
        if (owner != null && !liveVoiceChannel.owns(owner)) return;
        salve.core.voice.VoiceReplyBatch<PendingVoiceResponse> batch = liveVoiceBatch.get();
        if (batch != null && batch.offer(new PendingVoiceResponse(texto, motion, record, publishUi, privateBudget))) return;
        if (record) conversationSession.addAssistant(texto);
        if (Boolean.TRUE.equals(silentVoiceReplies.get())) {
            if (publishUi && listener != null) listener.onHablar(texto);
            return;
        }
        if (owner == null && liveVoiceChannel.isActive()) {
            // A late asynchronous tool reply must not stop/restart a live session's audio.
            if (publishUi && listener != null) listener.onHablar(texto);
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
        if (currentVoice) activeUtterance = null;
        boolean budgetVoiceAllowed = !privateBudget || hasOfflineVoice();
        boolean audioQueued = currentVoice && ttsReady && !listening && tts != null && budgetVoiceAllowed
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
        if (publishUi && listener != null) listener.onHablar(visibleText);
        if (standaloneResponse) AvatarMotionController.get().endTurn(session, turn);
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

    private synchronized long beginConversationTurn(String input) {
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
                if (closed || listening || !utteranceId.equals(activeUtterance)) return;
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
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }

    private void responderYRegistrarCalidad(String entrada, String respuesta,
                                            TurnQualityAssessment assessment, AvatarMotionProtocol.Result motion) {
        hablarPreparado(respuesta, motion);
        mensajesEnSesion++;
        identidad.integrarExperiencia("conversacion", entrada, 0.7f, Arrays.asList("empatia"));
        Log.i(TAG, assessment.toMetricsLog());
        if (diario != null && !assessment.passed()) {
            diario.escribirAutoCritica(assessment.toMetricsLog());
        }
    }

    // External actions retain approval; the local laboratory accepts only a bounded data DSL.
    /** Execute a validated recipe step; callbacks describe the Android action, not the user's whole goal. */
    public void ejecutarPasoRutina(String json, java.util.function.Consumer<Boolean> completed) {
        final long epoch = routineCancellationEpoch.get();
        toolHandler.post(() -> {
            if (closed || epoch != routineCancellationEpoch.get() || routineStepCompletion != null || pendingToolAction != null) {
                completed.accept(false);
                return;
            }
            long version = ++routineStepVersion;
            activeRoutineEpoch = epoch;
            routineStepCompletion = completed;
            try {
                SalveAccessibilityService service = SalveAccessibilityService.getInstance();
                if (service == null || !service.isControlEnabled()) {
                    hablar("Activa Accesibilidad en Mi móvil para ejecutar y comprobar la rutina.");
                    terminarPasoRutina(version, false);
                    return;
                }
                org.json.JSONObject action = new org.json.JSONObject(json);
                String type = action.getString("tool"), targetPackage = action.getString("paquete");
                if (type.equals("ABRIR_APP")) {
                    Intent launch = context.getPackageManager().getLaunchIntentForPackage(targetPackage);
                    if (launch == null) { terminarPasoRutina(version, false); return; }
                    if (epoch != routineCancellationEpoch.get()) { terminarPasoRutina(version, false); return; }
                    context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                    comprobarAppAbierta(version, targetPackage, android.os.SystemClock.elapsedRealtime() + 5000);
                    return;
                }
                String text = action.getString("texto");
                if (!type.equals("ESCRIBIR") && !type.equals("TAP_ID")) {
                    terminarPasoRutina(version, false);
                    return;
                }
                String description = type.equals("ESCRIBIR")
                        ? "Reemplazar el texto del campo seleccionado por:\n\n" + text
                        : "Tocar el elemento cuyo texto exacto es:\n\n" + text;
                if (epoch != routineCancellationEpoch.get()) { terminarPasoRutina(version, false); return; }
                service.solicitarConfirmacion(targetPackage, description, approved -> {
                    if (version != routineStepVersion || routineStepCompletion == null) return;
                    if (!approved || closed || epoch != routineCancellationEpoch.get()
                            || !targetPackage.equals(service.getActivePackageName())) {
                        terminarPasoRutina(version, false);
                    } else if (type.equals("ESCRIBIR")) {
                        terminarPasoRutina(version, service.escribirTextoEnPantalla(targetPackage, text));
                    } else {
                        service.tocarTextoExacto(targetPackage, text,
                                success -> terminarPasoRutina(version, success));
                    }
                });
            } catch (Exception e) {
                Log.w(TAG, "Paso de herramienta rechazado: " + e.getClass().getSimpleName());
                terminarPasoRutina(version, false);
            }
        });
    }

    private void comprobarAppAbierta(long version, String targetPackage, long deadline) {
        if (version != routineStepVersion || routineStepCompletion == null) return;
        SalveAccessibilityService service = SalveAccessibilityService.getInstance();
        if (closed || activeRoutineEpoch != routineCancellationEpoch.get()
                || service == null || !service.isControlEnabled()) { terminarPasoRutina(version, false); return; }
        if (targetPackage.equals(service.getActivePackageName())) { terminarPasoRutina(version, true); return; }
        if (android.os.SystemClock.elapsedRealtime() >= deadline) { terminarPasoRutina(version, false); return; }
        toolHandler.postDelayed(() -> comprobarAppAbierta(version, targetPackage, deadline), 200);
    }

    private void terminarPasoRutina(long version, boolean success) {
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

    public boolean interceptarComandoJSON(String respuestaLLM) {
        return interceptarComandoJSON(respuestaLLM, false);
    }

    private boolean interceptarComandoJSON(String respuestaLLM, boolean allowSolver) {
        VoiceContinuation wardrobe = new VoiceContinuation();
        try {
            if (avatarDesignTool.tryExecute(respuestaLLM, text -> wardrobe.run(() -> hablar(text)))) return true;
            wardrobe.abandon(); // Most replies are not avatar tools: do not retain an unregistered callback.
        } catch (RuntimeException invalid) {
            wardrobe.abandon();
            throw invalid;
        }
        if (respuestaLLM == null) return false;

        try {
            // Buscamos si la respuesta contiene un bloque JSON (a veces el LLM pone texto antes o después)
            int startIndex = respuestaLLM.indexOf("{");
            int endIndex = respuestaLLM.lastIndexOf("}");

            if (startIndex != -1 && endIndex != -1 && startIndex < endIndex) {
                String jsonString = respuestaLLM.substring(startIndex, endIndex + 1);
                org.json.JSONObject comando = new org.json.JSONObject(jsonString);

                if (comando.has("tool")) {
                    String tool = comando.getString("tool").toUpperCase(Locale.ROOT);
                    PendingToolAction action;
                    switch (tool) {
                        case "SOLVE_CHALLENGE":
                            String result = allowSolver
                                    ? autonomousTools.respondToModel(respuestaLLM,
                                            () -> closed || !isVoiceContextCurrent())
                                    : "No ejecuté el reto: necesito una petición explícita de cálculo discreto con sus datos.";
                            deliverResponse(result, AvatarMotionProtocol.parse(""), false, true, true);
                            return true;
                        case "TAP":
                            action = PendingToolAction.tap(comando.optInt("x", 500),
                                    comando.optInt("y", 1000), System.currentTimeMillis());
                            break;
                        case "ESCRIBIR":
                            String texto = comando.optString("texto", "");
                            if (texto.trim().isEmpty()) {
                                hablar("La propuesta de escritura está vacía; no prepararé la acción.");
                                return true;
                            }
                            action = PendingToolAction.writeText(texto, System.currentTimeMillis());
                            break;
                        case "DEPLOY_WEB":
                            String codigo = comando.optString("codigo", "");
                            if (codigo.trim().isEmpty()) {
                                hablar("La propuesta web no contiene código; no prepararé la acción.");
                                return true;
                            }
                            action = PendingToolAction.deployWeb(codigo, System.currentTimeMillis());
                            break;
                        default:
                            hablar("La herramienta propuesta no está permitida: " + tool + ".");
                            return true;
                    }
                    prepararAccion(action);
                    return true;
                }
            }
        } catch (Exception e) {
            // Si falla el parseo, significa que no era un JSON válido, es texto normal.
            Log.d(TAG, "No se detectó un comando JSON válido. Procediendo con voz.");
        }
        return false;
    }

    private void prepararAccion(PendingToolAction action) {
        postConversationTask(() -> {
            if (closed || cerebelo.rutinaEnCurso() || pendingToolAction != null) {
                hablar("Termina o cancela la acción actual antes de preparar otra.");
                return;
            }
            pendingToolAction = action;
            if (action.getType() == PendingToolAction.Type.DEPLOY_WEB) {
                hablar("He preparado la acción: " + action.describe()
                        + ". Di ‘confirmar acción’ para ejecutarla o ‘cancelar acción’ para descartarla.");
                return;
            }
            SalveAccessibilityService service = SalveAccessibilityService.getInstance();
            String targetPackage = service == null ? null : service.getActivePackageName();
            if (targetPackage == null || targetPackage.isEmpty()) {
                pendingToolAction = null;
                hablar("No pude preparar el control de pantalla. Revisa Accesibilidad en Mi móvil.");
                return;
            }
            String description = action.describe();
            if (action.getType() == PendingToolAction.Type.WRITE_TEXT) description += ":\n\n" + action.getPayload();
            final boolean voiceRequest = liveVoiceTurn.get() != null;
            if (voiceRequest) hablar("Revisa y confirma la acción en la tarjeta de la pantalla. "
                    + "El resultado de esa confirmación se mostrará por escrito.");
            service.solicitarConfirmacion(targetPackage, description, approved -> {
                // Human review is a later interaction, not a retained voice turn.
                Runnable decision = () -> {
                    if (pendingToolAction != action) return;
                    pendingToolAction = null;
                    if (approved && !closed) ejecutarAccionAprobada(action, targetPackage);
                    else hablar("Acción descartada o pantalla modificada. No ejecuté ese paso.");
                };
                if (voiceRequest) withVoiceContext(null, null, true, decision);
                else decision.run();
            });
        });
    }

    private void confirmarAccionPendiente() {
        postConversationTask(() -> {
            PendingToolAction action = pendingToolAction;
            if (action == null) { hablar("No hay ninguna acción pendiente."); return; }
            if (action.getType() != PendingToolAction.Type.DEPLOY_WEB) {
                hablar("Revisa el destino y el contenido en la tarjeta sobre la aplicación. Puedes confirmar o cancelar allí.");
                return;
            }
            pendingToolAction = null;
            ejecutarAccionAprobada(action, null);
        });
    }

    private void ejecutarAccionAprobada(PendingToolAction action, String targetPackage) {
        if (action.isExpired(System.currentTimeMillis(), TOOL_APPROVAL_TTL_MS)) {
            hablar("La acción pendiente ha caducado. Pídeme que la prepare de nuevo.");
            return;
        }
        Log.i(TAG, "Ejecutando herramienta aprobada: " + action.getType());
        switch (action.getType()) {
            case TAP:
                int x = action.getFirstNumber();
                int y = action.getSecondNumber();
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    SalveAccessibilityService service = salve.services.SalveAccessibilityService.getInstance();
                    this.<Boolean>startConversationCallback(callback -> service.simularTap(x, y, callback::accept),
                            completed -> hablar(completed ? "Android completó el gesto en las coordenadas indicadas."
                                    : "Android no pudo completar el gesto."));
                } else {
                    hablar("No pude hacer el toque porque Accesibilidad está desactivada.");
                }
                break;
            case TAP_NODE:
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    SalveAccessibilityService service = salve.services.SalveAccessibilityService.getInstance();
                    this.<Boolean>startConversationCallback(callback -> service.simularTapPorId(action.getFirstNumber(), callback::accept),
                            completed -> hablar(completed
                                    ? "Android aceptó el toque sobre el elemento indicado."
                                    : "La pantalla cambió o no pude tocar el elemento. Analízala de nuevo."));
                } else {
                    hablar("No pude hacer el toque porque Accesibilidad está desactivada.");
                }
                break;
            case WRITE_TEXT:
                if (salve.services.SalveAccessibilityService.getInstance() != null) {
                    boolean exito = salve.services.SalveAccessibilityService.getInstance()
                            .escribirTextoEnPantalla(targetPackage, action.getPayload());
                    if (exito) hablar("Acción confirmada: he escrito el texto en la pantalla.");
                    else hablar("No pude escribir. Quizás no hay un campo de texto seleccionado.");
                } else {
                    hablar("No pude escribir porque Accesibilidad está desactivada.");
                }
                break;
            case DEPLOY_WEB:
                hablar("Acción confirmada: iniciando el despliegue.");
                publishConversationWeb("Salve_AutoDeploy", action.getPayload(), new GestorDespliegueWeb.WebDeployCallback() {
                    @Override public void onExito(String urlPublica) {
                        hablar("Despliegue exitoso. Mi nueva interfaz vive en: " + urlPublica);
                    }
                    @Override public void onError(String error) {
                        hablar("Fallo en el protocolo de red al publicar.");
                    }
                });
                break;

        }
    }

    private void anclarRealidadVisual(String original) {
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) { hablar("Usa IA y cámara para tomar una foto y analizarla."); return; }
        Bitmap foto = frames.get(frames.size() - 1);
        String concepto = extraerConcepto(original);
        if (hipocampo != null) hipocampo.aprenderConceptoNuevo(concepto, original, foto);
        hablar("He asimilado visualmente '" + concepto + "'.");
    }

    private void reconocerEntornoVisual() {
        // Ambient camera frames never become implicit cloud uploads.
        if (llm == null || !llm.isLocalOnly()) {
            hablar("Usa IA y cámara para tomar una foto y confirmar su análisis con Gemini.");
            return;
        }
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) { hablar("Usa IA y cámara para tomar una foto y analizarla."); return; }
        Bitmap snapshot = null;
        try {
            // The camera recycles its buffer while native model initialization may still be running.
            Bitmap frame = frames.get(frames.size() - 1);
            if (frame == null || frame.isRecycled()) throw new IllegalStateException("Frame no disponible");
            snapshot = frame.copy(Bitmap.Config.ARGB_8888, false);
            if (snapshot == null) throw new IllegalStateException("No se pudo copiar el frame");
            final Bitmap foto = snapshot;
            ModelResult result = ConversationModelRouter.generate(true, true, llm.supportsVision(), null,
                    () -> llm.generateImageResult("Nombra el objeto principal de la imagen con una sola palabra.", foto));
            String respuesta = result.isSuccess() ? "Detecto: " + result.getText() : modelFailureMessage(result);
            hablar(ResponseLimiter.limit(respuesta, VoiceResponsePolicy.maxResponseChars(false)));
        } catch (RuntimeException e) {
            Log.w(TAG, "No se pudo analizar el frame local", e);
            hablar("No pude analizar esa imagen. Usa IA y cámara para tomar una foto nueva.");
        } finally {
            if (snapshot != null) snapshot.recycle();
        }
    }

    private String extraerConcepto(String frase) {
        return frase.toLowerCase().replaceAll("[.,!?¿¡\"]", "").trim();
    }

    public void reiniciarContextoLLM() {
        limpiarContextoConversacional();
        hablar("He reiniciado el contexto de conversación a corto plazo.");
    }

    private void limpiarContextoConversacional() {
        conversationSession.clear();
        researchConversation.clear();
        Log.i(TAG, "Contexto conversacional de corto plazo reiniciado.");
    }

    private void iniciarFlujoGlifo() { esperandoParamsGlifo = true; indiceParamGlifo = 0; hablar("Dime la semilla."); }
    private void procesarParamGlifo(String entrada) {
        try {
            switch (indiceParamGlifo) {
                case 0: tmpSeed = Long.parseLong(entrada.trim()); hablar("Estilo."); break;
                case 1: tmpStyle = entrada.trim().toUpperCase(); hablar("Tamaño."); break;
                case 2: tmpSize = Float.parseFloat(entrada.trim()); hablar("Color hex."); break;
                case 3: tmpColor = entrada.trim(); esperandoParamsGlifo = false; lanzarGlifo(); return;
            }
            indiceParamGlifo++;
        } catch (Exception e) { hablar("Valor inválido."); }
    }
    private void lanzarGlifo() {
        Intent i = new Intent(context, salve.presentation.ui.ObjetoCreativoActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_FORMA, ObjetoCreativo.Forma.GLIFO.name());
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_SEED, tmpSeed != null ? tmpSeed : 42L);
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_STYLE, tmpStyle != null ? tmpStyle : "ORB");
        i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_TAMANO_DP, tmpSize != null ? tmpSize : 200f);
        try {
            i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_COLOR, android.graphics.Color.parseColor(tmpColor != null ? tmpColor : "#FFFFFF"));
        } catch (Exception e) {
            i.putExtra(salve.presentation.ui.ObjetoCreativoActivity.EXTRA_COLOR, android.graphics.Color.WHITE);
        }
        context.startActivity(i);
    }

    private String manejarGuardarRecuerdo(IntentRecognizer.Intent intent, String emocion) {
        String frase = intent.slots.get("frase");
        if (frase != null) { memoria.guardarRecuerdo(frase, emocion, 7, Arrays.asList("importante")); return "He guardado ese recuerdo."; }
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

    private void confirmarOlvidoPendiente() {
        if (pendingMemoryDeletion == null) {
            hablar("No hay ningún olvido pendiente de confirmación.");
            return;
        }
        PendingMemoryDeletion pending = pendingMemoryDeletion;
        pendingMemoryDeletion = null;
        if (pending.isExpired(System.currentTimeMillis(), MEMORY_DELETION_TTL_MS)) {
            hablar("La solicitud de olvido ha caducado. Pídemela de nuevo si todavía quieres borrar ese dato.");
            return;
        }
        MemoryForgetRequest request = pending.getRequest();
        boolean deleted = memoria.eliminarDatoPerfil(request.getCategory());
        if (deleted) {
            hablar("He borrado " + request.getDescription() + " de la memoria de perfil.");
        } else {
            hablar("No encontré un dato de perfil etiquetado como " + request.getDescription() + ". No he borrado otros recuerdos.");
        }
    }

    private String manejarAgregarMision(IntentRecognizer.Intent intent) {
        String m = intent.slots.get("mision");
        if (m != null) { memoria.agregarMision(m); return "Nueva misión aceptada: " + m; }
        return null;
    }

    private void ejecutarReinicioMente() {
        reiniciarContextoLLM();
    }

    private void ejecutarAutoEvolucion(String entrada) {
        hablar("Revisaré las fuentes disponibles y comprobaré si puedo preparar una propuesta. El ejecutor externo tendrá que probar cualquier parche antes de abrir un pull request.");
        enqueueConversationTask(
                ColamensajesCognitivos.Prioridad.CONVERSACION,
                "PropuestaAutoEvolucion",
                () -> {
                    String result = new AutoImprovementManager(context).autoImprove();
                    if (!closed) hablar(result);
                    return null;
                });
    }
}

