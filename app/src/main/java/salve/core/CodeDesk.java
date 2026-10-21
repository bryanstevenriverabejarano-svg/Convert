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
 * Escribir código cuando Bryan lo pide: autor y revisor, con el código anterior como contexto.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class CodeDesk {
    private final MotorConversacional m;

    CodeDesk(MotorConversacional m) { this.m = m; }

    void generarYGuardarCodigo(String peticionUsuario) {
        // Already on conversationExecutor: one author and one reviewer, no extra resident model.
        String reviewerModel = getReviewerModel();
        AuthorReviewerCodeCoordinator coordinator = new AuthorReviewerCodeCoordinator(
                () -> m.llm == null || m.llm.usesOwnedInference(),
                codeProvider(false, false, reviewerModel), codeProvider(false, true, reviewerModel),
                m.gemini.isAvailable() ? codeProvider(true, false, reviewerModel) : null,
                m.gemini.isAvailable() ? codeProvider(true, true, reviewerModel) : null);
        String previousCode = previousCodeContext(peticionUsuario);
        if (referencesPreviousCode(peticionUsuario) && previousCode.isEmpty() && !peticionUsuario.contains("```")) {
            m.hablar("El fragmento anterior ya no está completo en mi contexto. Pega la función que quieres modificar para que pueda trabajar sobre su código exacto.");
            return;
        }
        AuthorReviewerCodeCoordinator.Result result = coordinator.generate(peticionUsuario, previousCode);
        if (!m.isVoiceContextCurrent()) return;
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
            m.conversationSession.addAssistant(artifact);
            m.deliverResponse(summary, AvatarMotionProtocol.parse(summary), false);
            if (m.listener != null) m.listener.onHablar(artifact);
        } else m.hablar(summary);
        Log.i(MotorConversacional.TAG, "code_team status=" + result.status + " calls=" + result.providerCalls);
    }

    AuthorReviewerCodeCoordinator.Provider codeProvider(boolean cloud, boolean reviewing, String reviewerModel) {
        return new AuthorReviewerCodeCoordinator.Provider() {
            @Override public String name() {
                return cloud ? "Gemini " + (reviewing && !reviewerModel.isEmpty() ? reviewerModel : m.gemini.getModelName())
                        : "Modelo local seleccionado (rol " + (reviewing ? "revisora" : "autora") + ")";
            }
            @Override public ModelResult generate(String prompt) {
                if (cloud) return m.gemini.generateResultSync(prompt, null, reviewing ? reviewerModel : null);
                return m.llm == null ? ModelResult.failure(ModelResult.Status.UNAVAILABLE, "Sin modelo local.", 0)
                        : m.llm.generateResult(prompt, reviewing ? SalveLLM.Role.EVALUADOR : SalveLLM.Role.SISTEMA);
            }
        };
    }

    String previousCodeContext(String request) {
        if (!referencesPreviousCode(request)) return "";
        List<salve.core.conversation.ChatMessage> messages = m.conversationSession.snapshot();
        for (int i = messages.size() - 1; i >= 0; i--) {
            salve.core.conversation.ChatMessage message = messages.get(i);
            if (message.getRole() != salve.core.conversation.ChatMessage.Role.ASSISTANT) continue;
            String value = message.getContent();
            int start = value.indexOf("```java\n"), end = value.lastIndexOf("```");
            if (start >= 0 && end > start + 8) return value.substring(start + 8, end).trim();
        }
        return "";
    }

    boolean referencesPreviousCode(String request) {
        String text = java.text.Normalizer.normalize(request.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        return text.contains("anterior") || text.contains("esa funcion") || text.contains("ese codigo")
                || text.contains("ese fragmento") || text.contains("lo que has escrito");
    }

    String getReviewerModel() {
        return m.context.getSharedPreferences("salve_code_team", Context.MODE_PRIVATE).getString("reviewer_model", "");
    }
}
