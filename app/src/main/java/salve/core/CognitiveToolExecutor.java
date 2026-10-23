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
 * Ejecuta las herramientas que el modelo pide en un turno. El catálogo (CognitiveToolCatalog) decide cuáles hay; aquí se reparten: dibujo al taller, web a la mesa de investigación, y memoria, nube, estado, pantalla y vestuario aquí.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class CognitiveToolExecutor {
    private final MotorConversacional m;

    CognitiveToolExecutor(MotorConversacional m) { this.m = m; }

    /** Existing Android/UI executors remain here; registry validates tool name and
     * allowed argument keys before invoking any operation. External effects still
     * use their specific confirmation or per-tool policy. */
    CognitiveTurn.Receipt executeCognitiveTool(String name, JsonObject args, String input) {
        switch (name) {
            case "memory.lookup": {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Sin autorización para recuerdos privados");
                String requested = args.has("query") && args.get("query").isJsonPrimitive()
                        && args.get("query").getAsJsonPrimitive().isString()
                        ? args.get("query").getAsString() : null;
                String scoped = salve.core.conversation.MemoryProbePolicy.scoped(
                        input, m.conversationSession.snapshot(), requested);
                ConversationMemoryGrounding.Result result = m.memoria.recuperarContextoConversacional(scoped);
                return new CognitiveTurn.Receipt(result.getStatus().name() + "\n" + result.getDataContext());
            }
            case "web.search":
                return m.mesaInvestigacion.herramientaBuscarWeb(CognitiveReply.string(args, "query").trim(), input);
            case "sketch.draw":
                return m.dibujo.herramientaDibujar(CognitiveReply.string(args, "motivos").trim());
            case "cloud.sync": {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Nube privada requiere identidad comprobada");
                if (!salve.data.sync.CloudSyncManager.isPCloudConfigured(m.context))
                    return new CognitiveTurn.Receipt("NUBE_SIN_CONECTAR: pCloud no está conectada; Bryan debe autorizarla una vez en IA y cámara → Nube pCloud.");
                if (!salve.data.sync.CloudSyncManager.isEnabled(m.context)
                        && salve.data.sync.CloudSyncManager.pausedByUser(m.context))
                    return new CognitiveTurn.Receipt("NUBE_EN_PAUSA: Bryan la pausó; no la reactives sin que te lo pida.");
                if (!salve.data.sync.CloudSyncManager.isEnabled(m.context))
                    salve.data.sync.CloudSyncManager.setEnabled(m.context, true, false);
                salve.data.sync.CloudSyncManager.RestoreBatch batch =
                        salve.data.sync.CloudSyncManager.restoreMemoryBatch(m.context, 120);
                int files = salve.data.sync.CloudSyncManager.syncKnowledge(m.context);
                boolean mindRestored = salve.data.sync.CloudSyncManager.restoreMind(m.context, false);
                salve.data.sync.SyncWorker.restartNow(m.context);
                m.registrarAccion(salve.core.mind.SelfActionLog.Kind.SEARCH, "sincronizar con pCloud",
                        "traje " + batch.restored + " recuerdos y moví " + files + " archivos de lo que aprendí");
                return new CognitiveTurn.Receipt("NUBE_SINCRONIZADA: " + batch.restored
                        + " recuerdos nuevos; " + files + " archivos de dibujos/acciones sincronizados"
                        + (mindRestored ? "; mente restaurada" : "") + ". "
                        + salve.data.sync.CloudSyncManager.memoryStatus(m.context)
                        + (batch.more ? " Quedan más: sigo en segundo plano." : ""));
            }
            case salve.core.schema.Abstraction.TOOL: {
                String concept = CognitiveReply.string(args, "concepto").trim();
                if (concept.isEmpty() || concept.length() > 60) throw new IllegalArgumentException("Concepto vacío o demasiado largo");
                salve.core.schema.ConceptStore.Concept c = m.mind().abstractConcept(concept, input, true);
                if (c == null) {
                    // Sin anclar: al menos los esquemas que delata la pregunta, con sus consecuencias genéricas.
                    StringBuilder hint = new StringBuilder("SIN_ANCLAR: no pude descomponer «" + concept + "» ahora; lo haré al soñar.");
                    for (salve.core.schema.ImageSchemas.Schema s : salve.core.schema.ImageSchemas.detect(input))
                        hint.append("\n- ").append(s.label).append(": ").append(String.join("; ", s.infer(null, null)));
                    return new CognitiveTurn.Receipt(hint.toString());
                }
                m.registrarAccion(salve.core.mind.SelfActionLog.Kind.LEARN_LESSON, "pensar «" + concept + "» por dentro",
                        "lo descompuse en " + c.schemas.size() + (c.schemas.size() == 1 ? " esquema" : " esquemas"));
                return new CognitiveTurn.Receipt(salve.core.schema.ConceptStore.reasoning(c));
            }
            case "runtime.status": {
                ModelResult last = m.lastConversationResult;
                return new CognitiveTurn.Receipt(m.cognitiveState() + "\n" + m.getModelStatusDescription()
                        + (last == null ? "" : "\nÚltima inferencia: " + last.getStatus()
                        + "; latencia_ms=" + last.getLatencyMillis()));
            }
            case "SOLVE_CHALLENGE": {
                JsonObject challenge = args.getAsJsonObject("challenge");
                String family = salve.core.autonomy.AutonomousToolCommand.offeredFamilyFor(input);
                if (!family.equals(CognitiveReply.string(challenge, "family")))
                    throw new IllegalArgumentException("Familia distinta");
                JsonObject legacy = args.deepCopy();
                legacy.addProperty("tool", "SOLVE_CHALLENGE");
                String result = m.autonomousTools.respondToModel(legacy.toString(),
                        () -> m.closed || !m.isVoiceContextCurrent());
                m.deliverResponse(result, AvatarMotionProtocol.parse(""), false, true, true);
                return CognitiveTurn.Receipt.handoff();
            }
            case "TAP": {
                m.prepararAccion(PendingToolAction.tap(coordinate(args, "x"),
                        coordinate(args, "y"), System.currentTimeMillis()));
                return CognitiveTurn.Receipt.handoff();
            }
            case "ESCRIBIR": {
                m.prepararAccion(PendingToolAction.writeText(requiredText(args, "texto", 2000),
                        System.currentTimeMillis()));
                return CognitiveTurn.Receipt.handoff();
            }
            case "DEPLOY_WEB": {
                m.prepararAccion(PendingToolAction.deployWeb(requiredText(args, "codigo", 20000),
                        System.currentTimeMillis()));
                return CognitiveTurn.Receipt.handoff();
            }
            case salve.core.mind.SelfFunctions.TOOL: {
                if (!salve.core.identity.IdentityManager.get(m.context).privateAccess())
                    throw new IllegalArgumentException("Solo para Bryan verificado");
                String action = salve.core.mind.SelfFunctions.action(CognitiveReply.string(args, "accion"));
                if (action == null) throw new IllegalArgumentException("Función desconocida");
                // La función responde ella misma (con sus datos reales) y el turno termina aquí.
                switch (action) {
                    case "estado_memoria": m.hablar(m.mind().memoryHealth()); break;
                    case "examen": m.hablar(m.examen.run()); break;
                    case "mapa_mente": m.hablar(m.abrirMapaMente()); break;
                    default: m.hablar(m.examen.proposals());
                }
                return CognitiveTurn.Receipt.handoff();
            }
            case "AVATAR_LIST":
            case "AVATAR_WEAR":
            case "AVATAR_CREATE":
                return avatarAction(name, args);
            default:
                throw new IllegalArgumentException("Herramienta no autorizada");
        }
    }

    CognitiveTurn.Receipt avatarAction(String name, JsonObject args) {
        if (args.has("tool")) throw new IllegalArgumentException("Campo no permitido");
        JsonObject legacy = args.deepCopy(); legacy.addProperty("tool", name);
        salve.avatar.AvatarDesignRequest.parse(legacy.toString()); // validates BEFORE dispatch
        MotorConversacional.VoiceContinuation continuation = m.new VoiceContinuation();
        boolean accepted = m.avatarDesignTool.tryExecute(legacy.toString(), text -> continuation.run(() -> m.hablar(text)));
        if (!accepted) { continuation.abandon(); throw new IllegalArgumentException("Vestuario no disponible"); }
        return CognitiveTurn.Receipt.handoff();
    }

    static int coordinate(JsonObject args, String key) {
        if (!args.get(key).isJsonPrimitive() || !args.getAsJsonPrimitive(key).isNumber()) throw new IllegalArgumentException("Coordenada inválida");
        int value = args.get(key).getAsBigDecimal().intValueExact();
        if (value < 0 || value > 10000) throw new IllegalArgumentException("Coordenada fuera de rango");
        return value;
    }

    static String requiredText(JsonObject args, String key, int maximum) {
        String text = CognitiveReply.string(args, key);
        if (text.trim().isEmpty() || text.length() > maximum) throw new IllegalArgumentException("Texto vacío o demasiado largo");
        return text;
    }
}
