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
 * Fotos en la conversación: analizarlas, elegir una guardada, reconocer a quien aparece y el entorno visual.
 * Sacado de MotorConversacional para aligerarlo; usa el motor a través de {@code m}.
 */
final class PhotoDesk {
    private final MotorConversacional m;

    PhotoDesk(MotorConversacional m) { this.m = m; }

    /** The caller transfers ownership; a bounded private copy is retained as visual memory. */
    void procesarImagen(String pregunta, Bitmap foto) {
        procesarImagen(pregunta, foto, m.llm != null && m.llm.usesOwnedInference());
    }

    void procesarImagen(String pregunta, Bitmap foto, boolean localOnly) {
        procesarImagen(pregunta, foto, localOnly, "", "", "");
    }

    void procesarImagen(String pregunta, Bitmap foto, boolean localOnly,
                              String personName, String relationship, String position) {
        if (foto == null || foto.isRecycled()) { m.hablar("No recibí una foto válida."); return; }
        m.goalAutonomy.userActivity();
        String entrada = pregunta == null || pregunta.trim().isEmpty() ? "Describe esta foto." : pregunta.trim();
        try {
            m.conversationExecutor.execute(() -> {
                AvatarMotionController.Session session = m.avatarSession;
                long turn = m.beginConversationTurn(entrada);
                m.avatarTurn.set(turn);
                m.goalAutonomy.beginUserTurn();
                try {
                    if (m.closed) return;
                    VisualMemoryRecord saved = m.visualMemories.capture(foto, entrada, personName, relationship, position, localOnly);
                    m.setActivePhoto(saved);
                    rememberDeclaredSelf(saved);
                    // «Esto es un mueble, dibújame uno»: analiza la geometría, aprende y dibuja.
                    m.turnSketchTag.set(null);
                    String photoSketch = m.dibujo.dibujarFoto(foto, salve.sketch.PhotoSketchRequest.parse(entrada, true));
                    if (!canAnalyzePhoto(saved) && photoSketch != null) {
                        m.conversationSession.addUser(entrada + " [Foto guardada: " + saved.id + "]");
                        m.hablar(photoSketch);
                        return;
                    }
                    if (!canAnalyzePhoto(saved)) {
                        m.conversationSession.addUser(entrada + " [Foto guardada: " + saved.id + "]");
                        m.hablar("He guardado la foto" + (saved.personName.isEmpty() ? "." : " con el nombre que confirmaste: " + saved.personName + ".")
                                + " Dolphin procesa texto, así que no he analizado la imagen. Puedes seguir hablando conmigo "
                                + "y usar los datos que has añadido; para analizar los píxeles hace falta un modelo con visión.");
                        return;
                    }
                    String visualInput = entrada + " [Foto guardada y adjunta: " + saved.id + "]";
                    m.conversationSession.addUser(visualInput);
                    CognitiveTurn.Outcome cognitive = m.runCognitiveTurn(visualInput,
                            "CONSULTA_VISUAL: describe sólo lo observable; los recuerdos no prueban lo que aparece en la foto."
                                    + (photoSketch == null ? "" : " | " + m.photoSketchContext),
                            null, false, m.memoria.recuperarContextoConversacional(entrada), true);
                    ModelResult result = cognitive.result;
                    String respuesta = result.isSuccess() ? result.getText() : m.modelFailureMessage(result);
                    AvatarMotionProtocol.Result motion = AvatarMotionProtocol.parse("");
                    if (!result.isSuccess() && session != null) AvatarMotionController.get().error(session, turn);
                    if (result.isSuccess()) m.setActivePhoto(m.visualMemories.update(saved.withAnalysis(respuesta)));
                    m.hablarPreparado(ResponseLimiter.limit(respuesta, VoiceResponsePolicy.maxResponseChars(false)), motion);
                } catch (Exception failure) {
                    m.hablar("No pude completar el guardado o análisis de la foto. "
                            + "Revisa Fotos guardadas antes de volver a enviarla.");
                    Log.w(MotorConversacional.TAG, "Fallo del archivo visual", failure);
                } finally {
                    m.goalAutonomy.endUserTurn();
                    if (session != null) AvatarMotionController.get().endTurn(session, turn);
                    m.avatarTurn.remove();
                    m.voiceTurn.remove();
                    foto.recycle();
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException e) {
            foto.recycle();
        }
    }

    boolean canAnalyzePhoto(VisualMemoryRecord photo) {
        boolean local = (m.llm != null && m.llm.usesOwnedInference()) || photo.localOnly;
        return local ? m.llm != null && m.llm.supportsVision() : true;
    }

    void seleccionarFotoGuardada(String id) {
        try { m.conversationExecutor.execute(() -> {
            if (m.closed) return;
            try {
                VisualMemoryRecord record = m.visualMemories.store.read(id);
                if (record == null) { m.hablar("No encuentro esa foto guardada."); return; }
                m.setActivePhoto(record);
                m.conversationSession.addUser("He seleccionado la foto guardada " + record.id + ": " + record.question);
                m.hablar("La foto seleccionada está en el contexto del chat. Puedes preguntarme sobre ella.");
            } catch (Exception error) { m.hablar("No pude recuperar esa foto guardada."); }
        }); } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    void identificarFoto(String id, String name, String relation, String position) {
        try { m.conversationExecutor.execute(() -> {
            if (m.closed) return;
            try {
                VisualMemoryRecord record = m.visualMemories.store.read(id);
                if (record == null) { m.hablar("No encuentro esa foto guardada."); return; }
                VisualMemoryRecord updated = m.visualMemories.update(record.identify(name, relation, position));
                rememberDeclaredSelf(updated);
                if (m.activePhoto != null && m.activePhoto.id.equals(id)) m.setActivePhoto(updated);
                m.conversationSession.addUser("Corrección de identidad declarada para la foto " + id + ": "
                        + updated.personName + "; relación: " + updated.relationship + "; posición: " + updated.position);
                m.hablar(name.trim().isEmpty() ? "He quitado la identidad de esa foto."
                        : "He guardado tu identificación de esa persona y actualizado su relación con la foto.");
            } catch (Exception error) { m.hablar("No pude guardar la identificación de esa foto."); }
        }); } catch (java.util.concurrent.RejectedExecutionException ignored) { }
    }

    void reconocerEntornoVisual() {
        // Ambient camera frames never become implicit cloud uploads.
        if (m.llm == null || !m.llm.usesOwnedInference()) {
            m.hablar("Usa IA y cámara para tomar una foto y confirmar su análisis con Gemini.");
            return;
        }
        List<Bitmap> frames = VideoAnalysisManager.getInstance().getRecentFrames();
        if (frames == null || frames.isEmpty()) { m.hablar("Usa IA y cámara para tomar una foto y analizarla."); return; }
        Bitmap snapshot = null;
        try {
            // The camera recycles its buffer while native model initialization may still be running.
            Bitmap frame = frames.get(frames.size() - 1);
            if (frame == null || frame.isRecycled()) throw new IllegalStateException("Frame no disponible");
            snapshot = frame.copy(Bitmap.Config.ARGB_8888, false);
            if (snapshot == null) throw new IllegalStateException("No se pudo copiar el frame");
            final Bitmap foto = snapshot;
            ModelResult result = ConversationModelRouter.generate(true, true, m.llm.supportsVision(), null,
                    () -> m.llm.generateImageResult("Nombra el objeto principal de la imagen con una sola palabra.", foto));
            String respuesta = result.isSuccess() ? "Detecto: " + result.getText() : m.modelFailureMessage(result);
            m.hablar(ResponseLimiter.limit(respuesta, VoiceResponsePolicy.maxResponseChars(false)));
        } catch (RuntimeException e) {
            Log.w(MotorConversacional.TAG, "No se pudo analizar el frame local", e);
            m.hablar("No pude analizar esa imagen. Usa IA y cámara para tomar una foto nueva.");
        } finally {
            if (snapshot != null) snapshot.recycle();
        }
    }

    void rememberDeclaredSelf(VisualMemoryRecord record) {
        if (record.identifiesUser() && m.creatorSide()) m.memoria.guardarDatoPerfil("Mi nombre es " + record.personName, "name");
    }
}
